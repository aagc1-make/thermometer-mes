import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const edge = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
const runtimeDir = 'D:\\My_project\\thermometer-mes-runtime'
const debugPort = 9333
const token = process.env.MES_TEST_TOKEN
const profileDir = mkdtempSync(join(tmpdir(), 'mes-layout-'))

assert.ok(token, 'MES_TEST_TOKEN is required')
mkdirSync(runtimeDir, { recursive: true })

const browser = spawn(edge, [
  '--headless=new',
  '--disable-gpu',
  '--no-first-run',
  '--no-default-browser-check',
  `--remote-debugging-port=${debugPort}`,
  `--user-data-dir=${profileDir}`,
  '--window-size=1440,900',
  'about:blank'
], { stdio: 'ignore' })

const delay = ms => new Promise(resolve => setTimeout(resolve, ms))

async function waitForTarget() {
  for (let i = 0; i < 60; i += 1) {
    try {
      const targets = await fetch(`http://127.0.0.1:${debugPort}/json/list`).then(r => r.json())
      const page = targets.find(target => target.type === 'page')
      if (page) return page
    } catch (_) {
      // Edge is still starting.
    }
    await delay(250)
  }
  throw new Error('Timed out waiting for Edge DevTools target')
}

function connect(url) {
  const ws = new WebSocket(url)
  const pending = new Map()
  let nextId = 1

  ws.addEventListener('message', event => {
    const message = JSON.parse(event.data)
    if (!message.id || !pending.has(message.id)) return
    const { resolve, reject } = pending.get(message.id)
    pending.delete(message.id)
    if (message.error) reject(new Error(message.error.message))
    else resolve(message.result)
  })

  const ready = new Promise((resolve, reject) => {
    ws.addEventListener('open', resolve, { once: true })
    ws.addEventListener('error', reject, { once: true })
  })

  return {
    ready,
    send(method, params = {}) {
      const id = nextId++
      return new Promise((resolve, reject) => {
        pending.set(id, { resolve, reject })
        ws.send(JSON.stringify({ id, method, params }))
      })
    },
    close() {
      ws.close()
    }
  }
}

let cdp
try {
  const target = await waitForTarget()
  cdp = connect(target.webSocketDebuggerUrl)
  await cdp.ready
  await cdp.send('Page.enable')
  await cdp.send('Network.enable')
  const cookie = await cdp.send('Network.setCookie', {
    name: 'Admin-Token',
    value: token,
    url: 'http://localhost/',
    path: '/'
  })
  assert.equal(cookie.success, true, 'authentication cookie should be accepted')
  await cdp.send('Page.navigate', { url: 'http://localhost/mes/md/workstation' })

  let ready = false
  for (let i = 0; i < 80; i += 1) {
    await delay(250)
    const state = await cdp.send('Runtime.evaluate', {
      expression: "Boolean(document.querySelector('.app-wrapper .sidebar-container .el-menu:not(.el-menu--collapse)'))",
      returnByValue: true
    })
    if (state.result.value) {
      ready = true
      break
    }
  }
  assert.equal(ready, true, 'authenticated layout should render')

  const measured = await cdp.send('Runtime.evaluate', {
    expression: `JSON.stringify((() => {
      const rect = selector => {
        const element = document.querySelector(selector)
        if (!element) return null
        const value = element.getBoundingClientRect()
        return { left: value.left, top: value.top, width: value.width, height: value.height }
      }
      return {
        viewport: document.documentElement.clientWidth,
        navbar: rect('.navbar'),
        rail: rect('.rail'),
        sidebar: rect('.sidebar-container'),
        main: rect('.main-container'),
        tags: rect('.tags-view-container'),
        hamburger: Boolean(document.querySelector('#hamburger-container')),
        collapsed: Boolean(document.querySelector('.sidebar-container .el-menu--collapse'))
      }
    })())`,
    returnByValue: true
  })
  const layout = JSON.parse(measured.result.value)

  assert.deepEqual(layout.navbar, { left: 0, top: 0, width: layout.viewport, height: 50 })
  assert.equal(layout.rail.left, 0)
  assert.equal(layout.rail.top, 50)
  assert.equal(layout.rail.width, 72)
  assert.equal(layout.sidebar.left, 72)
  assert.equal(layout.sidebar.top, 50)
  assert.equal(layout.sidebar.width, 200)
  assert.equal(layout.main.left, 272)
  assert.equal(layout.tags.left, 272)
  assert.equal(layout.tags.width, layout.viewport - 272)
  assert.equal(layout.hamburger, false)
  assert.equal(layout.collapsed, false)

  const screenshot = await cdp.send('Page.captureScreenshot', { format: 'png' })
  const screenshotPath = `${runtimeDir}\\fixed-layout.png`
  writeFileSync(screenshotPath, Buffer.from(screenshot.data, 'base64'))
  console.log(JSON.stringify({ ...layout, screenshotPath }, null, 2))
} finally {
  if (cdp) {
    try {
      await cdp.send('Browser.close')
    } catch (_) {
      cdp.close()
    }
  }
  browser.kill()
  await delay(500)
  rmSync(profileDir, { recursive: true, force: true, maxRetries: 5, retryDelay: 200 })
}

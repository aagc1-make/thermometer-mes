// 通过本机 HTTP 代理下载二进制文件（支持重定向、断点续传、进度输出）。
// 用法: node scripts/fetch-binary.mjs <url> <outPath>
import fs from 'node:fs'
import http from 'node:http'
import https from 'node:https'
import tls from 'node:tls'
import { URL } from 'node:url'

const PROXY = { host: '127.0.0.1', port: 7897 }
const [, , target, outPath] = process.argv
if (!target || !outPath) {
  console.error('用法: node scripts/fetch-binary.mjs <url> <outPath>')
  process.exit(2)
}

const existing = fs.existsSync(outPath) ? fs.statSync(outPath).size : 0

function connectProxy(host, port) {
  return new Promise((resolve, reject) => {
    const req = http.request({
      host: PROXY.host,
      port: PROXY.port,
      method: 'CONNECT',
      path: `${host}:${port}`,
      headers: { Host: `${host}:${port}` },
      timeout: 30000
    })
    req.on('connect', (res, socket) => {
      if (res.statusCode !== 200) return reject(new Error(`CONNECT ${res.statusCode}`))
      resolve(socket)
    })
    req.on('timeout', () => req.destroy(new Error('proxy timeout')))
    req.on('error', reject)
    req.end()
  })
}

async function request(url, startAt) {
  const u = new URL(url)
  const secure = u.protocol === 'https:'
  const port = Number(u.port || (secure ? 443 : 80))
  const headers = {
    Host: u.hostname,
    'User-Agent': 'Mozilla/5.0 dsh-fetch',
    Accept: '*/*',
    'Accept-Encoding': 'identity',
    Connection: 'close'
  }
  if (startAt > 0) headers.Range = `bytes=${startAt}-`

  const socket = await connectProxy(u.hostname, port)
  return new Promise((resolve, reject) => {
    const write = () => {
      const line = `GET ${u.pathname}${u.search} HTTP/1.1\r\n`
        + Object.entries(headers).map(([k, v]) => `${k}: ${v}`).join('\r\n')
        + '\r\n\r\n'
      if (secure) {
        const t = tls.connect({ socket, servername: u.hostname, rejectUnauthorized: false }, () => t.write(line))
        resolve(t)
      } else {
        socket.write(line)
        resolve(socket)
      }
    }
    write()
    socket.on('error', reject)
  })
}

function parseHead(buffer) {
  const sep = buffer.indexOf('\r\n\r\n')
  if (sep < 0) return null
  const head = buffer.subarray(0, sep).toString('latin1')
  const lines = head.split('\r\n')
  const status = Number((lines[0].match(/HTTP\/1\.[01] (\d{3})/) || [])[1] || 0)
  const headers = {}
  for (const line of lines.slice(1)) {
    const idx = line.indexOf(':')
    if (idx > 0) headers[line.slice(0, idx).trim().toLowerCase()] = line.slice(idx + 1).trim()
  }
  return { status, headers, bodyOffset: sep + 4 }
}

async function fetchOnce(url, startAt) {
  const stream = await request(url, startAt)
  const chunks = []
  let parsed = null
  let total = 0
  let bodyBytes = 0
  const file = startAt > 0 && parsed ? null : null

  // 流式处理：先解析响应头，再决定写入文件还是继续跟随重定向
  return new Promise((resolve, reject) => {
    let redirected = false
    onData()
    function onData() {
      stream.on('data', chunk => {
        if (redirected) return
        chunks.push(chunk)
        total += chunk.length
        if (!parsed) {
          const head = Buffer.concat(chunks)
          parsed = parseHead(head)
          if (!parsed) return
          if (parsed.status >= 300 && parsed.status < 400 && parsed.headers.location) {
            redirected = true
            stream.destroy()
            resolve({ redirect: parsed.headers.location })
            return
          }
          if (parsed.status !== 200 && parsed.status !== 206) {
            stream.destroy()
            reject(new Error(`HTTP ${parsed.status}`))
            return
          }
          const expected = Number(parsed.headers['content-length'] || 0)
          const mode = startAt > 0 && parsed.status === 206 ? 'a' : 'w'
          const out = fs.createWriteStream(outPath, { flags: mode })
          const body = Buffer.concat(chunks).subarray(parsed.bodyOffset)
          bodyBytes = body.length
          out.write(body)
          chunks.length = 0
          parsed.stream = out
          parsed.expected = expected
          stream.on('data', next => {
            bodyBytes += next.length
            out.write(next)
            if (bodyBytes % (20 * 1024 * 1024) < next.length) {
              console.log(`  ...${(bodyBytes / 1048576).toFixed(1)} MB`)
            }
          })
          stream.on('end', () => out.end(() => resolve({ ok: true, bytes: bodyBytes, expected })))
          stream.on('error', err => { out.end(); reject(err) })
          stream.on('close', () => out.end())
        }
      })
      stream.on('error', reject)
      stream.on('end', () => { if (!parsed) reject(new Error('空响应')) })
    }
  })
}

async function main() {
  let url = target
  let startAt = existing
  if (startAt > 0) console.log(`发现已存在文件 ${existing} 字节，尝试续传`)
  for (let i = 0; i < 6; i++) {
    const result = await fetchOnce(url, startAt)
    if (result.redirect) {
      url = new URL(result.redirect, url).toString()
      console.log(`重定向 -> ${url}`)
      continue
    }
    const size = fs.existsSync(outPath) ? fs.statSync(outPath).size : 0
    console.log(`完成: ${(size / 1048576).toFixed(1)} MB -> ${outPath}`)
    return
  }
  throw new Error('重定向次数过多')
}

main().catch(err => {
  console.error('下载失败:', err.message)
  process.exit(1)
})

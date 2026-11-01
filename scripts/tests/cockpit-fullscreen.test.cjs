const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')

function cockpit(screenfull) {
  const source = fs.readFileSync('ktg-mes-ui/src/views/mes/dashboard/index.vue', 'utf8')
  const script = source.match(/<script>([\s\S]*?)<\/script>/)[1]
    .replace(/^import .*$/mg, '').replace('export default', 'component =')
  const box = { component: null, screenfull, process: { env: {} },
    echarts: require('../../ktg-mes-ui/node_modules/echarts') }
  vm.createContext(box)
  vm.runInContext(script, box)
  const messages = []
  const ctx = { ...box.component.data(), ...box.component.methods,
    $refs: { cockpit: {} }, $message: { warning: m => messages.push(m), error: m => messages.push(m) } }
  ctx.$nextTick = callback => Promise.resolve(callback.call(ctx))
  return { ctx, definition: box.component, messages }
}

test('驾驶舱全屏只放大驾驶舱元素，按钮可再次退出', async () => {
  let target, exits = 0
  const screenfull = { isEnabled: true, element: null,
    request: async element => { target = element }, exit: async () => { exits++ } }
  const { ctx } = cockpit(screenfull)
  assert.equal(typeof ctx.toggleFullscreen, 'function', '驾驶舱需要提供全屏切换操作')
  await ctx.toggleFullscreen()
  assert.equal(target, ctx.$refs.cockpit)
  screenfull.element = ctx.$refs.cockpit
  ctx.isFullscreen = true
  await ctx.toggleFullscreen()
  assert.equal(exits, 1)
})

test('浏览器退出全屏后同步按钮状态并重排图表，页面全屏不冒充驾驶舱全屏', () => {
  const screenfull = { isEnabled: true, element: null }
  const { ctx } = cockpit(screenfull)
  let resized = 0
  ctx.charts = { stock: { resize: () => { resized++ } } }
  assert.equal(typeof ctx.onFullscreenChange, 'function', '需要监听浏览器的全屏状态变化')
  screenfull.element = ctx.$refs.cockpit
  ctx.onFullscreenChange()
  assert.equal(ctx.isFullscreen, true)
  screenfull.element = null
  ctx.onFullscreenChange()
  assert.equal(ctx.isFullscreen, false)
  screenfull.element = {}
  ctx.onFullscreenChange()
  assert.equal(ctx.isFullscreen, false)
  assert.equal(resized, 3)
})

test('不支持全屏或请求被拒绝时给出反馈，按钮恢复可用', async () => {
  for (const screenfull of [{ isEnabled: false },
    { isEnabled: true, request: async () => { throw new Error('denied') } }]) {
    const { ctx, messages } = cockpit(screenfull)
    assert.equal(typeof ctx.toggleFullscreen, 'function')
    await ctx.toggleFullscreen()
    assert.equal(messages.length, 1)
    assert.equal(ctx.fullscreenChanging, false)
  }
})

test('离开缓存页面时退出本驾驶舱全屏，不退出其它元素的全屏', async () => {
  let exits = 0
  const screenfull = { isEnabled: true, element: null, exit: async () => { exits++ } }
  const { ctx, definition } = cockpit(screenfull)
  assert.equal(typeof definition.deactivated, 'function', '缓存页面离开时需要清理全屏状态')
  screenfull.element = {}
  definition.deactivated.call(ctx)
  assert.equal(exits, 0)
  screenfull.element = ctx.$refs.cockpit
  definition.deactivated.call(ctx)
  assert.equal(exits, 1)
})

test('Esc 退出当前驾驶舱全屏，其它按键不触发退出', () => {
  let exits = 0
  const screenfull = { isEnabled: true, element: null, exit: async () => { exits++ } }
  const { ctx } = cockpit(screenfull)
  assert.equal(typeof ctx.onFullscreenKeydown, 'function')
  screenfull.element = ctx.$refs.cockpit
  ctx.onFullscreenKeydown({ key: 'Enter' })
  assert.equal(exits, 0)
  ctx.onFullscreenKeydown({ key: 'Escape' })
  assert.equal(exits, 1)
})

const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')

function login() {
  const saved = new Map([['defaultClient', '{"clientId":8}']])
  const code = fs.readFileSync('ktg-mes-ui/src/views/login.vue', 'utf8').match(/<script>([\s\S]*?)<\/script>/)[1]
    .replace(/^import .*$/mg, '').replace('export default', 'component =')
  const box = { component: null, require: name => name, encrypt: value => value,
    Cookies: { remove() {}, set() {} },
    localStorage: { setItem: (key, value) => saved.set(key, value), removeItem: key => saved.delete(key) } }
  vm.createContext(box); vm.runInContext(code, box)
  const ctx = { ...box.component.data(), ...box.component.methods,
    $store: { dispatch: async () => {} }, $router: { push: () => Promise.resolve() } }
  ctx.captchaReady = true; ctx.captchaOnOff = false
  Object.assign(ctx.loginForm, { username: 'fixture-user', password: 'fixture-password' })
  return { ctx, saved }
}

test('未打开打印设置时，登录保留已有默认客户端', async () => {
  const { ctx, saved } = login()
  await ctx.handleLogin()
  assert.equal(saved.get('defaultClient'), '{"clientId":8}')
})

test('用户显式清空打印客户端后，登录删除旧配置', async () => {
  const { ctx, saved } = login()
  ctx.clientList = [{ clientId: 8, clientName: '旧客户端' }]
  ctx.clientId = null; ctx.clientChanged = true
  await ctx.handleLogin()
  assert.equal(saved.has('defaultClient'), false)
})

test('用户明确选择新客户端后，登录使用新配置', async () => {
  const { ctx, saved } = login()
  ctx.clientList = [{ clientId: 9, clientName: '新客户端' }]
  ctx.clientId = 9; ctx.clientChanged = true
  await ctx.handleLogin()
  assert.equal(JSON.parse(saved.get('defaultClient')).clientId, 9)
})

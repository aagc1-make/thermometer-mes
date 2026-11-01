import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import vm from 'node:vm'

const read = relativePath => readFileSync(new URL(`../${relativePath}`, import.meta.url), 'utf8')

test('home hides the secondary sidebar while business pages and mobile keep their navigation', () => {
  const layout = read('ktg-mes-ui/src/layout/index.vue')
  const sidebar = read('ktg-mes-ui/src/layout/components/Sidebar/index.vue')
  const script = layout.match(/<script>([\s\S]*?)<\/script>/)[1]
    .replace(/^import .*$/mg, '').replace('export default', 'result =')
  const box = { result: null, ResizeMixin: {}, mapState: () => ({}), require: () => ({}),
    RightPanel: {}, AppMain: {}, Navbar: {}, Settings: {}, Sidebar: {}, TagsView: {}, Rail: {} }
  vm.createContext(box)
  vm.runInContext(script, box)
  const computed = box.result.computed
  const classes = (path, device, opened) => {
    const ctx = { $route: { path }, device, sidebar: { opened } }
    ctx.isHomePage = computed.isHomePage.call(ctx)
    return computed.classObj.call(ctx)
  }
  for (const path of ['/', '/index', '/index/']) assert.equal(classes(path, 'desktop', true).hideSidebar, true)
  assert.equal(classes('/mes/md/client', 'desktop', true).hideSidebar, false)
  assert.equal(classes('/mes/md/client', 'mobile', false).hideSidebar, true)
  assert.equal(classes('/mes/md/client', 'mobile', true).hideSidebar, false)
  assert.match(layout, /<sidebar[^>]+v-if="!sidebar.hide && !isHomePage"/)
  assert.match(sidebar, /return\s+this\.device\s*===\s*['"]mobile['"]\s*\?\s*!this\.sidebar\.opened\s*:\s*false/)
})

test('collapse control is available only on mobile', () => {
  const navbar = read('ktg-mes-ui/src/layout/components/Navbar.vue')

  assert.match(navbar, /<hamburger[^>]+v-if="device==='mobile'"/)
})

test('mobile drawer can close and reopen while desktop sidebar stays open', () => {
  const source = read('ktg-mes-ui/src/store/modules/app.js')
    .replace(/^import .*$/mg, '').replace('export default', 'result =')
  const box = { result: null, Cookies: { get: () => undefined, set() {} } }
  vm.createContext(box)
  vm.runInContext(source, box)
  const { state, mutations } = box.result
  mutations.TOGGLE_SIDEBAR(state)
  assert.equal(state.sidebar.opened, true)
  mutations.TOGGLE_DEVICE(state, 'mobile')
  mutations.CLOSE_SIDEBAR(state, true)
  assert.equal(state.sidebar.opened, false)
  mutations.TOGGLE_SIDEBAR(state)
  assert.equal(state.sidebar.opened, true)
  mutations.CLOSE_SIDEBAR(state, false)
  assert.equal(state.sidebar.opened, false)
})

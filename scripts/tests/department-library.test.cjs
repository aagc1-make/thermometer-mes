const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')

function responseInterceptor() {
  let onResponse
  const client = {
    interceptors: {
      request: { use() {} },
      response: { use(handler) { onResponse = handler } }
    }
  }
  const axios = { defaults: { headers: {} }, create: () => client }
  const box = {
    axios, process, Notification: { error() {} },
    errorCode: { 403: '当前操作没有权限', 404: '访问资源不存在', default: '系统未知错误' }
  }
  const source = fs.readFileSync('ktg-mes-ui/src/utils/request.js', 'utf8')
  vm.runInNewContext(source.replace(/^import .*$/mg, '').replace(/^export default .*$/m, '')
    .replace(/^export (let|function) /mg, '$1 '), box)
  return data => onResponse({ data, request: {} })
}

function libraryView({ admin = false, listDepartmentLibraries, listAdminDepartmentLibraries, listDept, createDepartmentLibrary }) {
  const box = { listDepartmentLibraries, listAdminDepartmentLibraries, listDept, createDepartmentLibrary, component: null }
  const file = 'ktg-mes-ui/src/views/mes/ai/DepartmentLibraries.vue'
  const script = fs.existsSync(file) ? fs.readFileSync(file, 'utf8').split('<script>')[1].split('</script>')[0] : ''
  vm.runInNewContext(script.replace(/^import .*$/mg, '').replace('export default', 'component ='), box)
  assert.ok(box.component, '缺少部门知识库页面组件')
  const notices = []
  const view = { ...box.component.data(), ...box.component.methods,
    $auth: { hasRole: role => admin && role === 'admin' },
    $message: { success: message => notices.push(message) } }
  for (const [name, getter] of Object.entries(box.component.computed)) {
    Object.defineProperty(view, name, { get: getter.bind(view) })
  }
  return { view, notices }
}

test('部门库列表使用服务端结果，空列表和请求失败有各自的状态', async () => {
  const library = { libraryId: 21, deptId: 2, deptName: '质量部', name: '质量部知识库' }
  let response = [library]
  const { view } = libraryView({ listDepartmentLibraries: async () => {
    if (response === null) throw new Error('暂时无法查询')
    return { data: response }
  } })
  await view.loadLibraries()
  assert.equal(view.libraries[0], library)
  response = []
  await view.loadLibraries()
  assert.equal(view.libraries.length, 0)
  assert.equal(view.loadError, '')
  response = null
  await view.loadLibraries()
  assert.match(view.loadError, /加载/)
  assert.equal(view.loading, false)
})

test('管理员创建后自动选中新建的库，切换时只显示所选库', async () => {
  let submitted
  const production = { deptId: 8, deptName: '生产部', status: '0' }
  const qualityLibrary = { libraryId: 29, deptId: 2, deptName: '质量部', name: '质量资料' }
  const productionLibrary = { libraryId: 30, deptId: 8, deptName: '生产部', name: '生产知识库' }
  const { view, notices } = libraryView({
    admin: true,
    listAdminDepartmentLibraries: async () => ({ data: submitted ? [qualityLibrary, productionLibrary] : [qualityLibrary] }),
    listDept: async () => ({ data: [production] }),
    createDepartmentLibrary: async data => { submitted = data }
  })
  await view.openCreate()
  assert.equal(view.departments[0].hasLibrary, false)
  view.form.deptId = 8
  view.form.name = '  生产知识库  '
  await view.saveLibrary()
  assert.equal(JSON.stringify(submitted), '{"deptId":8,"name":"生产知识库"}')
  assert.equal(view.creating, false)
  assert.equal(view.selectedLibrary, productionLibrary)
  view.selectedDeptId = 2
  assert.equal(view.selectedLibrary, qualityLibrary)
  assert.equal(notices.length, 1)
})

test('建库选项用上级路径区分同名部门，并标出已建库部门', async () => {
  const { view } = libraryView({
    admin: true,
    listDept: async () => ({ data: [
      { deptId: 1, deptName: '深圳总公司', status: '0', ancestors: '0' },
      { deptId: 2, deptName: '北京分公司', status: '0', ancestors: '0' },
      { deptId: 106, deptName: '财务部门', status: '0', ancestors: '0,1' },
      { deptId: 109, deptName: '财务部门', status: '0', ancestors: '0,2' },
      { deptId: 110, deptName: '停用部门', status: '1', ancestors: '0,2' }
    ] }),
    listAdminDepartmentLibraries: async () => ({ data: [{ deptId: 106 }] })
  })
  await view.openCreate()
  assert.equal(view.departments.map(dept => dept.deptId).join(','), '1,2,106,109')
  assert.equal(view.departments[2].label, '深圳总公司 / 财务部门（编号 106）（已建库）')
  assert.equal(view.departments[2].hasLibrary, true)
  assert.equal(view.departments[3].label, '北京分公司 / 财务部门（编号 109）')
  assert.equal(view.departments[3].hasLibrary, false)
})

test('真实响应拦截器把 403、404 和 409 的服务端文案作为错误对象返回', async () => {
  const handleResponse = responseInterceptor()
  for (const [code, msg] of [[403, '请由管理员授权'], [404, '部门已停用'], [409, '该部门已有知识库']]) {
    await assert.rejects(() => handleResponse({ code, msg }), error => {
      assert.equal(error.name, 'Error')
      assert.equal(error.message, msg)
      return true
    })
  }
  await assert.rejects(() => handleResponse({ code: 403 }), /当前操作没有权限/)
})

test('拒绝空名称，创建失败时显示真实 409 文案并保留表单', async () => {
  let attempts = 0
  const handleResponse = responseInterceptor()
  const { view } = libraryView({
    createDepartmentLibrary: async () => {
      attempts++
      return handleResponse({ code: 409, msg: '该部门已有知识库' })
    }
  })
  view.creating = true
  view.form = { deptId: 8, name: '  ' }
  await view.saveLibrary()
  assert.equal(attempts, 0)
  view.form.name = '生产知识库'
  await view.saveLibrary()
  assert.equal(attempts, 1)
  assert.equal(view.creating, true)
  assert.equal(view.form.name, '生产知识库')
  assert.equal(view.createError, '该部门已有知识库')
  assert.equal(view.saving, false)
})

test('新列表请求完成后，较旧的响应不会覆盖结果', async () => {
  let resolveOld
  const oldResponse = new Promise(resolve => { resolveOld = resolve })
  const library = { libraryId: 21, deptId: 2, deptName: '质量部', name: '新建质量部知识库' }
  let requests = 0
  const { view } = libraryView({
    listDepartmentLibraries: () => ++requests === 1 ? oldResponse : Promise.resolve({ data: [library] })
  })
  const oldLoad = view.loadLibraries()
  await view.loadLibraries()
  resolveOld({ data: [] })
  await oldLoad
  assert.equal(view.libraries[0], library)
  assert.equal(view.loading, false)
})

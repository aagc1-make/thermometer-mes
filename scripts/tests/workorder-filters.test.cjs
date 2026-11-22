const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')

/**
 * 工单列表的查询参数组装口径（查询区外观由共享组件 QueryBar 负责，见 query-bar.test.cjs）：
 * 日期区间必须拆成 requestDateBegin / requestDateEnd 两个参数发出去，数组不能直接透传。
 */
function component() {
  const source = fs.readFileSync('ktg-mes-ui/src/views/mes/pro/workorder/index.vue', 'utf8')
  const code = source.match(/<script>([\s\S]*?)<\/script>/)[1]
    .replace(/^import .*$/mg, '').replace('export default', 'component =')
  const box = {
    component: null,
    pipelineWorkorderContext: {},
    Treeselect: {}, ItemSelect: {}, ClientSelect: {}, VendorSelect: {},
    Workorderbom: {}, WorkorderItemList: {}, BarcodeImg: {}, ShortageAssistant: {}
  }
  vm.createContext(box)
  vm.runInContext(code, box)
  const ctx = Object.assign({}, box.component.data(), box.component.methods)
  Object.keys(box.component.computed || {}).forEach(key => {
    Object.defineProperty(ctx, key, { get: () => box.component.computed[key].call(ctx), configurable: true })
  })
  return ctx
}

test('需求日期区间拆成起止两个参数，数组本身不发给接口', () => {
  const ctx = component()
  ctx.queryParams.requestDateRange = ['2026-10-01', '2026-10-31']
  const query = ctx.buildQuery()
  assert.equal(query.requestDateBegin, '2026-10-01')
  assert.equal(query.requestDateEnd, '2026-10-31')
  assert.equal('requestDateRange' in query, false)
})

test('未选日期区间时不带起止参数，避免全表按 null 过滤', () => {
  const ctx = component()
  ctx.queryParams.requestDateRange = []
  const query = ctx.buildQuery()
  assert.equal(query.requestDateBegin, null)
  assert.equal(query.requestDateEnd, null)
})

test('工单关键词与编码复用同一个参数，不再单独发送工单名称', () => {
  const ctx = component()
  ctx.queryParams.workorderCode = '270001'
  const query = ctx.buildQuery()
  assert.equal(query.workorderCode, '270001')
  assert.equal(query.workorderName, null)
})

test('查询把页码拉回第一页，避免停留在旧页码看不到结果', () => {
  const ctx = component()
  ctx.queryParams.pageNum = 3
  let listed = 0
  ctx.getList = () => { listed += 1 }
  ctx.handleQuery()
  assert.equal(ctx.queryParams.pageNum, 1)
  assert.equal(listed, 1)
})

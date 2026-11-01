const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')

function pipelineModule() {
  const file = 'ktg-mes-ui/src/utils/workorderPipeline.js'
  if (!fs.existsSync(file)) return {}
  const box = { exports: {}, Promise, Set, Map }
  vm.createContext(box)
  vm.runInContext(fs.readFileSync(file, 'utf8').replace(/export (const|function|async function) /g, '$1 ') +
    '\nexports = { buildWorkorderPipeline, fetchPipelineRows, loadWorkorderPipeline, PIPELINE_STEPS };', box)
  return box.exports
}
function component(request = async () => ({ rows: [], total: 0 })) {
  const source = fs.readFileSync('ktg-mes-ui/src/views/dashboard/WorkorderPipeline.vue', 'utf8')
  const code = source.match(/<script>([\s\S]*?)<\/script>/)[1]
    .replace(/^import .*$/mg, '').replace('export default', 'component =')
  const box = { component: null, request, ...pipelineModule() }
  vm.createContext(box); vm.runInContext(code, box)
  const c = { ...box.component.data(), ...box.component.methods }
  for (const [key, get] of Object.entries(box.component.computed)) Object.defineProperty(c, key, { get: () => get.call(c) })
  return c
}
const wo = { workorderId: 1, workorderCode: 'MO-1', workorderType: 'SELF', productId: 10, quantity: 10, status: 'CONFIRMED', parentId: 0 }
const task = (id, processId, extra = {}) => ({ workorderId: 1, taskId: id, routeId: 20, processId, quantity: 10, isCheck: 'N', status: 'PREPARE', ...extra })
const feedback = (id, taskId, qty, extra = {}) => ({ workorderId: 1, recordId: id, feedbackCode: 'FB-' + id, taskId, processId: 100, quantityFeedback: qty, status: 'FINISHED', ...extra })
const bom = [{ itemId: 30, itemCode: 'RAW', quantity: 10, itemOrProduct: 'ITEM' }]
const issued = { issues: [{ workorderId: 1, issueId: 4, issueCode: 'ISS-1', status: 'FINISHED' }], issueLines: [{ issueId: 4, itemId: 30, itemCode: 'RAW', quantityIssued: 10 }] }
const scheduled = { bom, ...issued, processes: [{ routeId: 20, processId: 100, nextProcessId: 0, orderNum: 1, isCheck: 'N' }], tasks: [task(11, 100)], produces: [{ workorderId: 1, recordId: 700, processId: 100, status: 'FINISHED' }], produceLines: [{ recordId: 700, itemId: 10, quantityProduce: 10, qualityStatus: 'OK' }] }
function evaluate(data = {}, order = wo) {
  const c = component(); c.workorders = [order]; c.selectedId = order.workorderId
  c.steps = c.buildSteps(data)
  return c
}
const step = (c, key) => c.steps.find(s => s.key === key)

test('已确认的新工单直接提示排产，不受采购历史、缺料和销售历史影响', () => {
  const c = evaluate({ bom, stocks: [], sales: [{ status: 'FINISHED' }] })
  assert.equal(c.nextStep.key, 'schedule')
  assert.equal(c.currentIndex, 2)
})
test('保存但未确认的工单先提示确认', () => {
  assert.equal(evaluate({}, { ...wo, status: 'PREPARE' }).nextStep.key, 'confirm')
})
test('仅排部分工序或数量不足，继续提示排产', () => {
  for (const tasks of [[task(11, 100)], [task(11, 100), task(12, 101, { quantity: 3 })]]) {
    assert.equal(evaluate({ ...scheduled, processes: [{ routeId: 20, processId: 100 }, { routeId: 20, processId: 101 }], tasks }).nextStep.key, 'schedule')
  }
})
test('同工序分批排产累计达到计划量后可以领料', () => {
  const c = evaluate({ ...scheduled, issues: [], issueLines: [], stocks: [{ itemId: 30, warehouseCode: 'WH-RAW', quantityOnhand: 10 }], tasks: [task(11, 100, { quantity: 4 }), task(12, 100, { quantity: 6 })] })
  assert.equal(c.nextStep.key, 'issue')
})
test('排产后才检查缺料，冻结库存不计可用量', () => {
  const c = evaluate({ ...scheduled, issues: [], issueLines: [], stocks: [{ itemId: 30, warehouseCode: 'WH-RAW', quantityOnhand: 100, frozenFlag: 'Y' }] })
  assert.equal(c.nextStep.key, 'ready')
  assert.match(step(c, 'ready').text, /RAW/)
})
test('部分领料不算领完，其他工单领料不能抵扣', () => {
  const c = evaluate({ ...scheduled, stocks: [{ itemId: 30, warehouseCode: 'WH-RAW', quantityOnhand: 10 }], issues: [...issued.issues, { workorderId: 2, issueId: 5, status: 'FINISHED' }], issueLines: [{ issueId: 4, itemId: 30, quantityIssued: 3 }, { issueId: 5, itemId: 30, quantityIssued: 100 }] })
  assert.equal(c.nextStep.key, 'issue')
})
test('已领足的原料被生产消耗后，备料步骤不会退回缺料', () => {
  assert.equal(evaluate({ ...scheduled, stocks: [] }).nextStep.key, 'feedback')
})
test('同一任务的多张报工不能代替另一任务，数量不足不算完成', () => {
  const c = evaluate({ ...scheduled, tasks: [task(11, 100), task(12, 100)], feedbacks: [feedback(1, 11, 5), feedback(2, 11, 5)] })
  assert.notEqual(step(c, 'feedback').state, 'done')
})
test('同任务分批报工按数量累计，无检验工序标为不适用', () => {
  const c = evaluate({ ...scheduled, feedbacks: [feedback(1, 11, 4), feedback(2, 11, 6)] })
  assert.equal(step(c, 'feedback').state, 'done')
  assert.equal(step(c, 'ipqc').state, 'skipped')
  assert.equal(c.nextStep.key, 'finish')
  assert.equal(c.currentIndex, 7)
})
test('有报工待检时优先提示IPQC，检验放行后继续未完成工序', () => {
  const data = { ...scheduled, tasks: [task(11, 100, { isCheck: 'Y' }), task(12, 100)], feedbacks: [feedback(1, 11, 10, { status: 'UNCHECK', isCheck: 'Y' })], ipqcs: [{ workorderId: 1, taskId: 11, sourceDocId: 1, status: 'PREPARE' }] }
  assert.equal(evaluate(data).nextStep.key, 'ipqc')
  assert.equal(evaluate({ ...data, feedbacks: [feedback(1, 11, 10, { isCheck: 'Y' })], ipqcs: [{ workorderId: 1, taskId: 11, sourceDocId: 700, sourceDocType: 'FEEDBACK', sourceDocCode: 'FB-1', status: 'FINISHED', checkResult: 'ACCEPT' }] }).nextStep.key, 'feedback')
})
test('历史同产品、工单号前缀相似或ID冲突的单据不能影响本工单', () => {
  const c = evaluate({ ...scheduled, feedbacks: [feedback(1, 11, 10, { workorderId: 2, workorderCode: 'MO-1' })], ipqcs: [{ workorderId: 2, status: 'FINISHED', checkResult: 'ACCEPT' }], fgRecpts: [{ workorderId: 2, recptId: 6, status: 'FINISHED' }] })
  assert.equal(c.nextStep.key, 'feedback')
  assert.notEqual(step(c, 'fgIn').state, 'done')
})
test('完工状态不能伪造报工完成，产品入库需累计达到实际合格产出', () => {
  const order = { ...wo, status: 'FINISHED', quantityProduced: 10 }
  const data = { ...scheduled, feedbacks: [feedback(1, 11, 10)], fgRecpts: [{ workorderId: 1, recptId: 6, status: 'FINISHED' }], fgLines: [{ recptId: 6, itemId: 10, quantityRecived: 3 }] }
  assert.equal(evaluate(data, order).nextStep.key, 'fgIn')
  assert.equal(evaluate({ ...data, fgLines: [{ recptId: 6, itemId: 10, quantityRecived: 10 }] }, order).nextStep.key, 'sale')
  assert.equal(evaluate({ ...data, feedbacks: [] }, order).nextStep.key, 'feedback')
})
test('子工单完工后留在线边库，不强制成品入库或销售', () => {
  const c = evaluate({ ...scheduled, feedbacks: [feedback(1, 11, 10)] }, { ...wo, parentId: 9, status: 'FINISHED' })
  assert.equal(step(c, 'fgIn').state, 'skipped')
  assert.equal(c.nextStep, null)
  assert.equal(c.overallPercent, 100)
})
test('自制半成品不重复领料，无BOM时明确回到工单补齐用料', () => {
  const c = evaluate({ ...scheduled, bom: [{ ...bom[0], itemOrProduct: 'PRODUCT' }], issues: [], issueLines: [] })
  assert.equal(step(c, 'issue').state, 'skipped')
  const missing = evaluate({ ...scheduled, bom: [] })
  assert.equal(missing.nextStep.key, 'ready')
  assert.equal(missing.nextStep.route, '/mes/pro/workorder')
})
test('手动刷新重新查询工单，选择保留且包含已完工工单', async () => {
  let calls = 0
  const c = component(async ({ url }) => {
    if (url === '/mes/pro/workorder/list') { calls++; return { rows: [{ ...wo, status: 'FINISHED' }], total: 1 } }
    return { rows: [], total: 0 }
  })
  c.workorders = [wo]; c.selectedId = 1; c.loadPipeline = async () => {}
  await c.refresh()
  assert.equal(calls, 1)
  assert.equal(c.workorder.status, 'FINISHED')
})
test('切换工单后旧请求晚返回不得覆盖新工单步骤', async () => {
  const c = component(); let release
  c.workorders = [wo, { ...wo, workorderId: 2, workorderCode: 'MO-2', status: 'PREPARE' }]; c.selectedId = 1
  c.loadSnapshot = order => order.workorderId === 1 ? new Promise(resolve => { release = resolve }) : Promise.resolve({})
  const first = c.loadPipeline(); c.selectedId = 2; await c.loadPipeline()
  release({}); await first
  assert.equal(c.nextStep.key, 'confirm')
  assert.equal(c.loading, false)
})

test('流水线分页完整读取超过两页的数据，失败不冒充空结果', async () => {
  const { fetchPipelineRows } = pipelineModule()
  const rows = await fetchPipelineRows(async ({ params }) => ({ code: 200, rows: params.pageNum < 3 ? Array(200).fill({}) : [{ id: 401 }], total: 401 }), '/list')
  assert.equal(rows.length, 401)
  await assert.rejects(() => fetchPipelineRows(async () => ({ code: 403, msg: '无权限' }), '/list'))
  await assert.rejects(() => fetchPipelineRows(async () => ({ rows: [], total: 1 }), '/list'))
})
test('加载按工单ID查询生产和销售明细，排除其它工单与采购历史', async () => {
  const { loadWorkorderPipeline } = pipelineModule()
  const snapshot = await loadWorkorderPipeline(async ({ url, params }) => {
    if (url.includes('listProductProcess')) return { data: [{ routeId: 20, processId: 100 }] }
    assert.ok(!/arrival|iqc|itemrecpt|productsales\/list/.test(url), url)
    assert.equal(params.workorderId, 1)
    return { code: 200, rows: [{ workorderId: 2 }], total: 1 }
  }, wo)
  assert.equal(snapshot.tasks.length, 0)
  assert.equal(snapshot.bom.length, 0)
  assert.equal(snapshot.salesLines.length, 0)
})
test('有两批需检报工时，一张合格检验不能代替全部放行', () => {
  const c = evaluate({ ...scheduled, tasks: [task(11, 100, { isCheck: 'Y' })], feedbacks: [feedback(1, 11, 5), feedback(2, 11, 5)], ipqcs: [{ workorderId: 1, taskId: 11, sourceDocId: 1, status: 'FINISHED', checkResult: 'ACCEPT' }] })
  assert.equal(c.nextStep.key, 'ipqc')
})
test('流水线跳转携带当前工单上下文', () => {
  const c = evaluate(); let pushed
  c.$router = { push: value => { pushed = value; return Promise.resolve() } }
  c.go(c.nextStep)
  assert.equal(pushed.path, '/mes/pro/proschedule')
  assert.equal(pushed.query.pipelineWorkorderId, '1')
  assert.equal(pushed.query.pipelineWorkorderCode, 'MO-1')
})

function routeContext(query) {
  const file = 'ktg-mes-ui/src/mixins/pipelineWorkorderContext.js'
  if (!fs.existsSync(file)) return { definition: {}, ctx: { queryParams: {} } }
  const box = { definition: null }
  vm.createContext(box)
  vm.runInContext(fs.readFileSync(file, 'utf8').replace('export default', 'definition ='), box)
  const ctx = { queryParams: {}, $route: { query }, $set: (obj, key, value) => { obj[key] = value }, ...box.definition.methods }
  return { definition: box.definition, ctx }
}
test('目标页面首次进入和缓存切换工单均按工单ID筛选', () => {
  const { definition, ctx } = routeContext({ pipelineWorkorderId: '12', pipelineWorkorderCode: 'MO-12' })
  let queries = 0; ctx.getList = () => { queries++ }
  if (definition.created) definition.created.call(ctx)
  assert.equal(ctx.queryParams.workorderId, '12')
  ctx.$route.query = { pipelineWorkorderId: '13', pipelineWorkorderCode: 'MO-13' }
  definition.watch['$route.query.pipelineWorkorderId'].call(ctx)
  assert.equal(ctx.queryParams.workorderId, '13')
  assert.equal(ctx.queryParams.workorderCode, 'MO-13')
  assert.equal(queries, 1)
})
test('从流水线离开筛选上下文时清除工单筛选，不覆盖普通访问', () => {
  const { definition, ctx } = routeContext({ pipelineWorkorderId: '12', pipelineWorkorderCode: 'MO-12' })
  let queries = 0; ctx.getList = () => { queries++ }
  if (definition.created) definition.created.call(ctx)
  ctx.$route.query = {}; definition.watch['$route.query.pipelineWorkorderId'].call(ctx)
  assert.equal(ctx.queryParams.workorderId, null)
  assert.equal(ctx.queryParams.workorderCode, null)
  assert.equal(queries, 1)
})
test('IPQC来源ID是产品产出ID，按来源报工编码关联放行', () => {
  const c = evaluate({ ...scheduled, tasks: [task(11, 100, { isCheck: 'Y' })], feedbacks: [feedback(42, 11, 10)], ipqcs: [{ workorderId: 1, taskId: 11, sourceDocId: 900, sourceDocType: 'FEEDBACK', sourceDocCode: 'FB-42', status: 'FINISHED', checkResult: 'ACCEPT' }] })
  assert.equal(c.nextStep.key, 'finish')
})
test('入库目标按合格产出和末工序放行量计算，不采用工序累计报工数', () => {
  const c = evaluate({ ...scheduled, feedbacks: [feedback(1, 11, 10, { quantityUnquanlified: 2 })], produceLines: [{ recordId: 700, itemId: 10, quantityProduce: 8, qualityStatus: 'OK' }, { recordId: 700, itemId: 10, quantityProduce: 2, qualityStatus: 'NG' }], fgRecpts: [{ workorderId: 1, recptId: 6, status: 'FINISHED' }], fgLines: [{ recptId: 6, itemId: 10, quantityRecived: 8 }] }, { ...wo, status: 'FINISHED', quantityProduced: 50 })
  assert.equal(c.nextStep.key, 'sale')
  assert.equal(step(c, 'fgIn').docCode, '已入库 8/8')
})

test('成品入库后仍需发货，只有本工单已执行的销售数量足额才闭环', () => {
  const order = { ...wo, status: 'FINISHED' }
  const data = { ...scheduled, feedbacks: [feedback(1, 11, 10)],
    fgRecpts: [{ workorderId: 1, recptId: 6, status: 'FINISHED' }],
    fgLines: [{ recptId: 6, itemId: 10, quantityRecived: 10 }] }
  for (const salesLines of [[], [{ workorderId: 1, salesStatus: 'PREPARE', quantitySales: 10 }],
    [{ workorderId: 2, salesStatus: 'FINISHED', quantitySales: 10 }],
    [{ workorderId: 1, salesStatus: 'FINISHED', quantitySales: 4 }]]) {
    assert.equal(evaluate({ ...data, salesLines }, order).nextStep.key, 'sale')
  }
  const c = evaluate({ ...data, salesLines: [
    { workorderId: 1, salesStatus: 'FINISHED', quantitySales: 4 },
    { workorderId: 1, salesStatus: 'FINISHED', quantitySales: 6 }] }, order)
  assert.equal(c.nextStep, null)
  assert.equal(c.overallPercent, 100)
})
test('未放行产出、其它工单产出与其它产品不能抬高入库目标', () => {
  const c = evaluate({ ...scheduled, feedbacks: [feedback(1, 11, 10)], produces: [{ workorderId: 2, recordId: 700, processId: 100, status: 'FINISHED' }], fgRecpts: [{ workorderId: 1, recptId: 6, status: 'FINISHED' }], fgLines: [{ recptId: 6, itemId: 10, quantityRecived: 10 }] }, { ...wo, status: 'FINISHED' })
  assert.equal(c.nextStep.key, 'fgIn')
  assert.match(step(c, 'fgIn').text, /合格产出/)
})
test('缓存页面进入工单上下文时清除冲突筛选，离开时恢复之前的筛选', () => {
  const { definition, ctx } = routeContext({})
  ctx.queryParams = { pageNum: 1, pageSize: 10, feedbackCode: null, status: 'CONFIRMED' }
  ctx.getList = () => {}
  definition.created.call(ctx)
  ctx.queryParams.feedbackCode = 'FB-OTHER'
  ctx.$route.query = { pipelineWorkorderId: '12', pipelineWorkorderCode: 'MO-12' }
  definition.watch['$route.query.pipelineWorkorderId'].call(ctx)
  assert.equal(ctx.queryParams.feedbackCode, null)
  assert.equal(ctx.queryParams.status, null)
  ctx.$route.query = {}
  definition.watch['$route.query.pipelineWorkorderId'].call(ctx)
  assert.equal(ctx.queryParams.feedbackCode, 'FB-OTHER')
  assert.equal(ctx.queryParams.status, 'CONFIRMED')
})
test('页面重置会取消流水线隐藏的工单筛选', () => {
  const { definition, ctx } = routeContext({ pipelineWorkorderId: '12', pipelineWorkorderCode: 'MO-12' })
  definition.created.call(ctx)
  ctx.clearPipelineWorkorderContext()
  assert.equal(ctx.queryParams.workorderId, null)
  assert.equal(ctx.queryParams.workorderCode, null)
})
test('历史检验结论为空但数量核对通过且来源已过账时，不要求重新检验', () => {
  const c = evaluate({ ...scheduled, tasks: [task(11, 100, { isCheck: 'Y' })], feedbacks: [feedback(1, 11, 10)], ipqcs: [{ workorderId: 1, taskId: 11, sourceDocId: 700, sourceDocType: 'FEEDBACK', sourceDocCode: 'FB-1', status: 'FINISHED', checkResult: null, quantityCheck: 10, quantityQualified: 10, quantityUnqualified: 0 }] })
  assert.equal(c.nextStep.key, 'finish')
})
test('非关键工序不要求后端不会派发的IPQC', () => {
  const c = evaluate({ ...scheduled, processes: [{ routeId: 20, processId: 100, keyFlag: 'N', isCheck: 'Y' }], tasks: [task(11, 100, { isCheck: 'Y' })], feedbacks: [feedback(1, 11, 10)] })
  assert.equal(step(c, 'ipqc').state, 'skipped')
  assert.equal(c.nextStep.key, 'finish')
})
test('业务页重置先恢复表单初始值再清理工单上下文', () => {
  for (const file of ['pro/workorder', 'pro/schedule', 'pro/feedback', 'qc/ipqc', 'wm/issue', 'wm/productrecpt']) {
    const source = fs.readFileSync('ktg-mes-ui/src/views/mes/' + file + '/index.vue', 'utf8')
    const method = source.match(/resetQuery\(\)\s*\{([\s\S]*?)\n    \}/)[1]
    let sent
    const ctx = { queryParams: { workorderId: 1, workorderCode: 'MO-1' },
      resetForm() { this.queryParams.workorderCode = 'MO-1' },
      clearPipelineWorkorderContext() { this.queryParams.workorderId = null; this.queryParams.workorderCode = null },
      handleQuery() { sent = { ...this.queryParams } } }
    new Function(method).call(ctx)
    assert.equal(sent.workorderCode, null, file)
  }
})
test('实际mixin连续重置两次都不会恢复首次打开时的工单编码', () => {
  for (const file of ['pro/workorder', 'pro/schedule', 'pro/feedback', 'qc/ipqc', 'wm/issue', 'wm/productrecpt']) {
    const source = fs.readFileSync('ktg-mes-ui/src/views/mes/' + file + '/index.vue', 'utf8')
    const method = new Function(source.match(/resetQuery\(\)\s*\{([\s\S]*?)\n    \}/)[1])
    const { definition, ctx } = routeContext({ pipelineWorkorderId: '12', pipelineWorkorderCode: 'MO-12' })
    ctx.queryParams = { workorderCode: null }
    definition.created.call(ctx)
    const requests = []
    ctx.resetForm = () => { ctx.queryParams.workorderCode = 'MO-12' }
    ctx.handleQuery = () => { requests.push(ctx.queryParams.workorderCode) }
    method.call(ctx); method.call(ctx)
    assert.deepEqual(requests, [null, null], file)
  }
})

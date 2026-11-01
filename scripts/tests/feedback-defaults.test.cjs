const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')

// 报工弹窗"自动带出任务/工作站/数量"的口径与页面接线：默认值错了会直接报错工或重复建单。
function defaultsModule() {
  const file = 'ktg-mes-ui/src/utils/feedbackDefaults.js'
  const box = { exports: {}, Set, Map, Number }
  vm.createContext(box)
  vm.runInContext(fs.readFileSync(file, 'utf8').replace(/export (const|function) /g, '$1 ') +
    '\nexports = { taskRemaining, pendingTasks, defaultTask, openTaskIds, quantityDefaults };', box)
  return box.exports
}

function component(stubs = {}) {
  const source = fs.readFileSync('ktg-mes-ui/src/views/mes/pro/feedback/index.vue', 'utf8')
  const code = source.match(/<script>([\s\S]*?)<\/script>/)[1]
    .replace(/^import .*$/mg, '').replace('export default', 'component =')
  const box = {
    component: null, pipelineWorkorderContext: {}, WorkorderSelect: {}, UserSingleSelect: {},
    ProductProduce: {}, ItemConsume: {}, ...defaultsModule(), ...stubs
  }
  vm.createContext(box)
  vm.runInContext(code, box)
  const c = { ...box.component.data(), ...box.component.methods }
  c.formatDate = () => '2026-10-31 22:20:00'
  c.resetForm = () => {}
  c.user = { userId: 1, userName: 'admin', nickName: '若依' }
  c.queryParams = { workorderId: null, workorderCode: null }
  c.$modal = { msgError: () => {}, msgSuccess: () => {} }
  return { c, source }
}

const task = (taskId, processId, extra = {}) => ({
  taskId, processId, quantity: 10, quantityProduced: 0, status: 'NORMAL', isCheck: 'N', unitName: '台',
  taskCode: 'TASK' + taskId, taskName: '任务' + taskId,
  workstationId: 9000 + processId, workstationCode: 'WS' + processId, workstationName: '工作站' + processId,
  workorderId: 919035, workorderCode: 'MO202610310014', workorderName: '红外测温枪整机 首批生产',
  itemId: 9201, itemCode: 'TG-FG-001', itemName: '红外测温枪整机', specification: 'TG-300', unitOfMeasure: 'TAI',
  routeId: 9453, routeCode: 'RT-1', processCode: 'OP' + processId, processName: '工序' + processId, ...extra
})
const pending = [
  task(493, 9409, { processName: '包装入库' }),
  task(489, 9405, { processName: '整机总装', quantityProduced: 4 }),
  task(490, 9406, { processName: '整机功能测试', isCheck: 'Y' })
]
const processes = [{ processId: 9405, orderNum: 1 }, { processId: 9406, orderNum: 2 }, { processId: 9409, orderNum: 5 }]
const stubs = (rows = pending, feedbacks = []) => ({
  listProtask: async () => ({ rows, total: rows.length }),
  listFeedback: async () => ({ rows: feedbacks, total: feedbacks.length }),
  listProductprocess: async () => ({ data: processes })
})

test('剩余可报数量按"排产数量-已报工数量"算，报满和超报都不再计入', () => {
  const { taskRemaining } = defaultsModule()
  assert.equal(taskRemaining(task(1, 1)), 10)
  assert.equal(taskRemaining(task(1, 1, { quantityProduced: 4 })), 6)
  assert.equal(taskRemaining(task(1, 1, { quantityProduced: 10 })), 0)
  assert.equal(taskRemaining(task(1, 1, { quantityProduced: 12 })), 0)
})

test('可报工任务按工艺路线顺序排，已完工和已报满的排除', () => {
  const { pendingTasks } = defaultsModule()
  const rows = pendingTasks([...pending, task(494, 9410, { status: 'FINISHED' }), task(495, 9411, { quantityProduced: 10 })], processes)
  assert.deepEqual(rows.map(row => row.taskId), [489, 490, 493])
})

test('换任务默认把该工序剩余数量一次报完，免检工序拆出合格/不良两栏', () => {
  const { quantityDefaults } = defaultsModule()
  const partial = quantityDefaults(task(489, 9405, { quantityProduced: 4 }))
  assert.equal(partial.quantityFeedback, 6)
  assert.equal(partial.quantityQualified, 6)
  assert.equal(partial.quantityUnquanlified, 0)
  const checked = quantityDefaults(task(490, 9406, { isCheck: 'Y' }))
  assert.equal(checked.quantityFeedback, 10)
  assert.equal(checked.quantityQualified, 0)
  assert.equal(checked.quantityUnquanlified, 0)
})

test('首道待报工工序已有未完成报工单时不自动选中，也不越过它去报后面的工序', () => {
  const { defaultTask, openTaskIds } = defaultsModule()
  assert.equal(defaultTask(pending, processes, new Set()).taskId, 489)
  const openIds = openTaskIds([{ taskId: 489, status: 'PREPARE' }, { taskId: 493, status: 'FINISHED' }])
  assert.deepEqual(Array.from(openIds), ['489'])
  assert.equal(defaultTask(pending, processes, openIds), null)
  assert.equal(defaultTask([], processes, openIds), null)
})

test('新增弹窗默认填好报工类型、报工人、审核人和报工时间', () => {
  const { c } = component()
  c.reset()
  assert.equal(c.form.feedbackType, 'SELF')
  assert.equal(c.form.feedbackChannel, 'PC')
  assert.equal(c.form.userName, 'admin')
  assert.equal(c.form.nickName, '若依')
  assert.equal(c.form.recordUser, 'admin')
  assert.equal(c.form.recordNick, '若依')
  assert.equal(c.form.feedbackTime, '2026-10-31 22:20:00')
  assert.equal(c.taskOptions.length, 0)
})

test('未选工单时新增只要求选工单，不会凭空带出别人的任务', async () => {
  const { c } = component(stubs())
  await c.handleAdd()
  assert.equal(c.open, true)
  assert.equal(c.form.workorderId, null)
  assert.equal(c.form.taskId, null)
})

test('从流水线带入工单后一键带出任务、工作站和数量', async () => {
  const { c } = component(stubs())
  c.queryParams = { workorderId: '919035', workorderCode: 'MO202610310014' }
  await c.handleAdd()
  assert.equal(c.form.workorderId, 919035)
  assert.equal(c.form.taskId, 489)
  assert.equal(c.form.processName, '整机总装')
  assert.equal(c.form.workstationName, '工作站9405')
  assert.equal(c.form.workstationId, 9405 + 9000)
  assert.equal(c.form.isCheck, 'N')
  assert.equal(c.form.quantityFeedback, 6)
  assert.equal(c.form.quantityQualified, 6)
  assert.equal(c.form.quantityUnquanlified, 0)
  assert.equal(c.taskOptions.length, 3)
})

test('手工选工单后同样自动带出首道工序，不需要再选工作站', async () => {
  const { c } = component(stubs())
  c.reset()
  c.onWorkorderSelected({ workorderId: 919035, workorderCode: 'MO202610310014', productId: 9201, productCode: 'TG-FG-001', productName: '红外测温枪整机' })
  await new Promise(resolve => setImmediate(resolve))
  assert.equal(c.form.taskId, 489)
  assert.equal(c.form.workstationCode, 'WS9405')
  assert.equal(c.form.quantityFeedback, 6)
})

test('首道工序已有草稿报工单时提示在先处理，不自动建第二张', async () => {
  const { c } = component(stubs(pending, [{ taskId: 489, status: 'PREPARE' }]))
  c.queryParams = { workorderId: '919035' }
  await c.handleAdd()
  assert.equal(c.form.taskId, null)
  assert.equal(c.taskOptions[0].hasOpenFeedback, true)
  assert.equal(c.form.quantityFeedback, 0)
})

test('清空或改选任务时不会把上一张任务的工作站带到新单据', async () => {
  const { c } = component(stubs(pending, [{ taskId: 999, status: 'PREPARE' }]))
  c.queryParams = { workorderId: '919035' }
  await c.handleAdd()
  assert.equal(c.form.taskId, 489)
  c.onTaskChange(undefined)
  assert.equal(c.form.taskId, null)
  assert.equal(c.form.workstationId, null)
  assert.equal(c.form.quantityFeedback, 0)
  c.onTaskChange(490)
  assert.equal(c.form.taskId, 490)
  assert.equal(c.form.isCheck, 'Y')
  assert.equal(c.form.quantityFeedback, 10)
})

test('页面不再要求手工选工作站/任务：没有工作站和任务的选择弹窗', () => {
  const { source } = component()
  assert.equal(source.includes('WorkstationSelect'), false)
  assert.equal(source.includes('ProtaskSelect'), false)
  assert.equal(source.includes('form.taskCode" placeholder="请选择生产任务"'), false)
})

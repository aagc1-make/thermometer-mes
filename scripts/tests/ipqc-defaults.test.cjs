const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')

// 过程检验"一键带出"的口径与页面接线：来源填错会变成放行不了报工的孤儿单。
function moduleOf(file, exportsCode) {
  const box = { exports: {}, Set, Map, Number }
  vm.createContext(box)
  vm.runInContext(fs.readFileSync(file, 'utf8').replace(/export (const|function) /g, '$1 ') + '\nexports = ' + exportsCode + ';', box)
  return box.exports
}
const feedbackUtils = () => moduleOf('ktg-mes-ui/src/utils/feedbackDefaults.js',
  '{ taskRemaining, pendingTasks, defaultTask, openTaskIds, quantityDefaults }')
const ipqcUtils = () => moduleOf('ktg-mes-ui/src/utils/ipqcDefaults.js',
  '{ pickInspectionFeedback, pickProduceLine, findOpenIpqc, qualifiedFromCheck, inspectionDefaults }')

function component(stubs = {}) {
  const source = fs.readFileSync('ktg-mes-ui/src/views/mes/qc/ipqc/index.vue', 'utf8')
  const code = source.match(/<script>([\s\S]*?)<\/script>/)[1]
    .replace(/^import .*$/mg, '').replace('export default', 'component =')
  const box = {
    component: null, pipelineWorkorderContext: {}, WorkorderSelect: {}, WorkstationSelect: {},
    Ipqcline: {}, QCResutl: {}, ...feedbackUtils(), ...ipqcUtils(),
    genCode: async () => 'IPQC-TEST-0001',
    getUserProfile: async () => ({ data: { userName: 'admin', nickName: '若依' } }),
    ...stubs
  }
  vm.createContext(box)
  vm.runInContext(code, box)
  const c = { ...box.component.data(), ...box.component.methods }
  c.formatDate = () => '2026-10-31'
  c.resetForm = () => {}
  c.$modal = { msgError: () => {}, msgSuccess: () => {}, confirm: () => Promise.resolve() }
  c.queryParams = { workorderId: null }
  c.$refs = { form: { validate: cb => cb(true) } }
  return { c, source }
}

const feedback = (extra = {}) => ({
  recordId: 578, feedbackCode: 'FB202610310014', status: 'UNCHECK',
  workorderId: 919035, workorderCode: 'MO202610310014', workorderName: '红外测温枪整机 首批生产',
  taskId: 490, taskCode: 'TASK20260463', taskName: '红外测温枪整机【10】台',
  workstationId: 9406, workstationCode: 'TG-ST60', workstationName: '整机功能测试站',
  processId: 9406, processCode: 'TG-OP60', processName: '整机功能测试',
  itemId: 9201, itemCode: 'TG-FG-001', itemName: '红外测温枪整机',
  specification: 'TG-300', unitOfMeasure: 'TAI', unitName: '台',
  quantityFeedback: 10, quantityUncheck: 10, ...extra
})
const line = (extra = {}) => ({
  lineId: 320, recordId: 918060, itemId: 9201, itemCode: 'TG-FG-001', itemName: '红外测温枪整机',
  quantityProduce: 10, qualityStatus: 'OK', unitName: '台', ...extra
})
const draft = (extra = {}) => ({
  ipqcId: 118, ipqcCode: 'IPQC202610310013', status: 'PREPARE', ipqcType: 'IPQC',
  workorderId: 919035, workorderCode: 'MO202610310014', taskId: 490, taskCode: 'TASK20260463',
  workstationCode: 'TG-ST60', workstationName: '整机功能测试站', processName: '整机功能测试',
  quantityCheck: 10, quantityQualified: 0, quantityUnqualified: 0, checkResult: null, inspectDate: null,
  sourceDocType: 'FEEDBACK', sourceDocId: 918060, sourceDocCode: 'FB202610310014', sourceLineId: 320, ...extra
})
const task = (extra = {}) => ({
  taskId: 489, taskCode: 'TASK20260462', taskName: '红外测温枪整机【10】台', status: 'NORMAL',
  processId: 9405, processCode: 'TG-OP50', processName: '整机总装',
  workstationId: 9405, workstationCode: 'TG-ST50', workstationName: '整机总装站',
  workorderId: 919035, workorderCode: 'MO202610310014', workorderName: '红外测温枪整机 首批生产',
  itemId: 9201, itemCode: 'TG-FG-001', itemName: '红外测温枪整机', specification: 'TG-300',
  unitOfMeasure: 'TAI', unitName: '台', quantity: 10, quantityProduced: 0, ...extra
})
const stubs = ({ feedbacks = [feedback()], lines = [line()], ipqcs = [], tasks = [task()], processes = [{ processId: 9405, orderNum: 1 }] } = {}) => ({
  listFeedback: async () => ({ rows: feedbacks, total: feedbacks.length }),
  listProductproduceline: async () => ({ rows: lines, total: lines.length }),
  listIpqc: async () => ({ rows: ipqcs, total: ipqcs.length }),
  listProtask: async () => ({ rows: tasks, total: tasks.length }),
  listProductprocess: async () => ({ data: processes })
})

test('待检报工优先选中，其次用最近一张报工', () => {
  const { pickInspectionFeedback } = ipqcUtils()
  const finished = feedback({ recordId: 2, status: 'FINISHED' })
  const unchecked = feedback({ recordId: 3, status: 'UNCHECK' })
  assert.equal(pickInspectionFeedback([finished, unchecked]).recordId, 3)
  assert.equal(pickInspectionFeedback([finished]).recordId, 2)
  assert.equal(pickInspectionFeedback([{ recordId: 4, status: 'CANCELED' }]), null)
  assert.equal(pickInspectionFeedback([]), null)
})

test('产出行按报工物料匹配，缺少汇总数量时取第一条有效行', () => {
  const { pickProduceLine } = ipqcUtils()
  const other = line({ lineId: 9, itemId: 9999 })
  assert.equal(pickProduceLine([other, line()], feedback()).lineId, 320)
  assert.equal(pickProduceLine([other], feedback()).lineId, 9)
  assert.equal(pickProduceLine([line({ quantityProduce: 0 })], feedback()), null)
  assert.equal(pickProduceLine([line()], null), null)
})

test('同一产出行已有未完成检验单时复用它，已完成的不复用', () => {
  const { findOpenIpqc } = ipqcUtils()
  assert.equal(findOpenIpqc([draft({ status: 'FINISHED' })], line()), null)
  assert.equal(findOpenIpqc([draft()], line()).ipqcId, 118)
  assert.equal(findOpenIpqc([draft({ sourceLineId: 999 })], line()), null)
  assert.equal(findOpenIpqc([draft()], null), null)
})

test('合格数量始终按"检测数量-不合格数"配平', () => {
  const { qualifiedFromCheck } = ipqcUtils()
  assert.equal(qualifiedFromCheck(10, 0), 10)
  assert.equal(qualifiedFromCheck(10, 3), 7)
  assert.equal(qualifiedFromCheck(10, 12), 0)
  assert.equal(qualifiedFromCheck(null, 2), 0)
})

test('待检报工带出整张检验单，并写入来源单据', () => {
  const { inspectionDefaults } = ipqcUtils()
  const form = inspectionDefaults({ feedback: feedback(), line: line(), inspector: '若依', today: '2026-10-31' })
  assert.equal(form.ipqcType, 'IPQC')
  assert.equal(form.ipqcName, 'MO202610310014-整机功能测试-过程检验')
  assert.equal(form.workorderId, 919035)
  assert.equal(form.workstationCode, 'TG-ST60')
  assert.equal(form.processName, '整机功能测试')
  assert.equal(form.quantityCheck, 10)
  assert.equal(form.quantityQualified, 10)
  assert.equal(form.quantityUnqualified, 0)
  assert.equal(form.checkResult, 'ACCEPT')
  assert.equal(form.inspector, '若依')
  assert.equal(form.inspectDate, '2026-10-31')
  assert.equal(form.sourceDocType, 'FEEDBACK')
  assert.equal(form.sourceDocId, 918060)
  assert.equal(form.sourceDocCode, 'FB202610310014')
  assert.equal(form.sourceLineId, 320)
})

test('没有待检产出行时只带出数量，不伪造来源单据', () => {
  const { inspectionDefaults } = ipqcUtils()
  const form = inspectionDefaults({ feedback: feedback({ status: 'FINISHED', quantityUncheck: 0 }), inspector: '若依', today: '2026-10-31' })
  assert.equal(form.quantityCheck, 10)
  assert.equal(form.sourceDocType, null)
  assert.equal(form.sourceDocCode, null)
  const fromTask = inspectionDefaults({ task: task({ quantityProduced: 4 }), inspector: '若依', today: '2026-10-31' })
  assert.equal(fromTask.quantityCheck, 6)
  assert.equal(fromTask.sourceLineId, null)
})

test('新增弹窗默认填好检验类型、编号、结论、检测人员和日期', async () => {
  const { c } = component(stubs({ feedbacks: [] }))
  await c.handleAdd()
  assert.equal(c.open, true)
  assert.equal(c.form.ipqcType, 'IPQC')
  assert.equal(c.form.checkResult, 'ACCEPT')
  assert.equal(c.form.inspectDate, '2026-10-31')
  assert.equal(c.form.inspector, '若依')
  assert.equal(c.autoGenFlag, true)
  assert.equal(c.form.ipqcCode, 'IPQC-TEST-0001')
})

test('从待检报工一键带出任务、工作站、数量和来源单据', async () => {
  const { c } = component(stubs())
  c.queryParams = { workorderId: '919035', workorderCode: 'MO202610310014' }
  await c.handleAdd()
  assert.equal(c.form.workorderId, 919035)
  assert.equal(c.form.taskCode, 'TASK20260463')
  assert.equal(c.form.workstationCode, 'TG-ST60')
  assert.equal(c.form.processName, '整机功能测试')
  assert.equal(c.form.quantityCheck, 10)
  assert.equal(c.form.quantityQualified, 10)
  assert.equal(c.form.sourceDocType, 'FEEDBACK')
  assert.equal(c.form.sourceLineId, 320)
  assert.equal(c.inspectionTip.type, 'success')
  assert.match(c.inspectionTip.text, /FB202610310014/)
})

test('系统已自动生成检验单时直接打开它，不再建重复单', async () => {
  const { c } = component(stubs({ ipqcs: [draft()] }))
  c.queryParams = { workorderId: '919035' }
  await c.handleAdd()
  assert.equal(c.form.ipqcCode, 'IPQC202610310013')
  assert.equal(c.optType, 'edit')
  assert.equal(c.autoGenFlag, false)
  assert.match(c.title, /已自动生成/)
  assert.equal(c.form.quantityQualified, 10)
  assert.equal(c.inspectionTip.type, 'success')
})

test('已有的自动检验单已填过数量时不被覆盖', async () => {
  const { c } = component(stubs({ ipqcs: [draft({ quantityQualified: 7, quantityUnqualified: 3, checkResult: 'REJECT' })] }))
  c.queryParams = { workorderId: '919035' }
  await c.handleAdd()
  assert.equal(c.form.quantityQualified, 7)
  assert.equal(c.form.quantityUnqualified, 3)
  assert.equal(c.form.checkResult, 'REJECT')
})

test('该工单还没报工时按待报工工序带出工作站和数量', async () => {
  const { c } = component(stubs({ feedbacks: [] }))
  c.queryParams = { workorderId: '919035' }
  await c.handleAdd()
  assert.equal(c.form.taskId, 489)
  assert.equal(c.form.workstationCode, 'TG-ST50')
  assert.equal(c.form.quantityCheck, 10)
  assert.equal(c.form.sourceDocType, null)
  assert.equal(c.inspectionTip.type, 'info')
})

test('没有工单也没有待检报工时给出明确提示，不凭空造单', async () => {
  const { c } = component(stubs({ feedbacks: [] }))
  await c.handleAdd()
  assert.equal(c.form.workorderId, null)
  assert.equal(c.inspectionTip.type, 'warning')
  assert.match(c.inspectionTip.text, /没有待检报工/)
})

test('改检测数量或不合格数时自动配平合格数量', () => {
  const { c } = component(stubs())
  c.reset()
  c.form.quantityCheck = 10
  c.form.quantityUnqualified = 3
  c.handleUnqualifiedChanged()
  assert.equal(c.form.quantityQualified, 7)
  c.form.quantityCheck = 12
  c.handleCheckQuantityChanged()
  assert.equal(c.form.quantityQualified, 9)
  c.form.quantityLaborScrap = 1
  c.form.quantityMaterialScrap = 1
  c.form.quantityOtherScrap = 0
  c.handleScrapChanged()
  assert.equal(c.form.quantityUnqualified, 2)
  assert.equal(c.form.quantityQualified, 10)
})

test('页面不再把任务名称错绑成任务编号，完成按钮也不再要求先保存', () => {
  const { source } = component()
  assert.equal(/label="任务名称"[\s\S]{0,200}v-model="form\.taskCode"/.test(source), false)
  assert.equal(/label="任务名称"[\s\S]{0,200}v-model="form\.taskName"/.test(source), true)
  assert.equal(source.includes('&& form.ipqcId !=null">完成检验'), false)
  assert.equal(source.includes('>完成检验<'), true)
})

const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')

// ai/index.vue 会剥离 import 后放进沙箱运行，这里把它的纯逻辑依赖一并注入。
function answerRules() {
  const box = { exports: {} }
  vm.createContext(box)
  vm.runInContext(fs.readFileSync('ktg-mes-ui/src/utils/mesAnswerRules.js', 'utf8')
    .replace(/export (const|function) /g, '$1 ') +
    '\nexports={detectMesTopic,buildInspectionAnswer,buildUnqualifiedAnswer,knowledgeFallback};', box)
  return box.exports
}

const ITEMS = [
  { itemId: 9205, itemCode: 'TG-EL-002', itemName: '红外传感器模块', unitName: '个', unitOfMeasure: '个', enableFlag: 'Y' },
  { itemId: 9201, itemCode: 'TG-FG-001', itemName: '红外测温枪整机', unitName: '台', unitOfMeasure: '台', enableFlag: 'Y' },
  { itemId: 9206, itemCode: 'TG-EL-003', itemName: '显示组件', unitName: '个', unitOfMeasure: '个', enableFlag: 'Y' }
]
const IQC = [
  { iqcId: 61, iqcCode: 'IQC20261101001', itemId: 9205, itemCode: 'TG-EL-002', itemName: '红外传感器模块', templateId: 9821, status: 'FINISHED', checkResult: 'ACCEPT', quantityUnqualified: 2, quantityCheck: 20, quantityQualified: 18, vendorName: '深圳传感科技有限公司', inspectDate: '2026-11-01 09:20:00' },
  { iqcId: 62, iqcCode: 'IQC20261101002', itemId: 9206, itemName: '显示组件', status: 'FINISHED', checkResult: 'ACCEPT', quantityUnqualified: 0, quantityCheck: 10, inspectDate: '2026-11-01 10:00:00' }
]
const INDEXES = [
  { indexCode: 'TG-QI-02', indexName: '传感器响应', checkMethod: '通电抽检', qcTool: '测试治具', standerVal: 50, unitOfMeasure: 'mV', thresholdMin: 1, thresholdMax: 100, remark: '判定标准：通电后响应正常' },
  { indexCode: 'TG-QI-03', indexName: 'LCD 显示', checkMethod: '外观检查', qcTool: '目视', remark: '判定标准：无破损、无缺件' }
]
const RTV = [
  { rtId: 71, rtCode: 'RTV20261101001', rtName: '来料不合格退供-IQC20261101001', status: 'PREPARE', attr2: 'PRE_STOCK' },
  { rtId: 72, rtCode: 'RTV20261030002', rtName: '采购入库退供-20261030', status: 'FINISHED', attr2: 'STOCK' }
]

function chat({ denied = [], indexes = INDEXES, iqc = IQC } = {}) {
  const calls = []
  const request = async config => {
    calls.push(config)
    const url = config.url
    if (url === '/mes/md/mditem/list') return { rows: ITEMS, total: ITEMS.length }
    if (url === '/mes/qc/iqc/list') return { rows: iqc, total: iqc.length }
    if (url === '/mes/qc/iqcline/list') return { rows: [], total: 0 }
    if (url === '/mes/qc/templateproduct/list') return { rows: [{ recordId: 9862, templateId: 9821, itemId: 9205, quantityCheck: 1, quantityUnqualified: 0, crRate: 0, majRate: 0, minRate: 100 }], total: 1 }
    if (url === '/mes/qc/qctemplate/list') return { rows: [{ templateId: 9821, templateCode: 'TG-TPL-IQC', templateName: '外购物料来料检验方案', enableFlag: 'Y' }], total: 1 }
    if (url === '/mes/qc/templateindex/list') return { rows: indexes, total: indexes.length }
    if (url === '/mes/wm/rtvendor/list') return { rows: RTV, total: RTV.length }
    throw new Error('Unexpected API: ' + url)
  }
  const source = fs.readFileSync('ktg-mes-ui/src/views/mes/ai/index.vue', 'utf8')
  const code = source.match(/<script>([\s\S]*?)<\/script>/)[1].replace(/^import .*$/mg, '').replace('export default', 'component =')
  const box = { ...answerRules(), component: null, request, Date, console, setTimeout: () => 1, clearTimeout() {},
    sessionStorage: { setItem() {}, getItem() { return null }, removeItem() {} } }
  vm.createContext(box)
  vm.runInContext(code, box)
  const ctx = { ...box.component.data(), ...box.component.methods, answers: [],
    lastStock: null, lastWorkorder: null, waitingWeatherCity: false,
    $auth: { hasPermi: permission => !denied.includes(permission) }, $store: { state: { user: { id: 7 } } }, $refs: {},
    push(message) { this.answers.push(message) } }
  return { ctx, calls }
}

test('问检验项目时按质检方案列出检测项，只读接口且不需要模型分析', async () => {
  const { ctx, calls } = chat()
  await ctx.route('红外传感器来料检验需要检查什么？')
  const text = ctx.answers.find(m => m.type === 'text')
  const table = ctx.answers.find(m => m.type === 'list')
  assert.ok(text, '应当直接给出文字结论')
  assert.match(text.text, /外购物料来料检验方案/)
  assert.match(text.text, /共配置 2 项检验/)
  assert.ok(table, '应当给出检测项表格')
  assert.equal(table.rows.length, 2)
  assert.equal(table.rows[0].indexName, '传感器响应')
  assert.equal(table.rows[0].standard, '1 ~ 100 mV')
  assert.equal(table.rows[1].remark, '判定标准：无破损、无缺件')
  assert.ok(calls.every(config => (config.method || 'get') === 'get'), '只允许读取')
})

test('物料名不完全匹配时按词重叠命中，不会用全部物料顶替', async () => {
  const { ctx } = chat()
  await ctx.route('红外传感器来料检验要检查哪些项目？')
  assert.ok(!ctx.answers.some(m => m.type === 'ask'), '不应当反问')
  assert.match(ctx.answers.find(m => m.type === 'text').text, /红外传感器模块/)
})

test('物料匹配不到时给文字说明而不是空选项', async () => {
  const { ctx } = chat()
  await ctx.route('蓝色外壳来料检验要检查什么？')
  const answer = ctx.answers[0]
  assert.equal(answer.type, 'text')
  assert.match(answer.text, /没有找到/)
})

test('问不合格物料如何处置时给出真实路径和当前数量', async () => {
  const { ctx, calls } = chat()
  await ctx.route('不合格物料应该如何处理？')
  const text = ctx.answers.find(m => m.type === 'text')
  assert.match(text.text, /来料不合格退供/)
  assert.match(text.text, /累计 2 件不合格/)
  assert.match(text.text, /待执行的退供单 1 张（未入库 1 张）/)
  assert.match(text.text, /杂项出库/)
  const table = ctx.answers.find(m => m.type === 'list')
  assert.equal(table.rows.length, 1)
  assert.ok(calls.every(config => (config.method || 'get') === 'get'))
})

test('缺少来料检验权限时说明未读取，不假装没有不合格', async () => {
  const { ctx, calls } = chat({ denied: ['mes:qc:iqc:list'] })
  await ctx.route('不合格物料应该如何处理？')
  const text = ctx.answers.find(m => m.type === 'text')
  assert.match(text.text, /没有来料检验查看权限/)
  assert.ok(!calls.some(config => config.url === '/mes/qc/iqc/list'))
})

test('合格率等普通检验问题仍走原查询，不被业务规则劫持', async () => {
  const { ctx } = chat()
  let inspection = 0
  ctx.queryInspection = async () => { inspection++ }
  await ctx.route('本月检验合格率是多少')
  assert.equal(inspection, 1)
  assert.equal(ctx.answers.length, 0)
})

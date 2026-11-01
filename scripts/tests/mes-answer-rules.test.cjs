const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')

function engine() {
  const file = 'ktg-mes-ui/src/utils/mesAnswerRules.js'
  assert.ok(fs.existsSync(file), '需要实现业务规则问答模块')
  const box = { exports: {} }
  vm.createContext(box)
  vm.runInContext(fs.readFileSync(file, 'utf8').replace(/export (const|function) /g, '$1 ') +
    '\nexports={detectMesTopic,indexStandard,inspectionIndexRows,INSPECTION_COLUMNS,buildInspectionAnswer,buildUnqualifiedAnswer,knowledgeFallback};', box)
  return box.exports
}

const item = { itemId: 9205, itemCode: 'TG-EL-002', itemName: '红外传感器模块' }
const template = { templateId: 9821, templateCode: 'TG-TPL-IQC', templateName: '外购物料来料检验方案' }
const product = { templateId: 9821, itemId: 9205, quantityCheck: 5, quantityUnqualified: 1, crRate: 0, majRate: 1, minRate: 2.5 }
const indexes = [
  { indexCode: 'TG-QI-03', indexName: 'LCD 显示', checkMethod: '外观检查', qcTool: '目视', remark: '判定标准：无破损、无缺件' },
  { indexCode: 'TG-QI-02', indexName: '引脚间距', checkMethod: '卡尺测量', standerVal: 2.54, thresholdMin: 2.5, thresholdMax: 2.6, unitOfMeasure: 'mm', qcTool: '游标卡尺' }
]

test('流程类问题能识别主题，普通查询不会被劫持', () => {
  const api = engine()
  assert.equal(api.detectMesTopic('不合格物料应该如何处理？'), 'unqualified')
  assert.equal(api.detectMesTopic('红外传感器来料检验需要检查什么？'), 'inspection-items')
  assert.equal(api.detectMesTopic('IQC 需要检查哪些项目'), 'inspection-items')
  assert.equal(api.detectMesTopic('本月检验合格率是多少'), '')
  assert.equal(api.detectMesTopic('有哪些检验单'), '')
  assert.equal(api.detectMesTopic('盘点红外传感器的库存'), '')
})

test('检测项的合格范围优先用上下限，其次标准值', () => {
  const api = engine()
  assert.equal(api.indexStandard({ thresholdMin: 2.5, thresholdMax: 2.6, unitOfMeasure: 'mm' }), '2.5 ~ 2.6 mm')
  assert.equal(api.indexStandard({ standerVal: 25, unitOfMeasure: '℃' }), '25 ℃')
  assert.equal(api.indexStandard({}), '—')
})

test('已配置质检方案时按方案列出检测项与判定口径', () => {
  const api = engine()
  const answer = api.buildInspectionAnswer({ item, template, product, indexes })
  assert.match(answer.text, /外购物料来料检验方案/)
  assert.match(answer.text, /共配置 2 项检验/)
  assert.match(answer.text, /缺省检测数 5/)
  assert.match(answer.text, /CR\/MAJ\/MIN 允收水平 0% \/ 1% \/ 2.5%/)
  assert.match(answer.text, /合格量大于 0 判 ACCEPT/)
  assert.equal(answer.rows.length, 2)
  assert.equal(answer.rows[1].standard, '2.5 ~ 2.6 mm')
  assert.equal(answer.rows[1].qcTool, '游标卡尺')
  assert.equal(answer.rows[0].remark, '判定标准：无破损、无缺件')
  assert.equal(answer.columns, api.INSPECTION_COLUMNS)
})

test('未配置方案但检验单有记录时引用最近一张单，不编造项目', () => {
  const api = engine()
  const answer = api.buildInspectionAnswer({
    item,
    lastIqc: { iqcId: 9, iqcCode: 'IQC-0009', status: 'FINISHED' },
    lastIqcLines: [{ indexCode: 'IDX-09', indexName: '引脚共面度', checkMethod: '塞尺' }]
  })
  assert.match(answer.text, /IQC-0009/)
  assert.match(answer.text, /还没有配置质检方案指标/)
  assert.equal(answer.rows.length, 1)
})

test('方案和检验记录都没有时说明系统实际记录口径', () => {
  const api = engine()
  const answer = api.buildInspectionAnswer({ item })
  assert.match(answer.text, /没有维护质检方案和检测项/)
  assert.match(answer.text, /只记录检测数量/)
  assert.equal(answer.rows.length, 0)
  assert.equal(answer.title, '')
})

test('不合格处理回答按发现环节给出真实路径与当前数量', () => {
  const api = engine()
  const answer = api.buildUnqualifiedAnswer({
    iqcRows: [
      { iqcCode: 'IQC-1', itemName: '红外温度传感器', quantityUnqualified: 3, checkResult: 'REJECT', vendorName: '甲供应商', inspectDate: '2026-10-30 09:00:00', status: 'FINISHED' },
      { iqcCode: 'IQC-2', itemName: '上壳', quantityUnqualified: 0, status: 'FINISHED' },
      { iqcCode: 'IQC-3', itemName: '电池', quantityUnqualified: 2, checkResult: 'ACCEPT', status: 'FINISHED' }
    ],
    returnRows: [
      { rtCode: 'RTV-1', status: 'PREPARE', attr2: 'PRE_STOCK' },
      { rtCode: 'RTV-2', status: 'FINISHED', attr2: 'PRE_STOCK' },
      { rtCode: 'RTV-3', status: 'PREPARE', attr2: 'STOCK' }
    ]
  })
  assert.match(answer.text, /来料检验判不合格且尚未入库/)
  assert.match(answer.text, /累计 5 件不合格/)
  assert.match(answer.text, /其中 1 张整批不合格（合格量为 0）/)
  assert.match(answer.text, /待执行的退供单 2 张（未入库 1 张）/)
  assert.match(answer.text, /历史已执行退供 1 张/)
  assert.match(answer.text, /没有单独的「报废单」/)
  assert.equal(answer.rows.length, 2)
  assert.equal(answer.rows[0].iqcCode, 'IQC-1')
  assert.equal(answer.rows[0].quantityUnqualified, '3')
})

test('缺少读取权限时明说没读到，不谎报“没有不合格”', () => {
  const api = engine()
  const answer = api.buildUnqualifiedAnswer({ iqcRows: [], returnRows: [], iqcRowsRead: false, returnRowsRead: false })
  assert.match(answer.text, /没有来料检验查看权限/)
  assert.match(answer.text, /没有采购退供查看权限/)
  assert.doesNotMatch(answer.text, /当前没有已完成且不合格数量大于 0/)
})

test('知识库未命中时的兜底回答仍给出系统真实流程', () => {
  const api = engine()
  assert.match(api.knowledgeFallback('unqualified'), /采购退货/)
  assert.match(api.knowledgeFallback('inspection-items'), /质检方案/)
  assert.equal(api.knowledgeFallback(''), '')
})

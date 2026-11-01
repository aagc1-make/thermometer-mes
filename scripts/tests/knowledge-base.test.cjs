const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')

function engine() {
  const file = 'ktg-mes-ui/src/utils/knowledgeBase.js'
  assert.ok(fs.existsSync(file), '需要实现浏览器知识文档和检索模块')
  const box = { exports: {} }
  vm.createContext(box)
  vm.runInContext(fs.readFileSync(file, 'utf8').replace(/export (const|function) /g, '$1 ') +
    '\nexports={createKnowledgeDocument,addDocumentVersion,setDocumentActive,searchKnowledge,chunkSections};', box)
  return box.exports
}

function document(api, overrides = {}) {
  return api.createKnowledgeDocument({ id: 'doc-1', title: '来料检验规范', category: '检验规范',
    version: 'V1', filename: 'iqc.md', createdAt: '2026-11-01T00:00:00Z',
    sections: [{ label: '第 1 段', text: '红外传感器来料检验：检查外观与引脚，不合格物料隔离并登记缺陷。' },
      { label: '第 2 段', text: '传感器检测合格后，由仓管按合格数量办理采购入库。' }], ...overrides })
}

test('中文知识问题返回原文及准确的文件、版本、段落引用', () => {
  const api = engine(), doc = document(api)
  const hits = api.searchKnowledge([doc], '红外传感器来料检验需要检查什么？')
  assert.ok(hits.length > 0)
  assert.equal(hits[0].documentId, 'doc-1')
  assert.equal(hits[0].version, 'V1')
  assert.equal(hits[0].sectionIndex, 0)
  assert.ok(hits[0].excerpt.includes('检查外观与引脚'))
})

test('无关问题和停用文档不产生伪造的知识答案', () => {
  const api = engine(), doc = document(api)
  assert.equal(api.searchKnowledge([doc], '北京明天天气怎么样').length, 0)
  assert.equal(api.searchKnowledge([{ ...doc, active: false }], '来料检验').length, 0)
})

test('IQC缩写可检索来料检验资料，原文保持不变', () => {
  const api = engine(), doc = document(api)
  const hits = api.searchKnowledge([doc], 'IQC 的要求是什么')
  assert.ok(hits.some(hit => hit.excerpt.includes('来料检验')))
})

test('长段落分片保留来源段落，覆盖结尾内容且不超过片段上限', () => {
  const api = engine(), text = '安装工艺说明。'.repeat(180) + '末尾校准要求'
  const chunks = api.chunkSections([{ label: '第 8 页', text }])
  assert.ok(chunks.length > 1)
  assert.ok(chunks.every(c => c.text.length <= 480 && c.sectionIndex === 0 && c.sectionLabel === '第 8 页'))
  assert.ok(chunks.at(-1).text.includes('末尾校准要求'))
})

test('新版本替代旧版，同版本重复导入不能改变已有启用状态', () => {
  const api = engine(), first = document(api)
  const second = document(api, { id: 'doc-2', version: 'V2', sections: [{ label: '第 1 段', text: '新版来料检验必须检查引脚间距。' }] })
  const versions = api.addDocumentVersion([first], second)
  assert.equal(versions.find(d => d.id === 'doc-1').active, false)
  assert.equal(versions.find(d => d.id === 'doc-2').active, true)
  assert.equal(api.searchKnowledge(versions, '来料检验')[0].version, 'V2')
  assert.throws(() => api.addDocumentVersion(versions, document(api, { id: 'doc-3', version: 'V2' })), /版本.*存在/)
  assert.equal(versions.find(d => d.id === 'doc-2').active, true)
})

test('切换旧版本时同资料仅启用一个版本，停用后不自动回退', () => {
  const api = engine()
  const versions = api.addDocumentVersion([document(api)], document(api, { id: 'doc-2', version: 'V2' }))
  const switched = api.setDocumentActive(versions, 'doc-1', true)
  assert.equal(switched.find(d => d.id === 'doc-1').active, true)
  assert.equal(switched.find(d => d.id === 'doc-2').active, false)
  assert.equal(api.setDocumentActive(switched, 'doc-1', false).filter(d => d.active).length, 0)
})

test('空白、超长文本和同名ID覆盖被拒绝', () => {
  const api = engine()
  assert.throws(() => document(api, { sections: [{ text: '   ' }] }), /文本|内容/)
  assert.throws(() => document(api, { sections: [{ text: '甲'.repeat(300001) }] }), /30万|300000/)
  assert.throws(() => api.addDocumentVersion([document(api)], document(api, { version: 'V2' })), /标识|ID/)
})

test('可直接引用含HTML的原文，但不会把它改成可执行标记', () => {
  const api = engine(), text = '工艺规范：<script>alert(1)</script> 只是文档原文，不执行。'
  const doc = document(api, { sections: [{ label: '第 1 段', text }] })
  assert.equal(api.searchKnowledge([doc], '工艺规范')[0].excerpt, text)
})

test('文档数量和总正文限额在替换旧版本启用状态之前检查', () => {
  const api = engine(), incoming = document(api, { id: 'new', version: 'V2' })
  const full = Array.from({ length: 40 }, (_, index) => document(api, { id: 'doc-' + index, title: '资料' + index }))
  assert.throws(() => api.addDocumentVersion(full, incoming), /40/)
  const first = document(api), large = [first, { ...first, id: 'large', title: '大文档', chars: 1000000 }]
  assert.throws(() => api.addDocumentVersion(large, incoming), /100万/)
  assert.equal(first.active, true)
})

test('提问检验项目时正文要求排在只有标题的片段前面', () => {
  const api = engine(), doc = document(api, { title: '验收-来料检验规范', sections: [
    { label: '第 1 段', text: '# 红外传感器来料检验规范' },
    { label: '第 2 段', text: '来料检验需要检查传感器外观和引脚，不合格物料隔离并登记缺陷。' }
  ] })
  assert.equal(api.searchKnowledge([doc], '红外传感器来料检验需要检查什么？')[0].sectionIndex, 1)
})

test('检验问题不因仅共享“传感器”而混入装配工艺引用', () => {
  const api = engine(), inspection = document(api), assembly = document(api, { id: 'assembly', title: '装配工艺',
    sections: [{ text: '装配时应核对电池安装方向，保持传感器光学窗口清洁。' }] })
  const hits = api.searchKnowledge([inspection, assembly], '红外传感器来料检验需要检查什么？')
  assert.ok(hits.length > 0)
  assert.ok(hits.every(hit => hit.documentId === 'doc-1'))
})

test('不合格处置问题不引用只谈传感器调试、没有处置口径的资料', () => {
  const api = engine()
  const report = document(api, { id: 'report', title: '设备端与通信', sections: [
    { text: '温度驱动先读取传感器寄存器原始值，再按器件数据格式换算为摄氏度。每次读取都检查通信返回状态，失败时不更新当前有效温度。' },
    { text: '异常处理不设置成一个笼统的错误状态。传感器无响应时提示检查测温模块，Wi-Fi 未连接时只显示离线标志。' }
  ] })
  assert.equal(api.searchKnowledge([report], '不合格物料应该如何处理？').length, 0)
  const inspection = document(api)
  const hits = api.searchKnowledge([inspection, report], '不合格物料应该如何处理？')
  assert.ok(hits.length > 0)
  assert.ok(hits.every(hit => hit.documentId === 'doc-1'))
})

test('用“检测”表述的规范仍能回答检验项目问题', () => {
  const api = engine()
  const spec = document(api, { id: 'spec', title: '来料检测作业指导书',
    sections: [{ text: '来料检测按抽检方案执行，检测结果记录在来料检验单上。' }] })
  assert.ok(api.searchKnowledge([spec], '来料检验需要检查哪些项目').length > 0)
})

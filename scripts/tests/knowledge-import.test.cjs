const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')

function importer() {
  const file = 'ktg-mes-ui/src/utils/knowledgeImport.js'
  assert.ok(fs.existsSync(file), '需要实现前端文档提取')
  const box = { exports: {}, TextDecoder, TextEncoder, Uint8Array, DataView, Blob, Response,
    KNOWLEDGE_LIMITS: { fileBytes: 5 * 1024 * 1024, documentChars: 300000 } }
  vm.createContext(box)
  vm.runInContext(fs.readFileSync(file, 'utf8').replace(/^import .*$/mg, '').replace(/export (async function|function|const) /g, '$1 ') +
    '\nexports={readKnowledgeFile,sectionsFromText};', box)
  return box.exports
}
const file = (name, bytes, size = bytes.length) => ({ name, size, arrayBuffer: async () => bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength) })

test('前端读取UTF-8 Markdown并保留可引用段落', async () => {
  const api = importer()
  const sections = await api.readKnowledgeFile(file('检验规范.md', new TextEncoder().encode('# 来料检验\n\n检查红外传感器外观。')))
  assert.equal(sections.length, 2)
  assert.equal(sections[1].text, '检查红外传感器外观。')
})

test('UTF-16 BOM文本可读取，正文不会产生空字符', async () => {
  const api = importer(), bytes = Buffer.from('\ufeff安装方向必须一致。', 'utf16le')
  const sections = await api.readKnowledgeFile(file('工艺.txt', bytes))
  assert.equal(sections[0].text, '安装方向必须一致。')
})

test('过大、空白、旧DOC和损坏DOCX拒绝导入，不保存空文档', async () => {
  const api = importer(), bytes = new TextEncoder().encode('正文')
  await assert.rejects(() => api.readKnowledgeFile(file('a.txt', bytes, 6 * 1024 * 1024)), /5.*MiB|5.*MB/)
  await assert.rejects(() => api.readKnowledgeFile(file('a.doc', bytes)), /不支持/)
  await assert.rejects(() => api.readKnowledgeFile(file('a.docx', bytes)), /损坏|DOCX|格式/)
  await assert.rejects(() => api.readKnowledgeFile(file('a.txt', new TextEncoder().encode('  \n '))), /文本|内容/)
})

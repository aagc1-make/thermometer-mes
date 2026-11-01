export const KNOWLEDGE_LIMITS = { fileBytes: 5 * 1024 * 1024, documentChars: 300000, totalChars: 1000000, documents: 40 }

const clean = value => String(value == null ? '' : value).replace(/\r\n?/g, '\n').replace(/\u0000/g, '').trim()
const groupKey = doc => `${clean(doc.category).toLowerCase()}|${clean(doc.title).toLowerCase()}`

export function createKnowledgeDocument(input) {
  const title = clean(input.title); const version = clean(input.version); const category = clean(input.category) || '其他'
  if (!title || title.length > 120) throw new Error('文档名称必填，最多120个字符')
  if (!version || version.length > 40) throw new Error('版本号必填，最多40个字符')
  const sections = (input.sections || []).map((section, index) => ({
    label: clean(section.label) || `第 ${index + 1} 段`, text: clean(section.text)
  })).filter(section => section.text)
  const chars = sections.reduce((sum, section) => sum + section.text.length, 0)
  if (!chars) throw new Error('没有可导入的文本内容')
  if (chars > KNOWLEDGE_LIMITS.documentChars) throw new Error('单份文档正文不能超过30万字符')
  const id = input.id || (typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : `kb-${Date.now()}-${Math.random().toString(36).slice(2)}`)
  return { id, title, version, category, filename: clean(input.filename) || `${title}.txt`,
    createdAt: input.createdAt || new Date().toISOString(), chars, sections, active: true }
}

export function addDocumentVersion(documents, document) {
  if (documents.some(doc => doc.id === document.id)) throw new Error('文档标识已存在，不能覆盖原文')
  if (documents.some(doc => groupKey(doc) === groupKey(document) && doc.version.toLowerCase() === document.version.toLowerCase())) {
    throw new Error('该文档版本已存在，请填写新的版本号')
  }
  if (documents.length >= KNOWLEDGE_LIMITS.documents) throw new Error('最多保存40个文档版本，请先删除不用的版本')
  if (documents.reduce((sum, doc) => sum + doc.chars, 0) + document.chars > KNOWLEDGE_LIMITS.totalChars) {
    throw new Error('知识库正文总量不能超过100万字符，请先删除不用的版本')
  }
  return documents.map(doc => groupKey(doc) === groupKey(document) ? { ...doc, active: false } : doc).concat({ ...document, active: true })
}

export function setDocumentActive(documents, id, active) {
  const selected = documents.find(doc => doc.id === id)
  if (!selected) throw new Error('文档版本已不存在，请刷新列表')
  return documents.map(doc => {
    if (doc.id === id) return { ...doc, active: !!active }
    return active && groupKey(doc) === groupKey(selected) ? { ...doc, active: false } : doc
  })
}

export function chunkSections(sections) {
  const chunks = []
  sections.forEach((section, sectionIndex) => {
    const text = clean(section.text)
    let start = 0
    while (start < text.length) {
      const end = Math.min(start + 480, text.length)
      chunks.push({ sectionIndex, sectionLabel: section.label || `第 ${sectionIndex + 1} 段`, text: text.slice(start, end) })
      if (end === text.length) break
      start = end - 60
    }
  })
  return chunks
}

function queryTerms(question) {
  let text = clean(question).toLowerCase().replace(/请问|帮我|一下|如何|怎么|什么|哪些|是否|根据|知识库|文档|资料|要求|规定|应该|需要|进行|是什么|怎么样/g, ' ')
  const synonyms = [['iqc', '来料检验'], ['ipqc', '过程检验'], ['oqc', '出货检验'], ['bom', '物料清单']]
  synonyms.forEach(([code, name]) => { if (text.includes(code) || text.includes(name)) text += ` ${code} ${name}` })
  const terms = []
  ;(text.match(/[a-z0-9]+(?:[-_.][a-z0-9]+)*|[\u4e00-\u9fff]+/g) || []).forEach(word => {
    if (/^[a-z0-9]/.test(word)) { if (word.length >= 2) terms.push(word); return }
    for (let index = 0; index < word.length - 1; index++) terms.push(word.slice(index, index + 2))
  })
  return [...new Set(terms)]
}

/**
 * 问题所属主题的锚点词。
 *
 * 只共享「传感器」这类实体词，并不代表这段话在回答「检验要查什么」或「不合格怎么处理」。
 * 资料必须真正谈到该主题（出现任一锚点词）才允许作为引用，避免把设备报告当成检验规范。
 * 宁可少给一条引用，也不给答非所问的引用——业务规则问题会由助手的业务回答兜底。
 */
const TOPIC_ANCHORS = [
  { re: /(?:检验|检测|验收|抽检|iqc|ipqc|oqc)/i, anchors: ['检验', '检测', '验收', '抽检', '判定', '允收', 'aql', '缺陷', '抽样', '不良'] },
  { re: /(?:不合格|不良品|不良|退供|退货)/, anchors: ['不合格', '不良', '退供', '退货', '让步', '报废', '返工', '返修', '隔离', '处置'] }
]

function topicAnchors(question) {
  const text = clean(question).toLowerCase()
  const anchors = []
  TOPIC_ANCHORS.forEach(rule => { if (rule.re.test(text)) anchors.push.apply(anchors, rule.anchors) })
  return [...new Set(anchors)]
}

export function searchKnowledge(documents, question, limit = 3) {
  const terms = queryTerms(question)
  if (!terms.length) return []
  const anchors = topicAnchors(question)
  const candidates = []
  documents.filter(doc => doc.active).forEach(doc => {
    chunkSections(doc.sections).forEach(chunk => candidates.push({ doc, chunk, body: chunk.text.toLowerCase() }))
  })
  const frequency = new Map(terms.map(term => [term, candidates.filter(candidate => candidate.body.includes(term)).length]))
  const scored = candidates.map(({ doc, chunk, body }) => {
    if (anchors.length && !anchors.some(anchor => body.includes(anchor))) return null
    const matched = terms.filter(term => body.includes(term))
    if (!matched.length) return null
    const preciseCode = matched.some(term => /^[a-z0-9]/.test(term))
    if (!preciseCode && matched.length < Math.min(3, Math.ceil(terms.length * 0.3))) return null
    const headingOnly = /^#{1,6}\s+[^\n]+$/.test(chunk.text.trim()) || chunk.text.trim() === doc.title
    const score = (matched.reduce((sum, term) => sum + Math.log(2 + candidates.length / (1 + frequency.get(term))) * (/^[a-z]/.test(term) ? 1.6 : 1), 0) +
      terms.filter(term => doc.title.toLowerCase().includes(term)).length * 0.25) * (headingOnly ? 0.25 : 1)
    return { documentId: doc.id, title: doc.title, version: doc.version, filename: doc.filename, category: doc.category,
      sectionIndex: chunk.sectionIndex, sectionLabel: chunk.sectionLabel, excerpt: chunk.text, score }
  }).filter(Boolean).sort((a, b) => b.score - a.score)
  const seen = new Set(); const results = []
  for (const result of scored) {
    if (result.score < scored[0].score * 0.45) continue
    const key = `${result.documentId}:${result.sectionIndex}`
    if (seen.has(key)) continue
    seen.add(key)
    results.push(result)
    if (results.length >= limit) break
  }
  return results
}

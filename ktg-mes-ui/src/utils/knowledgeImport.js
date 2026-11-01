import { KNOWLEDGE_LIMITS } from './knowledgeBase'

let pdfLibrary

export function sectionsFromText(value) {
  const text = String(value || '').replace(/^\uFEFF/, '').replace(/\r\n?/g, '\n').trim()
  if (!text) throw new Error('文档没有可读取的文本内容')
  if (text.length > KNOWLEDGE_LIMITS.documentChars) throw new Error('单份文档正文不能超过30万字符')
  return text.split(/\n\s*\n/).map(part => part.trim()).filter(Boolean).map((part, index) => ({ label: `第 ${index + 1} 段`, text: part }))
}

function decodeText(bytes) {
  if (bytes[0] === 0xff && bytes[1] === 0xfe) return new TextDecoder('utf-16le').decode(bytes)
  if (bytes[0] === 0xfe && bytes[1] === 0xff) return new TextDecoder('utf-16be').decode(bytes)
  try { return new TextDecoder('utf-8', { fatal: true }).decode(bytes) } catch (error) { return new TextDecoder('gb18030', { fatal: true }).decode(bytes) }
}

function loadPdfLibrary() {
  const base = (process.env.BASE_URL || '/').replace(/\/?$/, '/')
  if (!pdfLibrary) {
    pdfLibrary = new Promise((resolve, reject) => {
      if (window.pdfjsLib) { resolve(window.pdfjsLib); return }
      const script = document.createElement('script')
      script.src = `${base}pdf/build/pdf.js`
      script.onload = () => window.pdfjsLib ? resolve(window.pdfjsLib) : reject(new Error('PDF读取组件未能加载'))
      script.onerror = () => { script.remove(); reject(new Error('PDF读取组件加载失败，请刷新后重试')) }
      document.head.appendChild(script)
    }).then(library => {
      library.GlobalWorkerOptions.workerSrc = `${base}pdf/build/pdf.worker.js`
      return library
    }).catch(error => { pdfLibrary = null; throw error })
  }
  return pdfLibrary
}

async function readPdf(bytes) {
  const library = await loadPdfLibrary()
  const base = (process.env.BASE_URL || '/').replace(/\/?$/, '/')
  const task = library.getDocument({ data: bytes, isEvalSupported: false, disableFontFace: true,
    cMapUrl: `${base}pdf/web/cmaps/`, cMapPacked: true, standardFontDataUrl: `${base}pdf/web/standard_fonts/` })
  try {
    const pdf = await task.promise; const sections = []
    if (pdf.numPages > 200) throw new Error('PDF最多支持200页，请拆分后导入')
    let chars = 0
    for (let pageNumber = 1; pageNumber <= pdf.numPages; pageNumber++) {
      const page = await pdf.getPage(pageNumber); const content = await page.getTextContent()
      const text = content.items.map(item => item.str + (item.hasEOL ? '\n' : ' ')).join('').replace(/([\u4e00-\u9fff])\s+(?=[\u4e00-\u9fff])/g, '$1').trim()
      chars += text.length
      if (chars > KNOWLEDGE_LIMITS.documentChars) throw new Error('单份文档正文不能超过30万字符')
      if (text) sections.push({ label: `第 ${pageNumber} 页`, text })
      page.cleanup()
    }
    if (!sections.length) throw new Error('PDF没有可提取的文字；扫描件请先识别文字，或改用粘贴文本')
    return sections
  } finally { await task.destroy() }
}

async function boundedInflate(bytes, expectedSize) {
  if (typeof DecompressionStream === 'undefined') throw new Error('当前浏览器不支持DOCX读取，请更新浏览器或粘贴文档正文')
  const reader = new Blob([bytes]).stream().pipeThrough(new window.DecompressionStream('deflate-raw')).getReader()
  const parts = []
  let size = 0
  try {
    let next = await reader.read()
    while (!next.done) {
      const value = next.value
      size += value.length
      if (size > 2 * 1024 * 1024 || size > expectedSize) throw new Error('DOCX正文解压大小异常，请拆分文档')
      parts.push(value)
      next = await reader.read()
    }
  } catch (error) { await reader.cancel().catch(() => {}); throw error } finally { reader.releaseLock() }
  if (size !== expectedSize) throw new Error('DOCX正文已损坏或不完整')
  const result = new Uint8Array(size)
  let offset = 0
  parts.forEach(part => { result.set(part, offset); offset += part.length })
  return result
}

// Read only word/document.xml from a bounded ZIP directory. No macros, HTML or embedded objects execute.
async function readDocx(bytes) {
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength)
  let end = -1
  for (let offset = bytes.length - 22; offset >= Math.max(0, bytes.length - 65557); offset--) {
    if (view.getUint32(offset, true) === 0x06054b50) { end = offset; break }
  }
  if (end < 0) throw new Error('DOCX格式损坏，请重新保存为DOCX后导入')
  const count = view.getUint16(end + 10, true)
  let offset = view.getUint32(end + 16, true); let entry
  if (count > 2000) throw new Error('DOCX内容过多，请拆分文档')
  for (let index = 0; index < count; index++) {
    if (offset + 46 > bytes.length || view.getUint32(offset, true) !== 0x02014b50) throw new Error('DOCX目录已损坏')
    const length = view.getUint16(offset + 28, true); const extra = view.getUint16(offset + 30, true); const comment = view.getUint16(offset + 32, true)
    if (offset + 46 + length + extra + comment > bytes.length) throw new Error('DOCX目录不完整')
    const name = new TextDecoder().decode(bytes.slice(offset + 46, offset + 46 + length))
    if (name === 'word/document.xml') {
      entry = { method: view.getUint16(offset + 10, true), flags: view.getUint16(offset + 8, true),
        size: view.getUint32(offset + 24, true), compressed: view.getUint32(offset + 20, true), offset: view.getUint32(offset + 42, true) }
      break
    }
    offset += 46 + length + extra + comment
  }
  if (!entry || entry.flags & 1 || entry.size > 2 * 1024 * 1024) throw new Error('DOCX正文缺失、加密或过大，请改用粘贴文本')
  const start = entry.offset
  if (start + 30 > bytes.length || view.getUint32(start, true) !== 0x04034b50) throw new Error('DOCX正文位置无效')
  const dataStart = start + 30 + view.getUint16(start + 26, true) + view.getUint16(start + 28, true)
  if (dataStart + entry.compressed > bytes.length) throw new Error('DOCX正文不完整')
  const compressed = bytes.slice(dataStart, dataStart + entry.compressed)
  const xmlBytes = entry.method === 0 ? compressed : entry.method === 8 ? await boundedInflate(compressed, entry.size) : null
  if (!xmlBytes || xmlBytes.length !== entry.size) throw new Error('不支持此DOCX压缩格式')
  const xml = new DOMParser().parseFromString(new TextDecoder('utf-8', { fatal: true }).decode(xmlBytes), 'application/xml')
  if (xml.getElementsByTagName('parsererror').length) throw new Error('DOCX正文XML已损坏')
  const sections = Array.from(xml.getElementsByTagNameNS('*', 'p')).map(node => {
    const read = part => part.nodeType === 1 && part.localName === 't' ? part.textContent : part.nodeType === 1 && part.localName === 'tab' ? '\t' : part.nodeType === 1 && part.localName === 'br' ? '\n' : Array.from(part.childNodes).map(read).join('')
    return read(node).trim()
  }).filter(Boolean).map((text, index) => ({ label: `第 ${index + 1} 段`, text }))
  if (!sections.length) throw new Error('DOCX没有可提取的正文文字')
  if (sections.reduce((sum, section) => sum + section.text.length, 0) > KNOWLEDGE_LIMITS.documentChars) throw new Error('单份文档正文不能超过30万字符')
  return sections
}

export async function readKnowledgeFile(file) {
  if (!file || !file.size) throw new Error('请选择有内容的文档')
  if (file.size > KNOWLEDGE_LIMITS.fileBytes) throw new Error('单个文件不能超过5 MiB')
  const extension = String(file.name || '').split('.').pop().toLowerCase()
  if (!['txt', 'md', 'markdown', 'csv', 'pdf', 'docx'].includes(extension)) throw new Error('不支持该格式，请导入TXT、Markdown、CSV、PDF或DOCX')
  const bytes = new Uint8Array(await file.arrayBuffer())
  if (extension === 'pdf') return readPdf(bytes)
  if (extension === 'docx') return readDocx(bytes)
  const text = decodeText(bytes)
  if (/[\u0000-\u0008\u000e-\u001f]/.test(text)) throw new Error('文件不是可读取的文本，请检查格式或改用粘贴文本')
  return sectionsFromText(text)
}

import { addDocumentVersion, setDocumentActive } from './knowledgeBase'

export function createKnowledgeStore(ownerId) {
  const owner = String(ownerId == null ? '' : ownerId)
  if (!owner) throw new Error('请登录后使用本地知识库')
  const name = `mes-ai-knowledge-v1-${encodeURIComponent(owner)}`

  async function open() {
    if (typeof indexedDB === 'undefined') throw new Error('当前浏览器不支持本地知识库存储')
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(name, 1)
      request.onupgradeneeded = () => request.result.createObjectStore('documents', { keyPath: 'id' })
      request.onerror = () => reject(new Error('无法打开本地知识库，请检查浏览器存储设置'))
      request.onblocked = () => reject(new Error('知识库存储升级被其它标签页阻塞，请关闭旧标签页后重试'))
      request.onsuccess = () => { request.result.onversionchange = () => request.result.close(); resolve(request.result) }
    })
  }

  async function transaction(mode, mutate) {
    const database = await open()
    return new Promise((resolve, reject) => {
      const tx = database.transaction('documents', mode); const store = tx.objectStore('documents')
      let result, failure
      tx.oncomplete = () => { database.close(); resolve(result) }
      tx.onabort = () => { database.close(); reject(failure || new Error(tx.error && tx.error.name === 'QuotaExceededError' ? '浏览器存储空间不足，文档未保存' : '本地知识库保存失败，请重试')) }
      tx.onerror = () => {} // onabort reports transaction failures after rollback.
      const request = store.getAll()
      request.onsuccess = () => {
        try { result = mutate(request.result || [], store) } catch (error) { failure = error; tx.abort() }
      }
    })
  }

  return {
    list: () => transaction('readonly', rows => rows.sort((a, b) => b.createdAt.localeCompare(a.createdAt))),
    add: document => transaction('readwrite', (rows, store) => {
      const next = addDocumentVersion(rows, document)
      next.forEach(doc => store.put(doc))
      return next
    }),
    activate: (id, active) => transaction('readwrite', (rows, store) => {
      const next = setDocumentActive(rows, id, active)
      next.forEach(doc => store.put(doc))
      return next
    }),
    remove: id => transaction('readwrite', (rows, store) => {
      if (!rows.some(doc => doc.id === id)) throw new Error('文档版本已不存在，请刷新列表')
      store.delete(id)
      return rows.filter(doc => doc.id !== id)
    })
  }
}

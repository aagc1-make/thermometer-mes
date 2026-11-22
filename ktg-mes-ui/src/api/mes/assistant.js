import request from '@/utils/request'
import { getToken } from '@/utils/auth'

export function getEngineStatus() {
  return request({ url: '/mes/ai/status', method: 'get', timeout: 6500 })
}

export function askBusinessQuestion(data) {
  return request({ url: '/mes/ai/business/answer', method: 'post', data, timeout: 75000 })
}

export function queryPurchaseReceipts(data) {
  return request({ url: '/mes/ai/business/purchase-receipts', method: 'post', data })
}

export function listBusinessTools() {
  return request({ url: '/mes/ai/business/tools', method: 'get' })
}

export async function streamDocumentAnswer(libraryId, options, signal, receive) {
  const response = await fetch(`${process.env.VUE_APP_BASE_API}/mes/ai/kb/libraries/${libraryId}/answer`, {
    method: 'POST',
    headers: { Authorization: 'Bearer ' + getToken(), 'Content-Type': 'application/json', Accept: 'text/event-stream' },
    body: JSON.stringify(options),
    signal
  })
  if (!response.ok || !response.headers.get('content-type')?.includes('text/event-stream') || !response.body) {
    const error = await response.json().catch(() => ({}))
    throw new Error(error.msg || '文档问答暂不可用，请稍后重试。')
  }
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let completed = false
  try {
    while (true) {
      const { value, done } = await reader.read()
      buffer += decoder.decode(value, { stream: !done })
      const events = buffer.split(/\r?\n\r?\n/)
      buffer = events.pop()
      for (const frame of events) {
        const type = frame.match(/^event:\s*(.+)$/m)
        const data = frame.split(/\r?\n/).filter(line => line.startsWith('data:')).map(line => line.slice(5)).join('\n')
        if (type && data) {
          const { value: payload } = JSON.parse(data)
          if (type[1] === 'error') throw new Error(payload)
          if (type[1] === 'done') completed = true
          receive(type[1], payload)
        }
      }
      if (done) break
    }
    if (!completed) throw new Error('回答连接已中断。')
  } finally {
    await reader.cancel().catch(() => {})
    reader.releaseLock()
  }
}

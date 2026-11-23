import request from '@/utils/request'
import store from '@/store'
import { getToken } from '@/utils/auth'
const { createChainRunner } = require('./businessChainRunner')
const { createChainStages } = require('./businessChainFlow')

let session = null

// Keep the running job outside a page component so opening a document does not lose it.
export function getBusinessChainSession(hasPermission) {
  const userId = store.state.user.id
  const token = getToken()
  if (session && session.userId === userId && session.token === token) return session
  if (session) session.runner.stop()
  const listeners = new Set()
  const runner = createChainRunner({
    userId,
    storage: window.localStorage,
    delayMs: 450,
    stages: createChainStages({ hasPermission }),
    request: config => {
      if (store.state.user.id !== userId || getToken() !== token) throw new Error('登录会话已切换，请重新登录后核对原单据')
      return request(config)
    },
    withLock: async work => {
      if (!navigator.locks) throw new Error('当前浏览器不支持安全的执行锁，请使用新版 Chrome 或 Edge')
      return navigator.locks.request(`mes-business-chain:${userId}`, { ifAvailable: true }, lock => {
        if (!lock) throw new Error('其他标签页正在执行贯通，请回原页面操作')
        return work()
      })
    },
    onChange: state => listeners.forEach(fn => fn(state))
  })
  session = {
    userId, token, runner,
    subscribe(fn) { listeners.add(fn); fn(runner.state); return () => listeners.delete(fn) }
  }
  return session
}

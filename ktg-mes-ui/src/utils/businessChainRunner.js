// The runner owns control and checkpoints; business rules live in businessChainFlow.
const ACTIVE = ['running', 'pausing', 'paused', 'stopping']
const STOP = new Error('CHAIN_STOP')

/** 执行号取 yymmddHHMMSS，直接作为单据编号前缀，便于在业务页面按号段追溯。 */
function nextRunId() {
  const d = new Date()
  const pad = v => String(v).padStart(2, '0')
  return `${String(d.getFullYear()).slice(2)}${pad(d.getMonth() + 1)}${pad(d.getDate())}${pad(d.getHours())}${pad(d.getMinutes())}${pad(d.getSeconds())}`
}

function createChainRunner({ request, storage, userId, stages = [], onChange = () => {}, delayMs = 0, withLock = work => work() }) {
  const key = `mes-business-chain:v1:${userId}`
  const state = { version: 1, runId: '', status: 'idle', index: 0, current: '', error: '', pending: null, unconfirmed: null, context: {}, documents: [], events: [], steps: stages.map(s => ({ title: s.title, description: s.description, status: 'waiting' })), summary: null }
  let job = null
  let wake = null
  let paused = false
  let stopping = false
  function saved() { try { return JSON.parse(storage.getItem(key) || 'null') } catch (e) { return null } }
  function publish() {
    state.updatedAt = Date.now()
    // Storage failure must stop writes: without a checkpoint, a refresh could duplicate them.
    storage.setItem(key, JSON.stringify(state))
    onChange(state)
  }
  const previous = saved()
  if (previous && previous.version === 1 && previous.runId) {
    Object.assign(state, previous)
    if (ACTIVE.includes(state.status)) {
      if (state.pending && state.pending.writing) state.unconfirmed = state.pending
      state.status = 'interrupted'
      state.error = '上轮执行已中断，请核对原单据后继续；系统将先查询原单据状态。'
    }
  }
  async function gate() {
    if (stopping) throw STOP
    if (paused) {
      state.status = 'paused'; publish()
      await new Promise(resolve => { wake = resolve })
      wake = null
    }
    if (stopping) throw STOP
  }
  const ctx = {
    state,
    checkpoint() { publish() },
    confirmWrite(url, matches = () => true, method = 'post') {
      if (state.unconfirmed && state.unconfirmed.url === url && state.unconfirmed.method.toLowerCase() === method && matches(state.unconfirmed.data || {})) {
        state.unconfirmed = null
        publish()
      }
    },
    document(type, id, code, path, status) {
      const old = state.documents.find(d => d.type === type && String(d.id) === String(id))
      const doc = { type, id, code, path, status }
      if (old) Object.assign(old, doc); else state.documents.push(doc)
      publish()
    },
    async call(label, config) {
      await gate()
      state.current = label
      const writing = !['get', 'head'].includes((config.method || 'get').toLowerCase())
      if (writing && state.unconfirmed) throw new Error(`上次写入结果未确认：${state.unconfirmed.label}。请稍后核对原单据；不会自动重发。`)
      state.pending = { label, method: config.method || 'get', url: config.url, writing, data: writing ? config.data : undefined }
      publish()
      let result
      try { result = await request(config) } catch (e) {
        // A rejected transport cannot prove whether a write committed (including 502/504).
        if (writing) state.unconfirmed = state.pending
        throw e
      }
      if (result && result.code !== undefined && result.code !== 200) throw new Error(result.msg || '业务校验未通过')
      state.pending = null
      state.events.push({ time: Date.now(), label })
      if (state.events.length > 120) state.events.shift()
      publish()
      if (delayMs && writing) await new Promise(resolve => setTimeout(resolve, delayMs))
      return result
    }
  }
  async function execute() {
    try {
      for (; state.index < stages.length; state.index++) {
        await gate()
        state.steps[state.index].status = 'running'; publish()
        await stages[state.index].run(ctx)
        state.steps[state.index].status = 'completed'; publish()
      }
      await gate()
      state.status = 'completed'; state.current = '全流程贯通执行完成'; state.error = ''; publish()
    } catch (e) {
      if (e === STOP) { state.status = 'stopped'; state.current = '已停止，已完成的单据予以保留' }
      else {
        const reason = e && e.message ? e.message : String(e || '业务服务不可用')
        state.status = state.unconfirmed || /timeout|超时|network|连接|fetch|会话|登录|未确认/i.test(reason) ? 'interrupted' : 'failed'
        if (state.status === 'interrupted' && state.pending && state.pending.writing) state.unconfirmed = state.pending
        state.error = `${state.current || '执行'}：${reason}`
        if (state.steps[state.index]) state.steps[state.index].status = 'failed'
      }
      try { publish() } catch (storageError) { state.status = 'failed'; state.error = '无法保存执行检查点，请恢复浏览器存储后核对单据。'; onChange(state) }
    } finally { job = null }
    return state
  }
  function launch(setup) {
    const task = withLock(async () => { setup(); return execute() })
    job = Promise.resolve(task).catch(e => { job = null; state.status = 'interrupted'; state.error = e.message || String(e); onChange(state); throw e })
    return job
  }
  return {
    state,
    async start() {
      if (!userId) throw new Error('请先登录后开始执行')
      if (job || ACTIVE.includes((saved() || {}).status)) throw new Error('当前用户已有执行任务在进行，请返回原任务继续或终止')
      return launch(() => {
        paused = false; stopping = false
        Object.assign(state, { runId: nextRunId(), status: 'running', index: 0, current: '准备执行', error: '', pending: null, unconfirmed: null, context: {}, documents: [], events: [], summary: null, steps: stages.map(s => ({ title: s.title, description: s.description, status: 'waiting' })) })
        publish()
      })
    },
    pause() { if (state.status === 'running') { paused = true; state.status = 'pausing'; publish() } },
    resume() {
      if (paused && job) { paused = false; state.status = 'running'; publish(); if (wake) wake(); return job }
      if (job || !['failed', 'interrupted'].includes(state.status)) return job
      return launch(() => { paused = false; stopping = false; state.status = 'running'; state.error = ''; publish() })
    },
    stop() {
      if (!job) { if (state.runId && state.status !== 'completed') { state.status = 'stopped'; publish() }; return }
      stopping = true; paused = false; state.status = 'stopping'; publish(); if (wake) wake()
    }
  }
}

module.exports = { createChainRunner }

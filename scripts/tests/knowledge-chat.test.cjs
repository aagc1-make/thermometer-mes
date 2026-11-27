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

function chat(result, getEngineStatus, personalRequest) {
  let requests = 0
  const narrations = []
  const source = fs.readFileSync('ktg-mes-ui/src/views/mes/ai/index.vue', 'utf8')
  const script = source.match(/<script>([\s\S]*?)<\/script>/)[1].replace(/^import .*$/mg, '').replace('export default', 'component =')
  const box = { ...answerRules(), component: null, console, Date, setTimeout: () => 1, clearTimeout() {}, sessionStorage: { setItem() {}, getItem() {}, removeItem() {} },
    // 数字人素材常量由被剥掉的 import 提供，沙箱补上空值，避免 data() 直接报错。
    AVATAR_ASSETS: '', avatarAnswerText: () => '', AvatarSpeech: class { stop() {} },
    getEngineStatus, narrateAnswer: async payload => { narrations.push(payload); return { data: { text: '' } } },
    askBusinessQuestion: async () => ({ data: { mode: 'legacy' } }),
    request: async options => {
      requests++
      if (personalRequest) return personalRequest(options)
      throw new Error('知识问答不能请求业务或模型接口')
    } }
  vm.createContext(box); vm.runInContext(script, box)
  const ctx = { ...box.component.data(), ...box.component.methods, queryMode: 'knowledge',
    // created() 才会实例化语音对象，这里直接给一个空实现，重置/新会话流程才能跑到断言。
    _avatarSpeech: { stop() {}, dispose() {}, unlock: async () => {}, warm() {}, mouthLevel: () => 0, play() {} },
    $refs: { knowledgeBase: { search: async () => result } }, $store: { state: { user: { id: 7 } } },
    $auth: { hasPermi: () => true }, $set: (obj, key, value) => { obj[key] = value } }
  for (const key of ['personalMode', 'knowledgeUserId']) {
    if (box.component.computed[key]) Object.defineProperty(ctx, key, { get: () => box.component.computed[key].call(ctx) })
  }
  ctx.$nextTick = callback => { if (callback) callback.call(ctx) }
  const difyStatus = () => {
    ctx.difyState = box.component.computed.difyState.call(ctx)
    ctx.difyLabel = box.component.computed.difyLabel.call(ctx)
    return {
      label: ctx.difyLabel,
      tooltip: box.component.computed.difyTooltip.call(ctx)
    }
  }
  return { ctx, requests: () => requests, difyStatus, narrations }
}
const source = { documentId: 'one', title: '来料检验', version: 'V2', filename: 'iqc.md', sectionIndex: 0, sectionLabel: '第 1 段', excerpt: '检查传感器引脚与外观。' }

test('知识模式下即使问题含执行动词也只检索文档，不产生业务写入', async () => {
  const { ctx, requests } = chat({ sources: [source], documentCount: 1, activeCount: 1 })
  await ctx.route('如何执行采购入库？')
  assert.equal(ctx.messages.at(-1).type, 'knowledge')
  assert.equal(ctx.messages.at(-1).sources[0].documentId, 'one')
  assert.equal(requests(), 0)
})

test('业务模式继续走原库存查询，不被知识库资料替代', async () => {
  const { ctx, requests } = chat()
  ctx.queryMode = 'business'
  let stockQueries = 0
  ctx.queryStock = async () => { stockQueries++ }
  ctx.$refs.knowledgeBase.search = async () => { throw new Error('不应检索资料') }
  await ctx.route('本月电池库存')
  assert.equal(stockQueries, 1)
  assert.equal(requests(), 0)
})

test('没有文档、没有启用版本和没有匹配分别给出明确反馈', async () => {
  for (const [result, message] of [
    [{ sources: [], documentCount: 0, activeCount: 0 }, /导入/],
    [{ sources: [], documentCount: 1, activeCount: 0 }, /启用/],
    [{ sources: [], documentCount: 1, activeCount: 1 }, /未找到/]
  ]) {
    const { ctx, requests } = chat(result)
    await ctx.route('传感器工艺要求')
    assert.match(ctx.messages.at(-1).text, message)
    assert.equal(requests(), 0)
  }
})

test('浏览器资料问答保留原文引用和现有回答润色流程', async () => {
  const { ctx, requests, narrations } = chat({ sources: [source], documentCount: 1, activeCount: 1 })
  await ctx.send('传感器来料检验要求')
  assert.equal(ctx.messages.at(-1).type, 'knowledge')
  assert.equal(ctx.messages.at(-1).sources[0].excerpt, '检查传感器引脚与外观。')
  assert.equal(JSON.parse(narrations[0].facts)[0].text, '检查传感器引脚与外观。 [1]')
  assert.equal(requests(), 0)
})

test('个人 GraphRAG 保留引用与路径，且只把问题送到个人知识库', async () => {
  const question = '如何执行来料检验操作？'
  const { ctx, requests, narrations } = chat(null, undefined, async options => {
    assert.equal(options.url, '/mes/knowledge/ask')
    assert.equal(options.data.question, question)
    assert.equal(options.data.mode, 'graph_rag')
    return { data: { answer: '需要检验响应时间。[1]', graphStatus: 'matched',
      sources: [{ number: 1, fileName: 'iqc.md', version: 2, chunkNo: 1, text: '来料时检验响应时间。' }],
      paths: [[{ factId: 'one', sourceNumber: 1 }]] } }
  })
  ctx.queryMode = 'personal_graph'
  ctx.avatarOpen = true
  let speechRequests = 0
  ctx.readAvatarAnswer = () => { speechRequests++ }
  await ctx.send(question)
  const reply = ctx.messages.at(-1)
  assert.equal(reply.type, 'personalKnowledge')
  assert.equal(reply.sources[0].excerpt, '来料时检验响应时间。')
  assert.equal(reply.paths[0][0].factId, 'one')
  assert.equal(requests(), 1)
  assert.equal(narrations.length, 0)
  assert.equal(speechRequests, 0)
})

test('切换账号后舍弃上一账号未返回的个人库答案', async () => {
  let resolveRequest
  const { ctx } = chat(null, undefined, () => new Promise(resolve => { resolveRequest = resolve }))
  ctx.queryMode = 'personal_rag'
  const pending = ctx.send('什么是私有规范？')
  assert.equal(typeof resolveRequest, 'function')
  ctx.$store.state.user.id = 8
  resolveRequest({ data: { answer: '上一账号的专用检验规范', sources: [], paths: [] } })
  await pending
  assert.equal(ctx.messages.some(message => message.text.includes('上一账号')), false)
})

test('个人知识库查询失败时保留错误反馈，且不调用其他模型', async () => {
  const { ctx, requests, narrations } = chat(null, undefined, async () => { throw new Error('个人检索服务离线') })
  ctx.queryMode = 'personal_rag'
  await ctx.send('如何处理退货？')
  assert.match(ctx.messages.at(-1).text, /个人检索服务离线/)
  assert.equal(requests(), 1)
  assert.equal(narrations.length, 0)
})

test('Dify 状态请求失败时显示状态不可用，恢复后显示后端报告的状态', async () => {
  let status = { dify: 'online', difyDetail: { configured: true, detail: '工作流已完成' } }
  const { ctx, difyStatus } = chat(undefined, async () => {
    if (!status) throw new Error('Network Error')
    return { data: status }
  })

  assert.equal(difyStatus().label, '检查中')
  await ctx.refreshEngineStatus()
  assert.equal(difyStatus().label, '在线')

  status = null
  await ctx.refreshEngineStatus()
  assert.equal(difyStatus().label, '状态不可用')
  assert.match(difyStatus().tooltip, /无法获取/)

  status = { dify: 'not_configured', difyDetail: { configured: false } }
  await ctx.refreshEngineStatus()
  assert.equal(difyStatus().label, '未配置')
})

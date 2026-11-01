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

function chat(result) {
  let requests = 0
  const source = fs.readFileSync('ktg-mes-ui/src/views/mes/ai/index.vue', 'utf8')
  const script = source.match(/<script>([\s\S]*?)<\/script>/)[1].replace(/^import .*$/mg, '').replace('export default', 'component =')
  const box = { ...answerRules(), component: null, console, Date, setTimeout: () => 1, clearTimeout() {}, sessionStorage: { setItem() {}, getItem() {}, removeItem() {} },
    request: async () => { requests++; throw new Error('知识问答不能请求业务或模型接口') } }
  vm.createContext(box); vm.runInContext(script, box)
  const ctx = { ...box.component.data(), ...box.component.methods, queryMode: 'knowledge',
    $refs: { knowledgeBase: { search: async () => result } }, $store: { state: { user: { id: 7 } } },
    $auth: { hasPermi: () => true }, $set: (obj, key, value) => { obj[key] = value } }
  ctx.$nextTick = callback => { if (callback) callback.call(ctx) }
  return { ctx, requests: () => requests }
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

test('知识回答的待分析上下文包含所引原文，但发送问题不会自动调用模型', async () => {
  const { ctx, requests } = chat({ sources: [source], documentCount: 1, activeCount: 1 })
  await ctx.send('传感器来料检验要求')
  assert.equal(ctx.messages.at(-1).type, 'knowledge')
  assert.ok(JSON.stringify(ctx.messages.at(-1).analysisContext.facts).includes('检查传感器引脚与外观。'))
  assert.equal(requests(), 0)
})

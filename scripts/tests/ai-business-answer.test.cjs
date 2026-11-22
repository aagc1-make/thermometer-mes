const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const compiler = require('../../ktg-mes-ui/node_modules/vue-template-compiler')

const source = fs.readFileSync('ktg-mes-ui/src/views/mes/ai/index.vue', 'utf8')
const code = source.match(/<script>([\s\S]*?)<\/script>/)[1]
  .replace(/^import .*$/mg, '').replace('export default', 'component =')

function assistant(total = 56) {
  const requests = []
  const box = {
    component: null,
    Date: class extends Date {
      constructor(...args) { super(...(args.length ? args : ['2026-11-22T10:00:00'])) }
    },
    queryPurchaseReceipts: async query => {
      requests.push({ ...query })
      const data = { total, results: [] }
      if (query.page && total) {
        Object.assign(data, { page: query.page, offset: (query.page - 1) * 25, lastPage: 3,
          results: [{ code: 'purchaseReceipts', total, rows: [{ receiptCode: 'IR-001' }] }] })
      }
      return { data }
    },
    askBusinessQuestion: async () => ({ data: {
      mode: 'rules', answer: '上壳当前库存为 60 件。',
      results: [{ code: 'stock', total: 1, arguments: {}, rows: [{ itemCode: 'TG-01', balance: 60 }] }]
    } })
  }
  vm.runInNewContext(code, box)
  const ctx = Object.assign(box.component.data(), box.component.methods, {
    handleArrivalFollowup: async () => false,
    matchItem: async () => ({ items: [{ itemCode: 'TG-01', itemName: '上壳' }] }),
    push(message) { this.messages.push(message) },
    $nextTick() {}
  })
  return { ctx, requests }
}

test('采购入库只问记录数时只发计数查询，并在用户确认后查询明细', async () => {
  const { ctx, requests } = assistant()
  await ctx.send('这个月上壳有多少条采购入库记录？')
  const original = ctx.messages.at(-1)
  assert.deepEqual(requests, [{ itemCode: 'TG-01', from: '2026-11-01', to: '2026-11-30', page: null }])
  assert.match(original.answer, /本月共有 56 条上壳的采购入库记录/)
  assert.equal(original.toolResults.length, 0)
  assert.equal(original.offerDetails, true)

  await ctx.send('需要')
  assert.deepEqual(requests[1], { ...requests[0], page: 1 })
  assert.equal(ctx.messages.at(-1).toolResults[0].rows[0].receiptCode, 'IR-001')
  assert.equal(original.toolResults.length, 0)
})

test('追问上周只改时间，旧消息的明细按钮仍使用自己的范围', async () => {
  const { ctx, requests } = assistant()
  await ctx.send('本月上壳的采购记录有几笔？')
  const oldScope = ctx.messages.at(-1).receiptScope
  await ctx.send('只看上周的')
  assert.deepEqual(requests[1], { itemCode: 'TG-01', from: '2026-11-09', to: '2026-11-15', page: 1 })
  await ctx.send('查看入库明细', { scope: oldScope, page: 1 })
  assert.deepEqual(requests[2], { ...requests[0], page: 1 })
})

test('用户直接要明细时当轮查询，空结果没有展开入口', async () => {
  const { ctx, requests } = assistant(0)
  await ctx.send('列出2026-11-01到2026-11-15上壳采购入库记录')
  assert.deepEqual(requests[0], { itemCode: 'TG-01', from: '2026-11-01', to: '2026-11-15', page: 1 })
  assert.match(ctx.messages.at(-1).answer, /未查到上壳的采购入库记录/)
  assert.equal(ctx.messages.at(-1).offerDetails, false)
  assert.equal(ctx.messages.at(-1).toolResults.length, 0)
})

test('同一问题既问总数又明确要明细时直接查询明细', async () => {
  const { ctx, requests } = assistant()
  await ctx.send('本月上壳采购入库有多少条记录，并列出明细')
  assert.deepEqual(requests[0], { itemCode: 'TG-01', from: '2026-11-01', to: '2026-11-30', page: 1 })
  assert.equal(ctx.messages.at(-1).offerDetails, false)
})

test('普通工具回答在明细之前显示返回的自然语言结论', async () => {
  const { ctx } = assistant()
  await ctx.queryBusinessTools('上壳库存')
  const message = ctx.messages.at(-1)
  assert.equal(message.answer, '上壳当前库存为 60 件。')
  assert.equal(message.toolResults[0].rows[0].balance, 60)

  const component = compiler.parseComponent(fs.readFileSync('ktg-mes-ui/src/views/mes/ai/BusinessToolAnswer.vue', 'utf8'))
  const template = compiler.compile(component.template.content)
  assert.deepEqual(template.errors, [])
  const details = template.ast.children.find(node => node.tag === 'details')
  assert.equal('open' in details.attrsMap, false)
  assert.ok(template.ast.children.findIndex(node => node.tag === 'p') < template.ast.children.indexOf(details))
})

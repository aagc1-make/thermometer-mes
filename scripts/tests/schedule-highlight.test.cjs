const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')

function component({ task = async () => ({ rows: [], total: 0 }), orders = [] } = {}) {
  const source = fs.readFileSync('ktg-mes-ui/src/views/mes/pro/schedule/index.vue', 'utf8')
  const code = source.match(/<script>([\s\S]*?)<\/script>/)[1]
    .replace(/^import .*$/mg, '').replace('export default', 'component =')
  const box = { component: null, pipelineWorkorderContext: {}, Date, Promise, Set, ProTask: {}, GanttChar: {}, Treeselect: {},
    listProtask: task, listWorkorder: async () => ({ rows: orders, total: orders.length }) }
  vm.createContext(box); vm.runInContext(code, box)
  const ctx = { ...box.component.data(), ...box.component.methods, handleTree: rows => rows }
  return ctx
}
const recent = id => ({ workorderId: id, status: 'CONFIRMED', createTime: new Date(Date.now() - 60000).toISOString() })

test('仅最近创建且已确认无任务的工单高亮，旧工单和已排产工单不高亮', async () => {
  const rows = [recent(1), recent(2), { ...recent(3), createTime: new Date(Date.now() - 3600000).toISOString() }]
  const c = component({ orders: rows, task: async p => ({ rows: p.workorderId === 2 ? [{ workorderId: '2' }] : [], total: p.workorderId === 2 ? 1 : 0 }) })
  await c.getList()
  assert.equal(c.rowClassName({ row: rows[0] }), 'row-unscheduled')
  assert.equal(c.rowClassName({ row: rows[1] }), '')
  assert.equal(c.rowClassName({ row: rows[2] }), '')
})

test('任务查询失败或返回不完整时不错误高亮', async () => {
  for (const task of [async () => { throw new Error('offline') }, async () => ({ rows: [], total: 1 }), async () => ({})]) {
    const row = recent(4), c = component({ orders: [row], task })
    await c.getList()
    assert.equal(c.rowClassName({ row }), '')
  }
})

test('再次查询发现已产生任务后立即取消高亮', async () => {
  const row = recent(5); let scheduled = false
  const c = component({ orders: [row], task: async () => scheduled ? { rows: [{ workorderId: 5 }], total: 1 } : { rows: [], total: 0 } })
  await c.getList(); assert.equal(c.rowClassName({ row }), 'row-unscheduled')
  scheduled = true
  await c.getList(); assert.equal(c.rowClassName({ row }), '')
})

test('缺少时间、未来时间、非确认工单和已过时的工单不高亮', () => {
  const c = component(); c.unscheduledWorkorderIds = ['1']
  for (const value of [undefined, 'invalid', new Date(Date.now() + 3600000).toISOString(), new Date(Date.now() - 11 * 60000).toISOString()]) {
    assert.equal(c.rowClassName({ row: { ...recent(1), createTime: value } }), '')
  }
  assert.equal(c.rowClassName({ row: { ...recent(1), status: 'PREPARE' } }), '')
})

test('旧请求晚返回不得覆盖较新一次刷新结果', async () => {
  const row = recent(6); let resolveFirst; let call = 0; let started
  const firstStarted = new Promise(resolve => { started = resolve })
  const c = component({ orders: [row], task: () => ++call === 1 ? new Promise(resolve => { resolveFirst = resolve; started() }) : Promise.resolve({ rows: [{ workorderId: 6 }], total: 1 }) })
  const first = c.getList(); await firstStarted
  await c.getList(); resolveFirst({ rows: [], total: 0 }); await first
  assert.equal(c.rowClassName({ row }), '')
})

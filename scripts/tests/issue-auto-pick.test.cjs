const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')

function load(name, api = {}) {
  const source = fs.readFileSync(`ktg-mes-ui/src/views/mes/wm/issue/${name}.vue`, 'utf8')
  const code = source.match(/<script>([\s\S]*?)<\/script>/)[1]
    .replace(/^import .*$/mg, '').replace('export default', 'component =')
  const box = { component: null, pipelineWorkorderContext: {}, WorkorderSelect: {}, WorkstationSelect: {},
    Issueline: {}, IssueDetail: {}, StockSelect: {}, ...api }
  vm.createContext(box); vm.runInContext(code, box)
  const c = { ...box.component.data(), ...box.component.methods,
    resetForm() {}, formatDate: () => '2026-10-31 10:00:00', parseTime: value => value,
    $modal: { msgSuccess() {}, msgWarning() {} }, $refs: {}, getList() {} }
  return { c, source }
}

test('新建领料单默认自动生成编号，工单已知名称日期客户直接带入', async () => {
  const { c } = load('index', { genCode: async () => 'LL-20261031-01' })
  await c.handleAdd()
  assert.equal(c.autoGenFlag, true)
  assert.equal(c.form.issueCode, 'LL-20261031-01')
  c.onWorkorderSelected({ workorderId: 10, workorderCode: 'MO-01', workorderName: '测温枪组装',
    requestDate: '2026-11-02 08:00:00', clientId: 1, clientCode: 'C-01', clientName: '客户甲' })
  assert.equal(c.form.issueName, '测温枪组装领料')
  assert.equal(c.form.requiredTime, '2026-11-02 08:00:00')
  assert.equal(c.form.clientName, '客户甲')
})

test('切换工单更新自动名称，但保留人工修改的名称和需求日期', () => {
  const { c } = load('index')
  c.reset(); c.onWorkorderSelected({ workorderId: 1, workorderName: '甲工单' })
  c.onWorkorderSelected({ workorderId: 2, workorderName: '乙工单' })
  assert.equal(c.form.issueName, '乙工单领料')
  c.form.issueName = '紧急补料'; c.form.requiredTime = '2026-11-03 09:00:00'
  c.onWorkorderSelected({ workorderId: 3, workorderName: '丙工单', requestDate: '2026-11-05 08:00:00' })
  assert.equal(c.form.issueName, '紧急补料')
  assert.equal(c.form.requiredTime, '2026-11-03 09:00:00')
})

test('一键拣货仅发一次整单请求，完成后刷新明细并转为待领出', async () => {
  let calls = 0; let resolve
  const pending = new Promise(r => { resolve = r })
  const { c } = load('index', { autoPickIssue: async id => { calls++; assert.equal(id, 12); return pending } })
  c.form = { issueId: 12, status: 'APPROVING' }
  let refresh = 0; c.$refs.detail = { getListWithDetail() { refresh++ } }; c.$nextTick = fn => fn()
  const first = c.handleAutoPick(); await c.handleAutoPick()
  assert.equal(c.picking, true); assert.equal(calls, 1)
  resolve({ data: { status: 'APPROVED', lineCount: 6, detailCount: 6 } }); await first
  assert.equal(c.picking, false); assert.equal(c.form.status, 'APPROVED'); assert.equal(refresh, 1)
})

test('缺料失败保留原状态、展示具体缺口并恢复按钮', async () => {
  const { c } = load('index', { autoPickIssue: async () => { throw new Error('TG-ST-006 可用库存不足，尚缺 3 片') } })
  c.form = { issueId: 12, status: 'APPROVING' }
  await c.handleAutoPick()
  assert.equal(c.form.status, 'APPROVING'); assert.equal(c.picking, false)
  assert.match(c.pickError, /TG-ST-006.*3/)
})

test('草稿一键拣货先保存已填写的单头，校验不通过时不发送拣货请求', async () => {
  const events = []
  const { c } = load('index', {
    updateIssueheader: async form => { events.push(['save', form.issueName]) },
    autoPickIssue: async () => { events.push(['pick']); return { data: { status: 'APPROVED', lineCount: 6 } } }
  })
  c.form = { issueId: 12, status: 'PREPARE', issueName: '已修改的名称' }
  c.$nextTick = fn => fn()
  c.$refs.form = { validate: callback => callback(false) }
  await c.handleAutoPick(); assert.equal(events.length, 0); assert.equal(c.picking, false)
  c.$refs.form.validate = callback => callback(true)
  await c.handleAutoPick()
  assert.deepEqual(events, [['save', '已修改的名称'], ['pick']])
})

test('自动拣齐后可重新进入调整分配，更新失败不改变本地状态', async () => {
  let fail = false
  const { c } = load('index', { updateIssueheader: async data => {
    assert.equal(data.issueId, 12); assert.equal(data.status, 'APPROVING')
    if (fail) throw new Error('网络异常')
  } })
  c.form = { issueId: 12, status: 'APPROVED' }
  await c.handleAdjustPick(); assert.equal(c.form.status, 'APPROVING'); assert.equal(c.picking, false)
  c.form.status = 'APPROVED'; fail = true
  await c.handleAdjustPick(); assert.equal(c.form.status, 'APPROVED'); assert.match(c.pickError, /网络异常/)
})

test('手工选库存按剩余需求自动填写数量，不再带入整批库存', () => {
  const { c } = load('detail')
  c.issuelineList = [{ lineId: 1, quantityIssued: 10, details: [{ detailId: 2, quantity: 4 }] }]
  c.form = { lineId: 1 }
  c.onStockSelected({ materialStockId: 7, quantityOnhand: 200, batchCode: 'B-01', areaName: 'A01' })
  assert.equal(c.form.quantity, 6); assert.equal(c.form.quantityMax, 6)
  c.form.detailId = 2
  c.onStockSelected({ materialStockId: 7, quantityOnhand: 3 })
  assert.equal(c.form.quantity, 3); assert.equal(c.form.quantityMax, 3)
})

test('手工拣货剩余数量处理小数并汇总已有分配', () => {
  const { c } = load('detail')
  assert.equal(c.remainingQuantity({ quantityIssued: 0.3, details: [{ quantity: 0.1 }] }), 0.2)
  assert.equal(c.remainingQuantity({ quantityIssued: 10, details: [{ quantity: 6 }, { quantity: 4 }] }), 0)
})

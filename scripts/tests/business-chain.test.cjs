const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const file = require('node:path').resolve(__dirname, '../../ktg-mes-ui/src/utils/businessChainRunner.js')
const api = fs.existsSync(file) ? require(file) : {}
const flowFile = require('node:path').resolve(__dirname, '../../ktg-mes-ui/src/utils/businessChainFlow.js')
const flow = fs.existsSync(flowFile) ? require(flowFile) : {}
const memory = () => { const data = {}; return { getItem: k => data[k] || null, setItem: (k, v) => { data[k] = v } } }
const deferred = () => { let resolve; const promise = new Promise(r => { resolve = r }); return { promise, resolve } }

test('贯通执行器提供独立的控制接口', () => {
  assert.equal(typeof api.createChainRunner, 'function')
})

test('重复点击开始不重复提交，另一个标签页也不能启动新链', async () => {
  const wait = deferred(); let calls = 0
  const storage = memory()
  const options = { userId: 1, storage, request: async () => { calls++; await wait.promise; return { code: 200 } }, stages: [{ title: '创建', run: ctx => ctx.call('创建单据', { method: 'post', url: '/create' }) }] }
  const runner = api.createChainRunner(options)
  const run = runner.start()
  await Promise.resolve(); await Promise.resolve()
  await assert.rejects(runner.start(), /正在|进行/)
  await assert.rejects(api.createChainRunner(options).start(), /正在|进行/)
  wait.resolve(); await run
  assert.equal(calls, 1); assert.equal(runner.state.status, 'completed')
})

test('暂停在当前请求结束后阻止下一次写入，继续后只执行剩余操作', async () => {
  const wait = deferred(); const paused = deferred(); const calls = []
  const runner = api.createChainRunner({ userId: 1, storage: memory(), request: async c => { calls.push(c.url); if (c.url === '/one') await wait.promise; return {} }, onChange: s => { if (s.status === 'paused') paused.resolve() }, stages: [{ title: '两次写入', run: async ctx => { await ctx.call('一', { method: 'post', url: '/one' }); await ctx.call('二', { method: 'post', url: '/two' }) } }] })
  const run = runner.start(); await Promise.resolve(); await Promise.resolve()
  runner.pause(); wait.resolve(); await paused.promise
  assert.deepEqual(calls, ['/one'])
  runner.resume(); await run
  assert.deepEqual(calls, ['/one', '/two']); assert.equal(runner.state.status, 'completed')
})

test('停止保留当前请求结果并阻止后续操作', async () => {
  const wait = deferred(); const calls = []
  const runner = api.createChainRunner({ userId: 1, storage: memory(), request: async c => { calls.push(c.url); await wait.promise; return {} }, stages: [{ title: '写入', run: async ctx => { await ctx.call('一', { method: 'post', url: '/one' }); await ctx.call('二', { method: 'post', url: '/two' }) } }] })
  const run = runner.start(); await Promise.resolve(); await Promise.resolve()
  runner.stop(); wait.resolve(); await run
  assert.equal(runner.state.status, 'stopped'); assert.deepEqual(calls, ['/one'])
})

test('失败停止后续阶段，写请求超时标记结果待核对', async () => {
  let later = false
  const runner = api.createChainRunner({ userId: 1, storage: memory(), request: async () => { throw new Error('系统接口请求超时') }, stages: [{ title: '创建', run: ctx => ctx.call('创建到货单', { method: 'post', url: '/create' }) }, { title: '后续', run: () => { later = true } }] })
  await runner.start()
  assert.equal(runner.state.status, 'interrupted'); assert.equal(later, false)
  assert.match(runner.state.error, /创建到货单.*超时/); assert.equal(runner.state.pending.url, '/create')
})

test('刷新恢复只读取检查点，不自动执行接口', async () => {
  const storage = memory(); const wait = deferred()
  const runner = api.createChainRunner({ userId: 7, storage, request: async () => { await wait.promise; return {} }, stages: [{ title: '创建', run: ctx => ctx.call('新增', { method: 'post', url: '/new' }) }] })
  const run = runner.start(); await Promise.resolve(); await Promise.resolve()
  let calls = 0
  const restored = api.createChainRunner({ userId: 7, storage, request: () => { calls++ }, stages: [] })
  assert.equal(restored.state.status, 'interrupted'); assert.equal(calls, 0)
  assert.ok(restored.state.runId); assert.equal(restored.state.pending.url, '/new')
  runner.stop(); wait.resolve(); await run
})

test('领料仅分配本次批次并支持同一批次多个库位，不回退历史库存', () => {
  const stocks = [{ itemId: 1, batchCode: 'OLD', quantityOnhand: 100, warehouseCode: 'WH-RAW' }, { itemId: 1, batchCode: 'NEW', quantityOnhand: 4, warehouseCode: 'WH-RAW' }, { itemId: 1, batchCode: 'NEW', quantityOnhand: 6, warehouseCode: 'WH-RAW' }]
  const allocated = flow.allocateBatchStocks(stocks, 1, ['NEW'], 10, 'WH-RAW')
  assert.deepEqual(allocated.map(s => s.quantity), [4, 6])
  assert.ok(allocated.every(s => s.batchCode === 'NEW'))
  assert.throws(() => flow.allocateBatchStocks(stocks, 1, ['NEW'], 11, 'WH-RAW'), /本次批次.*不足/)
  assert.throws(() => flow.allocateBatchStocks(stocks, 1, [], 1, 'WH-RAW'), /批次/)
})

test('恢复创建操作先按唯一编码查询，已存在则不再写入', async () => {
  let writes = 0
  const ctx = { call: async (label, c) => { if (c.method !== 'get') writes++; return { code: 200, rows: [{ noticeId: 9, noticeCode: 'D-ONE' }], total: 1 } } }
  const result = await flow.createChainApi(ctx).ensure('/mes/wm/arrivalnotice', { noticeCode: 'D-ONE' }, r => r.noticeCode === 'D-ONE', { noticeCode: 'D-ONE' }, '到货单')
  assert.equal(result.noticeId, 9); assert.equal(writes, 0)
})

test('准备检查缺主数据时没有任何写入', async () => {
  let writes = 0
  const runner = api.createChainRunner({ userId: 1, storage: memory(), request: async c => { if (c.method !== 'get') writes++; return { code: 200, rows: [], total: 0 } }, stages: flow.createChainStages() })
  await runner.start()
  assert.equal(runner.state.status, 'failed'); assert.equal(writes, 0)
  assert.match(runner.state.error, /物料|配置/)
})

test('其他标签页恢复失败不能覆盖仍在运行的检查点', async () => {
  const storage = memory(); const wait = deferred(); let locked = false
  const withLock = async work => { if (locked) throw new Error('其他标签页正在执行贯通'); locked = true; try { return await work() } finally { locked = false } }
  const options = { userId: 3, storage, withLock, request: async () => { await wait.promise; return {} }, stages: [{ title: '写入', run: ctx => ctx.call('创建单据', { method: 'post', url: '/create' }) }] }
  const original = api.createChainRunner(options); const run = original.start()
  await Promise.resolve(); await Promise.resolve()
  const other = api.createChainRunner(options)
  await assert.rejects(other.resume(), /其他标签页/)
  assert.equal(JSON.parse(storage.getItem('mes-business-chain:v1:3')).status, 'running')
  assert.equal(JSON.parse(storage.getItem('mes-business-chain:v1:3')).pending.url, '/create')
  original.stop(); wait.resolve(); await run
})

test('锁被占用后释放仍可恢复，不遗留拒绝的运行任务', async () => {
  let blocked = false; let calls = 0
  const storage = memory()
  const options = { userId: 8, storage, withLock: async work => { if (blocked) throw new Error('其他标签页正在执行贯通'); return work() }, request: async () => { calls++; if (calls === 1) throw new Error('网络连接异常'); return {} }, stages: [{ title: '读取', run: ctx => ctx.call('核对', { method: 'get', url: '/read' }) }] }
  const runner = api.createChainRunner(options)
  await runner.start(); blocked = true
  await assert.rejects(runner.resume(), /其他标签页/)
  blocked = false; await runner.resume()
  assert.equal(calls, 2); assert.equal(runner.state.status, 'completed')
})

test('超时写入尚未能查到结果时不得重发 POST，确认原结果后方可继续', async () => {
  let writes = 0; let committed = false
  const runner = api.createChainRunner({ userId: 1, storage: memory(), request: async c => {
    if (c.method === 'post') { writes++; throw new Error('请求超时') }
    return { code: 200, rows: committed ? [{ noticeId: 9, noticeCode: 'ONE' }] : [], total: committed ? 1 : 0 }
  }, stages: [{ title: '创建', run: ctx => flow.createChainApi(ctx).ensure('/arrival', { noticeCode: 'ONE' }, r => r.noticeCode === 'ONE', { noticeCode: 'ONE' }, '到货通知') }] })
  await runner.start(); assert.equal(writes, 1)
  await runner.resume(); assert.equal(writes, 1); assert.equal(runner.state.status, 'interrupted')
  committed = true; await runner.resume()
  assert.equal(writes, 1); assert.equal(runner.state.status, 'completed')
})

test('找到草稿不能确认仍未完成的提交 PUT', async () => {
  let puts = 0
  const runner = api.createChainRunner({ userId: 1, storage: memory(), request: async c => {
    if (c.method === 'put') { puts++; throw new Error('请求超时') }
    return c.url.endsWith('/9') ? { data: { noticeId: 9, noticeCode: 'ONE', status: 'PREPARE' } } : { rows: [{ noticeId: 9, noticeCode: 'ONE', status: 'PREPARE' }], total: 1 }
  }, stages: [{ title: '提交', run: async ctx => {
    const a = flow.createChainApi(ctx)
    const head = await a.ensure('/arrival', { noticeCode: 'ONE' }, r => r.noticeCode === 'ONE', { noticeCode: 'ONE' }, '到货单')
    await a.status('/arrival', head.noticeId, 'APPROVING', ['PREPARE'])
  } }] })
  await runner.start(); await runner.resume()
  assert.equal(puts, 1); assert.equal(runner.state.status, 'interrupted')
})

test('网关504及未知写错误都保留待核对标记', async () => {
  const runner = api.createChainRunner({ userId: 1, storage: memory(), request: async () => { throw new Error('Request failed with status code 504') }, stages: [{ title: '创建', run: ctx => ctx.call('创建', { method: 'post', url: '/create' }) }] })
  await runner.start()
  assert.equal(runner.state.status, 'interrupted'); assert.equal(runner.state.unconfirmed.url, '/create')
})

test('缺少产出和流水查询权限时在写入前阻断', async () => {
  assert.ok(flow.REQUIRED_PERMISSIONS.includes('mes:wm:productproduce:list'))
  assert.ok(flow.REQUIRED_PERMISSIONS.includes('mes:wm:transaction:list'))
  let calls = 0
  const runner = api.createChainRunner({ userId: 1, storage: memory(), request: async () => { calls++ }, stages: flow.createChainStages({ hasPermission: p => p !== 'mes:wm:productproduce:list' }) })
  await runner.start(); assert.equal(calls, 0); assert.match(runner.state.error, /productproduce:list/)
})

// —— 成品仓库位：历史结存占满库位后，贯通仍可重复执行 ——
const warehouseSetup = () => ({
  items: {
    'TG-FG-001': { itemId: 9201, itemCode: 'TG-FG-001', itemName: '红外测温枪整机', unitOfMeasure: 'TAI', unitName: '台' },
    'TG-EL-001': { itemId: 9204, itemCode: 'TG-EL-001', itemName: '红外温度传感器' }
  },
  warehouses: [{ warehouseId: 9703, warehouseCode: 'WH-FG', warehouseName: '成品仓', frozenFlag: 'N' }],
  locations: [{ locationId: 9718, locationCode: 'L-FG', locationName: '成品区', warehouseId: 9703, frozenFlag: 'N' }],
  areas: [
    { areaId: 9728, areaCode: 'FG-01', areaName: '成品库位', locationId: 9718, enableFlag: 'Y', frozenFlag: 'N', productMixing: 'N', batchMixing: 'N' },
    { areaId: 9730, areaCode: 'FG-02', areaName: '成品库位2', locationId: 9718, enableFlag: 'Y', frozenFlag: 'N', productMixing: 'N', batchMixing: 'N' }
  ],
  initialStock: [
    { areaId: 9728, itemId: 9201, itemCode: 'TG-FG-001', batchCode: 'BAT-OLD', quantityOnhand: 4 },
    { areaId: 9730, itemId: 9218, itemCode: 'TG-DEMO-001', batchCode: 'BAT-DEMO', quantityOnhand: 4 }
  ]
})
const slotContext = (context, store) => {
  const writes = []
  let nextId = 9800
  const ctx = {
    state: { runId: 'D-TEST', context, documents: [] },
    writes,
    confirmWrite() {},
    checkpoint() {},
    document(type, id, code, path, status) { ctx.state.documents.push({ type, id, code, path, status }) },
    async call(label, config) {
      const method = (config.method || 'get').toLowerCase()
      if (method !== 'get') writes.push({ url: config.url, data: config.data })
      if (method === 'post' && config.url === '/mes/wm/area') {
        const area = { areaId: nextId++, ...config.data, locationId: config.data.locationId }
        store.push(area)
        return { code: 200, data: area.areaId }
      }
      if (method === 'get' && config.url === '/mes/wm/area/list') {
        const rows = store.filter(a => !config.params.areaCode || a.areaCode === config.params.areaCode)
        return { code: 200, rows, total: rows.length }
      }
      throw new Error('未预期的请求：' + config.url)
    }
  }
  return ctx
}

test('历史结存占满成品库位时不混放，改为建立物料专用库位并只写入一次', async () => {
  const context = warehouseSetup(); const store = []
  const ctx = slotContext(context, store)
  const reservations = []
  const slot = await flow.ensureSlot(ctx, flow.createChainApi(ctx), 'WH-FG', 'TG-FG-001', reservations, () => true)
  assert.equal(slot.areaCode, flow.chainAreaCode('WH-FG', 'TG-FG-001'))
  assert.equal(writesOf(ctx).length, 1)
  const created = writesOf(ctx)[0].data
  assert.equal(created.productMixing, 'N'); assert.equal(created.batchMixing, 'Y')
  assert.equal(created.locationId, 9718); assert.equal(created.enableFlag, 'Y')
  assert.equal(ctx.state.documents.length, 1); assert.equal(ctx.state.documents[0].code, slot.areaCode)
  // 第二轮贯通复用同一库位，不再写主数据
  const again = slotContext(context, store)
  const reused = await flow.ensureSlot(again, flow.createChainApi(again), 'WH-FG', 'TG-FG-001', [], () => true)
  assert.equal(reused.areaCode, slot.areaCode); assert.equal(writesOf(again).length, 0)
  assert.equal(again.state.documents.length, 0)
})

test('库位本可混放时直接使用现有库位，不写主数据', async () => {
  const context = warehouseSetup()
  context.areas[0].batchMixing = 'Y'
  const ctx = slotContext(context, [])
  const slot = await flow.ensureSlot(ctx, flow.createChainApi(ctx), 'WH-FG', 'TG-FG-001', [], () => true)
  assert.equal(slot.areaCode, 'FG-01'); assert.equal(writesOf(ctx).length, 0)
})

test('没有新增库位权限时在写入前停止并说明原因', async () => {
  const context = warehouseSetup(); const ctx = slotContext(context, [])
  await assert.rejects(flow.ensureSlot(ctx, flow.createChainApi(ctx), 'WH-FG', 'TG-FG-001', [], p => p !== flow.CHAIN_AREA_PERMISSION), /mes:wm:area:add/)
  assert.equal(writesOf(ctx).length, 0)
  assert.match(flow.findSlot(context, 'WH-FG', 9201, []).blocked.join('；'), /FG-01.*批次混放.*FG-02.*物料混放/)
})

function writesOf(ctx) { return ctx.writes }

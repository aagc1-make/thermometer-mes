const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')

const NOTICE = { noticeId: 31, noticeCode: 'AN20261031003', status: 'PREPARE', vendorName: '测试供应商', arrivalDate: '2026-10-31' }
const LINE = { noticeId: 31, lineId: 72, itemId: 6, itemCode: 'TG-EL-006', itemName: '电池', unitName: '节', quantityArrival: 10, iqcCheck: 'Y' }
const created = (notice = NOTICE) => ({ role: 'bot', type: 'arrival', ...notice, finished: true, busy: false, error: '', lines: [{ ...LINE }] })

function chat({ history = [created()], notice = NOTICE, lines = [LINE], inspections = [], receipts = [], denied = [] } = {}) {
  const calls = []
  const storage = new Map()
  const source = fs.readFileSync('ktg-mes-ui/src/views/mes/ai/index.vue', 'utf8')
  const code = source.match(/<script>([\s\S]*?)<\/script>/)[1]
    .replace(/^import .*$/mg, '').replace('export default', 'component =')
  const request = async config => {
    calls.push(config)
    assert.equal(config.method, 'get', '追问只能读取业务数据')
    if (config.url === '/mes/wm/arrivalnotice/list') return { rows: notice ? [notice] : [], total: notice ? 1 : 0 }
    if (/^\/mes\/wm\/arrivalnotice\/\d+$/.test(config.url)) return { code: 200, data: notice }
    if (config.url === '/mes/wm/arrivalnoticeline/list') return { rows: lines, total: lines.length }
    if (config.url === '/mes/qc/iqc/list') return { rows: inspections, total: inspections.length }
    if (config.url === '/mes/wm/itemrecpt/list') return { rows: receipts, total: receipts.length }
    if (config.url === '/mes/md/mditem/list') return { rows: [{ itemId: 6, itemCode: 'TG-EL-006', itemName: '电池', unitName: '节', unitOfMeasure: '节', enableFlag: 'Y' }], total: 1 }
    if (config.url === '/mes/md/vendor/list') return { rows: [{ vendorId: 8, vendorName: '测试供应商', enableFlag: 'Y' }], total: 1 }
    if (config.url === '/mes/pro/workorder/list') return { rows: [{ workorderId: 200, workorderCode: 'WO-002', quantity: 10, quantityProduced: 0, unitName: '台' }], total: 1 }
    throw new Error('Unexpected API: ' + config.url)
  }
  const box = { component: null, request, Date, console, setTimeout: () => 1, clearTimeout() {},
    sessionStorage: { getItem: key => storage.get(key), setItem: (key, value) => storage.set(key, value), removeItem: key => storage.delete(key) } }
  vm.createContext(box)
  vm.runInContext(code, box)
  const ctx = { ...box.component.data(), ...box.component.methods,
    $auth: { hasPermi: permission => !denied.includes(permission) },
    $store: { state: { user: { id: 7 } } }, $refs: {},
    $nextTick: fn => fn.call(ctx), $set: (obj, key, value) => { obj[key] = value },
    $router: { push: target => { ctx.navigation = target; return Promise.resolve() } } }
  ctx.messages.push(...history)
  return { ctx, calls }
}

test('引用到货通知查询采购入库时，只返回关联入库单而非检验单', async () => {
  const { ctx, calls } = chat({ notice: { ...NOTICE, status: 'APPROVED' }, receipts: [
    { recptId: 80, recptCode: 'R20261031004', noticeId: 31, status: 'PREPARE' },
    { recptId: 81, recptCode: 'R_OTHER', noticeId: 99, status: 'PREPARE' }
  ] })
  await ctx.route('已提交到货通知，接下来给我它的采购入库单')
  const card = ctx.messages.at(-1)
  assert.equal(card.type, 'arrivalReceipt')
  assert.deepEqual(Array.from(card.receipts, r => r.recptCode), ['R20261031004'])
  assert.equal(calls.find(c => c.url === '/mes/wm/itemrecpt/list').params.noticeId, 31)
})

test('入库草稿尚不存在时明确说明，不把已完成的IQC冒充采购入库单', async () => {
  const { ctx } = chat({ inspections: [{ sourceDocType: 'ARRIVAL_NOTICE', sourceDocId: 31, status: 'FINISHED' }] })
  await ctx.route('看看它的采购入库单')
  assert.equal(ctx.messages.at(-1).type, 'arrivalReceipt')
  assert.equal(ctx.messages.at(-1).receipts.length, 0)
  assert.match(ctx.messages.at(-1).nextStep, /未找到/)
})

test('刚创建到货通知后口语送检指向原单，不能变成物料或入库查询', async () => {
  const { ctx, calls } = chat()
  await ctx.route('把刚刚的到货通知让我检验一下下')
  const detail = ctx.messages.at(-1)
  assert.equal(detail.type, 'arrivalDetail')
  assert.equal(detail.notice.noticeCode, 'AN20261031003')
  assert.equal(detail.lines[0].quantityArrival, 10)
  assert.match(detail.nextStep, /草稿/)
  assert.match(detail.nextStep, /提交/)
  assert.ok(calls.every(c => !/mditem|transaction|\/intent/.test(c.url)))
})

test('省略单据名的“看看它”可延续最近单据，普通业务问题仍独立解析', async () => {
  const { ctx } = chat()
  await ctx.route('让我看看它')
  assert.equal(ctx.messages.at(-1).notice.noticeId, 31)
  assert.equal(ctx.matchRule('本月电池采购入库情况'), 'queryFlowIn')
})

test('紧接创建后的“让我检验一下”延续当前单据，切换话题后不借用陈旧单据', async () => {
  const { ctx } = chat()
  await ctx.route('让我检验一下下')
  assert.equal(ctx.messages.at(-1).type, 'arrivalDetail')
  ctx.messages.push({ role: 'bot', type: 'stock', itemCode: 'TG-ST-001' })
  await ctx.route('让我检查一下')
  assert.notEqual(ctx.messages.at(-1).type, 'arrivalDetail')
})

test('有到货上下文时，新的需检到货请求仍创建确认卡片，不能被旧单据吸走', async () => {
  for (const question of ['帮我新增到货通知：电池10节，需要来料检验', '参考刚刚的到货通知，新增电池20节的到货通知，需要检验']) {
    const { ctx, calls } = chat()
    await ctx.route(question)
    const draft = ctx.messages.at(-1)
    assert.equal(draft.type, 'arrival')
    assert.equal(draft.noticeId, null)
    assert.equal(draft.finished, false)
    assert.ok(calls.every(c => !/^\/mes\/wm\/arrivalnotice\//.test(c.url)))
  }
})

test('“刚刚创建好的”属于旧单据定语，仍应支持送检追问', async () => {
  const { ctx } = chat()
  await ctx.route('把刚刚创建好的到货通知让我检验一下')
  assert.equal(ctx.messages.at(-1).type, 'arrivalDetail')
})

test('显式工单号不能被泛化的“单号”误当成到货通知编号', async () => {
  const { ctx, calls } = chat()
  await ctx.route('查看工单号 WO-002 的进度')
  const card = ctx.messages.findLast(m => m.type === 'list')
  assert.equal(card.rows[0].workorderCode, 'WO-002')
  assert.ok(calls.every(c => !/arrivalnotice/.test(c.url)))
})

test('明确单号优先于旧上下文，只读取指定通知的检验记录', async () => {
  const notice = { ...NOTICE, noticeId: 40, noticeCode: 'AN20261031004', status: 'APPROVING' }
  const { ctx, calls } = chat({ notice, lines: [{ ...LINE, noticeId: 40 }], inspections: [
    { iqcId: 90, iqcCode: 'IQC40', sourceDocType: 'ARRIVAL_NOTICE', sourceDocId: 40, status: 'PREPARE' },
    { iqcId: 91, iqcCode: 'IQC31', sourceDocType: 'ARRIVAL_NOTICE', sourceDocId: 31 },
    { iqcId: 92, iqcCode: 'IQC_OTHER', sourceDocType: 'OTHER', sourceDocId: 40 }
  ] })
  await ctx.route('检验到货通知 AN20261031004')
  const detail = ctx.messages.at(-1)
  assert.equal(detail.notice.noticeId, 40)
  assert.deepEqual(Array.from(detail.inspections, i => i.iqcId), [90])
  assert.equal(calls.find(c => c.url === '/mes/wm/arrivalnotice/list').params.noticeCode, 'AN20261031004')
  assert.equal(calls.find(c => c.url === '/mes/qc/iqc/list').params.sourceDocId, 40)
})

test('免检草稿要求先修改检验标志，不假装已有待检单', async () => {
  const { ctx } = chat({ lines: [{ ...LINE, iqcCheck: 'N' }] })
  await ctx.route('刚才那个到货通知送检')
  assert.match(ctx.messages.at(-1).nextStep, /免检/)
  assert.match(ctx.messages.at(-1).nextStep, /需要来料检验/)
  assert.equal(ctx.messages.at(-1).inspections.length, 0)
})

test('未确认或已取消的最近草稿不能被误认为之前已创建的单据', async () => {
  for (const finished of [false, true]) {
    const pending = { role: 'bot', type: 'arrival', noticeId: null, noticeCode: '', finished, lines: [LINE] }
    const { ctx, calls } = chat({ history: [created(), pending] })
    await ctx.route('刚刚的到货通知让我检验一下')
    assert.equal(ctx.messages.at(-1).type, 'text')
    assert.match(ctx.messages.at(-1).text, finished ? /未创建|取消/ : /确认创建/)
    assert.equal(calls.length, 0)
  }
})

test('没有历史上下文时澄清单号，不查询全部到货或全部物料', async () => {
  const { ctx, calls } = chat({ history: [] })
  await ctx.route('刚刚的到货通知让我检验一下')
  assert.equal(ctx.messages.at(-1).type, 'ask')
  assert.match(ctx.messages.at(-1).text, /单号/)
  assert.equal(calls.length, 0)
})

test('多个历史单据的“另一张”必须澄清，不能悄悄选最后一张', async () => {
  const { ctx, calls } = chat({ history: [created(), created({ ...NOTICE, noticeId: 40, noticeCode: 'AN20261031004' })] })
  await ctx.route('另一张到货通知让我检查一下')
  assert.equal(ctx.messages.at(-1).type, 'ask')
  assert.ok(ctx.messages.at(-1).options.some(o => o.includes('AN20261031003')))
  assert.ok(ctx.messages.at(-1).options.some(o => o.includes('AN20261031004')))
  assert.equal(calls.length, 0)
})

test('失效单据和权限不足不能用历史记录伪装成当前可检验单', async () => {
  const deleted = chat({ notice: null })
  await deleted.ctx.route('刚刚的到货通知让我检验一下')
  assert.equal(deleted.ctx.messages.at(-1).type, 'text')
  assert.match(deleted.ctx.messages.at(-1).text, /不存在|未找到/)
  const denied = chat({ denied: ['mes:wm:arrivalnotice:query'] })
  await denied.ctx.route('刚刚的到货通知让我检验一下')
  assert.match(denied.ctx.messages.at(-1).text, /权限/)
  assert.equal(denied.calls.length, 0)
})

test('恢复会话后仍能指向原单，清空对话后不再沿用旧单号', async () => {
  const { ctx } = chat()
  ctx.saveChat()
  ctx.messages = []
  ctx.restoreChat()
  await ctx.route('刚刚的到货通知让我检验一下')
  assert.equal(ctx.messages.at(-1).notice.noticeCode, 'AN20261031003')
  ctx.reset()
  await ctx.route('刚刚的到货通知让我检验一下')
  assert.equal(ctx.messages.at(-1).type, 'ask')
})

test('送检追问不触发新增或提交，打开原单携带确切ID', async () => {
  const { ctx, calls } = chat()
  await ctx.route('帮我提交刚才的到货通知进行检验')
  assert.equal(ctx.messages.at(-1).type, 'arrivalDetail')
  assert.ok(calls.every(c => c.method === 'get'))
  await ctx.openArrivalNotice(NOTICE)
  assert.equal(ctx.navigation.query.openNoticeId, '31')
  assert.equal(ctx.navigation.query.noticeCode, 'AN20261031003')
})

function businessPage(file, query, apis, denied = []) {
  const code = fs.readFileSync(file, 'utf8').match(/<script>([\s\S]*?)<\/script>/)[1]
    .replace(/^import .*$/mg, '').replace('export default', 'component =')
  const box = { component: null, require: name => name, ...apis }
  for (const match of fs.readFileSync(file, 'utf8').matchAll(/^import\s+(\w+)\s+from/gm)) box[match[1]] = {}
  vm.createContext(box)
  vm.runInContext(code, box)
  const ctx = { $route: { query }, $auth: { hasPermi: p => !denied.includes(p) }, resetForm() {} }
  Object.assign(ctx, box.component.data.call(ctx), box.component.methods)
  return { ctx, options: box.component }
}

test('从助手打开原到货通知时直接显示确切单据详情，复用页面时跟随新单号', async () => {
  const reads = []
  const { ctx, options } = businessPage('ktg-mes-ui/src/views/mes/wm/arrivalnotice/index.vue',
    { noticeCode: NOTICE.noticeCode, openNoticeId: '31' }, {
      listArrivalnotice: async params => { reads.push(params.noticeCode); return { rows: [], total: 0 } },
      getArrivalnotice: async id => ({ data: { ...NOTICE, noticeId: Number(id) } }),
      listItemrecpt: async () => ({ rows: [] })
    })
  options.created.call(ctx)
  await Promise.resolve()
  await Promise.resolve()
  assert.equal(ctx.form.noticeId, 31)
  assert.equal(ctx.open, true)
  assert.equal(ctx.optType, 'view')
  ctx.$route.query = { noticeCode: 'AN20261031004', openNoticeId: '40' }
  const watcher = options.watch['$route.query']
  ;(watcher.handler || watcher).call(ctx)
  await Promise.resolve()
  assert.equal(ctx.form.noticeId, 40)
  assert.equal(reads.at(-1), 'AN20261031004')
})

test('检验入口定位到关联IQC，不受列表默认的今日检测日期过滤', async () => {
  let params
  const { ctx, options } = businessPage('ktg-mes-ui/src/views/mes/qc/iqc/iqc.vue',
    { iqcCode: 'IQC40', openIqcId: '90' }, {
      listIqc: async query => { params = { ...query }; return { rows: [], total: 0 } },
      getIqc: async id => ({ data: { iqcId: Number(id), iqcCode: 'IQC40', status: 'PREPARE' } })
    })
  options.created.call(ctx)
  await Promise.resolve()
  assert.equal(params.iqcCode, 'IQC40')
  assert.equal(params.inspectDate, null)
  assert.equal(ctx.form.iqcId, 90)
  assert.equal(ctx.optType, 'edit')
})

test('快速切换到货通知后，旧请求晚返回不能覆盖新单据', async () => {
  const pending = new Map()
  const { ctx } = businessPage('ktg-mes-ui/src/views/mes/wm/arrivalnotice/index.vue',
    { noticeCode: NOTICE.noticeCode, openNoticeId: '31' }, {
      listArrivalnotice: async () => ({ rows: [], total: 0 }),
      getArrivalnotice: id => new Promise(resolve => pending.set(Number(id), resolve)),
      listItemrecpt: async () => ({ rows: [] })
    })
  ctx.applyAssistantRoute()
  ctx.$route.query = { noticeCode: 'AN20261031004', openNoticeId: '40' }
  ctx.applyAssistantRoute()
  pending.get(40)({ data: { ...NOTICE, noticeId: 40 } })
  await Promise.resolve()
  assert.equal(ctx.form.noticeId, 40)
  pending.get(31)({ data: NOTICE })
  await Promise.resolve()
  assert.equal(ctx.form.noticeId, 40)
})

test('打开另一IQC时立即移除旧表单，等待新数据期间不暴露旧单检测项', async () => {
  let resolve
  const { ctx } = businessPage('ktg-mes-ui/src/views/mes/qc/iqc/iqc.vue',
    { iqcCode: 'IQC41', openIqcId: '91' }, {
      listIqc: async () => ({ rows: [], total: 0 }),
      getIqc: () => new Promise(done => { resolve = done })
    })
  ctx.form = { iqcId: 90, iqcCode: 'IQC40' }
  ctx.open = true
  ctx.applyAssistantRoute()
  assert.equal(ctx.form.iqcId, null)
  resolve({ data: { iqcId: 91, iqcCode: 'IQC41', status: 'PREPARE' } })
  await Promise.resolve()
  assert.equal(ctx.form.iqcId, 91)
})

test('采购入库页面从缓存切回时重新读取第一页，并能定位关联单据', async () => {
  const calls = []
  const query = { recptCode: 'R20261031004', openRecptId: '80' }
  const { ctx, options } = businessPage('ktg-mes-ui/src/views/mes/wm/itemrecpt/index.vue', query, {
    listItemrecpt: async params => { calls.push({ ...params }); return { rows: [{ recptId: 80 }], total: 1 } },
    getItemrecpt: async id => ({ data: { recptId: Number(id), recptCode: 'R20261031004' } })
  })
  ctx.$route.path = '/mes/wm/itemrecpt'
  ctx.queryParams.pageNum = 3
  options.activated.call(ctx)
  await Promise.resolve()
  assert.equal(calls[0].pageNum, 1)
  assert.equal(calls[0].recptCode, 'R20261031004')
  assert.equal(ctx.form.recptId, 80)
})

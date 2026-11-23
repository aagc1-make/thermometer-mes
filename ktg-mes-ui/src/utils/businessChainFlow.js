const PRODUCT_CODES = ['TG-SF-001', 'TG-SF-002', 'TG-FG-001']
const RAW_CODES = ['TG-EL-001', 'TG-EL-002', 'TG-EL-003', 'TG-EL-004', 'TG-EL-005', 'TG-EL-006', 'TG-ST-001', 'TG-ST-002', 'TG-ST-003', 'TG-ST-004', 'TG-ST-005', 'TG-ST-006', 'TG-PK-001', 'TG-PK-002']
const REQUIRED_PERMISSIONS = [
  'mes:md:mditem:list', 'mes:md:vendor:list', 'mes:md:client:list', 'mes:md:workstation:list',
  'mes:wm:warehouse:list', 'mes:wm:location:list', 'mes:wm:area:list', 'mes:wm:wmstock:list', 'mes:wm:transaction:list', 'mes:wm:productproduce:list',
  ...['arrivalnotice', 'itemrecpt', 'issueheader', 'productrecpt', 'salesnotice', 'productsales'].reduce((all, name) => all.concat(['list', 'query', 'add', 'edit'].map(action => `mes:wm:${name}:${action}`)), []),
  ...['workorder', 'protask', 'feedback'].reduce((all, name) => all.concat(['list', 'query', 'add', 'edit'].map(action => `mes:pro:${name}:${action}`)), []),
  ...['iqc', 'ipqc', 'oqc'].reduce((all, name) => all.concat(['list', 'query', 'edit'].map(action => `mes:qc:${name}:${action}`)), [])
]
// 全流程贯通：本轮单据的统一标记与试产批次命名空间（单据清理与批次隔离都依赖这两个常量）。
const CHAIN_MARK = '全流程贯通'
const PILOT_PREFIX = 'PILOT-'
const CHAIN_AREA_API = '/mes/wm/area'
const CHAIN_AREA_PATH = '/mes/wm/area/index'
const CHAIN_AREA_PERMISSION = 'mes:wm:area:add'
const same = (a, b) => String(a) === String(b)
const number = v => Number(v || 0)
const close = (a, b) => Math.abs(number(a) - number(b)) < 0.00001
function requireValue(value, message) { if (!value) throw new Error(message); return value }
function dateTime(offset = 0) {
  const d = new Date(Date.now() + offset * 86400000)
  const p = v => String(v).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}
function allocateBatchStocks(stocks, itemId, batches, quantity, warehouseCode) {
  requireValue(batches && batches.length, '本次物料尚未形成可用批次')
  let remaining = number(quantity)
  const result = []
  for (const stock of stocks) {
    if (!same(stock.itemId, itemId) || stock.warehouseCode !== warehouseCode || !batches.includes(stock.batchCode) || stock.frozenFlag === 'Y' || number(stock.quantityOnhand) <= 0) continue
    const take = Math.min(remaining, number(stock.quantityOnhand))
    if (take > 0) result.push({ ...stock, quantity: take })
    remaining -= take
    if (remaining < 0.00001) break
  }
  requireValue(remaining < 0.00001, `本次批次库存不足：物料 ${itemId}，缺少 ${remaining}`)
  return result
}

function createChainApi(ctx) {
  const confirmHead = (url, head) => {
    if (!ctx.confirmWrite) return
    const name = url.split('/').pop()
    const field = ({ arrivalnotice: 'noticeId', salesnotice: 'noticeId', workorder: 'workorderId', itemrecpt: 'recptId', productrecpt: 'recptId', issueheader: 'issueId', productsales: 'salesId', iqc: 'iqcId', ipqc: 'ipqcId', oqc: 'oqcId' })[name]
    if (!field || !head[field]) return
    const order = name === 'productsales' ? ['PREPARE', 'UNSTOCK', 'UNCHECK', 'UNSHIPPING', 'UNEXECUTE', 'FINISHED'] : name === 'workorder' || name === 'salesnotice' ? ['PREPARE', 'CONFIRMED', 'FINISHED'] : ['PREPARE', 'APPROVING', 'APPROVED', 'FINISHED']
    ctx.confirmWrite(url, data => same(data[field], head[field]) && data.status && (data.status === head.status || head.status === 'FINISHED' || (order.includes(data.status) && order.indexOf(head.status) > order.indexOf(data.status)) || (name === 'productsales' && data.status === 'UNSHIPPING' && head.status === 'UNCHECK')), 'put')
    if (head.status === 'FINISHED') ctx.confirmWrite(`${url}/${head[field]}`, () => true, 'put')
  }
  const get = async (url, params) => ctx.call('核对 ' + url, { url, method: 'get', params })
  const write = async (url, data, label, method = 'post') => ctx.call(label, { url, method, data })
  const list = async (url, params = {}) => {
    const rows = []; let pageNum = 1
    while (true) {
      const r = await get(url + '/list', { ...params, pageNum, pageSize: 200 })
      const page = r.rows || []
      page.forEach(row => confirmHead(url, row))
      rows.push(...page)
      if (!page.length || (r.total !== undefined ? rows.length >= Number(r.total) : page.length < 200)) return rows
      requireValue(pageNum++ < 500, '查询记录过多，请缩小查询范围')
    }
  }
  const detail = async (url, id) => {
    const head = requireValue((await get(`${url}/${id}`)).data, '原单据不存在：' + url + '/' + id)
    confirmHead(url, head)
    return head
  }
  const ensure = async (url, params, match, data, label) => {
    let found = (await list(url, params)).filter(match)
    requireValue(found.length <= 1, label + '存在多张同来源单据，请先核对')
    if (!found.length) {
      await write(url, data, '创建' + label)
      found = (await list(url, params)).filter(match)
    }
    if (found.length === 1 && ctx.confirmWrite) ctx.confirmWrite(url, match)
    return requireValue(found.length === 1 && found[0], label + '创建结果未确认，请核对原单据后继续')
  }
  const status = async (url, id, target, allowed, patch = {}) => {
    const head = await detail(url, id)
    if (head.status === target || head.status === 'FINISHED') {
      if (ctx.confirmWrite) ctx.confirmWrite(url, data => data.status === target && Object.keys(head).some(k => /Id$/.test(k) && same(head[k], id) && same(data[k], id)), 'put')
      return head
    }
    requireValue(allowed.includes(head.status), `单据 ${id} 状态为 ${head.status}，不能推进到 ${target}`)
    await write(url, { ...head, ...patch, status: target }, `提交 ${head.noticeCode || head.recptCode || head.workorderCode || head.issueCode || head.salesCode || id} → ${target}`, 'put')
    return detail(url, id)
  }
  const execute = async (url, id) => {
    const head = await detail(url, id)
    if (head.status !== 'FINISHED') await write(`${url}/${id}`, undefined, '执行过账 ' + (head.recptCode || head.issueCode || head.salesCode || id), 'put')
    const result = await detail(url, id)
    requireValue(result.status === 'FINISHED', '执行结果尚未确认，请核对原单据')
    if (ctx.confirmWrite) ctx.confirmWrite(`${url}/${id}`, () => true, 'put')
    return result
  }
  return { get, write, list, detail, ensure, status, execute }
}

function itemFields(item) {
  return { itemId: item.itemId, itemCode: item.itemCode, itemName: item.itemName, specification: item.specification, unitOfMeasure: item.unitOfMeasure, unitName: item.unitName }
}
function partnerFields(partner, type) {
  return { [`${type}Id`]: partner[`${type}Id`], [`${type}Code`]: partner[`${type}Code`], [`${type}Name`]: partner[`${type}Name`] }
}
function addBatches(c, itemId, batches) {
  c.batches[itemId] = Array.from(new Set((c.batches[itemId] || []).concat(batches.filter(Boolean))))
}
function doc(ctx, type, head, idField, codeField, path) {
  const query = { [codeField]: head[codeField] }
  if (type === '生产工单') query.openWorkorderId = String(head[idField])
  if (type === '到货通知') query.openNoticeId = String(head[idField])
  if (type === '采购入库') query.openRecptId = String(head[idField])
  if (type === '来料检验') query.openIqcId = String(head[idField])
  ctx.document(type, head[idField], head[codeField], { path, query }, head.status)
}
async function inspect(ctx, type, inspection, quantity) {
  const api = createChainApi(ctx); const url = `/mes/qc/${type}`; const idField = `${type}Id`
  let d = await api.detail(url, inspection[idField])
  if (d.status !== 'FINISHED') {
    await api.write(url, { ...d, quantityCheck: quantity, quantityQualified: quantity, quantityUnqualified: 0, checkResult: 'ACCEPT', inspectDate: dateTime(), status: 'FINISHED' }, `完成 ${d[`${type}Code`]}：判定合格，整批放行`, 'put')
    d = await api.detail(url, inspection[idField])
  }
  requireValue(d.status === 'FINISHED' && close(d.quantityQualified, quantity) && number(d.quantityUnqualified) === 0, '检验未按本轮数量放行：' + d[`${type}Code`])
  if (ctx.confirmWrite) ctx.confirmWrite(url, data => same(data[idField], d[idField]) && data.status === 'FINISHED', 'put')
  doc(ctx, { iqc: '来料检验', ipqc: '过程检验', oqc: '出货检验' }[type], d, idField, `${type}Code`, `/mes/qc/${type}`)
}
function chainAreaCode(warehouseCode, itemCode) { return `AR-${warehouseCode}-${itemCode}` }
function activeLocation(c, warehouseId) {
  return c.locations.find(l => same(l.warehouseId, warehouseId) && l.enableFlag !== 'N' && l.frozenFlag !== 'Y')
}
function areaConflict(c, area, itemId, reservations) {
  const stock = c.initialStock.filter(s => same(s.areaId, area.areaId) && number(s.quantityOnhand) > 0)
  const occupied = stock.concat(reservations.filter(s => same(s.areaId, area.areaId)))
  if (area.productMixing !== 'Y') {
    const foreign = occupied.find(s => !same(s.itemId, itemId))
    if (foreign) return `已存放物料 ${foreign.itemCode || foreign.itemId}，库位不允许物料混放`
  }
  if (area.batchMixing !== 'Y' && occupied.length) {
    const line = stock[0]
    return line ? `已存放批次 ${line.batchCode || line.batchId}，库位不允许批次混放` : '本轮已占用该库位，库位不允许批次混放'
  }
  return ''
}
function slotOf(c, area) {
  const loc = c.locations.find(l => same(l.locationId, area.locationId))
  const wh = loc && c.warehouses.find(w => same(w.warehouseId, loc.warehouseId))
  requireValue(loc && wh, '库位缺少库区或仓库配置：' + (area.areaCode || area.areaId))
  return { warehouseId: wh.warehouseId, warehouseCode: wh.warehouseCode, warehouseName: wh.warehouseName, locationId: loc.locationId, locationCode: loc.locationCode, locationName: loc.locationName, areaId: area.areaId, areaCode: area.areaCode, areaName: area.areaName }
}
// 只返回符合库位混放规则的位置；不可用原因逐条保留，供失败说明使用。
function findSlot(c, warehouseCode, itemId, reservations) {
  const wh = requireValue(c.warehouses.find(w => w.warehouseCode === warehouseCode && w.enableFlag !== 'N' && w.frozenFlag !== 'Y'), '仓库不可用：' + warehouseCode)
  const blocked = []
  for (const loc of c.locations.filter(l => same(l.warehouseId, wh.warehouseId) && l.enableFlag !== 'N' && l.frozenFlag !== 'Y')) {
    for (const area of c.areas.filter(a => same(a.locationId, loc.locationId) && a.enableFlag !== 'N' && a.frozenFlag !== 'Y')) {
      const conflict = areaConflict(c, area, itemId, reservations)
      if (!conflict) return { slot: slotOf(c, area), area, warehouse: wh }
      blocked.push(`${area.areaCode}（${conflict}）`)
    }
  }
  return { blocked, warehouse: wh }
}
// 现有库位都放不下本次批次时，沿用或建立本仓库的物料专用库位：
// 只允许存放该物料、允许多个批次，既不混放历史物料，也不消耗历史批次。
async function ensureSlot(ctx, api, warehouseCode, itemCode, reservations, hasPermission) {
  const c = ctx.state.context; const item = c.items[itemCode]; const areaCode = chainAreaCode(warehouseCode, itemCode)
  const found = findSlot(c, warehouseCode, item.itemId, reservations)
  if (found.slot) {
    if (found.area.areaCode === areaCode && ctx.confirmWrite) ctx.confirmWrite(CHAIN_AREA_API, data => !!data && data.areaCode === areaCode, 'post')
    reservations.push({ areaId: found.area.areaId, itemId: item.itemId })
    return found.slot
  }
  const detail = found.blocked.length ? '：' + found.blocked.join('；') : '：仓库内没有启用的库位'
  requireValue(!hasPermission || hasPermission(CHAIN_AREA_PERMISSION), `仓库 ${warehouseCode} 没有可存放本次批次的库位${detail}。自动建立物料专用库位需要权限 ${CHAIN_AREA_PERMISSION}`)
  const location = requireValue(activeLocation(c, found.warehouse.warehouseId), `仓库 ${warehouseCode} 没有可用库区，无法建立物料专用库位`)
  let area = c.areas.find(a => a.areaCode === areaCode)
  if (!area) {
    area = await api.ensure(CHAIN_AREA_API, { areaCode }, a => a.areaCode === areaCode, { areaCode, areaName: `物料专用库位（${item.itemName}）`, locationId: location.locationId, enableFlag: 'Y', frozenFlag: 'N', productMixing: 'N', batchMixing: 'Y', remark: '自动建立：只存放本项物料，允许存放多个批次' }, '物料专用库位 ' + areaCode)
    if (!c.areas.some(a => same(a.areaId, area.areaId))) c.areas.push(area)
    ctx.document('专用库位', area.areaId, area.areaCode, { path: CHAIN_AREA_PATH, query: { locationId: location.locationId } }, '已启用')
    ctx.checkpoint()
  }
  requireValue(area.enableFlag !== 'N' && area.frozenFlag !== 'Y', `物料专用库位 ${area.areaCode} 已停用或冻结，请在库位设置中恢复`)
  const conflict = areaConflict(c, area, item.itemId, reservations)
  requireValue(!conflict, `物料专用库位 ${area.areaCode} 当前不可用：${conflict}`)
  reservations.push({ areaId: area.areaId, itemId: item.itemId })
  return slotOf(c, area)
}

async function prepare(ctx, hasPermission) {
  const missing = hasPermission ? REQUIRED_PERMISSIONS.filter(p => !hasPermission(p)) : []
  requireValue(!missing.length, '当前账号缺少执行权限：' + missing.join('、'))
  const api = createChainApi(ctx); const c = ctx.state.context
  const items = await api.list('/mes/md/mditem')
  const absent = RAW_CODES.concat(PRODUCT_CODES).filter(code => !items.some(i => i.itemCode === code && i.enableFlag !== 'N'))
  requireValue(!absent.length, '缺少启用的标准物料配置：' + absent.join('、'))
  c.items = Object.fromEntries(items.map(i => [i.itemCode, i])); c.quantities = { 'TG-FG-001': 10 }; c.boms = {}; c.routes = {}; c.routeBoms = {}; c.batches = {}
  for (const code of PRODUCT_CODES.slice().reverse()) {
    const item = c.items[code]
    c.boms[code] = (await api.list('/mes/md/bom', { itemId: item.itemId })).filter(b => same(b.itemId, item.itemId) && b.enableFlag !== 'N')
    requireValue(c.boms[code].length, '产品缺少有效 BOM：' + code)
    c.routes[code] = (await api.get(`/mes/pro/routeprocess/listProductProcess/${item.itemId}`)).data || []
    requireValue(c.routes[code].length, '产品缺少工艺路线：' + code)
    c.routes[code].sort((a, b) => number(a.orderNum) - number(b.orderNum))
    c.routeBoms[code] = await api.list('/mes/pro/routeproductbom', { productId: item.itemId, routeId: c.routes[code][0].routeId })
    requireValue(c.routeBoms[code].length, '产品缺少工序投料配置：' + code)
    if (code === 'TG-FG-001') for (const child of PRODUCT_CODES.slice(0, 2)) {
      const line = c.boms[code].find(b => b.bomItemCode === child)
      c.quantities[child] = number(requireValue(line, '整机 BOM 缺少半成品：' + child).quantity) * 10
      requireValue(c.quantities[child] > 0, '半成品 BOM 数量必须大于 0')
    }
  }
  c.workstations = await api.list('/mes/md/workstation')
  for (const code of PRODUCT_CODES) for (const process of c.routes[code]) {
    requireValue(c.workstations.some(s => same(s.processId, process.processId) && s.enableFlag !== 'N'), '工序缺少可用工作站：' + process.processName)
  }
  const vendors = await api.list('/mes/md/vendor'); const clients = await api.list('/mes/md/client')
  c.vendors = ['V-TG-001', 'V-TG-002', 'V-TG-003'].map(code => requireValue(vendors.find(v => v.vendorCode === code && v.enableFlag !== 'N'), '缺少供应商：' + code))
  c.client = requireValue(clients.find(v => v.clientCode === 'C-TG-001' && v.enableFlag !== 'N'), '缺少客户 C-TG-001')
  c.warehouses = await api.list('/mes/wm/warehouse'); c.locations = await api.list('/mes/wm/location'); c.areas = await api.list('/mes/wm/area'); c.initialStock = await api.list('/mes/wm/wmstock')
  c.rawQuantities = {}
  for (const code of PRODUCT_CODES) for (const bom of c.boms[code]) {
    if (PRODUCT_CODES.includes(bom.bomItemCode)) continue
    requireValue(RAW_CODES.includes(bom.bomItemCode) && number(bom.quantity) > 0, 'BOM 含当前流程不支持的物料或数量：' + bom.bomItemCode)
    c.rawQuantities[bom.bomItemCode] = number(c.rawQuantities[bom.bomItemCode]) + number(bom.quantity) * c.quantities[code]
  }
  const reservations = []; c.slots = {}
  for (const code of Object.keys(c.rawQuantities)) c.slots[code] = await ensureSlot(ctx, api, 'WH-RAW', code, reservations, hasPermission)
  c.slots['TG-FG-001'] = await ensureSlot(ctx, api, 'WH-FG', 'TG-FG-001', reservations, hasPermission)
  c.arrivals = []; c.workorders = {}; c.tasks = {}
  ctx.checkpoint()
}

async function arrivals(ctx) {
  const api = createChainApi(ctx); const c = ctx.state.context
  const groups = [[], [], []]
  for (const code of Object.keys(c.rawQuantities)) groups[code.startsWith('TG-PK') ? 2 : code.startsWith('TG-ST') && code !== 'TG-ST-004' ? 1 : 0].push(code)
  for (let i = 0; i < groups.length; i++) {
    const noticeCode = `${ctx.state.runId}-A${i + 1}`
    let head = await api.ensure('/mes/wm/arrivalnotice', { noticeCode }, h => h.noticeCode === noticeCode, { noticeCode, noticeName: '首批到货-' + ['电控件', '结构件', '包装材料'][i], ...partnerFields(c.vendors[i], 'vendor'), arrivalDate: dateTime(), poCode: PILOT_PREFIX + ctx.state.runId, remark: CHAIN_MARK, status: 'PREPARE' }, '到货通知 ' + noticeCode)
    if (head.status === 'PREPARE') {
      for (const code of groups[i]) {
        const item = c.items[code]
        await api.ensure('/mes/wm/arrivalnoticeline', { noticeId: head.noticeId }, l => same(l.noticeId, head.noticeId) && same(l.itemId, item.itemId), { noticeId: head.noticeId, ...itemFields(item), quantityArrival: c.rawQuantities[code], iqcCheck: 'Y' }, '到货行 ' + code)
      }
      head = await api.status('/mes/wm/arrivalnotice', head.noticeId, 'APPROVING', ['PREPARE'])
    }
    c.arrivals[i] = head; ctx.checkpoint()
    doc(ctx, '到货通知', head, 'noticeId', 'noticeCode', '/mes/wm/arrivalnotice')
  }
}
async function incomingInspection(ctx) {
  const api = createChainApi(ctx); const c = ctx.state.context
  for (const notice of c.arrivals) {
    const lines = await api.list('/mes/wm/arrivalnoticeline', { noticeId: notice.noticeId })
    const checks = await api.list('/mes/qc/iqc', { sourceDocId: notice.noticeId, sourceDocType: 'ARRIVAL_NOTICE' })
    for (const line of lines) {
      const iqc = requireValue(checks.find(q => same(q.sourceDocId, notice.noticeId) && same(q.sourceLineId, line.lineId)), '到货通知尚未生成对应 IQC 草稿：' + line.itemCode)
      await inspect(ctx, 'iqc', iqc, number(line.quantityArrival))
    }
  }
}
async function shelf(ctx, detailPath, headIdField, headId, line, slot, quantity) {
  const api = createChainApi(ctx)
  const existing = (await api.list(detailPath, { [headIdField]: headId, lineId: line.lineId })).filter(d => same(d.lineId, line.lineId))
  const total = existing.reduce((sum, d) => sum + number(d.quantity), 0)
  if (close(total, quantity)) {
    if (ctx.confirmWrite) ctx.confirmWrite(detailPath, data => same(data.lineId, line.lineId) && existing.some(d => same(d.batchCode, data.batchCode) && close(d.quantity, data.quantity)))
    return
  }
  requireValue(total === 0, '存在部分上架明细，请核对后手工补齐：' + line.itemCode)
  requireValue(line.batchCode && line.batchId, '入库行未生成来源批次：' + line.itemCode)
  await api.write(detailPath, { ...itemFields(line), [headIdField]: headId, lineId: line.lineId, quantity, batchId: line.batchId, batchCode: line.batchCode, ...slot }, '上架 ' + line.itemCode + ' / ' + line.batchCode)
}
async function purchaseReceipt(ctx) {
  const api = createChainApi(ctx); const c = ctx.state.context
  for (const notice of c.arrivals) {
    let head = requireValue((await api.list('/mes/wm/itemrecpt', { noticeId: notice.noticeId })).find(r => same(r.noticeId, notice.noticeId)), 'IQC 完成后未找到采购入库草稿：' + notice.noticeCode)
    const lines = (await api.list('/mes/wm/itemrecptline', { recptId: head.recptId })).filter(l => same(l.recptId, head.recptId))
    requireValue(lines.length, '采购入库草稿无明细')
    if (head.status !== 'FINISHED') {
      if (head.status === 'PREPARE') head = await api.status('/mes/wm/itemrecpt', head.recptId, 'APPROVING', ['PREPARE'])
      for (const line of lines) await shelf(ctx, '/mes/wm/itemreceptdetail', 'recptId', head.recptId, line, c.slots[line.itemCode], number(line.quantityRecived))
      await api.status('/mes/wm/itemrecpt', head.recptId, 'APPROVED', ['APPROVING'])
      head = await api.execute('/mes/wm/itemrecpt', head.recptId)
    }
    for (const line of lines) addBatches(c, line.itemId, [line.batchCode])
    ctx.checkpoint(); doc(ctx, '采购入库', head, 'recptId', 'recptCode', '/mes/wm/itemrecpt')
  }
}
async function workorders(ctx) {
  const api = createChainApi(ctx); const c = ctx.state.context; const item = c.items['TG-FG-001']; const code = `${ctx.state.runId}-MO`
  const parent = await api.ensure('/mes/pro/workorder', { workorderCode: code }, w => w.workorderCode === code, { workorderCode: code, workorderName: item.itemName + ' 首批生产', productId: item.itemId, productCode: item.itemCode, productName: item.itemName, productSpc: item.specification, unitOfMeasure: item.unitOfMeasure, unitName: item.unitName, quantity: 10, workorderType: 'SELF', parentId: 0, ancestors: '0', orderSource: 'ORDER', sourceCode: ctx.state.runId, requestDate: dateTime(7), ...partnerFields(c.client, 'client'), remark: CHAIN_MARK }, '整机工单')
  c.workorders['TG-FG-001'] = parent
  const boms = await api.list('/mes/pro/workorderbom', { workorderId: parent.workorderId })
  const childLines = PRODUCT_CODES.slice(0, 2).map(child => requireValue(boms.find(b => b.itemCode === child), '工单 BOM 未展开半成品：' + child))
  let children = (await api.list('/mes/pro/workorder', { parentId: parent.workorderId })).filter(w => same(w.parentId, parent.workorderId))
  if (childLines.some(b => !children.some(w => same(w.productId, b.itemId)))) {
    await api.write('/mes/pro/workorder/subBatch', { parentId: parent.workorderId, bomLineIds: childLines.map(b => b.lineId) }, '按 BOM 自动生成半成品子工单')
    children = (await api.list('/mes/pro/workorder', { parentId: parent.workorderId })).filter(w => same(w.parentId, parent.workorderId))
  }
  for (const child of PRODUCT_CODES.slice(0, 2)) c.workorders[child] = requireValue(children.find(w => w.productCode === child), '未找到关联子工单：' + child)
  if (ctx.confirmWrite) ctx.confirmWrite('/mes/pro/workorder/subBatch', data => same(data.parentId, parent.workorderId) && childLines.every(b => children.some(w => w.attr1 === `AUTO:SUB_WORKORDER:${parent.workorderId}:${b.lineId}`)))
  for (const product of PRODUCT_CODES) {
    let wo = c.workorders[product]
    if (wo.status === 'PREPARE') wo = await api.status('/mes/pro/workorder', wo.workorderId, 'CONFIRMED', ['PREPARE'], { requestDate: dateTime(7) })
    c.workorders[product] = wo; ctx.checkpoint(); doc(ctx, '生产工单', wo, 'workorderId', 'workorderCode', '/mes/pro/workorder')
  }
}
async function schedule(ctx) {
  const api = createChainApi(ctx); const c = ctx.state.context
  for (const code of PRODUCT_CODES) {
    const wo = c.workorders[code]
    for (const process of c.routes[code]) {
      const station = c.workstations.find(s => same(s.processId, process.processId) && s.enableFlag !== 'N')
      await api.ensure('/mes/pro/protask', { workorderId: wo.workorderId }, t => same(t.workorderId, wo.workorderId) && same(t.processId, process.processId), { workorderId: wo.workorderId, workstationId: station.workstationId, routeId: process.routeId, processId: process.processId, quantity: wo.quantity, duration: 1, startTime: dateTime(), requestDate: dateTime(7), colorCode: process.colorCode || '#2563eb', isCheck: process.isCheck, remark: CHAIN_MARK }, '排产 ' + process.processName)
    }
    c.tasks[code] = (await api.list('/mes/pro/protask', { workorderId: wo.workorderId })).filter(t => same(t.workorderId, wo.workorderId)); ctx.checkpoint()
  }
}
async function issue(ctx, code) {
  const api = createChainApi(ctx); const c = ctx.state.context; const wo = c.workorders[code]
  let head = requireValue((await api.list('/mes/wm/issueheader', { workorderId: wo.workorderId })).find(h => same(h.workorderId, wo.workorderId)), '排产后未找到领料草稿：' + wo.workorderCode)
  if (head.status !== 'FINISHED') {
    if (head.status === 'PREPARE') head = await api.status('/mes/wm/issueheader', head.issueId, 'APPROVING', ['PREPARE'])
    const lines = await api.list('/mes/wm/issueline', { issueId: head.issueId })
    requireValue(lines.length, '领料草稿没有明细')
    for (const line of lines) {
      const existing = (await api.list('/mes/wm/issuedetail', { issueId: head.issueId, lineId: line.lineId })).filter(d => same(d.lineId, line.lineId))
      requireValue(existing.every(d => (c.batches[line.itemId] || []).includes(d.batchCode)), '已有领料明细使用了非本次批次')
      if (ctx.confirmWrite) ctx.confirmWrite('/mes/wm/issuedetail', data => same(data.lineId, line.lineId) && existing.some(d => same(d.materialStockId, data.materialStockId) && same(d.batchCode, data.batchCode) && close(d.quantity, data.quantity)))
      const done = existing.reduce((sum, d) => sum + number(d.quantity), 0)
      requireValue(done <= number(line.quantityIssued), '已有拣货数量超出领料需求')
      if (close(done, line.quantityIssued)) continue
      const stocks = await api.list('/mes/wm/wmstock', { itemId: line.itemId, warehouseCode: 'WH-RAW' })
      const used = Object.fromEntries(existing.map(d => [String(d.materialStockId), number(d.quantity)]))
      const available = stocks.map(s => ({ ...s, quantityOnhand: number(s.quantityOnhand) - number(used[s.materialStockId]) }))
      for (const allocation of allocateBatchStocks(available, line.itemId, c.batches[line.itemId], number(line.quantityIssued) - done, 'WH-RAW')) {
        const { materialStockId, batchId, batchCode, quantity, warehouseId, warehouseCode, warehouseName, locationId, locationCode, locationName, areaId, areaCode, areaName } = allocation
        await api.write('/mes/wm/issuedetail', { issueId: head.issueId, lineId: line.lineId, ...itemFields(line), materialStockId, batchId, batchCode, quantity, warehouseId, warehouseCode, warehouseName, locationId, locationCode, locationName, areaId, areaCode, areaName }, '拣选本次批次 ' + line.itemCode + ' / ' + batchCode)
      }
    }
    requireValue((await api.get(`/mes/wm/issueheader/checkQuantity/${head.issueId}`)).data === true, '领料数量核对未通过')
    await api.status('/mes/wm/issueheader', head.issueId, 'APPROVED', ['APPROVING'])
    head = await api.execute('/mes/wm/issueheader', head.issueId)
  }
  doc(ctx, '生产领料', head, 'issueId', 'issueCode', '/mes/wm/issue')
}
async function production(ctx, code) {
  const api = createChainApi(ctx); const c = ctx.state.context; const wo = c.workorders[code]; const qty = number(wo.quantity)
  await issue(ctx, code)
  for (const process of c.routes[code]) {
    const task = requireValue(c.tasks[code].find(t => same(t.processId, process.processId)), '排产任务不存在')
    let fb = await api.ensure('/mes/pro/feedback', { taskId: task.taskId }, f => same(f.taskId, task.taskId) && same(f.workorderId, wo.workorderId), { feedbackType: 'SELF', feedbackChannel: 'PC', workorderId: wo.workorderId, workorderCode: wo.workorderCode, workorderName: wo.workorderName, taskId: task.taskId, taskCode: task.taskCode, workstationId: task.workstationId, routeId: process.routeId, processId: process.processId, ...itemFields(c.items[code]), quantity: qty, quantityFeedback: qty, quantityQualified: process.isCheck === 'Y' ? 0 : qty, quantityUnquanlified: 0, quantityUncheck: 0, isCheck: process.isCheck, feedbackTime: dateTime(), lotNumber: PILOT_PREFIX + ctx.state.runId, remark: CHAIN_MARK, status: 'PREPARE' }, '报工 ' + process.processName)
    if (['PREPARE', 'APPROVING'].includes(fb.status)) {
      const scope = {}
      for (const bom of c.routeBoms[code].filter(b => same(b.processId, process.processId))) scope[bom.itemId] = requireValue(c.batches[bom.itemId] && c.batches[bom.itemId].length && c.batches[bom.itemId], '消耗物料缺少本次来源批次：' + bom.itemCode)
      await api.write(`/mes/pro/feedback/${fb.recordId}/with-batches`, scope, '执行报工 ' + process.processName + '（限定本次批次）', 'put')
      fb = await api.detail('/mes/pro/feedback', fb.recordId)
    }
    if (['UNCHECK', 'FINISHED'].includes(fb.status) && ctx.confirmWrite) ctx.confirmWrite(`/mes/pro/feedback/${fb.recordId}/with-batches`, () => true, 'put')
    if (fb.status === 'UNCHECK') {
      const checks = (await api.list('/mes/qc/ipqc', { workorderId: wo.workorderId, taskId: task.taskId })).filter(q => same(q.taskId, task.taskId) && same(q.workorderId, wo.workorderId))
      requireValue(checks.length, '关键工序未生成 IPQC 草稿：' + process.processName)
      for (const check of checks) await inspect(ctx, 'ipqc', check, number(check.quantityCheck) || qty)
      fb = await api.detail('/mes/pro/feedback', fb.recordId)
    }
    requireValue(fb.status === 'FINISHED', '报工尚未放行：' + process.processName)
    doc(ctx, '生产报工', fb, 'recordId', 'feedbackCode', '/mes/pro/feedback')
    const produces = (await api.list('/mes/wm/productproduce', { feedbackId: fb.recordId })).filter(p => same(p.feedbackId, fb.recordId))
    for (const produce of produces) {
      const lines = (await api.list('/mes/wm/productproduceline', { recordId: produce.recordId })).filter(l => same(l.recordId, produce.recordId) && l.qualityStatus === 'OK')
      for (const line of lines) addBatches(c, line.itemId, [line.batchCode])
    }
    ctx.checkpoint()
  }
  let finished = await api.detail('/mes/pro/workorder', wo.workorderId)
  if (finished.status !== 'FINISHED') {
    await api.write(`/mes/pro/workorder/finish/${wo.workorderId}`, undefined, '完成工单 ' + wo.workorderCode, 'put')
    finished = await api.detail('/mes/pro/workorder', wo.workorderId)
  }
  requireValue(finished.status === 'FINISHED' && close(finished.quantityProduced, qty), '工单完工数量未达到计划')
  if (ctx.confirmWrite) ctx.confirmWrite(`/mes/pro/workorder/finish/${wo.workorderId}`, () => true, 'put')
  c.workorders[code] = finished; ctx.checkpoint(); doc(ctx, '生产工单', finished, 'workorderId', 'workorderCode', '/mes/pro/workorder')
}
async function productReceipt(ctx) {
  const api = createChainApi(ctx); const c = ctx.state.context; const wo = c.workorders['TG-FG-001']
  const heads = (await api.list('/mes/wm/productrecpt', { workorderId: wo.workorderId })).filter(h => same(h.workorderId, wo.workorderId))
  requireValue(heads.length === 1, '整机末工序完成后未生成唯一的产品入库草稿')
  let head = heads[0]
  const lines = (await api.list('/mes/wm/productrecptline', { recptId: head.recptId })).filter(l => same(l.recptId, head.recptId))
  requireValue(lines.length && close(lines.reduce((s, l) => s + number(l.quantityRecived), 0), 10), '产品入库来源数量不等于 10')
  if (head.status !== 'FINISHED') {
    if (head.status === 'PREPARE') head = await api.status('/mes/wm/productrecpt', head.recptId, 'APPROVING', ['PREPARE'])
    for (const line of lines) {
      requireValue((c.batches[line.itemId] || []).includes(line.batchCode), '产品入库批次不属于本次产出')
      await shelf(ctx, '/mes/wm/productrecptdetail', 'recptId', head.recptId, line, c.slots['TG-FG-001'], number(line.quantityRecived))
    }
    requireValue((await api.get(`/mes/wm/productrecpt/checkQuantity/${head.recptId}`)).data === true, '产品入库上架数量未核对通过')
    await api.status('/mes/wm/productrecpt', head.recptId, 'APPROVED', ['APPROVING'])
    head = await api.execute('/mes/wm/productrecpt', head.recptId)
  }
  c.productReceipt = head; c.finishedBatches = lines.map(l => l.batchCode); ctx.checkpoint()
  doc(ctx, '产品入库', head, 'recptId', 'recptCode', '/mes/wm/productrecpt')
}
async function sales(ctx) {
  const api = createChainApi(ctx); const c = ctx.state.context; const item = c.items['TG-FG-001']; const code = `${ctx.state.runId}-SN`
  let notice = await api.ensure('/mes/wm/salesnotice', { noticeCode: code }, n => n.noticeCode === code, { noticeCode: code, noticeName: '首批发货-测温枪 6 台', ...partnerFields(c.client, 'client'), salesDate: dateTime(), status: 'PREPARE', remark: CHAIN_MARK }, '发货通知')
  if (notice.status === 'PREPARE') {
    requireValue(c.finishedBatches.length === 1, '销售发货需要唯一成品来源批次')
    const stocks = await api.list('/mes/wm/wmstock', { itemId: item.itemId, warehouseCode: 'WH-FG' })
    const stock = allocateBatchStocks(stocks, item.itemId, c.finishedBatches, 6, 'WH-FG')[0]
    await api.ensure('/mes/wm/salesnoticeline', { noticeId: notice.noticeId }, l => same(l.noticeId, notice.noticeId) && same(l.itemId, item.itemId), { noticeId: notice.noticeId, ...itemFields(item), quantitySales: 6, oqcCheck: 'Y', batchId: stock.batchId, batchCode: stock.batchCode }, '发货明细')
    notice = await api.status('/mes/wm/salesnotice', notice.noticeId, 'CONFIRMED', ['PREPARE'])
  }
  doc(ctx, '发货通知', notice, 'noticeId', 'noticeCode', '/mes/wm/salesnotice')
  let head = requireValue((await api.list('/mes/wm/productsales', { noticeId: notice.noticeId })).find(h => same(h.noticeId, notice.noticeId)), '发货通知未生成销售出库草稿')
  if (head.status === 'PREPARE') head = await api.status('/mes/wm/productsales', head.salesId, 'UNSTOCK', ['PREPARE'])
  const lines = (await api.list('/mes/wm/productsalesline', { salesId: head.salesId })).filter(l => same(l.salesId, head.salesId))
  requireValue(lines.length === 1 && close(lines[0].quantitySales, 6), '销售出库明细与本次发货数量不一致')
  if (head.status === 'UNSTOCK') {
    for (const line of lines) {
      const details = (await api.list('/mes/wm/productsalesdetail', { salesId: head.salesId, lineId: line.lineId })).filter(d => same(d.lineId, line.lineId))
      requireValue(details.every(d => c.finishedBatches.includes(d.batchCode)), '销售拣货含其他来源批次')
      if (ctx.confirmWrite) ctx.confirmWrite('/mes/wm/productsalesdetail', data => same(data.lineId, line.lineId) && details.some(d => same(d.materialStockId, data.materialStockId) && same(d.batchCode, data.batchCode) && close(d.quantity, data.quantity)))
      const done = details.reduce((s, d) => s + number(d.quantity), 0)
      requireValue(done <= 6, '已有销售拣货超出 6 台')
      if (close(done, 6)) continue
      const stocks = await api.list('/mes/wm/wmstock', { itemId: line.itemId, warehouseCode: 'WH-FG' })
      const available = stocks.map(s => ({ ...s, quantityOnhand: number(s.quantityOnhand) - details.filter(d => same(d.materialStockId, s.materialStockId)).reduce((sum, d) => sum + number(d.quantity), 0) }))
      for (const stock of allocateBatchStocks(available, line.itemId, c.finishedBatches, 6 - done, 'WH-FG')) {
        const { materialStockId, batchId, batchCode, quantity, warehouseId, warehouseCode, warehouseName, locationId, locationCode, locationName, areaId, areaCode, areaName } = stock
        await api.write('/mes/wm/productsalesdetail', { salesId: head.salesId, lineId: line.lineId, ...itemFields(line), materialStockId, batchId, batchCode, quantity, warehouseId, warehouseCode, warehouseName, locationId, locationCode, locationName, areaId, areaCode, areaName }, '销售拣选本次成品批次 ' + batchCode)
      }
    }
    requireValue((await api.get(`/mes/wm/productsales/checkQuantity/${head.salesId}`)).data === true, '销售拣货数量未核对通过')
    head = await api.status('/mes/wm/productsales', head.salesId, 'UNSHIPPING', ['UNSTOCK'])
  }
  if (head.status === 'UNCHECK') {
    const checks = (await api.list('/mes/qc/oqc', { sourceDocId: head.salesId })).filter(q => same(q.sourceDocId, head.salesId) && lines.some(l => same(l.lineId, q.sourceLineId)))
    requireValue(checks.length, '销售出库未自动生成 OQC 草稿')
    for (const check of checks) await inspect(ctx, 'oqc', check, 6)
    head = await api.detail('/mes/wm/productsales', head.salesId)
  }
  if (head.status === 'UNSHIPPING') head = await api.status('/mes/wm/productsales', head.salesId, 'UNEXECUTE', ['UNSHIPPING'], { carrier: '自有车辆配送', waybillCode: ctx.state.runId })
  requireValue(['UNEXECUTE', 'FINISHED'].includes(head.status), '销售出库尚未完成质量放行：' + head.status)
  head = await api.execute('/mes/wm/productsales', head.salesId)
  c.sales = head; ctx.checkpoint(); doc(ctx, '销售出库', head, 'salesId', 'salesCode', '/mes/wm/productsales')
}
async function verify(ctx) {
  const api = createChainApi(ctx); const c = ctx.state.context; const item = c.items['TG-FG-001']
  const stocks = await api.list('/mes/wm/wmstock', { itemId: item.itemId, warehouseCode: 'WH-FG' })
  const remaining = stocks.filter(s => same(s.itemId, item.itemId) && s.warehouseCode === 'WH-FG' && c.finishedBatches.includes(s.batchCode)).reduce((s, r) => s + number(r.quantityOnhand), 0)
  const receipt = await api.detail('/mes/wm/productrecpt', c.productReceipt.recptId)
  const sale = await api.detail('/mes/wm/productsales', c.sales.salesId)
  requireValue(receipt.status === 'FINISHED' && sale.status === 'FINISHED' && close(remaining, 4), `本次成品结存核对未通过：实际 ${remaining} 台，预期 4 台`)
  const transactions = await api.list('/mes/wm/transaction', { itemCode: item.itemCode })
  const own = transactions.filter(t => c.finishedBatches.includes(t.batchCode))
  requireValue(own.some(t => same(t.sourceDocId, receipt.recptId) && t.sourceDocType === 'PRODUCT_RECPT') && own.some(t => same(t.sourceDocId, sale.salesId) && t.sourceDocType === 'PRODUCT_SALES'), '未查到本次成品入库与销售出库的库存流水')
  ctx.state.summary = { produced: 10, received: 10, sold: 6, remaining, batches: c.finishedBatches, documents: ctx.state.documents.length, transactionCount: own.length }
  ctx.checkpoint()
}

function createChainStages({ hasPermission } = {}) {
  return [
    { title: '准备检查', description: '检查账号权限、标准物料、BOM、工艺及可用库位；库位不足时自动建立物料专用库位。', run: ctx => prepare(ctx, hasPermission) },
    { title: '采购到货', description: '按 BOM 需求建立三类到货通知，提交后自动派发来料检验。', run: arrivals },
    { title: '来料检验 IQC', description: '按整批合格完成系统自动生成的检验任务并放行。', run: incomingInspection },
    { title: '采购入库', description: '复用入库草稿，自动上架并记录本次原料批次。', run: purchaseReceipt },
    { title: '父子生产工单', description: '建立整机工单，并从 BOM 生成两个半成品子工单。', run: workorders },
    { title: '工序排产', description: '按工艺顺序安排九道工序，系统自动生成领料草稿。', run: schedule },
    { title: '电控预装与 IPQC', description: '本次原料领料、两道工序报工与过程检验，合格半成品进入线边库。', run: ctx => production(ctx, 'TG-SF-001') },
    { title: '手柄预装与 IPQC', description: '完成手柄生产与检验，批次关联至整机父工单。', run: ctx => production(ctx, 'TG-SF-002') },
    { title: '整机装配与 IPQC', description: '依次总装、测试、校准、检验和包装，消耗限定为本次原料与半成品批次。', run: ctx => production(ctx, 'TG-FG-001') },
    { title: '成品入库', description: '包装完成后使用系统生成的入库单，将 10 台整机移入成品仓。', run: productReceipt },
    { title: '出货检验与销售', description: '发货通知联动销售单和 OQC，检验放行后出库 6 台。', run: sales },
    { title: '闭环核对', description: '核对本次单据、批次、库存流水及 4 台结存。', run: verify }
  ]
}

module.exports = { createChainStages, createChainApi, allocateBatchStocks, REQUIRED_PERMISSIONS, findSlot, ensureSlot, chainAreaCode, areaConflict, CHAIN_AREA_PERMISSION, CHAIN_AREA_PATH, CHAIN_AREA_API }

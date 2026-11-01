// 工单主线的只读状态口径；首页与核查脚本共用，采购/销售不冒充工单进度。
export const PIPELINE_STEPS = [
  { key: 'confirm', name: '工单确认', short: '工单确认', route: '/mes/pro/workorder', hint: '保存工单后确认下达' },
  { key: 'schedule', name: '生产排产', short: '生产排产', route: '/mes/pro/proschedule', hint: '按工艺路线安排各工序任务' },
  { key: 'ready', name: '备料检查', short: '备料检查', route: '/mes/wm/wmstock', hint: '核对外购料库存与本工单已领数量' },
  { key: 'issue', name: '生产领料', short: '生产领料', route: '/mes/wm/issue', hint: '排产自动生成草稿，拣货后执行领出' },
  { key: 'feedback', name: '生产报工', short: '生产报工', route: '/mes/pro/feedback', hint: '按工序、按实际数量报工并审核' },
  { key: 'ipqc', name: '过程检验', short: '过程检验', route: '/mes/qc/ipqc', hint: '需检工序报工后检验放行，再继续生产' },
  { key: 'finish', name: '工单完工', short: '工单完工', route: '/mes/pro/workorder', hint: '核对工序产出并完成工单' },
  { key: 'fgIn', name: '产品入库', short: '产品入库', route: '/mes/wm/productrecpt', hint: '顶层工单完工后执行产品入库' },
  { key: 'sale', name: '销售完成', short: '销售完成', route: '/mes/wm/productsales', hint: '按工单成品开销售出库单，发货即闭环' }
]

const same = (a, b) => a != null && b != null && String(a) === String(b)
const qty = value => Number.isFinite(Number(value)) ? Math.max(Number(value), 0) : 0
const sum = (rows, field) => rows.reduce((total, row) => total + qty(row[field]), 0)
const enough = (actual, required) => actual + 0.000001 >= required
const active = row => row.status !== 'CANCELED'
const itemKey = row => row.itemId != null ? 'id:' + row.itemId : 'code:' + row.itemCode
const belongs = (row, wo) => row.workorderId != null
  ? same(row.workorderId, wo.workorderId)
  : !!wo.workorderCode && row.workorderCode === wo.workorderCode
const codes = (rows, field) => Array.from(new Set(rows.map(row => row[field]).filter(Boolean))).join('、')

export function buildWorkorderPipeline(wo, data = {}) {
  const own = key => (data[key] || []).filter(row => active(row) && belongs(row, wo))
  const bom = (data.bom || []).filter(row => row.workorderId == null || same(row.workorderId, wo.workorderId))
  const tasks = own('tasks')
  const issues = own('issues')
  const feedbacks = own('feedbacks').filter(row => tasks.some(t => same(t.taskId, row.taskId)))
  const checks = own('ipqcs').filter(row => tasks.some(t => same(t.taskId, row.taskId)))
  const receipts = own('fgRecpts')
  const produces = own('produces').filter(p => p.status === 'FINISHED')
  const processes = data.processes || []
  const plan = qty(wo.quantity)
  const scheduled = processes.filter(p => enough(sum(tasks.filter(t => same(t.processId, p.processId) && same(t.routeId, p.routeId)), 'quantity'), plan))
  const scheduleDone = plan > 0 && processes.length > 0 && scheduled.length === processes.length

  // 自制半成品由工序产出、投料；领料草稿只包含外购料，不以原料仓无半成品阻断。
  const purchased = bom.filter(row => row.itemOrProduct !== 'PRODUCT' && qty(row.quantity) > 0)
  const requirements = new Map()
  purchased.forEach(row => {
    const key = itemKey(row)
    const old = requirements.get(key)
    requirements.set(key, { ...row, quantity: qty(row.quantity) + (old ? old.quantity : 0) })
  })
  const issuedLines = (data.issueLines || []).filter(line => issues.some(h => h.status === 'FINISHED' && same(h.issueId, line.issueId)))
  const rawStocks = (data.stocks || []).filter(row => row.warehouseCode === 'WH-RAW' && row.frozenFlag !== 'Y')
  const missing = []
  let issuedItems = 0
  requirements.forEach((row, key) => {
    const issued = sum(issuedLines.filter(line => itemKey(line) === key), 'quantityIssued')
    const raw = sum(rawStocks.filter(stock => itemKey(stock) === key), 'quantityOnhand')
    if (enough(issued, row.quantity)) issuedItems++
    // 已领用量是不可丢失的完成证据，生产消耗后不会因现存量下降而退回备料。
    if (!enough(raw + issued, row.quantity)) missing.push(`${row.itemCode || row.itemName} 需 ${row.quantity}，已领 ${issued}，原料仓可用 ${raw}`)
  })
  const validBom = bom.length > 0 && bom.some(row => qty(row.quantity) > 0)
  const issueDone = validBom && issuedItems === requirements.size
  const finished = feedbacks.filter(row => row.status === 'FINISHED')
  const doneTasks = tasks.filter(t => qty(t.quantity) > 0 && enough(sum(finished.filter(f => same(f.taskId, t.taskId)), 'quantityFeedback'), qty(t.quantity)))
  const feedbackDone = scheduleDone && tasks.length > 0 && doneTasks.length === tasks.length
  const needsCheck = t => {
    const rule = processes.find(p => same(p.processId, t.processId) && same(p.routeId, t.routeId))
    return (!rule || rule.keyFlag !== 'N') && (t.isCheck === 'Y' || rule && rule.isCheck === 'Y')
  }
  const checkTasks = tasks.filter(needsCheck)
  const waiting = feedbacks.filter(f => f.status === 'UNCHECK')
  const pendingChecks = checks.filter(c => c.status !== 'FINISHED')
  const releasedChecks = checks.filter(c => c.status === 'FINISHED' && (
    c.checkResult === 'ACCEPT' || c.checkResult === 'REJECT' ||
    // 兼容旧已过账检验：结论字段为空时必须同时有数量平衡、真实已过账来源作依据。
    (qty(c.quantityCheck) > 0 && Math.abs(qty(c.quantityQualified) + qty(c.quantityUnqualified) - qty(c.quantityCheck)) < 0.000001 &&
      produces.some(p => same(p.recordId, c.sourceDocId)))
  ))
  const checkedTasks = checkTasks.filter(t => {
    const batches = finished.filter(f => same(f.taskId, t.taskId))
    // sourceDocId 是产品产出单 ID；来源报工编码才对应当前报工，不能混用两张表的 ID。
    return doneTasks.includes(t) && batches.length > 0 && batches.every(f => releasedChecks.some(c =>
      same(c.taskId, t.taskId) && c.sourceDocType === 'FEEDBACK' && !!f.feedbackCode && c.sourceDocCode === f.feedbackCode))
  })
  const checkApplicable = processes.some(p => p.keyFlag !== 'N' && p.isCheck === 'Y') || checkTasks.length > 0 || waiting.length > 0 || checks.length > 0
  const checksDone = scheduleDone && !waiting.length && !pendingChecks.length && checkedTasks.length === checkTasks.length &&
    releasedChecks.length === checks.length
  const finishedReceipts = receipts.filter(h => h.status === 'FINISHED')
  const received = sum((data.fgLines || []).filter(line => finishedReceipts.some(h => same(h.recptId, line.recptId)) &&
    (line.itemId != null ? same(line.itemId, wo.productId) : line.itemCode === wo.productCode)), 'quantityRecived')
  // 与 ProductReceiptLinkageService 一致：合格产出与末工序有效放行量共同限定入库目标。
  // 工单 quantityProduced 累加各工序报工，既非成品数量，也非合格数量。
  const productLines = (data.produceLines || []).filter(line => line.qualityStatus === 'OK' && same(line.itemId, wo.productId) &&
    produces.some(p => same(p.recordId, line.recordId)))
  const outputProcesses = new Set(produces.filter(p => productLines.some(line => same(line.recordId, p.recordId))).map(p => String(p.processId)))
  const terminal = processes.filter(p => same(p.nextProcessId, 0)).sort((a, b) => qty(b.orderNum) - qty(a.orderNum) || qty(b.processId) - qty(a.processId))[0]
  const packaging = terminal ? finished.filter(f => same(f.processId, terminal.processId)) : []
  const packagingCap = Math.max(0, sum(packaging, 'quantityFeedback') - sum(packaging, 'quantityUnquanlified'))
  const receiptTarget = outputProcesses.size === 1 ? Math.min(sum(productLines, 'quantityProduce'), packagingCap) : 0
  const child = qty(wo.parentId) > 0
  // 销售完成口径：只认已发货（销售出库单 FINISHED）的出库行；草稿与在途单不提前算完成。
  const salesLines = own('salesLines').filter(line => line.salesStatus !== 'CANCELED')
  const shippedLines = salesLines.filter(line => line.salesStatus === 'FINISHED')
  const shipped = sum(shippedLines, 'quantitySales')
  const opened = sum(salesLines, 'quantitySales')
  const saleTarget = received
  const saleDone = saleTarget > 0 && enough(shipped, saleTarget)

  const states = {
    confirm: wo.status === 'CONFIRMED' || wo.status === 'FINISHED',
    schedule: scheduleDone,
    ready: validBom && missing.length === 0,
    issue: issueDone,
    feedback: feedbackDone,
    ipqc: checksDone,
    finish: wo.status === 'FINISHED',
    fgIn: receiptTarget > 0 && enough(received, receiptTarget),
    sale: saleDone
  }
  const skipped = { issue: validBom && !requirements.size, ipqc: scheduleDone && !checkApplicable, fgIn: child, sale: child }
  const docs = {
    confirm: wo.workorderCode,
    schedule: `${scheduled.length}/${processes.length} 道工序已排足`,
    ready: validBom ? `${requirements.size - missing.length}/${requirements.size} 项外购料可用` : '用料未配置',
    issue: codes(issues, 'issueCode') || `${issuedItems}/${requirements.size} 项已领足`,
    feedback: `${doneTasks.length}/${tasks.length} 个任务报工完成`,
    ipqc: codes(checks, 'ipqcCode') || (checkApplicable ? '等待检验派单' : '无须检验'),
    finish: wo.status === 'FINISHED' ? '已完工' : '待工序完成',
    fgIn: child ? '产出留在线边库' : (receiptTarget > 0 ? `已入库 ${received}/${receiptTarget}` : '待合格产出放行'),
    sale: child ? '随整机销售' : (codes(shippedLines, 'salesCode') || (saleTarget > 0 ? `已发货 ${shipped}/${saleTarget}` : '待产品入库'))
  }
  const texts = {
    confirm: states.confirm ? '工单已下达，按工艺路线安排生产' : '工单保存后还需确认下达，再进行生产排产',
    schedule: !processes.length ? '产品未配置可用工艺路线，请先维护工艺路线再排产' : `已排足 ${scheduled.length}/${processes.length} 道工序；各工序排产数量需覆盖工单计划量 ${plan}，保存任务后自动生成领料草稿`,
    ready: !validBom ? '工单用料未配置，请先核对并补齐工单 BOM' : (missing.length ? `缺料 ${missing.length} 项：${missing.join('；')}。外购料通过到货、IQC、采购入库补齐` : '原料仓未冻结库存与本工单已领数量满足外购料需求；现存量仅供备料参考，拣货时仍需核对批次。自制半成品由对应工序在线边库投料'),
    issue: skipped.issue ? '本工单仅使用自制半成品，由对应工序投料，无须重复领料' : `已领足 ${issuedItems}/${requirements.size} 项外购料；排产后打开自动生成的领料草稿，拣货并执行领出`,
    feedback: `按任务累计已完成报工数量，${doneTasks.length}/${tasks.length} 个任务报工完成；待检报工需先完成过程检验，放行后继续下一工序`,
    ipqc: skipped.ipqc ? '当前工艺路线无须过程检验' : `已放行 ${checkedTasks.length}/${checkTasks.length} 个需检任务；待检报工 ${waiting.length} 张，待办检验 ${pendingChecks.length} 张。各工序、各批次分别核对，不以一张合格单代替全部检验`,
    finish: states.finish ? '工单已完工，仍需核对报工和检验依据' : '完成各工序报工与检验后核对工单完工状态',
    fgIn: child ? '半成品子工单产出在线边库供后续工序使用，无须进入成品仓' : (receiptTarget > 0 ? `累计已执行入库 ${received}，合格放行目标 ${receiptTarget}；仅统计本工单产品入库明细，草稿和部分入库不代表全部完成` : '尚无可核实的成品合格产出或末工序放行依据，请核对工序产出与末工序报工，不以计划量或累计报工量替代'),
    sale: child
      ? '半成品子工单产出在线边库随整机入库，不单独开销售出库单，销售完成跟随主工单'
      : (saleTarget <= 0
        ? '成品尚未入库，先完成产品入库，再按工单成品开销售出库单发货'
        : (saleDone
          ? `已按工单成品发货 ${shipped}/${saleTarget}，工单从确认到销售发货全流程闭环`
          : `已发货 ${shipped}/${saleTarget}，待发运 ${Math.max(opened - shipped, 0)}；在「仓储 → 销售出库」新增出库单，用「工单成品」选择本工单已入库的批次与数量，执行出库后计入完成`))
  }
  let current = PIPELINE_STEPS.find(def => !states[def.key] && !skipped[def.key])
  // 工序生产与检验交替发生：有待检报工时先放行，避免卡在“报工未完成”。
  if (current && current.key === 'feedback' && (waiting.length || pendingChecks.length)) current = { key: 'ipqc' }
  return PIPELINE_STEPS.map(def => {
    const state = skipped[def.key] ? 'skipped' : (states[def.key] ? 'done' : (current && def.key === current.key ? 'doing' : 'todo'))
    return { ...def, route: def.key === 'ready' && !validBom ? '/mes/pro/workorder' : def.route,
      state, stateText: { done: '已完成', skipped: '不适用', doing: '下一步', todo: '待办理' }[state],
      docCode: docs[def.key], docTitle: texts[def.key], text: texts[def.key],
      shortages: def.key === 'ready' ? missing : [] }
  })
}

export async function fetchPipelineRows(request, url, params = {}) {
  const rows = []
  for (let pageNum = 1; pageNum <= 100; pageNum++) {
    const res = await request({ url, method: 'get', params: { ...params, pageNum, pageSize: 200 }})
    if (!res || (res.code != null && Number(res.code) !== 200) || !Array.isArray(res.rows) || !Number.isFinite(Number(res.total))) {
      throw new Error('无法完整读取流水线数据：' + url)
    }
    rows.push(...res.rows)
    if (rows.length >= Number(res.total)) return rows
    if (!res.rows.length) break
  }
  throw new Error('流水线数据分页不完整，请缩小查询范围：' + url)
}

export async function loadWorkorderPipeline(request, wo) {
  const ownRows = async url => (await fetchPipelineRows(request, url, { workorderId: wo.workorderId })).filter(row => belongs(row, wo) && active(row))
  const [bom, tasks, issues, feedbacks, ipqcs, fgRecpts, produces, salesLines, route] = await Promise.all([
    ownRows('/mes/pro/workorderbom/list'), ownRows('/mes/pro/protask/list'),
    ownRows('/mes/wm/issueheader/list'), ownRows('/mes/pro/feedback/list'),
    ownRows('/mes/qc/ipqc/list'), ownRows('/mes/wm/productrecpt/list'), ownRows('/mes/wm/productproduce/list'),
    // 销售出库行带所属出库单状态：只有已执行（FINISHED）的发货才算销售完成
    ownRows('/mes/wm/productsalesline/list'),
    request({ url: '/mes/pro/routeprocess/listProductProcess/' + wo.productId, method: 'get' }).catch(error => {
      if (String(error && error.message || error).includes('未配置工艺路线')) return { data: [] }
      throw error
    })
  ])
  if (!route || !Array.isArray(route.data)) throw new Error('工艺路线数据读取失败')
  const purchased = bom.filter(row => row.itemOrProduct !== 'PRODUCT' && qty(row.quantity) > 0)
  const stockItems = Array.from(new Map(purchased.map(row => [itemKey(row), row])).values())
  const [stockPages, issuePages, fgPages, producePages] = await Promise.all([
    Promise.all(stockItems.map(row => fetchPipelineRows(request, '/mes/wm/wmstock/list', row.itemId != null ? { itemId: row.itemId } : { itemCode: row.itemCode }))),
    Promise.all(issues.filter(h => h.status === 'FINISHED').map(h => fetchPipelineRows(request, '/mes/wm/issueline/list', { issueId: h.issueId }))),
    Promise.all(fgRecpts.filter(h => h.status === 'FINISHED').map(h => fetchPipelineRows(request, '/mes/wm/productrecptline/list', { recptId: h.recptId }))),
    Promise.all(produces.filter(p => p.status === 'FINISHED').map(p => fetchPipelineRows(request, '/mes/wm/productproduceline/list', { recordId: p.recordId, itemId: wo.productId })))
  ])
  return { bom, tasks, issues, feedbacks, ipqcs, fgRecpts, produces, salesLines, processes: route.data,
    stocks: [].concat(...stockPages), issueLines: [].concat(...issuePages), fgLines: [].concat(...fgPages), produceLines: [].concat(...producePages) }
}

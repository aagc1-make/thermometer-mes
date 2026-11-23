// 工单主线的只读状态口径；首页与核查脚本共用，采购/销售不冒充工单进度。
export const PIPELINE_STEPS = [
  { key: 'order', name: '客户订单', short: '客户订单', route: '/order/salesorder', hint: '客户下单并确认，后续工单/质检/发货单按订单号关联' },
  { key: 'confirm', name: '工单确认', short: '工单确认', route: '/mes/pro/workorder', hint: '保存工单后确认下达' },
  { key: 'schedule', name: '生产排产', short: '生产排产', route: '/mes/pro/proschedule', hint: '按工艺路线安排各工序任务' },
  { key: 'ready', name: '备料检查', short: '备料检查', route: '/mes/wm/wmstock', hint: '核对外购料库存与本工单已领数量' },
  { key: 'issue', name: '生产领料', short: '生产领料', route: '/mes/wm/issue', hint: '排产自动生成草稿，拣货后执行领出' },
  { key: 'feedback', name: '生产报工', short: '生产报工', route: '/mes/pro/feedback', hint: '按工序、按实际数量报工并审核' },
  { key: 'ipqc', name: '过程检验', short: '过程检验', route: '/mes/qc/ipqc', hint: '需检工序报工后检验放行，再继续生产' },
  { key: 'finish', name: '工单完工', short: '工单完工', route: '/mes/pro/workorder', hint: '核对工序产出并完成工单' },
  { key: 'fgIn', name: '产品入库', short: '产品入库', route: '/mes/wm/productrecpt', hint: '完工自动生成入库草稿，提交后需上架再执行入库过账' },
  { key: 'sale', name: '销售完成', short: '销售完成', route: '/mes/wm/productsales', hint: '确认发货通知生成出库草稿，拣货、运单后执行出库即闭环' }
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
  // 销售出库行可能早于工单绑定（历史/手工单），按订单号兜底匹配，保证已出库能同步到工单流水线。
  const salesLines = (data.salesLines || []).filter(row => active(row) && (
    belongs(row, wo) || (!!wo.sourceDocCode && row.soCode != null && String(row.soCode) === String(wo.sourceDocCode))))
  const shippedLines = salesLines.filter(line => line.salesStatus === 'FINISHED')
  const shipped = sum(shippedLines, 'quantitySales')
  const opened = sum(salesLines, 'quantitySales')
  const saleTarget = received
  const saleDone = saleTarget > 0 && enough(shipped, saleTarget)

  // 入库/上架进度：按入库单状态给出下一步，避免只显示笼统的“去出库”。
  const receiptDrafts = receipts.filter(h => h.status === 'PREPARE')
  const receiptApproving = receipts.filter(h => h.status === 'APPROVING')
  const receiptApproved = receipts.filter(h => h.status === 'APPROVED')
  const saleDocs = new Map()
  salesLines.forEach(line => {
    if (!line.salesCode) return
    saleDocs.set(line.salesCode, { code: line.salesCode, status: line.salesStatus })
  })
  const saleByStatus = status => Array.from(saleDocs.values()).filter(doc => doc.status === status)
  const saleCodes = status => codes(saleByStatus(status), 'code')

  const states = {
    order: !!wo.sourceDocCode && (wo.sourceDocType === 'SALES_ORDER' || wo.orderSource === 'SALES_ORDER'),
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
  const skipped = { order: !states.order, issue: validBom && !requirements.size, ipqc: scheduleDone && !checkApplicable, fgIn: child, sale: child }
  const docs = {
    order: wo.sourceDocCode || '',
    confirm: wo.workorderCode,
    schedule: `${scheduled.length}/${processes.length} 道工序已排足`,
    ready: validBom ? `${requirements.size - missing.length}/${requirements.size} 项外购料可用` : '用料未配置',
    issue: codes(issues, 'issueCode') || `${issuedItems}/${requirements.size} 项已领足`,
    feedback: `${doneTasks.length}/${tasks.length} 个任务报工完成`,
    ipqc: codes(checks, 'ipqcCode') || (checkApplicable ? '等待检验派单' : '无须检验'),
    finish: wo.status === 'FINISHED' ? '已完工' : '待工序完成',
    fgIn: child ? '产出留在线边库' : (codes(receipts, 'recptCode') || (receiptTarget > 0 ? `已入库 ${received}/${receiptTarget}` : '待合格产出放行')),
    sale: child ? '随整机销售' : (codes(Array.from(saleDocs.values()), 'code') || (saleTarget > 0 ? `已发货 ${shipped}/${saleTarget}` : '待产品入库'))
  }
  const texts = {
    order: states.order
      ? `来自客户订单 ${wo.sourceDocCode}，工单、质检单、发货单均按该订单号与订单明细关联，可点击格进入订单查看全链路单据`
      : '该工单为手工建单，没有客户订单来源；可点击格进入「订单」先做客户下单，再按订单生成工单/质检/发货单',
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
  // 前序环节的动态指引：与入库/销售一致，给出当前单号与具体动作，而不是笼统的“去办理”。
  const orderNext = states.order
    ? { action: '查看订单 ' + wo.sourceDocCode, guide: `客户订单 ${wo.sourceDocCode} 已确认，工单、质检单、发货通知均按该订单号关联` }
    : { action: '去下客户订单', guide: '先建客户订单并确认，再一键生成生产工单与出货检验单' }
  const confirmNext = states.confirm
    ? { action: '查看工单 ' + wo.workorderCode, guide: `工单 ${wo.workorderCode} 已下达，可继续排产` }
    : { action: '确认工单 ' + wo.workorderCode, guide: `核对产品、计划量 ${plan} 与交期后点击「确认」，系统自动生成质检单、发货通知与销售出库草稿` }
  const scheduleNext = scheduleDone
    ? { action: '查看排产', guide: `已排足 ${scheduled.length}/${processes.length} 道工序，计划量 ${plan}；保存后排产任务可继续报工` }
    : { action: '去排产', guide: processes.length
      ? `按工艺路线为 ${processes.length} 道工序排产，数量需覆盖计划量 ${plan}，保存后自动生成领料草稿`
      : '产品未配置可用工艺路线，请先维护工艺路线再排产' }
  const readyNext = !validBom
    ? { action: '补工单用料', guide: '工单 BOM 未配置，先补齐用料再核对库存' }
    : missing.length
      ? { action: '去补料', guide: `缺料 ${missing.length} 项：${missing.join('；')}。外购料需到货、IQC、采购入库后才有可用库存` }
      : { action: '核对库存', guide: `外购料 ${requirements.size} 项原料仓可用，拣货时仍需按批次核对` }
  const issueCodes = codes(issues, 'issueCode')
  const openIssue = issues.find(h => h.status === 'PREPARE') || issues.find(h => h.status === 'APPROVING') || issues.find(h => h.status === 'APPROVED')
  const issueNext = skipped.issue
    ? { action: '无须领料', guide: '本工单仅使用自制半成品，由对应工序投料' }
    : issueDone
      ? { action: '查看领料单 ' + issueCodes, guide: `领料单 ${issueCodes} 已执行领出，${issuedItems}/${requirements.size} 项外购料已领足` }
      : openIssue
        ? {
          action: (openIssue.status === 'PREPARE' ? '提交领料 ' : (openIssue.status === 'APPROVING' ? '去拣货 ' : '执行领出 ')) + openIssue.issueCode,
          guide: `领料单 ${openIssue.issueCode} ${openIssue.status === 'PREPARE' ? '为草稿，核对领料行后提交' : (openIssue.status === 'APPROVING' ? '待拣货，按库位拣货并核对数量' : '已拣货，执行领出过账')}`
        }
        : { action: '生成领料草稿', guide: '排产保存后会自动生成领料草稿，如缺失请回到排产重新保存' }
  const pendingTask = tasks.find(t => qty(t.quantity) > 0 && !doneTasks.includes(t))
  const feedbackNext = feedbackDone
    ? { action: '查看报工', guide: `${doneTasks.length}/${tasks.length} 个任务已报满，继续下一工序或完工` }
    : { action: '去报工', guide: pendingTask
      ? `任务 ${pendingTask.taskCode || ''} 还差 ${Math.max(qty(pendingTask.quantity) - sum(finished.filter(f => same(f.taskId, pendingTask.taskId)), 'quantityFeedback'), 0)} 待报工；待检报工需先完成过程检验`
      : '按工序、按实际数量报工并审核，需检工序报工后先完成检验再继续' }
  const openCheck = pendingChecks[0]
  const waitingInspect = waiting.length > 0
  const ipqcNext = skipped.ipqc
    ? { action: '无须检验', guide: '当前工艺路线无须过程检验' }
    : checksDone
      ? { action: '查看检验单 ' + codes(checks, 'ipqcCode'), guide: `检验单 ${codes(checks, 'ipqcCode') || '-'} 已全部放行` }
      : waitingInspect
        ? { action: '完成检验 ' + (waiting[0].feedbackCode || ''), guide: `报工 ${waiting[0].feedbackCode || ''} 待检，检验放行后才能继续下一工序` }
        : openCheck
          ? { action: '完成检验 ' + openCheck.ipqcCode, guide: `检验单 ${openCheck.ipqcCode} 待完成，按批次核对合格与不合格数量` }
          : { action: '去过程检验', guide: '需检工序报工后完成检验放行，再继续生产' }
  const finishNext = states.finish
    ? { action: '查看工单 ' + wo.workorderCode, guide: `工单 ${wo.workorderCode} 已完工，下一步执行产品入库` }
    : { action: '完成工单 ' + wo.workorderCode, guide: '核对各工序报工与检验放行后完成工单，完工后系统自动生成产品入库草稿' }
  const firstNext = {
    order: orderNext, confirm: confirmNext, schedule: scheduleNext, ready: readyNext,
    issue: issueNext, feedback: feedbackNext, ipqc: ipqcNext, finish: finishNext
  }
  // 下游环节的动态指引：带上真实单号与状态，明确“现在该做什么”，替代笼统的“去出库”。
  const fgInNext = receiptApproving.length
    ? { action: '确认上架 ' + receiptApproving[0].recptCode, guide: `入库单 ${codes(receiptApproving, 'recptCode')} 待上架，点击进入逐行分配库位并确认上架` }
    : receiptApproved.length
      ? { action: '执行入库 ' + receiptApproved[0].recptCode, guide: `入库单 ${codes(receiptApproved, 'recptCode')} 已上架，点击进入执行入库过账` }
      : receiptDrafts.length
        ? { action: '提交入库草稿 ' + receiptDrafts[0].recptCode, guide: `入库草稿 ${codes(receiptDrafts, 'recptCode')} 已按工单链自动生成，点击进入核对批次与数量并提交` }
        : receiptTarget <= 0
          ? { action: '核对末工序产出', guide: '末工序合格产出放行后，系统自动生成产品入库草稿（单号与工单同源）' }
          : { action: '继续入库 ' + received + '/' + receiptTarget, guide: `已入库 ${received}/${receiptTarget}，仍有合格产出待生成或执行入库` }
  const firstSale = status => (saleByStatus(status)[0] || {}).code || ''
  const saleNext = saleByStatus('UNEXECUTE').length
    ? { action: '执行出库 ' + firstSale('UNEXECUTE'), guide: `销售出库单 ${saleCodes('UNEXECUTE')} 待执行出库，点击进入完成发货` }
    : saleByStatus('UNSHIPPING').length
      ? { action: '填写运单 ' + firstSale('UNSHIPPING'), guide: `销售出库单 ${saleCodes('UNSHIPPING')} 待填写运单，点击进入录入承运商与运输单号` }
      : saleByStatus('UNCHECK').length
        ? { action: '完成出货检验', guide: `销售出库单 ${saleCodes('UNCHECK')} 待检验，检验放行后才能继续出库` }
        : saleByStatus('UNSTOCK').length
          ? { action: '去拣货 ' + firstSale('UNSTOCK'), guide: `销售出库单 ${saleCodes('UNSTOCK')} 待拣货，点击进入按库位拣货并核对数量` }
          : saleByStatus('PREPARE').length
            ? { action: '提交出库草稿 ' + firstSale('PREPARE'), guide: `销售出库草稿 ${saleCodes('PREPARE')} 已自动绑定发货通知与批次，点击进入核对并提交拣货` }
            : saleDone
              ? { action: '查看出库单', guide: `已按工单成品发货 ${shipped}/${saleTarget}，销售环节闭环` }
              : saleTarget > 0
                ? { action: '确认发货通知', guide: `成品已入库 ${received}，去「发货通知」确认后自动生成销售出库草稿` }
                : { action: '先完成产品入库', guide: '成品入库后才能开销售出库单发货' }

  // 严格串行：前一个环节未完成且非「不适用」时，后续环节即使已有数据也不能算完成，
  // 避免出现「第 3 步没做完、第 4 步已显示已完成」的跳步现象。
  const effective = {}
  let blocked = false
  PIPELINE_STEPS.forEach(def => {
    if (skipped[def.key] || blocked) {
      effective[def.key] = false
      return
    }
    effective[def.key] = !!states[def.key]
    if (!effective[def.key]) blocked = true
  })
  let current = PIPELINE_STEPS.find(def => !effective[def.key] && !skipped[def.key])
  // 工序生产与检验交替发生：有待检报工时先放行，避免卡在“报工未完成”。
  if (current && current.key === 'feedback' && (waiting.length || pendingChecks.length)) current = { key: 'ipqc' }
  return PIPELINE_STEPS.map(def => {
    const state = skipped[def.key] ? 'skipped' : (effective[def.key] ? 'done' : (current && def.key === current.key ? 'doing' : 'todo'))
    const blockedByPrev = !skipped[def.key] && !!states[def.key] && !effective[def.key]
    // 有报工在等检验时，「过程检验」这一步带上待检报工号，点击后质检页直接打开对应检验单
    const autoInspect = def.key === 'ipqc' && state === 'doing' && waiting.length > 0
    // 预建的下游草稿单号只在走到该环节后才展示，未到的环节只显示办理提示
    const reached = state !== 'todo'
    let guide = def.hint
    let action = ''
    if (reached) {
      if (def.key === 'fgIn') {
        if (state === 'skipped') {
          guide = '半成品产出留在线边库，无须进入成品仓'
        } else if (state === 'done') {
          action = '查看入库单'
          guide = `已入库 ${received}/${receiptTarget}，入库单 ${codes(receipts, 'recptCode')}`
        } else {
          action = fgInNext.action
          guide = fgInNext.guide
        }
      } else if (def.key === 'sale') {
        if (state === 'skipped') {
          guide = '半成品不单独销售，随主工单整机发货'
        } else if (state === 'done') {
          action = '查看出库单'
          guide = `已发货 ${shipped}/${saleTarget}，销售环节闭环`
        } else {
          action = saleNext.action
          guide = saleNext.guide
        }
      } else if (firstNext[def.key]) {
        action = firstNext[def.key].action
        guide = firstNext[def.key].guide
      }
    }
    return { ...def, autoInspect, feedbackCode: autoInspect ? (waiting[0].feedbackCode || '') : '',
      route: def.key === 'ready' && !validBom ? '/mes/pro/workorder' : def.route,
      state, stateText: state === 'skipped' && def.key === 'order'
        ? '无客户订单'
        : { done: '已完成', skipped: '不适用', doing: '下一步', todo: '待办理' }[state],
      docCode: reached ? docs[def.key] : '',
      guide, action,
      docTitle: blockedByPrev ? texts[def.key] + '（前序环节未完成，本环节暂不计为已完成）' : texts[def.key],
      text: blockedByPrev ? texts[def.key] + '（前序环节未完成，本环节暂不计为已完成）' : texts[def.key],
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
  // 销售出库按订单号取单：行上的 workorder 绑定可能晚于出库（历史数据/手工单），不能因此漏统计
  const loadSalesLines = async () => {
    if (!wo.sourceDocCode) return []
    const docs = await fetchPipelineRows(request, '/mes/wm/productsales/list', { soCode: wo.sourceDocCode })
    const pages = await Promise.all(docs.filter(doc => doc.salesId != null && doc.status !== 'CANCELED').map(doc =>
      fetchPipelineRows(request, '/mes/wm/productsalesline/list', { salesId: doc.salesId })
        .then(lines => lines.map(line => ({ ...line, soCode: doc.soCode || wo.sourceDocCode, salesStatus: line.salesStatus || doc.status })))))
    return [].concat(...pages)
  }
  const [bom, tasks, issues, feedbacks, ipqcs, fgRecpts, produces, salesLines, route] = await Promise.all([
    ownRows('/mes/pro/workorderbom/list'), ownRows('/mes/pro/protask/list'),
    ownRows('/mes/wm/issueheader/list'), ownRows('/mes/pro/feedback/list'),
    ownRows('/mes/qc/ipqc/list'), ownRows('/mes/wm/productrecpt/list'), ownRows('/mes/wm/productproduce/list'),
    // 销售出库按订单号取单：行上的 workorder 绑定可能晚于出库（历史数据/手工单），不能因此漏统计
    loadSalesLines(),
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

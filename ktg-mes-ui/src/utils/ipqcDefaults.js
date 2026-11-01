// 过程检验默认值：优先用"待检报工"带出整张检验单（含来源单据），其次用该工单的待报工工序。
// 检验单必须挂来源报工/产出行，完成时才会放行报工；只填数量不填来源会变成放行不了的孤儿单。
const num = value => Number.isFinite(Number(value)) ? Number(value) : 0

/** 待检报工优先：报工已执行、等检验的那张就是本次要检的来源。 */
export function pickInspectionFeedback(feedbacks = []) {
  const rows = (feedbacks || []).filter(row => row && row.status !== 'CANCELED')
  return rows.find(row => row.status === 'UNCHECK') || rows[0] || null
}

/** 待检报工对应的产品产出行：来源单据 ID/行 ID 由它决定，来源不对就放行不了报工。 */
export function pickProduceLine(lines = [], feedback) {
  if (!feedback) return null
  const rows = (lines || []).filter(line => line && num(line.quantityProduce) > 0)
  return rows.find(line => feedback.itemId != null && String(line.itemId) === String(feedback.itemId)) || rows[0] || null
}

/** 同一产出行已有未完成的检验单时直接用它，避免一键再造一张重复单。 */
export function findOpenIpqc(ipqcs = [], line) {
  if (!line) return null
  return (ipqcs || []).find(row => row && row.status !== 'FINISHED' &&
    String(row.sourceLineId) === String(line.lineId)) || null
}

/** 合格数量 = 检测数量 - 不合格数：后端要求三者必须配平，否则保存/完成都会报错。 */
export function qualifiedFromCheck(quantityCheck, quantityUnqualified) {
  return Math.max(0, num(quantityCheck) - num(quantityUnqualified))
}

/** 由来源（待检报工 / 待报工任务）拼出检验单默认值。 */
export function inspectionDefaults({ feedback = null, task = null, line = null, inspector = '', today = '' } = {}) {
  const source = feedback || task || {}
  const quantityCheck = line
    ? num(line.quantityProduce)
    : feedback
      ? (num(feedback.quantityUncheck) > 0 ? num(feedback.quantityUncheck) : num(feedback.quantityFeedback))
      : Math.max(0, num(task && task.quantity) - num(task && task.quantityProduced))
  const workorderCode = source.workorderCode || ''
  const processName = source.processName || ''
  return {
    ipqcType: 'IPQC',
    ipqcName: [workorderCode, processName, '过程检验'].filter(Boolean).join('-'),
    workorderId: source.workorderId || null,
    workorderCode: source.workorderCode || null,
    workorderName: source.workorderName || null,
    taskId: source.taskId || null,
    taskCode: source.taskCode || null,
    taskName: source.taskName || null,
    workstationId: source.workstationId || null,
    workstationCode: source.workstationCode || null,
    workstationName: source.workstationName || null,
    processId: source.processId || null,
    processCode: source.processCode || null,
    processName: source.processName || null,
    itemId: (line && line.itemId) || source.itemId || null,
    itemCode: (line && line.itemCode) || source.itemCode || null,
    itemName: (line && line.itemName) || source.itemName || null,
    specification: (line && line.specification) || source.specification || null,
    unitOfMeasure: (line && line.unitOfMeasure) || source.unitOfMeasure || null,
    unitName: (line && line.unitName) || source.unitName || null,
    quantityCheck,
    quantityQualified: quantityCheck,
    quantityUnqualified: 0,
    quantityLaborScrap: 0,
    quantityMaterialScrap: 0,
    quantityOtherScrap: 0,
    crQuantity: 0,
    majQuantity: 0,
    minQuantity: 0,
    // 默认按检验通过填写，有不良品时先改不合格数，合格数会跟着配平
    checkResult: 'ACCEPT',
    inspectDate: today,
    inspector,
    sourceDocType: line ? 'FEEDBACK' : null,
    sourceDocId: line ? line.recordId : null,
    sourceDocCode: line && feedback ? feedback.feedbackCode : null,
    sourceLineId: line ? line.lineId : null
  }
}

// 报工默认值口径：把"选工单 → 选任务 → 选工作站 → 填数量"压缩成自动带出，减少手工选择。
// 后端 ProFeedbackController#add 会用工作站的工序覆盖报工工序，所以工作站必须取任务上的那个。
const num = value => Number.isFinite(Number(value)) ? Number(value) : 0
const OPEN_STATUS = ['PREPARE', 'APPROVING', 'UNCHECK']

/** 任务剩余可报数量：排产数量 - 已报工累计（报工执行时按 quantityFeedback 累加）。 */
export function taskRemaining(task) {
  return Math.max(0, num(task && task.quantity) - num(task && task.quantityProduced))
}

/** 还可以继续报工的任务：未完工、未取消，且排产数量还没报满。 */
export function pendingTasks(tasks, processes = []) {
  const order = new Map((processes || []).map(process => [String(process.processId), num(process.orderNum)]))
  const rank = task => order.has(String(task.processId)) ? order.get(String(task.processId)) : Number.MAX_SAFE_INTEGER
  return (tasks || [])
    .filter(task => task && task.status !== 'FINISHED' && task.status !== 'CANCELED' && taskRemaining(task) > 0)
    .sort((a, b) => rank(a) - rank(b) || num(a.taskId) - num(b.taskId))
}

/**
 * 默认任务：工艺顺序上第一道还有剩余量的工序。
 * 该工序已有未完成的报工单（草稿/待审核/待检验）时返回 null，既不重复建单，也不越过它去报后面的工序。
 */
export function defaultTask(tasks, processes = [], openTaskIds = new Set()) {
  const first = pendingTasks(tasks, processes)[0]
  if (!first) return null
  return openTaskIds.has(String(first.taskId)) ? null : first
}

/** 未完成的报工单覆盖了哪些任务，用于避免一键再造一张重复草稿。 */
export function openTaskIds(feedbacks = []) {
  return new Set((feedbacks || [])
    .filter(row => row && OPEN_STATUS.includes(row.status))
    .map(row => String(row.taskId)))
}

/** 数量默认值：默认把该任务剩余可报量一次报完；免检工序再拆出合格/不良两栏。 */
export function quantityDefaults(task) {
  const remaining = taskRemaining(task)
  return task && task.isCheck === 'Y'
    ? { quantityFeedback: remaining, quantityQualified: 0, quantityUnquanlified: 0 }
    : { quantityFeedback: remaining, quantityQualified: remaining, quantityUnquanlified: 0 }
}

// 到货通知 / 来料检验 / 采购入库的只读关联展示工具
export function text(value) {
  return value === null || value === undefined || value === '' ? '未关联' : String(value)
}

export function formatDate(value) {
  if (!value) return ''
  return String(value).slice(0, 10)
}

const COMPLETED = ['FINISHED', 'CONFIRMED']

export function isCompletedIqc(iqc) {
  return !!iqc && COMPLETED.indexOf(iqc.status) > -1
}

export function mergeIqcLinks(row, iqcRows) {
  const map = new Map()
  const push = (item) => {
    if (!item) return
    const key = item.iqcId != null ? String(item.iqcId) : ('code:' + item.iqcCode)
    if (!item.iqcCode && !item.iqcId) return
    if (!map.has(key)) {
      map.set(key, Object.assign({}, item))
    } else {
      // 行上的 iqc_id/iqc_code 只是快照，真实状态/数量以 qc_iqc 查询结果为准
      map.set(key, Object.assign({}, map.get(key), item))
    }
  }
  if (row && row.iqcId != null) {
    push({
      iqcId: row.iqcId,
      iqcCode: row.iqcCode,
      status: row.iqcStatus,
      quantityQualified: row.quantityQuanlified,
      quantityUnqualified: null,
      quantityRecived: row.quantityArrival
    })
  }
  (iqcRows || []).forEach(push)
  return Array.from(map.values()).sort((a, b) => {
    // 已完成的排在前面；同状态按编号
    const ac = isCompletedIqc(a) ? 0 : 1
    const bc = isCompletedIqc(b) ? 0 : 1
    if (ac !== bc) return ac - bc
    return String(a.iqcCode || '').localeCompare(String(b.iqcCode || ''))
  })
}

export function inspectionStatus(row) {
  if (!row || row.iqcCheck !== 'Y') return '免检'
  const links = row.iqcLinks || mergeIqcLinks(row, [])
  const completed = links.filter(isCompletedIqc)
  if (!completed.length) return '待检'
  const sourceQuantity = completed.reduce((max, iqc) => {
    const q = Number(iqc.quantityRecived != null ? iqc.quantityRecived : iqc.quantityCheck)
    return Number.isFinite(q) ? Math.max(max, q) : max
  }, Number(row.quantityArrival != null ? row.quantityArrival : 0))
  const qualified = completed.reduce((max, iqc) => {
    const q = Number(iqc.quantityQualified)
    return Number.isFinite(q) ? Math.max(max, q) : max
  }, 0)
  if (qualified <= 0) return '不合格'
  if (sourceQuantity > 0 && qualified < sourceQuantity) return '部分合格'
  return '合格'
}

export function inspectionStatusType(status) {
  if (status === '合格') return 'success'
  if (status === '部分合格') return 'warning'
  if (status === '不合格') return 'danger'
  if (status === '免检') return 'info'
  return ''
}

export function businessStatus(value) {
  const map = {
    PREPARE: '草稿',
    APPROVING: '待入库',
    APPROVED: '待执行',
    CONFIRMED: '已确认',
    FINISHED: '已完成',
    CANCELED: '已取消',
    ACCEPT: '合格',
    REJECT: '不合格'
  }
  return map[value] || text(value)
}

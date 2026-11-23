/**
 * 智能助手的业务规则回答：把「检验要查什么」「不合格料怎么处置」这类流程问题，
 * 变成基于系统实际配置与单据的确定性回答。
 *
 * 设计取舍：
 * 1. 只做纯计算与文案拼装，接口调用留在页面里，便于在 Node 中直接单测；
 * 2. 不调用大模型、不执行任何写操作，回答里的数字都来自调用方传入的真实数据；
 * 3. 回答中的流程描述必须与后端实现一致（退供来源校验、RQC 拆分、成品入库只看合格产出），
 *    改业务流程时这里要同步改。
 */

const clean = value => String(value == null ? '' : value).replace(/\s+/g, ' ').trim()

function num(value) {
  if (value == null || value === '') return '—'
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return clean(value)
  return String(Math.round(parsed * 10000) / 10000)
}

function percent(value) {
  if (value == null || value === '') return '未设置'
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return clean(value)
  // 库里 cr_rate/maj_rate/min_rate 存的就是百分数（如 2.50 表示 2.5%），不再乘 100。
  return `${Math.round(parsed * 100) / 100}%`
}

/**
 * 识别问题主题。
 * - unqualified：不合格/不良/退供等物料处置问题
 * - inspection-items：来料/过程/出货检验要检查哪些项目
 * - ''：其他问题，交回原有路由
 */
export function detectMesTopic(question) {
  const q = clean(question).toLowerCase()
  if (!q) return ''
  const asksHandling = /(?:处理|处置|怎么办|怎么处理|如何|流程|怎么走|退回|退掉|退供|退给|报废|隔离|返工|返修|让步)/.test(q)
  if (asksHandling && /(?:不合格|不良品|不良|废品|ng\b)/.test(q)) return 'unqualified'
  const asksItems = /(?:项目|指标|检查什么|检验什么|查什么|检查哪些|检验哪些|哪些项目|哪些指标|检查内容|检验内容|检验标准|判定标准|检验要求|检验哪几项)/.test(q)
  if (asksItems && /(?:检验|检查|检测|验收|抽检|iqc|ipqc|oqc)/.test(q)) return 'inspection-items'
  return ''
}

/** 单个检测项的合格范围文案：优先上下限，其次标准值 */
export function indexStandard(index) {
  if (!index) return '—'
  const unit = clean(index.unitOfMeasure)
  const suffix = unit ? ` ${unit}` : ''
  const min = index.thresholdMin
  const max = index.thresholdMax
  if (min != null || max != null) {
    return `${min == null ? '不限' : num(min)} ~ ${max == null ? '不限' : num(max)}${suffix}`
  }
  if (index.standerVal != null) return `${num(index.standerVal)}${suffix}`
  return '—'
}

/** 检测项 → 表格行（质检方案的 template_index 与来料检验单的 iqc_line 字段同名） */
export function inspectionIndexRows(indexes) {
  return (indexes || []).map(index => ({
    indexCode: clean(index.indexCode) || '—',
    indexName: clean(index.indexName) || '—',
    checkMethod: clean(index.checkMethod) || '—',
    standard: indexStandard(index),
    // 文字类判定项（外观、手感）没有数值范围，判定标准一般写在 remark 里，不能丢。
    remark: clean(index.remark) || '—',
    qcTool: clean(index.qcTool) || '—'
  }))
}

export const INSPECTION_COLUMNS = [
  { prop: 'indexCode', label: '检测项编码', width: 120 },
  { prop: 'indexName', label: '检测项', width: 120 },
  { prop: 'checkMethod', label: '检验方法', width: 110 },
  { prop: 'standard', label: '合格范围', width: 110 },
  { prop: 'remark', label: '判定说明' },
  { prop: 'qcTool', label: '检验工具', width: 100 }
]

const INSPECTION_SOURCE = '数据来源：质检方案（qc_template / qc_template_product / qc_template_index）与来料检验单（qc_iqc / qc_iqc_line）实时读取'

/**
 * 回答「某物料的来料检验要检查什么」。
 * 三级证据：已配置的检测项 → 最近一张检验单实际记录的检测项 → 两者都没有时说清系统到底记录什么。
 */
export function buildInspectionAnswer({ item, template, product, indexes, lastIqc, lastIqcLines } = {}) {
  const name = clean(item && item.itemName)
  const label = `${name || '该物料'}${item && item.itemCode ? `（${clean(item.itemCode)}）` : ''}`
  if (indexes && indexes.length) {
    const names = indexes.map(index => clean(index.indexName)).filter(Boolean)
    const shown = names.slice(0, 6).join('、')
    const more = names.length > 6 ? ` 等 ${names.length} 项` : ''
    const text = [`${label}在质检方案《${clean(template && template.templateName) || '未命名方案'}》下共配置 ${indexes.length} 项检验：${shown}${more}。`]
    if (product) {
      const acceptance = [`缺省检测数 ${num(product.quantityCheck)}`, `最大不合格数 ${num(product.quantityUnqualified)}`]
      if (product.crRate != null || product.majRate != null || product.minRate != null) {
        acceptance.push(`CR/MAJ/MIN 允收水平 ${percent(product.crRate)} / ${percent(product.majRate)} / ${percent(product.minRate)}`)
      }
      text.push(`判定参数：${acceptance.join('，')}。`)
    }
    text.push('逐项按检验方法测出结果，与该指标的合格范围比对；合格数量 + 不合格数量必须等于检测数量，合格量大于 0 判 ACCEPT、等于 0 判 REJECT，检验单完成前不能做采购入库。')
    return {
      text: text.join('\n'),
      title: `${label} · 来料检验项目`,
      columns: INSPECTION_COLUMNS,
      rows: inspectionIndexRows(indexes),
      source: INSPECTION_SOURCE
    }
  }
  if (lastIqcLines && lastIqcLines.length) {
    const text = [
      `${label}还没有配置质检方案指标，下面是最近一张来料检验单 ${clean(lastIqc && lastIqc.iqcCode) || ''} 实际记录的 ${lastIqcLines.length} 项检验内容，以这张单据为准：`,
      '要固化成标准检验项目，请在「质量管理 → 质检方案」里为这个物料绑定方案并维护检测项。'
    ]
    return {
      text: text.join('\n'),
      title: `${label} · 最近检验单的检测项`,
      columns: INSPECTION_COLUMNS,
      rows: inspectionIndexRows(lastIqcLines),
      source: INSPECTION_SOURCE
    }
  }
  const text = [
    `${label}目前没有维护质检方案和检测项，系统也不会凭经验生成检验项目。`,
    '现有口径：来料检验单只记录检测数量、合格数量、不合格数量和判定结论（合格量大于 0 判 ACCEPT，等于 0 判 REJECT），不产生逐项检验记录。',
    '要按项目检验，请先在「质量管理 → 质检方案」为物料绑定方案并在「检测项设置」维护指标；配好后再来问，我可以直接列出这些项目。'
  ]
  return { text: text.join('\n'), title: '', columns: [], rows: [], source: INSPECTION_SOURCE }
}

const UNQUALIFIED_PATHS = [
  '① 来料检验判不合格且尚未入库：按不合格数量生成「来料不合格退供」草稿（未入库退供），在「仓储管理 → 退料退货 → 采购退货」提交并执行；因为从未入库，执行不产生库存流水。',
  '② 部分合格：采购入库只按合格数量建单，不合格部分不进可用库存，同样走 ① 的未入库退供。',
  '③ 已入库后才发现不合格：从该批采购入库单生成退供草稿，执行时按批次拣货并扣减库存，累计退供量不能超过入库量。',
  '④ 生产环节不合格：过程检验判 NG 的产出不会建成品入库草稿（只有合格产出入库）；生产退料由「质量管理 → 退料检验（RQC）」按合格/不合格数量拆分，不合格行标记为不合格。',
  '⑤ 客户退货：走「退料退货 → 销售退货」，退回后重新检验，再决定返修或再次发货。'
]

const UNQUALIFIED_LIMITS = '系统没有单独的「报废单」和「让步接收」审批流：报废目前只能在「仓储管理 → 杂项出库」出账并备注原因；让步接收按部分合格（只入库合格数量）体现。'

/**
 * 回答「不合格物料应该如何处理」：先说系统真实处置路径，再给当前待处理数据。
 */
export function buildUnqualifiedAnswer({ iqcRows = [], returnRows = [], iqcRowsRead = true, returnRowsRead = true } = {}) {
  const ngRows = iqcRows.filter(row => Number(row.quantityUnqualified || 0) > 0)
  const ngTotal = ngRows.reduce((sum, row) => sum + Number(row.quantityUnqualified || 0), 0)
  const rejected = ngRows.filter(row => String(row.checkResult || '').toUpperCase() === 'REJECT').length
  const open = returnRows.filter(row => ['PREPARE', 'APPROVING', 'APPROVED'].includes(row.status))
  const finished = returnRows.filter(row => row.status === 'FINISHED')
  const preStockCount = status => returnRows.filter(row => row.attr2 === 'PRE_STOCK' && row.status === status).length

  const state = []
  if (!iqcRowsRead) {
    state.push('当前账号没有来料检验查看权限，因此没有读取不合格数量。')
  } else {
    state.push(ngTotal > 0
      ? `当前不合格来料：已完成检验中 ${ngRows.length} 张单、累计 ${num(ngTotal)} 件不合格${rejected ? `，其中 ${rejected} 张整批不合格（合格量为 0）` : ''}。`
      : '当前没有已完成且不合格数量大于 0 的来料检验单。')
  }
  if (!returnRowsRead) {
    state.push('当前账号没有采购退供查看权限，因此没有读取退供单。')
  } else if (open.length) {
    state.push(`待执行的退供单 ${open.length} 张（未入库 ${preStockCount('PREPARE') + preStockCount('APPROVING') + preStockCount('APPROVED')} 张）。`)
  } else {
    state.push('当前没有待执行的退供单。')
  }
  if (returnRowsRead && finished.length) state.push(`历史已执行退供 ${finished.length} 张。`)

  const text = [
    '不合格物料按「在哪一步发现」分几条路径处理，系统里都是真实单据，不是标记一下就算处理完：',
    UNQUALIFIED_PATHS.join('\n'),
    state.join(''),
    UNQUALIFIED_LIMITS
  ].join('\n')

  const rows = ngRows
    .slice()
    .sort((left, right) => String(right.inspectDate || '').localeCompare(String(left.inspectDate || '')))
    .slice(0, 10)
    .map(row => ({
      iqcCode: clean(row.iqcCode) || '—',
      itemName: clean(row.itemName) || '—',
      quantityUnqualified: num(row.quantityUnqualified),
      checkResult: clean(row.checkResult) || '—',
      vendorName: clean(row.vendorName) || '—',
      inspectDate: String(row.inspectDate || '').slice(0, 10) || '—'
    }))

  return {
    text,
    title: '当前不合格来料明细（最近 10 条）',
    columns: [
      { prop: 'iqcCode', label: '来料检验单', width: 150 },
      { prop: 'itemName', label: '物料' },
      { prop: 'quantityUnqualified', label: '不合格数', width: 90, align: 'right' },
      { prop: 'checkResult', label: '判定', width: 90 },
      { prop: 'vendorName', label: '供应商', width: 140 },
      { prop: 'inspectDate', label: '检测日期', width: 110 }
    ],
    rows,
    source: '数据来源：来料检验单 qc_iqc、采购退供单 wm_rt_vendor 实时读取；统计范围为全部已完成的来料检验单'
  }
}

export function knowledgeFallback(topic) {
  if (topic === 'unqualified') {
    return [
      '知识库没有收录「不合格物料处理」的对应资料，但这个问题系统里有明确的处理路径，按实际实现回答：',
      UNQUALIFIED_PATHS.join('\n'),
      UNQUALIFIED_LIMITS,
      '要看当前有哪些不合格数量和待处理退供单，切换到「业务助手」再问同一句即可。'
    ].join('\n')
  }
  if (topic === 'inspection-items') {
    return [
      '知识库没有收录「检验项目」的对应资料，但系统里的检查内容是有来源的：来料检验的检测项来自「质量管理 → 质检方案」。',
      '方案先绑定物料，方案下的检测项（检验方法、合格范围、检验工具、缺陷等级）就是逐项要检查的内容；没有配置方案时，检验单只记录检测数量、合格数量、不合格数量和判定结论。',
      '要看某个物料具体配置了哪些检测项，切换到「业务助手」再问同一句即可。'
    ].join('\n')
  }
  return ''
}

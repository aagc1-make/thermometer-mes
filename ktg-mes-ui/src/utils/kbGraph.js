export const entityTypes = {
  Item: { label: '物料', color: '#537ca6' },
  Product: { label: '产品', color: '#2b557d' },
  Process: { label: '工序', color: '#5b867b' },
  Equipment: { label: '设备', color: '#a0784c' },
  InspectionItem: { label: '检验项', color: '#806e91' },
  Workorder: { label: '工单', color: '#8c7070' },
  Defect: { label: '缺陷', color: '#aa675f' },
  Dept: { label: '部门', color: '#777c99' },
  Document: { label: '文档', color: '#798b9e' }
}

export const graphRelations = {
  USED_IN: '用于',
  HAS_PROCESS: '包含工序',
  CHECKED_BY: '检验项',
  REQUIRES: '需要设备',
  MAY_CAUSE: '可能涉及',
  MENTIONS: '提及'
}

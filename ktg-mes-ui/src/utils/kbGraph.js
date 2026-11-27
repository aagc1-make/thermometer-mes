export const entityTypes = {
  Item: { label: '物料', color: '#297c70', tint: '#e2f1eb', icon: 'el-icon-box' },
  Product: { label: '产品', color: '#5552ab', tint: '#ebeafa', icon: 'el-icon-goods' },
  Process: { label: '工序', color: '#9b5f26', tint: '#fbecd9', icon: 'el-icon-set-up' },
  Equipment: { label: '设备', color: '#486c94', tint: '#e4edf6', icon: 'el-icon-cpu' },
  InspectionItem: { label: '检验项', color: '#9a486c', tint: '#f8e7ee', icon: 'el-icon-finished' },
  Workorder: { label: '工单', color: '#5f7145', tint: '#edf1de', icon: 'el-icon-tickets' },
  Defect: { label: '缺陷', color: '#a44d39', tint: '#fae8df', icon: 'el-icon-warning-outline' },
  Dept: { label: '部门', color: '#426879', tint: '#e1edf1', icon: 'el-icon-office-building' },
  Document: { label: '文档', color: '#6a5c99', tint: '#eee9f7', icon: 'el-icon-document' }
}

const unknownType = { color: '#586573', tint: '#eceff1', icon: 'el-icon-connection' }

export function entityStyle(type) {
  return entityTypes[type] || { ...unknownType, label: type || '实体' }
}

export const graphRelations = {
  USED_IN: '用于',
  HAS_PROCESS: '包含工序',
  CHECKED_BY: '检验项',
  REQUIRES: '需要设备',
  MAY_CAUSE: '可能涉及',
  MENTIONS: '提及'
}

import request from '@/utils/request'

// 贯通单据清理：候选工单 / 删除预览 / 执行清理
export function listChainCandidates(params) {
  return request({
    url: '/mes/pro/chain/candidates',
    method: 'get',
    params: params
  })
}

export function previewChainCleanup(workorderIds) {
  return request({
    url: '/mes/pro/chain/preview',
    method: 'post',
    data: { workorderIds: workorderIds }
  })
}

export function executeChainCleanup(workorderIds, confirmText) {
  return request({
    url: '/mes/pro/chain/execute',
    method: 'post',
    data: { workorderIds: workorderIds, confirmText: confirmText },
    timeout: 60000
  })
}

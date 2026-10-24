import request from '@/utils/request'

// 查询库存流水列表
export function listTransaction(query) {
  return request({
    url: '/mes/wm/transaction/list',
    method: 'get',
    params: query
  })
}

// 查询库存流水详细
export function getTransaction(transactionId) {
  return request({
    url: '/mes/wm/transaction/' + transactionId,
    method: 'get'
  })
}

// 导出库存流水
export function exportTransaction(query) {
  return request({
    url: '/mes/wm/transaction/export',
    method: 'post',
    params: query
  })
}

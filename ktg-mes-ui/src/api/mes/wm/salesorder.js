import request from '@/utils/request'

// 查询客户订单列表
export function listSalesorder(query) {
  return request({
    url: '/mes/wm/salesorder/list',
    method: 'get',
    params: query
  })
}

// 查询客户订单详细（含订单行）
export function getSalesorder(orderId) {
  return request({
    url: '/mes/wm/salesorder/' + orderId,
    method: 'get'
  })
}

// 新增客户订单
export function addSalesorder(data) {
  return request({
    url: '/mes/wm/salesorder',
    method: 'post',
    data: data
  })
}

// 修改客户订单
export function updateSalesorder(data) {
  return request({
    url: '/mes/wm/salesorder',
    method: 'put',
    data: data
  })
}

// 删除客户订单
export function delSalesorder(orderIds) {
  return request({
    url: '/mes/wm/salesorder/' + orderIds,
    method: 'delete'
  })
}

// 确认下单
export function confirmSalesorder(orderId) {
  return request({
    url: '/mes/wm/salesorder/confirm/' + orderId,
    method: 'put'
  })
}

// 一键生成生产工单
export function generateWorkorder(orderId) {
  return request({
    url: '/mes/wm/salesorder/generateWorkorder/' + orderId,
    method: 'post'
  })
}

// 生成出货检验单
export function generateOqc(orderId) {
  return request({
    url: '/mes/wm/salesorder/generateOqc/' + orderId,
    method: 'post'
  })
}

// 生成发货通知单
export function generateNotice(orderId) {
  return request({
    url: '/mes/wm/salesorder/generateNotice/' + orderId,
    method: 'post'
  })
}

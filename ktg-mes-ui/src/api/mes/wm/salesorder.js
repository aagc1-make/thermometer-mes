import request from '@/utils/request'

const base = '/mes/wm/salesorder'

export function listSalesOrders(params) {
  return request({ url: base + '/list', method: 'get', params })
}

export function getSalesOrder(id) {
  return request({ url: base + '/' + id, method: 'get' })
}

export function saveSalesOrder(data) {
  return request({ url: base, method: data.orderId ? 'put' : 'post', data })
}

export function deleteSalesOrder(id) {
  return request({ url: base + '/' + id, method: 'delete' })
}

export function confirmSalesOrder(id) {
  return request({ url: base + '/confirm/' + id, method: 'put' })
}

export function generateSalesOrderDocuments(id, action) {
  return request({ url: base + '/' + action + '/' + id, method: 'post' })
}

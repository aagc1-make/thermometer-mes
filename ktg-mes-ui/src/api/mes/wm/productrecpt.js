import request from '@/utils/request'

// 查询产品入库录列表
export function listProductrecpt(query) {
  return request({
    url: '/mes/wm/productrecpt/list',
    method: 'get',
    params: query
  })
}

// 查询产品入库录详细
export function getProductrecpt(recptId) {
  return request({
    url: '/mes/wm/productrecpt/' + recptId,
    method: 'get'
  })
}

/**
 * 检查入库数量与上架数量是否一致
 * @param  recptId 
 * @returns 
 */
export function checkQuantity(recptId) {
  return request({
    url: '/mes/wm/productrecpt/checkQuantity/' + recptId,
    method: 'get'
  })
}

// 新增产品入库录
export function addProductrecpt(data) {
  return request({
    url: '/mes/wm/productrecpt',
    method: 'post',
    data: data
  })
}

// 修改产品入库录
export function updateProductrecpt(data) {
  return request({
    url: '/mes/wm/productrecpt',
    method: 'put',
    data: data
  })
}

// 删除产品入库录
export function delProductrecpt(recptId) {
  return request({
    url: '/mes/wm/productrecpt/' + recptId,
    method: 'delete'
  })
}

//执行入库
export function execute(recptId) {
  return request({
    url: '/mes/wm/productrecpt/' + recptId,
    method: 'put'
  })
}

// 上架-库区候选（含余量/推荐/混放拦截）
export function locationOptions(params) {
  return request({
    url: '/mes/wm/productrecpt/locationOptions',
    method: 'get',
    params: params
  })
}

// 上架-库位候选（含余量/推荐/混放拦截）
export function areaOptions(params) {
  return request({
    url: '/mes/wm/productrecpt/areaOptions',
    method: 'get',
    params: params
  })
}

// 确认上架（待上架 -> 已上架）
export function confirmStocking(recptId) {
  return request({
    url: '/mes/wm/productrecpt/confirmStocking/' + recptId,
    method: 'post'
  })
}

import request from '@/utils/request'

// 基于已执行采购入库生成可调整的退供草稿。
export function createFromReceipt(recptId) {
  return request({ url: '/mes/wm/rtvendor/fromReceipt/' + recptId, method: 'post' })
}

// 基于来料检验未入库不合格数量（重新）生成退供草稿；取消/删除后可再次生成。
export function createFromIqc(iqcId) {
  return request({ url: '/mes/wm/rtvendor/fromIqc/' + iqcId, method: 'post' })
}

// 查询供应商退货列表
export function listRtvendor(query) {
  return request({
    url: '/mes/wm/rtvendor/list',
    method: 'get',
    params: query
  })
}

// 查询供应商退货详细
export function getRtvendor(rtId) {
  return request({
    url: '/mes/wm/rtvendor/' + rtId,
    method: 'get'
  })
}

/**
 * 检查退货数量与拣货数量是否一致
 * @param  rtId 
 * @returns 
 */
export function checkQuantity(rtId) {
  return request({
    url: '/mes/wm/rtvendor/checkQuantity/' + rtId,
    method: 'get'
  })
}

// 新增供应商退货
export function addRtvendor(data) {
  return request({
    url: '/mes/wm/rtvendor',
    method: 'post',
    data: data
  })
}

// 修改供应商退货
export function updateRtvendor(data) {
  return request({
    url: '/mes/wm/rtvendor',
    method: 'put',
    data: data
  })
}

// 删除供应商退货
export function delRtvendor(rtId) {
  return request({
    url: '/mes/wm/rtvendor/' + rtId,
    method: 'delete'
  })
}

//执行入库
export function execute(rtId) {
  return request({
    url: '/mes/wm/rtvendor/' + rtId,
    method: 'put'
  })
}

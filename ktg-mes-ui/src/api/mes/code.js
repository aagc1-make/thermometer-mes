import request from '@/utils/request'

// 新增弹窗打开时预取一个业务单号（灰色只读展示，保存时原样回传）
// 传 referenceCode（订单号/工单号）时取链号，日期段与流水与参照单号完全一致
export function nextBizCode(ruleCode, referenceCode) {
  return request({
    url: '/mes/code/next/' + ruleCode,
    method: 'get',
    params: referenceCode ? { referenceCode } : {}
  })
}

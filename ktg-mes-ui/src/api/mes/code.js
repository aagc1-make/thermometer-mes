import request from '@/utils/request'

export function previewMesCode(rule, referenceCode) {
  return request({
    url: '/mes/code/next/' + rule,
    method: 'get',
    params: referenceCode ? { referenceCode } : undefined
  })
}

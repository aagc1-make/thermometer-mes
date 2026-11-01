import request from '@/utils/request'

/** 当前账号的人脸录入状态 */
export function getFaceStatus() {
  return request({
    url: '/system/user/face/status',
    method: 'get'
  })
}

/** 录入或更新人脸（需要登录密码与授权确认） */
export function enrollFace(data) {
  return request({
    url: '/system/user/face/enroll',
    method: 'post',
    data,
    timeout: 30000
  })
}

/** 解绑人脸 */
export function unbindFace(data) {
  return request({
    url: '/system/user/face',
    method: 'delete',
    data,
    timeout: 20000
  })
}

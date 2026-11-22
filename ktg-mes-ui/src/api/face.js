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

/** 追加一个补充角度（主模板不变，最多 3 个） */
export function appendFace(data) {
  return request({
    url: '/system/user/face/append',
    method: 'post',
    data,
    timeout: 30000
  })
}

/** 管理员：人脸录入信息列表 */
export function listFaceTemplates(params) {
  return request({
    url: '/system/user/face/admin/list',
    method: 'get',
    params
  })
}

/** 管理员：停用或允许某账号的人脸登录 */
export function changeFaceLoginStatus(userId, enabled) {
  return request({
    url: '/system/user/face/admin/' + userId + '/status',
    method: 'put',
    data: { enabled }
  })
}

/** 管理员：重置某账号的人脸模板 */
export function resetFaceTemplate(userId) {
  return request({
    url: '/system/user/face/admin/' + userId,
    method: 'delete'
  })
}

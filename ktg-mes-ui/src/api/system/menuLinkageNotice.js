import request from '@/utils/request'

export function listUnreadMenuLinkageNotices() {
  return request({
    url: '/system/menu-linkage-notice/unread',
    method: 'get'
  })
}

export function markMenuLinkageNoticeRead(menuPath) {
  return request({
    url: '/system/menu-linkage-notice/read',
    method: 'put',
    data: { menuPath }
  })
}

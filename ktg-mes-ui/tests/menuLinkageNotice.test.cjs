const test = require('node:test')
const assert = require('node:assert/strict')

const {
  MENU_NOTICE_POLL_INTERVAL,
  normalizeMenuPath,
  isMenuPathUnread,
  hasUnreadRouteTree
} = require('../src/utils/menuLinkageNotice')

test('uses the approved thirty second polling interval', () => {
  assert.equal(MENU_NOTICE_POLL_INTERVAL, 30000)
})

test('normalizes query hash and trailing slash from a menu path', () => {
  assert.equal(normalizeMenuPath('/mes/qc/iqc/?page=1#top'), '/mes/qc/iqc')
})

test('matches unread menus by normalized absolute path', () => {
  assert.equal(isMenuPathUnread(['/mes/qc/iqc'], '/mes/qc/iqc/'), true)
  assert.equal(isMenuPathUnread(['/mes/qc/oqc'], '/mes/qc/iqc'), false)
})

test('reports an unread child for relative and regrouped absolute routes', () => {
  assert.equal(hasUnreadRouteTree(
    [{ path: 'iqc', meta: { title: '来料检验' } }],
    '/mes/qc',
    ['/mes/qc/iqc']
  ), true)

  assert.equal(hasUnreadRouteTree(
    [{ path: '/mes/wm/itemrecpt', meta: { title: '采购入库' } }],
    '/_grp_warehouse_0',
    ['/mes/wm/itemrecpt']
  ), true)
})

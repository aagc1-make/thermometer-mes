const MENU_NOTICE_POLL_INTERVAL = 30000

function normalizeMenuPath(value) {
  if (!value) return ''
  const path = String(value).split('#')[0].split('?')[0]
  if (path === '/') return path
  return path.replace(/\/+$/, '') || '/'
}

function resolveMenuPath(basePath, routePath) {
  const child = normalizeMenuPath(routePath)
  if (child.charAt(0) === '/') return child
  const base = normalizeMenuPath(basePath)
  return normalizeMenuPath((base === '/' ? '' : base) + '/' + child)
}

function isMenuPathUnread(unreadPaths, menuPath) {
  const target = normalizeMenuPath(menuPath)
  return (unreadPaths || []).some(path => normalizeMenuPath(path) === target)
}

function hasUnreadRouteTree(routes, basePath, unreadPaths) {
  return (routes || []).some(route => {
    if (route.hidden) return false
    const fullPath = resolveMenuPath(basePath, route.path)
    if (route.children && route.children.length) {
      return hasUnreadRouteTree(route.children, fullPath, unreadPaths)
    }
    return isMenuPathUnread(unreadPaths, fullPath)
  })
}

module.exports = {
  MENU_NOTICE_POLL_INTERVAL,
  normalizeMenuPath,
  resolveMenuPath,
  isMenuPathUnread,
  hasUnreadRouteTree
}

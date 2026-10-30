import {
  listUnreadMenuLinkageNotices,
  markMenuLinkageNoticeRead
} from '@/api/system/menuLinkageNotice'

const { normalizeMenuPath } = require('@/utils/menuLinkageNotice')

// 轮询与页面切换可能在同一毫秒内触发同一请求，
// RuoYi 的 request.js 会把 1 秒内的相同请求判为"重复提交"并抛错，
// 界面上就会出现"数据正在处理，请勿重复提交"的弹窗。
// 这里在 store 层做 in-flight 去重，保证同一时刻只发出一个请求。
let refreshInFlight = null
const readInFlight = {}

const menuNotice = {
  state: {
    unreadMenuPaths: []
  },

  mutations: {
    SET_UNREAD_MENU_PATHS: (state, paths) => {
      state.unreadMenuPaths = (paths || []).map(normalizeMenuPath)
    },
    REMOVE_UNREAD_MENU_PATH: (state, menuPath) => {
      const target = normalizeMenuPath(menuPath)
      state.unreadMenuPaths = state.unreadMenuPaths.filter(path => path !== target)
    }
  },

  actions: {
    refreshMenuLinkageNotices({ commit }) {
      if (refreshInFlight) {
        return refreshInFlight
      }
      refreshInFlight = listUnreadMenuLinkageNotices()
        .then(response => {
          commit('SET_UNREAD_MENU_PATHS', response.data || [])
          return response.data || []
        })
        .finally(() => {
          refreshInFlight = null
        })
      return refreshInFlight
    },

    markMenuLinkageNoticeRead({ commit, state }, menuPath) {
      const target = normalizeMenuPath(menuPath)
      if (!target) {
        return Promise.resolve()
      }
      // 已经是已读状态就不再发请求（避免重复点击产生重复 PUT）
      if (!(state.unreadMenuPaths || []).includes(target)) {
        return Promise.resolve()
      }
      if (readInFlight[target]) {
        return readInFlight[target]
      }
      readInFlight[target] = markMenuLinkageNoticeRead(target)
        .then(() => {
          commit('REMOVE_UNREAD_MENU_PATH', target)
        })
        .finally(() => {
          delete readInFlight[target]
        })
      return readInFlight[target]
    }
  }
}

export default menuNotice

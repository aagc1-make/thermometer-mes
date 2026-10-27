import {
  listUnreadMenuLinkageNotices,
  markMenuLinkageNoticeRead
} from '@/api/system/menuLinkageNotice'

const { normalizeMenuPath } = require('@/utils/menuLinkageNotice')

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
      return listUnreadMenuLinkageNotices().then(response => {
        commit('SET_UNREAD_MENU_PATHS', response.data || [])
        return response.data || []
      })
    },
    markMenuLinkageNoticeRead({ commit }, menuPath) {
      const target = normalizeMenuPath(menuPath)
      return markMenuLinkageNoticeRead(target).then(() => {
        commit('REMOVE_UNREAD_MENU_PATH', target)
      })
    }
  }
}

export default menuNotice

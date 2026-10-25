import Cookies from 'js-cookie'

const state = {
  sidebar: {
    opened: true, // 固定展开，不提供收起功能
    withoutAnimation: false,
    hide: false
  },
  device: 'desktop',
  size: Cookies.get('size') || 'medium'
}

const mutations = {
  TOGGLE_SIDEBAR: state => {
    // 已禁用收起功能：始终展开
    state.sidebar.opened = true
    state.sidebar.withoutAnimation = false
    Cookies.set('sidebarStatus', 1)
  },
  CLOSE_SIDEBAR: (state, withoutAnimation) => {
    // 已禁用收起功能：忽略外部收起请求（如窗口 resize）
    state.sidebar.opened = true
    state.sidebar.withoutAnimation = withoutAnimation
    Cookies.set('sidebarStatus', 1)
  },
  TOGGLE_DEVICE: (state, device) => {
    state.device = device
  },
  SET_SIZE: (state, size) => {
    state.size = size
    Cookies.set('size', size)
  },
  SET_SIDEBAR_HIDE: (state, status) => {
    state.sidebar.hide = status
  }
}

const actions = {
  toggleSideBar({ commit }) {
    commit('TOGGLE_SIDEBAR')
  },
  closeSideBar({ commit }, { withoutAnimation }) {
    commit('CLOSE_SIDEBAR', withoutAnimation)
  },
  toggleDevice({ commit }, device) {
    commit('TOGGLE_DEVICE', device)
  },
  setSize({ commit }, size) {
    commit('SET_SIZE', size)
  },
  toggleSideBarHide({ commit }, status) {
    commit('SET_SIDEBAR_HIDE', status)
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}

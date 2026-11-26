<template>
  <div :class="classObj" class="app-wrapper mes-workspace" :style="{'--current-color': theme}"
    @keydown.esc="device === 'mobile' && sidebar.opened && handleClickOutside()">
    <div v-if="device==='mobile'&&sidebar.opened" class="drawer-bg" @click="handleClickOutside"/>
    <!-- 可访问性：键盘用户可跳过导航直达主内容（WCAG 2.4.1） -->
    <a class="skip-to-content" href="#main-content" @click.prevent="skipToContent">跳到主内容</a>
    <rail v-show="device !== 'mobile' || sidebar.opened" />
    <sidebar v-if="!sidebar.hide && !isHomePage" v-show="!isAssistantWorkspace && (device !== 'mobile' || sidebar.opened)" class="sidebar-container" />
    <div class="fixed-header">
      <navbar />
    </div>
    <div :class="{ hasTagsView: needTagsView && !isAssistantWorkspace }" class="main-container">
      <tags-view v-if="needTagsView" v-show="!isAssistantWorkspace" class="workspace-tabs" />
      <app-main />
      <right-panel>
        <settings />
      </right-panel>
    </div>
  </div>
</template>

<script>
import RightPanel from '@/components/RightPanel'
import { AppMain, Navbar, Settings, Sidebar, TagsView } from './components'
import Rail from './components/Rail'
import ResizeMixin from './mixin/ResizeHandler'
import { mapState } from 'vuex'

const {
  MENU_NOTICE_POLL_INTERVAL
} = require('@/utils/menuLinkageNotice')

export default {
  name: 'Layout',
  components: {
    AppMain,
    Navbar,
    RightPanel,
    Settings,
    Sidebar,
    Rail,
    TagsView
  },
  mixins: [ResizeMixin],
  data() {
    return {
      menuNoticeTimer: null
    }
  },
  computed: {
    ...mapState({
      theme: state => state.settings.theme,
      sidebar: state => state.app.sidebar,
      device: state => state.app.device,
      needTagsView: state => state.settings.tagsView
    }),
    // 首页（驾驶舱）隐藏左侧菜单栏，把横向空间让给流水线与表格；rail 图标导航保留
    isHomePage() {
      const path = this.$route.path || ''
      return path === '/index' || path === '/' || path === '/index/'
    },
    isAssistantWorkspace() {
      return /^\/mes\/ai\/chat\/?$/.test(this.$route.path)
    },
    classObj() {
      return {
        hideSidebar: this.device === 'mobile' ? !this.sidebar.opened : this.isHomePage,
        sidebarHidden: this.sidebar.hide,
        assistantWorkspace: this.isAssistantWorkspace,
        openSidebar: this.device !== 'mobile' || this.sidebar.opened,
        withoutAnimation: this.sidebar.withoutAnimation,
        mobile: this.device === 'mobile'
      }
    }
  },
  watch: {
    '$route.path'(path) {
      // 说明：不再"进入即清除"，改为"点击菜单项时清除"（见 SidebarItem.handleMenuOpen）。
      // 原因：左栏点击域会跳该域首页，若进入即清除，蓝点会瞬间被自己点掉，用户看不到提示。
      this.touchMenuNotice(path)
    }
  },
  mounted() {
    this.$store.dispatch('refreshMenuLinkageNotices').catch(() => {})
    this.menuNoticeTimer = window.setInterval(() => {
      this.$store.dispatch('refreshMenuLinkageNotices').catch(() => {})
    }, MENU_NOTICE_POLL_INTERVAL)
  },
  beforeDestroy() {
    if (this.menuNoticeTimer) {
      window.clearInterval(this.menuNoticeTimer)
      this.menuNoticeTimer = null
    }
  },
  methods: {
    // 保留占位：蓝点清除统一由菜单点击触发，避免与侧边栏重复发 PUT 请求
    touchMenuNotice() {},
    // 跳过导航：把焦点移到主内容区，方便读屏/键盘用户
    skipToContent() {
      const el = document.getElementById('main-content')
      if (el) el.focus()
    },
    handleClickOutside() {
      this.$store.dispatch('app/closeSideBar', { withoutAnimation: false }).then(() => {
        const toggle = this.$el.querySelector('#hamburger-container')
        if (toggle) toggle.focus()
      })
    }
  }
}
</script>

<style lang="scss" scoped>
  @import "~@/assets/styles/mixin.scss";

  .app-wrapper {
    @include clearfix;
    position: relative;
    height: 100%;
    width: 100%;

    &.mobile.openSidebar {
      position: fixed;
      top: 0;
    }
  }

  .drawer-bg {
    background: rgba(37, 44, 53, .30);
    position: fixed;
    inset: var(--mes-header-height) 0 0;
    z-index: 999;
  }

  .fixed-header {
    position: fixed;
    top: 0;
    right: 0;
    left: 0;
    z-index: 1010;
  }
</style>

<template>
  <div :class="classObj" class="app-wrapper" :style="{'--current-color': theme}">
    <div v-if="device==='mobile'&&sidebar.opened" class="drawer-bg" @click="handleClickOutside"/>
    <!-- 可访问性：键盘用户可跳过导航直达主内容（WCAG 2.4.1） -->
    <a class="skip-to-content" href="#main-content" @click.prevent="skipToContent">跳到主内容</a>
    <rail />
    <sidebar v-if="!sidebar.hide" class="sidebar-container" />
    <div :class="{hasTagsView:needTagsView,sidebarHide:sidebar.hide}" class="main-container">
      <div class="fixed-header">
        <navbar />
        <tags-view v-if="needTagsView" />
      </div>
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
import variables from '@/assets/styles/variables.scss'

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
      sideTheme: state => state.settings.sideTheme,
      sidebar: state => state.app.sidebar,
      device: state => state.app.device,
      needTagsView: state => state.settings.tagsView
    }),
    classObj() {
      return {
        hideSidebar: this.device === 'mobile' && !this.sidebar.opened,
        openSidebar: this.device !== 'mobile' || this.sidebar.opened,
        withoutAnimation: this.sidebar.withoutAnimation,
        mobile: this.device === 'mobile'
      }
    },
    variables() {
      return variables;
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
      this.$store.dispatch('app/closeSideBar', { withoutAnimation: false })
    }
  }
}
</script>

<style lang="scss" scoped>
  @import "~@/assets/styles/mixin.scss";
  @import "~@/assets/styles/variables.scss";

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
    background: #000;
    opacity: 0.3;
    width: 100%;
    top: 0;
    height: 100%;
    position: absolute;
    z-index: 999;
  }

  .fixed-header {
    position: fixed;
    top: 0;
    right: 0;
    left: 0;
    z-index: 9;
    width: 100%;
  }

  .sidebarHide .fixed-header {
    width: 100%;
  }

  .mobile .fixed-header {
    width: 100%;
  }
</style>

<template>
  <div class="navbar">
    <hamburger v-if="device==='mobile'" id="hamburger-container" :is-active="sidebar.opened" class="hamburger-container" role="button" tabindex="0" aria-label="菜单" :aria-expanded="sidebar.opened" @keyup.enter.native="toggleSideBar" @keyup.space.native.prevent="toggleSideBar" @toggleClick="toggleSideBar" />

    <brand v-if="$store.state.settings.sidebarLogo" />
    <div v-if="!topNav" class="nav-divider" aria-hidden="true"></div>
    <breadcrumb id="breadcrumb-container" class="breadcrumb-container" v-if="!topNav"/>
    <top-nav id="topmenu-container" class="topmenu-container" v-if="topNav"/>

    <div class="right-menu">
      <template v-if="device!=='mobile'">
        <div class="tool-pill" role="group" aria-label="快捷工具">
        <search id="header-search" class="right-menu-item" aria-label="全局搜索" />

        <screenfull id="screenfull" class="right-menu-item hover-effect" aria-label="切换全屏" />

        <el-tooltip content="布局大小" effect="dark" placement="bottom">
          <size-select id="size-select" class="right-menu-item hover-effect" aria-label="调整布局大小" />
        </el-tooltip>
        </div>
      </template>

      <el-dropdown class="avatar-container" trigger="click">
        <div class="avatar-wrapper" role="button" tabindex="0" aria-label="用户菜单" aria-haspopup="true">
          <img :src="avatar" class="user-avatar" alt="用户头像" width="40" height="40" loading="lazy" decoding="async" @error="handleAvatarError">
          <i class="el-icon-caret-bottom" aria-hidden="true" />
        </div>
        <el-dropdown-menu slot="dropdown">
          <router-link to="/user/profile">
            <el-dropdown-item>个人中心</el-dropdown-item>
          </router-link>
          <el-dropdown-item @click.native="setting = true">
            <span>布局设置</span>
          </el-dropdown-item>
          <li class="el-dropdown-menu__item is-disabled">按钮字体：OPPO Sans 4.0</li>
          <el-dropdown-item divided @click.native="logout">
            <span>退出登录</span>
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import Breadcrumb from '@/components/Breadcrumb'
import TopNav from '@/components/TopNav'
import Hamburger from '@/components/Hamburger'
import Screenfull from '@/components/Screenfull'
import SizeSelect from '@/components/SizeSelect'
import Search from '@/components/HeaderSearch'
import Brand from './Brand'

export default {
  components: {
    Breadcrumb,
    TopNav,
    Hamburger,
    Screenfull,
    SizeSelect,
    Search,
    Brand
  },
  computed: {
    ...mapGetters([
      'sidebar',
      'avatar',
      'device'
    ]),
    setting: {
      get() {
        return this.$store.state.settings.showSettings
      },
      set(val) {
        this.$store.dispatch('settings/changeSetting', {
          key: 'showSettings',
          value: val
        })
      }
    },
    topNav: {
      get() {
        return this.$store.state.settings.topNav
      }
    }
  },
  methods: {
    // 头像加载失败（如数据库里存了不存在的头像路径）时回退到默认头像，避免出现碎图
    handleAvatarError(e) {
      const fallback = require('@/assets/images/user.png')
      if (e && e.target && e.target.src.indexOf(fallback) === -1) {
        e.target.src = fallback
      }
    },
    toggleSideBar() {
      this.$store.dispatch('app/toggleSideBar')
    },
    async logout() {
      this.$confirm('确定注销并退出系统吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('LogOut').then(() => {
          location.href = '/index';
        })
      }).catch(() => {});
    }
  }
}
</script>

<style lang="scss" scoped>
.navbar {
  display: flex;
  align-items: center;
  gap: 14px;
  height: var(--mes-header-height);
  padding: 0 clamp(12px, 1.6vw, 24px);
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-border);

  .hamburger-container {
    display: grid;
    place-items: center;
    flex: 0 0 34px;
    height: 34px;
    padding: 0 !important;
    border-radius: 6px;
    color: var(--color-text);
    cursor: pointer;

    svg {
      fill: currentColor;
    }
  }

  .nav-divider {
    flex: 0 0 1px;
    height: 20px;
    background: var(--color-border);
  }

  .breadcrumb-container,
  .topmenu-container {
    min-width: 0;
    overflow: hidden;
    white-space: nowrap;
  }

  .right-menu {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-shrink: 0;
    margin-left: auto;
  }

  .tool-pill {
    display: flex;
    align-items: center;
    gap: 2px;
  }

  .right-menu-item,
  .avatar-wrapper {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 34px;
    height: 34px;
    padding: 0 6px;
    border-radius: 6px;
    color: var(--color-text-secondary);
    font-size: 16px;
    cursor: pointer;
  }

  .avatar-wrapper {
    gap: 5px;

    .user-avatar {
      width: 28px;
      height: 28px;
      border: 1px solid var(--color-border);
      border-radius: 50%;
      object-fit: cover;
    }

    .el-icon-caret-bottom {
      font-size: 10px;
    }
  }

  .hamburger-container:hover,
  .right-menu-item:hover,
  .avatar-wrapper:hover {
    color: var(--color-primary);
    background: var(--color-rail-hover);
  }
}

@media (max-width: 600px) {
  .navbar {
    gap: 7px;

    .nav-divider,
    .breadcrumb-container {
      display: none;
    }
  }
}
</style>

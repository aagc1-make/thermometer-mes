<template>
  <div class="navbar">
    <hamburger v-if="device==='mobile'" id="hamburger-container" :is-active="sidebar.opened" class="hamburger-container" @toggleClick="toggleSideBar" />

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

export default {
  components: {
    Breadcrumb,
    TopNav,
    Hamburger,
    Screenfull,
    SizeSelect,
    Search
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
  height: 50px;
  position: relative;
  background: #fff;
  box-shadow: none;
  display: flex;
  align-items: center;
  padding: 0 14px 0 10px;
  gap: 10px;

  /* 折叠按钮：白透圆角块（图标深蓝灰） */
  .hamburger-container {
    flex: 0 0 auto;
    height: 36px;
    width: 36px;
    line-height: 36px;
    text-align: center;
    border-radius: 12px;
    background: rgba(255, 255, 255, 0.6);
    transition: transform 0.2s ease, background 0.2s ease;
    -webkit-tap-highlight-color: transparent;

    svg {
      fill: #3f5170;
    }

    &:hover {
      transform: scale(1.06);
      background: rgba(255, 255, 255, 0.9);
    }
  }

  .nav-divider {
    flex: 0 0 auto;
    width: 1px;
    height: 22px;
    background: #c2d1ea;
  }

  .breadcrumb-container {
    flex: 0 1 auto;
    overflow: hidden;
    white-space: nowrap;
  }

  .topmenu-container {
    position: absolute;
    left: 50px;
  }

  .errLog-container {
    display: inline-block;
    vertical-align: top;
  }

  .right-menu {
    margin-left: auto;
    display: flex;
    align-items: center;
    height: 100%;

    &:focus {
      outline: none;
    }

    /* 快捷工具：整体胶囊容器，按钮各自圆形 */
    .tool-pill {
      display: flex;
      align-items: center;
      background: rgba(255, 255, 255, 0.55);
      border-radius: 999px;
      padding: 3px 6px;
      gap: 2px;

      .right-menu-item {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 32px;
        height: 32px;
        padding: 0;
        border-radius: 50%;
        font-size: 17px;
        color: #4b5c7e;
        vertical-align: baseline;

        &.hover-effect {
          cursor: pointer;
          transition: background 0.2s, transform 0.2s;

          &:hover {
            background: #ffffff;
            transform: translateY(-1px);
          }
        }
      }

      /* 每个工具按钮 hover 一种柔和色 */
      .right-menu-item:nth-child(1):hover { color: #5b7fc7; }
      .right-menu-item:nth-child(2):hover { color: #7fa98b; }
      .right-menu-item:nth-child(3):hover { color: #c2a17b; }
    }

    .avatar-container {
      margin-left: 8px;
      height: auto;

      .avatar-wrapper {
        margin-top: 0;
        position: relative;
        display: flex;
        align-items: center;

        .user-avatar {
          cursor: pointer;
          width: 34px;
          height: 34px;
          border-radius: 50%;
          border: 2px solid #ffffff;
          box-shadow: 0 0 0 2px #b9cbe8, 0 3px 8px rgba(91, 127, 199, 0.22);
        }

        .el-icon-caret-bottom {
          cursor: pointer;
          position: static;
          margin-left: 5px;
          font-size: 12px;
          color: #6d7f9e;
        }
      }
    }
  }
}
</style>

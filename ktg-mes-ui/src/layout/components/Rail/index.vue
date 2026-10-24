<template>
  <div class="rail">
    <div
      v-for="d in domains"
      :key="d.key"
      class="rail-item"
      :class="{ active: d.key === activeKey }"
      :title="d.title"
      role="button"
      tabindex="0"
      :aria-label="d.title"
      @click="go(d)"
      @keyup.enter="go(d)"
    >
      <i :class="d.icon" aria-hidden="true"></i>
      <span class="rail-item__text">{{ d.title }}</span>
    </div>
  </div>
</template>

<script>
import { railDomains, findDomainByPath } from '../../rail-config'

export default {
  name: 'Rail',
  computed: {
    domains() {
      return railDomains
    },
    activeKey() {
      return findDomainByPath(this.$route.path).key
    }
  },
  methods: {
    go(domain) {
      if (domain.key === this.activeKey) return
      // 首页属于框架内置静态路由，不在 sidebarRouters 里，需直接指定
      const target = domain.key === 'home' ? '/index' : this.firstPath(domain)
      if (target) {
        this.$router.push(target).catch(() => {})
      }
    },
    /** 取该域下第一个可点击的叶子菜单路径（菜单 path 是相对段，需要逐级拼接成绝对路径） */
    firstPath(domain) {
      const routers = this.$store.getters.sidebarRouters || []
      const join = (base, p) => {
        if (!p) return base || '/'
        if (String(p).charAt(0) === '/') return p
        return (base === '/' ? '' : base || '') + '/' + p
      }
      const root = routers.find(r => {
        const full = join('', r.path)
        return domain.roots.some(p => full === p || full.indexOf(p + '/') === 0)
      })
      if (!root) return null
      const walk = (node, base) => {
        const full = join(base, node.path)
        if (!node.children || node.children.length === 0) return full
        for (const child of node.children) {
          const hit = walk(child, full)
          if (hit) return hit
        }
        return full
      }
      return walk(root, '')
    }
  }
}
</script>

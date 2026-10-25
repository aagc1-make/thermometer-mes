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
      const target = this.firstPath(domain)
      if (target) {
        // 已在目标页时给予明确反馈（vue-router 对重复导航会静默中止）
        if (target === this.$route.path) {
          this.$message && this.$message.info('已在当前页面')
          return
        }
        this.$router.push(target).catch(() => {})
        return
      }
      const msg = `「${domain.title || domain.key}」下没有可用页面，请检查菜单权限`
      this.$message && this.$message.warning(msg)
      console.warn('[rail] 无法跳转：', domain.key, domain.roots)
    },
    /** 取该域下第一个「路由真实存在」的叶子菜单路径 */
    firstPath(domain) {
      const routers = this.$store.getters.sidebarRouters || []
      // 逐级拼接为绝对路径：兼容「相对段(md)」和「带斜杠段(/md)」两种菜单写法
      const join = (base, p) => {
        const a = String(base || '').replace(/\/+$/, '')
        const b = String(p || '').replace(/^\/+/, '').replace(/\/+$/, '')
        if (!b) return a || '/'
        const merged = (a + '/' + b).replace(/\/+/g, '/')
        return merged.charAt(0) === '/' ? merged : '/' + merged
      }
      // 首选：直接读取 vue-router 已注册的真实路由表（最可靠）
      const fromRouter = () => {
        if (!this.$router || typeof this.$router.getRoutes !== 'function') return []
        return this.$router
          .getRoutes()
          .filter(r => r && r.path && r.path !== '*' && r.path !== '/404')
          .filter(r => !r.redirect && String(r.path).indexOf(':') === -1)
          .filter(r => !r.children || r.children.length === 0)
          .map(r => r.path)
      }

      // 兜底：递归拍平 sidebarRouters，同时兼容「相对段」与「已是完整绝对路径」两种结构
      const fromSidebar = () => {
        const out = []
        const push = (p) => { if (p && out.indexOf(p) === -1) out.push(p) }
        const flatten = (list, base) => {
          (list || []).forEach(n => {
            if (!n || n.hidden) return
            const rel = join(base, n.path)
            const abs = String(n.path || '').charAt(0) === '/' ? String(n.path) : rel
            const isLeaf = !n.children || n.children.length === 0
            if (isLeaf) {
              push(rel)
              push(abs)
            } else {
              flatten(n.children, rel)
              if (abs !== rel) flatten(n.children, abs)
            }
          })
        }
        flatten(routers, '')
        return out
      }

      const all = fromRouter()
      const source = all.length ? all : fromSidebar()

      const matched = source.filter(p => domain.roots.some(root =>
        p === root || p.indexOf(root + '/') === 0
      ))
      if (!matched.length) {
        console.warn('[rail] 该域下没有匹配路由：', domain.key, domain.roots,
          '可用路由样例：', source.slice(0, 25))
        return null
      }
      // 只保留能被 vue-router 真正解析到的地址（排除目录节点 / 404 通配）
      const usable = matched.filter(p => {
        const r = this.$router.resolve(p)
        if (!r || !r.route) return false
        if (r.route.redirect) return false
        if (r.route.path === '/404' || r.route.path === '*') return false
        return true
      }).sort()
      if (!usable.length) {
        console.warn('[rail] 该域路由均不可用：', domain.key, matched)
        return null
      }
      return usable[0]
    }
  }
}
</script>

<template>
  <main class="mes-status">
    <h1>此页面需要访问权限</h1>
    <p>当前账号没有打开此页面的权限。如需访问，请联系系统管理员。</p>
    <div class="mes-status__actions">
      <router-link to="/index">返回工作台 <i class="el-icon-right" aria-hidden="true" /></router-link>
      <el-button @click="back">返回上一页</el-button>
    </div>
  </main>
</template>

<script>
export default {
  name: 'Page401',
  methods: {
    back() {
      if (this.$route.query.noGoBack) this.$router.push('/index')
      else this.$router.go(-1)
    }
  }
}
</script>

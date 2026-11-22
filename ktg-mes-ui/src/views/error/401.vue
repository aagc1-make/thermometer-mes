<template>
  <main class="mes-status-page" aria-labelledby="access-denied-title">
    <section class="mes-status-page__content">
      <p class="mes-status-page__code">401</p>
      <h1 id="access-denied-title" class="mes-page-heading">没有访问权限</h1>
      <p class="mes-status-page__detail">如需访问此页面，请联系系统管理员。</p>
      <div class="mes-status-page__actions">
        <router-link to="/" class="mes-button mes-button--primary">返回首页</router-link>
        <el-button icon="el-icon-arrow-left" @click="back">返回上一页</el-button>
      </div>
    </section>
  </main>
</template>

<script>
export default {
  name: 'Page401',
  methods: {
    back() {
      if (this.$route.query.noGoBack) {
        this.$router.push({ path: '/' })
      } else {
        this.$router.go(-1)
      }
    }
  }
}
</script>

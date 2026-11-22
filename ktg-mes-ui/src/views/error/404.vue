<template>
  <main class="mes-status-page" aria-labelledby="not-found-title">
    <section class="mes-status-page__content">
      <p class="mes-status-page__code">404</p>
      <h1 id="not-found-title" class="mes-page-heading">找不到页面</h1>
      <p class="mes-status-page__detail">请检查页面地址，或返回首页重新选择菜单。</p>
      <router-link to="/" class="mes-button mes-button--primary">返回首页</router-link>
    </section>
  </main>
</template>

<script>
export default { name: 'Page404' }
</script>

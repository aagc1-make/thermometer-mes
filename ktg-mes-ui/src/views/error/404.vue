<template>
  <main class="mes-status">
    <h1>找不到这个页面</h1>
    <p>页面可能已移动，或链接地址有误。可以从工作台重新打开业务模块。</p>
    <div class="mes-status__actions">
      <router-link to="/index">返回工作台 <i class="el-icon-right" aria-hidden="true" /></router-link>
    </div>
  </main>
</template>

<script>
export default { name: 'Page404' }
</script>

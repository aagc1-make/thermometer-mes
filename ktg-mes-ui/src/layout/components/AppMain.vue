<template>
  <main class="app-main" id="main-content" role="main" tabindex="-1" aria-label="主内容区">
    <transition name="fade-transform" mode="out-in">
      <keep-alive :include="cachedViews">
        <router-view :key="key" />
      </keep-alive>
    </transition>
  </main>
</template>

<script>
export default {
  name: 'AppMain',
  computed: {
    cachedViews() {
      return this.$store.state.tagsView.cachedViews
    },
    key() {
      return this.$route.path
    }
  }
}
</script>

<style lang="scss" scoped>
.app-main {
  min-height: 100vh;
  width: 100%;
  position: relative;
  overflow: hidden;
  padding-top: var(--mes-header-height);
}

.hasTagsView .app-main {
  padding-top: calc(var(--mes-header-height) + var(--mes-tabs-height));
}
</style>

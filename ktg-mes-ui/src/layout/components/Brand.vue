<template>
  <router-link class="brand" to="/" aria-label="红外测温枪 MES 首页">
    <span class="brand-mark" aria-hidden="true">
      <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
        <path d="M3.5 13.5 7 8l3 3.5 2-5 4.5 7M3.5 16h13" />
      </svg>
    </span>
    <span>红外测温枪 <b>MES</b></span>
  </router-link>
</template>

<style lang="scss" scoped>
.brand {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
  color: var(--color-text);
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;

  b {
    color: var(--color-primary);
    font-weight: inherit;
  }
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: var(--color-primary-soft);
  color: var(--color-primary);

  svg {
    width: 17px;
    height: 17px;
  }
}
</style>

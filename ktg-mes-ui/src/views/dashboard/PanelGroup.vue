<template>
  <section class="device-overview" aria-labelledby="home-device-title">
    <div class="device-overview__head">
      <div>
        <h2 id="home-device-title">设备概览</h2>
        <p>数据来源：设备台账</p>
      </div>
      <el-button v-if="error" size="small" @click="loadDevices">加载失败，点击重试</el-button>
      <el-button v-else size="small" @click="openDevices">
        查看设备台账 <i class="el-icon-arrow-right" aria-hidden="true" />
      </el-button>
    </div>

    <div v-loading="loading" class="device-overview__grid">
      <button
        v-for="card in cards"
        :key="card.key"
        class="device-card"
        :class="'device-card--' + card.key"
        type="button"
        @click="openDevices"
      >
        <span class="device-card__label">
          <span class="device-card__icon"><i :class="card.icon" aria-hidden="true" /></span>
          {{ card.label }}
        </span>
        <span class="device-card__value">
          <strong>{{ loading || error ? '—' : stats[card.key] }}</strong>
          <small>台</small>
        </span>
      </button>
    </div>
    <p v-if="!loading && !error && stats.other" class="device-overview__note">
      另有 {{ stats.other }} 台设备状态未归入以上分类，请查看设备台账。
    </p>
  </section>
</template>

<script>
import { listMachinery } from '@/api/mes/dv/machinery'
export default {
  data() { return { loading: false, error: false, stats: { total: 0, working: 0, repair: 0, stop: 0, other: 0 }, cards: [
    { key: 'total', label: '设备总数', icon: 'el-icon-monitor' }, { key: 'working', label: '生产中', icon: 'el-icon-video-play' },
    { key: 'repair', label: '维修中', icon: 'el-icon-setting' }, { key: 'stop', label: '停机', icon: 'el-icon-video-pause' }
  ] } },
  created() { this.loadDevices() },
  methods: {
    async loadDevices() {
      if (this.loading) return
      this.loading = true; this.error = false
      try {
        const rows = []; let pageNum = 1; let total = 0
        do {
          const result = await listMachinery({ pageNum, pageSize: 500 })
          const page = result.rows || []
          total = result.total == null ? rows.length + page.length : Number(result.total)
          if (!page.length && rows.length < total) throw new Error('设备数据分页异常')
          rows.push(...page); pageNum++
        } while (rows.length < total)
        const stats = { total: rows.length, working: 0, repair: 0, stop: 0, other: 0 }
        rows.forEach(r => { const key = { WORKING: 'working', REPAIR: 'repair', STOP: 'stop' }[r.status] || 'other'; stats[key]++ })
        this.stats = stats
      } catch (e) { this.error = true } finally { this.loading = false }
    },
    openDevices() { return this.$router.push('/mes/dv/machinery').catch(() => {}) }
  }
}
</script>

<style lang="scss" scoped>
.device-overview {
  min-width: 0;

  &__head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 10px;

    h2 {
      margin: 0;
      font-size: 17px;
      font-weight: 600;
      line-height: 1.4;
    }

    p {
      margin: 3px 0 0;
      color: var(--color-text-secondary);
      font-size: 12px;
    }
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 12px;
  }

  &__note {
    margin: 10px 0 0;
    color: var(--color-text-secondary);
    font-size: 12px;
  }
}

.device-card {
  min-width: 0;
  padding: 12px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  background: #fff;
  box-shadow: var(--shadow-card);
  color: var(--color-text);
  cursor: pointer;
  text-align: left;

  &:hover {
    border-color: var(--color-primary);
  }

  &__label,
  &__value {
    display: flex;
    align-items: center;
    gap: 7px;
  }

  &__label {
    color: var(--color-text-secondary);
    font-size: 13px;
  }

  &__icon {
    display: grid;
    flex: 0 0 26px;
    height: 26px;
    place-items: center;
    border-radius: var(--radius-control);
    color: #3d5a91;
    background: var(--color-primary-bg);
    font-size: 15px;
  }

  &__value {
    align-items: baseline;
    flex-wrap: wrap;
    margin-top: 7px;

    strong {
      overflow-wrap: anywhere;
      font-size: 26px;
      font-variant-numeric: tabular-nums;
      line-height: 1.12;
    }

    small {
      color: var(--color-text-secondary);
      font-size: 12px;
    }
  }

  &--working .device-card__icon {
    color: #246d5e;
    background: #edf5e6;
  }

  &--repair .device-card__icon {
    color: #986014;
    background: #fff8eb;
  }

  &--stop .device-card__icon {
    color: var(--color-text-secondary);
    background: var(--color-bg);
  }
}

@media (max-width: 700px) {
  .device-overview__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .device-card {
    padding: 9px 12px;
  }
}
</style>

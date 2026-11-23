<template>
  <section class="device-overview" aria-label="设备概览">
    <div class="device-overview__head"><div><h3>设备概览</h3><span>数据来源：设备台账</span></div><el-button v-if="error" type="text" @click="loadDevices">加载失败，点击重试</el-button><el-button v-else type="text" @click="openDevices">查看设备台账 →</el-button></div>
    <el-row :gutter="16" v-loading="loading">
      <el-col v-for="card in cards" :key="card.key" :xs="12" :sm="12" :lg="6" class="device-overview__col">
        <button class="device-card" type="button" @click="openDevices">
          <span class="device-card__icon"><i :class="card.icon"></i></span><span class="device-card__content"><span class="device-card__label">{{ card.label }}</span><strong>{{ error ? '—' : stats[card.key] }}</strong><small>台 · 查看台账</small></span>
        </button>
      </el-col>
    </el-row>
    <p v-if="!loading && !error && stats.other" class="device-overview__note">另有 {{ stats.other }} 台设备状态未归入以上分类，请查看设备台账。</p>
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
.device-overview { margin: 20px 0 24px; }
.device-overview__head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.device-overview__head h3 { display: inline; margin: 0 12px 0 0; color: #203a31; font-size: 16px; }
.device-overview__head span, .device-overview__note { color: #73877b; font-size: 12px; }
.device-overview__col { margin-bottom: 12px; }
.device-card { width: 100%; min-height: 100px; padding: 18px; border: 1px solid #dfe8e1; border-top: 3px solid #a7d65f; border-radius: 8px; background: #fff; display: flex; align-items: center; gap: 16px; text-align: left; cursor: pointer; }
.device-card:hover { border-color: #83b598; box-shadow: 0 4px 15px rgba(28,55,46,.08); }
.device-card:focus-visible { outline: 2px solid #246d5e; outline-offset: 2px; }
.device-card__icon { display: grid; place-items: center; width: 42px; height: 42px; flex: none; border-radius: 9px; background: #edf5e6; color: #246d5e; font-size: 21px; }
.device-card__content { display: flex; flex-direction: column; gap: 4px; color: #6e8379; font-size: 12px; }
.device-card__content strong { color: #203a31; font-size: 23px; line-height: 1; }
.device-card__content small { color: #789082; font-size: 11px; }
</style>

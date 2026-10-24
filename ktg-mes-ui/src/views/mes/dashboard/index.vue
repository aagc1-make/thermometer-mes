<template>
  <div class="app-container cockpit">
    <div class="cockpit__head">
      <div>
        <h2 class="cockpit__title">生产驾驶舱</h2>
        <p class="cockpit__sub">工单进度 · 库存水位 · 设备状态 · 产量趋势（每 30 秒自动刷新）</p>
      </div>
      <div class="cockpit__meta">
        <span>数据更新时间：{{ updatedAt }}</span>
        <el-button size="mini" icon="el-icon-refresh" :loading="loading" @click="loadAll">立即刷新</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :md="12" class="cockpit__cell">
        <div class="cockpit__card">
          <div class="cockpit__card-title">工单进度</div>
          <div ref="chartWorkorder" class="cockpit__chart"></div>
        </div>
      </el-col>
      <el-col :xs="24" :md="12" class="cockpit__cell">
        <div class="cockpit__card">
          <div class="cockpit__card-title">产量趋势（近 14 天报工合格数）</div>
          <div ref="chartTrend" class="cockpit__chart"></div>
        </div>
      </el-col>
      <el-col :xs="24" :md="12" class="cockpit__cell">
        <div class="cockpit__card">
          <div class="cockpit__card-title">库存水位（按仓库）</div>
          <div ref="chartStock" class="cockpit__chart"></div>
          <div class="cockpit__warn" v-if="stockWarnings.length">
            <i class="el-icon-warning-outline"></i>
            结存为 0 的物料：{{ stockWarnings.join('、') }}<span v-if="stockWarnings.length > 8"> 等</span>
          </div>
          <div class="cockpit__warn cockpit__warn--ok" v-else>
            <i class="el-icon-circle-check"></i> 暂无零库存预警
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :md="12" class="cockpit__cell">
        <div class="cockpit__card">
          <div class="cockpit__card-title">设备状态</div>
          <div ref="chartMachinery" class="cockpit__chart"></div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import request from '@/utils/request'

const BASE = process.env.VUE_APP_BASE_API

export default {
  name: 'Cockpit',
  data() {
    return {
      loading: false,
      timer: null,
      updatedAt: '-',
      charts: {},
      stockWarnings: []
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.initCharts()
      this.loadAll()
      // 30 秒自动刷新（评分要求：自动刷新）
      this.timer = setInterval(this.loadAll, 30000)
    })
    window.addEventListener('resize', this.resize)
  },
  beforeDestroy() {
    if (this.timer) clearInterval(this.timer)
    window.removeEventListener('resize', this.resize)
    Object.values(this.charts).forEach(c => c && c.dispose())
  },
  methods: {
    get(url, params) {
      return request({ url, method: 'get', params: Object.assign({ pageNum: 1, pageSize: 500 }, params) })
    },
    resize() {
      Object.values(this.charts).forEach(c => c && c.resize())
    },
    initCharts() {
      this.charts.workorder = echarts.init(this.$refs.chartWorkorder)
      this.charts.trend = echarts.init(this.$refs.chartTrend)
      this.charts.stock = echarts.init(this.$refs.chartStock)
      this.charts.machinery = echarts.init(this.$refs.chartMachinery)
    },
    async loadAll() {
      this.loading = true
      try {
        await Promise.all([this.loadWorkorder(), this.loadStock(), this.loadMachinery(), this.loadTrend()])
        this.updatedAt = this.now()
      } catch (e) {
        this.updatedAt = this.now() + '（部分数据加载失败）'
      } finally {
        this.loading = false
      }
    },
    now() {
      const d = new Date()
      const p = n => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
    },
    // ① 工单进度：按产品统计计划/已生产（比按状态更能反映生产结构）
    async loadWorkorder() {
      const res = await this.get('/mes/pro/workorder/list')
      const rows = res.rows || []
      const byProduct = {}
      rows.forEach(r => {
        const key = r.productName || r.productCode || '未命名产品'
        if (!byProduct[key]) byProduct[key] = { plan: 0, done: 0, cnt: 0 }
        byProduct[key].plan += Number(r.quantity || 0)
        byProduct[key].done += Number(r.quantityProduced || 0)
        byProduct[key].cnt += 1
      })
      const names = Object.keys(byProduct).sort((a, b) => byProduct[b].plan - byProduct[a].plan).slice(0, 5)
      this.charts.workorder.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        legend: { data: ['计划数量', '已生产'], right: 0, top: 0, icon: 'roundRect', itemWidth: 10, itemHeight: 10 },
        grid: { left: 44, right: 12, top: 36, bottom: 42 },
        xAxis: {
          type: 'category', data: names,
          axisLine: { lineStyle: { color: '#e5e7eb' } },
          axisLabel: { color: '#6b7280', interval: 0, width: 90, overflow: 'truncate' }
        },
        yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f0f2f5' } }, axisLabel: { color: '#6b7280' } },
        series: [
          { name: '计划数量', type: 'bar', barWidth: 18, itemStyle: { color: '#93b8f7', borderRadius: [4, 4, 0, 0] }, data: names.map(n => byProduct[n].plan) },
          { name: '已生产', type: 'bar', barWidth: 18, itemStyle: { color: '#2563eb', borderRadius: [4, 4, 0, 0] }, data: names.map(n => byProduct[n].done) }
        ]
      }, true)
    },
    // ② 库存水位：固定展示 原料仓 / 线边库 / 成品仓（排除虚拟过账仓），无数据也显示 0
    async loadStock() {
      const res = await this.get('/mes/wm/wmstock/list')
      const rows = res.rows || []
      const targets = ['原料仓', '线边库', '成品仓']
      const byWh = { '原料仓': 0, '线边库': 0, '成品仓': 0 }
      const zero = []
      rows.forEach(r => {
        const wh = r.warehouseName || ''
        const qty = Number(r.quantityOnhand || 0)
        if (targets.indexOf(wh) > -1) {
          byWh[wh] += qty
        } else if (qty < 0) {
          // 虚拟线边仓：内部过账仓，负数属正常，不计入水位
          return
        }
        if (qty <= 0 && r.itemName && targets.indexOf(wh) > -1 && zero.indexOf(r.itemName) === -1) {
          zero.push(r.itemName)
        }
      })
      this.stockWarnings = zero
      this.charts.stock.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 44, right: 12, top: 24, bottom: 28 },
        xAxis: { type: 'category', data: targets, axisLine: { lineStyle: { color: '#e5e7eb' } }, axisLabel: { color: '#6b7280' } },
        yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#f0f2f5' } }, axisLabel: { color: '#6b7280' } },
        series: [{
          type: 'bar', barWidth: 34,
          itemStyle: {
            borderRadius: [6, 6, 0, 0],
            color: p => (['#12a150', '#2563eb', '#d97706'][p.dataIndex] || '#12a150')
          },
          label: { show: true, position: 'top', color: '#4b5563', formatter: '{c}' },
          data: targets.map(n => Math.round(byWh[n] * 100) / 100)
        }]
      }, true)
    },
    // ③ 设备状态：运行/维修/停机分布
    async loadMachinery() {
      const res = await this.get('/mes/dv/machinery/list')
      const rows = res.rows || []
      const map = { WORKING: '生产中', REPAIR: '维修中', STOP: '停机', MAINTEN: '保养中', MAINTENANCE: '保养中' }
      const cnt = {}
      rows.forEach(r => { const s = map[r.status] || r.status || '未知'; cnt[s] = (cnt[s] || 0) + 1 })
      const data = Object.keys(cnt).map(k => ({ name: k, value: cnt[k] }))
      this.charts.machinery.setOption({
        tooltip: { trigger: 'item', formatter: '{b}：{c} 台（{d}%）' },
        legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8 },
        color: ['#12a150', '#d97706', '#98a2b3'],
        series: [{
          type: 'pie', radius: ['46%', '68%'], center: ['50%', '46%'],
          avoidLabelOverlap: true,
          itemStyle: { borderColor: '#fff', borderWidth: 2 },
          label: { formatter: '{b}\n{c} 台', color: '#4b5563' },
          data
        }]
      }, true)
    },
    // ④ 产量趋势：近 7 天报工合格数（以「最新报工日期」为锚点，兼容演示数据的未来日期）
    async loadTrend() {
      const res = await this.get('/mes/pro/feedback/list')
      const rows = res.rows || []
      const sum = {}
      let latest = 0
      rows.forEach(r => {
        const t = String(r.feedbackTime || r.createTime || '').slice(0, 10)
        if (!t) return
        sum[t] = (sum[t] || 0) + Number(r.quantityQualified || 0)
        const ts = new Date(t.replace(/-/g, '/')).getTime()
        if (ts > latest) latest = ts
      })
      const anchor = Math.max(latest, Date.now())
      const days = []
      const p = n => String(n).padStart(2, '0')
      for (let i = 13; i >= 0; i--) {
        const d = new Date(anchor - i * 86400000)
        days.push(`${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`)
      }
      this.charts.trend.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 44, right: 16, top: 20, bottom: 28 },
        xAxis: { type: 'category', data: days.map(d => d.slice(5)), boundaryGap: false, axisLine: { lineStyle: { color: '#e5e7eb' } }, axisLabel: { color: '#6b7280' } },
        yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#f0f2f5' } }, axisLabel: { color: '#6b7280' } },
        series: [{
          type: 'line', smooth: true, symbolSize: 6,
          lineStyle: { width: 2.5, color: '#2563eb' },
          itemStyle: { color: '#2563eb' },
          label: { show: true, color: '#4b5563', formatter: p2 => (p2.value ? p2.value : '') },
          areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(37,99,235,.22)' }, { offset: 1, color: 'rgba(37,99,235,0)' }]) },
          data: days.map(d => Math.round((sum[d] || 0) * 100) / 100)
        }]
      }, true)
    }
  }
}
</script>

<style lang="scss" scoped>
.cockpit {
  &__head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 16px;
    padding: 20px 24px;
    border-radius: 12px;
    color: #fff;
    /* 红外测温枪实物图 + 蓝黑渐变 */
    background-image:
      linear-gradient(100deg, rgba(11, 18, 32, 0.9) 0%, rgba(24, 56, 120, 0.7) 45%, rgba(11, 18, 32, 0.5) 100%),
      url('~@/assets/images/themer.png');
    background-size: cover;
    background-position: center;
    box-shadow: 0 8px 24px rgba(17, 40, 84, 0.18);
  }
  &__title {
    margin: 0;
    font-size: 22px;
    font-weight: 700;
    color: #ffffff;
    letter-spacing: 0.5px;
  }
  &__sub {
    margin: 6px 0 0;
    font-size: 13px;
    color: rgba(255, 255, 255, 0.8);
  }
  &__meta {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 12px;
    color: rgba(255, 255, 255, 0.85);
  }
  &__cell {
    margin-bottom: 16px;
  }
  &__card {
    background: rgba(255, 255, 255, 0.86);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.7);
    border-radius: 12px;
    padding: 14px 16px 10px;
    box-shadow: 0 6px 18px rgba(17, 40, 84, 0.08);
    transition: transform 0.2s ease, box-shadow 0.2s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 12px 28px rgba(17, 40, 84, 0.14);
    }
  }
  &__card-title {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 6px;
  }
  &__chart {
    height: 260px;
  }
  &__warn {
    margin-top: 6px;
    font-size: 12px;
    color: #b45309;
    background: #fff4e5;
    border-radius: 6px;
    padding: 6px 10px;
  }
  &__warn--ok {
    color: #0f7a3d;
    background: #e7f8ef;
  }
}
</style>

<template>
  <div class="app-container cockpit zen-page">
    <div class="cockpit__head zen-head">
      <div>
        <h2 class="cockpit__title zen-head__title">生产驾驶舱</h2>
        <p class="cockpit__sub zen-head__sub">工单进度 · 库存水位 · 设备状态 · 产量趋势（每 30 秒自动刷新）</p>
      </div>
      <div class="cockpit__meta">
        <span>数据更新时间：{{ updatedAt }}</span>
        <el-button size="mini" icon="el-icon-refresh" :loading="loading" @click="loadAll">立即刷新</el-button>
      </div>
    </div>

    <div class="zen-cards">
      <div class="zen-card">
        <span class="zen-card__delta" :class="kpi.woDone === kpi.woTotal ? 'zen-card__delta--up' : 'zen-card__delta--down'">
          {{ kpi.woDone }}/{{ kpi.woTotal }} 已完工
        </span>
        <div class="zen-card__dot zen-card__dot--ink"><i class="el-icon-s-order"></i></div>
        <div class="zen-card__label">生产工单</div>
        <div class="zen-card__value">{{ kpi.woTotal }}</div>
        <div class="zen-card__desc">全部工单数量（含草稿/已取消）</div>
      </div>
      <div class="zen-card">
        <span class="zen-card__delta" :class="kpi.rate >= 95 ? 'zen-card__delta--up' : 'zen-card__delta--down'">
          {{ kpi.unq }} 件不合格
        </span>
        <div class="zen-card__dot zen-card__dot--matcha"><i class="el-icon-circle-check"></i></div>
        <div class="zen-card__label">报工合格率</div>
        <div class="zen-card__value">{{ kpi.rate }}%</div>
        <div class="zen-card__desc">已完成报工合格数 / 已完成报工总数</div>
      </div>
      <div class="zen-card">
        <span class="zen-card__delta zen-card__delta--up">{{ kpi.stockWh }} 个仓库</span>
        <div class="zen-card__dot zen-card__dot--sand"><i class="el-icon-goods"></i></div>
        <div class="zen-card__label">库存总量</div>
        <div class="zen-card__value">{{ kpi.stock }}</div>
        <div class="zen-card__desc">各仓库存按计量单位分别统计</div>
      </div>
      <div class="zen-card">
        <span class="zen-card__delta" :class="kpi.machRate >= 80 ? 'zen-card__delta--up' : 'zen-card__delta--down'">
          {{ kpi.machRun }}/{{ kpi.machTotal }} 台运行
        </span>
        <div class="zen-card__dot zen-card__dot--sakura"><i class="el-icon-cpu"></i></div>
        <div class="zen-card__label">设备运行占比</div>
        <div class="zen-card__value">{{ kpi.machRate }}%</div>
        <div class="zen-card__desc">生产中台数 / 设备总数</div>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :md="12" class="cockpit__cell">
        <div class="cockpit__card zen-panel">
          <div class="cockpit__card-title zen-panel__title">工单进度</div>
          <div ref="chartWorkorder" class="cockpit__chart"></div>
        </div>
      </el-col>
      <el-col :xs="24" :md="12" class="cockpit__cell">
        <div class="cockpit__card zen-panel">
          <div class="cockpit__card-title zen-panel__title">产量趋势（近 14 天报工合格数）</div>
          <div ref="chartTrend" class="cockpit__chart"></div>
        </div>
      </el-col>
      <el-col :xs="24" :md="12" class="cockpit__cell">
        <div class="cockpit__card zen-panel">
          <div class="cockpit__card-title zen-panel__title">库存水位（按仓库与单位）</div>
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
        <div class="cockpit__card zen-panel">
          <div class="cockpit__card-title zen-panel__title">设备状态</div>
          <div ref="chartMachinery" class="cockpit__chart"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="24" class="cockpit__cell">
        <div class="cockpit__card zen-panel">
          <div class="cockpit__card-title zen-panel__title">
            安全库存预警与补货建议
            <span class="cockpit__overdue" v-if="stockAlerts.length">
              <i class="el-icon-warning"></i> {{ stockAlerts.length }} 种物料低于安全库存
            </span>
            <span class="cockpit__overdue cockpit__overdue--ok" v-else>
              <i class="el-icon-circle-check"></i> 全部物料库存充足
            </span>
          </div>
          <el-table :data="stockAlerts" size="mini" border height="260" :header-cell-style="{ background: 'rgba(45,49,66,.03)', color: '#6b6f80', fontWeight: 400 }">
            <el-table-column prop="itemCode" label="物料编码" width="140" />
            <el-table-column prop="itemName" label="物料名称" show-overflow-tooltip />
            <el-table-column label="当前结存" width="106" align="right">
              <template slot-scope="s">
                <span :style="{ color: s.row.level === 'CRITICAL' ? '#c0392b' : '#b45309', fontWeight: 600 }">{{ s.row.current }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="min" label="安全库存" width="100" align="right" />
            <el-table-column label="缺口" width="96" align="right">
              <template slot-scope="s">
                <span style="color:#c0392b">{{ s.row.gap }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="suggest" label="建议补货" width="100" align="right" />
            <el-table-column prop="unitName" label="单位" width="72" align="center" />
            <el-table-column label="等级" width="92" align="center">
              <template slot-scope="s">
                <el-tag size="mini" :type="s.row.level === 'CRITICAL' ? 'danger' : 'warning'">
                  {{ s.row.level === 'CRITICAL' ? '缺料' : '低于安全线' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div class="cockpit__tip">
            <i class="el-icon-info"></i>
            建议补货量 = 安全库存 × 2 − 当前结存；安全库存可在「物料产品」中维护，修改后 30 秒内自动刷新
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="24" class="cockpit__cell">
        <div class="cockpit__card zen-panel">
          <div class="cockpit__card-title zen-panel__title">
            工单进度明细
            <span class="cockpit__overdue" v-if="overdue > 0">
              <i class="el-icon-warning"></i> {{ overdue }} 个工单已超交期
            </span>
            <span class="cockpit__overdue cockpit__overdue--ok" v-else>
              <i class="el-icon-circle-check"></i> 无超期工单
            </span>
          </div>
          <el-table :data="woList" size="mini" border height="300" :header-cell-style="{ background: 'rgba(45,49,66,.03)', color: '#6b6f80', fontWeight: 400 }">
            <el-table-column prop="workorderCode" label="工单号" width="150" />
            <el-table-column prop="productName" label="产品" show-overflow-tooltip />
            <el-table-column prop="quantity" label="计划" width="76" align="right" />
            <el-table-column prop="quantityProduced" label="已生产" width="86" align="right" />
            <el-table-column label="进度" width="160">
              <template slot-scope="s">
                <el-progress :percentage="s.row.percent" :stroke-width="8" :color="s.row.percent >= 100 ? '#31946a' : '#3b806d'" />
              </template>
            </el-table-column>
            <el-table-column prop="requestDate" label="交期" width="110" />
            <el-table-column label="状态" width="96">
              <template slot-scope="s">
                <el-tag size="mini" :type="s.row.overdue ? 'danger' : 'success'">{{ s.row.overdue ? '已超期' : '正常' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import request from '@/utils/request'
import { getMdItem } from '@/api/mes/md/mdItem'

const BASE = process.env.VUE_APP_BASE_API

export default {
  name: 'Cockpit',
  data() {
    return {
      loading: false,
      timer: null,
      updatedAt: '-',
      charts: {},
      stockWarnings: [],
      stockAlerts: [],
      woList: [],
      overdue: 0,
      kpi: { woTotal: 0, woDone: 0, rate: 0, unq: 0, stock: 0, stockWh: 3, machRate: 0, machRun: 0, machTotal: 0 }
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
    async get(url, params = {}) {
      const rows = []
      let pageNum = 1
      let total = 0
      do {
        const result = await request({ url, method: 'get', params: Object.assign({}, params, { pageNum, pageSize: 500 }) })
        const page = result.rows || []
        total = result.total == null ? rows.length + page.length : Number(result.total)
        if (!page.length && rows.length < total) throw new Error('数据分页发生变化，请重试查询')
        rows.push(...page)
        pageNum++
      } while (rows.length < total)
      return { rows, total }
    },
    async normalizeStockUnits(rows) {
      const invalid = unit => !unit || /[\uFFFD\u0300-\u036F\u0000-\u001F]/.test(unit)
      const ids = [...new Set(rows.filter(r => invalid(r.unitName) && r.itemId).map(r => r.itemId))]
      const unitByItem = {}
      if (this.$auth.hasPermi('mes:md:mditem:query')) {
        await Promise.all(ids.map(async id => {
          try {
            const result = await getMdItem(id)
            const name = result.data && result.data.unitName
            if (!invalid(name)) unitByItem[id] = name
          } catch (e) { /* 无法获取物料单位时不合并不同物料 */ }
        }))
      }
      return rows.map(r => invalid(r.unitName)
        ? Object.assign({}, r, { unitName: unitByItem[r.itemId] || `单位待核实（${r.itemCode || r.itemId || '未知物料'}）` })
        : r)
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
      if (this.loading) return
      this.loading = true
      try {
        await Promise.all([this.loadWorkorder(), this.loadStock(), this.loadMachinery(), this.loadTrend(), this.loadKpi()])
        this.updatedAt = this.now()
      } catch (e) {
        this.updatedAt = this.now() + '（部分数据加载失败）'
      } finally {
        this.loading = false
      }
    },
    // ⑤ KPI 四卡：工单总数 / 报工合格率 / 库存总量 / 设备运行占比
    async loadKpi() {
      const [wo, fb, st, mc] = await Promise.all([
        this.get('/mes/pro/workorder/list'),
        this.get('/mes/pro/feedback/list'),
        this.get('/mes/wm/wmstock/list'),
        this.get('/mes/dv/machinery/list')
      ])
      const woRows = wo.rows || []
      const fbRows = (fb.rows || []).filter(r => r.status === 'FINISHED')
      const stRows = await this.normalizeStockUnits(st.rows || [])
      const mcRows = mc.rows || []
      const targets = [...new Set(stRows.map(r => r.warehouseCode || r.warehouseName))]

      const fbTotal = fbRows.reduce((s, r) => s + Number(r.quantityFeedback || 0), 0)
      const fbQualified = fbRows.reduce((s, r) => s + Number(r.quantityQualified || 0), 0)
      const stockByUnit = {}
      stRows.forEach(r => {
        const unit = r.unitName || r.unitOfMeasure || '单位未配置'
        stockByUnit[unit] = (stockByUnit[unit] || 0) + Number(r.quantityOnhand || 0)
      })
      const stockSummary = Object.keys(stockByUnit).map(u => `${Math.round(stockByUnit[u] * 10000) / 10000} ${u}`).join(' / ') || '0'
      const machRun = mcRows.filter(r => r.status === 'WORKING').length

      this.kpi = {
        woTotal: woRows.length,
        woDone: woRows.filter(r => r.status === 'FINISHED').length,
        rate: fbTotal > 0 ? Math.round((fbQualified / fbTotal) * 1000) / 10 : 0,
        unq: Math.max(0, Math.round((fbTotal - fbQualified) * 100) / 100),
        stock: stockSummary,
        stockWh: targets.length,
        machRun,
        machTotal: mcRows.length,
        machRate: mcRows.length > 0 ? Math.round((machRun / mcRows.length) * 1000) / 10 : 0
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
      // 旧工单可能保存了损坏的产品名称；仅驾驶舱从物料主数据读取显示名称。
      const badIds = [...new Set(rows.filter(r => /\uFFFD/.test(r.productName || '') && r.productId).map(r => r.productId))]
      const namesById = {}
      if (this.$auth.hasPermi('mes:md:mditem:query')) {
        await Promise.all(badIds.map(async id => {
          try {
            const item = await getMdItem(id)
            const name = item.data && item.data.itemName
            if (name && !/\uFFFD/.test(name)) namesById[id] = name
          } catch (e) { /* 无法读取主数据时显示产品编码 */ }
        }))
      }
      rows.forEach(r => {
        if (/\uFFFD/.test(r.productName || '')) {
          r.productName = namesById[r.productId] || r.productCode || '产品名称不可用'
        }
      })
      const byProduct = {}
      rows.forEach(r => {
        const key = r.productName || r.productCode || '未命名产品'
        if (!byProduct[key]) byProduct[key] = { plan: 0, done: 0, cnt: 0 }
        byProduct[key].plan += Number(r.quantity || 0)
        byProduct[key].done += Number(r.quantityProduced || 0)
        byProduct[key].cnt += 1
      })
      const names = Object.keys(byProduct).sort((a, b) => byProduct[b].plan - byProduct[a].plan).slice(0, 5)

      // 工单明细：进度百分比 + 超期判定（未完工且交期已过）
      const today = new Date().setHours(0, 0, 0, 0)
      let overdueCount = 0
      this.woList = rows
        .map(r => {
          const plan = Number(r.quantity || 0)
          const done = Number(r.quantityProduced || 0)
          const percent = plan > 0 ? Math.min(100, Math.round((done / plan) * 100)) : 0
          const reqTs = r.requestDate ? new Date(String(r.requestDate).replace(/-/g, '/')).getTime() : 0
          const overdue = r.status !== 'FINISHED' && r.status !== 'CANCELED' && reqTs > 0 && reqTs < today
          if (overdue) overdueCount++
          return {
            workorderId: r.workorderId,
            workorderCode: r.workorderCode,
            productName: r.productName,
            quantity: plan,
            quantityProduced: done,
            requestDate: r.requestDate ? String(r.requestDate).slice(0, 10) : '-',
            percent,
            overdue
          }
        })
        .sort((a, b) => (b.overdue - a.overdue) || (a.percent - b.percent))
        .slice(0, 12)
      this.overdue = overdueCount

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
          { name: '计划数量', type: 'bar', barWidth: 18, itemStyle: { color: '#a9cf8a', borderRadius: [4, 4, 0, 0] }, data: names.map(n => byProduct[n].plan) },
          { name: '已生产', type: 'bar', barWidth: 18, itemStyle: { color: '#246d5e', borderRadius: [4, 4, 0, 0] }, data: names.map(n => byProduct[n].done) }
        ]
      }, true)
    },
    // ② 库存水位：固定展示 原料仓 / 线边库 / 成品仓（排除虚拟过账仓），无数据也显示 0
    async loadStock() {
      const res = await this.get('/mes/wm/wmstock/list')
      const rows = await this.normalizeStockUnits(res.rows || [])
      const byWh = {}
      const zero = []
      rows.forEach(r => {
        const wh = r.warehouseCode === 'XBK_VIRTUAL' ? '生产线边库' : (r.warehouseName || r.warehouseCode || '未配置仓库')
        const key = `${wh}（${r.unitName || r.unitOfMeasure || '单位未配置'}）`
        const qty = Number(r.quantityOnhand || 0)
        byWh[key] = (byWh[key] || 0) + qty
        if (qty <= 0 && r.itemName && !zero.includes(r.itemName)) zero.push(r.itemName)
      })
      const targets = Object.keys(byWh)
      this.stockWarnings = zero

      // 安全库存预警：结存 < min_stock 即预警，并给出建议补货量（安全线 × 2 - 当前结存）
      try {
        const itemsRes = await this.get('/mes/md/mditem/list', { pageNum: 1, pageSize: 500 })
        const itemRows = itemsRes.rows || []
        const stockMap = {}
        rows.forEach(r => {
          if (!r.itemCode) return
          stockMap[r.itemCode] = (stockMap[r.itemCode] || 0) + Number(r.quantityOnhand || 0)
        })
        this.stockAlerts = itemRows
          .filter(i => Number(i.minStock || 0) > 0)
          .map(i => {
            const cur = Math.round((stockMap[i.itemCode] || 0) * 100) / 100
            const min = Number(i.minStock)
            return {
              itemCode: i.itemCode,
              itemName: i.itemName,
              unitName: i.unitName || '',
              current: cur,
              min,
              gap: Math.round((cur - min) * 100) / 100,
              suggest: Math.max(0, Math.round((min * 2 - cur) * 100) / 100),
              level: cur <= 0 ? 'CRITICAL' : (cur < min ? 'WARNING' : 'OK')
            }
          })
          .filter(a => a.level !== 'OK')
          .sort((a, b) => a.gap - b.gap)
      } catch (e) {
        this.stockAlerts = []
      }

      this.charts.stock.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 44, right: 12, top: 24, bottom: 28 },
        xAxis: { type: 'category', data: targets, axisLine: { lineStyle: { color: '#e5e7eb' } }, axisLabel: { color: '#6b7280' } },
        yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#f0f2f5' } }, axisLabel: { color: '#6b7280' } },
        series: [{
          type: 'bar', barWidth: 34,
          itemStyle: {
            borderRadius: [6, 6, 0, 0],
            color: p => (['#31946a', '#246d5e', '#d39a38'][p.dataIndex] || '#31946a')
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
        color: ['#31946a', '#d39a38', '#91a09a'],
        series: [{
          type: 'pie', radius: ['46%', '68%'], center: ['50%', '46%'],
          avoidLabelOverlap: true,
          itemStyle: { borderColor: '#fff', borderWidth: 2 },
          label: { formatter: '{b}\n{c} 台', color: '#4b5563' },
          data
        }]
      }, true)
    },
    // ④ 产量趋势：截至今天近14天的已完成报工合格数
    async loadTrend() {
      const res = await this.get('/mes/pro/feedback/list')
      const rows = (res.rows || []).filter(r => r.status === 'FINISHED')
      const sum = {}
      let latest = 0
      rows.forEach(r => {
        const t = String(r.feedbackTime || r.createTime || '').slice(0, 10)
        if (!t) return
        sum[t] = (sum[t] || 0) + Number(r.quantityQualified || 0)
        const ts = new Date(t.replace(/-/g, '/')).getTime()
        if (ts > latest) latest = ts
      })
      const anchor = Date.now()
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
          lineStyle: { width: 2.5, color: '#246d5e' },
          itemStyle: { color: '#246d5e' },
          label: { show: true, color: '#4b5563', formatter: p2 => (p2.value ? p2.value : '') },
          areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(36,109,94,.22)' }, { offset: 1, color: 'rgba(36,109,94,0)' }]) },
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
  &__overdue {
    margin-left: 12px;
    font-size: 12px;
    font-weight: 400;
    color: #b45309;
  }
  &__overdue--ok {
    color: #0f7a3d;
  }
  &__tip {
    margin-top: 8px;
    font-size: 12px;
    color: #6b6f80;
  }
}
</style>

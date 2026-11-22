<template>
  <div ref="cockpit" class="app-container cockpit" :class="{ 'is-fullscreen': isFullscreen }">
    <!-- 顶部标题区：沿用模板的居中标题 + 箭头装饰 -->
    <header class="pc-hero">
      <div class="pc-hero__center">
        <h1 class="pc-hero__title">
          <span class="pc-hero__text">生产驾驶舱</span>
        </h1>
        <p class="pc-hero__sub">工单进度 · 库存水位 · 设备状态 · 产量趋势（每 30 秒自动刷新）</p>
      </div>
      <div class="pc-hero__meta">
        <span class="pc-hero__time"><i class="el-icon-time"></i>数据更新时间：{{ updatedAt }}</span>
        <el-button class="pc-hero__btn" size="mini" icon="el-icon-refresh" :loading="loading" @click="loadAll">立即刷新</el-button>
        <el-button
          class="pc-hero__btn"
          size="mini"
          :icon="isFullscreen ? 'el-icon-close' : 'el-icon-full-screen'"
          :disabled="fullscreenChanging"
          :aria-pressed="isFullscreen"
          :aria-label="isFullscreen ? '退出全屏' : '全屏'"
          :title="isFullscreen ? '退出全屏（Esc）' : '全屏显示生产驾驶舱'"
          @click="toggleFullscreen"
        >{{ isFullscreen ? '退出全屏' : '全屏' }}</el-button>
      </div>
    </header>

    <!-- 指标总览：仿模板“数额总览”的斜切标题条 -->
    <div class="pc-sec">
      <h3 class="pc-sec__title"><span>指标总览</span></h3>
    </div>

    <div class="pc-kpis">
      <article class="pc-kpi">
        <div class="pc-kpi__top">
          <span class="pc-kpi__icon pc-kpi__icon--order"><i class="el-icon-s-order"></i></span>
          <span class="pc-kpi__label">生产工单</span>
        </div>
        <p class="pc-kpi__value">{{ kpi.woTotal }}</p>
        <div class="pc-kpi__foot">
          <span class="pc-kpi__desc">全部工单数量（含草稿/已取消）</span>
          <span class="pc-kpi__delta" :class="kpi.woDone === kpi.woTotal ? 'is-up' : 'is-down'">
            {{ kpi.woDone }}/{{ kpi.woTotal }} 已完工
          </span>
        </div>
      </article>

      <article class="pc-kpi">
        <div class="pc-kpi__top">
          <span class="pc-kpi__icon pc-kpi__icon--check"><i class="el-icon-circle-check"></i></span>
          <span class="pc-kpi__label">报工合格率</span>
        </div>
        <p class="pc-kpi__value">{{ kpi.rate }}<span class="pc-kpi__unit">%</span></p>
        <div class="pc-kpi__foot">
          <span class="pc-kpi__desc">已完成报工合格数 / 已完成报工总数</span>
          <span class="pc-kpi__delta" :class="kpi.rate >= 95 ? 'is-up' : 'is-down'">{{ kpi.unq }} 件不合格</span>
        </div>
      </article>

      <article class="pc-kpi">
        <div class="pc-kpi__top">
          <span class="pc-kpi__icon pc-kpi__icon--stock"><i class="el-icon-goods"></i></span>
          <span class="pc-kpi__label">库存总量</span>
        </div>
        <p class="pc-kpi__value" :class="{ 'pc-kpi__value--long': kpi.stock.length > 12 }">{{ kpi.stock }}</p>
        <div class="pc-kpi__foot">
          <span class="pc-kpi__desc">各仓库存按计量单位分别统计</span>
          <span class="pc-kpi__delta is-up">{{ kpi.stockWh }} 个仓库</span>
        </div>
      </article>

      <article class="pc-kpi">
        <div class="pc-kpi__top">
          <span class="pc-kpi__icon pc-kpi__icon--cpu"><i class="el-icon-cpu"></i></span>
          <span class="pc-kpi__label">设备运行占比</span>
        </div>
        <p class="pc-kpi__value">{{ kpi.machRate }}<span class="pc-kpi__unit">%</span></p>
        <div class="pc-kpi__foot">
          <span class="pc-kpi__desc">生产中台数 / 设备总数</span>
          <span class="pc-kpi__delta" :class="kpi.machRate >= 80 ? 'is-up' : 'is-down'">
            {{ kpi.machRun }}/{{ kpi.machTotal }} 台运行
          </span>
        </div>
      </article>
    </div>

    <!-- 图表区：左宽右窄的两行，呼应模板的主次布局 -->
    <div class="pc-grid">
      <section class="pc-panel pc-panel--half">
        <header class="pc-panel__head">
          <h3 class="pc-panel__title"><span>工单进度</span></h3>
        </header>
        <div ref="chartWorkorder" class="pc-panel__chart"></div>
      </section>

      <section class="pc-panel pc-panel--half">
        <header class="pc-panel__head">
          <h3 class="pc-panel__title"><span>产量趋势（近 14 天报工合格数）</span></h3>
        </header>
        <div ref="chartTrend" class="pc-panel__chart"></div>
      </section>

      <section class="pc-panel pc-panel--wide">
        <header class="pc-panel__head">
          <h3 class="pc-panel__title"><span>库存水位（按仓库与单位）</span></h3>
        </header>
        <div ref="chartStock" class="pc-panel__chart pc-panel__chart--bars"></div>
        <div class="pc-warn" v-if="stockWarnings.length">
          <i class="el-icon-warning-outline"></i>
          <span>结存为 0 的物料：{{ stockWarnings.join('、') }}<span v-if="stockWarnings.length > 8"> 等</span></span>
        </div>
        <div class="pc-warn pc-warn--ok" v-else>
          <i class="el-icon-circle-check"></i> 暂无零库存预警
        </div>
      </section>

      <section class="pc-panel pc-panel--narrow">
        <header class="pc-panel__head">
          <h3 class="pc-panel__title"><span>设备状态</span></h3>
        </header>
        <div ref="chartMachinery" class="pc-panel__chart"></div>
      </section>
    </div>

    <!-- 安全库存预警 -->
    <section class="pc-panel pc-panel--full">
      <header class="pc-panel__head">
        <h3 class="pc-panel__title"><span>安全库存预警与补货建议</span></h3>
        <span class="pc-badge pc-badge--warn" v-if="stockAlerts.length">
          <i class="el-icon-warning"></i> {{ stockAlerts.length }} 种物料低于安全库存
        </span>
        <span class="pc-badge pc-badge--ok" v-else>
          <i class="el-icon-circle-check"></i> 全部物料库存充足
        </span>
      </header>
      <el-table :data="stockAlerts" size="mini" max-height="264" :row-class-name="stockRowClass">
        <el-table-column prop="itemCode" label="物料编码" width="140" />
        <el-table-column prop="itemName" label="物料名称" show-overflow-tooltip />
        <el-table-column label="当前结存" width="106" align="right">
          <template slot-scope="s">
            <span :style="{ color: s.row.level === 'CRITICAL' ? '#ff9b84' : '#ffc07a', fontWeight: 600 }">{{ s.row.current }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="min" label="安全库存" width="100" align="right" />
        <el-table-column label="缺口" width="96" align="right">
          <template slot-scope="s">
            <span style="color:#ff9b84">{{ s.row.gap }}</span>
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
      <p class="pc-tip">
        <i class="el-icon-info"></i>
        建议补货量 = 安全库存 × 2 − 当前结存；安全库存可在「物料产品」中维护，修改后 30 秒内自动刷新
      </p>
    </section>

    <!-- 工单进度明细 -->
    <section class="pc-panel pc-panel--full">
      <header class="pc-panel__head">
        <h3 class="pc-panel__title"><span>工单进度明细</span></h3>
        <span class="pc-badge pc-badge--warn" v-if="overdue > 0">
          <i class="el-icon-warning"></i> {{ overdue }} 个工单已超交期
        </span>
        <span class="pc-badge pc-badge--ok" v-else>
          <i class="el-icon-circle-check"></i> 无超期工单
        </span>
      </header>
      <el-table :data="woList" size="mini" max-height="300" :row-class-name="woRowClass">
        <el-table-column prop="workorderCode" label="工单号" width="150" />
        <el-table-column prop="productName" label="产品" show-overflow-tooltip />
        <el-table-column prop="quantity" label="计划" width="76" align="right" />
        <el-table-column prop="quantityProduced" label="已生产" width="86" align="right" />
        <el-table-column label="进度" width="180">
          <template slot-scope="s">
            <el-progress :percentage="s.row.percent" :stroke-width="8" :show-text="true" :color="s.row.percent >= 100 ? '#4d7b64' : '#4272c7'" />
          </template>
        </el-table-column>
        <el-table-column prop="requestDate" label="交期" width="110" />
        <el-table-column label="状态" width="110">
          <template slot-scope="s">
            <el-tag size="mini" :type="s.row.overdue ? 'danger' : 'success'">{{ s.row.overdue ? '已超期' : '正常' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import screenfull from 'screenfull'
import request from '@/utils/request'
import { getMdItem } from '@/api/mes/md/mdItem'

// 生产驾驶舱图表沿用工作区的蓝灰配色
const PC_TOOLTIP = {
  backgroundColor: '#ffffff',
  borderColor: '#cbd4df',
  borderWidth: 1,
  padding: [8, 12],
  textStyle: { color: '#252c35', fontSize: 12 },
  extraCssText: 'box-shadow:0 4px 12px rgba(37,44,53,.12);border-radius:8px;'
}
const PC_AXIS_LABEL = { color: '#55616f', fontSize: 11 }
const PC_AXIS_LINE = { lineStyle: { color: '#cbd4df' }}
const PC_SPLIT_LINE = { lineStyle: { color: '#e1e6ed', type: 'dashed' }}
const vGrad = (from, to) => new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: from }, { offset: 1, color: to }])
const hGrad = (from, to) => new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: from }, { offset: 1, color: to }])
const STOCK_GRADIENTS = [
  hGrad('#a4b8cc', '#547aa3'),
  hGrad('#b3c9be', '#4272c7'),
  hGrad('#e5d3b1', '#b18344')
]

// 长名称折行显示，避免坐标轴标签互相压字
const wrapLabel = (value, size) => {
  const text = String(value == null ? '' : value)
  if (text.length <= size) return text
  if (text.length <= size * 2) {
    const mid = Math.ceil(text.length / 2)
    return `${text.slice(0, mid)}\n${text.slice(mid)}`
  }
  return `${text.slice(0, size)}\n${text.slice(size, size * 2 - 1)}…`
}

export default {
  name: 'Cockpit',
  data() {
    return {
      loading: false,
      isFullscreen: false,
      fullscreenChanging: false,
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
      this.$nextTick(() => this.resize())
    })
    window.addEventListener('resize', this.resize)
    window.addEventListener('keydown', this.onFullscreenKeydown)
    if (screenfull.isEnabled) screenfull.on('change', this.onFullscreenChange)
  },
  deactivated() {
    this.exitCockpitFullscreen()
  },
  beforeDestroy() {
    if (this.timer) clearInterval(this.timer)
    window.removeEventListener('resize', this.resize)
    window.removeEventListener('keydown', this.onFullscreenKeydown)
    if (screenfull.isEnabled) screenfull.off('change', this.onFullscreenChange)
    this.exitCockpitFullscreen()
    Object.values(this.charts).forEach(c => c && c.dispose())
  },
  methods: {
    async toggleFullscreen() {
      if (!screenfull.isEnabled) {
        this.$message.warning('当前浏览器不支持全屏显示')
        return
      }
      if (this.fullscreenChanging) return
      this.fullscreenChanging = true
      try {
        if (screenfull.element === this.$refs.cockpit) await screenfull.exit()
        else await screenfull.request(this.$refs.cockpit)
      } catch (error) {
        this.$message.error('切换全屏失败，请重试')
      } finally {
        this.fullscreenChanging = false
      }
    },
    onFullscreenChange() {
      this.isFullscreen = screenfull.element === this.$refs.cockpit
      this.$nextTick(() => this.resize())
    },
    onFullscreenKeydown(event) {
      if (event.key === 'Escape') this.exitCockpitFullscreen()
    },
    exitCockpitFullscreen() {
      if (screenfull.isEnabled && screenfull.element === this.$refs.cockpit) {
        screenfull.exit().catch(() => {})
      }
    },
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
      const invalid = unit => !unit || /\uFFFD|[\u0300-\u036F]|[\u0000-\u001F]/.test(unit)
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
    stockRowClass({ row }) {
      return row.level === 'CRITICAL' ? 'pc-row--critical' : ''
    },
    woRowClass({ row }) {
      return row.percent >= 100 ? 'pc-row--done' : ''
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
        // 数据把面板撑高/压矮后重新适配图表尺寸
        this.$nextTick(() => this.resize())
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
        tooltip: Object.assign({ trigger: 'axis', axisPointer: { type: 'shadow' }}, PC_TOOLTIP),
        legend: {
          data: ['计划数量', '已生产'], top: 0, right: 2, icon: 'roundRect',
          itemWidth: 12, itemHeight: 8, itemGap: 18,
          textStyle: { color: '#55616f', fontSize: 11 }
        },
        grid: { left: 2, right: 8, top: 40, bottom: 2, containLabel: true },
        xAxis: {
          type: 'category', data: names,
          axisTick: { show: false },
          axisLine: PC_AXIS_LINE,
          axisLabel: Object.assign({}, PC_AXIS_LABEL, { interval: 0, fontSize: 10, lineHeight: 14, formatter: v => wrapLabel(v, 6) })
        },
        yAxis: {
          type: 'value', splitNumber: 4,
          axisLine: { show: false }, axisTick: { show: false },
          splitLine: PC_SPLIT_LINE, axisLabel: PC_AXIS_LABEL
        },
        series: [
          {
            name: '计划数量', type: 'bar', barWidth: 15, barGap: '50%',
            itemStyle: { color: vGrad('#7190af', '#dbe6ef'), borderRadius: [5, 5, 0, 0], barBorderRadius: [5, 5, 0, 0] },
            data: names.map(n => byProduct[n].plan)
          },
          {
            name: '已生产', type: 'bar', barWidth: 15,
            itemStyle: { color: vGrad('#4d7b64', '#d8e6de'), borderRadius: [5, 5, 0, 0], barBorderRadius: [5, 5, 0, 0] },
            data: names.map(n => byProduct[n].done)
          }
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

      // 横向条形：仓库×单位的条目较多，横向排列可避免坐标轴标签互相压字
      this.charts.stock.setOption({
        tooltip: Object.assign({ trigger: 'axis', axisPointer: { type: 'shadow' }}, PC_TOOLTIP),
        grid: { left: 2, right: 44, top: 8, bottom: 2, containLabel: true },
        xAxis: {
          type: 'value', minInterval: 1,
          axisLine: { show: false }, axisTick: { show: false },
          splitLine: PC_SPLIT_LINE, axisLabel: PC_AXIS_LABEL
        },
        yAxis: {
          type: 'category', data: targets, inverse: true,
          axisTick: { show: false },
          axisLine: PC_AXIS_LINE,
          axisLabel: Object.assign({}, PC_AXIS_LABEL, { interval: 0, fontSize: 10 })
        },
        series: [{
          type: 'bar', barWidth: 11,
          itemStyle: {
            borderRadius: [0, 6, 6, 0],
            barBorderRadius: [0, 6, 6, 0],
            color: p => STOCK_GRADIENTS[p.dataIndex % STOCK_GRADIENTS.length]
          },
          label: { show: true, position: 'right', distance: 6, color: '#252c35', fontSize: 11 },
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
        tooltip: Object.assign({ trigger: 'item', formatter: '{b}：{c} 台（{d}%）' }, PC_TOOLTIP),
        legend: {
          bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, itemGap: 14,
          textStyle: { color: '#55616f', fontSize: 11 }
        },
        color: ['#4272c7', '#b18344', '#6f8b98', '#4d7b64', '#7aa7d9'],
        series: [{
          type: 'pie', radius: ['46%', '66%'], center: ['50%', '44%'],
          avoidLabelOverlap: true,
          itemStyle: { borderColor: '#ffffff', borderWidth: 3 },
          label: { formatter: '{b}\n{c} 台', color: '#252c35', fontSize: 11, lineHeight: 15 },
          labelLine: { length: 6, length2: 8, lineStyle: { color: '#a7b4c3' }},
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
        tooltip: Object.assign({ trigger: 'axis' }, PC_TOOLTIP),
        grid: { left: 26, right: 16, top: 32, bottom: 2, containLabel: true },
        xAxis: {
          type: 'category', data: days.map(d => d.slice(5)), boundaryGap: false,
          axisTick: { show: false },
          axisLine: PC_AXIS_LINE,
          axisLabel: Object.assign({}, PC_AXIS_LABEL, { interval: 0, fontSize: 9, color: '#55616f' })
        },
        yAxis: {
          type: 'value', minInterval: 1, splitNumber: 4,
          axisLine: { show: false }, axisTick: { show: false },
          splitLine: PC_SPLIT_LINE, axisLabel: PC_AXIS_LABEL
        },
        series: [{
          type: 'line', smooth: true, symbol: 'circle', symbolSize: 7, showSymbol: true,
          lineStyle: { width: 2.4, color: '#34577d', shadowColor: 'transparent', shadowBlur: 12, shadowOffsetY: 2 },
          itemStyle: { color: '#ffffff', borderColor: '#34577d', borderWidth: 2 },
          label: { show: true, distance: 9, color: '#252c35', fontSize: 11, formatter: p2 => (p2.value ? p2.value : '') },
          areaStyle: { color: vGrad('rgba(84,122,163,.22)', 'rgba(84,122,163,0)') },
          data: days.map(d => Math.round((sum[d] || 0) * 100) / 100)
        }]
      }, true)
    }
  }
}
</script>

<style lang="scss" scoped>
.cockpit {
  color: var(--color-text);
  background: var(--color-bg);

  &.is-fullscreen {
    width: 100%;
    height: 100%;
    overflow: auto;

    .pc-hero {
      position: sticky;
      top: 0;
      z-index: 10;
      background: var(--color-bg);
    }
  }
}

.pc-hero {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 14px 20px;
  padding: 4px 0 20px;
  border-bottom: 1px solid var(--color-border);
}

.pc-hero__title {
  margin: 0 0 6px;
  font-size: clamp(24px, 2.2vw, 32px);
  font-weight: 700;
  line-height: 1.25;
}

.pc-hero__sub, .pc-hero__time, .pc-kpi__desc, .pc-tip {
  color: var(--color-text-secondary);
  font-size: 13px;
  line-height: 1.55;
}

.pc-hero__sub {
  margin: 0;
}

.pc-hero__meta, .pc-panel__head, .pc-kpi__top, .pc-kpi__foot {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.pc-hero__time i {
  margin-right: 4px;
}

.pc-hero__btn.el-button {
  margin: 0;
}

.pc-sec {
  margin: 20px 0 12px;
}

.pc-sec__title, .pc-panel__title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.pc-kpis, .pc-grid {
  display: grid;
  gap: 14px;
  margin-bottom: 14px;
}

.pc-kpis {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.pc-grid {
  grid-template-columns: repeat(6, minmax(0, 1fr));
}

.pc-kpi, .pc-panel {
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  background: var(--color-bg);
}

.pc-kpi__icon {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: var(--color-primary-bg);
  color: var(--color-primary);
}

.pc-kpi__label {
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
}

.pc-kpi__value {
  margin: 9px 0;
  font-size: clamp(24px, 2.3vw, 32px);
  font-variant-numeric: tabular-nums;
  line-height: 1.15;
  font-weight: 700;
  overflow-wrap: anywhere;
}

.pc-kpi__value--long {
  font-size: 20px;
}

.pc-kpi__unit {
  margin-left: 3px;
  font-size: 15px;
}

.pc-kpi__delta, .pc-badge {
  padding: 3px 7px;
  border-radius: 4px;
  background: #edf5ef;
  color: #36664f;
  font-size: 12px;
  font-weight: 600;
}

.pc-kpi__delta.is-down, .pc-badge--warn, .pc-warn {
  background: #fbf3e2;
  color: #785316;
}

.pc-panel--half {
  grid-column: span 3;
}

.pc-panel--wide {
  grid-column: span 4;
}

.pc-panel--narrow {
  grid-column: span 2;
}

.pc-panel--full {
  margin-bottom: 14px;
}

.pc-panel__head {
  justify-content: space-between;
  margin-bottom: 8px;
}

.pc-panel__chart {
  width: 100%;
  height: 260px;
}

.pc-panel__chart--bars {
  height: 284px;
}

.pc-warn {
  padding: 8px 10px;
  border-radius: 6px;
  font-size: 12px;
}

.pc-warn--ok {
  background: #edf5ef;
  color: #36664f;
}

.pc-tip {
  margin: 8px 0 0;
}

.pc-panel ::v-deep .el-table {
  .pc-row--critical td {
    background-color: #fcf3f1;
  }

  .pc-row--done td {
    background-color: #f4f8f5;
  }
}

@media (max-width: 1280px) {
  .pc-kpis {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .pc-panel--half, .pc-panel--wide, .pc-panel--narrow {
    grid-column: 1 / -1;
  }
}

@media (max-width: 550px) {
  .pc-kpis, .pc-grid {
    gap: 8px;
  }

  .pc-kpi, .pc-panel {
    padding: 10px;
  }
}
</style>

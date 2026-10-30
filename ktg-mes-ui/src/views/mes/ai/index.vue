<template>
  <div class="app-container ai">
    <div class="ai__head">
      <div>
        <h2 class="ai__title">MES 智能助手</h2>
        <p class="ai__sub">用自然语言查询库存、出入库、工单与检验数据 —— 结果全部来自真实业务库</p>
      </div>
      <el-button size="mini" icon="el-icon-delete" @click="reset">清空对话</el-button>
    </div>

    <div class="ai__body" ref="scroller">
      <div v-for="(m, i) in messages" :key="i" :class="['msg', 'msg--' + m.role]">
        <div class="msg__bubble">
          <!-- 文本 -->
          <div v-if="m.type === 'text'" class="msg__text">{{ m.text }}</div>

          <!-- 盘点结果 -->
          <div v-else-if="m.type === 'stock'" class="msg__card">
            <div class="msg__card-title">{{ m.title }}</div>
            <div class="kpis">
              <div class="kpi"><span>期初库存</span><b>{{ m.begin }}</b></div>
              <div class="kpi"><span>期间入库</span><b class="up">+{{ m.inQty }}</b></div>
              <div class="kpi"><span>期间出库</span><b class="down">-{{ m.outQty }}</b></div>
              <div class="kpi"><span>期末库存</span><b>{{ m.end }}</b></div>
            </div>
            <el-table :data="m.lines" size="mini" border max-height="240">
              <el-table-column prop="itemCode" label="物料" width="130" />
              <el-table-column prop="itemName" label="名称" show-overflow-tooltip />
              <el-table-column prop="batchCode" label="批次" width="120" />
              <el-table-column prop="warehouseName" label="仓库" width="110" />
              <el-table-column prop="inQty" label="入库" width="80" align="right" />
              <el-table-column prop="outQty" label="出库" width="80" align="right" />
              <el-table-column prop="endQty" label="结存" width="80" align="right" />
            </el-table>
            <div class="msg__meta">{{ m.source }}</div>
          </div>

          <!-- 通用列表 -->
          <div v-else-if="m.type === 'list'" class="msg__card">
            <div class="msg__card-title">{{ m.title }}</div>
            <el-table :data="m.rows" size="mini" border max-height="240">
              <el-table-column v-for="c in m.columns" :key="c.prop" :prop="c.prop" :label="c.label" :width="c.width" :align="c.align || 'left'" show-overflow-tooltip />
            </el-table>
            <div class="msg__meta">{{ m.source }}</div>
          </div>

          <!-- 追问 -->
          <div v-else-if="m.type === 'ask'" class="msg__card">
            <div class="msg__text">{{ m.text }}</div>
            <div class="chips">
              <span class="chip" v-for="(c, ci) in m.options" :key="ci" @click="send(c)">{{ c }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="msg msg--bot" v-if="loading">
        <div class="msg__bubble"><span class="dot"></span><span class="dot"></span><span class="dot"></span></div>
      </div>
    </div>

    <div class="ai__input">
      <el-input v-model="input" placeholder="例如：盘点一下 10 月份测温显示电控预装件的库存情况" @keyup.enter.native="send()" clearable />
      <el-button type="primary" icon="el-icon-s-promotion" @click="send()">发送</el-button>
    </div>

    <div class="ai__tips">
      <span class="ai__tips-label">试试：</span>
      <span class="chip" v-for="(q, i) in samples" :key="i" @click="send(q)">{{ q }}</span>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'

const CHAT_WELCOME = '你好，我是 MES 智能助手。可以问我：\n· 盘点 10 月份测温显示电控预装件的库存\n· 这个月采购入库了多少电子件\n· 有哪些工单进度落后\n· 本月的检验合格率是多少'

export default {
  name: 'MesAi',
  data() {
    return {
      input: '',
      loading: false,
      messages: [{ role: 'bot', type: 'text', text: CHAT_WELCOME }],
      samples: [
        '盘点 10 月份测温显示电控预装件的库存',
        '本月采购入库了多少电子件',
        '哪些工单进度落后',
        '现在哪些物料低于安全库存'
      ]
    }
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
    p2(n) { const p = x => String(x).padStart(2, '0'); return `${n.getFullYear()}-${p(n.getMonth() + 1)}-${p(n.getDate())}` },
    reset() {
      this.messages = [{ role: 'bot', type: 'text', text: CHAT_WELCOME }]
    },
    push(m) { this.messages.push(m); this.$nextTick(this.scrollBottom) },
    scrollBottom() { const el = this.$refs.scroller; if (el) el.scrollTop = el.scrollHeight },

    async send(text) {
      const q = (text || this.input || '').trim()
      if (!q || this.loading) return
      this.input = ''
      this.push({ role: 'user', type: 'text', text: q })
      this.loading = true
      try {
        await this.route(q)
      } catch (e) {
        this.push({ role: 'bot', type: 'text', text: '查询出错了：' + (e && e.message ? e.message : '未知错误') })
      } finally {
        this.loading = false
        this.$nextTick(this.scrollBottom)
      }
    },

    /* ---------- 意图解析（规则引擎；LLM 只做兜底，故意不参与算数） ---------- */
    parseTime(q) {
      const now = new Date()
      let m = q.match(/(\d{1,2})\s*月/)
      if (m) {
        const mon = Number(m[1]) - 1
        if (mon < 0 || mon > 11) throw new Error('月份应在 1 到 12 之间')
        const year = q.match(/(20\d{2})\s*年/)
        const y = year ? Number(year[1]) : (now.getMonth() < mon ? now.getFullYear() - 1 : now.getFullYear())
        return { from: this.p2(new Date(y, mon, 1)), to: this.p2(new Date(y, mon + 1, 0)), label: `${m[1]} 月` }
      }
      if (q.includes('本月') || q.includes('这个月') || q.includes('当月')) {
        return { from: this.p2(new Date(now.getFullYear(), now.getMonth(), 1)), to: this.p2(new Date(now.getFullYear(), now.getMonth() + 1, 0)), label: '本月' }
      }
      if (q.includes('上月') || q.includes('上个月')) {
        return { from: this.p2(new Date(now.getFullYear(), now.getMonth() - 1, 1)), to: this.p2(new Date(now.getFullYear(), now.getMonth(), 0)), label: '上月' }
      }
      m = q.match(/近\s*(\d+)\s*天/)
      if (m) {
        const d = Number(m[1])
        return { from: this.p2(new Date(Date.now() - (d - 1) * 86400000)), to: this.p2(now), label: `近 ${d} 天` }
      }
      return { from: this.p2(new Date(now.getFullYear(), now.getMonth(), 1)), to: this.p2(new Date(now.getFullYear(), now.getMonth() + 1, 0)), label: '本月' }
    },

    async matchItem(q) {
      const res = await this.get('/mes/md/mditem/list')
      const rows = res.rows || []
      const hit = rows.filter(i => (q.includes(i.itemName) || (i.itemCode && q.includes(i.itemCode))))
      if (hit.length) return { item: hit[0], items: hit, rows }
      // 模糊：材质/半成品关键词
      const fuzzy = rows.filter(i => {
        if (q.includes('电控') && i.itemName && i.itemName.indexOf('电控') > -1) return true
        if (q.includes('手柄') && i.itemName && i.itemName.indexOf('手柄') > -1) return true
        if (q.includes('电子件') && i.itemCode && i.itemCode.indexOf('TG-EL') === 0) return true
        if (q.includes('结构件') && i.itemCode && i.itemCode.indexOf('TG-ST') === 0) return true
        if (q.includes('包装') && i.itemCode && i.itemCode.indexOf('TG-PK') === 0) return true
        if (q.includes('整机') && i.itemCode === 'TG-FG-001') return true
        return false
      })
      return { item: fuzzy[0] || null, items: fuzzy, rows, fuzzy }
    },

    /** 规则引擎：命中直接走确定性查询（0 成本，覆盖常见问法） */
    matchRule(q) {
      if (/低于|安全库存|预警|缺料/.test(q)) return 'queryAlert'
      if (/库存|盘点|结存|还有多少/.test(q)) return 'queryStock'
      if (/入库|采购|到货|收货/.test(q)) return 'queryFlowIn'
      if (/出库|领料|发货|销售/.test(q)) return 'queryFlowOut'
      if (/工单|进度|排产/.test(q)) return 'queryWorkorder'
      if (/低于|安全库存|预警|缺料/.test(q)) return 'queryAlert'
      if (/合格率|检验|质检/.test(q)) return 'queryInspection'
      return null
    },

    queryFlowIn(q) { return this.queryFlow(q, 'IN') },
    queryFlowOut(q) { return this.queryFlow(q, 'OUT') },

    /** 大模型意图识别（后端代理，双模型兜底；失败返回 null，由规则兜底） */
    async askLlm(q) {
      try {
        const res = await request({
          url: '/mes/ai/intent',
          method: 'post',
          data: { question: q },
          timeout: 20000
        })
        if (res && res.code === 200 && res.data) return res.data
        return null
      } catch (e) {
        return null
      }
    },

    metricMethod(metric, q) {
      switch (metric) {
        case 'stock': return () => this.queryStock(q)
        case 'inbound': return () => this.queryFlow(q, 'IN')
        case 'outbound': return () => this.queryFlow(q, 'OUT')
        case 'workorder': return () => this.queryWorkorder(q)
        case 'alert': return () => this.queryAlert()
        case 'inspection': return () => this.queryInspection(q)
        default: return null
      }
    },

    /** 主流程：规则 → 大模型 → 追问 */
    async route(q) {
      // ① 规则命中
      const ruleFn = this.matchRule(q)
      if (ruleFn) return this[ruleFn](q)

      // ② 规则未命中：请大模型解析槽位
      const slots = await this.askLlm(q)
      if (slots && slots.metric && slots.metric !== 'unknown') {
        const resolved = [q, slots.timeText, slots.category, slots.warehouse].filter(Boolean).join(' ')
        const fn = this.metricMethod(slots.metric, resolved)
        if (fn) {
          const ok = await fn()
          if (ok !== false) {
            if (slots.model) {
              this.push({ role: 'bot', type: 'text', text: `（意图由 ${slots.model} 解析，耗时 ${slots.costMs || 0} ms；数据仍来自数据库确定性查询）` })
            }
            return
          }
        }
      }

      // ③ 都不行：追问，绝不编造
      this.push({
        role: 'bot', type: 'ask',
        text: slots && slots.metric === 'unknown'
          ? '这个问题不在 MES 业务数据范围内，我无法给出可靠答案。你是想查下面哪一类？'
          : '这个问题我暂时没把握，你是想查下面哪一类？',
        options: ['盘点某物料某月的库存', '本月采购入库情况', '工单进度落后清单', '低于安全库存的物料']
      })
    },

    /* ---------- ① 盘点：期初 / 入库 / 出库 / 期末 + 批次明细 ---------- */
    async queryStock(q) {
      const t = this.parseTime(q)
      const { item, items, rows, fuzzy } = await this.matchItem(q)
      if (items && items.length > 1) {
        this.push({ role: 'bot', type: 'ask', text: '匹配到多个物料，请明确要盘点的物料（不同物料单位不能直接相加）：', options: items.map(i => `盘点 ${t.from.slice(0, 4)}年${t.from.slice(5, 7)}月 ${i.itemName} 的库存`) })
        return
      }
      if (!item) {
        this.push({
          role: 'bot', type: 'ask',
          text: `没有找到与「${q.replace(/盘点|库存|情况|一下|的|请|帮我|查/g, '')}」匹配的物料，你是指下面哪一种？`,
          options: (fuzzy && fuzzy.length ? fuzzy : rows).slice(0, 4).map(i => `盘点 ${t.label} ${i.itemName} 的库存`)
        })
        return
      }
      const [tx, stock] = await Promise.all([
        this.get('/mes/wm/transaction/list', { itemId: item.itemId, itemCode: item.itemCode }),
        this.get('/mes/wm/wmstock/list', { itemId: item.itemId, itemCode: item.itemCode })
      ])
      const warehouse = this.warehouseFilter(q, [...(tx.rows || []), ...(stock.rows || [])])
      const txRows = (tx.rows || []).filter(warehouse)
      const byBatch = {}
      const group = r => {
        const key = [r.itemCode, r.batchCode || '', r.warehouseCode || r.warehouseId || ''].join('|')
        if (!byBatch[key]) byBatch[key] = { itemCode: r.itemCode, itemName: r.itemName, batchCode: r.batchCode || '无批次', warehouseName: r.warehouseName, inQty: 0, outQty: 0, endQty: 0 }
        return byBatch[key]
      }
      ;(stock.rows || []).filter(warehouse).forEach(r => { group(r).endQty += Number(r.quantityOnhand || 0) })
      // 当前余额减去查询期末之后的已过账流水，保留初始化期初余额。
      txRows.forEach(r => {
        const day = String(r.transactionDate || '').slice(0, 10)
        const qty = Number(r.transactionQuantity || 0)
        const batch = group(r)
        if (day > t.to) batch.endQty -= qty
        if (day >= t.from && day <= t.to) {
          if (qty >= 0) batch.inQty += qty
          else batch.outQty -= qty
        }
      })
      const round = n => Math.round(n * 10000) / 10000
      const lines = Object.values(byBatch).map(r => Object.assign({}, r, { inQty: round(r.inQty), outQty: round(r.outQty), endQty: round(r.endQty) }))
      const inQty = lines.reduce((v, r) => v + r.inQty, 0)
      const outQty = lines.reduce((v, r) => v + r.outQty, 0)
      const endQty = lines.reduce((v, r) => v + r.endQty, 0)
      this.push({ role: 'bot', type: 'stock', title: `${t.label} · ${item.itemName}（${item.itemCode}）库存盘点`,
        begin: round(endQty - inQty + outQty), inQty: round(inQty), outQty: round(outQty), end: round(endQty), lines,
        source: `数据来源：库存现有量及完整库存流水，${t.from} ~ ${t.to}；按仓库与批次核对，单位：${item.unitName || item.unitOfMeasure || '同物料单位'}` })
    },

    warehouseFilter(q, rows) {
      const names = [...new Set(rows.map(r => r.warehouseName).filter(Boolean))]
      const name = names.sort((a, b) => b.length - a.length).find(n => q.includes(n))
      const code = rows.map(r => r.warehouseCode).filter(Boolean).find(c => q.includes(c))
      if (!name && !code && /[\u4e00-\u9fa5A-Za-z0-9_-]+(?:仓库|仓|库)/.test(q.replace(/库存|入库|出库|结存/g, ''))) {
        throw new Error('未找到指定仓库，请使用仓库的完整名称或编码')
      }
      return r => (!name || r.warehouseName === name) && (!code || r.warehouseCode === code)
    },

    /* ---------- ② 出入库汇总 ---------- */
    async queryFlow(q, dir) {
      const t = this.parseTime(q)
      const { item, items } = await this.matchItem(q)
      const selected = items || (item ? [item] : [])
      const params = { params: { beginTime: t.from + ' 00:00:00', endTime: t.to + ' 23:59:59' } }
      if (selected.length === 1) params.itemCode = selected[0].itemCode
      const doc = q.match(/(?:单号|单据|采购单|出库单|入库单)[：:\s]*([A-Za-z][A-Za-z0-9_-]*\d[A-Za-z0-9_-]*)/)
      if (doc) params.sourceDocCode = doc[1]
      const tx = await this.get('/mes/wm/transaction/list', params)
      const warehouse = this.warehouseFilter(q, tx.rows || [])
      const types = /采购|到货|收货/.test(q) ? ['ITEM_RECPT'] : /领料/.test(q) ? ['ITEM_ISSUE_OUT'] : /销售|发货/.test(q) ? ['PRODUCT_SALES'] : null
      const rows = (tx.rows || []).filter(warehouse).filter(r => {
        if (selected.length && !selected.some(i => i.itemCode === r.itemCode)) return false
        if (types && !types.includes(r.transactionType) && !types.includes(r.sourceDocType)) return false
        const qty = Number(r.transactionQuantity || 0)
        return dir === 'IN' ? qty > 0 : qty < 0
      })
      const byItem = {}
      rows.forEach(r => {
        const k = r.itemCode
        if (!byItem[k]) byItem[k] = { itemCode: k, itemName: r.itemName, qty: 0, cnt: 0 }
        byItem[k].qty += Math.abs(Number(r.transactionQuantity || 0))
        byItem[k].cnt += 1
      })
      const list = Object.keys(byItem).map(k => Object.assign({}, byItem[k], { qty: Math.round(byItem[k].qty * 100) / 100 })).sort((a, b) => b.qty - a.qty)
      this.push({
        role: 'bot', type: 'list',
        title: `${t.label}${selected.length ? ' · ' + selected.map(i => i.itemName).join('、') : ''}${dir === 'IN' ? '入库' : '出库'}汇总`,
        columns: [
          { prop: 'itemCode', label: '物料编码', width: 130 },
          { prop: 'itemName', label: '物料名称' },
          { prop: 'qty', label: dir === 'IN' ? '入库数' : '出库数', width: 100, align: 'right' },
          { prop: 'cnt', label: '单据行数', width: 90, align: 'right' }
        ],
        rows: list,
        source: `数据来源：wm_transaction ${t.from} ~ ${t.to}，共 ${rows.length} 条${dir === 'IN' ? '入库' : '出库'}流水`
      })
    },

    /* ---------- ③ 工单进度 ---------- */
    async queryWorkorder(q = '') {
      const doc = q.match(/(?:工单号?|单号)[：:\s]*([A-Za-z][A-Za-z0-9_-]*\d[A-Za-z0-9_-]*)/)
      const res = await this.get('/mes/pro/workorder/list', doc ? { workorderCode: doc[1] } : {})
      const range = /\d\s*月|本月|上月|这个月|上个月|近\s*\d+\s*天/.test(q) ? this.parseTime(q) : null
      const rows = (res.rows || []).filter(r => (!doc || r.workorderCode === doc[1]) && (!range || (String(r.createTime || '').slice(0, 10) >= range.from && String(r.createTime || '').slice(0, 10) <= range.to)))
      const today = new Date().setHours(0, 0, 0, 0)
      const list = rows.map(r => {
        const plan = Number(r.quantity || 0), done = Number(r.quantityProduced || 0)
        const percent = plan > 0 ? Math.min(100, Math.round(done / plan * 100)) : 0
        const req = r.requestDate ? new Date(String(r.requestDate).replace(/-/g, '/')).getTime() : 0
        return {
          workorderCode: r.workorderCode, productName: r.productName,
          plan, done, percent,
          lag: r.status !== 'FINISHED' && r.status !== 'CANCELED' && req > 0 && req < today ? '已超期' : '正常'
        }
      }).sort((a, b) => a.percent - b.percent).slice(0, 10)
      this.push({
        role: 'bot', type: 'list',
        title: doc ? `工单 ${doc[1]} 进度` : '工单进度（按完成比例升序，最多 10 张）',
        columns: [
          { prop: 'workorderCode', label: '工单号', width: 150 },
          { prop: 'productName', label: '产品' },
          { prop: 'plan', label: '计划', width: 80, align: 'right' },
          { prop: 'done', label: '已产', width: 80, align: 'right' },
          { prop: 'percent', label: '进度%', width: 80, align: 'right' },
          { prop: 'lag', label: '交期', width: 90 }
        ],
        rows: list,
        source: `数据来源：pro_workorder，共 ${rows.length} 张工单`
      })
    },

    /* ---------- ④ 安全库存 ---------- */
    async queryAlert() {
      const [items, stock] = await Promise.all([this.get('/mes/md/mditem/list'), this.get('/mes/wm/wmstock/list')])
      const stockMap = {}
      ;(stock.rows || []).forEach(r => { stockMap[r.itemCode] = (stockMap[r.itemCode] || 0) + Number(r.quantityOnhand || 0) })
      const rows = (items.rows || [])
        .filter(i => Number(i.minStock || 0) > 0)
        .map(i => {
          const cur = Math.round((stockMap[i.itemCode] || 0) * 100) / 100
          return { itemCode: i.itemCode, itemName: i.itemName, current: cur, min: Number(i.minStock), gap: Math.round((cur - Number(i.minStock)) * 100) / 100, suggest: Math.max(0, Math.round((Number(i.minStock) * 2 - cur) * 100) / 100) }
        })
        .filter(a => a.gap < 0)
        .sort((a, b) => a.gap - b.gap)
      this.push({
        role: 'bot', type: 'list',
        title: `低于安全库存的物料（${rows.length} 种）`,
        columns: [
          { prop: 'itemCode', label: '物料编码', width: 130 },
          { prop: 'itemName', label: '物料名称' },
          { prop: 'current', label: '结存', width: 80, align: 'right' },
          { prop: 'min', label: '安全库存', width: 90, align: 'right' },
          { prop: 'gap', label: '缺口', width: 80, align: 'right' },
          { prop: 'suggest', label: '建议补货', width: 90, align: 'right' }
        ],
        rows,
        source: '数据来源：md_item.min_stock + wm_material_stock 实时结存'
      })
    },

    /* ---------- ⑤ 检验合格率 ---------- */
    async queryInspection(q) {
      const t = this.parseTime(q)
      const res = await this.get('/mes/qc/ipqc/list')
      const rows = (res.rows || []).filter(r => {
        const d = String(r.inspectDate || '').slice(0, 10)
        return r.status === 'FINISHED' && d >= t.from && d <= t.to
      })
      const total = rows.reduce((s, r) => s + Number(r.quantityCheck || 0), 0)
      const bad = rows.reduce((s, r) => s + Number(r.quantityUnqualified || 0), 0)
      const rate = total > 0 ? Math.round((total - bad) / total * 1000) / 10 : 0
      this.push({
        role: 'bot', type: 'list',
        title: `${t.label}过程检验统计：检验 ${total} 件，不合格 ${bad} 件，合格率 ${total > 0 ? rate + '%' : '暂无可统计结果'}`,
        columns: [
          { prop: 'ipqcCode', label: '检验单', width: 140 },
          { prop: 'processName', label: '工序' },
          { prop: 'quantityCheck', label: '检验数', width: 90, align: 'right' },
          { prop: 'quantityUnqualified', label: '不合格', width: 90, align: 'right' },
          { prop: 'status', label: '状态', width: 90 }
        ],
        rows: rows.slice(0, 20),
        source: `数据来源：qc_ipqc ${t.from} ~ ${t.to}，共 ${rows.length} 张检验单`
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.ai {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 130px);

  &__head {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 12px;
  }
  &__title { margin: 0; font-size: 20px; font-weight: 600; color: #1f2937; }
  &__sub { margin: 4px 0 0; font-size: 13px; color: #6b7280; }

  &__body {
    flex: 1;
    overflow-y: auto;
    padding: 8px 4px 12px;
  }

  &__input {
    display: flex;
    gap: 10px;
    padding: 10px 0 6px;
  }

  &__tips {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
    padding-bottom: 6px;
  }
  &__tips-label { font-size: 12px; color: #9ca3af; }
}

.msg {
  display: flex;
  margin-bottom: 14px;

  &--user { justify-content: flex-end; }

  &__bubble {
    max-width: 82%;
    padding: 12px 14px;
    border-radius: 12px;
    background: #fff;
    box-shadow: 0 1px 3px rgba(17, 24, 39, 0.06);
    font-size: 13px;
    color: #1f2937;
    line-height: 1.7;
  }
  &--user &__bubble {
    background: #2563eb;
    color: #fff;
  }

  &__text { white-space: pre-wrap; }

  &__card { min-width: 520px; }
  &__card-title {
    font-weight: 600;
    margin-bottom: 10px;
    color: #111827;
  }
  &__meta {
    margin-top: 8px;
    font-size: 11px;
    color: #9ca3af;
  }
}

.kpis {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 12px;
}
.kpi {
  background: #f7f9fc;
  border-radius: 8px;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;

  span { font-size: 11px; color: #6b7280; }
  b { font-size: 18px; color: #111827; }
  .up { color: #0f7a3d; }
  .down { color: #b45309; }
}

.chips { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
.chip {
  padding: 5px 12px;
  border-radius: 999px;
  background: #eaf1fe;
  color: #2563eb;
  font-size: 12px;
  cursor: pointer;
  transition: all .15s ease;

  &:hover { background: #2563eb; color: #fff; }
}

.dot {
  display: inline-block;
  width: 6px; height: 6px;
  margin-right: 4px;
  border-radius: 50%;
  background: #9ca3af;
  animation: blink 1.2s infinite;
}
.dot:nth-child(2) { animation-delay: .2s; }
.dot:nth-child(3) { animation-delay: .4s; }
@keyframes blink { 0%, 100% { opacity: .25; } 50% { opacity: 1; } }
</style>

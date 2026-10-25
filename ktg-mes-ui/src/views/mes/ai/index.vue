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
    get(url, params) {
      return request({ url, method: 'get', params: Object.assign({ pageNum: 1, pageSize: 500 }, params) })
    },
    p2(n) { const p = x => String(x).padStart(2, '0'); return `${n.getFullYear()}-${p(n.getMonth() + 1)}-${p(n.getDate())}` },
    reset() {
      this.messages = [{ role: 'bot', type: 'text', text: CHAT_WELCOME }]
    },
    push(m) { this.messages.push(m); this.$nextTick(this.scrollBottom) },
    scrollBottom() { const el = this.$refs.scroller; if (el) el.scrollTop = el.scrollHeight },

    async send(text) {
      const q = (text || this.input || '').trim()
      if (!q) return
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
        const y = now.getMonth() < mon ? now.getFullYear() - 1 : now.getFullYear()
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
        return { from: this.p2(new Date(Date.now() - d * 86400000)), to: this.p2(now), label: `近 ${d} 天` }
      }
      return { from: this.p2(new Date(now.getFullYear(), now.getMonth(), 1)), to: this.p2(new Date(now.getFullYear(), now.getMonth() + 1, 0)), label: '本月' }
    },

    async matchItem(q) {
      const res = await this.get('/mes/md/mditem/list')
      const rows = res.rows || []
      const hit = rows.filter(i => (q.includes(i.itemName) || (i.itemCode && q.includes(i.itemCode))))
      if (hit.length) return { item: hit[0], rows }
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
      return { item: fuzzy[0] || null, rows, fuzzy }
    },

    /** 规则引擎：命中直接走确定性查询（0 成本，覆盖常见问法） */
    matchRule(q) {
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
        const fn = this.metricMethod(slots.metric, q)
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
      const { item, rows, fuzzy } = await this.matchItem(q)
      if (!item) {
        this.push({
          role: 'bot', type: 'ask',
          text: `没有找到与「${q.replace(/盘点|库存|情况|一下|的|请|帮我|查/g, '')}」匹配的物料，你是指下面哪一种？`,
          options: (fuzzy && fuzzy.length ? fuzzy : rows).slice(0, 4).map(i => `盘点 ${t.label} ${i.itemName} 的库存`)
        })
        return
      }
      // 期间流水
      const tx = await this.get('/mes/wm/transaction/list', { itemCode: item.itemCode, beginTime: t.from, endTime: t.to })
      const txRows = tx.rows || []
      let inQty = 0, outQty = 0
      const byBatch = {}
      txRows.forEach(r => {
        const qty = Number(r.transactionQuantity || 0)
        const key = r.batchCode || '无批次'
        if (!byBatch[key]) byBatch[key] = { itemCode: r.itemCode, itemName: r.itemName, batchCode: key, inQty: 0, outQty: 0 }
        if (Number(qty) >= 0) { byBatch[key].inQty += qty; inQty += qty } else { byBatch[key].outQty += Math.abs(qty); outQty += Math.abs(qty) }
      })
      const endQty = inQty - outQty
      const lines = Object.keys(byBatch).map(k => Object.assign({}, byBatch[k], { endQty: Math.round((byBatch[k].inQty - byBatch[k].outQty) * 100) / 100 }))
      this.push({
        role: 'bot', type: 'stock',
        title: `${t.label} · ${item.itemName}（${item.itemCode}）库存盘点`,
        begin: 0, inQty: Math.round(inQty * 100) / 100, outQty: Math.round(outQty * 100) / 100, end: Math.round(endQty * 100) / 100,
        lines,
        source: `数据来源：wm_transaction${t.from} ~ ${t.to}，共 ${txRows.length} 条流水（可在「库存流水」页手工核对）`
      })
    },

    /* ---------- ② 出入库汇总 ---------- */
    async queryFlow(q, dir) {
      const t = this.parseTime(q)
      const { item } = await this.matchItem(q)
      const params = { beginTime: t.from, endTime: t.to }
      if (item) params.itemCode = item.itemCode
      const tx = await this.get('/mes/wm/transaction/list', params)
      const rows = (tx.rows || []).filter(r => {
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
        title: `${t.label}${item ? ' · ' + item.itemName : ''}${dir === 'IN' ? '入库' : '出库'}汇总`,
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
    async queryWorkorder() {
      const res = await this.get('/mes/pro/workorder/list')
      const rows = res.rows || []
      const today = new Date().setHours(0, 0, 0, 0)
      const list = rows.map(r => {
        const plan = Number(r.quantity || 0), done = Number(r.quantityProduced || 0)
        const percent = plan > 0 ? Math.min(100, Math.round(done / plan * 100)) : 0
        const req = r.requestDate ? new Date(String(r.requestDate).replace(/-/g, '/')).getTime() : 0
        return {
          workorderCode: r.workorderCode, productName: r.productName,
          plan, done, percent,
          lag: r.status !== 'FINISHED' && req > 0 && req < today ? '已超期' : '正常'
        }
      }).sort((a, b) => a.percent - b.percent).slice(0, 10)
      this.push({
        role: 'bot', type: 'list',
        title: '工单进度（进度最落后的 10 张）',
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
        const d = String(r.createTime || '').slice(0, 10)
        return d >= t.from && d <= t.to
      })
      const total = rows.reduce((s, r) => s + Number(r.quantityCheck || 0), 0)
      const bad = rows.reduce((s, r) => s + Number(r.quantityUnqualified || 0), 0)
      const rate = total > 0 ? Math.round((total - bad) / total * 1000) / 10 : 0
      this.push({
        role: 'bot', type: 'list',
        title: `${t.label}过程检验统计：检验 ${total} 件，不合格 ${bad} 件，合格率 ${rate}%`,
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

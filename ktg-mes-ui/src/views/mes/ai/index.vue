<template>
  <div class="app-container ai">
    <div class="ai__head">
      <div>
        <el-button size="mini" icon="el-icon-reading" @click="$refs.knowledgeBase.open()">知识库<span v-if="knowledgeDocuments.length">（{{ knowledgeDocuments.length }}）</span></el-button>
        <h2 class="ai__title">MES 智能助手</h2>
        <p class="ai__sub">用自然语言查询库存、出入库、工单与检验数据 —— 结果全部来自真实业务库</p>
      </div>
      <div>
        <el-button type="primary" size="mini" icon="el-icon-video-play" @click="$refs.businessChain.open()">全流程贯通</el-button>
        <el-button size="mini" icon="el-icon-delete" @click="reset">清空对话</el-button>
      </div>
    </div>

    <business-chain ref="businessChain" />
    <knowledge-base ref="knowledgeBase" :user-id="knowledgeUserId" @change="knowledgeDocuments = $event" />
    <div class="ai__mode">
      <el-radio-group v-model="queryMode" size="mini" aria-label="助手工作模式"><el-radio-button label="business">业务助手</el-radio-button><el-radio-button label="knowledge">知识问答</el-radio-button></el-radio-group>
      <span>{{ queryMode === 'knowledge' ? '检索本浏览器已启用的资料，回答附原文引用。' : '查询实时业务数据，或按确认步骤处理业务单据。' }}</span>
      <el-button v-if="queryMode === 'knowledge'" type="text" size="mini" @click="$refs.knowledgeBase.open()">管理与导入资料</el-button>
    </div>
    <div class="ai__body" ref="scroller">
      <div v-for="(m, i) in messages" :key="i" :class="['msg', 'msg--' + m.role]">
        <div class="msg__bubble">
          <!-- 文本 -->
          <div v-if="m.type === 'text'" class="msg__text">{{ m.text }}</div>
          <knowledge-answer v-else-if="m.type === 'knowledge'" :message="m" @source="showKnowledgeSource" />

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
            <el-button v-if="m.workorderId && $auth.hasPermi('mes:pro:workorder:query')" type="text" icon="el-icon-view" @click="openWorkorder(m.workorderId)">打开工单详情</el-button>
          </div>

          <!-- 追问 -->
          <div v-else-if="m.type === 'ask'" class="msg__card">
            <div class="msg__text">{{ m.text }}</div>
            <div class="chips">
              <span class="chip" v-for="(c, ci) in m.options" :key="ci" @click="send(c)">{{ c }}</span>
            </div>
          </div>

          <div v-else-if="m.type === 'arrivalReceipt'" class="msg__card">
            <div class="msg__card-title">到货通知 {{ m.notice.noticeCode }} 的采购入库单</div>
            <p class="msg__text">{{ m.nextStep }}</p>
            <el-table v-if="m.receipts.length" :data="m.receipts" size="mini" border>
              <el-table-column prop="recptCode" label="入库单号" />
              <el-table-column prop="vendorName" label="供应商" />
              <el-table-column prop="status" label="状态" width="110" />
            </el-table>
            <template v-if="$auth.hasPermi('mes:wm:itemrecpt:query')"><el-button v-for="r in m.receipts" :key="r.recptId" type="text" @click="openArrivalReceipt(r)">查看 {{ r.recptCode }}</el-button></template>
            <div class="msg__meta">数据来源：采购入库单（按到货通知实时查询）</div>
          </div>
          <!-- 对话中的原单据：读取当前状态，不把指代词当作物料名 -->
          <div v-else-if="m.type === 'arrivalDetail'" class="msg__card">
            <div class="msg__card-title">到货通知 {{ m.notice.noticeCode }} · {{ m.statusLabel }}</div>
            <p class="msg__text">供应商：{{ m.notice.vendorName || '未填写' }}；到货日期：{{ m.notice.arrivalDate || '未填写' }}</p>
            <el-table :data="m.lines" size="mini" border max-height="240">
              <el-table-column prop="itemCode" label="物料编码" width="130" />
              <el-table-column prop="itemName" label="物料名称" />
              <el-table-column prop="quantityArrival" label="到货数量" width="100" align="right" />
              <el-table-column prop="unitName" label="单位" width="70" />
              <el-table-column label="来料检验" width="90"><template slot-scope="scope">{{ scope.row.iqcCheck === 'Y' ? '需要' : scope.row.iqcCheck === 'N' ? '免检' : '未设置' }}</template></el-table-column>
            </el-table>
            <p class="msg__text">{{ m.nextStep }}</p>
            <div v-for="iqc in m.inspections" :key="iqc.iqcId">
              <span>{{ iqc.iqcCode }} · {{ iqc.itemName || '' }} · {{ inspectionStatus(iqc.status) }}</span>
              <el-button v-if="$auth.hasPermi('mes:qc:iqc:query')" type="text" @click="openArrivalInspection(iqc)">打开来料检验</el-button>
            </div>
            <el-button type="text" @click="openArrivalNotice(m.notice)">查看到货通知</el-button>
            <el-button v-if="m.notice.status === 'PREPARE' && $auth.hasPermi('mes:wm:arrivalnotice:edit')" type="text" @click="openArrivalNotice(m.notice, true)">完善并提交到货通知</el-button>
            <div class="msg__meta">{{ m.source }}</div>
          </div>

          <!-- 到货通知草稿确认：识别到“新增到货通知”后先列清物料与数量，用户确认才落库 -->
          <div v-else-if="m.type === 'arrival'" class="msg__card">
            <div class="msg__card-title">确认新增到货通知（草稿）</div>
            <p class="msg__text">{{ m.summary }}</p>
            <el-table class="msg__table" :data="m.lines" size="mini" border>
              <el-table-column prop="itemCode" label="物料编码" width="120" />
              <el-table-column prop="itemName" label="物料名称" show-overflow-tooltip />
              <el-table-column prop="unitName" label="单位" width="70" />
              <el-table-column prop="quantityArrival" label="到货数量" width="100" align="right" />
            </el-table>
            <div class="msg__form">
              <div class="msg__form-item">
                <span class="msg__form-label">供应商</span>
                <el-select v-model="m.vendorId" size="mini" filterable clearable placeholder="请选择供应商" :disabled="m.busy || m.finished">
                  <el-option v-for="v in m.vendors" :key="v.vendorId" :label="v.vendorName" :value="v.vendorId" />
                </el-select>
              </div>
              <div class="msg__form-item">
                <span class="msg__form-label">到货日期</span>
                <el-date-picker v-model="m.arrivalDate" type="date" size="mini" value-format="yyyy-MM-dd" placeholder="请选择到货日期" :disabled="m.busy || m.finished" />
              </div>
              <div class="msg__form-item">
                <span class="msg__form-label">来料检验</span>
                <el-select v-model="m.iqcCheck" size="mini" :disabled="m.busy || m.finished">
                  <el-option label="需要（提交后生成来料检验草稿）" value="Y" />
                  <el-option label="免检" value="N" />
                </el-select>
              </div>
            </div>
            <p v-if="m.result" class="msg__text msg__ok">{{ m.result }}</p>
            <p v-if="m.error" class="msg__text msg__bad">{{ m.error }}</p>
            <el-button v-if="!m.finished" type="primary" size="mini" :loading="m.busy" @click="confirmArrival(m)">确认创建</el-button>
            <el-button v-if="!m.finished" size="mini" :disabled="m.busy" @click="cancelArrival(m)">取消</el-button>
            <el-button v-if="m.noticeId && m.noticeCode" type="text" @click="openArrivalNotice(m)">查看到货通知</el-button>
          </div>

          <div v-else-if="m.type === 'draft'" class="msg__card">
            <div class="msg__card-title">确认创建生产工单草稿</div>
            <p>产品：{{ m.item.itemName }}（{{ m.item.itemCode }}）</p>
            <p>生产数量：{{ m.quantity }} {{ m.item.unitName || m.item.unitOfMeasure || '单位未标注' }}</p>
            <p>工单类型：自产；来源：库存备货；状态：草稿</p>
            <div>需求日期：<el-date-picker v-model="m.requestDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择需求日期" :disabled="m.busy || m.finished" /></div>
            <p v-if="m.error" class="msg__text">{{ m.error }}</p>
            <el-button v-if="!m.finished" type="primary" size="mini" :loading="m.busy" @click="confirmDraft(m)">确认创建草稿</el-button>
            <el-button v-if="!m.finished" size="mini" :disabled="m.busy" @click="cancelDraft(m)">取消</el-button>
            <el-button v-if="m.workorderId && $auth.hasPermi('mes:pro:workorder:query')" type="text" @click="openWorkorder(m.workorderId)">打开工单详情</el-button>
          </div>
          <div v-else-if="m.type === 'posting'" class="msg__card">
            <div class="msg__card-title">确认{{ m.label }}</div>
            <p>单据编号：{{ m.code }}；当前状态：{{ m.status }}</p>
            <el-table :data="m.lines" size="mini" border max-height="220">
              <el-table-column prop="itemCode" label="物料编码" width="130" />
              <el-table-column prop="itemName" label="物料名称" />
              <el-table-column prop="quantity" label="单据数量" width="100" />
              <el-table-column prop="unit" label="单位" width="80" />
              <el-table-column prop="batchCode" label="行批次" width="130" />
            </el-table>
            <p>执行后将按单据明细变更库存并生成流水，质量、数量及库位仍由系统校验。</p>
            <p v-if="m.error" class="msg__text msg__bad">{{ m.error }}</p>
            <el-button v-if="!m.finished" type="primary" size="mini" :loading="m.busy" @click="confirmPosting(m)">确认执行</el-button>
            <el-button v-if="!m.finished" size="mini" :disabled="m.busy" @click="cancelPosting(m)">取消</el-button>
            <el-button v-if="m.finished" type="text" @click="$router.push(m.path)">打开对应单据</el-button>
          </div>
          <div v-if="m.analysisContext" class="msg__meta">
            <el-button type="text" size="mini" :loading="m.analysisBusy" @click="requestAnalysis(m)">请求大模型分析</el-button>
            <span>{{ m.type === 'knowledge' ? '需确认发送当前问题与引用的文档片段' : '需确认发送当前问题和部分回答' }}</span>
          </div>
        </div>
      </div>

      <div class="msg msg--bot" v-if="loading">
        <div class="msg__bubble"><span class="dot"></span><span class="dot"></span><span class="dot"></span></div>
      </div>
    </div>

    <div class="ai__input">
      <el-input v-model="input" :placeholder="queryMode === 'knowledge' ? '例如：红外传感器来料检验需要检查哪些项目？' : '例如：盘点一下 10 月份测温显示电控预装件的库存情况'" @keyup.enter.native="send()" clearable />
      <el-button type="primary" icon="el-icon-s-promotion" @click="send()">发送</el-button>
    </div>

    <div class="ai__tips">
      <span class="ai__tips-label">试试：</span>
      <span class="chip" v-for="(q, i) in displaySamples" :key="i" @click="send(q)">{{ q }}</span>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'
import { detectMesTopic, buildInspectionAnswer, buildUnqualifiedAnswer, knowledgeFallback } from '@/utils/mesAnswerRules'

const CHAT_WELCOME = '你好，我是 MES 智能助手。可以问我：\n· 盘点 10 月份测温显示电控预装件的库存\n· 这个月采购入库了多少电子件\n· 有哪些工单进度落后\n· 本月的检验合格率是多少\n也可以让我登记单据，例如“帮我添加 10 个上壳、10 个电池的到货通知”'
const CHAT_TTL = 10 * 60 * 1000
let chatExpiryTimer = null

export default {
  name: 'MesAi',
  components: { BusinessChain: () => import('./BusinessChain'), KnowledgeBase: () => import('./KnowledgeBase'), KnowledgeAnswer: () => import('./KnowledgeAnswer') },
  data() {
    return {
      input: '',
      queryMode: 'business',
      knowledgeDocuments: [],
      loading: false,
      llmUnavailable: false,
      waitingWeatherCity: false,
      lastStock: null,
      lastWorkorder: null,
      messages: [{ role: 'bot', type: 'text', text: CHAT_WELCOME }],
      samples: [
        '盘点 10 月份测温显示电控预装件的库存',
        '红外传感器来料检验要检查哪些项目',
        '不合格物料应该如何处理',
        '本月采购入库了多少电子件',
        '帮我添加 10 个上壳、10 个电池的到货通知',
        '哪些工单进度落后',
        '现在哪些物料低于安全库存'
      ]
    }
  },
  computed: {
    knowledgeUserId() { return this.$store && this.$store.state.user.id || '' },
    displaySamples() {
      return this.queryMode === 'knowledge' ? ['红外传感器来料检验需要检查什么？', '测温枪装配工艺有哪些注意事项？', '不合格物料应该如何处理？'] : this.samples
    }
  },
  created() {
    this.restoreChat()
  },
  watch: {
    messages: { deep: true, handler() { this.saveChat() } },
    lastStock: { deep: true, handler() { this.saveChat() } },
    lastWorkorder() { this.saveChat() },
    waitingWeatherCity() { this.saveChat() }
  },
  methods: {
    chatKey() {
      const id = this.$store && this.$store.state.user.id
      return id ? `mes-ai-chat:${id}` : null
    },
    scheduleChatExpiry(key, expiresAt) {
      clearTimeout(chatExpiryTimer)
      chatExpiryTimer = setTimeout(() => {
        try {
          const saved = JSON.parse(sessionStorage.getItem(key) || 'null')
          if (saved && saved.expiresAt <= Date.now()) sessionStorage.removeItem(key)
          if (this.chatKey() === key && saved && saved.expiresAt <= Date.now()) this.reset()
        } catch (e) { /* storage may be unavailable */ }
      }, Math.max(0, expiresAt - Date.now()))
    },
    saveChat() {
      if (this._restoringChat) return
      const key = this.chatKey()
      if (!key) return
      try {
        if (this.messages.length <= 1) { sessionStorage.removeItem(key); return }
        const expiresAt = Date.now() + CHAT_TTL
        sessionStorage.setItem(key, JSON.stringify({ expiresAt, messages: this.messages, lastStock: this.lastStock, lastWorkorder: this.lastWorkorder, waitingWeatherCity: this.waitingWeatherCity }))
        this.scheduleChatExpiry(key, expiresAt)
      } catch (e) { /* keep chat available in memory when storage is full or disabled */ }
    },
    restoreChat() {
      const key = this.chatKey()
      if (!key) return
      try {
        const saved = JSON.parse(sessionStorage.getItem(key) || 'null')
        if (!saved || !Number.isFinite(saved.expiresAt) || saved.expiresAt <= Date.now() || !Array.isArray(saved.messages)) { sessionStorage.removeItem(key); return }
        this._restoringChat = true
        this.messages = saved.messages.map(m => {
          if ((m.type === 'draft' || m.type === 'arrival' || m.type === 'posting') && m.busy) {
            return Object.assign({}, m, { busy: false, finished: true, error: '刷新前的提交结果尚未确认，请先到对应单据页面核对，避免重复创建。' })
          }
          return Object.assign({}, m, { analysisBusy: false })
        })
        this.lastStock = saved.lastStock || null
        this.lastWorkorder = saved.lastWorkorder || null
        this.waitingWeatherCity = !!saved.waitingWeatherCity
        this.scheduleChatExpiry(key, saved.expiresAt)
        this.$nextTick(() => { this._restoringChat = false; this.scrollBottom() })
      } catch (e) { this._restoringChat = false; try { sessionStorage.removeItem(key) } catch (ignored) {} }
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
    p2(n) { const p = x => String(x).padStart(2, '0'); return `${n.getFullYear()}-${p(n.getMonth() + 1)}-${p(n.getDate())}` },
    reset() {
      clearTimeout(chatExpiryTimer)
      try { const key = this.chatKey(); if (key) sessionStorage.removeItem(key) } catch (e) { /* storage may be unavailable */ }
      this.messages = [{ role: 'bot', type: 'text', text: CHAT_WELCOME }]
      this.lastStock = null
      this.lastWorkorder = null
      this.waitingWeatherCity = false
    },
    push(m) { this.messages.push(m); this.$nextTick(this.scrollBottom) },
    scrollBottom() { const el = this.$refs.scroller; if (el) el.scrollTop = el.scrollHeight },

    async send(text) {
      const q = (text || this.input || '').trim()
      if (!q || this.loading) return
      this.input = ''
      this.push({ role: 'user', type: 'text', text: q })
      const responseStart = this.messages.length
      this.loading = true
      try {
        await this.route(q)
      } catch (e) {
        const reason = e && e.message ? e.message : typeof e === 'string' ? e : ''
        const message = /timeout|超时/i.test(reason) ? '查询超时，请稍后重试。' : /network|连接/i.test(reason) ? '无法连接业务服务，请检查服务是否启动。' : `查询未完成：${reason || '服务暂时不可用'}。`
        this.push({ role: 'bot', type: 'text', text: message })
      } finally {
        const replies = this.messages.slice(responseStart).filter(m => m.role === 'bot')
        if (replies.length) {
          const facts = replies.map(m => {
            const keys = m.type === 'stock' ? ['itemCode', 'itemName', 'batchCode', 'warehouseName', 'inQty', 'outQty', 'endQty'] : (m.columns || []).map(c => c.prop)
            const rows = m.rows || m.lines || []
            return {
              type: m.type, title: m.title, text: m.text, source: m.source,
              begin: m.begin, inQty: m.inQty, outQty: m.outQty, end: m.end,
              rows: rows.slice(0, 20).map(row => keys.reduce((shown, key) => { shown[key] = row[key]; return shown }, {})),
              rowsTruncated: rows.length > 20, options: m.options, summary: m.summary,
              citations: m.type === 'knowledge' ? m.sources : undefined
            }
          })
          this.$set(replies[replies.length - 1], 'analysisContext', { question: q, facts })
        }
        this.loading = false
        this.$nextTick(this.scrollBottom)
      }
    },

    async requestAnalysis(m) {
      if (m.analysisBusy || !m.analysisContext) return
      try {
        await this.$confirm(m.type === 'knowledge' ? '将当前问题与已引用的文档片段发送给已配置的外部模型服务。未引用的正文不会发送，请确认内容适合发送。' : '将当前问题及部分回答、表格数据发送给已配置的外部模型服务进行分析。请确认内容适合发送。', '确认发送', { confirmButtonText: '确认发送', cancelButtonText: '取消', type: 'warning' })
      } catch (e) { return }
      const facts = JSON.stringify(m.analysisContext.facts)
      if (facts.length > 12000) {
        this.$modal.msgWarning('当前结果过长，请缩小查询范围后再请求分析。')
        return
      }
      this.$set(m, 'analysisBusy', true)
      try {
        const result = await request({ url: '/mes/ai/analysis', method: 'post', data: { question: m.analysisContext.question, facts }, timeout: 20000 })
        if (!result || !result.data || !result.data.text) throw new Error('模型未返回分析结果')
        this.push({ role: 'bot', type: 'text', text: `大模型分析（${result.data.model || '模型'}）：\n${result.data.text}\n\n分析可能存在误差，请以表格及原始业务数据为准。` })
      } catch (e) {
        this.push({ role: 'bot', type: 'text', text: '大模型分析暂不可用；原始回答和表格仍可查看。' })
      } finally { this.$set(m, 'analysisBusy', false) }
    },

    /* ---------- 意图解析（规则引擎；LLM 只做兜底，故意不参与算数） ---------- */
    async queryKnowledge(question) {
      const knowledgeBase = this.$refs.knowledgeBase
      if (!knowledgeBase) { this.push({ role: 'bot', type: 'text', text: '知识库正在加载，请稍后重试。' }); return }
      const result = await knowledgeBase.search(question)
      if (!result.documentCount) { this.push({ role: 'bot', type: 'text', text: '知识库还没有资料，请先点击“知识库”导入文档，再进行知识问答。' }); return }
      if (!result.activeCount) { this.push({ role: 'bot', type: 'text', text: '当前没有启用的文档版本，请在知识库中启用需要检索的版本。' }); return }
      const topic = detectMesTopic(question)
      if (!result.sources.length) {
        // 资料里确实没有该主题时，别再让用户自己换关键词：业务规则问题用系统真实流程回答。
        const fallback = knowledgeFallback(topic)
        this.push({ role: 'bot', type: 'text', text: fallback || '在已启用的文档中未找到相关片段。请补充对应资料，或使用文档中的物料、工序、检验项目等具体关键词。' })
        return
      }
      const businessHint = (topic === 'inspection-items' || topic === 'unqualified')
        ? ' 如需按系统实际配置和单据核对，请切到「业务助手」再问同一句。' : ''
      this.push({ role: 'bot', type: 'knowledge', title: '知识库检索结果',
        text: '以下是已启用资料中与问题相关的原文片段：' + businessHint, sources: result.sources,
        source: '本浏览器知识文档；按名称、版本和段落定位原文' })
    },
    showKnowledgeSource(source) {
      if (this.$refs.knowledgeBase) this.$refs.knowledgeBase.showSource(source)
    },
    parseTime(q) {
      const now = new Date()
      const range = q.match(/(?:(20\d{2})\s*[-年]\s*)?(\d{1,2})\s*[-月]\s*(\d{1,2})\s*日?\s*(?:到|至|~|～)\s*(?:(20\d{2})\s*[-年]\s*)?(?:(\d{1,2})\s*[-月]\s*)?(\d{1,2})\s*日?/)
      if (range) {
        const makeDay = (year, month, day) => {
          const d = new Date(year, month - 1, day)
          if (d.getFullYear() !== year || d.getMonth() !== month - 1 || d.getDate() !== day) throw new Error('日期无效，请检查查询时间')
          return this.p2(d)
        }
        const year = Number(range[1] || (now.getMonth() + 1 < Number(range[2]) ? now.getFullYear() - 1 : now.getFullYear()))
        const from = makeDay(year, Number(range[2]), Number(range[3]))
        const to = makeDay(Number(range[4] || year), Number(range[5] || range[2]), Number(range[6]))
        if (from > to) throw new Error('开始日期不能晚于结束日期')
        return { from, to, label: `${from} 至 ${to}` }
      }
      if (/\d\s*日/.test(q)) throw new Error('请提供完整日期范围，例如 2026年10月1日到10月15日')
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
        if (d < 1 || d > 366) throw new Error('查询天数应在 1 到 366 之间')
        return { from: this.p2(new Date(Date.now() - (d - 1) * 86400000)), to: this.p2(now), label: `近 ${d} 天` }
      }
      return { from: this.p2(new Date(now.getFullYear(), now.getMonth(), 1)), to: this.p2(new Date(now.getFullYear(), now.getMonth() + 1, 0)), label: '本月' }
    },

    async matchItem(q) {
      const res = await this.get('/mes/md/mditem/list')
      const rows = res.rows || []
      const hit = rows.filter(i => (q.includes(i.itemName) || (i.itemCode && q.includes(i.itemCode))))
      if (hit.length) return { item: hit[0], items: hit, rows }
      const mention = this.materialMention(q)
      if (mention.length >= 2) {
        const partial = rows.filter(i => (i.itemName && i.itemName.includes(mention)) || (i.itemCode && i.itemCode.toLowerCase().includes(mention.toLowerCase())))
        if (partial.length) return { item: partial[0], items: partial, rows, fuzzy: partial }
      }
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

    /**
     * 检验/流程类问题的物料匹配。
     * 提问通常带「来料检验要检查什么」这类业务词，物料名不一定完整出现（问「红外传感器」，
     * 主数据里叫「红外传感器模块」），所以精确匹配失败时按相邻两字的命中数排序，命中接近的让用户选。
     */
    async matchBusinessItem(q) {
      const matched = await this.matchItem(q)
      if (matched.item || (matched.items && matched.items.length)) return matched
      const rows = matched.rows || []
      const mention = this.materialMention(q)
      if (!mention) return matched
      const grams = []
      for (let i = 0; i + 1 < mention.length; i++) grams.push(mention.slice(i, i + 2))
      const threshold = Math.min(2, grams.length)
      const scored = rows
        .map(item => ({ item, hits: grams.filter(gram => (item.itemName || '').includes(gram)).length }))
        .filter(entry => entry.hits >= threshold)
        .sort((left, right) => right.hits - left.hits)
      if (!scored.length) return { ...matched, fuzzy: [] }
      const best = scored[0].hits
      const candidates = scored.filter(entry => entry.hits >= best - 1).map(entry => entry.item)
      return { item: candidates.length === 1 ? candidates[0] : null, items: candidates, rows }
    },

    /**
     * 剥离时间词、业务动词、单据类型与通用名词后，剩下的才可能是用户点名的物料。
     * 只用于“匹配不到任何物料”时判断用户是否真的点名了物料，避免把全量结果冒充成该物料结果。
     */
    materialMention(q) {
      let s = String(q || '')
      s = s.replace(/(?:(?:20\d{2})\s*[-年]\s*)?\d{1,2}\s*[-月]\s*\d{1,2}\s*日?\s*(?:到|至|~|～)\s*(?:(?:20\d{2})\s*[-年]\s*)?(?:\d{1,2}\s*[-月]\s*)?\d{1,2}\s*日?/g, ' ')
      s = s.replace(/(?:20\d{2}\s*年)?\d{1,2}\s*月份?|本月|这个月|当月|上月|上个月|近\s*\d+\s*天/g, ' ')
      s = s.replace(/(采购|到货|收货|入库|出库|领料|发货|销售|工单|进度|排产|安全库存|库存|盘点|结存|检验|质检|合格率|低于|预警|缺料)/g, ' ')
      s = s.replace(/(还剩多少|剩余多少|剩下多少|剩多少|多少|哪些|哪个|什么|怎么样|怎么|情况|统计|汇总|明细|记录|清单|报表|数据|查询|查一下|查|看一下|看|帮我|请问|请|一共|总计|全部|所有|整体|通知|计划|订单|单据|单号)/g, ' ')
      s = s.replace(/(物料|产品|商品|东西|数量|有|的|了|吗|呢|单)/g, ' ')
      s = s.replace(/[，。？?！!、,.:：;；\s"'（）()【】\[\]]/g, '')
      return s.trim()
    },

    /** 规则引擎：命中直接走确定性查询（0 成本，覆盖常见问法） */
    matchRule(q) {
      if (/低于|安全库存|预警|缺料/.test(q)) return 'queryAlert'
      if (/库存|盘点|结存|还有多少|还剩多少|剩余多少|剩多少|剩下多少/.test(q)) return 'queryStock'
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
      this.llmUnavailable = false
      try {
        const res = await request({
          url: '/mes/ai/intent',
          method: 'post',
          data: { question: q },
          timeout: 20000
        })
        if (res && res.code === 200 && res.data) return res.data
        this.llmUnavailable = true
        return null
      } catch (e) {
        this.llmUnavailable = true
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

    /** 主流程：解析单据指代 → 写操作 → 专属问答 → 规则 → 大模型 → 追问 */
    async route(q) {
      if (this.queryMode === 'knowledge') return this.queryKnowledge(q)
      if (/(?:一键|自动|完整|整个|全流程|业务流程).{0,8}贯通|贯通.{0,8}(?:整个|完整|全流程|业务流程)/.test(q)) {
        this.$refs.businessChain.open()
        this.push({ role: 'bot', type: 'text', text: '已打开全流程贯通。点击“开始执行”，即可按顺序执行采购、质检、生产和销售主链，默认生产 10 台、销售 6 台、结存 4 台。执行会生成真实单据并变更库存。' })
        return
      }
      if (await this.handleArrivalFollowup(q)) return
      if (/执行|过账/.test(q) && /采购入库|原料入库|原材料入库|生产领料|领料单|产品入库|成品入库|销售出库|销售发货/.test(q)) return this.preparePosting(q)
      // 业务规则题先走确定性回答：检验项目、不合格处置在系统里都有真实配置和单据可查，
      // 不该落到“知识库无结果”或让模型自由发挥。
      const topic = detectMesTopic(q)
      if (topic) {
        this.lastStock = null
        this.lastWorkorder = null
        this.waitingWeatherCity = false
        if (topic === 'unqualified') return this.queryUnqualifiedHandling()
        return this.queryInspectionItems(q)
      }
      // 写操作优先：新增/创建单据必须排在入库、库存等读查询规则之前，
      // 否则“帮我添加一条到货通知”会因为含“采购/到货”被答成入库汇总。
      const action = this.matchAction(q)
      if (action === 'arrival') return this.prepareArrivalNotice(q)
      if (action === 'unsupported') {
        this.lastStock = null
        this.lastWorkorder = null
        this.waitingWeatherCity = false
        this.push({
          role: 'bot', type: 'ask',
          text: `这是新增类操作，我不会用查询结果代替。目前能直接新增的是「到货通知」和「生产工单草稿」，${this.actionDocName(q)}请到对应菜单新增${/入库/.test(q) ? '（采购入库由到货通知 + 来料检验联动生成）' : ''}。要我先帮您登记到货通知吗？`,
          options: ['帮我新增到货通知', '本月采购入库汇总']
        })
        return
      }
      if (/放假|节假日|假期|休假|调休/.test(q)) {
        this.lastStock = null
        this.lastWorkorder = null
        this.waitingWeatherCity = false
        if (/请假|年假|我.*休假|个人休假/.test(q)) {
          this.push({ role: 'bot', type: 'text', text: '节假日设置记录的是系统日历的休息日和工作日，不能代表个人请假或年假。请在班组排班或人员管理中核对个人安排。' })
          return
        }
        return this.queryHolidays(q)
      }
      if (/有哪些|有哪(?:些|几个)|多少|名单|列表|查看|查询|列出/.test(q)) {
        if (/供应商/.test(q) && !/订单|采购|退货|入库/.test(q)) return this.queryBasicList('vendor')
        if (/仓库/.test(q) && !/库存|入库|出库|流水|物料/.test(q)) return this.queryBasicList('warehouse')
        if (/班组/.test(q) && !/成员|人员|排班|日期|今天|明天/.test(q)) return this.queryBasicList('team')
        if (/设备|机器|机械/.test(q) && !/异常|不合格|点检|维修|故障|运行|停机|状态|保养/.test(q)) return this.queryBasicList('equipment')
      }
      if (/功能|模块|能做什么|怎么用/.test(q)) {
        const guides = [
          { re: /主数据|基础数据/, text: '主数据可以维护物料、客户、供应商及生产基础信息。可以问我“公司有哪些客户”或“有哪些供应商”。' },
          { re: /仓储|仓库管理/, text: '仓储包括库存、采购入库、领料和销售出库。可以问我“盘点上壳库存”或“有哪些仓库”。' },
          { re: /生产管理|生产模块/, text: '生产模块包含工单、排产、工艺和报工。可以问我指定工单进度或“哪些工单已经超期”。' },
          { re: /质量|质检/, text: '质量模块包括来料、过程和出货检验。可以问我“本月过程检验合格率”。' },
          { re: /设备管理|设备模块/, text: '设备模块包括设备台账和点检记录。可以问我“有哪些设备”或“哪些设备最近点检有异常”。' },
          { re: /排班|班组/, text: '班组排班包括班组、排班计划、日历和节假日设置。可以问我“有哪些班组”或“公司什么时候放假”。' },
          { re: /系统管理|系统模块/, text: '系统模块包含用户、角色和权限等配置。可以问我“我的角色是什么”；可见内容以账号权限为准。' }
        ]
        const guide = guides.find(g => g.re.test(q))
        if (guide) { this.push({ role: 'bot', type: 'text', text: guide.text }); return }
      }
      if (/设备|机器|机械/.test(q)) {
        this.lastStock = null
        this.lastWorkorder = null
        this.waitingWeatherCity = false
        if (/不合格|异常|不正常|点检/.test(q)) return this.queryAbnormalEquipment()
        this.push({ role: 'bot', type: 'ask', text: '你想查点检异常吗？如需查看设备当前运行状态，请前往设备台账。', options: ['查看最近点检异常的设备'] })
        return
      }
      if (/我|当前账号|我的/.test(q) && /角色|权限/.test(q) && !/创建|新增|修改|删除/.test(q)) {
        const info = await request({ url: '/getInfo', method: 'get' })
        const roles = info.roles || []
        this.push({ role: 'bot', type: 'text', text: roles.length ? `当前账号的角色标识：${roles.join('、')}。具体可用功能以菜单和操作权限为准。` : '当前账号没有查到角色信息，请联系管理员。' })
        return
      }
      if (/客户|合作客户/.test(q) && /有哪些|有哪(?:些|几个)|有什么|多少|列表|清单|查看|查询|查一下|列出|介绍/.test(q) && !/订单|出库|销售|发货|退货|金额|联系人|电话/.test(q)) {
        this.waitingWeatherCity = false
        this.lastStock = null
        this.lastWorkorder = null
        return this.queryCustomers(q)
      }
      if (/部门|组织架构|组织结构/.test(q) && /有哪些|有哪几个|有什么|列表|结构|架构|介绍|查看|查询|查一下|列出/.test(q)) {
        this.waitingWeatherCity = false
        this.lastStock = null
        this.lastWorkorder = null
        return this.queryDepartments()
      }
      if (/车间/.test(q) && /有哪些|有哪几个|有什么|列表|查看|查询|查一下|列出/.test(q)) {
        this.waitingWeatherCity = false
        this.lastStock = null
        this.lastWorkorder = null
        return this.queryWorkshops()
      }
      if (/(到货通知|到货单|收货通知)/.test(q) && /流程|步骤|怎么|如何|怎样|在哪|哪里/.test(q)) {
        this.waitingWeatherCity = false
        this.push({ role: 'bot', type: 'text', text: '新增到货通知有两种方式：① 直接告诉我物料和数量，例如“帮我新增到货通知：10 个上壳、10 个电池”，我生成草稿、你确认后写入；② 到「仓储管理 → 到货通知」点新增，保存明细后提交。提交时会按每行的“是否来料检验”生成来料检验草稿。' })
        return
      }
      if (/采购|到货|入库/.test(q) && /流程|步骤|怎么走|怎么操作|如何操作/.test(q)) {
        this.push({ role: 'bot', type: 'text', text: '采购入库通常按以下步骤进行：登记到货通知，需检物料完成来料检验，核对合格数量后处理采购入库草稿，再完成上架并执行入库。具体操作以单据状态和账号权限为准。' })
        return
      }
      if (/你(能|可以|会)(做|干|帮|查|回答)|你是(谁|什么)|介绍(?:一下)?(?:你自己|自己|你)|自我介绍|有什么功能|如何使用|怎么使用/.test(q)) {
        this.waitingWeatherCity = false
        this.push({ role: 'bot', type: 'text', text: '你好，我是 MES 智能助手。我可以查询库存、入出库、工单进度、安全库存和过程检验；也能经你确认后新增到货通知、创建生产工单草稿、打开指定工单，以及查询指定城市的当前天气。业务数据会显示来源，天气需要联网。' })
        return
      }
      if (/天气|气温|温度多少|下雨/.test(q)) {
        this.lastStock = null
        this.lastWorkorder = null
        if (/明天|后天|下周|昨天/.test(q)) { this.waitingWeatherCity = false; this.push({ role: 'bot', type: 'text', text: '目前支持查询指定城市的当前天气，例如“上海今天天气怎么样”。' }); return }
        const city = q.replace(/请问|帮我|查一下|查询|查查|查|看一下|看看|告诉我|今天|今日|现在|当前|实时|的|天气|气温|温度多少|下雨|怎么样|如何|多少|吗|呢|是否|会|[\s，。？?！!]/g, '').replace(/市$/, '')
        if (!city) { this.waitingWeatherCity = true; this.push({ role: 'bot', type: 'text', text: '请告诉我想查询哪个城市的天气，例如上海。' }); return }
        this.waitingWeatherCity = false
        return this.queryWeather(city)
      }
      if (this.waitingWeatherCity && /^[\u4e00-\u9fa5A-Za-z]{2,20}市?$/.test(q) && !this.matchRule(q)) {
        this.waitingWeatherCity = false
        return this.queryWeather(q.replace(/市$/, ''))
      }
      this.waitingWeatherCity = false
      if (/(创建|新建|建立|生成).{0,30}(工单|生产)|(?:工单).{0,10}(创建|新建|生成)/.test(q)) {
        return this.prepareDraft(q)
      }
      if (/^(打开|查看|进入)(它|该工单|这个工单|详情)$/.test(q) && this.lastWorkorder) {
        return this.findAndOpenWorkorder(this.lastWorkorder)
      }
      const open = q.match(/(?:打开|查看详情|进入)\s*(?:工单)?\s*([A-Za-z][A-Za-z0-9_-]*\d[A-Za-z0-9_-]*)/i)
      if (open && /工单|^MO/i.test(q)) return this.findAndOpenWorkorder(open[1])
      // 只处理明确的省略式库存追问，不将其他主题误当作上一轮查询。
      const dateFollowup = /^(?:那|那么)?\s*(?:(?:20\d{2}\s*年\s*)?\d{1,2}\s*月(?:份)?|近\s*\d+\s*天|上个月|上月|本月|这个月|当月)\s*(?:呢|怎么样|的情况)?[？?。！!]*$/.test(q)
      const warehouseFollowup = /^(?:只看|仅看|查看|换成|那|那么)\s*.+(?:仓库|仓|库)(?:呢|的情况)?[？?。！!]*$/.test(q)
      if (this.lastStock && warehouseFollowup && !this.matchRule(q)) {
        const name = this.lastStock.warehouses.find(n => q.includes(n))
        if (!name) {
          this.push({ role: 'bot', type: 'ask', text: '没有找到该物料对应的仓库，请选择完整仓库名称。', options: this.lastStock.warehouses.map(n => `只看${n}`) })
          return
        }
        return this.queryStock(`盘点 ${this.lastStock.time} ${this.lastStock.itemCode} ${name} 库存`)
      }
      if (this.lastStock && (/^(那|那么|上个月|本月|这个月|上月)/.test(q) || dateFollowup) && !this.matchRule(q)) {
        const t = q.match(/(?:20\d{2}\s*年\s*)?\d{1,2}\s*月(?:份)?|上个月|上月|本月|这个月|当月|近\s*\d+\s*天/)
        if (dateFollowup && t) return this.queryStock(`盘点 ${t[0]} ${this.lastStock.itemCode} ${this.lastStock.warehouse || ''} 库存`)
        const matched = await this.matchItem(q)
        const item = matched.items && matched.items.length === 1 ? matched.item : null
        if (matched.items && matched.items.length > 1) return this.queryStock('盘点 ' + q + ' 库存')
        if (item || t) return this.queryStock(`盘点 ${t ? t[0] : this.lastStock.time} ${item ? item.itemCode : this.lastStock.itemCode} ${this.lastStock.warehouse || ''} 库存`)
      }
      // ① 规则命中
      const ruleFn = this.matchRule(q)
      if (ruleFn) return this[ruleFn](q)

      // ② 规则未命中：请大模型解析槽位
      const slots = await this.askLlm(q)
      if (slots && slots.metric === 'action') {
        // 模型识别到“写操作”：仍走本地确定性流程，绝不拿查询结果顶替
        const act = this.matchAction(q)
        if (act === 'arrival') return this.prepareArrivalNotice(q)
        this.lastStock = null
        this.lastWorkorder = null
        this.push({
          role: 'bot', type: 'ask',
          text: '我理解这是新增/修改类操作，不会用查询结果代替。目前能直接新增的是「到货通知」和「生产工单草稿」，其余单据请到对应菜单操作。',
          options: ['帮我新增到货通知', '本月采购入库汇总']
        })
        return
      }
      if (slots && slots.metric && slots.metric !== 'unknown') {
        // 写操作动词 + 读指标：模型把“新增单据”看成了查询，宁可追问也不答非所问
        if (/(新增|添加|新建|创建|录入|登记|删除|修改|更新|提交)/.test(q) && !/查询|查一下|查看|看看|列出|汇总|统计|明细|列表|多少|几张/.test(q)) {
          this.push({
            role: 'bot', type: 'ask',
            text: '你的意思是要改动单据数据，我不会用查询结果代替。目前能直接新增的是「到货通知」和「生产工单草稿」，其余单据请到对应菜单操作。',
            options: ['帮我新增到货通知', '本月采购入库汇总']
          })
          return
        }
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
        text: this.llmUnavailable ? '智能意图识别暂时不可用。你仍可以直接查询库存、客户、工单或节假日等已支持的内容。' : '我暂时没有识别出这个问题对应的查询方式，并不代表系统没有相关信息。请换个说法或选择下面的示例。',
        options: ['盘点某物料某月的库存', '本月采购入库情况', '工单进度落后清单', '公司什么时候放假']
      })
    },

    async queryAbnormalEquipment() {
      if (!this.$auth.hasPermi('mes:dv:checkrecord:list')) {
        this.push({ role: 'bot', type: 'text', text: '当前账号没有设备点检记录查询权限，请联系管理员。' })
        return
      }
      const [records, lines] = await Promise.all([
        this.get('/mes/dv/checkrecord/list', { status: 'FINISHED' }),
        this.get('/mes/dv/checkrecordline/list', { checkStatus: 'N' })
      ])
      const latest = {}
      ;(records.rows || []).filter(r => r.status === 'FINISHED' && r.machineryId && r.checkTime).forEach(r => {
        const key = String(r.machineryId)
        const old = latest[key]
        if (!old || String(r.checkTime) > String(old.checkTime) || (String(r.checkTime) === String(old.checkTime) && Number(r.recordId) > Number(old.recordId))) latest[key] = r
      })
      const abnormalities = {}
      ;(lines.rows || []).filter(l => l.checkStatus === 'N').forEach(l => {
        const key = String(l.recordId)
        if (!abnormalities[key]) abnormalities[key] = []
        abnormalities[key].push([l.subjectName, l.checkResult].filter(Boolean).join('：') || '异常')
      })
      const rows = Object.values(latest).filter(r => abnormalities[String(r.recordId)]).map(r => ({
        machineryCode: r.machineryCode, machineryName: r.machineryName, checkTime: r.checkTime,
        recordId: r.recordId, abnormal: abnormalities[String(r.recordId)].join('；')
      }))
      this.push({ role: 'bot', type: 'list', title: '最近一次已完成点检存在异常的设备',
        columns: [{ prop: 'machineryCode', label: '设备编码', width: 140 }, { prop: 'machineryName', label: '设备名称' }, { prop: 'checkTime', label: '点检时间', width: 170 }, { prop: 'abnormal', label: '异常项目和描述' }, { prop: 'recordId', label: '点检记录编号', width: 120 }],
        rows, source: '来源：设备点检记录及异常明细；按设备最近一次有点检时间的已完成记录判断，未包含无已完成记录的设备，不代表设备当前运行状态。' })
      this.push({ role: 'bot', type: 'text', text: rows.length ? `最近一次已完成点检中，有 ${rows.length} 台设备记录了异常，具体项目见上表。` : '已查询的最近一次已完成点检中没有发现异常记录；未完成点检或没有点检记录的设备无法据此判断。' })
    },

    async queryHolidays(q) {
      if (!this.$auth.hasPermi('mes:cal:calholiday:list')) {
        this.push({ role: 'bot', type: 'text', text: '当前账号没有节假日设置的查询权限，请联系管理员。' })
        return
      }
      if (/春节|国庆|劳动节|五一|元旦|清明|端午|中秋/.test(q) && !/\d\s*月|本月|上月|这个月|上个月/.test(q)) {
        this.push({ role: 'bot', type: 'text', text: '系统节假日设置只记录日期和休息类型，不记录节日名称，无法直接判断某个节日的放假安排。请提供具体月份，例如“查询今年10月的放假安排”。' })
        return
      }
      const response = await request({ url: '/mes/cal/calholiday/list', method: 'get' })
      if (!response || !Array.isArray(response.data)) throw new Error('节假日数据格式异常，请稍后重试')
      const now = new Date()
      let from = this.p2(now), to = '', scope = '今天及之后'
      const year = q.match(/(20\d{2})\s*年/)
      if (/\d\s*月|本月|上月|这个月|上个月|当月|近\s*\d+\s*天/.test(q)) {
        const t = this.parseTime(q); from = t.from; to = t.to; scope = `${from} 至 ${to}`
      } else if (year || /今年|明年|去年/.test(q)) {
        const y = year ? Number(year[1]) : now.getFullYear() + (/明年/.test(q) ? 1 : /去年/.test(q) ? -1 : 0)
        from = `${y}-01-01`; to = `${y}-12-31`; scope = `${y} 年`
      }
      const rows = response.data.filter(r => r.holidayType === 'HOLIDAY' && r.theDay && String(r.theDay).slice(0, 10) >= from && (!to || String(r.theDay).slice(0, 10) <= to))
        .map(r => ({ theDay: String(r.theDay).slice(0, 10), type: '休息日' })).sort((a, b) => a.theDay.localeCompare(b.theDay))
      this.push({ role: 'bot', type: 'list', title: `${scope} · 系统节假日设置`, columns: [{ prop: 'theDay', label: '日期', width: 150 }, { prop: 'type', label: '日历类型', width: 120 }], rows, source: '来源：班组排班 → 节假日设置，仅列系统标记为休息日的日期；不等同于个人排班或官方假期通知。' })
      this.push({ role: 'bot', type: 'text', text: rows.length ? `${scope}，系统中标记了 ${rows.length} 条休息日记录，最早一条为 ${rows[0].theDay}。个人是否休息还需结合所在班组的排班及公司通知确认。` : `${scope}没有查到系统标记的休息日。这不代表一定不放假，可能是尚未维护，请以公司通知和个人排班为准。` })
    },
    async queryBasicList(kind) {
      const configs = {
        vendor: { label: '供应商', permission: 'mes:md:vendor:list', url: '/mes/md/vendor/list', code: 'vendorCode', name: 'vendorName', source: '主数据 → 供应商管理' },
        warehouse: { label: '仓库', permission: 'mes:wm:warehouse:list', url: '/mes/wm/warehouse/list', code: 'warehouseCode', name: 'warehouseName', source: '仓储 → 仓库设置' },
        team: { label: '班组', permission: 'mes:cal:team:list', url: '/mes/cal/team/list', code: 'teamCode', name: 'teamName', source: '班组排班 → 班组设置' },
        equipment: { label: '设备', permission: 'mes:dv:machinery:list', url: '/mes/dv/machinery/list', code: 'machineryCode', name: 'machineryName', source: '设备 → 设备台账' }
      }
      const config = configs[kind]
      if (!this.$auth.hasPermi(config.permission)) { this.push({ role: 'bot', type: 'text', text: `当前账号没有${config.label}列表查询权限，请联系管理员。` }); return }
      this.lastStock = null; this.lastWorkorder = null; this.waitingWeatherCity = false
      const result = await this.get(config.url)
      const rows = (result.rows || []).map(r => ({ code: r[config.code] || '未填写', name: r[config.name] || '未填写' }))
      this.push({ role: 'bot', type: 'list', title: `${config.label}名单`, columns: [{ prop: 'code', label: `${config.label}编码`, width: 160 }, { prop: 'name', label: `${config.label}名称` }], rows, source: `来源：${config.source}，共 ${rows.length} 条；以当前账号可查询范围为准。` })
      this.push({ role: 'bot', type: 'text', text: rows.length ? `当前查询到 ${rows.length} 条${config.label}记录，编码和名称可在${config.source}核对。` : `当前查询范围内没有查到${config.label}记录，请到${config.source}核对。` })
    },
    async queryCustomers(q) {
      if (!this.$auth.hasPermi('mes:md:client:list')) {
        this.push({ role: 'bot', type: 'text', text: '当前账号没有客户列表查询权限，请联系管理员。' })
        return
      }
      const params = /启用|有效/.test(q) && !/未启用|停用|禁用/.test(q) ? { enableFlag: 'Y' } : /未启用|停用|禁用/.test(q) ? { enableFlag: 'N' } : {}
      const result = await this.get('/mes/md/client/list', params)
      const rows = (result.rows || []).map(c => ({ clientCode: c.clientCode || '未填写', clientName: c.clientName || '未填写', clientNick: c.clientNick || '未填写', status: c.enableFlag === 'Y' ? '启用' : c.enableFlag === 'N' ? '停用' : '未标注' }))
      this.push({ role: 'bot', type: 'list', title: '客户名单', columns: [
        { prop: 'clientCode', label: '客户编码', width: 150 },
        { prop: 'clientName', label: '客户名称' },
        { prop: 'clientNick', label: '客户简称' },
        { prop: 'status', label: '状态', width: 90 }
      ], rows, source: `数据来源：主数据 → 客户管理，共 ${rows.length} 条；显示范围以当前账号权限及接口返回为准` })
      const enabled = rows.filter(c => c.status === '启用').length
      const disabled = rows.filter(c => c.status === '停用').length
      const unknown = rows.length - enabled - disabled
      this.push({ role: 'bot', type: 'text', text: rows.length ? `目前查询到 ${rows.length} 条客户记录，其中启用 ${enabled} 条、停用 ${disabled} 条${unknown ? `、状态未标注 ${unknown} 条` : ''}。客户编码和名称可在客户管理中核对。` : '当前查询范围内没有找到客户记录，可到客户管理核对筛选条件。' })
    },
    async queryDepartments() {
      const result = await request({ url: '/system/dept/treeselect', method: 'get' })
      const names = []
      const visit = nodes => (nodes || []).forEach(n => { if (n.label) names.push(n.label); visit(n.children) })
      visit(result.data)
      this.push({ role: 'bot', type: 'text', text: names.length ? `当前账号可见的组织与部门有：${names.join('、')}。来源：系统部门树；显示范围受账号数据权限限制。` : '当前账号没有查到可见的部门信息，请联系管理员核对数据范围。' })
    },
    async queryWorkshops() {
      const result = await this.get('/mes/md/workshop/list', { enableFlag: 'Y' })
      const names = (result.rows || []).filter(r => r.enableFlag !== 'N' && r.workshopName).map(r => r.workshopName)
      this.push({ role: 'bot', type: 'text', text: names.length ? `系统中当前启用的车间有：${names.join('、')}。来源：车间设置。` : '没有查到启用的车间，请在车间设置中核对。' })
    },

    async queryWeather(city) {
      const controller = new AbortController()
      const timer = setTimeout(() => controller.abort(), 10000)
      try {
        const provinces = { 山东: 'Shandong', 广东: 'Guangdong', 江苏: 'Jiangsu', 浙江: 'Zhejiang', 福建: 'Fujian', 河南: 'Henan', 河北: 'Hebei', 湖北: 'Hubei', 湖南: 'Hunan', 四川: 'Sichuan', 安徽: 'Anhui', 江西: 'Jiangxi', 辽宁: 'Liaoning', 吉林: 'Jilin', 黑龙江: 'Heilongjiang', 陕西: 'Shaanxi', 山西: 'Shanxi', 云南: 'Yunnan', 贵州: 'Guizhou', 海南: 'Hainan', 甘肃: 'Gansu', 青海: 'Qinghai', 新疆: 'Xinjiang', 广西: 'Guangxi', 内蒙古: 'Inner Mongolia', 宁夏: 'Ningxia', 西藏: 'Tibet' }
        const province = Object.keys(provinces).find(p => city.startsWith(p) && city.replace(p, '').replace(/^省/, '').length >= 2)
        const cityName = province ? city.slice(province.length).replace(/^省/, '') : city
        // 部分中文地名只匹配到村庄或岛屿，用常见城市的拼音检索，再以返回的中文名核对。
        const cityAliases = { 青岛: 'Qingdao', 北京: 'Beijing', 上海: 'Shanghai', 天津: 'Tianjin', 重庆: 'Chongqing', 广州: 'Guangzhou', 深圳: 'Shenzhen', 杭州: 'Hangzhou', 南京: 'Nanjing', 济南: 'Jinan', 成都: 'Chengdu', 武汉: 'Wuhan', 西安: "Xi'an", 郑州: 'Zhengzhou', 苏州: 'Suzhou', 厦门: 'Xiamen', 合肥: 'Hefei', 长沙: 'Changsha', 福州: 'Fuzhou', 昆明: 'Kunming', 沈阳: 'Shenyang', 大连: 'Dalian', 哈尔滨: 'Harbin', 宁波: 'Ningbo' }
        const searchName = cityAliases[cityName] || cityName
        const geoResponse = await fetch(`https://geocoding-api.open-meteo.com/v1/search?name=${encodeURIComponent(searchName)}&count=100&language=zh&format=json`, { signal: controller.signal })
        if (!geoResponse.ok) throw new Error('weather service')
        const geo = await geoResponse.json()
        const places = (geo.results || []).filter(p => !province || (p.admin1 && (p.admin1.includes(province) || p.admin1.toLowerCase().includes(provinces[province].toLowerCase()))))
        if (!places.length) { this.push({ role: 'bot', type: 'text', text: `没有找到“${city}”，请提供更完整的城市名称。` }); return }
        const exact = places.filter(p => p.name === cityName || p.name === cityName + '市')
        const candidates = exact.length ? exact : places
        const rank = p => ({ PPLC: 4, PPLA: 3, PPLA2: 2, PPLA3: 1 }[p.feature_code] || 0)
        const highest = Math.max(...candidates.map(rank))
        const preferred = candidates.filter(p => rank(p) === highest)
        if (preferred.length !== 1) { this.push({ role: 'bot', type: 'text', text: `“${city}”匹配到多个地点，请提供省份和城市名称。` }); return }
        const place = preferred[0]
        const response = await fetch(`https://api.open-meteo.com/v1/forecast?latitude=${encodeURIComponent(place.latitude)}&longitude=${encodeURIComponent(place.longitude)}&current=temperature_2m,weather_code&timezone=auto`, { signal: controller.signal })
        if (!response.ok) throw new Error('weather service')
        const data = await response.json()
        if (!data.current || data.current.temperature_2m == null || data.current.weather_code == null || !data.current.time) throw new Error('weather data missing')
        const code = data.current.weather_code
        const desc = code === 0 ? '晴' : [1, 2, 3].includes(code) ? '多云' : [45, 48].includes(code) ? '雾' : [51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82].includes(code) ? '有雨' : [71, 73, 75, 77, 85, 86].includes(code) ? '有雪' : [95, 96, 99].includes(code) ? '雷雨' : '天气状况未知'
        this.push({ role: 'bot', type: 'text', text: `${place.name}${place.admin1 && place.admin1 !== place.name ? '（' + place.admin1 + '）' : ''}当前天气：${desc}，气温 ${data.current.temperature_2m}°C。数据时间 ${data.current.time.replace('T', ' ')}（当地时间），来源：Open-Meteo。` })
      } catch (e) {
        this.push({ role: 'bot', type: 'text', text: '天气服务暂时不可用，请检查网络后重试；库存和工单查询仍可使用。' })
      } finally { clearTimeout(timer) }
    },

    postingConfig(q) {
      if (/采购入库|原料入库|原材料入库/.test(q)) return { label: '采购入库', url: '/mes/wm/itemrecpt', codeKey: 'recptCode', idKey: 'recptId', status: 'APPROVED', permission: 'mes:wm:itemrecpt', path: '/mes/wm/itemrecpt' }
      if (/生产领料|领料单/.test(q)) return { label: '生产领料', url: '/mes/wm/issueheader', codeKey: 'issueCode', idKey: 'issueId', status: 'APPROVED', permission: 'mes:wm:issueheader', path: '/mes/wm/issue' }
      if (/产品入库|成品入库/.test(q)) return { label: '产品入库', url: '/mes/wm/productrecpt', codeKey: 'recptCode', idKey: 'recptId', status: 'APPROVED', permission: 'mes:wm:productrecpt', path: '/mes/wm/productrecpt' }
      if (/销售出库|销售发货/.test(q)) return { label: '销售出库', url: '/mes/wm/productsales', codeKey: 'salesCode', idKey: 'salesId', status: 'UNEXECUTE', permission: 'mes:wm:productsales', path: '/mes/wm/productsales' }
      return null
    },
    async preparePosting(q) {
      const config = this.postingConfig(q)
      if (!config) return
      if (!this.$auth.hasPermi(config.permission + ':list') || !this.$auth.hasPermi(config.permission + ':edit')) {
        this.push({ role: 'bot', type: 'text', text: `当前账号缺少${config.label}查询或执行权限。` }); return
      }
      const codeMatch = q.match(/[A-Za-z][A-Za-z0-9_-]*\d[A-Za-z0-9_-]*/)
      if (!codeMatch) { this.push({ role: 'bot', type: 'text', text: `请提供完整的${config.label}单号，例如“执行${config.label}单 PR20261030002”。` }); return }
      const code = codeMatch[0]
      const result = await this.get(config.url + '/list', { [config.codeKey]: code })
      const docs = (result.rows || []).filter(r => String(r[config.codeKey] || '').toUpperCase() === code.toUpperCase())
      if (docs.length !== 1) { this.push({ role: 'bot', type: 'text', text: docs.length ? '该单号匹配到多张单据，请在单据页面核对。' : `未找到${config.label}单 ${code}，请核对单号及权限。` }); return }
      const doc = docs[0]
      if (doc.status !== config.status) { this.push({ role: 'bot', type: 'text', text: `${config.label}单 ${code} 当前状态为 ${doc.status || '未知'}，未处于可执行状态（${config.status}），请先在单据页面完成前置步骤。` }); return }
      if (!doc[config.idKey]) throw new Error('单据缺少有效标识，无法执行')
      const lineUrls = { '/mes/wm/itemrecpt': ['/mes/wm/itemrecptline/list', 'quantityRecived'], '/mes/wm/issueheader': ['/mes/wm/issueline/list', 'quantityIssued'], '/mes/wm/productrecpt': ['/mes/wm/productrecptline/list', 'quantityRecived'], '/mes/wm/productsales': ['/mes/wm/productsalesline/list', 'quantitySales'] }
      const [lineUrl, qtyKey] = lineUrls[config.url]
      const lineResult = await this.get(lineUrl, { [config.idKey]: doc[config.idKey] })
      const lines = (lineResult.rows || []).filter(r => String(r[config.idKey]) === String(doc[config.idKey])).map(r => ({ itemCode: r.itemCode, itemName: r.itemName, quantity: r[qtyKey], unit: r.unitName || r.unitOfMeasure, batchCode: r.batchCode || '以明细为准' }))
      if (!lines.length) { this.push({ role: 'bot', type: 'text', text: `未找到${config.label}单明细，请先在单据页面核对。` }); return }
      this.push({ role: 'bot', type: 'posting', label: config.label, code: doc[config.codeKey], id: doc[config.idKey], status: doc.status, url: config.url, permission: config.permission, path: config.path, lines, busy: false, finished: false, error: '' })
    },
    cancelPosting(m) { if (!m.busy) { m.finished = true; m.error = '已取消，未执行过账。' } },
    async confirmPosting(m) {
      if (m.busy || m.finished) return
      if (!this.$auth.hasPermi(m.permission + ':edit')) { m.error = '当前账号没有执行权限。'; return }
      m.busy = true
      try {
        await request({ url: m.url + '/' + encodeURIComponent(m.id), method: 'put' })
        m.finished = true
        m.error = `${m.label}单 ${m.code} 已执行，请在对应单据和库存流水页面核对。`
      } catch (e) {
        m.finished = true
        m.error = `执行结果未能确认：${(e && e.message) || '服务异常'}。请先到单据页面核对状态和流水，避免重复操作。`
      } finally { m.busy = false }
    },
    async prepareDraft(q) {
      this.lastStock = null
      this.lastWorkorder = null
      if (!this.$auth.hasPermi('mes:pro:workorder:add')) {
        this.push({ role: 'bot', type: 'text', text: '当前账号没有创建生产工单的权限。' }); return
      }
      const number = q.match(/(?:生产|数量为?|数量[:：]?\s*)\s*(\d+(?:\.\d+)?)\s*(?:台|个|件|套|只|支|把|份)?|(?:^|\D)(\d+(?:\.\d+)?)\s*(?:台|个|件|套|只|支|把|份)/)
      const quantity = number ? Number(number[1] || number[2]) : 0
      if (!Number.isInteger(quantity) || quantity < 1 || quantity > 99999999) {
        this.push({ role: 'bot', type: 'text', text: '请说明 1 至 99999999 的整数生产数量，例如“创建生产 10 台测温枪样机的工单”。' }); return
      }
      const result = await this.get('/mes/md/mditem/list', { itemOrProduct: 'PRODUCT', enableFlag: 'Y' })
      const items = (result.rows || []).filter(i => i.itemOrProduct === 'PRODUCT' && i.enableFlag !== 'N' && ((i.itemName && q.includes(i.itemName)) || (i.itemCode && q.includes(i.itemCode))))
      if (items.length !== 1) {
        this.push({ role: 'bot', type: 'text', text: items.length ? '匹配到多个产品，请用完整产品编码重新说明。' : '没有找到匹配的启用产品，请提供产品完整名称或编码。' }); return
      }
      this.push({ role: 'bot', type: 'draft', item: items[0], quantity, requestDate: '', busy: false, finished: false, workorderId: null, error: '' })
    },
    cancelDraft(draft) {
      if (draft.busy || draft.finished) return
      draft.finished = true
      draft.error = '已取消，未创建工单。'
    },
    async confirmDraft(draft) {
      if (draft.busy || draft.finished) return
      if (!this.$auth.hasPermi('mes:pro:workorder:add')) { draft.error = '当前账号没有创建生产工单的权限。'; return }
      if (!draft.requestDate || !/^\d{4}-\d{2}-\d{2}$/.test(draft.requestDate)) { draft.error = '请选择需求日期。'; return }
      draft.error = ''
      draft.busy = true
      let posting = false
      try {
        const code = await request({ url: '/system/autocode/get/WORKORDER_CODE', method: 'get' })
        if (typeof code !== 'string' || !code.trim()) throw new Error('无法生成工单编号，请稍后重试。')
        const item = draft.item
        posting = true
        const result = await request({ url: '/mes/pro/workorder', method: 'post', data: {
          workorderCode: code, workorderName: (item.itemName + '生产工单').slice(0, 100),
          workorderType: 'SELF', orderSource: 'STORE', productId: item.itemId,
          productCode: item.itemCode, productName: item.itemName, productSpc: item.specification,
          unitOfMeasure: item.unitOfMeasure, unitName: item.unitName, quantity: draft.quantity,
          requestDate: draft.requestDate, parentId: 0, status: 'PREPARE'
        } })
        draft.workorderId = result.data
        draft.finished = true
        draft.error = `草稿 ${code} 已创建，可在生产工单中查看。`
      } catch (e) {
        const reason = e && e.message && !/timeout|network/i.test(e.message) ? `（${e.message}）` : ''
        draft.error = posting ? `创建请求未能确认结果${reason}，请先到生产工单列表核对，避免重复创建。` : `工单编号生成失败${reason}，请稍后重试。`
        if (posting) draft.finished = true
      } finally { draft.busy = false }
    },

    /* ---------- 写操作意图：新增到货通知 ---------- */
    /**
     * 识别“写操作”。规则引擎原来只有查询意图，句子里出现“采购/到货/入库”就会被当成入库汇总，
     * 于是“帮我添加一条到货通知”被答成了入库统计。这里把新增/创建类请求单独识别，并排在读查询规则之前。
     */
    matchAction(q) {
      const text = String(q || '')
      // “怎么新增 / 流程是什么 / 为什么不能新增”属于操作指引或追问，不是写指令
      if (/流程|步骤|怎么|如何|怎样|在哪|哪里|为什么/.test(text)) return null
      // “查询新增的物料”这类句子里的“新增”是定语，仍应按查询处理
      if (/(查询|查一下|查看|看看|列出|找)[^，。]{0,10}(新增|添加|新建|创建)的/.test(text)) return null
      if (!/(新增|添加|新建|创建|录入|登记|补录|建一张|开一张|加一条|加一笔|记一笔)/.test(text)) return null
      if (/工单|生产任务|生产计划/.test(text)) return null // 交给已有“生产工单草稿”流程
      if (/到货|收货/.test(text)) return 'arrival'
      return 'unsupported'
    },
    actionDocName(q) {
      if (/入库/.test(q)) return '采购入库单'
      if (/出库|领料/.test(q)) return '出库/领料单'
      if (/检验|质检/.test(q)) return '检验单'
      if (/退料|退货|退供/.test(q)) return '退货/退料单'
      if (/盘点/.test(q)) return '盘点单'
      return '这类单据'
    },
    /** 从一句话里解析“物料 + 数量”，例：公司采购了 10 个上壳部件，10 个电池 */
    parseArrivalLines(q, items) {
      const text = String(q || '')
      // 日期里的数字不是到货数量
      const dateSpans = []
      const dateRe = /(20\d{2})\s*[-/年]\s*(\d{1,2})\s*[-/月]\s*(\d{1,2})/g
      let dm
      while ((dm = dateRe.exec(text)) !== null) dateSpans.push([dm.index, dm.index + dm[0].length])
      const nums = []
      const numRe = /(\d+(?:\.\d+)?)\s*(个|件|节|只|支|台|套|把|份|箱|包|米|千克|公斤|kg|pcs|卷|片|颗)?/gi
      let nm
      while ((nm = numRe.exec(text)) !== null) {
        const head = text.slice(Math.max(0, nm.index - 1), nm.index)
        const tail = text.slice(nm.index + nm[0].length, nm.index + nm[0].length + 1)
        if (/[年月日号时分秒%]/.test(tail) || /[年月日号]/.test(head)) continue
        if (dateSpans.some(span => nm.index >= span[0] && nm.index < span[1])) continue
        nums.push({ value: Number(nm[1]), idx: nm.index, end: nm.index + nm[0].length })
      }
      const hits = []
      ;(items || []).forEach(item => {
        ;[item.itemName, item.itemCode].filter(Boolean).forEach(name => {
          let idx = text.indexOf(name)
          while (idx >= 0) {
            hits.push({ item, idx, end: idx + name.length })
            idx = text.indexOf(name, idx + name.length)
          }
        })
      })
      // 物料名互相包含时（如“电池”与“电池盒”）只保留更长的那一个，避免重复出行
      const pruned = hits.filter(h => !hits.some(o => o !== h && o.idx <= h.idx && o.end >= h.end && (o.end - o.idx) > (h.end - h.idx)))
      pruned.sort((a, b) => a.idx - b.idx)
      const unique = []
      pruned.forEach(hit => {
        if (!unique.some(u => String(u.item.itemId) === String(hit.item.itemId))) unique.push(hit)
      })
      // 数量归到最近的物料：跨过分句的数字不认，避免“上壳 10 个，电池 5 个”串行
      const assigned = {}
      nums.forEach(num => {
        let best = null
        unique.forEach(hit => {
          const gap = num.idx >= hit.end ? num.idx - hit.end : (hit.idx >= num.end ? hit.idx - num.end : 0)
          const between = num.idx >= hit.end ? text.slice(hit.end, num.idx) : text.slice(num.end, hit.idx)
          if (gap > 10 || /[，,；;、。！!？?]/.test(between)) return
          if (!best || gap < best.gap) best = { hit, gap }
        })
        if (!best) return
        const key = String(best.hit.item.itemId)
        if (!assigned[key] || best.gap < assigned[key].gap) assigned[key] = { num, gap: best.gap }
      })
      // “上壳和电池各 10 个”这类写法：各 = 每行同样的数量
      const each = text.match(/各\s*(\d+(?:\.\d+)?)/)
      if (each) {
        unique.forEach(hit => {
          const key = String(hit.item.itemId)
          if (!assigned[key]) assigned[key] = { num: { value: Number(each[1]) }, gap: 0 }
        })
      }
      const lines = unique.map(hit => {
        const picked = assigned[String(hit.item.itemId)]
        return {
          itemId: hit.item.itemId,
          itemCode: hit.item.itemCode,
          itemName: hit.item.itemName,
          specification: hit.item.specification || '',
          unitOfMeasure: hit.item.unitOfMeasure || '',
          unitName: hit.item.unitName || hit.item.unitOfMeasure || '未标注',
          quantityArrival: picked ? Math.round(picked.num.value * 100) / 100 : 0
        }
      })
      return { hits: lines.length, lines: lines.filter(l => l.quantityArrival > 0), missing: lines.filter(l => !(l.quantityArrival > 0)) }
    },
    /** 到货日期：句中有明确日期用它，否则默认今天 */
    parseArrivalDate(q) {
      const text = String(q || '')
      const pad = n => String(n).padStart(2, '0')
      let m = text.match(/(20\d{2})\s*[-/年]\s*(\d{1,2})\s*[-/月]\s*(\d{1,2})\s*[日号]?/)
      if (m) return `${m[1]}-${pad(m[2])}-${pad(m[3])}`
      m = text.match(/(\d{1,2})\s*月\s*(\d{1,2})\s*[日号]/)
      if (m) return `${new Date().getFullYear()}-${pad(m[1])}-${pad(m[2])}`
      return this.p2(new Date())
    },
    /** 句子里点名了哪个供应商（名称或简称），没有就留空让用户选 */
    matchVendor(q, vendors) {
      const text = String(q || '')
      const found = []
      ;(vendors || []).forEach(v => {
        ;[v.vendorName, v.vendorNick].filter(Boolean).forEach(name => {
          if (text.includes(name)) found.push({ v, len: name.length })
        })
      })
      if (!found.length) return null
      found.sort((a, b) => b.len - a.len)
      return found[0].v
    },
    async prepareArrivalNotice(q) {
      this.lastStock = null
      this.lastWorkorder = null
      this.waitingWeatherCity = false
      if (!this.$auth.hasPermi('mes:wm:arrivalnotice:add')) {
        this.push({ role: 'bot', type: 'text', text: '当前账号没有新增到货通知的权限，请联系管理员开通「到货通知-新增」。' })
        return
      }
      const itemRes = await this.get('/mes/md/mditem/list', { enableFlag: 'Y' })
      const items = (itemRes.rows || []).filter(i => i.enableFlag !== 'N')
      const parsed = this.parseArrivalLines(q, items)
      const nameOf = list => list.map(l => l.itemName).join('、')
      if (!parsed.hits) {
        const raw = this.materialMention(q)
        // 只说了“添加一条记录”时，不要把动词残片当成物料名展示
        const mention = raw && !/(添加|新增|创建|登记|补录|记录|单据|一条|一个)/.test(raw) ? raw : ''
        const grams = []
        for (let i = 0; i + 1 < mention.length; i++) grams.push(mention.slice(i, i + 2))
        const near = items.filter(i => i.itemName && grams.some(g => i.itemName.indexOf(g) > -1)).slice(0, 4)
        this.push({
          role: 'bot', type: 'ask',
          text: mention
            ? `没有找到与「${mention}」匹配的物料，请按“物料 + 数量”说明，例如“帮我新增到货通知：10 个上壳、10 个电池”。`
            : '还没有说明到货物资和数量。请按“物料 + 数量”告诉我，例如“帮我新增到货通知：10 个上壳、10 个电池”。',
          options: near.map(i => `帮我新增到货通知：10 ${i.unitName || '个'}${i.itemName}`)
        })
        return
      }
      if (parsed.missing.length) {
        this.push({
          role: 'bot', type: 'ask',
          text: `已识别到 ${nameOf(parsed.lines.concat(parsed.missing))}，但「${nameOf(parsed.missing)}」没有说明到货数量，请补充数量后再创建。`,
          options: parsed.missing.map(l => `帮我新增到货通知：10 ${l.unitName}${l.itemName}`)
        })
        return
      }
      const vendorRes = await this.get('/mes/md/vendor/list', { enableFlag: 'Y' })
      const vendors = (vendorRes.rows || []).filter(v => v.enableFlag !== 'N')
      const vendor = this.matchVendor(q, vendors)
      this.push({
        role: 'bot', type: 'arrival',
        summary: `将新增一张到货通知草稿，共 ${parsed.lines.length} 行：${parsed.lines.map(l => `${l.itemName} ${l.quantityArrival} ${l.unitName}`).join('、')}。请确认供应商、到货日期和是否来料检验，点“确认创建”后才写入数据库。`,
        lines: parsed.lines,
        vendors,
        vendorId: vendor ? vendor.vendorId : null,
        arrivalDate: this.parseArrivalDate(q),
        iqcCheck: 'N',
        busy: false, finished: false, error: '', result: '', noticeId: null, noticeCode: ''
      })
    },
    cancelArrival(m) {
      if (m.busy || m.finished) return
      m.finished = true
      m.result = '已取消，未创建到货通知。'
    },
    /** 确认后：先生成单号并创建主单，再逐行写入物料明细；中途失败会回滚空主单 */
    async confirmArrival(m) {
      if (m.busy || m.finished) return
      if (!this.$auth.hasPermi('mes:wm:arrivalnotice:add')) { m.error = '当前账号没有新增到货通知的权限。'; return }
      const vendor = (m.vendors || []).find(v => String(v.vendorId) === String(m.vendorId))
      if (!vendor) { m.error = '请先选择供应商。'; return }
      if (!m.arrivalDate || !/^\d{4}-\d{2}-\d{2}$/.test(m.arrivalDate)) { m.error = '请选择到货日期。'; return }
      if (!m.lines.length) { m.error = '没有可写入的物料明细。'; return }
      m.error = ''
      m.busy = true
      let noticeId = null
      let code = ''
      let posting = false
      try {
        const gen = await request({ url: '/system/autocode/get/ARRIVALNOTICE_CODE', method: 'get' })
        if (typeof gen !== 'string' || !gen.trim()) throw new Error('无法生成到货通知单号，请稍后重试')
        code = gen.trim()
        posting = true
        const header = await request({ url: '/mes/wm/arrivalnotice', method: 'post', data: {
          noticeCode: code,
          noticeName: ('供应商' + vendor.vendorName + '到货通知单').slice(0, 100),
          vendorId: vendor.vendorId, vendorCode: vendor.vendorCode, vendorName: vendor.vendorName, vendorNick: vendor.vendorNick,
          arrivalDate: m.arrivalDate, status: 'PREPARE', remark: '由 MES 智能助手创建'
        } })
        if (!header || header.code !== 200) throw new Error((header && header.msg) || '到货通知主单创建失败')
        // 新增接口只返回影响行数，用单号回查 noticeId 再写明细
        const found = await this.get('/mes/wm/arrivalnotice/list', { noticeCode: code })
        const notice = (found.rows || []).find(r => r.noticeCode === code)
        if (!notice) throw new Error(`主单已提交，但未取回单号 ${code} 的记录，请到到货通知列表核对后再补录明细`)
        noticeId = notice.noticeId
        for (const line of m.lines) {
          const res = await request({ url: '/mes/wm/arrivalnoticeline', method: 'post', data: {
            noticeId: noticeId, itemId: line.itemId, itemCode: line.itemCode, itemName: line.itemName,
            specification: line.specification, unitOfMeasure: line.unitOfMeasure, unitName: line.unitName,
            quantityArrival: line.quantityArrival, iqcCheck: m.iqcCheck
          } })
          if (!res || res.code !== 200) throw new Error(`${line.itemName} 明细保存失败：${(res && res.msg) || '未知错误'}`)
        }
        m.noticeId = noticeId
        m.noticeCode = code
        m.finished = true
        m.result = `到货通知单 ${code} 已创建（草稿），共 ${m.lines.length} 行：${m.lines.map(l => `${l.itemName} ${l.quantityArrival} ${l.unitName}`).join('、')}。可到「到货通知」补全联系方式后提交${m.iqcCheck === 'Y' ? '，提交后按需生成来料检验草稿' : ''}。`
      } catch (e) {
        const reason = e && e.message ? e.message : String(e || '')
        if (noticeId) {
          try {
            await request({ url: '/mes/wm/arrivalnotice/' + noticeId, method: 'delete' })
            m.error = `创建未完成（${reason || '服务异常'}），已回滚刚生成的草稿，未留下空单据，可修改后重试。`
          } catch (e2) {
            m.finished = true
            m.noticeId = noticeId
            m.noticeCode = code
            m.error = `明细未全部写入（${reason || '服务异常'}）。单据 ${code} 已存在但只有部分明细，请到「到货通知」核对并补录，避免重复创建。`
          }
        } else {
          m.error = posting
            ? `创建请求未能确认结果（${reason || '服务异常'}），请先到「到货通知」按单号核对，避免重复创建。`
            : `创建未开始：${reason || '服务异常'}，请稍后重试。`
        }
      } finally { m.busy = false }
    },
    /** 只使用结构化业务消息恢复单据指代；普通文字和模型回答不能充当单据凭据。 */
    async handleArrivalFollowup(q) {
      const text = String(q || '').trim()
      const named = /到货通知|到货单|收货通知/.test(text)
      const wantsReceipt = /采购入库|原料入库|原材料入库/.test(text) && !/执行|过账/.test(text)
      // “刚刚创建好的”是指代定语；“新增……需要检验”仍是新的创建请求。
      const actionText = text.replace(/(?:新增|添加|新建|创建|录入|登记|补录)(?:好|完成)?的/g, '')
      if (this.matchAction(actionText) === 'arrival') return false
      if (!named && /工单|入库|出库|领料|采购单|检验单/.test(text) && !wantsReceipt) return false
      const reference = /刚刚|刚才|刚建|刚创|刚加|上一[张笔次个]|上次|之前|前面|这[张个笔]|那[张个笔]|这单|那单|它|该单|另一[张个笔]/.test(text)
      const operation = /检验|质检|送检|检查|核对|查看|看看|看下|看一下|打开|详情|状态|提交|修改|编辑|删除|取消/.test(text)
      const code = text.match(/\bAN[A-Za-z0-9_-]*\d[A-Za-z0-9_-]*\b/i)
        || text.match(/(?:到货通知(?:单)?|到货单|收货通知|单号)\s*[:：]?\s*([A-Za-z][A-Za-z0-9_-]*\d[A-Za-z0-9_-]*)/)
      const noticeCode = code ? (code[1] || code[0]).toUpperCase() : ''
      // 完整的新任务及统计查询照常解析，不能被旧单据吸走。
      if (/库存|盘点|合格率|统计|汇总|流程|步骤|怎么|如何|为什么/.test(text)) return false
      if (!operation && !(named && reference) && !noticeCode && !(wantsReceipt && reference)) return false
      const history = this.messages.filter(m => m.role === 'bot')
      const latest = history[history.length - 1]
      const continuation = /^(?:(?:请|帮我|给我|让我|麻烦你?|那就|接着|继续|再)\s*)*(?:检验|质检|送检|检查|核对|查看|看看|看下|看一下|打开)(?:一(?:下下?|遍)|下)?(?:吧|好吗|呢)?[。？?！!]*$/.test(text)
      if (!named && !noticeCode && !((reference || continuation) && latest && /^(arrival|arrivalDetail|arrivalReceipt)$/.test(latest.type))) return false

      const candidates = []
      history.slice().reverse().forEach(m => {
        const doc = /^(arrivalDetail|arrivalReceipt)$/.test(m.type) ? m.notice : m.type === 'arrival' ? m : null
        if (doc && (!doc.noticeId || !candidates.some(d => String(d.noticeId) === String(doc.noticeId)))) candidates.push(doc)
      })
      let target
      if (noticeCode) target = { noticeCode }
      else if (/另一[张个笔]|之前|前面|上一[张笔次个]|上次/.test(text) || (!reference && candidates.length > 1)) {
        target = null
      } else target = candidates[0]
      if (!target) {
        this.push({ role: 'bot', type: 'ask', text: candidates.length ? '请确认要查看或检验哪一张到货通知，选择单号后继续。' : '当前对话没有可确定的到货通知，请提供完整单号，例如“查看到货通知 AN20261031003”。',
          options: candidates.filter(d => d.noticeCode).slice(0, 6).map(d => `查看到货通知 ${d.noticeCode} 的检验状态`) })
        return true
      }
      if (!target.noticeId && !target.noticeCode) {
        this.push({ role: 'bot', type: 'text', text: target.finished ? '刚才的到货通知尚未创建或已取消；请先核对创建结果，或提供已存在的单号。' : '刚才的到货通知还在等待确认，请在上一张草稿卡片中补齐信息并点击“确认创建”，创建后再进行送检。' })
        return true
      }
      if (!this.$auth.hasPermi('mes:wm:arrivalnotice:query') || !this.$auth.hasPermi('mes:wm:arrivalnotice:list')) {
        this.push({ role: 'bot', type: 'text', text: '已识别到你指的是到货通知，但当前账号缺少到货通知查询权限，请联系管理员。' })
        return true
      }
      this.waitingWeatherCity = false
      this.lastStock = null
      this.lastWorkorder = null
      let notice
      if (target.noticeId) {
        const res = await request({ url: '/mes/wm/arrivalnotice/' + encodeURIComponent(target.noticeId), method: 'get' })
        notice = res.data
        if (notice && (String(notice.noticeId) !== String(target.noticeId) || notice.noticeCode !== target.noticeCode)) notice = null
      } else {
        const res = await this.get('/mes/wm/arrivalnotice/list', { noticeCode: target.noticeCode })
        const exact = (res.rows || []).filter(d => String(d.noticeCode || '').toUpperCase() === target.noticeCode)
        if (exact.length === 1) notice = exact[0]
      }
      if (!notice || !notice.noticeId) {
        this.push({ role: 'bot', type: 'text', text: `未找到到货通知 ${target.noticeCode}，单据可能已不存在或当前账号不可见，请核对单号。` })
        return true
      }
      if (wantsReceipt) {
        if (!this.$auth.hasPermi('mes:wm:itemrecpt:list')) {
          this.push({ role: 'bot', type: 'text', text: '当前账号缺少采购入库单查询权限。' })
          return true
        }
        const result = await this.get('/mes/wm/itemrecpt/list', { noticeId: notice.noticeId })
        const receipts = (result.rows || []).filter(r => String(r.noticeId) === String(notice.noticeId))
        const nextStep = receipts.length ? `已找到 ${receipts.length} 张关联采购入库单；请核对明细及状态后办理入库。` : '未找到关联采购入库单。需检行全部完成且有可入库的合格数量后才会生成草稿；请核对到货明细与检验状态。'
        this.push({ role: 'bot', type: 'arrivalReceipt', notice, receipts, nextStep })
        return true
      }
      const res = await this.get('/mes/wm/arrivalnoticeline/list', { noticeId: notice.noticeId })
      const lines = (res.rows || []).filter(l => String(l.noticeId) === String(notice.noticeId))
      const canInspect = this.$auth.hasPermi('mes:qc:iqc:list')
      let inspections = []
      if (canInspect) {
        const iqc = await this.get('/mes/qc/iqc/list', { sourceDocType: 'ARRIVAL_NOTICE', sourceDocId: notice.noticeId })
        inspections = (iqc.rows || []).filter(i => i.sourceDocType === 'ARRIVAL_NOTICE' && String(i.sourceDocId) === String(notice.noticeId))
      }
      const needCheck = lines.some(l => l.iqcCheck === 'Y')
      let nextStep
      if (notice.status === 'PREPARE') {
        nextStep = needCheck
          ? '这张到货通知仍是草稿。请打开原单核对联系方式和明细并提交，提交后会为需检行生成来料检验草稿，再填写检验结果。'
          : '这张到货通知仍是草稿，当前没有需检行（已设置的行采用免检）。如要送检，请先在原单中把对应行改为“需要来料检验”，核对信息并提交。'
      } else if (inspections.length) {
        nextStep = `已找到这张到货通知关联的 ${inspections.length} 张来料检验单，可打开对应检验单核对状态或填写结果。`
      } else {
        nextStep = canInspect
          ? '当前未找到这张到货通知关联的来料检验单，请打开原单核对检验标志和提交状态。'
          : '当前账号没有来料检验查询权限，无法核对关联检验单，请联系管理员。'
      }
      if (!lines.length) nextStep = '这张到货通知没有查到物料明细，请先打开原单核对并补齐明细，再进行送检。'
      const statuses = { PREPARE: '草稿', APPROVING: '待检验', APPROVED: '待入库', FINISHED: '已完成', CANCELED: '已取消' }
      this.push({ role: 'bot', type: 'arrivalDetail', notice, lines, inspections, nextStep, statusLabel: statuses[notice.status] || notice.status || '未知状态',
        source: `数据来源：到货通知 ${notice.noticeCode} 及其明细${canInspect ? '、关联来料检验单' : ''}（实时查询）` })
      return true
    },
    inspectionStatus(status) {
      return ({ PREPARE: '草稿', CONFIRMED: '已确认', APPROVING: '检验中', FINISHED: '已完成' })[status] || status || '未知状态'
    },
    openArrivalInspection(iqc) {
      if (!iqc.iqcId || !this.$auth.hasPermi('mes:qc:iqc:query')) return
      return this.$router.push({ path: '/mes/qc/iqc', query: { iqcCode: iqc.iqcCode, openIqcId: String(iqc.iqcId) } })
    },
    openArrivalReceipt(receipt) {
      if (!receipt.recptId || !this.$auth.hasPermi('mes:wm:itemrecpt:query')) return
      return this.$router.push({ path: '/mes/wm/itemrecpt', query: { recptCode: receipt.recptCode, openRecptId: String(receipt.recptId) } })
    },
    openArrivalNotice(m, edit = false) {
      if (!m.noticeCode) return
      return this.$router.push({ path: '/mes/wm/arrivalnotice', query: { noticeCode: m.noticeCode, openNoticeId: m.noticeId ? String(m.noticeId) : undefined, edit: edit ? '1' : undefined } })
    },

    /* ---------- ① 盘点：期初 / 入库 / 出库 / 期末 + 批次明细 ---------- */
    async queryStock(q) {
      this.lastWorkorder = null
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
        source: `数据来源：库存现有量及可查询库存流水，${t.from} ~ ${t.to}；历史结存根据期后流水回推，依赖流水完整；按仓库与批次核对，单位：${item.unitName || item.unitOfMeasure || '同物料单位'}` })
      const warehouseNames = [...(tx.rows || []), ...(stock.rows || [])].reduce((all, r) => all.concat(r.warehouseName || [], r.warehouseCode || []), [])
      const warehouseName = warehouseNames.sort((a, b) => b.length - a.length).find(name => q.includes(name)) || ''
      this.lastStock = { itemCode: item.itemCode, time: `${t.from}至${t.to}`, warehouse: warehouseName, warehouses: [...new Set(warehouseNames)] }
      const net = round(inQty - outQty)
      const trend = net > 0 ? `期间净增加 ${net}` : net < 0 ? `期间净减少 ${Math.abs(net)}` : '期间入出库数量相同'
      this.push({ role: 'bot', type: 'text', text: `${item.itemName}在${t.label}的期末库存为 ${round(endQty)} ${item.unitName || item.unitOfMeasure || ''}；期间入库 ${round(inQty)}，出库 ${round(outQty)}，${trend}。各仓库与批次的明细见上表；历史结果依赖流水完整。` })
    },

    async findAndOpenWorkorder(code) {
      if (!this.$auth.hasPermi('mes:pro:workorder:query')) {
        this.push({ role: 'bot', type: 'text', text: '当前账号没有查看工单详情的权限。' }); return
      }
      const result = await this.get('/mes/pro/workorder/list', { workorderCode: code })
      const found = (result.rows || []).find(r => String(r.workorderCode).toUpperCase() === code.toUpperCase())
      if (!found) { this.push({ role: 'bot', type: 'text', text: `未找到工单 ${code}，请检查编号。` }); return }
      this.push({ role: 'bot', type: 'text', text: `已找到工单 ${found.workorderCode}，正在打开详情。` })
      return this.openWorkorder(found.workorderId)
    },
    openWorkorder(id) {
      return this.$router.push({ path: '/mes/pro/workorder', query: { openWorkorderId: String(id) } })
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
      this.lastWorkorder = null
      this.lastStock = null
      const t = this.parseTime(q)
      const { item, items, rows: allItems, fuzzy } = await this.matchItem(q)
      const selected = items || (item ? [item] : [])
      const dirText = dir === 'IN' ? '入库' : '出库'
      const doc = q.match(/(?:单号|单据|采购单|出库单|入库单)[：:\s]*([A-Za-z][A-Za-z0-9_-]*\d[A-Za-z0-9_-]*)/)
      // 点名了物料却匹配不到时，绝不能把“全部流水”当成该物料的结果返回（追问，绝不编造）
      if (!selected.length && !doc) {
        const mention = this.materialMention(q)
        if (mention) {
          const grams = []
          for (let i = 0; i + 1 < mention.length; i++) grams.push(mention.slice(i, i + 2))
          const near = (allItems || []).filter(i => i.itemName && grams.some(g => i.itemName.indexOf(g) > -1))
          const candidates = (near.length ? near : (fuzzy && fuzzy.length ? fuzzy : allItems || [])).slice(0, 4)
          const options = candidates.map(i => `${t.label} ${i.itemName} ${dirText}情况`)
          options.push(`查看${t.label}全部${dirText}`)
          this.push({
            role: 'bot', type: 'ask',
            text: `没有找到与「${mention}」匹配的物料，因此不会用全部${dirText}单据代替它的结果。你是指下面哪一种？`,
            options
          })
          return
        }
      }
      const params = { params: { beginTime: t.from + ' 00:00:00', endTime: t.to + ' 23:59:59' } }
      if (selected.length === 1) params.itemCode = selected[0].itemCode
      if (doc) params.sourceDocCode = doc[1]
      const tx = await this.get('/mes/wm/transaction/list', params)
      const warehouse = this.warehouseFilter(q, tx.rows || [])
      const types = /采购|到货|收货/.test(q) ? ['ITEM_RECPT'] : /领料/.test(q) ? ['ITEM_ISSUE_OUT'] : /销售|发货/.test(q) ? ['PRODUCT_SALES', 'PRODUCT_SALSE'] : null
      const rows = (tx.rows || []).filter(warehouse).filter(r => {
        if (selected.length && !selected.some(i => i.itemCode === r.itemCode)) return false
        if (types && !types.includes(r.transactionType) && !types.includes(r.sourceDocType)) return false
        const qty = Number(r.transactionQuantity || 0)
        return dir === 'IN' ? qty > 0 : qty < 0
      })
      const byItem = {}
      rows.forEach(r => {
        const k = [r.itemCode, r.warehouseCode || r.warehouseId || r.warehouseName || ''].join('|')
        const product = (allItems || []).find(i => i.itemCode === r.itemCode)
        if (!byItem[k]) byItem[k] = { itemCode: r.itemCode, itemName: r.itemName, warehouseName: r.warehouseName || '未标注', unit: r.unitName || (product && (product.unitName || product.unitOfMeasure)) || '未标注', qty: 0, cnt: 0 }
        byItem[k].qty += Math.abs(Number(r.transactionQuantity || 0))
        byItem[k].cnt += 1
      })
      const list = Object.keys(byItem).map(k => Object.assign({}, byItem[k], { qty: Math.round(byItem[k].qty * 100) / 100 })).sort((a, b) => b.qty - a.qty)
      this.push({
        role: 'bot', type: 'list',
        title: `${t.label}${selected.length ? ' · ' + selected.map(i => i.itemName).join('、') : ' · 全部物料'}${dir === 'IN' ? '入库' : '出库'}汇总`,
        columns: [
          { prop: 'itemCode', label: '物料编码', width: 130 },
          { prop: 'itemName', label: '物料名称' },
          { prop: 'warehouseName', label: '仓库', width: 120 },
          { prop: 'qty', label: dir === 'IN' ? '入库数' : '出库数', width: 100, align: 'right' },
          { prop: 'unit', label: '单位', width: 80 },
          { prop: 'cnt', label: '流水条数', width: 90, align: 'right' }
        ],
        rows: list,
        source: `数据来源：wm_transaction ${t.from} ~ ${t.to}，共 ${rows.length} 条${dir === 'IN' ? '入库' : '出库'}流水`
      })
      const itemCount = new Set(list.map(r => r.itemCode)).size
      const warehouseCount = new Set(list.map(r => r.warehouseName).filter(name => name !== '未标注')).size
      const unknownWarehouse = list.some(r => r.warehouseName === '未标注') ? '，部分记录未标注仓库' : ''
      this.push({ role: 'bot', type: 'text', text: `${t.label}${selected.length ? '' : '（未指定物料，统计全部物料）'}共找到 ${rows.length} 条${dir === 'IN' ? '入库' : '出库'}流水，涉及 ${itemCount} 种物料、${warehouseCount} 个已标注仓库${unknownWarehouse}。各仓库数量和单位见上表，不同物料或单位不直接相加。` })
    },

    /* ---------- ③ 工单进度 ---------- */
    async queryWorkorder(q = '') {
      this.lastWorkorder = null
      this.lastStock = null
      // 兼容“MO05246601 工单现在是什么状态”这类编号在前的问法：无关键字时直接识别独立工单号
      const doc = q.match(/(?:工单号?|单号)[：:\s]*([A-Za-z][A-Za-z0-9_-]*\d[A-Za-z0-9_-]*)/)
        || q.match(/(MO\d{6,}|[A-Z]{2,}\d{6,})/i)
      const res = await this.get('/mes/pro/workorder/list', doc ? { workorderCode: doc[1] } : {})
      const range = /\d\s*月|20\d{2}-\d{1,2}-\d{1,2}|本月|上月|这个月|上个月|近\s*\d+\s*天/.test(q) ? this.parseTime(q) : null
      let rows = (res.rows || []).filter(r => (!doc || r.workorderCode === doc[1]) && (!range || (String(r.createTime || '').slice(0, 10) >= range.from && String(r.createTime || '').slice(0, 10) <= range.to)))
      const today = new Date().setHours(0, 0, 0, 0)
      const overdueOnly = !doc && /落后|超期|逾期|延期/.test(q)
      if (overdueOnly) {
        rows = rows.filter(r => r.status !== 'FINISHED' && r.status !== 'CANCELED' && r.requestDate && new Date(String(r.requestDate).replace(/-/g, '/')).getTime() < today)
      }
      if (doc && rows.length === 0) {
        this.push({
          role: 'bot', type: 'text',
          text: `未找到工单 ${doc[1]}，请确认工单号是否正确（当前账号可查询的工单范围内没有该编号）。`
        })
        return
      }
      const list = rows.map(r => {
        const plan = Number(r.quantity || 0), done = Number(r.quantityProduced || 0)
        const percent = plan > 0 ? Math.min(100, Math.round(done / plan * 100)) : 0
        const req = r.requestDate ? new Date(String(r.requestDate).replace(/-/g, '/')).getTime() : 0
        const over = plan > 0 && done > plan ? done - plan : 0
        const overText = plan > 0 && done > plan
          ? `超产 ${over}（${Math.round(over / plan * 100)}%）`
          : ''
        return {
          workorderCode: r.workorderCode, productName: r.productName,
          plan, done, percent, requestDate: r.requestDate ? String(r.requestDate).slice(0, 10) : '未设置',
          overText,
          lag: !req || Number.isNaN(req) ? '未设置交期' : r.status !== 'FINISHED' && r.status !== 'CANCELED' && req < today ? '已超期' : '正常'
        }
      }).sort((a, b) => a.percent - b.percent).slice(0, 10)
      this.push({
        role: 'bot', type: 'list',
        title: doc ? `工单 ${doc[1]} 进度` : overdueOnly ? '已超期未完成工单（最多 10 张）' : '工单进度（按完成比例升序，最多 10 张）',
        workorderId: doc && rows.length === 1 ? rows[0].workorderId : null,
        columns: [
          { prop: 'workorderCode', label: '工单号', width: 150 },
          { prop: 'productName', label: '产品' },
          { prop: 'plan', label: '计划', width: 80, align: 'right' },
          { prop: 'done', label: '已产', width: 80, align: 'right' },
          { prop: 'percent', label: '进度%', width: 80, align: 'right' },
          { prop: 'overText', label: '超产', width: 120 },
          { prop: 'requestDate', label: '需求日期', width: 110 },
          { prop: 'lag', label: '交期状态', width: 100 }
        ],
        rows: list,
        source: `数据来源：pro_workorder，共 ${rows.length} 张工单`
      })
      if (doc && rows.length === 1) {
        const w = rows[0]
        this.lastWorkorder = w.workorderCode
        const statuses = { PREPARE: '草稿', CONFIRMED: '已确认', FINISHED: '已完成', CANCELED: '已取消' }
        this.push({ role: 'bot', type: 'text', text: `工单 ${w.workorderCode} 的状态为 ${statuses[w.status] || w.status || '未知'}，计划生产 ${w.quantity || 0}，已生产 ${w.quantityProduced || 0}。你可以说“打开它”查看详情。` })
      } else if (overdueOnly) {
        const shown = list.map(r => r.workorderCode).join('、')
        this.push({ role: 'bot', type: 'text', text: rows.length
          ? `截至今天，在当前账号可查询范围内，共有 ${rows.length} 张工单已超过需求日期且尚未完成或取消。${rows.length > list.length ? `表格展示其中 ${list.length} 张，包括` : '涉及'}${shown}。建议优先核对这些工单的需求日期与生产进度。`
          : '截至今天，在当前账号可查询范围内，没有查到超过需求日期且未完成或取消的工单。未设置需求日期的工单无法判断是否超期。' })
      } else if (!doc) {
        this.push({ role: 'bot', type: 'text', text: `当前查询到 ${rows.length} 张工单，表格按完成比例从低到高展示${rows.length > list.length ? `前 ${list.length} 张` : ` ${list.length} 张`}。可根据需求日期和交期状态进一步核对。` })
      }
    },

    /* ---------- ④ 安全库存 ---------- */
    async queryAlert() {
      this.lastWorkorder = null
      this.lastStock = null
      const [items, stock] = await Promise.all([this.get('/mes/md/mditem/list'), this.get('/mes/wm/wmstock/list')])
      const stockMap = {}
      ;(stock.rows || []).forEach(r => { stockMap[r.itemCode] = (stockMap[r.itemCode] || 0) + Number(r.quantityOnhand || 0) })
      const rows = (items.rows || [])
        .filter(i => Number(i.minStock || 0) > 0)
        .map(i => {
          const cur = Math.round((stockMap[i.itemCode] || 0) * 100) / 100
          return { itemCode: i.itemCode, itemName: i.itemName, unit: i.unitName || i.unitOfMeasure || '未标注', current: cur, min: Number(i.minStock), gap: Math.round((cur - Number(i.minStock)) * 100) / 100, suggest: Math.max(0, Math.round((Number(i.minStock) * 2 - cur) * 100) / 100) }
        })
        .filter(a => a.gap < 0)
        .sort((a, b) => a.gap - b.gap)
      this.push({
        role: 'bot', type: 'list',
        title: `低于安全库存的物料（${rows.length} 种）`,
        columns: [
          { prop: 'itemCode', label: '物料编码', width: 130 },
          { prop: 'itemName', label: '物料名称' },
          { prop: 'unit', label: '单位', width: 80 },
          { prop: 'current', label: '结存', width: 80, align: 'right' },
          { prop: 'min', label: '安全库存', width: 90, align: 'right' },
          { prop: 'gap', label: '缺口', width: 80, align: 'right' },
          { prop: 'suggest', label: '建议补货', width: 90, align: 'right' }
        ],
        rows,
        source: '数据来源：md_item.min_stock + wm_material_stock 实时结存'
      })
      this.push({ role: 'bot', type: 'text', text: rows.length ? `目前有 ${rows.length} 种物料低于安全库存，具体缺口和建议补货量见上表。` : '目前没有发现低于安全库存的物料。' })
    },

    /* ---------- ⑤ 检验合格率 ---------- */
    async queryInspection(q) {
      this.lastWorkorder = null
      this.lastStock = null
      const t = this.parseTime(q)
      const res = await this.get('/mes/qc/ipqc/list')
      const rows = (res.rows || []).filter(r => {
        const d = String(r.inspectDate || '').slice(0, 10)
        return r.status === 'FINISHED' && d >= t.from && d <= t.to
      })
      const groups = {}
      rows.forEach(r => {
        const unit = r.unitName || r.unitOfMeasure || ''
        const key = unit || `未标注单位（${r.itemCode || r.ipqcCode || '未知物料'}）`
        if (!groups[key]) groups[key] = { total: 0, bad: 0 }
        groups[key].total += Number(r.quantityCheck || 0)
        groups[key].bad += Number(r.quantityUnqualified || 0)
      })
      const summary = Object.keys(groups).map(unit => {
        const { total, bad } = groups[unit]
        const rate = total > 0 ? Math.round((total - bad) / total * 1000) / 10 + '%' : '暂无'
        return `${unit}：检验 ${total}，不合格 ${bad}，合格率 ${rate}`
      }).join('；')
      this.push({
        role: 'bot', type: 'list',
        title: `${t.label}过程检验明细（按单位分别统计）`,
        columns: [
          { prop: 'ipqcCode', label: '检验单', width: 140 },
          { prop: 'processName', label: '工序' },
          { prop: 'itemName', label: '物料' },
          { prop: 'unitName', label: '单位', width: 80 },
          { prop: 'quantityCheck', label: '检验数', width: 90, align: 'right' },
          { prop: 'quantityUnqualified', label: '不合格', width: 90, align: 'right' },
          { prop: 'status', label: '状态', width: 90 }
        ],
        rows: rows.slice(0, 20),
        source: `数据来源：qc_ipqc ${t.from} ~ ${t.to}，共 ${rows.length} 张检验单`
      })
      this.push({ role: 'bot', type: 'text', text: rows.length ? `${t.label}过程检验共 ${rows.length} 张，按单位分别统计：${summary}。` : `${t.label}没有可统计的过程检验记录。` })
    },

    /* ---------- ⑥ 业务规则问答：来料检验要检查什么 ---------- */
    async queryInspectionItems(q) {
      this.lastStock = null
      this.lastWorkorder = null
      const { item, items, fuzzy } = await this.matchBusinessItem(q)
      if (!item) {
        const candidates = [(items || []), (fuzzy || [])].find(list => list.length) || []
        if (candidates.length) {
          this.push({
            role: 'bot', type: 'ask',
            text: '匹配到多个物料，请明确要查哪一个的检验项目（不同物料的检测项不能混用）：',
            options: candidates.slice(0, 4).map(i => `${i.itemName} 来料检验要检查哪些项目`)
          })
          return
        }
        this.push({
          role: 'bot', type: 'text',
          text: `没有找到与「${this.materialMention(q) || '该物料'}」匹配的物料，因此不会用全部物料的检测项冒充它的结果。请改用物料编码或更完整的名称再问，例如「红外传感器模块 来料检验要检查哪些项目」。`
        })
        return
      }
      // 来料检验单能提供"最近实际检验了什么"，缺权限时退化为只读质检方案。
      let lastIqc = null
      let lastIqcLines = []
      if (this.$auth.hasPermi('mes:qc:iqc:list')) {
        const iqc = await this.get('/mes/qc/iqc/list', { itemId: item.itemId })
        const finished = (iqc.rows || [])
          .filter(row => row.status === 'FINISHED')
          .sort((left, right) => String(right.inspectDate || '').localeCompare(String(left.inspectDate || '')))
        lastIqc = finished[0] || null
        if (lastIqc) {
          const lines = await this.get('/mes/qc/iqcline/list', { iqcId: lastIqc.iqcId })
          lastIqcLines = lines.rows || []
        }
      }
      const products = await this.get('/mes/qc/templateproduct/list', { itemId: item.itemId })
      const productRows = products.rows || []
      const templateId = (lastIqc && lastIqc.templateId) || (productRows[0] && productRows[0].templateId) || null
      let template = null
      let product = null
      let indexes = []
      if (templateId) {
        const [templates, indexRows] = await Promise.all([
          this.get('/mes/qc/qctemplate/list', { templateId }),
          this.get('/mes/qc/templateindex/list', { templateId })
        ])
        template = (templates.rows || []).find(row => String(row.templateId) === String(templateId)) || null
        product = productRows.find(row => String(row.templateId) === String(templateId)) || null
        indexes = indexRows.rows || []
      }
      const answer = buildInspectionAnswer({ item, template, product, indexes, lastIqc, lastIqcLines })
      this.push({ role: 'bot', type: 'text', text: answer.text })
      if (answer.rows.length) {
        this.push({ role: 'bot', type: 'list', title: answer.title, columns: answer.columns, rows: answer.rows, source: answer.source })
      }
    },

    /* ---------- ⑦ 业务规则问答：不合格物料如何处理 ---------- */
    async queryUnqualifiedHandling() {
      this.lastStock = null
      this.lastWorkorder = null
      const canReadIqc = this.$auth.hasPermi('mes:qc:iqc:list')
      const canReadReturns = this.$auth.hasPermi('mes:wm:rtvendor:list')
      let iqcRows = []
      if (canReadIqc) {
        const iqc = await this.get('/mes/qc/iqc/list')
        iqcRows = (iqc.rows || []).filter(row => row.status === 'FINISHED')
      }
      let returnRows = []
      if (canReadReturns) {
        const rtv = await this.get('/mes/wm/rtvendor/list')
        // 只看与不合格处置相关的退供：未入库退供（attr2=PRE_STOCK）或名称含"不合格"的退供单。
        returnRows = (rtv.rows || []).filter(row => row.attr2 === 'PRE_STOCK' || /不合格/.test(String(row.rtName || '')))
      }
      const answer = buildUnqualifiedAnswer({ iqcRows, returnRows, iqcRowsRead: canReadIqc, returnRowsRead: canReadReturns })
      this.push({ role: 'bot', type: 'text', text: answer.text })
      if (answer.rows.length) {
        this.push({ role: 'bot', type: 'list', title: answer.title, columns: answer.columns, rows: answer.rows, source: answer.source })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.ai__mode { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; margin: 0 0 14px; }
.ai__mode > span { color: #6b7280; font-size: 12px; }
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
  &__form {
    display: flex;
    flex-wrap: wrap;
    gap: 12px 18px;
    margin: 10px 0 6px;
  }
  &__form-item {
    display: flex;
    align-items: center;
    gap: 6px;
  }
  &__form-label {
    font-size: 12px;
    color: #4b5563;
  }
  &__ok { color: #0f7a3d; }
  &__bad { color: #b91c1c; }

  /* 对话卡片里的表格按内容收缩：全局 .el-table 的 min-height 是为列表页防抖动的 */
  &__table {
    min-height: 0;
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

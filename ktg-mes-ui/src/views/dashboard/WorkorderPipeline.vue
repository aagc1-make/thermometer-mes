<template>
  <el-card class="wo-pl" shadow="never">
    <div slot="header" class="wo-pl__header">
      <div class="wo-pl__title">
        <span class="wo-pl__bar"></span>
        <span class="wo-pl__title-text">工单流水线</span>
        <span class="wo-pl__sub">以工单为核心 · {{ stepDefs.length }} 个环节</span>
      </div>

      <div class="wo-pl__summary">
        <template v-if="hasWorkorder && steps.length && !errorMsg">
          <span class="wo-pl__chip">
            当前第 <b>{{ currentIndex }}</b> / {{ stepDefs.length }} 步
          </span>
          <span class="wo-pl__chip wo-pl__chip--next" v-if="nextStep">
            <i class="wo-pl__dot"></i>
            下一步：<b>{{ nextStep.short }}</b>
            <span v-if="nextStep.action" class="wo-pl__chip-action">{{ nextStep.action }}</span>
          </span>
          <span class="wo-pl__chip wo-pl__chip--ok" v-else>
            <i class="el-icon-circle-check"></i> 工单流程已完成
          </span>
        </template>
        <span class="wo-pl__chip wo-pl__chip--warn" v-if="errorMsg">{{ errorMsg }}</span>
      </div>

      <div class="wo-pl__tools">
        <el-select
          ref="woSelect"
          v-model="selectedId"
          class="wo-pl__select"
          size="small"
          filterable
          remote
          reserve-keyword
          :remote-method="searchWorkorders"
          placeholder="输入工单号 / 日期 / 产品名搜索"
          :loading="loadingWorkorders"
          @change="handleSelect"
          @input.native="onSelectInput"
          @visible-change="onSelectVisibleChange"
        >
          <el-option
            v-for="w in workorders"
            :key="w.workorderId"
            :label="optionLabel(w)"
            :value="w.workorderId"
          >
            <div class="wo-pl__opt">
              <span class="wo-pl__opt-code">{{ w.workorderCode }}</span>
              <span class="wo-pl__opt-date">{{ optionDate(w) }}</span>
              <span class="wo-pl__opt-product">{{ w.productName || w.productCode || '' }}</span>
            </div>
          </el-option>
        </el-select>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="refresh">刷新</el-button>
        <el-button size="small" type="text" @click="detailVisible = !detailVisible">
          {{ detailVisible ? '收起步骤明细' : '展开步骤明细' }}
        </el-button>
      </div>
    </div>

    <div class="wo-pl__empty" v-if="!loadingWorkorders && !hasWorkorders">
      暂无可跟踪的自产工单。先在「生产 → 生产工单」新增、保存并确认工单，再进行生产排产。
    </div>

    <template v-else>
      <div class="wo-pl__flow-note">全链路从「订单」开始：客户下单并确认后，一键生成生产工单与出货检验单，再生成发货通知单；工单确认后先排产，排产生成领料草稿；需检工序报工后先完成 IPQC，再继续生产；工单完工后执行产品入库，最后按工单成品开销售出库单，发货即完成全生命周期。各环节单据按订单/工单链自动取号（前缀不同、日期与流水一致），全局唯一，可在单号上直接对应同一业务链。</div>
      <div class="wo-pl__scroll">
        <div class="wo-pl__grid" v-loading="loading" :style="{ gridTemplateColumns: gridColumns }">
          <div class="wo-pl__cell wo-pl__cell--head wo-pl__cell--wo">
            工单 / 产品
          </div>
          <div
            v-for="(s, i) in steps"
            :key="'h-' + s.key"
            class="wo-pl__cell wo-pl__cell--head"
            :class="{ 'is-current-head': s.state === 'doing' }"
            :title="s.hint"
          >
            <span class="wo-pl__step-no">{{ i + 1 }}</span>
            <span class="wo-pl__step-name">{{ s.short }}</span>
          </div>

          <div class="wo-pl__cell wo-pl__cell--wo wo-pl__cell--body" :title="workorderTip">
            <div class="wo-pl__wo-code">{{ woSafe.workorderCode }}</div>
            <div class="wo-pl__wo-product">{{ woSafe.productName }}</div>
            <el-progress
              :percentage="overallPercent"
              :stroke-width="6"
              :show-text="false"
              :color="overallPercent >= 100 ? '#31946a' : '#5e75ff'"
            />
            <div class="wo-pl__wo-meta">
              计划 {{ woSafe.quantity }}{{ woSafe.unitName || woSafe.unitOfMeasure || '' }} · 累计报工 {{ woSafe.quantityProduced || 0 }}
            </div>
          </div>

          <div
            v-for="s in steps"
            :key="'b-' + s.key"
            class="wo-pl__cell wo-pl__cell--body wo-pl__cell--state"
            :class="'is-' + s.state"
            :title="stepTip(s)"
            @click="go(s)"
          >
            <i class="wo-pl__reddot" v-if="s.state === 'doing'"></i>
            <span class="wo-pl__state-icon">
              <i v-if="s.state === 'done'" class="el-icon-circle-check"></i>
              <i v-else-if="s.state === 'skipped'" class="el-icon-remove-outline"></i>
              <i v-else-if="s.state === 'doing'" class="el-icon-more-outline"></i>
              <i v-else class="el-icon-time"></i>
            </span>
            <span class="wo-pl__state-text">{{ s.stateText }}</span>
            <span v-if="s.docCode" class="wo-pl__state-doc">{{ s.docCode }}</span>
            <span class="wo-pl__state-hint">{{ s.guide || s.hint }}</span>
          </div>
        </div>
      </div>

      <div class="wo-pl__legend">
        <span class="wo-pl__legend-item is-done"><i class="el-icon-circle-check"></i> 已完成</span>
        <span class="wo-pl__legend-item is-doing"><i class="wo-pl__dot"></i> 下一步（点格子可直接跳转）</span>
        <span class="wo-pl__legend-item is-todo"><i class="el-icon-time"></i> 待办理</span>
        <span class="wo-pl__legend-item is-todo"><i class="el-icon-remove-outline"></i> 不适用</span>
        <span class="wo-pl__legend-tip">按本工单实际单据与数量更新，30 秒自动刷新</span>
      </div>

      <div v-if="materialStep && materialStep.shortages.length" class="wo-pl__shortage">
        <i class="el-icon-warning-outline"></i>
        <span>{{ materialStep.text }}</span>
      </div>
      <div class="wo-pl__related">
        <span>补料与后续业务：</span>
        <el-button type="text" size="mini" @click="$router.push('/order/salesorder')">客户订单</el-button>
        <el-button type="text" size="mini" @click="$router.push('/mes/wm/salesnotice')">发货通知</el-button>
        <el-button type="text" size="mini" @click="$router.push('/mes/wm/arrivalnotice')">到货通知</el-button>
        <el-button type="text" size="mini" @click="$router.push('/mes/qc/iqc')">来料检验</el-button>
        <el-button type="text" size="mini" @click="$router.push('/mes/wm/itemrecpt')">采购入库</el-button>
        <el-button type="text" size="mini" @click="$router.push('/mes/wm/productsales')">销售出库</el-button>
        <span>订单号贯穿工单、质检单、发货单；缺料时补料，成品入库后按「工单成品」开销售出库单，发货完成即工单闭环。</span>
      </div>

      <el-table
        v-if="detailVisible"
        class="wo-pl__detail"
        size="mini"
        border
        :data="steps"
        :header-cell-style="{ background: 'rgba(45,49,66,.03)', color: '#6b6f80', fontWeight: 400 }"
      >
        <el-table-column label="#" width="52" align="center">
          <template slot-scope="s">{{ s.$index + 1 }}</template>
        </el-table-column>
        <el-table-column label="步骤" width="170">
          <template slot-scope="s">
            <span class="wo-pl__detail-name">{{ s.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template slot-scope="s">
            <el-tag size="mini" :type="tagType(s.row.state)">{{ s.row.stateText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="单据 / 依据" min-width="200" show-overflow-tooltip>
          <template slot-scope="s">{{ s.row.docCode || '-' }}</template>
        </el-table-column>
        <el-table-column label="说明" min-width="240" show-overflow-tooltip>
          <template slot-scope="s">{{ s.row.text || s.row.hint }}<span v-if="s.row.action" class="wo-pl__detail-action">（{{ s.row.action }}）</span></template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center">
          <template slot-scope="s">
            <el-button type="text" size="mini" :disabled="s.row.state === 'skipped'" @click="go(s.row)">{{ s.row.state === 'done' ? '查看' : '去操作' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </el-card>
</template>

<script>
import request from '@/utils/request'
import { parseTime } from '@/utils/ruoyi'
import { PIPELINE_STEPS, buildWorkorderPipeline, fetchPipelineRows, loadWorkorderPipeline } from '@/utils/workorderPipeline'

const POLL_INTERVAL = 30000

export default {
  name: 'WorkorderPipeline',
  data() {
    return {
      stepDefs: PIPELINE_STEPS,
      loadingWorkorders: false,
      loading: false,
      detailVisible: false,
      errorMsg: '',
      workorders: [],
      selectedId: null,
      // 已选工单的快照：搜索只替换下拉选项，不影响左侧流水线展示
      selectedWorkorder: null,
      hasWorkorders: false,
      searchKeyword: '',
      searchTimer: null,
      // 默认列表与关键字搜索共用一个请求序号，谁后发起谁生效，避免慢请求覆盖新结果
      listSeq: 0,
      steps: [],
      timer: null,
      pipelineRequestId: 0
    }
  },
  computed: {
    workorder() {
      return this.selectedWorkorder
    },
    hasWorkorder() { return !!this.workorder },
    woSafe() { return this.workorder || {} },
    doneCount() { return this.steps.filter(s => s.state === 'done' || s.state === 'skipped').length },
    currentIndex() {
      const index = this.steps.findIndex(s => s.state === 'doing')
      return index < 0 ? this.steps.length : index + 1
    },
    nextStep() { return this.steps.find(s => s.state === 'doing') || null },
    overallPercent() { return this.steps.length ? Math.round(this.doneCount / this.steps.length * 100) : 0 },
    gridColumns() { return ['220px'].concat(this.stepDefs.map(() => 'minmax(112px, 1fr)')).join(' ') },
    materialStep() { return this.steps.find(s => s.key === 'ready') || null },
    workorderTip() {
      const wo = this.woSafe
      if (!wo.workorderCode) return ''
      return [
        `工单 ${wo.workorderCode}${wo.productName ? ' · ' + wo.productName : ''}`,
        `计划 ${wo.quantity || 0}${wo.unitName || wo.unitOfMeasure || ''}，累计报工 ${wo.quantityProduced || 0}`,
        wo.clientName ? `客户：${wo.clientName}` : '无客户订单'
      ].join('\n')
    }
  },
  mounted() {
    this._preferredWorkorderId = this.$route && this.$route.query ? this.$route.query.pipelineWorkorderId : null
    this.$nextTick(this.unlockSelectInput)
    this.loadWorkorders()
    this.startPolling()
  },
  activated() {
    if (this._pipelineInactive) {
      this._pipelineInactive = false
      this.refresh()
      this.startPolling()
    }
  },
  deactivated() {
    this._pipelineInactive = true
    this.stopPolling()
  },
  beforeDestroy() {
    this.pipelineRequestId++
    this.listSeq++
    if (this.searchTimer) window.clearTimeout(this.searchTimer)
    this.searchTimer = null
    this.stopPolling()
  },
  methods: {
    startPolling() {
      this.stopPolling()
      this.timer = window.setInterval(this.refreshQuiet, POLL_INTERVAL)
    },
    stopPolling() {
      if (this.timer) window.clearInterval(this.timer)
      this.timer = null
    },
    unlockSelectInput() {
      // el-select 在下拉收起时会把内部输入框设为 readonly，导致看起来“锁住”。
      // 这里固定其 readonly 为 false，让搜索框任何时候都能输入（与是否有工单无关）。
      const select = this.$refs.woSelect
      if (!select || select._woUnlocked) return
      try {
        Object.defineProperty(select, 'readonly', { configurable: true, get: () => false })
        select._woUnlocked = true
      } catch (e) {
        // 拿不到实例时忽略，不影响原有选择功能
      }
    },
    onSelectInput() {
      // 直接输入（下拉未展开）时也把下拉打开，保证输入与远程搜索同时生效
      const select = this.$refs.woSelect
      if (select && !select.visible) select.visible = true
    },
    onSelectVisibleChange(visible) {
      // 不论是点箭头还是点输入框，只要下拉展开就把光标放进输入框，保证可以立即输入
      if (!visible) return
      this.$nextTick(() => {
        const select = this.$refs.woSelect
        const input = select && select.$el && select.$el.querySelector('input')
        if (input && document.activeElement !== input) input.focus()
      })
    },
    optionLabel(w) {
      const status = { PREPARE: '待确认', CONFIRMED: '已确认', FINISHED: '已完工' }[w.status] || w.status || ''
      return `${w.workorderCode} · ${w.productName || w.productCode || ''}（${status}）`
    },
    optionDate(w) {
      return w && w.requestDate ? parseTime(w.requestDate, '{y}-{m}-{d}') : ''
    },
    isPickable(w) {
      return ['PREPARE', 'CONFIRMED', 'FINISHED'].includes(w.status) && (!w.workorderType || w.workorderType === 'SELF')
    },
    searchWorkorders(query) {
      const keyword = String(query || '').trim()
      this.searchKeyword = keyword
      if (this.searchTimer) window.clearTimeout(this.searchTimer)
      // 空关键字回到默认列表；至少 1 个字符才远程查询，输入做 300ms 防抖
      this.searchTimer = window.setTimeout(() => {
        this.searchTimer = null
        if (keyword) this.runSearch(keyword)
        else this.loadWorkorders()
      }, keyword ? 300 : 0)
    },
    async runSearch(keyword) {
      const seq = ++this.listSeq
      this.loadingWorkorders = true
      try {
        const res = await request({
          url: '/mes/pro/workorder/list',
          method: 'get',
          params: { workorderType: 'SELF', keyword, pageNum: 1, pageSize: 50 }
        })
        // 已被更新的检索或默认列表请求替代、或用户已清空关键字时，丢弃这次结果
        if (seq !== this.listSeq || keyword !== this.searchKeyword) return
        const rows = res && Array.isArray(res.rows) ? res.rows : []
        // 只展示后端真实命中的工单，不把当前选中项塞进结果，否则搜不到时会出现“假命中”
        this.workorders = rows.filter(w => this.isPickable(w))
        const current = this.workorders.find(w => String(w.workorderId) === String(this.selectedId))
        if (current) this.selectedWorkorder = current
      } catch (e) {
        if (seq === this.listSeq && keyword === this.searchKeyword) this.workorders = []
      } finally {
        if (seq === this.listSeq) this.loadingWorkorders = false
      }
    },
    tagType(state) { return state === 'done' ? 'success' : (state === 'doing' ? 'warning' : 'info') },
    /** 每个环节的悬浮提示：环节说明 + 当前可做的操作 */
    stepTip(step) {
      if (!step) return ''
      const next = step.action ? `\n现在可做：${step.action}` : ''
      return `${step.name}：${step.text || step.hint}\n${this.stepActionText(step)}${next}`
    },
    stepActionText(step) {
      if (!step) return ''
      if (step.state === 'skipped') return '本环节不适用，无需操作'
      if (step.state === 'done') return '已完成，点击查看本环节单据'
      if (step.state === 'doing') return '下一步，点击进入办理'
      return '前序环节完成后可办理'
    },
    handleSelect(value) {
      const picked = this.workorders.find(w => String(w.workorderId) === String(value)) || null
      if (picked) this.selectedWorkorder = picked
      this.loadPipeline()
      // 选中后收起并失焦，避免输入框里残留“工单号 · 产品（状态）”被当成关键字继续拼接
      this.$nextTick(() => {
        const select = this.$refs.woSelect
        if (select && typeof select.blur === 'function') select.blur()
      })
    },
    go(step) {
      if (!step || !step.route) return
      // 客户订单环节即使本工单没有订单来源也允许跳转，方便从流水线直接去下单
      if (step.state === 'skipped' && step.key !== 'order') return
      if (step.key === 'order') {
        const query = this.woSafe.sourceDocCode ? { orderCode: this.woSafe.sourceDocCode } : {}
        this.$router.push({ path: '/order/salesorder', query }).catch(() => {})
        return
      }
      if (!this.workorder) return
      const query = {
        pipelineWorkorderId: String(this.workorder.workorderId),
        pipelineWorkorderCode: this.workorder.workorderCode
      }
      // 销售出库只认订单号，带上来源订单，避免跳到出库列表后查不到本单
      if (this.workorder.sourceDocCode) query.pipelineSoCode = this.workorder.sourceDocCode
      // 待检报工：跳到过程检验时直接打开该报工对应的检验单
      if (step.autoInspect) {
        query.autoInspect = '1'
        if (step.feedbackCode) query.feedbackCode = step.feedbackCode
        query.inspectToken = String(Date.now())
      }
      this.$router.push({ path: step.route, query }).catch(() => {})
    },
    refreshQuiet() {
      if (this.loading || this.loadingWorkorders) return
      if (typeof document !== 'undefined' && document.hidden) return
      // 下拉正在按关键字检索时只刷新流水线，不覆盖用户当前的搜索结果
      if (this.searchKeyword) return this.loadPipeline()
      return this.refresh()
    },
    refresh() {
      // 关键字还在输入框里时，刷新也按关键字重新检索，避免刷新把搜索结果换成默认列表
      return this.searchKeyword ? this.runSearch(this.searchKeyword) : this.loadWorkorders()
    },
    fetchAll(url, params) { return fetchPipelineRows(request, url, params) },
    async loadWorkorders() {
      const seq = ++this.listSeq
      this.searchKeyword = ''
      this.loadingWorkorders = true
      try {
        const rows = await this.fetchAll('/mes/pro/workorder/list', { workorderType: 'SELF' })
        if (seq !== this.listSeq) return
        const visible = rows.filter(w => this.isPickable(w))
        visible.sort((a, b) => Number(a.status === 'FINISHED') - Number(b.status === 'FINISHED') || Number(b.workorderId) - Number(a.workorderId))
        this.workorders = visible
        // 从入库/出库页“返回流水线”时，优先保持原工单，避免跳回默认第一张
        const preferred = String(this._preferredWorkorderId || '')
        const preferredHit = preferred ? visible.find(w => String(w.workorderId) === preferred) : null
        if (preferredHit) {
          this.selectedId = preferredHit.workorderId
          this._preferredWorkorderId = null
        }
        if (!visible.some(w => String(w.workorderId) === String(this.selectedId))) this.selectedId = visible.length ? visible[0].workorderId : null
        this.selectedWorkorder = visible.find(w => String(w.workorderId) === String(this.selectedId)) || null
        this.hasWorkorders = visible.length > 0
        if (this.selectedId != null) await this.loadPipeline()
        else {
          this.pipelineRequestId++
          this.steps = []
          this.loading = false
          this.errorMsg = ''
        }
      } catch (e) {
        if (seq !== this.listSeq) return
        this.pipelineRequestId++
        this.steps = []
        this.loading = false
        this.errorMsg = '工单加载失败，请刷新重试'
      } finally {
        if (seq === this.listSeq) this.loadingWorkorders = false
      }
    },
    loadSnapshot(wo) { return loadWorkorderPipeline(request, wo) },
    async loadPipeline() {
      const requestId = ++this.pipelineRequestId
      const wo = this.workorder
      this.steps = []
      this.errorMsg = ''
      if (!wo) { this.loading = false; return }
      this.loading = true
      try {
        const snapshot = await this.loadSnapshot(wo)
        if (requestId !== this.pipelineRequestId || String(this.selectedId) !== String(wo.workorderId)) return
        this.steps = this.buildSteps(snapshot)
      } catch (e) {
        if (requestId === this.pipelineRequestId) this.errorMsg = '流水线数据加载失败，请刷新重试'
      } finally {
        if (requestId === this.pipelineRequestId) this.loading = false
      }
    },
    buildSteps(snapshot) { return buildWorkorderPipeline(this.workorder || {}, snapshot) }
  }
}
</script>

<style lang="scss" scoped>
.wo-pl {
  margin: 0 16px 24px;
  border-radius: 12px;

  &__header {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px;
  }

  &__title {
    display: flex;
    align-items: center;
    font-size: 15px;
    font-weight: 600;
    color: #2d3142;
  }

  &__bar {
    width: 3px;
    height: 14px;
    margin-right: 8px;
    border-radius: 2px;
    background: #5e75ff;
  }

  &__sub {
    margin-left: 10px;
    font-size: 12px;
    font-weight: 400;
    color: #8a90a2;
  }

  &__summary {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
    min-width: 260px;
  }

  &__chip {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    height: 24px;
    padding: 0 10px;
    font-size: 12px;
    color: #5b6478;
    background: rgba(45, 49, 66, 0.04);
    border-radius: 12px;

    b {
      color: #2d3142;
    }

    &--next {
      color: #b45309;
      background: rgba(245, 158, 11, 0.12);
    }

    &--ok {
      color: #31946a;
      background: rgba(49, 148, 106, 0.12);
    }

    &--warn {
      color: #c0392b;
      background: rgba(192, 57, 43, 0.1);
    }
  }

  &__chip-action {
    max-width: 260px;
    overflow: hidden;
    padding-left: 6px;
    border-left: 1px solid rgba(180, 83, 9, 0.3);
    color: #986014;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__dot {
    display: inline-block;
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: #f0483e;
    box-shadow: 0 0 0 3px rgba(240, 72, 62, 0.18);
    animation: wo-pl-pulse 1.4s infinite;
  }

  &__tools {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-left: auto;
  }

  &__select {
    width: 320px;
  }

  &__opt {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
  }

  &__opt-code {
    font-weight: 600;
    color: #2d3142;
  }

  &__opt-date {
    color: #7a8498;
  }

  &__opt-product {
    overflow: hidden;
    color: #5b6478;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__empty {
    padding: 28px 0;
    text-align: center;
    font-size: 13px;
    color: #8a90a2;
  }

  &__scroll {
    /* 始终显示横向滚动条，避免最后一个环节被裁切且用户不知道可以滚动 */
    overflow-x: scroll;
    overflow-y: hidden;
    padding-bottom: 6px;
    scrollbar-width: thin;

    &::-webkit-scrollbar {
      height: 10px;
    }

    &::-webkit-scrollbar-track {
      background: #f1f3f8;
      border-radius: 6px;
    }

    &::-webkit-scrollbar-thumb {
      background: #c2c9d9;
      border-radius: 6px;
    }

    &::-webkit-scrollbar-thumb:hover {
      background: #a6afc4;
    }
  }

  &__flow-note, &__related {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
    color: #7a8498;
    font-size: 12px;
    line-height: 1.7;
    margin-bottom: 12px;
  }

  &__shortage {
    display: flex;
    align-items: baseline;
    gap: 8px;
    padding: 10px 12px;
    margin-bottom: 10px;
    color: #986014;
    background: #fff8eb;
    border-radius: 6px;
    font-size: 12px;
    line-height: 1.7;
  }

  &__grid {
    display: grid;
    width: 100%;
    /* 10 个环节 + 工单列的最小宽度，保证窄屏时出现横向滚动而不是裁切 */
    min-width: 1340px;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    overflow: hidden;
  }

  &__cell {
    min-height: 62px;
    padding: 8px 10px;
    font-size: 12px;
    border-right: 1px solid #f2f4f8;
    border-bottom: 1px solid #f2f4f8;
    box-sizing: border-box;

    &--head {
      display: flex;
      flex-direction: row;
      align-items: center;
      justify-content: flex-start;
      gap: 4px;
      min-height: 48px;
      color: #6b6f80;
      background: rgba(45, 49, 66, 0.03);
      font-weight: 500;

      &.is-current-head {
        color: #b45309;
        background: rgba(245, 158, 11, 0.12);
      }
    }

    &--wo {
      border-right: 1px solid #ebeef5;
      background: #fafbfd;
    }

    &--body {
      display: flex;
      flex-direction: column;
      justify-content: center;
      gap: 3px;
    }

    &--state {
      position: relative;
      cursor: pointer;
      transition: background-color 0.18s ease;

      &:hover {
        background: rgba(94, 117, 255, 0.06);
      }

      &.is-done {
        .wo-pl__state-icon,
        .wo-pl__state-text {
          color: #31946a;
        }
      }

      &.is-doing {
        background: rgba(240, 72, 62, 0.05);
        box-shadow: inset 0 0 0 1px rgba(240, 72, 62, 0.35);

        .wo-pl__state-icon,
        .wo-pl__state-text {
          color: #c0392b;
          font-weight: 600;
        }
      }

      &.is-todo {
        .wo-pl__state-icon,
        .wo-pl__state-text {
          color: #a2a8b8;
        }
      }
    }
  }

  &__step-no {
    display: inline-block;
    width: 16px;
    height: 16px;
    margin-right: 4px;
    line-height: 16px;
    text-align: center;
    font-size: 11px;
    color: #8a90a2;
    background: #eef1f7;
    border-radius: 50%;
  }

  &__step-name {
    font-size: 12px;
  }

  &__state-icon {
    font-size: 15px;
    line-height: 1;
  }

  &__state-text {
    font-size: 12px;
  }

  &__state-doc {
    overflow: hidden;
    font-size: 11px;
    color: #9aa0b0;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__state-hint {
    display: -webkit-box;
    overflow: hidden;
    font-size: 11px;
    line-height: 1.35;
    color: #b3b9c7;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }

  &__reddot {
    position: absolute;
    top: 5px;
    left: 5px;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #f0483e;
    box-shadow: 0 0 0 3px rgba(240, 72, 62, 0.2);
    animation: wo-pl-pulse 1.4s infinite;
  }

  &__wo-code {
    font-size: 12px;
    font-weight: 600;
    color: #2d3142;
  }

  &__wo-product {
    margin-bottom: 2px;
    overflow: hidden;
    font-size: 12px;
    color: #5b6478;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__wo-meta {
    font-size: 11px;
    color: #9aa0b0;
  }

  &__legend {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 16px;
    margin: 10px 2px 0;
    font-size: 12px;
    color: #7b8296;
  }

  &__legend-item {
    display: inline-flex;
    align-items: center;
    gap: 5px;

    &.is-done {
      color: #31946a;
    }

    &.is-doing {
      color: #c0392b;
    }

    &.is-todo {
      color: #a2a8b8;
    }
  }

  &__legend-tip {
    margin-left: auto;
    color: #a2a8b8;
  }

  &__detail {
    margin-top: 14px;
  }

  &__detail-name {
    font-size: 12px;
    color: #2d3142;
  }

  &__detail-action {
    color: #986014;
  }
}

@keyframes wo-pl-pulse {
  0% {
    transform: scale(1);
    box-shadow: 0 0 0 0 rgba(240, 72, 62, 0.45);
  }
  70% {
    transform: scale(1);
    box-shadow: 0 0 0 6px rgba(240, 72, 62, 0);
  }
  100% {
    transform: scale(1);
    box-shadow: 0 0 0 0 rgba(240, 72, 62, 0);
  }
}
</style>

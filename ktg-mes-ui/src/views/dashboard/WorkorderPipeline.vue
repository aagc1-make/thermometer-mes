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
            下一步：<b>{{ nextStep.name }}</b>
          </span>
          <span class="wo-pl__chip wo-pl__chip--ok" v-else>
            <i class="el-icon-circle-check"></i> 工单流程已完成
          </span>
        </template>
        <span class="wo-pl__chip wo-pl__chip--warn" v-if="errorMsg">{{ errorMsg }}</span>
      </div>

      <div class="wo-pl__tools">
        <el-select
          v-model="selectedId"
          class="wo-pl__select"
          size="small"
          filterable
          placeholder="选择工单"
          :loading="loadingWorkorders"
          @change="loadPipeline"
        >
          <el-option
            v-for="w in workorders"
            :key="w.workorderId"
            :label="optionLabel(w)"
            :value="w.workorderId"
          />
        </el-select>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="refresh">刷新</el-button>
        <el-button size="small" type="text" @click="detailVisible = !detailVisible">
          {{ detailVisible ? '收起步骤明细' : '展开步骤明细' }}
        </el-button>
      </div>
    </div>

    <div class="wo-pl__empty" v-if="!loadingWorkorders && !workorders.length">
      暂无可跟踪的自产工单。先在「生产 → 生产工单」新增、保存并确认工单，再进行生产排产。
    </div>

    <template v-else>
      <div class="wo-pl__flow-note">工单确认后先排产，排产生成领料草稿；需检工序报工后先完成 IPQC，再继续生产。工单完工后执行产品入库，最后按工单成品开销售出库单，发货即完成全生命周期。</div>
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
          >
            <span class="wo-pl__step-no">{{ i + 1 }}</span>
            <span class="wo-pl__step-name">{{ s.short }}</span>
          </div>

          <div class="wo-pl__cell wo-pl__cell--wo wo-pl__cell--body">
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
            <span class="wo-pl__state-doc" :title="s.docTitle || s.docCode">{{ s.docCode || s.hint }}</span>
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
        <el-button type="text" size="mini" @click="$router.push('/mes/wm/arrivalnotice')">到货通知</el-button>
        <el-button type="text" size="mini" @click="$router.push('/mes/qc/iqc')">来料检验</el-button>
        <el-button type="text" size="mini" @click="$router.push('/mes/wm/itemrecpt')">采购入库</el-button>
        <el-button type="text" size="mini" @click="$router.push('/mes/wm/productsales')">销售出库</el-button>
        <span>缺料时补料；成品入库后按「工单成品」开销售出库单，发货完成即工单闭环。</span>
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
          <template slot-scope="s">{{ s.row.text || s.row.hint }}</template>
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
      steps: [],
      timer: null,
      pipelineRequestId: 0,
      workorderRequestId: 0
    }
  },
  computed: {
    workorder() {
      return this.workorders.find(w => String(w.workorderId) === String(this.selectedId)) || null
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
    materialStep() { return this.steps.find(s => s.key === 'ready') || null }
  },
  mounted() {
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
    this.workorderRequestId++
    this.pipelineRequestId++
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
    optionLabel(w) {
      const status = { PREPARE: '待确认', CONFIRMED: '已确认', FINISHED: '已完工' }[w.status] || w.status || ''
      return `${w.workorderCode} · ${w.productName || w.productCode || ''}（${status}）`
    },
    tagType(state) { return state === 'done' ? 'success' : (state === 'doing' ? 'warning' : 'info') },
    go(step) {
      if (!step || !step.route || step.state === 'skipped' || !this.workorder) return
      this.$router.push({ path: step.route, query: {
        pipelineWorkorderId: String(this.workorder.workorderId),
        pipelineWorkorderCode: this.workorder.workorderCode
      } }).catch(() => {})
    },
    refreshQuiet() {
      if (this.loading || this.loadingWorkorders) return
      if (typeof document !== 'undefined' && document.hidden) return
      return this.refresh()
    },
    refresh() { return this.loadWorkorders() },
    fetchAll(url, params) { return fetchPipelineRows(request, url, params) },
    async loadWorkorders() {
      const requestId = ++this.workorderRequestId
      this.loadingWorkorders = true
      try {
        const rows = await this.fetchAll('/mes/pro/workorder/list', { workorderType: 'SELF' })
        if (requestId !== this.workorderRequestId) return
        const visible = rows.filter(w => ['PREPARE', 'CONFIRMED', 'FINISHED'].includes(w.status) && (!w.workorderType || w.workorderType === 'SELF'))
        visible.sort((a, b) => Number(a.status === 'FINISHED') - Number(b.status === 'FINISHED') || Number(b.workorderId) - Number(a.workorderId))
        this.workorders = visible
        if (!visible.some(w => String(w.workorderId) === String(this.selectedId))) this.selectedId = visible.length ? visible[0].workorderId : null
        if (this.selectedId != null) await this.loadPipeline()
        else {
          this.pipelineRequestId++
          this.steps = []
          this.loading = false
          this.errorMsg = ''
        }
      } catch (e) {
        if (requestId !== this.workorderRequestId) return
        this.pipelineRequestId++
        this.steps = []
        this.loading = false
        this.errorMsg = '工单加载失败，请刷新重试'
      } finally {
        if (requestId === this.workorderRequestId) this.loadingWorkorders = false
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

  &__empty {
    padding: 28px 0;
    text-align: center;
    font-size: 13px;
    color: #8a90a2;
  }

  &__scroll {
    overflow-x: auto;
    padding-bottom: 4px;
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
    min-width: 100%;
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

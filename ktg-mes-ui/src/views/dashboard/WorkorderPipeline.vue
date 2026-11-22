<template>
  <section class="pipeline-section" aria-labelledby="home-pipeline-title">
    <el-card class="wo-pl" shadow="never">
      <div slot="header" class="wo-pl__header">
        <div>
          <h2 id="home-pipeline-title">工单流水线</h2>
          <p>{{ stepDefs.length }} 个环节 · 每 30 秒自动刷新</p>
        </div>

        <div class="wo-pl__tools">
          <div class="wo-pl__field">
            <label for="home-pipeline-order">跟踪工单</label>
            <el-select
              id="home-pipeline-order"
              v-model="selectedId"
              class="wo-pl__select"
              size="small"
              filterable
              placeholder="选择工单"
              :loading="loadingWorkorders"
              @change="loadPipeline()"
            >
              <el-option
                v-for="w in workorders"
                :key="w.workorderId"
                :label="optionLabel(w)"
                :value="w.workorderId"
              />
            </el-select>
          </div>
          <el-button size="small" icon="el-icon-refresh" :loading="loadingWorkorders || loading" @click="refresh">刷新</el-button>
        </div>
      </div>

      <div v-loading="loadingWorkorders || loading" class="wo-pl__body" :aria-busy="String(loadingWorkorders || loading)">
        <el-alert v-if="errorMsg" :title="errorMsg" type="error" :closable="false" show-icon />
        <p v-else-if="!loadingWorkorders && !workorders.length" class="wo-pl__empty">
          暂无可跟踪的自产工单。先在「生产 → 生产工单」新增、保存并确认工单，再进行生产排产。
        </p>
        <template v-else-if="steps.length">
          <div class="wo-pl__summary">
            <div class="wo-pl__order">
              <div class="wo-pl__order-heading">
                <strong>{{ woSafe.workorderCode }}</strong>
                <span>{{ woSafe.productName }}</span>
              </div>
              <div class="wo-pl__order-meta">
                <span>计划 <b>{{ woSafe.quantity }}</b>{{ woSafe.unitName || woSafe.unitOfMeasure || '' }}</span>
                <span>累计报工 <b>{{ woSafe.quantityProduced || 0 }}</b></span>
              </div>
              <div class="wo-pl__progress">
                <span>流程进度</span>
                <el-progress
                  :percentage="overallPercent"
                  :stroke-width="5"
                  :show-text="false"
                  color="var(--color-progress)"
                />
                <span>{{ doneCount }} / {{ steps.length }}</span>
              </div>
            </div>

            <div v-if="nextStep" class="wo-pl__next">
              <div>
                <p class="wo-pl__next-label">下一步 · 第 {{ currentIndex }} / {{ stepDefs.length }} 步</p>
                <h3>{{ nextStep.name }}</h3>
                <p>{{ nextStep.docCode || nextStep.hint }}</p>
              </div>
              <el-button class="wo-pl__next-button" type="primary" size="small" @click="go(nextStep)">
                前往{{ nextStep.name }} <i class="el-icon-arrow-right" aria-hidden="true" />
              </el-button>
            </div>
            <div v-else class="wo-pl__next wo-pl__next--done">
              <h3><i class="el-icon-circle-check" aria-hidden="true" /> 工单流程已完成</h3>
            </div>
          </div>

          <div class="wo-pl__stages-header">
            <h3>流程环节</h3>
            <el-button
              type="text"
              size="small"
              :aria-expanded="String(detailVisible)"
              aria-controls="home-pipeline-detail"
              @click="detailVisible = !detailVisible"
            >
              {{ detailVisible ? '收起步骤明细' : '展开步骤明细' }}
              <i :class="detailVisible ? 'el-icon-arrow-up' : 'el-icon-arrow-down'" aria-hidden="true" />
            </el-button>
          </div>
          <ol class="wo-pl__stages" aria-label="工单流程环节">
            <li v-for="(s, i) in steps" :key="s.key">
              <button
                class="wo-pl__step"
                :class="'is-' + s.state"
                type="button"
                :disabled="s.state === 'skipped' || !s.route"
                :aria-current="s.state === 'doing' ? 'step' : null"
                @click="go(s)"
              >
                <span class="wo-pl__step-heading">
                  <span class="wo-pl__step-no">{{ i + 1 }}</span>
                  <strong>{{ s.short }}</strong>
                </span>
                <span class="wo-pl__step-state">{{ s.stateText }}</span>
                <span class="wo-pl__step-doc" :title="s.docTitle || s.docCode">{{ s.docCode || s.hint }}</span>
              </button>
            </li>
          </ol>

          <div v-if="materialStep && materialStep.shortages.length" class="wo-pl__shortage">
            <i class="el-icon-warning-outline" aria-hidden="true" />
            <span>{{ materialStep.text }}</span>
          </div>

          <div class="wo-pl__related">
            <span>补料与后续业务</span>
            <shortage-assistant v-if="workorder" :key="workorder.workorderId" :workorder-id="workorder.workorderId" />
            <el-button type="text" size="small" @click="$router.push('/mes/wm/arrivalnotice')">到货通知</el-button>
            <el-button type="text" size="small" @click="$router.push('/mes/qc/iqc')">来料检验</el-button>
            <el-button type="text" size="small" @click="$router.push('/mes/wm/itemrecpt')">采购入库</el-button>
            <el-button type="text" size="small" @click="$router.push('/mes/wm/productsales')">销售出库</el-button>
          </div>

          <div v-show="detailVisible" id="home-pipeline-detail" class="wo-pl__detail">
            <p>工单确认后先排产，排产生成领料草稿。需检工序报工后先完成 IPQC，再继续生产。缺料时补料。工单完工后执行产品入库，最后按「工单成品」开销售出库单，发货即完成全生命周期。</p>
            <div class="wo-pl__detail-scroll" role="region" aria-label="工单步骤明细表格" tabindex="0">
              <el-table v-if="detailVisible" size="mini" :data="steps">
                <el-table-column label="#" width="46" align="center">
                  <template slot-scope="s">{{ s.$index + 1 }}</template>
                </el-table-column>
                <el-table-column label="步骤" width="110" prop="name" />
                <el-table-column label="状态" width="94">
                  <template slot-scope="s">
                    <el-tag size="mini" :type="tagType(s.row.state)">{{ s.row.stateText }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="单据 / 依据" min-width="154" show-overflow-tooltip>
                  <template slot-scope="s">{{ s.row.docCode || '-' }}</template>
                </el-table-column>
                <el-table-column label="说明" min-width="220" show-overflow-tooltip>
                  <template slot-scope="s">{{ s.row.text || s.row.hint }}</template>
                </el-table-column>
                <el-table-column label="操作" width="82" align="center">
                  <template slot-scope="s">
                    <el-button type="text" size="mini" :disabled="s.row.state === 'skipped'" @click="go(s.row)">{{ s.row.state === 'done' ? '查看' : '去操作' }}</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </template>
        <p v-else class="wo-pl__empty">正在加载工单流水线</p>
      </div>
    </el-card>
  </section>
</template>

<script>
import request from '@/utils/request'
import { PIPELINE_STEPS, buildWorkorderPipeline, fetchPipelineRows, loadWorkorderPipeline } from '@/utils/workorderPipeline'

const POLL_INTERVAL = 30000

export default {
  name: 'WorkorderPipeline',
  components: { ShortageAssistant: () => import('@/views/mes/pro/workorder/ShortageAssistant.vue') },
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
    woSafe() { return this.workorder || {} },
    doneCount() { return this.steps.filter(s => s.state === 'done' || s.state === 'skipped').length },
    currentIndex() {
      const index = this.steps.findIndex(s => s.state === 'doing')
      return index < 0 ? this.steps.length : index + 1
    },
    nextStep() { return this.steps.find(s => s.state === 'doing') || null },
    overallPercent() { return this.steps.length ? Math.round(this.doneCount / this.steps.length * 100) : 0 },
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
        const previousId = this.selectedId
        this.workorders = visible
        if (!visible.some(w => String(w.workorderId) === String(this.selectedId))) this.selectedId = visible.length ? visible[0].workorderId : null
        if (this.selectedId != null) await this.loadPipeline(this.selectedId === previousId)
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
    async loadPipeline(keepSteps = false) {
      const requestId = ++this.pipelineRequestId
      const wo = this.workorder
      if (!keepSteps) this.steps = []
      this.errorMsg = ''
      if (!wo) { this.loading = false; return }
      this.loading = true
      try {
        const snapshot = await this.loadSnapshot(wo)
        if (requestId !== this.pipelineRequestId || String(this.selectedId) !== String(wo.workorderId)) return
        this.steps = this.buildSteps(snapshot)
      } catch (e) {
        if (requestId === this.pipelineRequestId) {
          this.steps = []
          this.errorMsg = '流水线数据加载失败，请刷新重试'
        }
      } finally {
        if (requestId === this.pipelineRequestId) this.loading = false
      }
    },
    buildSteps(snapshot) { return buildWorkorderPipeline(this.workorder || {}, snapshot) }
  }
}
</script>

<style lang="scss" scoped>
.pipeline-section {
  min-width: 0;
}

.wo-pl {
  &__header,
  &__tools,
  &__stages-header,
  &__related {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  &__header,
  &__stages-header {
    justify-content: space-between;
    flex-wrap: wrap;
  }

  &__header {
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

  &__tools {
    flex: 0 1 460px;
    min-width: 0;
  }

  &__field {
    display: flex;
    flex: 1;
    align-items: center;
    gap: 8px;
    min-width: 0;
    font-size: 12px;
    color: var(--color-text-secondary);
    white-space: nowrap;
  }

  &__select {
    flex: 1;
    min-width: 0;

    ::v-deep .el-input__inner:not(:focus) {
      text-overflow: ellipsis;
    }
  }

  &__empty {
    margin: 0;
    padding: 18px 0;
    color: var(--color-text-secondary);
    font-size: 13px;
    line-height: 1.6;
    text-align: center;
  }

  &__summary {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
    margin-bottom: 10px;
  }

  &__order,
  &__next {
    min-width: 0;
    padding: 12px 14px;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-control);
    overflow-wrap: anywhere;
  }

  &__order-heading {
    display: flex;
    align-items: baseline;
    flex-wrap: wrap;
    gap: 4px 10px;
    font-size: 13px;

    strong {
      font-size: 14px;
    }
  }

  &__order-heading span,
  &__order-meta,
  &__progress {
    color: var(--color-text-secondary);
  }

  &__order-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 4px 16px;
    margin-top: 7px;
    font-size: 12px;

    b {
      color: var(--color-text);
    }
  }

  &__progress {
    display: grid;
    grid-template-columns: auto minmax(0, 1fr) auto;
    align-items: center;
    gap: 8px;
    margin-top: 8px;
    font-size: 12px;
    font-variant-numeric: tabular-nums;
  }

  &__next {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 8px 12px;
    background: var(--color-primary-bg);

    > div {
      min-width: 0;
    }

    h3 {
      margin: 3px 0;
      font-size: 16px;
      line-height: 1.4;
    }

    p {
      margin: 0;
      color: var(--color-text-secondary);
      font-size: 12px;
      line-height: 1.5;
    }

    .wo-pl__next-label {
      color: var(--color-primary);
      font-weight: 600;
    }

    &--done {
      color: #246d5e;
      background: #edf5e6;
    }
  }

  &__next-button {
    border-color: var(--color-success);
    background: var(--color-success);

    &:hover,
    &:focus {
      border-color: #3d6651;
      background: #3d6651;
    }
  }

  &__stages-header h3 {
    margin: 0;
    font-size: 14px;
  }

  &__stages-header .el-button,
  &__related .el-button,
  &__detail .el-button:not(.is-disabled) {
    color: var(--color-primary);
  }

  &__stages {
    display: grid;
    grid-template-columns: repeat(9, minmax(0, 1fr));
    gap: 6px;
    margin: 2px 0 0;
    padding: 0;
    list-style: none;

    li {
      min-width: 0;
    }
  }

  &__step {
    position: relative;
    display: grid;
    grid-template-columns: minmax(0, 1fr);
    gap: 4px;
    width: 100%;
    height: 100%;
    min-height: 74px;
    padding: 8px;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-control);
    background: #fff;
    color: var(--color-text);
    text-align: left;
    cursor: pointer;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 3px;
      border-radius: 5px 5px 0 0;
      background: var(--color-border);
    }

    &.is-done::before {
      background: var(--color-progress);
    }

    &:not(:disabled):hover {
      border-color: var(--color-primary);
    }

    &:focus-visible {
      outline-color: var(--color-primary);
    }

    &:disabled {
      cursor: default;
    }

    &.is-doing {
      border-color: var(--color-border-strong);
      background: var(--color-primary-soft);

      &::before {
        background: linear-gradient(90deg, var(--color-progress-current) 40%, var(--color-border) 40%);
      }

      .wo-pl__step-state {
        color: var(--color-primary);
        font-weight: 700;
      }
    }

    &.is-done .wo-pl__step-state {
      color: #246d5e;
    }
  }

  &__step-heading {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 3px 5px;
    min-width: 0;
    font-size: 12px;
    overflow-wrap: anywhere;
  }

  &__step-no {
    color: var(--color-text-secondary);
    font-size: 11px;
    font-variant-numeric: tabular-nums;
  }

  &__step-state,
  &__step-doc {
    color: var(--color-text-secondary);
    font-size: 12px;
  }

  &__step-doc {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__shortage {
    display: flex;
    align-items: baseline;
    gap: 8px;
    margin-top: 12px;
    padding: 8px 10px;
    border-radius: var(--radius-control);
    color: #986014;
    background: #fff8eb;
    font-size: 12px;
    line-height: 1.6;
    overflow-wrap: anywhere;
  }

  &__related {
    flex-wrap: wrap;
    margin-top: 10px;
    color: var(--color-text-secondary);
    font-size: 12px;

    .el-button {
      margin: 0;
    }
  }

  &__detail {
    border-top: 1px solid var(--color-border);
    margin-top: 6px;

    p {
      margin: 10px 0;
      color: var(--color-text-secondary);
      font-size: 12px;
      line-height: 1.6;
    }
  }

  &__detail-scroll {
    overflow-x: auto;

    &:focus-visible {
      outline-offset: -2px;
    }

    .el-table {
      min-width: 706px;
    }
  }
}

@media (max-width: 1100px) {
  .wo-pl__stages {
    grid-template-columns: repeat(5, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .wo-pl__summary {
    grid-template-columns: minmax(0, 1fr);
    gap: 8px;
  }

  .wo-pl__stages {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .wo-pl ::v-deep .el-card__header,
  .wo-pl ::v-deep .el-card__body {
    padding: 12px;
  }
}

@media (max-width: 420px) {
  .wo-pl__tools {
    flex-basis: 100%;
    align-items: flex-end;
  }

  .wo-pl__field {
    display: grid;
    gap: 4px;
  }
}
</style>

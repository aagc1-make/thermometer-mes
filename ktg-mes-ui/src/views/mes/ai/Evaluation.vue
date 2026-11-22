<template>
  <div class="app-container ai-evaluation">
    <header class="evaluation-heading">
      <div>
        <h1 class="mes-page-heading">问答评测</h1>
        <p>{{ libraryId === 'acceptance' ? '核对 30 道文档题、20 道数据题和 100 条标注关系。' : '使用测温枪演示样例，核对检索、业务数据、引用和图谱关系。' }}</p>
      </div>
      <div class="evaluation-controls">
        <el-select v-model="libraryId" placeholder="选择评测范围" size="small" aria-label="评测范围"
          :disabled="busy || loadingLibraries" @change="clearReport">
          <el-option v-if="suiteAvailable" label="三部门验收集" value="acceptance" />
          <el-option v-for="library in libraries" :key="library.libraryId" :label="library.name" :value="library.libraryId" />
        </el-select>
        <el-button size="small" type="primary" icon="el-icon-video-play" :loading="busy"
          :disabled="!libraryId || loadingLibraries" @click="run">运行评测</el-button>
      </div>
    </header>

    <el-alert v-if="error" type="error" :closable="false" show-icon :title="error" />

    <div class="evaluation-metrics" aria-label="评测指标">
      <button v-for="metric in metrics" :key="metric.key" type="button" class="evaluation-metric"
        :disabled="!report" :aria-pressed="!!report && selectedKey === metric.key" @click="selectMetric(metric.key)">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.total ? percent(metric) : '—' }}</strong>
        <div class="evaluation-track" aria-hidden="true">
          <i :style="{ width: metric.total ? percent(metric) : '0%' }" />
        </div>
        <small v-if="metric.total">{{ metric.matched }} / {{ metric.total }} {{ metric.unit }}<span v-if="metric.skipped"> · {{ metric.skipped }} 项未评测</span></small>
        <small v-else>{{ report ? '没有可评测样本' : '等待运行' }}</small>
      </button>
    </div>

    <section v-if="report && currentMetric" class="evaluation-results" aria-label="评测明细">
      <header>
        <div>
          <h2>{{ currentMetric.label }}样例</h2>
          <p>{{ report.libraryName }} · {{ formatTime(report.finishedAt) }} · {{ (report.elapsedMs / 1000).toFixed(1) }} 秒</p>
        </div>
        <el-button size="mini" icon="el-icon-download" @click="downloadReport">下载报告</el-button>
      </header>
      <div class="evaluation-results__body">
        <div class="evaluation-table">
          <el-table :data="currentMetric.cases" :current-row-key="selectedCase" row-key="name" max-height="560"
            highlight-current-row size="small" @row-click="chooseCase">
            <el-table-column label="样例" min-width="200">
              <template slot-scope="{ row }">
                <button type="button" class="evaluation-case" :aria-pressed="selectedCase === row.name"
                  @click.stop="chooseCase(row)">{{ row.name }}</button>
              </template>
            </el-table-column>
            <el-table-column label="结果" width="130">
              <template slot-scope="{ row }">
                <span class="evaluation-status" :class="'status--' + row.status">
                  <i :class="statusIcon[row.status]" aria-hidden="true" /> {{ statusText[row.status] }}
                  <small v-if="row.total">{{ row.matched }}/{{ row.total }}</small>
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <aside v-if="detail" class="evaluation-detail" aria-label="样例详情">
          <p class="evaluation-definition">{{ currentMetric.definition }}</p>
          <h3>{{ detail.name }}</h3>
          <h4>核对依据</h4>
          <pre>{{ detail.expected }}</pre>
          <h4>实际结果</h4>
          <pre>{{ detail.actual }}</pre>
        </aside>
      </div>
    </section>
    <section v-else class="evaluation-empty" :aria-busy="busy" role="status">
      <i :class="busy ? 'el-icon-loading' : 'el-icon-data-analysis'" aria-hidden="true" />
      <p>{{ busy ? '正在读取样例和业务数据…' : '运行评测后，可以逐条核对样例。' }}</p>
    </section>
  </div>
</template>

<script>
import request from '@/utils/request'
import { listAdminDepartmentLibraries, listDepartmentLibraries } from '@/api/mes/knowledgeLibrary'
import { saveAs } from 'file-saver'

const initialMetrics = [
  { key: 'recall', label: 'Recall@5' },
  { key: 'numbers', label: '数值正确率' },
  { key: 'citations', label: '引用覆盖率' },
  { key: 'triples', label: '三元组抽检准确率' }
]

export default {
  name: 'AiEvaluation',
  data() {
    return {
      libraries: [], libraryId: null, suiteAvailable: false, loadingLibraries: true, busy: false, error: '',
      report: null, selectedKey: 'recall', selectedCase: '',
      statusText: { passed: '通过', failed: '不符', skipped: '未评测' },
      statusIcon: { passed: 'el-icon-circle-check', failed: 'el-icon-circle-close', skipped: 'el-icon-minus' }
    }
  },
  computed: {
    metrics() { return this.report ? this.report.metrics : initialMetrics },
    currentMetric() { return this.metrics.find(metric => metric.key === this.selectedKey) },
    detail() { return this.currentMetric && this.currentMetric.cases && this.currentMetric.cases.find(row => row.name === this.selectedCase) }
  },
  async created() {
    try {
      const [admin, own, suite] = await Promise.all([
        listAdminDepartmentLibraries(), listDepartmentLibraries(),
        request({ url: '/mes/ai/evaluation/suite', method: 'get' })
      ])
      this.suiteAvailable = suite.data === true
      this.libraries = admin.data.filter(library => library.status === 'enabled')
      const requested = this.$route.query.libraryId || (own.data[0] && String(own.data[0].libraryId))
      const selected = this.libraries.find(library => String(library.libraryId) === requested) || this.libraries[0]
      this.libraryId = this.suiteAvailable && this.$route.query.suite === '1'
        ? 'acceptance' : (selected && selected.libraryId)
    } catch (error) {
      this.error = error.message || '部门知识库暂不可用。'
    } finally {
      this.loadingLibraries = false
    }
  },
  methods: {
    clearReport() { this.report = null; this.error = ''; this.selectedCase = '' },
    async run() {
      if (this.busy || !this.libraryId) return
      this.clearReport()
      this.busy = true
      try {
        const { data } = await request({
          url: '/mes/ai/evaluation/run', method: 'post',
          data: this.libraryId === 'acceptance' ? { suite: true } : { libraryId: this.libraryId },
          timeout: 120000
        })
        this.report = data
        this.selectMetric('recall')
      } catch (error) {
        this.error = error.message || '评测暂未完成，请重试。'
      } finally {
        this.busy = false
      }
    },
    selectMetric(key) {
      this.selectedKey = key
      const first = this.currentMetric.cases[0]
      this.selectedCase = first ? first.name : ''
    },
    chooseCase(row) { this.selectedCase = row.name },
    percent(metric) { return `${Math.round(1000 * metric.matched / metric.total) / 10}%` },
    formatTime(value) { return new Date(value).toLocaleString('zh-CN', { hour12: false }) },
    downloadReport() {
      const blob = new Blob([JSON.stringify(this.report, null, 2)], { type: 'application/json;charset=utf-8' })
      saveAs(blob, `问答评测-${this.report.finishedAt.slice(0, 10)}.json`)
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/assets/styles/mes-palette";

.ai-evaluation { color: $mes-text; padding: 24px 26px; }
.evaluation-heading, .evaluation-results > header, .evaluation-controls {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
}
.evaluation-heading { margin-bottom: 18px; flex-wrap: wrap; }
.evaluation-heading h1 { margin: 0; }
.evaluation-heading p, .evaluation-results p { margin: 6px 0 0; color: $mes-text-sub; font-size: 13px; line-height: 1.45; }
.evaluation-controls .el-select { width: 208px; }
.evaluation-metrics { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; margin: 12px 0 18px; }
.evaluation-metric {
  background: $mes-card; border: 1px solid $mes-border; color: $mes-text; border-radius: 8px;
  display: flex; flex-direction: column; gap: 9px; text-align: left; padding: 14px; min-width: 0; cursor: pointer; font: inherit;
}
.evaluation-metric:disabled { cursor: default; }
.evaluation-metric[aria-pressed='true'] { background: $mes-primary-soft; border-color: $mes-marker; }
.evaluation-metric > span { font-size: 13px; color: $mes-text-sub; }
.evaluation-metric > strong { font-size: 27px; font-weight: 650; font-variant-numeric: tabular-nums; }
.evaluation-metric small { color: $mes-text-sub; line-height: 1.3; font-size: 12px; }
.evaluation-track { height: 3px; width: 100%; border-radius: 3px; background: $mes-border; }
.evaluation-track i { display: block; height: 100%; border-radius: 3px; background: $mes-marker; }
.evaluation-results, .evaluation-empty { border: 1px solid $mes-border; background: $mes-card; border-radius: 9px; overflow: hidden; }
.evaluation-results > header { padding: 16px; border-bottom: 1px solid $mes-border; }
.evaluation-results h2, .evaluation-results h3 { margin: 0; font-size: 16px; }
.evaluation-results__body { display: grid; grid-template-columns: minmax(0, 1.3fr) minmax(280px, 1fr); }
.evaluation-table { min-width: 0; }
.evaluation-table ::v-deep .el-table { border: 0; }
.evaluation-case { border: 0; padding: 3px 0; text-align: left; color: $mes-primary; background: transparent; cursor: pointer; font: inherit; }
.evaluation-case:hover { text-decoration: underline; }
.evaluation-status { font-size: 12px; white-space: nowrap; }
.evaluation-status small { margin-left: 3px; color: $mes-text-sub; font-variant-numeric: tabular-nums; }
.status--passed { color: $mes-success; }
.status--failed { color: $mes-danger; }
.status--skipped { color: $mes-text-sub; }
.evaluation-detail { border-left: 1px solid $mes-border; padding: 16px; min-width: 0; }
.evaluation-detail .evaluation-definition { margin: 0 0 12px; }
.evaluation-detail h3 { line-height: 1.45; }
.evaluation-detail h4 { margin: 12px 0 6px; font-size: 12px; color: $mes-text-sub; }
.evaluation-detail pre { padding: 10px; border-radius: 6px; margin: 0; background: $mes-primary-soft; font: inherit; font-size: 13px; line-height: 1.55; white-space: pre-wrap; overflow-wrap: anywhere; }
.evaluation-empty { min-height: 230px; display: grid; place-content: center; justify-items: center; color: $mes-text-sub; }
.evaluation-empty i { font-size: 22px; color: $mes-marker; }
@media (max-width: 760px) {
  .ai-evaluation { padding: 16px; }
  .evaluation-metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .evaluation-results__body { grid-template-columns: 1fr; }
  .evaluation-detail { border-left: 0; border-top: 1px solid $mes-border; }
}
</style>

<template>
  <span v-if="canRead">
    <el-button size="small" icon="el-icon-data-analysis" @click="openPanel">分析缺料</el-button>
    <el-dialog title="工单缺料分析" :visible.sync="visible" append-to-body
      width="min(960px, 94vw)" :close-on-click-modal="false" :before-close="beforeClose">
      <div v-loading="loading" class="shortage-panel">
        <el-alert v-if="error" type="warning" :closable="false" :title="error" />
        <template v-if="plan">
          <h3>{{ plan.workorderCode }} · 备料检查</h3>
          <p v-if="!plan.materials.length">当前工单没有可核算的外购 BOM 行，请核对工单物料。</p>
          <template v-else>
            <p>{{ shortages.length ? '有 ' + shortages.length + ' 种外购料存在缺口' : '当前外购料已齐套' }}。原料仓现存和本工单已执行领料分别列示。</p>
            <el-table :data="plan.materials" size="mini" max-height="250">
              <el-table-column label="物料" min-width="165">
                <template slot-scope="s">{{ s.row.itemName }} · {{ s.row.itemCode }}</template>
              </el-table-column>
              <el-table-column label="单位" prop="unit" width="72" />
              <el-table-column label="需求" prop="required" align="right" />
              <el-table-column label="已领" prop="issued" align="right" />
              <el-table-column label="原料仓现存" prop="onHand" min-width="95" align="right" />
              <el-table-column label="缺口" prop="missing" align="right" />
            </el-table>
          </template>

          <div v-if="uncovered.length && !hasActive && !plan.closed" class="shortage-section">
            <h4>待采购跟进</h4>
            <p v-for="row in uncovered" :key="row.itemId">{{ row.itemName }}（{{ row.itemCode }}）仍缺 {{ row.uncovered }} {{ row.unit }}，当前符合条件的来源仓无法覆盖。</p>
          </div>
          <div v-if="plan.documents.length" class="shortage-section">
            <h4>关联调拨单</h4>
            <div v-for="doc in plan.documents" :key="doc.transferId">
              <el-button v-if="canOpen" type="text" @click="openDocument(doc)">{{ doc.transferCode }}</el-button>
              <span v-else>{{ doc.transferCode }}</span>
              <dict-tag :options="dict.type.mes_transfer_status" :value="doc.status" />
            </div>
          </div>
          <p v-if="plan.closed">工单已结束，仅供查看。</p>
          <p v-else-if="hasActive">已有调拨单待处理，完成或撤销后可重新分析剩余需求。</p>
          <div v-else-if="sources.length" class="shortage-section">
            <h4>调入原料仓 WH-RAW</h4>
            <p>按入库先后分配。请选择本次调拨的批次，仓管员在上架时指定目标库位。</p>
            <el-table :data="sources" row-key="stockId" size="mini" max-height="240" @selection-change="selected = $event">
              <el-table-column v-if="canCreate" type="selection" width="42" />
              <el-table-column label="物料" prop="itemName" min-width="112" />
              <el-table-column label="来源仓 / 库位" min-width="160">
                <template slot-scope="s">{{ s.row.warehouse }} / {{ s.row.location }} / {{ s.row.area }}</template>
              </el-table-column>
              <el-table-column label="批次" prop="batch" min-width="100" />
              <el-table-column label="可调" prop="available" align="right" />
              <el-table-column label="建议调拨" min-width="95" align="right">
                <template slot-scope="s">{{ s.row.quantity }} {{ s.row.unit }}</template>
              </el-table-column>
            </el-table>
          </div>
          <div v-if="shortages.length && !hasActive && !plan.closed" class="shortage-section">
            <el-button type="text" :loading="explaining" :disabled="explaining || busy" @click="explain">请 AI 解读缺料依据</el-button>
            <p v-if="explanation" class="shortage-explanation">{{ explanation }}</p>
          </div>
        </template>
      </div>
      <span slot="footer">
        <el-button size="small" :disabled="busy" @click="visible = false">关闭</el-button>
        <el-button size="small" :disabled="busy || loading" @click="refresh">刷新分析</el-button>
        <el-button v-if="plan && !plan.closed && !hasActive && canCreate && sources.length"
          type="primary" size="small" :loading="busy" :disabled="!selected.length || selected.length > 500 || stale"
          @click="confirmDraft">确认生成调拨草稿（{{ selected.length }} 行）</el-button>
      </span>
    </el-dialog>
  </span>
</template>

<script>
import request from '@/utils/request'

const readPermissions = ['mes:pro:workorder:query', 'mes:wm:wmstock:list', 'mes:wm:issueheader:list', 'mes:wm:transfer:list']

export default {
  name: 'ShortageAssistant',
  dicts: ['mes_transfer_status'],
  props: { workorderId: { type: [Number, String], required: true } },
  data() {
    return { visible: false, plan: null, selected: [], loading: false, busy: false, stale: false,
      error: '', explanation: '', explaining: false, revision: 0 }
  },
  computed: {
    canRead() { return this.$auth.hasPermiAnd(readPermissions) },
    canCreate() { return this.$auth.hasPermi('mes:wm:transfer:add') },
    canOpen() { return this.$auth.hasPermi('mes:wm:transfer:query') },
    url() { return '/mes/pro/workorder/' + this.workorderId + '/shortage' },
    shortages() { return this.plan ? this.plan.materials.filter(row => row.missing > 0) : [] },
    uncovered() { return this.shortages.filter(row => row.uncovered > 0) },
    hasActive() { return this.plan && this.plan.documents.some(doc => doc.active) },
    sources() {
      return this.shortages.reduce((all, row) => all.concat(row.sources.map(source => ({
        ...source, itemName: row.itemName, unit: row.unit
      }))), [])
    }
  },
  beforeDestroy() { this.revision++ },
  methods: {
    beforeClose(done) { if (!this.busy) done() },
    openPanel() { this.visible = true; this.refresh() },
    async refresh() {
      const revision = ++this.revision
      this.plan = null
      this.selected = []
      this.loading = true
      this.explaining = false
      this.explanation = ''
      this.stale = false
      this.error = ''
      try {
        const { data } = await request({ url: this.url, method: 'get' })
        if (this.revision === revision) this.plan = data
      } catch (err) {
        if (this.revision === revision) this.error = err.message || '缺料数据加载失败，请重试。'
      } finally {
        if (this.revision === revision) this.loading = false
      }
    },
    async confirmDraft() {
      if (this.busy || this.stale || !this.selected.length) return
      this.busy = true
      const revision = this.revision
      try {
        const { data } = await request({ url: this.url + '/draft', method: 'post', data: {
          requestId: this.plan.requestId, lines: this.selected.map(({ stockId, version }) => ({ stockId, version }))
        } })
        if (this.revision !== revision) return
        this.$modal.msgSuccess('已生成调拨草稿 ' + data.transferCode)
        await this.refresh()
      } catch (err) {
        if (this.revision === revision) {
          this.stale = true
          this.error = (err.message || '确认结果暂时未知') + '。请刷新分析并核对关联调拨单。'
        }
      } finally { this.busy = false }
    },
    async explain() {
      const revision = this.revision
      try {
        await this.$confirm('将向已配置的模型发送前 12 种缺料的名称、数量及可调建议，用于解释本次分析。', '解读缺料依据')
      } catch (_) { return }
      if (revision !== this.revision || !this.visible) return
      this.explaining = true
      const rows = this.shortages.slice(0, 12).map(r => ({
        物料: r.itemName, 单位: r.unit, 需求: r.required, 已领: r.issued, 原料仓现存: r.onHand,
        缺口: r.missing, 建议调拨总量: Number((r.missing - r.uncovered).toFixed(2)), 待跟进: r.uncovered
      }))
      try {
        const { data } = await request({ url: '/mes/ai/analysis', method: 'post', timeout: 65000, data: {
          question: '请按物料解释缺口、可调拨方案和剩余待跟进数量。只解释当前建议，不承诺交期。', facts: JSON.stringify(rows)
        } })
        if (revision === this.revision) this.explanation = data.text
      } catch (err) {
        if (revision === this.revision) this.explanation = err.message || '模型暂不可用，可继续按上表处理。'
      } finally { if (revision === this.revision) this.explaining = false }
    },
    openDocument(doc) {
      this.visible = false
      this.$router.push({ path: '/mes/wm/transfer', query: { openTransferId: String(doc.transferId) } })
    }
  }
}
</script>

<style scoped>
.shortage-panel { min-height: 90px; }
.shortage-panel h3, .shortage-panel h4 { margin: 0 0 8px; }
.shortage-panel p { line-height: 1.6; margin: 6px 0 10px; }
.shortage-section { margin-top: 18px; }
.shortage-section .el-tag { margin-left: 8px; }
.shortage-explanation { white-space: pre-line; }
</style>

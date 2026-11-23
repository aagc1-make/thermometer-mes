<template>
  <el-dialog :title="title" :visible.sync="dialogVisible" width="1120px" append-to-body @closed="onClosed">
    <div v-loading="loading" class="linkage-doc">
      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

      <template v-if="docType === 'ARRIVAL' && header">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="通知单编号">{{ header.noticeCode }}</el-descriptions-item>
          <el-descriptions-item label="单据状态">{{ businessStatus(header.status) }}</el-descriptions-item>
          <el-descriptions-item label="到货日期">{{ formatDate(header.arrivalDate) }}</el-descriptions-item>
          <el-descriptions-item label="供应商">{{ header.vendorName }}</el-descriptions-item>
          <el-descriptions-item label="采购订单">{{ text(header.poCode) }}</el-descriptions-item>
          <el-descriptions-item label="通知单名称">{{ text(header.noticeName) }}</el-descriptions-item>
        </el-descriptions>

        <h4 class="linkage-title">到货明细</h4>
        <el-table :data="arrivalLines" size="mini" border>
          <el-table-column label="物料" min-width="180">
            <template slot-scope="scope">{{ scope.row.itemCode }} / {{ scope.row.itemName }}</template>
          </el-table-column>
          <el-table-column label="到货数量" width="90" align="center" prop="quantityArrival" />
          <el-table-column label="合格数量" width="90" align="center" prop="quantityQuanlified" />
          <el-table-column label="检验状态" width="100" align="center">
            <template slot-scope="scope">
              <el-tag :type="inspectionStatusType(inspectionStatus(scope.row))" size="mini">
                {{ inspectionStatus(scope.row) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="来料检验单" min-width="150">
            <template slot-scope="scope">
              <span v-if="scope.row.iqcCheck !== 'Y'">免检</span>
              <span v-else-if="lineIqcs(scope.row).length === 0">待检</span>
              <span v-else>
                <el-button
                  v-for="iqc in lineIqcs(scope.row)"
                  :key="iqc.iqcId || iqc.iqcCode"
                  type="text"
                  size="mini"
                  @click="openChild('IQC', iqc.iqcId)"
                >{{ iqc.iqcCode }}</el-button>
              </span>
            </template>
          </el-table-column>
        </el-table>

        <h4 class="linkage-title">关联采购入库单</h4>
        <el-table v-if="receipts.length" :data="receipts" size="mini" border>
          <el-table-column label="入库单编号" min-width="160">
            <template slot-scope="scope">
              <el-button type="text" size="mini" @click="openChild('RECPT', scope.row.recptId)">
                {{ scope.row.recptCode }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="入库单名称" min-width="180" prop="recptName" />
          <el-table-column label="状态" width="90" align="center">
            <template slot-scope="scope">{{ businessStatus(scope.row.status) }}</template>
          </el-table-column>
          <el-table-column label="入库日期" width="110" align="center">
            <template slot-scope="scope">{{ formatDate(scope.row.recptDate) }}</template>
          </el-table-column>
        </el-table>
        <div v-else class="linkage-empty">暂无关联入库单</div>
      </template>

      <template v-else-if="docType === 'IQC' && header">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="检验单编号">{{ header.iqcCode }}</el-descriptions-item>
          <el-descriptions-item label="单据状态">{{ businessStatus(header.status) }}</el-descriptions-item>
          <el-descriptions-item label="检测结论">{{ businessStatus(header.checkResult) }}</el-descriptions-item>
          <el-descriptions-item label="接收数量">{{ header.quantityRecived }}</el-descriptions-item>
          <el-descriptions-item label="检测数量">{{ header.quantityCheck }}</el-descriptions-item>
          <el-descriptions-item label="合格 / 不合格">{{ header.quantityQualified }} / {{ header.quantityUnqualified }}</el-descriptions-item>
        </el-descriptions>

        <h4 class="linkage-title">来源单据</h4>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="来源到货通知">
            <el-button
              v-if="header.sourceDocType === 'ARRIVAL_NOTICE' && header.sourceDocId"
              type="text"
              size="mini"
              @click="openChild('ARRIVAL', header.sourceDocId)"
            >{{ header.sourceDocCode }}</el-button>
            <span v-else>{{ text(header.sourceDocCode) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="来源物料行">
            <span v-if="sourceLine">
              {{ sourceLine.itemCode }} / {{ sourceLine.itemName }}（到货 {{ sourceLine.quantityArrival }}，来源行 #{{ sourceLine.lineId }}）
            </span>
            <span v-else>未关联</span>
          </el-descriptions-item>
        </el-descriptions>

        <h4 class="linkage-title">关联采购入库单</h4>
        <el-table v-if="receipts.length" :data="receipts" size="mini" border>
          <el-table-column label="入库单编号" min-width="160">
            <template slot-scope="scope">
              <el-button type="text" size="mini" @click="openChild('RECPT', scope.row.recptId)">
                {{ scope.row.recptCode }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="入库单名称" min-width="180" prop="recptName" />
          <el-table-column label="状态" width="90" align="center">
            <template slot-scope="scope">{{ businessStatus(scope.row.status) }}</template>
          </el-table-column>
          <el-table-column label="入库日期" width="110" align="center">
            <template slot-scope="scope">{{ formatDate(scope.row.recptDate) }}</template>
          </el-table-column>
        </el-table>
        <div v-else class="linkage-empty">暂无关联入库单</div>

        <template v-if="iqcLines.length">
          <h4 class="linkage-title">检验项目</h4>
          <el-table :data="iqcLines" size="mini" border>
            <el-table-column label="项目编码" prop="indexCode" width="120" />
            <el-table-column label="项目名称" prop="indexName" />
            <el-table-column label="标准值" prop="standerVal" width="140" />
            <el-table-column label="单位" prop="unitName" width="80" />
          </el-table>
        </template>
      </template>

      <template v-else-if="docType === 'RECPT' && header">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="入库单编号">{{ header.recptCode }}</el-descriptions-item>
          <el-descriptions-item label="单据状态">{{ businessStatus(header.status) }}</el-descriptions-item>
          <el-descriptions-item label="入库日期">{{ formatDate(header.recptDate) }}</el-descriptions-item>
          <el-descriptions-item label="供应商">{{ header.vendorName }}</el-descriptions-item>
          <el-descriptions-item label="采购订单">{{ text(header.poCode) }}</el-descriptions-item>
          <el-descriptions-item label="来源到货通知">
            <el-button
              v-if="header.noticeId"
              type="text"
              size="mini"
              @click="openChild('ARRIVAL', header.noticeId)"
            >{{ header.noticeCode }}</el-button>
            <span v-else>未关联</span>
          </el-descriptions-item>
        </el-descriptions>

        <h4 class="linkage-title">入库明细</h4>
        <el-table :data="recptLines" size="mini" border>
          <el-table-column label="物料" min-width="180">
            <template slot-scope="scope">{{ scope.row.itemCode }} / {{ scope.row.itemName }}</template>
          </el-table-column>
          <el-table-column label="入库数量" width="90" align="center" prop="quantityRecived" />
          <el-table-column label="批次号" width="130" align="center" prop="batchCode" />
          <el-table-column label="来源到货行" width="110" align="center">
            <template slot-scope="scope">{{ scope.row.noticeLineId || '未关联' }}</template>
          </el-table-column>
          <el-table-column label="检验状态" width="100" align="center">
            <template slot-scope="scope">
              <el-tag :type="inspectionStatusType(inspectionStatus(scope.row))" size="mini">
                {{ inspectionStatus(scope.row) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="来料检验单" min-width="150">
            <template slot-scope="scope">
              <span v-if="scope.row.iqcCheck !== 'Y'">免检</span>
              <span v-else-if="lineIqcs(scope.row).length === 0">未关联</span>
              <span v-else>
                <el-button
                  v-for="iqc in lineIqcs(scope.row)"
                  :key="iqc.iqcId || iqc.iqcCode"
                  type="text"
                  size="mini"
                  @click="openChild('IQC', iqc.iqcId)"
                >{{ iqc.iqcCode }}</el-button>
              </span>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <div v-else-if="!loading" class="linkage-empty">暂无可展示的关联单据</div>
    </div>

    <div slot="footer">
      <el-button @click="dialogVisible = false">关 闭</el-button>
    </div>

    <linkage-doc-dialog
      v-if="child.visible"
      :visible.sync="child.visible"
      :doc-type="child.type"
      :doc-id="child.id"
    />
  </el-dialog>
</template>

<script>
import { getArrivalnotice } from '@/api/mes/wm/arrivalnotice'
import { listArrivalnoticeline, getArrivalnoticeline } from '@/api/mes/wm/arrivalnoticeline'
import { getItemrecpt, listItemrecpt } from '@/api/mes/wm/itemrecpt'
import { listItemrecptline } from '@/api/mes/wm/itemrecptline'
import { getIqc, listIqc } from '@/api/mes/qc/iqc'
import { listIqcline } from '@/api/mes/qc/iqcline'
import {
  text, formatDate, businessStatus, mergeIqcLinks, inspectionStatus, inspectionStatusType
} from '@/utils/linkage'

export default {
  name: 'LinkageDocDialog',
  props: {
    visible: { type: Boolean, default: false },
    docType: { type: String, default: '' },
    docId: { type: [String, Number], default: null }
  },
  data() {
    return {
      dialogVisible: false,
      loading: false,
      loadError: '',
      header: null,
      arrivalLines: [],
      receipts: [],
      recptLines: [],
      iqcLines: [],
      sourceLine: null,
      child: { visible: false, type: '', id: null }
    }
  },
  computed: {
    title() {
      if (this.docType === 'ARRIVAL') return '到货通知详情' + (this.header && this.header.noticeCode ? ' - ' + this.header.noticeCode : '')
      if (this.docType === 'IQC') return '来料检验详情' + (this.header && this.header.iqcCode ? ' - ' + this.header.iqcCode : '')
      if (this.docType === 'RECPT') return '采购入库详情' + (this.header && this.header.recptCode ? ' - ' + this.header.recptCode : '')
      return '关联单据'
    }
  },
  watch: {
    visible: {
      immediate: true,
      handler(value) {
        this.dialogVisible = !!value
        if (value) this.load()
      }
    },
    dialogVisible(value) {
      this.$emit('update:visible', value)
    },
    docType() {
      if (this.dialogVisible) this.load()
    },
    docId() {
      if (this.dialogVisible) this.load()
    }
  },
  methods: {
    text,
    formatDate,
    businessStatus,
    inspectionStatus,
    inspectionStatusType,
    lineIqcs(row) {
      return row.iqcLinks || mergeIqcLinks(row, [])
    },
    async load() {
      this.loading = true
      this.loadError = ''
      this.header = null
      this.arrivalLines = []
      this.receipts = []
      this.recptLines = []
      this.iqcLines = []
      this.sourceLine = null
      this.child.visible = false
      try {
        if (this.docId == null || this.docId === '') return
        if (this.docType === 'ARRIVAL') {
          await this.loadArrival()
        } else if (this.docType === 'IQC') {
          await this.loadIqc()
        } else if (this.docType === 'RECPT') {
          await this.loadReceipt()
        }
      } catch (e) {
        this.loadError = '关联单据加载失败：' + (e && e.message ? e.message : e)
      } finally {
        this.loading = false
      }
    },
    async loadArrival() {
      const [notice, lines, receiptRes] = await Promise.all([
        getArrivalnotice(this.docId),
        listArrivalnoticeline({ noticeId: this.docId, pageSize: 200 }),
        listItemrecpt({ noticeId: this.docId, pageSize: 100 })
      ])
      this.header = notice.data || null
      this.arrivalLines = lines.rows || []
      this.receipts = receiptRes.rows || []
      await Promise.all(this.arrivalLines.map(line => this.loadLineIqcs(line)))
    },
    async loadLineIqcs(line) {
      if (!line || line.iqcCheck !== 'Y' || !line.lineId) {
        this.$set(line, 'iqcLinks', mergeIqcLinks(line, []))
        return
      }
      try {
        const res = await listIqc({ sourceLineId: line.lineId, pageSize: 50 })
        this.$set(line, 'iqcLinks', mergeIqcLinks(line, res.rows || []))
      } catch (e) {
        this.$set(line, 'iqcLinks', mergeIqcLinks(line, []))
      }
    },
    async loadIqc() {
      const [iqc, lines] = await Promise.all([
        getIqc(this.docId),
        listIqcline({ iqcId: this.docId, pageSize: 200 })
      ])
      this.header = iqc.data || null
      this.iqcLines = lines.rows || []
      if (!this.header) return
      const tasks = []
      if (this.header.sourceLineId) {
        tasks.push(getArrivalnoticeline(this.header.sourceLineId).then(res => { this.sourceLine = res.data || null }).catch(() => { this.sourceLine = null }))
      }
      if (this.header.sourceDocType === 'ARRIVAL_NOTICE' && this.header.sourceDocId) {
        tasks.push(listItemrecpt({ noticeId: this.header.sourceDocId, pageSize: 100 }).then(res => { this.receipts = res.rows || [] }).catch(() => { this.receipts = [] }))
      }
      await Promise.all(tasks)
    },
    async loadReceipt() {
      const [recpt, lines] = await Promise.all([
        getItemrecpt(this.docId),
        listItemrecptline({ recptId: this.docId, pageSize: 200 })
      ])
      this.header = recpt.data || null
      this.recptLines = lines.rows || []
      await Promise.all(this.recptLines.map(line => this.loadReceiptLineIqcs(line)))
    },
    async loadReceiptLineIqcs(line) {
      if (!line || line.iqcCheck !== 'Y' || !line.noticeLineId) {
        this.$set(line, 'iqcLinks', mergeIqcLinks(line, []))
        return
      }
      try {
        const res = await listIqc({ sourceLineId: line.noticeLineId, pageSize: 50 })
        this.$set(line, 'iqcLinks', mergeIqcLinks(line, res.rows || []))
      } catch (e) {
        this.$set(line, 'iqcLinks', mergeIqcLinks(line, []))
      }
    },
    openChild(type, id) {
      if (id == null || id === '') return
      this.child.type = type
      this.child.id = id
      this.child.visible = true
    },
    onClosed() {
      this.child.visible = false
      this.header = null
    }
  }
}
</script>

<style scoped>
.linkage-doc { min-height: 120px; }
.linkage-title { margin: 18px 0 8px; font-size: 14px; color: #303133; }
.linkage-empty { margin: 8px 0 12px; color: #909399; font-size: 13px; }
</style>

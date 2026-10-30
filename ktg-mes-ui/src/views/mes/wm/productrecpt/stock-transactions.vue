<template>
  <div>
    <el-alert v-if="!canView" title="当前账号没有库存流水查询权限" type="info" :closable="false" />
    <template v-else>
      <el-alert v-if="error" title="库存流水加载失败，请稍后重试" type="error" :closable="false" />
      <el-table v-loading="loading" :data="rows" :empty-text="emptyText" size="small">
        <el-table-column label="物料名称" prop="itemName" min-width="120" show-overflow-tooltip />
        <el-table-column label="仓库" prop="warehouseName" min-width="100" />
        <el-table-column label="库区" prop="locationName" min-width="100" />
        <el-table-column label="库位" prop="areaName" min-width="100" />
        <el-table-column label="批次号" prop="batchCode" min-width="130" />
        <el-table-column label="变动方向" width="95" align="center">
          <template slot-scope="scope">{{ scope.row.transactionFlag > 0 ? '入库' : '出库' }}</template>
        </el-table-column>
        <el-table-column label="变动数量" width="110" align="center">
          <template slot-scope="scope">{{ Math.abs(scope.row.transactionQuantity) }} {{ scope.row.unitName }}</template>
        </el-table-column>
        <el-table-column label="变动时间" prop="transactionDate" min-width="160" />
      </el-table>
      <pagination v-show="total > 0" :total="total" :page.sync="page" :limit.sync="pageSize" @pagination="load" />
    </template>
  </div>
</template>

<script>
import { listTransaction } from '@/api/mes/wm/transaction'

export default {
  name: 'ProductReceiptStockTransactions',
  props: { recptId: { type: [Number, String], required: true }, status: String },
  data() {
    return { rows: [], total: 0, page: 1, pageSize: 10, loading: false, error: false }
  },
  computed: {
    canView() { return this.$auth.hasPermi('mes:wm:transaction:list') },
    emptyText() { return this.error ? '加载失败' : (this.status === 'FINISHED' ? '暂无关联库存流水' : '尚未执行，暂无库存流水') }
  },
  created() { if (this.canView) this.load() },
  methods: {
    load() {
      this.loading = true
      this.error = false
      listTransaction({ pageNum: this.page, pageSize: this.pageSize, sourceDocType: 'PRODUCT_RECPT', sourceDocId: this.recptId })
        .then(res => { this.rows = res.rows || []; this.total = res.total || 0 })
        .catch(() => { this.rows = []; this.total = 0; this.error = true })
        .finally(() => { this.loading = false })
    }
  }
}
</script>

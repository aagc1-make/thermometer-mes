<template>
  <el-dialog
    title="选择工单成品"
    v-if="showFlag"
    :visible.sync="showFlag"
    width="82%"
    center
    append-to-body
  >
    <div class="wo-product__tip">
      <i class="el-icon-info"></i>
      只列已执行产品入库的工单成品；可售 = 已入库 − 已开单（含草稿与在途），选定后带出产品、批次与可售数量。
    </div>

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="工单编号" prop="workorderCodeLike">
        <el-input
          v-model="queryParams.workorderCodeLike"
          placeholder="请输入工单编号"
          clearable
          style="width: 200px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="产品编码" prop="itemCodeLike">
        <el-input
          v-model="queryParams.itemCodeLike"
          placeholder="请输入产品编码"
          clearable
          style="width: 200px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="产品名称" prop="itemNameLike">
        <el-input
          v-model="queryParams.itemNameLike"
          placeholder="请输入产品名称"
          clearable
          style="width: 200px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table
      v-loading="loading"
      :data="workorderProductList"
      height="420"
      highlight-current-row
      @current-change="handleCurrent"
      @row-dblclick="handleRowDbClick"
    >
      <el-table-column width="50" align="center">
        <template v-slot="scope">
          <el-radio
            v-model="selectedWorkorderId"
            :label="scope.row.workorderId"
            @change="handleRowChange(scope.row)"
          >{{ "" }}</el-radio>
        </template>
      </el-table-column>
      <el-table-column label="工单编号" width="190" align="center" prop="workorderCode" :show-overflow-tooltip="true" />
      <el-table-column label="产品编码" width="110" align="center" prop="itemCode" />
      <el-table-column label="产品名称" min-width="140" align="left" prop="itemName" :show-overflow-tooltip="true" />
      <el-table-column label="规格型号" min-width="130" align="left" prop="specification" :show-overflow-tooltip="true" />
      <el-table-column label="单位" width="60" align="center" prop="unitName" />
      <el-table-column label="已入库" width="80" align="center" prop="receivedQuantity" />
      <el-table-column label="已开单" width="80" align="center" prop="issuedQuantity" />
      <el-table-column label="可售" width="75" align="center" prop="availableQuantity">
        <template slot-scope="scope">
          <span class="wo-product__available">{{ scope.row.availableQuantity }}</span>
        </template>
      </el-table-column>
      <el-table-column label="批次" width="120" align="center" prop="batchCode">
        <template slot-scope="scope">
          <span>{{ scope.row.batchCode || '多批次/待拣货' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="最近入库" width="150" align="center" prop="recptCode" :show-overflow-tooltip="true" />
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <div slot="footer" class="dialog-footer">
      <el-button type="primary" @click="confirmSelect">确 定</el-button>
      <el-button @click="showFlag = false">取 消</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { listWorkorderProducts } from '@/api/mes/wm/productsales'

export default {
  name: 'WorkorderProductSelect',
  props: {
    // 编辑已有出库行时排除自身占用，避免"自己把自己的数量算成已开单"
    excludeLineId: { type: [Number, String], default: null }
  },
  data() {
    return {
      showFlag: false,
      loading: false,
      showSearch: true,
      total: 0,
      workorderProductList: [],
      selectedWorkorderId: undefined,
      selectedRow: undefined,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        workorderCodeLike: null,
        itemCodeLike: null,
        itemNameLike: null,
        onlyAvailable: 'Y',
        excludeLineId: null
      }
    }
  },
  methods: {
    handleOpen() {
      this.showFlag = true
      this.selectedWorkorderId = undefined
      this.selectedRow = undefined
      this.queryParams.pageNum = 1
      this.queryParams.excludeLineId = this.excludeLineId
      this.getList()
    },
    getList() {
      this.loading = true
      listWorkorderProducts(this.queryParams).then(response => {
        this.workorderProductList = response.rows || []
        this.total = Number(response.total) || 0
        this.loading = false
      }).catch(() => {
        this.workorderProductList = []
        this.total = 0
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleCurrent(row) {
      if (!row) return
      this.selectedWorkorderId = row.workorderId
      this.selectedRow = row
    },
    handleRowChange(row) {
      this.selectedRow = row
    },
    handleRowDbClick(row) {
      this.selectedRow = row
      this.selectedWorkorderId = row.workorderId
      this.confirmSelect()
    },
    confirmSelect() {
      if (!this.selectedRow) {
        this.$modal.msgWarning('请选择一条工单成品')
        return
      }
      this.$emit('onSelected', this.selectedRow)
      this.showFlag = false
    }
  }
}
</script>

<style lang="scss" scoped>
.wo-product__tip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  margin-bottom: 12px;
  font-size: 12px;
  color: #7a8498;
  background: #f5f7fb;
  border-radius: 6px;

  i {
    color: #5e75ff;
  }
}

.wo-product__available {
  font-weight: 600;
  color: #31946a;
}
</style>

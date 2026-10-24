<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="82px">
      <el-form-item label="物料编码" prop="itemCode">
        <el-input v-model="queryParams.itemCode" placeholder="请输入物料编码" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="物料名称" prop="itemName">
        <el-input v-model="queryParams.itemName" placeholder="请输入物料名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="批次号" prop="batchCode">
        <el-input v-model="queryParams.batchCode" placeholder="请输入批次号" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="仓库" prop="warehouseCode">
        <el-select v-model="queryParams.warehouseCode" placeholder="请选择仓库" clearable>
          <el-option
            v-for="w in warehouseOptions"
            :key="w.warehouseCode"
            :label="w.warehouseName"
            :value="w.warehouseCode"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="单据类型" prop="transactionType">
        <el-select v-model="queryParams.transactionType" placeholder="请选择单据类型" clearable>
          <el-option
            v-for="(label, value) in typeMap"
            :key="value"
            :label="label"
            :value="value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="单据编号" prop="sourceDocCode">
        <el-input v-model="queryParams.sourceDocCode" placeholder="请输入来源单据编号" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="变动日期">
        <el-date-picker
          v-model="dateRange"
          style="width: 240px"
          value-format="yyyy-MM-dd HH:mm:ss"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :default-time="['00:00:00', '23:59:59']"
        ></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['mes:wm:transaction:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="transactionList">
      <el-table-column label="流水号" align="center" prop="transactionId" width="90" />
      <el-table-column label="单据类型" align="center" prop="transactionType" width="130">
        <template slot-scope="scope">
          <el-tag :type="scope.row.transactionFlag > 0 ? 'success' : 'warning'" size="mini">
            {{ typeMap[scope.row.transactionType] || scope.row.transactionType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="物料编码" align="center" prop="itemCode" width="120" />
      <el-table-column label="物料名称" align="center" prop="itemName" show-overflow-tooltip />
      <el-table-column label="规格" align="center" prop="specification" show-overflow-tooltip />
      <el-table-column label="批次号" align="center" prop="batchCode" width="130" />
      <el-table-column label="仓库" align="center" prop="warehouseName" width="100" />
      <el-table-column label="库区" align="center" prop="locationName" width="110" />
      <el-table-column label="库位" align="center" prop="areaName" width="110" />
      <el-table-column label="变动方向" align="center" prop="transactionFlag" width="90">
        <template slot-scope="scope">
          <span :style="{ color: scope.row.transactionFlag > 0 ? '#13ce66' : '#ff4949' }">
            {{ scope.row.transactionFlag > 0 ? '入 +' : '出 -' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="变动数量" align="center" prop="transactionQuantity" width="100">
        <template slot-scope="scope">
          {{ Math.abs(scope.row.transactionQuantity) }} {{ scope.row.unitName }}
        </template>
      </el-table-column>
      <el-table-column label="来源单据" align="center" prop="sourceDocCode" width="160" show-overflow-tooltip />
      <el-table-column label="变动时间" align="center" prop="transactionDate" width="160" />
      <el-table-column label="操作" align="center" width="90" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleView(scope.row)"
            v-hasPermi="['mes:wm:transaction:query']"
          >详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog title="库存流水详情" :visible.sync="open" width="720px" append-to-body>
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="流水号">{{ detail.transactionId }}</el-descriptions-item>
        <el-descriptions-item label="单据类型">{{ typeMap[detail.transactionType] || detail.transactionType }}</el-descriptions-item>
        <el-descriptions-item label="物料编码">{{ detail.itemCode }}</el-descriptions-item>
        <el-descriptions-item label="物料名称">{{ detail.itemName }}</el-descriptions-item>
        <el-descriptions-item label="规格">{{ detail.specification }}</el-descriptions-item>
        <el-descriptions-item label="批次号">{{ detail.batchCode }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detail.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="库区 / 库位">{{ detail.locationName }} / {{ detail.areaName }}</el-descriptions-item>
        <el-descriptions-item label="变动数量">{{ detail.transactionQuantity }} {{ detail.unitName }}</el-descriptions-item>
        <el-descriptions-item label="变动方向">{{ detail.transactionFlag > 0 ? '入库' : '出库' }}</el-descriptions-item>
        <el-descriptions-item label="来源单据">{{ detail.sourceDocCode }}</el-descriptions-item>
        <el-descriptions-item label="关联流水">{{ detail.relatedTransactionId }}</el-descriptions-item>
        <el-descriptions-item label="变动时间" :span="2">{{ detail.transactionDate }}</el-descriptions-item>
      </el-descriptions>
      <div slot="footer" class="dialog-footer">
        <el-button @click="open = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listTransaction, getTransaction, exportTransaction } from "@/api/mes/wm/transaction";
import { listWarehouse } from "@/api/mes/wm/warehouse";

export default {
  name: "Transaction",
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      transactionList: [],
      warehouseOptions: [],
      dateRange: [],
      open: false,
      detail: null,
      // 库存事务类型（对应后端 transaction_type）
      typeMap: {
        ITEM_RECPT: '采购入库',
        ITEM_ISSUE_OUT: '生产领料出库',
        ITEM_ISSUE_IN: '生产领料入线边库',
        ITEM_CONSUME: '生产消耗',
        PRODUCT_PRODUCE: '产品产出',
        PRODUCT_RECPT_OUT: '成品入库出线边库',
        PRODUCT_RECPT_IN: '成品入库入成品仓',
        PRODUCT_SALSE: '销售出库',
        RT_VENDOR: '采购退货',
        RT_ISSUE: '生产退料',
        RT_SALES: '销售退货',
        TRANSFER: '库存转移',
        OUTSOURCE_ISSUE: '外协发料',
        OUTSOURCE_RECPT: '外协入库',
        MISC_RECPT: '杂项入库',
        MISC_ISSUE: '杂项出库',
        STOCK_TAKING: '盘点调整'
      },
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        itemCode: null,
        itemName: null,
        batchCode: null,
        warehouseCode: null,
        transactionType: null,
        sourceDocCode: null
      }
    };
  },
  created() {
    this.getList();
    listWarehouse({ pageNum: 1, pageSize: 100 }).then(res => {
      this.warehouseOptions = res.rows || [];
    });
  },
  methods: {
    getList() {
      this.loading = true;
      this.queryParams.params = {};
      if (this.dateRange && this.dateRange.length === 2) {
        this.queryParams.params["beginTime"] = this.dateRange[0];
        this.queryParams.params["endTime"] = this.dateRange[1];
      }
      listTransaction(this.queryParams).then(res => {
        this.transactionList = res.rows;
        this.total = res.total;
        this.loading = false;
      }).catch(() => { this.loading = false; });
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.dateRange = [];
      this.queryParams.params = {};
      this.resetForm("queryForm");
      this.handleQuery();
    },
    handleView(row) {
      getTransaction(row.transactionId).then(res => {
        this.detail = res.data;
        this.open = true;
      });
    },
    handleExport() {
      this.queryParams.params = {};
      if (this.dateRange && this.dateRange.length === 2) {
        this.queryParams.params["beginTime"] = this.dateRange[0];
        this.queryParams.params["endTime"] = this.dateRange[1];
      }
      this.download('mes/wm/transaction/export', {
        ...this.queryParams
      }, `库存流水_${new Date().getTime()}.xlsx`);
    }
  }
};
</script>

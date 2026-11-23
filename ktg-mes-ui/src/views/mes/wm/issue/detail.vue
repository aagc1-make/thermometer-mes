<template>
  <div class="app-container issue-materials">
    <div class="issue-materials__heading">
      <div><strong>领料明细</strong><span>批次、仓库、库位和数量由库存自动匹配</span></div>
      <span>{{ total }} 项物料</span>
    </div>

    <el-table v-loading="loading" :data="issuelineList" @selection-change="handleSelectionChange">
      <el-table-column type="expand" style="overflow-x: auto; width: 100%;">
        <template slot-scope="props">
          <el-table :data="props.row.details" class="child-table" style="margin-left: 60px;">            
            <el-table-column label="批次号" width="120px" align="center" prop="batchCode" />
            <el-table-column label="仓库名称" width="120px" align="center" prop="warehouseName" />
            <el-table-column label="库区名称" width="120px" align="center" prop="locationName" />
            <el-table-column label="库位名称" width="150px" align="center" prop="areaName" />  
            <el-table-column label="数量" width="120px" align="center" prop="quantity" />            
            <el-table-column label="操作" align="center" width="100px" v-if="canEdit" class-name="small-padding fixed-width">
              <template slot-scope="scope">
                <el-button
                  size="mini"
                  type="text"
                  icon="el-icon-edit"
                  @click="handleUpdate(scope.row)"
                  v-hasPermi="['mes:wm:issueheader:edit']"
                >修改</el-button>
                <el-button
                  size="mini"
                  type="text"
                  icon="el-icon-delete"
                  @click="handleDelete(scope.row)"
                  v-hasPermi="['mes:wm:issueheader:remove']"
                >删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </el-table-column>
      <el-table-column label="物料" min-width="150" prop="itemCode">
        <template slot-scope="scope"><strong class="issue-item-name">{{ scope.row.itemName }}</strong><span class="issue-item-code">{{ scope.row.itemCode }}</span></template>
      </el-table-column>
      <el-table-column label="规格型号" min-width="100" prop="specification" :show-overflow-tooltip="true"/>
      <el-table-column label="需领" align="right" width="75" prop="quantityIssued" />
      <el-table-column label="已拣" align="right" width="75">
        <template slot-scope="scope">{{ pickedQuantity(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="待拣" align="right" width="75">
        <template slot-scope="scope"><span :class="{ 'issue-remaining': remainingQuantity(scope.row) > 0 }">{{ remainingQuantity(scope.row) }}</span></template>
      </el-table-column>
      <el-table-column label="单位" align="center" width="55" prop="unitName" />
      <el-table-column label="分配结果 · 批次 / 库位" min-width="210">
        <template slot-scope="scope">
          <div v-for="detail in scope.row.details" :key="detail.detailId" class="issue-allocation">
            <span>{{ detail.batchCode }} · {{ detail.quantity }}{{ scope.row.unitName }}</span>
            <small>{{ detail.warehouseName }} / {{ detail.locationName }} / {{ detail.areaName }}</small>
          </div>
          <span v-if="!scope.row.details || !scope.row.details.length" class="issue-item-code">等待一键分配</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="85">
        <template slot-scope="scope"><el-tag size="mini" :type="pickedQuantity(scope.row) > Number(scope.row.quantityIssued) ? 'danger' : remainingQuantity(scope.row) === 0 ? 'success' : 'info'">{{ pickedQuantity(scope.row) > Number(scope.row.quantityIssued) ? '已超拣' : remainingQuantity(scope.row) === 0 ? '已拣齐' : '待拣货' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="调整" align="center" v-if="canEdit" width="85" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-shopping-cart-full"
            @click="handleAdd(scope.row)"
            :disabled="remainingQuantity(scope.row) <= 0"
            v-hasPermi="['mes:wm:issueheader:edit']"
          >手工拣货</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getListWithDetail"
    />

    <!-- 添加或修改生产领料单明细对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8">
              <el-form-item label="物资编码" prop="itemCode">
                  <el-input v-model="form.itemCode" readonly="readonly" placeholder="请选择产品物料编码" >
                  </el-input>
              </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="物资名称" prop="itemName">
              <el-input v-model="form.itemName" readonly="readonly"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位" prop="unitName">
              <el-input v-model="form.unitName" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>       
        <el-row>
          <el-col :span="24">
            <el-form-item label="规格型号" prop="specification">
              <el-input v-model="form.specification" readonly="readonly" />
            </el-form-item>
          </el-col>          
        </el-row>
        <el-row>
            <el-col :span="8">
              <el-form-item label="仓库" prop="warehouseName">
                <el-input v-model="form.warehouseName" readonly="readonly" >
                  <el-button slot="append" @click="handleSelectStock" icon="el-icon-search"></el-button>
                </el-input>
              </el-form-item>
              <StockSelect ref="stockSelect" :itemId="form.itemId" @onSelected="onStockSelected"></StockSelect>
            </el-col>
            <el-col :span="8">
              <el-form-item label="库区" prop="locationName">
                <el-input v-model="form.locationName" readonly="readonly" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="库位" prop="areaName">
                <el-input v-model="form.areaName" readonly="readonly" />
              </el-form-item>
            </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="批次号" prop="batchCode">
              <el-input v-model="form.batchCode" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="数量" prop="quantity">
              <el-input-number v-model="form.quantity" :max="form.quantityMax" :min="0" :precision="4" placeholder="自动带入待拣数量" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listIssuedetail, getIssuedetail, delIssuedetail, addIssuedetail, updateIssuedetail } from "@/api/mes/wm/issuedetail";
import { listWithDetail } from "@/api/mes/wm/issueline";
import StockSelect from "@/components/stockSelect/single.vue"
export default {
  name: "Issuedetail",
  components:{ StockSelect},
  props:{
    issueId: null,
    optType: null,
    status: { type: String, default: 'APPROVING' },
  },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 生产领料单行表格数据（带明细）
      issuelineList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        issueId: this.issueId,        lineId: null,        itemId: null,        itemCode: null,        itemName: null,        specification: null,        unitOfMeasure: null,        unitName: null },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        itemCode: [
          { required: true, message: "物资不能为空", trigger: "blur" }
        ],        quantity: [
          { required: true, message: "数量不能为空", trigger: "blur" }
        ],      }
    };
  },
  computed: {
    canEdit() { return this.optType !== 'view' && this.status === 'APPROVING'; }
  },
  watch: {
    issueId(value) {
      this.queryParams.issueId = value;
      this.queryParams.pageNum = 1;
      this.open = false;
      this.getListWithDetail();
    },
    canEdit(value) { if (!value) this.open = false; }
  },
  created() {
    this.getListWithDetail();
  },
  methods: {
    /** 查询生产领料单明细列表 */
    getListWithDetail() {
      this.loading = true;
      const issueId = this.issueId;
      return listWithDetail(this.queryParams).then(response => {
        if (issueId !== this.issueId) return;
        this.issuelineList = response.rows;
        this.total = response.total;
      }).finally(() => {
        if (issueId === this.issueId) this.loading = false;
      });
    },
    pickedQuantity(row, excludeDetailId) {
      return Number((row.details || []).reduce((sum, detail) =>
        detail.detailId === excludeDetailId && excludeDetailId != null ? sum : sum + Number(detail.quantity || 0), 0).toFixed(4));
    },
    remainingQuantity(row, excludeDetailId) {
      return Math.max(0, Number((Number(row.quantityIssued || 0) - this.pickedQuantity(row, excludeDetailId)).toFixed(4)));
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        detailId: null,        issueId: this.issueId,        lineId: null,        itemId: null,        itemCode: null,        itemName: null,        specification: null,        unitOfMeasure: null,        unitName: null,        quantity: null,        batchId: null,        batchCode: null,        warehouseId: null,        warehouseCode: null,        warehouseName: null,        locationId: null,        locationCode: null,        locationName: null,        areaId: null,        areaCode: null,        areaName: null,        remark: null,        attr1: null,        attr2: null,        attr3: null,        attr4: null,        createBy: null,        createTime: null,        updateBy: null,        updateTime: null      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getListWithDetail();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.detailId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd(row) {
      this.reset();
      this.form.issueId = row.issueId
      this.form.lineId = row.lineId
      this.form.itemId = row.itemId
      this.form.itemCode = row.itemCode
      this.form.itemName = row.itemName
      this.form.specification = row.specification
      this.form.unitOfMeasure = row.unitOfMeasure
      this.form.unitName = row.unitName
      this.form.batchId = row.batchId
      this.form.batchCode = row.batchCode
      this.form.quantity = this.remainingQuantity(row)
      this.form.quantityMax = this.form.quantity
      this.open = true;
      this.title = "添加生产领料单明细";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const detailId = row.detailId || this.ids
      getIssuedetail(detailId).then(response => {
        this.form = response.data;
        const line = this.issuelineList.find(item => item.lineId === this.form.lineId);
        this.form.quantityMax = line ? this.remainingQuantity(line, this.form.detailId) : this.form.quantity;
        this.open = true;
        this.title = "修改生产领料单明细";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.detailId != null) {
            updateIssuedetail(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getListWithDetail();
            });
          } else {
            addIssuedetail(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getListWithDetail();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const detailIds = row.detailId || this.ids;
      this.$modal.confirm('是否确认删除生产领料单明细编号为"' + detailIds + '"的数据项？').then(function() {
        return delIssuedetail(detailIds);
      }).then(() => {
        this.getListWithDetail();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('wm/issuedetail/export', {
        ...this.queryParams
      }, `issuedetail_${new Date().getTime()}.xlsx`)
    },
    handleSelectStock(){
      this.$refs.stockSelect.handleOpen();
    },
        //物料选择弹出框
    onStockSelected(obj){
        if(obj != undefined && obj != null){
          this.form.materialStockId = obj.materialStockId;
          this.form.itemId = obj.itemId;
          this.form.itemCode = obj.itemCode;
          this.form.itemName = obj.itemName;
          this.form.specification = obj.specification;
          this.form.unitOfMeasure = obj.unitOfMeasure;
          this.form.unitName = obj.unitName;
          this.form.batchId = obj.batchId;
          this.form.batchCode = obj.batchCode;
          this.form.warehouseId = obj.warehouseId;
          this.form.warehouseCode = obj.warehouseCode;
          this.form.warehouseName = obj.warehouseName;
          this.form.locationId = obj.locationId;
          this.form.locationCode = obj.locationCode;
          this.form.locationName = obj.locationName;
          this.form.areaId = obj.areaId;
          this.form.areaCode = obj.areaCode;
          this.form.areaName = obj.areaName;
          const line = this.issuelineList.find(item => item.lineId === this.form.lineId);
          const remaining = line ? this.remainingQuantity(line, this.form.detailId) : 0;
          const quantity = Math.max(0, Math.min(Number(obj.quantityOnhand || 0), remaining));
          this.form.quantity = quantity;
          this.form.quantityMax = quantity;
        }
    },

  }
};
</script>
<style scoped>
.issue-materials { padding: 12px; }
.issue-materials__heading { display: flex; justify-content: space-between; align-items: center; padding: 0 0 18px; color: #8492a6; font-size: 12px; gap: 12px; }
.issue-materials__heading strong { color: #25344a; font-size: 15px; margin-right: 16px; }
.issue-item-name { display: block; color: #34435a; font-weight: 500; }
.issue-item-code { display: block; color: #8b98aa; font-size: 12px; }
.issue-remaining { color: #c27b24; font-weight: 600; }
.issue-allocation { margin: 3px 0; line-height: 1.6; font-size: 12px; }
.issue-allocation small { display: block; color: #8b98aa; font-size: 12px; }
::v-deep .child-table .el-table__header th {
  background-color: #D9DEF2; /* 浅蓝色 */
}
</style>

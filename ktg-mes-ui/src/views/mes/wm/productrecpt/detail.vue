<template>
    <div class="app-container">
        <el-alert
            v-if="status === 'APPROVING'"
            :title="'入库单 ' + (recptCode || '-') + ' · 待上架：请逐行分配库位并确认上架数量，全部上架后才能确认并过账入库。'"
            type="info" :closable="false" show-icon style="margin-bottom: 12px">
            <span v-if="workorderCode">本单由工单 {{ workorderCode }} 完工后自动生成，单号与工单同源；入库行批次已自动绑定。</span>
        </el-alert>
        <el-alert
            v-else-if="status === 'APPROVED'"
            :title="'入库单 ' + (recptCode || '-') + ' 已上架，核对无误后执行入库过账。'"
            type="success" :closable="false" show-icon style="margin-bottom: 12px" />
        <el-alert
            v-else-if="status === 'FINISHED'"
            :title="'入库单 ' + (recptCode || '-') + ' 已入库，库存与批次流水已同步。'"
            type="success" :closable="false" show-icon style="margin-bottom: 12px" />
        <el-row>
            <el-col :span="24" style="text-align: right;">
                <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">刷新</el-button>
            </el-col>
        </el-row>
        <el-table v-loading="loading" :data="productrecptlineList" @selection-change="handleSelectionChange">
            <el-table-column type="expand" style="overflow-x: auto; width: 100%;">
                <template slot-scope="props">
                <el-table :data="props.row.details" class="child-table" style="margin-left: 60px;">
                    <el-table-column label="仓库名称" width="120px" align="center" prop="warehouseName" />
                    <el-table-column label="库区名称" width="120px" align="center" prop="locationName" />
                    <el-table-column label="库位名称" width="150px" align="center" prop="areaName" />
                    <el-table-column label="数量" width="120px" align="center" prop="quantity" />
                    <el-table-column label="操作" align="center" width="100px" v-if="canStock" class-name="small-padding fixed-width">
                    <template slot-scope="scope">
                        <el-button
                        size="mini"
                        type="text"
                        icon="el-icon-edit"
                        @click="handleUpdate(scope.row, props.row)"
                        v-hasPermi="['mes:wm:productrecpt:edit']"
                        >修改</el-button>
                        <el-button
                        size="mini"
                        type="text"
                        icon="el-icon-delete"
                        @click="handleDelete(scope.row)"
                        v-hasPermi="['mes:wm:productrecpt:remove']"
                        >删除</el-button>
                    </template>
                    </el-table-column>
                </el-table>
                </template>
            </el-table-column>
            <el-table-column label="物资" width="120px" align="center" prop="itemCode" />
            <el-table-column label="物资名称" width="150px" align="center" prop="itemName" :show-overflow-tooltip="true"/>
            <el-table-column label="规格型号" width="120px" align="center" prop="specification" :show-overflow-tooltip="true"/>
            <el-table-column label="单位" align="center" prop="unitName" />
            <el-table-column label="入库数量" align="center" prop="quantityRecived" />
            <el-table-column label="已上架" align="center" width="90">
                <template slot-scope="scope">{{ formatQty(stockedOf(scope.row)) }}</template>
            </el-table-column>
            <el-table-column label="待上架" align="center" width="90">
                <template slot-scope="scope">
                    <span :style="{ color: remainOf(scope.row) > 0 ? '#E6A23C' : '#67C23A' }">{{ formatQty(remainOf(scope.row)) }}</span>
                </template>
            </el-table-column>
            <el-table-column label="批次号" align="center" prop="batchCode" />
            <el-table-column label="操作" align="center" width="100px" v-if="canStock" class-name="small-padding fixed-width">
                <template slot-scope="scope">
                <el-button
                    size="mini"
                    type="text"
                    icon="el-icon-shopping-cart-full"
                    :disabled="remainOf(scope.row) <= 0"
                    @click="handleAdd(scope.row)"
                    v-hasPermi="['mes:wm:productrecpt:add']"
                >上架</el-button>
                <printLabel :businessId="scope.row.batchId" :businessCode="scope.row.batchCode" labelText="标签打印" businessType = "BATCH"></printLabel>
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

    <!-- 上架（添加或修改产品入库明细）对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="1080px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="物资编码" prop="itemCode">
              <el-input v-model="form.itemCode" readonly="readonly" />
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
          <el-col :span="16">
            <el-form-item label="规格型号" prop="specification">
              <el-input v-model="form.specification" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="批次号" prop="batchCode">
              <el-input v-model="form.batchCode" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="入库仓库" prop="warehouseId">
              <el-select v-model="form.warehouseId" placeholder="请选择仓库" style="width: 100%" @change="handleWarehouseChange">
                <el-option v-for="w in warehouses" :key="w.warehouseId" :label="w.warehouseName" :value="w.warehouseId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="入库库区" prop="locationId">
              <el-select v-model="form.locationId" placeholder="请选择库区" style="width: 100%" @change="handleLocationChange">
                <el-option
                  v-for="l in locationOptions"
                  :key="l.locationId"
                  :value="l.locationId"
                  :label="locationLabel(l)">
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="入库库位" prop="areaId">
              <el-select v-model="form.areaId" placeholder="请选择库位" style="width: 100%" @change="handleAreaChange">
                <el-option
                  v-for="a in areaOptions"
                  :key="a.areaId"
                  :value="a.areaId"
                  :label="areaLabel(a)">
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="上架数量" prop="quantity">
              <el-input-number v-model="form.quantity" :min="0" :max="maxQuantity" placeholder="请输入上架数量" />
              <div class="qty-tip">本行入库 {{ formatQty(lineQty) }}，已上架 {{ formatQty(stockedQty) }}，待上架 {{ formatQty(lineRemain) }}</div>
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-alert v-if="currentAreaTip" :title="currentAreaTip" :type="currentAreaTipType" :closable="false" show-icon />
            <el-alert v-else-if="recommendTip" :title="recommendTip" type="success" :closable="false" show-icon />
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 入库单行查看界面 -->
    <el-dialog :title="linetitle" :visible.sync="lineopen" width="960px" append-to-body>
      <el-form ref="lineform" :model="lineform" label-width="100px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="物料编码"  prop="itemCode">
              <el-input v-model="form.itemCode" readonly="readonly" >
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="物料名称" prop="itemName">
              <el-input v-model="form.itemName" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位" prop="unitName">
              <el-input v-model="form.unitName" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="入库数量" prop="quantityRecived">
              <el-input-number :min="0.01" :max="99999999" v-model="form.quantityRecived" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="生产日期期" prop="produceDate">
              <el-date-picker disabled
                v-model="form.produceDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择生产日期">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="有效期" prop="expireDate">
              <el-date-picker disabled
                v-model="form.expireDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择有效期">
              </el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="生产批号" prop="lotNumber">
              <el-input v-model="form.lotNumber" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="批次号" prop="batchCode">
              <el-input v-model="form.batchCode" readonly="readonly"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="closeLineDialog">关 闭</el-button>
      </div>
    </el-dialog>

    </div>
  </template>

  <script>
  import { listProductrecptdetail, getProductrecptdetail, delProductrecptdetail, addProductrecptdetail, updateProductrecptdetail } from "@/api/mes/wm/productrecptdetail";
  import { getProductrecptline, listWithDetail } from "@/api/mes/wm/productrecptline";
  import { locationOptions, areaOptions } from "@/api/mes/wm/productrecpt";
  import printLabel from "@/components/printerLabel/index.vue"
  import {getTreeList} from "@/api/mes/wm/warehouse"
  export default {
    name: "Productrecptline",
    components:{
      printLabel
    },
    props: {
        recptId: null,
        recptCode: null,
        workorderCode: null,
        optType: null,
        status: null,
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
        // 产品入库记录行表格数据
        productrecptlineList: [],
        // 仓库列表（扁平）
        warehouses: [],
        // 当前选择仓库下的库区候选
        locationOptions: [],
        // 当前选择库区下的库位候选
        areaOptions: [],
        // 当前操作行
        currentLine: null,
        // 当前选中库区
        currentLocation: null,
        currentArea: null,
        // 弹出层标题
        title: "",
        // 是否显示弹出层
        open: false,
        // 查询参数
        queryParams: {
          pageNum: 1,
          pageSize: 10,
          recptId: this.recptId,
          materialStockId: null,
          itemId: null,
          itemCode: null,
          itemName: null,
          specification: null,
          unitOfMeasure: null,
          unitName: null,
          quantityRecived: null,
          batchCode: null,
          warehouseId: null,
          warehouseCode: null,
          warehouseName: null,
          locationId: null,
          locationCode: null,
          locationName: null,
          areaId: null,
          areaCode: null,
          areaName: null,
        },
        // 表单参数
        form: {},
        // 表单校验
        rules: {
            warehouseId: [
                { required: true, message: "请选择入库仓库", trigger: "change" }
            ],
            locationId: [
                { required: true, message: "请选择入库库区", trigger: "change" }
            ],
            areaId: [
                { required: true, message: "请选择入库库位", trigger: "change" }
            ],
            quantity: [
                { required: true, message: "上架数量不能为空", trigger: "blur" }
            ]
        },
        //入库单行相关
        lineform: {},
        linetitle: "",
        lineopen: false,
      };
    },
    computed: {
        canStock() {
            return this.optType != 'view' && this.status === 'APPROVING';
        },
        lineQty() {
            return this.currentLine ? Number(this.currentLine.quantityRecived || 0) : 0;
        },
        stockedQty() {
            if (!this.currentLine) return 0;
            return this.stockedOf(this.currentLine);
        },
        lineRemain() {
            const remain = this.lineQty - this.stockedQty;
            return remain > 0 ? remain : 0;
        },
        maxQuantity() {
            let max = this.lineRemain;
            if (this.currentArea && this.currentArea.unlimited === false && this.currentArea.remaining != null) {
                max = Math.min(max, Number(this.currentArea.remaining));
            }
            return max > 0 ? max : 0;
        },
        currentAreaTip() {
            if (this.currentArea && this.currentArea.mixingBlocked) {
                return this.currentArea.mixingMessage + '，请更换库位';
            }
            return '';
        },
        currentAreaTipType() {
            return 'warning';
        },
        recommendTip() {
            const area = this.currentArea;
            if (area && area.recommended) {
                return '已推荐库位：' + area.areaName + (area.recommendReason ? '（' + area.recommendReason + '）' : '');
            }
            const loc = this.currentLocation;
            if (loc && loc.recommended) {
                return '已推荐库区：' + loc.locationName + (loc.recommendReason ? '（' + loc.recommendReason + '）' : '');
            }
            return '';
        }
    },
    created() {
      this.getListWithDetail();
      this.getWarehouseList();
    },
    methods: {
        /** 查询产品入库记录行列表 */
        getListWithDetail() {
            this.loading = true;
            listWithDetail(this.queryParams).then(response => {
            this.productrecptlineList = response.rows;
            this.total = response.total;
            this.loading = false;
            });
        },
        getWarehouseList(){
            getTreeList().then( response =>{
                this.warehouses = (response.data || []).map(w => ({
                    warehouseId: w.warehouseId,
                    warehouseName: w.warehouseName
                }));
            });
        },
        formatQty(value) {
            const num = Number(value || 0);
            return Number.isInteger(num) ? String(num) : num.toFixed(2);
        },
        stockedOf(row) {
            if (!row || !row.details) return 0;
            return row.details.reduce((sum, d) => sum + Number(d.quantity || 0), 0);
        },
        remainOf(row) {
            const remain = Number(row.quantityRecived || 0) - this.stockedOf(row);
            return remain > 0 ? remain : 0;
        },
        unitText() {
            return this.form.unitName ? this.form.unitName : '';
        },
        locationLabel(l) {
            const prefix = l.recommended ? '【推荐】' : '';
            const qty = l.unlimited ? '不限量' : ('余量 ' + this.formatQty(l.remaining));
            const block = l.mixingBlocked ? '（不可混放）' : '';
            return prefix + l.locationName + '（' + qty + this.unitText() + '）' + block;
        },
        areaLabel(a) {
            const prefix = a.recommended ? '【推荐】' : '';
            const qty = a.unlimited ? '不限量' : ('余量 ' + this.formatQty(a.remaining));
            const block = a.mixingBlocked ? '（不可混放）' : '';
            return prefix + a.areaName + '（' + qty + this.unitText() + '）' + block;
        },
        loadLocations(autoSelect) {
            this.locationOptions = [];
            this.areaOptions = [];
            this.currentLocation = null;
            this.currentArea = null;
            if (!this.form.warehouseId || !this.form.lineId) return Promise.resolve();
            const selectRecommended = autoSelect !== false;
            return locationOptions({
                warehouseId: this.form.warehouseId,
                lineId: this.form.lineId,
                quantity: this.form.quantity || this.lineRemain
            }).then(response => {
                this.locationOptions = response.data || [];
                if (selectRecommended) {
                    const recommended = this.locationOptions.find(l => l.recommended)
                        || this.locationOptions.find(l => !l.mixingBlocked);
                    if (recommended) {
                        this.form.locationId = recommended.locationId;
                        this.currentLocation = recommended;
                        return this.loadAreas();
                    }
                }
            });
        },
        loadAreas(autoSelect) {
            this.areaOptions = [];
            this.currentArea = null;
            if (!this.form.locationId || !this.form.lineId) return Promise.resolve();
            this.currentLocation = this.locationOptions.find(l => l.locationId === this.form.locationId) || null;
            const selectRecommended = autoSelect !== false;
            return areaOptions({
                locationId: this.form.locationId,
                lineId: this.form.lineId,
                quantity: this.form.quantity || this.lineRemain
            }).then(response => {
                this.areaOptions = response.data || [];
                if (selectRecommended) {
                    const recommended = this.areaOptions.find(a => a.recommended)
                        || this.areaOptions.find(a => !a.mixingBlocked);
                    if (recommended) {
                        this.form.areaId = recommended.areaId;
                        this.currentArea = recommended;
                    }
                }
            });
        },
        handleWarehouseChange() {
            this.form.locationId = null;
            this.form.areaId = null;
            this.loadLocations();
        },
        handleLocationChange() {
            this.form.areaId = null;
            this.loadAreas();
        },
        handleAreaChange(areaId) {
            this.currentArea = this.areaOptions.find(a => a.areaId === areaId) || null;
        },
        // 取消按钮
        cancel() {
            this.open = false;
            this.reset();
        },
        // 表单重置
        reset() {
            this.form = {
                detailId: null, lineId: null, recptId: this.recptId, materialStockId: null, itemId: null, itemCode: null, itemName: null, specification: null, unitOfMeasure: null, unitName: null, quantity: null, batchId: null, batchCode: null, warehouseId: null, warehouseCode: null, warehouseName: null, locationId: null, locationCode: null, locationName: null, areaId: null, areaCode: null, areaName: null, remark: null, attr1: null, attr2: null, attr3: null, attr4: null, createBy: null, createTime: null, updateBy: null, updateTime: null };
            this.locationOptions = [];
            this.areaOptions = [];
            this.currentLine = null;
            this.currentLocation = null;
            this.currentArea = null;
            this.resetForm("form");
        },
        //行表单重置
        resetLineForm() {
            this.lineform = {
                lineId: null,
                recptId: this.recptId,
                itemId: null,
                itemCode: null,
                itemName: null,
                specification: null,
                unitOfMeasure: null,
                unitName: null,
                quantityRecived: 1,
                batchId: null,
                batchCode: null,
                warehouseId: null,
                warehouseCode: null,
                warehouseName: null,
                locationId: null,
                locationCode: null,
                locationName: null,
                areaId: null,
                areaCode: null,
                areaName: null,
                remark: null,
                attr1: null,
                attr2: null,
                attr3: null,
                attr4: null,
                createBy: null,
                createTime: null,
                updateBy: null,
                updateTime: null
            };
            this.resetForm("lineform");
        },
        /** 搜索按钮操作 */
        handleQuery() {
            this.queryParams.pageNum = 1;
            this.getListWithDetail();
        },
        /** 重置按钮操作 */
        resetQuery() {
            this.resetForm("queryForm");
            this.getListWithDetail();
        },
        // 多选框选中数据
        handleSelectionChange(selection) {
            this.ids = selection.map(item => item.lineId)
            this.single = selection.length!==1
            this.multiple = !selection.length
        },
        handleView(row) {
            this.resetLineForm();
            const lineId = row.lineId || this.ids
            getProductrecptline(lineId).then(response => {
                this.form = response.data;
                this.lineopen = true;
                this.linetitle = "查看物料入库单行";
            });
        },
        closeLineDialog() {
            this.lineopen = false;
        },
        /** 新增上架明细 */
        handleAdd(row) {
            this.reset();
            this.currentLine = row;
            this.form.recptId = row.recptId
            this.form.lineId = row.lineId
            this.form.materialStockId = row.materialStockId;
            this.form.itemId = row.itemId
            this.form.itemCode = row.itemCode
            this.form.itemName = row.itemName
            this.form.specification = row.specification
            this.form.unitOfMeasure = row.unitOfMeasure
            this.form.unitName = row.unitName
            this.form.batchId = row.batchId
            this.form.batchCode = row.batchCode
            this.form.quantity = this.lineRemain;
            this.open = true;
            this.title = "上架 - 分配库位与数量";
            // 默认落在成品仓（如有），并加载推荐库区/库位
            const fg = this.warehouses.find(w => (w.warehouseName || '').indexOf('成品') >= 0) || this.warehouses[0];
            if (fg) {
                this.form.warehouseId = fg.warehouseId;
                this.loadLocations();
            }
        },
        /** 修改上架明细 */
        handleUpdate(row, lineRow) {
            this.reset();
            this.currentLine = lineRow || this.productrecptlineList.find(l => l.lineId === row.lineId);
            const detailId = row.detailId || this.ids
            getProductrecptdetail(detailId).then(response => {
                this.form = response.data;
                this.title = "修改上架明细";
                this.open = true;
                const savedLocationId = response.data.locationId;
                const savedAreaId = response.data.areaId;
                // 回填原库位：先加载候选（不自动改选），再恢复原值
                this.loadLocations(false).then(() => {
                    this.form.locationId = savedLocationId || this.form.locationId;
                    this.currentLocation = this.locationOptions.find(l => l.locationId === this.form.locationId) || null;
                    return this.loadAreas(false);
                }).then(() => {
                    this.form.areaId = savedAreaId || this.form.areaId;
                    this.currentArea = this.areaOptions.find(a => a.areaId === this.form.areaId) || null;
                });
            });
        },
        /** 提交按钮 */
        submitForm() {
            this.$refs["form"].validate(valid => {
                if (!valid) return;
                if (!this.canStock) {
                    this.$modal.msgError("当前单据状态不允许上架");
                    return;
                }
                if (!this.form.quantity || Number(this.form.quantity) <= 0) {
                    this.$modal.msgError("上架数量必须大于0");
                    return;
                }
                if (Number(this.form.quantity) > this.lineRemain) {
                    this.$modal.msgError("上架数量超过待上架数量，本行还差 " + this.formatQty(this.lineRemain));
                    return;
                }
                if (this.currentArea && this.currentArea.mixingBlocked) {
                    this.$modal.msgError(this.currentArea.mixingMessage + "，请更换库位");
                    return;
                }
                if (this.currentArea && this.currentArea.unlimited === false
                        && this.currentArea.remaining != null
                        && Number(this.form.quantity) > Number(this.currentArea.remaining)) {
                    this.$modal.msgError("超过库位[" + this.currentArea.areaName + "]余量 "
                        + this.formatQty(this.currentArea.remaining) + "，请调整数量或更换库位");
                    return;
                }
                if (this.form.detailId != null) {
                    updateProductrecptdetail(this.form).then(response => {
                    this.$modal.msgSuccess("修改成功");
                    this.open = false;
                    this.getListWithDetail();
                    });
                } else {
                    addProductrecptdetail(this.form).then(response => {
                    this.$modal.msgSuccess("上架成功");
                    this.open = false;
                    this.getListWithDetail();
                    });
                }
            });
        },
        /** 删除按钮操作 */
        handleDelete(row) {
            const detailIds = row.detailId || this.ids;
            this.$modal.confirm('是否确认删除产品入库记录明细编号为"' + detailIds + '"的数据项？').then(function() {
                return delProductrecptdetail(detailIds);
            }).then(() => {
                this.getListWithDetail();
                this.$modal.msgSuccess("删除成功");
            }).catch(() => {});
        },
    }
  };
  </script>

  <style scoped>
  .qty-tip {
    font-size: 12px;
    color: #909399;
    line-height: 20px;
    margin-top: 4px;
  }
  </style>

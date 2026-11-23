<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="入库单编号" prop="recptCode">
        <el-input
          v-model="queryParams.recptCode"
          placeholder="请输入入库单编号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="入库单名称" prop="recptName">
        <el-input
          v-model="queryParams.recptName"
          placeholder="请输入入库单名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="生产工单" prop="workorderCode">
        <el-input
          v-model="queryParams.workorderCode"
          placeholder="请输入生产工单编码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="仓库" prop="warehouseName">
        <el-input
          v-model="queryParams.warehouseName"
          placeholder="请输入仓库名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['mes:wm:productrecpt:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mes:wm:productrecpt:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mes:wm:productrecpt:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['mes:wm:productrecpt:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-alert
      v-if="pipelineBanner"
      class="mb8"
      :type="pipelineBanner.type"
      :closable="false"
      show-icon
      :title="pipelineBanner.title"
    >
      <span>{{ pipelineBanner.text }}</span>
      <template v-if="pipelineBanner.row">
        <el-button
          v-if="pipelineBanner.row.status == 'PREPARE'"
          type="text"
          size="mini"
          @click="handleUpdate(pipelineBanner.row)"
        >打开草稿</el-button>
        <el-button
          v-if="pipelineBanner.row.status == 'APPROVING'"
          type="text"
          size="mini"
          @click="handleStocking(pipelineBanner.row, true)"
        >去上架</el-button>
        <el-button
          v-if="pipelineBanner.row.status == 'APPROVED'"
          type="text"
          size="mini"
          @click="handleExecute(pipelineBanner.row)"
        >执行入库</el-button>
        <el-button
          v-if="pipelineBanner.row.status == 'FINISHED'"
          type="text"
          size="mini"
          @click="handleView(pipelineBanner.row)"
        >查看单据</el-button>
        <el-button type="text" size="mini" @click="goWorkorder">返回工单流水线</el-button>
      </template>
    </el-alert>

    <el-alert
      v-if="ngNotice"
      class="mb8"
      type="error"
      :closable="false"
      show-icon
      :title="'工单 ' + ngNotice.workorderCode + ' 的产出有 ' + ngNotice.quantity + ' 台不合格，未生成产品入库草稿'"
    >
      <span>不合格批次：{{ ngNotice.batches || '-' }}。该批不进入成品仓，请为该产品重新下单，备注填写「{{ ngNotice.workorderCode }} 的二次订单」。</span>
      <el-button type="text" size="mini" @click="goReorder">去下二次订单</el-button>
    </el-alert>

    <el-table
      v-loading="loading"
      :data="productrecptList"
      :row-class-name="rowClassName"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="入库单编号" width="130px" align="center" prop="recptCode" >
        <template slot-scope="scope">
          <el-button
            type="text"
            @click="handleView(scope.row)"
            v-hasPermi="['mes:wm:productrecpt:query']"
          >{{scope.row.recptCode}}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="入库单名称"  align="center" prop="recptName" :show-overflow-tooltip="true"/>
      <el-table-column label="生产工单编码" width="150px" align="center" prop="workorderCode" />
      <el-table-column label="产品编码" align="center" prop="itemCode" />
      <el-table-column label="产品名称" align="center" prop="itemName" />
      <el-table-column label="入库日期" align="center" prop="recptDate" width="120">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.recptDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="单据状态" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.mes_product_recpt_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-shopping-cart-full"
            v-if="scope.row.status =='APPROVING'"
            @click="handleStocking(scope.row, true)"
            v-hasPermi="['mes:wm:productrecpt:edit']"
          >去上架</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-video-play"
            v-if="scope.row.status =='APPROVED'"
            @click="handleExecute(scope.row)"
            v-hasPermi="['mes:wm:productrecpt:edit']"
          >执行入库</el-button>          
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            v-if="scope.row.status =='PREPARE'"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['mes:wm:productrecpt:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            v-if="scope.row.status =='PREPARE'"
            @click="handleDelete(scope.row)"
            v-hasPermi="['mes:wm:productrecpt:remove']"
          >删除</el-button>
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

    <!-- 添加或修改产品入库录对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="960px" append-to-body>
      <el-alert
        v-if="form.recptId != null"
        class="mb8"
        :type="recptAlertType"
        :closable="false"
        show-icon
        :title="'入库单 ' + (form.recptCode || '-') + ' · 工单 ' + (form.workorderCode || '-') + ' · ' + recptStatusLabel"
      >
        <span>{{ recptFlowTip }}</span>
      </el-alert>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="入库单编号" prop="recptCode">
              <el-input v-model="form.recptCode" disabled placeholder="保存后由系统自动生成" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入库单名称" prop="recptName">
              <el-input v-model="form.recptName" placeholder="请输入入库单名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="生产工单" prop="workorderCode">
              <el-input v-model="form.workorderCode" placeholder="请选择生产工单" >
                <el-button slot="append" icon="el-icon-search" @click="handleWorkorderSelect"></el-button>
              </el-input>
              <WorkorderSelect ref="woSelect" @onSelected="onWorkorderSelected"></WorkorderSelect>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="生产工单名称" prop="workorderName">
              <el-input v-model="form.workorderName" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="入库日期" prop="recptDate">
              <el-date-picker clearable
                v-model="form.recptDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择入库日期">
              </el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <el-divider v-if="form.recptId !=null" content-position="center">物料信息</el-divider>
        <el-card shadow="always" v-if="form.recptId !=null && form.status == 'PREPARE'" class="box-card">
          <Productrecptline ref=line :recptId="form.recptId" :itemId="form.itemId" :optType="optType"></Productrecptline>
        </el-card>
        <el-card v-if="form.recptId !=null && form.status !='PREPARE'" class="box-card">
          <ProductrecptDetail
            ref="detail"
            :recptId="form.recptId"
            :recptCode="form.recptCode"
            :workorderCode="form.workorderCode"
            :optType="optType"
            :status="form.status"
          ></ProductrecptDetail>
        </el-card>
        <el-tabs v-if="open && optType === 'view' && form.recptId != null" style="margin-top: 16px">
          <el-tab-pane label="库存变动">
            <StockTransactions :key="form.recptId" :recptId="form.recptId" :status="form.status" />
          </el-tab-pane>
        </el-tabs>
        <div slot="footer" class="dialog-footer">
          <el-button type="primary" @click="saveForm" v-if="form.status =='PREPARE' && optType !='view' ">保 存</el-button>
          <el-button type="warning" @click="submitToStock" v-if="form.status =='PREPARE' && form.recptId !=null && optType !='view' ">提 交</el-button>
          <el-button type="warning" @click="submitToExecute" v-if="form.status =='APPROVING' && form.recptId !=null && optType !='view' ">确认上架</el-button>
          <el-button type="warning" @click="handleExecute(form)" v-if="form.status =='APPROVED' && optType !='view' ">执行入库</el-button>
          <el-button type="danger" @click="cancel"  v-if="form.status !='PREPARE' && form.status !='FINISHED' && optType !='view' " >取 消</el-button>
          <el-button @click="close">关 闭</el-button>
        </div>
    </el-dialog>
  </div>
</template>

<script>
import pipelineWorkorderContext from '@/mixins/pipelineWorkorderContext'
import { listProductrecpt, getProductrecpt, delProductrecpt, addProductrecpt, updateProductrecpt, checkQuantity ,execute, confirmStocking} from "@/api/mes/wm/productrecpt";
import { nextBizCode } from "@/api/mes/code";
import { listProductproduce } from "@/api/mes/wm/productproduce";
import { listProductproduceline } from "@/api/mes/wm/productproduceline";
import WorkorderSelect from "@/components/workorderSelect/single.vue"
import Productrecptline from "./line.vue"
import ProductrecptDetail from "./detail.vue"
import StockTransactions from "./stock-transactions.vue"
import {getTreeList} from "@/api/mes/wm/warehouse"
export default {
  mixins: [pipelineWorkorderContext],
  name: "Productrecpt",
  dicts: ['mes_order_status', 'mes_product_recpt_status'],
  components:{WorkorderSelect,Productrecptline,ProductrecptDetail,StockTransactions},
  data() {
    return {
      optType: undefined,
      // 从“执行入库”入口进入上架界面时，上架完成后询问是否立即过账
      executeAfterStock: false,
      warehouseInfo:[],
      warehouseOptions:[],
      warehouseProps:{
        multiple: false,
        value: 'pId',
        label: 'pName',
      },
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
      // 产品入库录表格数据
      productrecptList: [],
      // 工单产出不合格提示：有 NG 产出且无入库草稿时提示用户下二次订单
      ngNotice: null,
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        recptCode: null,
        recptName: null,
        workorderId: null,
        workorderCode: null,
        workorderName: null,
        warehouseId: null,
        warehouseCode: null,
        warehouseName: null,
        locationId: null,
        locationCode: null,
        locationName: null,
        areaId: null,
        areaCode: null,
        areaName: null,
        recptDate: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        recptCode: [
          { required: true, message: "入库单编号不能为空", trigger: "blur" },
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        recptName: [
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        recptDate: [
          { required: true, message: "请选择入库日期", trigger: "blur" }
        ],
        remark: [
          { max: 250, message: '长度必须小于250个字符', trigger: 'blur' }
        ]
      }
    };
  },
  computed: {
    /** 流水线上下文（按工单）内，优先展示当前需要处理的入库单 */
    activeRecpt() {
      const rows = this.productrecptList || []
      return rows.find(row => row.status === 'APPROVING')
        || rows.find(row => row.status === 'APPROVED')
        || rows.find(row => row.status === 'PREPARE')
        || rows.find(row => row.status === 'FINISHED')
        || null
    },
    /** 从流水线进入时，用一条常驻提示说明工单链号、当前状态与下一步 */
    pipelineBanner() {
      if (this.loading || !this.queryParams.workorderId) return null
      const row = this.activeRecpt
      const workorderCode = this.queryParams.workorderCode || (row && row.workorderCode) || '-'
      if (!row) {
        return {
          type: 'info',
          title: '工单 ' + workorderCode + ' 暂无产品入库单',
          text: '工单完工且末工序合格产出放行后，系统会自动生成产品入库草稿，单号与工单同源（cr + 日期 + 流水）。',
          row: null
        }
      }
      const labels = { PREPARE: '草稿', APPROVING: '待上架', APPROVED: '已上架', FINISHED: '已入库', CANCELED: '已取消' }
      const next = {
        PREPARE: '请核对入库数量与批次，点击「提交」后进入待上架。',
        APPROVING: '请逐行分配库位并确认上架数量，全部上架后再执行入库过账。',
        APPROVED: '已上架，执行入库过账后完成本工单入库。',
        FINISHED: '本工单产品已入库，可在「销售出库」按工单成品发货。'
      }[row.status] || ''
      return {
        type: row.status === 'FINISHED' ? 'success' : (row.status === 'APPROVING' ? 'warning' : 'info'),
        title: '工单 ' + workorderCode + ' · 入库单 ' + row.recptCode + '（' + (labels[row.status] || row.status) + '）',
        text: next + ' 单号由系统按工单链自动绑定，全局唯一。',
        row
      }
    },
    recptStatusLabel() {
      return { PREPARE: '草稿', APPROVING: '待上架', APPROVED: '已上架', FINISHED: '已入库', CANCELED: '已取消' }[this.form.status] || (this.form.status || '')
    },
    recptAlertType() {
      if (this.form.status === 'FINISHED') return 'success'
      if (this.form.status === 'APPROVING') return 'warning'
      return 'info'
    },
    recptFlowTip() {
      return {
        PREPARE: '草稿已按工单链自动绑定；核对入库行与批次后点击「提交」，单据进入待上架。',
        APPROVING: '待上架：请逐行分配库位并填写上架数量，全部上架后点击「确认上架」。',
        APPROVED: '已上架：点击「执行入库」完成过账，库存与批次流水会同步更新。',
        FINISHED: '已入库：本工单成品已进入成品仓，销售出库可直接按该批次发货。',
        CANCELED: '已取消：如需重新入库，请重新生成入库单。'
      }[this.form.status] || ''
    }
  },
  created() {
    this._autoOpenedWorkorderId = null;
    this.getList();
  },
  methods: {
    /** 查询产品入库录列表 */
    getList() {
      this.loading = true;
      listProductrecpt(this.queryParams).then(response => {
        this.productrecptList = response.rows;
        this.total = response.total;
        this.loading = false;
        this.loadNgNotice();
        this.autoOpenPipelineDraft();
      });
    },
    /**
     * 从流水线进入时自动打开当前可办理的入库单，避免用户到页面后找不到单据。
     * 每个工单上下文只自动打开一次，关闭后不再反复弹出。
     */
    autoOpenPipelineDraft() {
      const workorderId = this.queryParams.workorderId;
      if (!workorderId || this.open) return;
      if (String(this._autoOpenedWorkorderId || '') === String(workorderId)) return;
      const rows = this.productrecptList || [];
      const row = rows.find(item => item.status === 'APPROVING') || rows.find(item => item.status === 'PREPARE');
      if (!row) return;
      this._autoOpenedWorkorderId = String(workorderId);
      if (row.status === 'APPROVING') this.handleStocking(row, true);
      else this.handleUpdate(row);
    },
    /** 返回工单流水线（保留当前工单） */
    goWorkorder() {
      this.$router.push({
        path: '/index',
        query: { pipelineWorkorderId: String(this.queryParams.workorderId || '') }
      }).catch(() => {});
    },
    /** 系统自动生成的入库单：高亮提示，避免与手工单混淆 */
    rowClassName({ row }) {
      if (!row || !row.attr1) return '';
      return String(row.attr1).indexOf('AUTO:PRODUCT_RECPT:') === 0 ? 'row-auto-receipt' : '';
    },
    /** 按工单汇总不合格产出：有 NG 且不能入库时，提示用户下二次订单 */
    async loadNgNotice() {
      const workorderId = this.queryParams.workorderId;
      this.ngNotice = null;
      if (!workorderId) return;
      try {
        const produceResponse = await listProductproduce({ workorderId, pageNum: 1, pageSize: 50 });
        const records = ((produceResponse && produceResponse.rows) || []).filter(row => row.status === 'FINISHED');
        const linePages = await Promise.all(records.map(row =>
          listProductproduceline({ recordId: row.recordId, pageNum: 1, pageSize: 200 }).catch(() => ({ rows: [] }))
        ));
        const ngLines = [].concat(...linePages.map(page => (page && page.rows) || []))
          .filter(line => line.qualityStatus === 'NG' && Number(line.quantityProduce) > 0);
        if (!ngLines.length) return;
        this.ngNotice = {
          quantity: ngLines.reduce((sum, line) => sum + Number(line.quantityProduce || 0), 0),
          batches: Array.from(new Set(ngLines.map(line => line.batchCode).filter(Boolean))).join('、'),
          workorderCode: this.queryParams.workorderCode || ''
        };
      } catch (e) {
        this.ngNotice = null;
      }
    },
    /** 跳到客户订单，备注预填为原工单的二次订单 */
    goReorder() {
      if (!this.ngNotice || !this.ngNotice.workorderCode) return;
      const code = this.ngNotice.workorderCode;
      this.$router.push({ path: '/order/salesorder', query: {
        reorderFrom: code,
        reorderRemark: code + ' 的二次订单'
      } }).catch(() => {});
    },
    // 取消按钮
    close() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        recptId: null,
        recptCode: null,
        recptName: null,
        workorderId: null,
        workorderCode: null,
        workorderName: null,
        warehouseId: null,
        warehouseCode: null,
        warehouseName: null,
        locationId: null,
        locationCode: null,
        locationName: null,
        itemId: null,
        itemCode: null,
        itemName: null,
        unitOfMeasure: null,
        clientCode: null,
        clientName: null,
        areaId: null,
        areaCode: null,
        areaName: null,
        recptDate: new Date(),
        status: "PREPARE",
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
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.clearPipelineWorkorderContext();
      this._autoOpenedWorkorderId = null;
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.recptId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      // 手工新增后不再触发流水线的自动打开，避免刚保存又被弹窗打断
      if (this.queryParams.workorderId) this._autoOpenedWorkorderId = String(this.queryParams.workorderId);
      // 从流水线按工单进入时，新增单直接绑定该工单，单号沿用工单链的日期与流水
      const workorderCode = this.queryParams.workorderId ? this.queryParams.workorderCode : null;
      if (workorderCode) {
        this.form.workorderId = this.queryParams.workorderId;
        this.form.workorderCode = workorderCode;
        this.form.recptName = workorderCode + '产品入库';
      }
      nextBizCode('CPRK', workorderCode || undefined).then(res => {
        this.form.recptCode = (res && res.data) || '';
      });
      this.open = true;
      this.title = workorderCode ? '添加工单 ' + workorderCode + ' 的产品入库单' : '添加产品入库单';
      this.optType = 'add'
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const recptId = row.recptId || this.ids
      getProductrecpt(recptId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改产品入库单";
        this.optType = 'edit'
      });
    },
    /** 查看单据 */
    handleView(row) {
      this.reset();
      const recptId = row.recptId || this.ids
      getProductrecpt(recptId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "查看产品入库单";
        this.optType = 'view'
      });
    },
    /**
     * 上架按钮操作（打开上架界面）
     * @param row 入库单
     * @param executeAfterStock 上架完成后是否询问立即过账
     */
    handleStocking(row, executeAfterStock){
      const recptId = row.recptId || this.ids;
      getProductrecpt(recptId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = response.data.status === 'APPROVING' ? "上架（待上架 → 已上架）" : "入库单上架";
        this.optType = "edit";
        this.executeAfterStock = !!executeAfterStock;
      });
    },

    //执行入库：待上架先进入上架界面，已上架才允许过账
    handleExecute(row){
      if (row.status === 'APPROVING') {
        this.handleStocking(row, true);
        return;
      }
      if (row.status !== 'APPROVED') {
        this.$modal.msgWarning("当前状态不可执行入库");
        return;
      }
      const recptId = row.recptId || this.ids;
      const code = row.recptCode || this.form.recptCode || '';
      this.$modal.confirm('入库单 ' + code + ' 已上架，确认执行入库过账？过账后库存与批次流水将同步更新。')
        .then(() => this.doExecute(recptId, code)).catch(() => {});
    },

    // 过账入库
    doExecute(recptId, code) {
      return execute(recptId).then(() => {
        this.$modal.msgSuccess("入库单 " + (code || '') + " 入库成功");
        if (this.open) this.open = false;
        this.getList();
      });
    },
    /** 提交按钮 */
    saveForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.recptId != null) {
            updateProductrecpt(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addProductrecpt(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },

    //提交按钮(提交到待上架状态)
    submitToStock(){
      this.$refs["form"].validate(valid => {
        if (!valid) return;
        const code = this.form.recptCode;
        this.$modal.confirm('入库单 ' + code + ' 提交后进入「待上架」，需逐行分配库位并确认上架后才能过账入库，是否继续？')
          .then(() => {
            this.form.status = 'APPROVING';
            updateProductrecpt(this.form).then(response => {
              this.$modal.msgSuccess("入库单 " + code + " 已提交，下一步：逐行分配库位并确认上架");
              this.open = false;
              this.getList();
            }
           ).catch(() => {
              this.form.status = 'PREPARE';
           });;
          }).catch(() => {});
      });
    },

    submitToExecute(){
      const code = this.form.recptCode;
      this.$modal.confirm('确认入库单 ' + code + ' 上架完成？系统将校验每行上架数量与入库数量一致，并检查库位余量与混放规则。')
        .then(() => {
          const executeAfter = this.executeAfterStock;
          // 待上架 -> 已上架：服务端校验上架数量必须等于入库行数量，且余量/混放通过
          confirmStocking(this.form.recptId).then(() => {
            this.$modal.msgSuccess("入库单 " + code + " 已上架，下一步：执行入库过账");
            this.open = false;
            this.getList();
            if (executeAfter) {
              const recptId = this.form.recptId;
              this.$modal.confirm('入库单 ' + code + ' 上架已完成，是否立即执行入库过账？').then(() => {
                this.doExecute(recptId, code);
              }).catch(() => {});
            }
          }).catch(() => {});
        }).catch(() => {});
    },


    //取消
    cancel(){
      let that = this;
      this.$modal.confirm('确认撤销入库单？').then(function() {        
        const oldStatus = that.form.status;
        that.form.status = 'CANCELED';
        updateProductrecpt(that.form).then(response => {
            that.$modal.msgSuccess("撤销成功");
            that.open = false;
            that.getList();
          } 
          ).catch(() => {
            that.form.status = oldStatus;
          });
          return true;
      }).catch(() => {});
    },

    /** 删除按钮操作 */
    handleDelete(row) {
      const recptIds = row.recptId || this.ids;
      this.$modal.confirm('是否确认删除产品入库单编号为"' + row.recptCode + '"的数据项？').then(function() {
        return delProductrecpt(recptIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('wm/productrecpt/export', {
        ...this.queryParams
      }, `productrecpt_${new Date().getTime()}.xlsx`)
    },
    //选择生产工单
    handleWorkorderSelect(){
      this.$refs.woSelect.handleOpen(this.form.workorderId)
    },
    onWorkorderSelected(row){
      if(row != undefined && row != null){
        this.form.workorderId = row.workorderId;
        this.form.workorderCode = row.workorderCode;
        this.form.workorderName = row.workorderName;
        this.form.itemId = row.productId;
        this.form.itemCode = row.productCode;
        this.form.itemName = row.productName;
        this.form.unitOfMeasure = row.unitOfMeasure;
        this.form.unitName = row.unitName;
        this.form.clientCode = row.clientCode;
        this.form.clientName = row.clientName;
        if (!this.form.recptName) {
          this.form.recptName = (row.workorderName || row.workorderCode) + '产品入库';
        }
        // 新增单据在选定工单后重新取链号：前缀为 cr，日期与流水与工单号一致
        if (this.form.recptId == null && row.workorderCode) {
          nextBizCode('CPRK', row.workorderCode).then(res => {
            this.form.recptCode = (res && res.data) || this.form.recptCode;
          });
        }
      }
    },
  }
};
</script>

<style scoped>
::v-deep .el-table .row-auto-receipt > td {
  background-color: #fff8e1 !important;
}
::v-deep .el-table .row-auto-receipt:hover > td {
  background-color: #fff1c9 !important;
}
</style>

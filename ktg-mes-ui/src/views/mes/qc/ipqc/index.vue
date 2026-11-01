<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="检验单编号" prop="ipqcCode">
        <el-input
          v-model="queryParams.ipqcCode"
          placeholder="请输入检验单编号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="检验类型" prop="ipqcType">
        <el-select v-model="queryParams.ipqcType" placeholder="请选择检验类型" clearable>
          <el-option
            v-for="dict in dict.type.mes_qc_detail_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="工单编号" prop="workorderCode">
        <el-input
          v-model="queryParams.workorderCode"
          placeholder="请输入工单编码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="产品物料编码" prop="itemCode">
        <el-input
          v-model="queryParams.itemCode"
          placeholder="请输入产品物料编码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="产品物料名称" prop="itemName">
        <el-input
          v-model="queryParams.itemName"
          placeholder="请输入产品物料名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="检测结果" prop="checkResult">
        <el-select v-model="queryParams.checkResult" placeholder="请选择检验结果" clearable>
          <el-option
            v-for="dict in dict.type.mes_qc_result"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
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
          v-hasPermi="['mes:qc:ipqc:add']"
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
          v-hasPermi="['mes:qc:ipqc:edit']"
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
          v-hasPermi="['mes:qc:ipqc:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['mes:qc:ipqc:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table
      v-loading="loading"
      :data="ipqcList"
      :row-class-name="rowClassName"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="检验单编号" width="150px" align="center" prop="ipqcCode" >
        <template slot-scope="scope">
          <el-button
            type="text"
            @click="handleView(scope.row)"
            v-hasPermi="['mes:qc:ipqc:query']"
          >{{scope.row.ipqcCode}}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="检验类型" align="center" prop="ipqcType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.mes_pqc_type" :value="scope.row.ipqcType"/>
        </template>
      </el-table-column>
      <el-table-column label="工单编号" width="150px" align="center" prop="workorderCode" />
      <el-table-column label="产品物料编码" width="120px" align="center" prop="itemCode" />
      <el-table-column label="产品物料名称" width="150px" align="center" prop="itemName" :show-overflow-tooltip="true"/>
      <el-table-column label="规格型号" align="center" prop="specification" :show-overflow-tooltip="true"/>
      <el-table-column label="单位" align="center" prop="unitName" />
      <el-table-column label="检测数量" align="center" prop="quantityCheck" />
      <el-table-column label="检测结果" align="center" prop="checkResult" >
        <template slot-scope="scope">
          <dict-tag :options="dict.type.mes_qc_result" :value="scope.row.checkResult"/>
        </template>
      </el-table-column>

      <el-table-column label="检测日期" align="center" prop="inspectDate" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.inspectDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="检测人员" align="center" prop="inspector" />
      <el-table-column label="单据状态" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.mes_order_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-if="scope.row.status =='PREPARE'"
            v-hasPermi="['mes:qc:ipqc:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-if="scope.row.status =='PREPARE'"
            v-hasPermi="['mes:qc:ipqc:remove']"
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

    <!-- 添加或修改过程检验单对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-alert
          v-if="optType !== 'view' && inspectionTip.text"
          class="ipqc-auto-hint"
          :closable="false"
          show-icon
          :type="inspectionTip.type"
          :title="inspectionTip.text"
        />
        <el-row>
          <el-col :span="8">
            <el-form-item label="检验单编号" prop="ipqcCode">
              <el-input v-model="form.ipqcCode" placeholder="请输入检验单编号" />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item  label-width="80">
              <el-switch v-model="autoGenFlag"
                  active-color="#13ce66"
                  active-text="自动生成"
                  @change="handleAutoGenChange(autoGenFlag)" v-if="optType != 'view' && form.status =='PREPARE'">
              </el-switch>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="检验单名称" prop="ipqcName">
              <el-input v-model="form.ipqcName" placeholder="请输入检验单名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="检验类型" prop="ipqcType">
              <el-select v-model="form.ipqcType" placeholder="请选择检验类型">
                <el-option
                  v-for="dict in dict.type.mes_pqc_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="工单编码" prop="workorderCode">
              <el-input v-if="form.ipqcId == null" v-model="form.workorderCode" placeholder="请选择生产工单" >
                <el-button slot="append" icon="el-icon-search" @click="handleWorkorderSelect"></el-button>
              </el-input>
              <!--保存过则不允许修改工单，需要修改则删除重做-->
              <el-input v-else v-model="form.workorderCode" >
              </el-input>
            </el-form-item>
            <WorkorderSelect ref="woSelect" @onSelected="onWorkorderSelected"></WorkorderSelect>
          </el-col>
          <el-col :span="8">
            <el-form-item label="工单名称" prop="workorderName">
              <el-input v-model="form.workorderName" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="工作站编号" prop="workstationCode">
              <el-input v-model="form.workstationCode" placeholder="请输入工作站编号" >
                <el-button slot="append" icon="el-icon-search" @click="handleWorkstationSelect"></el-button>
              </el-input>
            </el-form-item>
            <WorkstationSelect ref="wsSelect"  @onSelected="onWorkstationSelected"> </WorkstationSelect>
          </el-col>
          <el-col :span="8">
            <el-form-item label="工作站名称" prop="workstationName">
              <el-input v-model="form.workstationName" readonly="readonly"  />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="检测数量" prop="quantityCheck">
              <el-input-number v-model="form.quantityCheck" :max="99999999" placeholder="请输入检测数量" @change="handleCheckQuantityChanged" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-collapse v-model="expandedSections">
          <el-collapse-item name="moreInfo">
            <template slot="title">
              更多信息<i class="header-icon el-icon-info"></i>
            </template>
            <el-row>
              <el-col :span="8">
                <el-form-item label="产品编码" prop="itemCode">
                  <el-input v-model="form.itemCode" placeholder="请输入产品编码" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="产品名称" prop="itemName">
                  <el-input v-model="form.itemName" placeholder="请输入产品名称" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="单位" prop="unitName">
                  <el-input v-model="form.unitName" placeholder="请输入单位" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="24">
                <el-form-item label="规格型号" prop="specification">
                  <el-input v-model="form.specification" type="textarea" placeholder="请输入内容" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="8">
                <el-form-item label="工序编码" prop="processCode">
                  <el-input v-model="form.processCode" placeholder="请输入工序编码" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="工序名称" prop="processName">
                  <el-input v-model="form.processName" placeholder="请输入工序名称" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="任务编号" prop="taskCode">
                  <el-input v-model="form.taskCode" placeholder="请输入任务编号" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="8">
                <el-form-item label="任务名称" prop="taskName">
                  <el-input v-model="form.taskName" placeholder="请输入任务名称" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="不合格数" prop="quantityUnqualified">
                  <el-input-number v-model="form.quantityUnqualified" :max="99999999" placeholder="请输入不合格数" @change="handleUnqualifiedChanged" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="合格品数量" prop="quantityQualified">
                  <el-input-number v-model="form.quantityQualified" :max="99999999" placeholder="请输入合格品数量" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row v-if="form.quantityUnqualified >0">
              <el-col :span="8">
                <el-form-item label="工废" prop="quantityLaborScrap">
                  <el-input-number :min="0" @change="handleScrapChanged" v-model="form.quantityLaborScrap" placeholder="请输入工废数量" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="料废" prop="quantityMaterialScrap">
                  <el-input-number :min="0" @change="handleScrapChanged" v-model="form.quantityMaterialScrap" placeholder="请输入料废数量" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="其他" prop="quantityOtherScrap">
                  <el-input-number :min="0" @change="handleScrapChanged" v-model="form.quantityOtherScrap" placeholder="请输入其他原因废品数量" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="8">
                <el-form-item label="致命缺陷数量" prop="crQuantity">
                  <el-input-number v-model="form.crQuantity" :max="99999999" placeholder="请输入致命缺陷数量" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="严重缺陷数量" prop="majQuantity">
                  <el-input-number v-model="form.majQuantity" :max="99999999" placeholder="请输入严重缺陷数量" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="轻微缺陷数量" prop="minQuantity">
                  <el-input-number v-model="form.minQuantity" :max="99999999" placeholder="请输入轻微缺陷数量" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-collapse-item>
        </el-collapse>

        <el-row>
          <el-col :span="8">
            <el-form-item label="检测日期" prop="inspectDate">
              <el-date-picker clearable
                v-model="form.inspectDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择检测日期">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="检测结论" prop="checkResult">
              <el-select v-model="form.checkResult" placeholder="请选择检验结论">
                <el-option
                  v-for="dict in dict.type.mes_qc_result"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="检测人员" prop="inspector">
              <el-input v-model="form.inspector" placeholder="请输入检测人员" />
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
      <el-tabs type="border-card" v-if="form.ipqcId != null">
          <el-tab-pane label="检测项">
            <Ipqcline ref=line :ipqcId="form.ipqcId" :optType="optType"></Ipqcline>
          </el-tab-pane>
          <el-tab-pane label="检测结果">
            <QCResutl ref="qcResult" :qcId="form.ipqcId" :qcType="'PQC'" :qcDetailType="form.qcDetailType" :optType="optType"></QCResutl>
          </el-tab-pane>
      </el-tabs>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm" v-if="form.status =='PREPARE' && optType !='view' ">保 存</el-button>
        <el-button type="success" @click="handleFinish" v-if="form.status =='PREPARE' && optType !='view'">完成检验</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import pipelineWorkorderContext from '@/mixins/pipelineWorkorderContext'
import { listIpqc, getIpqc, delIpqc, addIpqc, updateIpqc } from "@/api/mes/qc/ipqc";
import { listFeedback } from "@/api/mes/pro/feedback";
import { listProtask } from "@/api/mes/pro/protask";
import { listProductproduceline } from "@/api/mes/wm/productproduceline";
import { listProductprocess } from "@/api/mes/pro/routeprocess";
import { pendingTasks } from "@/utils/feedbackDefaults";
import { findOpenIpqc, inspectionDefaults, pickInspectionFeedback, pickProduceLine, qualifiedFromCheck } from "@/utils/ipqcDefaults";
import {genCode} from "@/api/system/autocode/rule"
import WorkorderSelect from "@/components/workorderSelect/single.vue"
import WorkstationSelect from "@/components/workstationSelect/simpletableSingle.vue"
import Ipqcline from "./line.vue"
import QCResutl from "../qcresult/index.vue";
import { getUserProfile } from "@/api/system/user";
export default {
  mixins: [pipelineWorkorderContext],
  name: "Ipqc",
  components: {WorkorderSelect,WorkstationSelect,Ipqcline,QCResutl},
  dicts: ['mes_pqc_type','mes_qc_result','mes_order_status'],
  data() {
    return {
      autoGenFlag:false,
      optType: undefined,
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
      // 过程检验单表格数据
      ipqcList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      expandedSections: ['moreInfo'],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        ipqcCode: null,
        ipqcName: null,
        ipqcType: null,
        templateId: null,
        workorderId: null,
        workorderCode: null,
        workorderName: null,
        taskId: null,
        taskCode: null,
        taskName: null,
        workstationId: null,
        workstationCode: null,
        workstationName: null,
        processId: null,
        processCode: null,
        processName: null,
        itemId: null,
        itemCode: null,
        itemName: null,
        specification: null,
        unitOfMeasure: null,
        quantityCheck: null,
        quantityUnqualified: null,
        quantityQualified: null,
        crRate: null,
        majRate: null,
        minRate: null,
        crQuantity: null,
        majQuantity: null,
        minQuantity: null,
        checkResult: null,
        inspectDate: null,
        inspector: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 当前登录用户
      user: {},
      // 自动带出过程：来源、提示文案
      inspectionLoading: false,
      inspectionTip: {
        type: "info",
        text: "选择生产工单后，系统按待检报工自动带出工序、工作站和检测数量，核对结论后点「完成检验」。"
      },
      // 表单校验
      rules: {
        ipqcCode: [
          { required: true, message: "请输入或生产检验单编号", trigger: "blur" }
        ],
        ipqcType: [
          { required: true, message: "请选择检验类型", trigger: "change" }
        ],

        workorderCode: [
          { required: true, message: "请选择生产工单", trigger: "blur" }
        ],

        workstationCode: [
          { required: true, message: "请选择工作站", trigger: "blur" }
        ],

        quantityCheck: [
          { required: true, message: "检测数量不能为空", trigger: "blur" }
        ],

        quantityUnqualified: [
          { required: true, message: "不合格品数量不能为空", trigger: "blur" }
        ],

        quantityQualified: [
          { required: true, message: "合格品数量不能为空", trigger: "blur" }
        ],
        remark: [
          { max: 250, message: '长度必须小于250个字符', trigger: 'blur' }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询过程检验单列表 */
    getList() {
      this.loading = true;
      listIpqc(this.queryParams).then(response => {
        this.ipqcList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.expandedSections = ['moreInfo'];
      this.form = {
        ipqcId: null,
        ipqcCode: null,
        ipqcName: null,
        ipqcType: null,
        templateId: null,
        workorderId: null,
        workorderCode: null,
        workorderName: null,
        taskId: null,
        taskCode: null,
        taskName: null,
        workstationId: null,
        workstationCode: null,
        workstationName: null,
        processId: null,
        processCode: null,
        processName: null,
        itemId: null,
        itemCode: null,
        itemName: null,
        specification: null,
        unitOfMeasure: null,
        quantityCheck: null,
        quantityUnqualified: null,
        quantityQualified: null,
        quantityLaborScrap: 0,
        quantityMaterialScrap: 0,
        quantityOtherScrap: 0,
        crRate: null,
        majRate: null,
        minRate: null,
        crQuantity: null,
        majQuantity: null,
        minQuantity: null,
        checkResult: null,
        inspectDate: new Date(),
        inspector: null,
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
      this.autoGenFlag = false;
      this.inspectionLoading = false;
      this.inspectionTip = { type: "info", text: "" };
      // 作废还在飞的取数请求，避免关闭弹窗后旧结果写回新表单
      this._inspectionRequestSequence = (this._inspectionRequestSequence || 0) + 1;
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
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.ipqcId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作：待检报工优先带出整张检验单，用户只需核对结论 */
    async handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加过程检验单";
      this.optType = "add";
      // 自制工序检验是过程检验的常见场景，编号直接自动生成，检测日期取当天
      this.form.ipqcType = "IPQC";
      this.form.inspectDate = this.formatDate(new Date(), "yyyy-MM-dd");
      this.form.checkResult = "ACCEPT";
      this.autoGenFlag = true;
      this.handleAutoGenChange(true);
      this.inspectionTip = { type: "info", text: "正在读取待检报工…" };
      await this.ensureCurrentUser();
      // 从首页流水线点进来时带着工单上下文，直接按该工单带出待检来源
      const workorderId = Number(this.queryParams.workorderId);
      await this.loadInspectionDefaults(workorderId > 0 ? workorderId : null);
    },
    /** 个人资料还没回来时补一次，检测人员默认当前用户 */
    async ensureCurrentUser() {
      if (!this.user || !this.user.userName) {
        try {
          const response = await getUserProfile();
          this.user = response.data || this.user || {};
        } catch (e) {
          // 取不到就保持手工填写，不阻断新增
        }
      }
      if (!this.form.inspector) this.form.inspector = this.user.nickName || this.user.userName || null;
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const ipqcId = row.ipqcId || this.ids
      getIpqc(ipqcId).then(response => {
        this.form = response.data;
        // 库里带时分秒的日期先截成 yyyy-MM-dd，避免日期框回填成空
        if (this.form.inspectDate) this.form.inspectDate = this.normalizeInspectDate(this.form.inspectDate);
        this.open = true;
        this.title = "修改过程检验单";
        this.optType = "edit";
      });
    },
    //查看单据
    handleView(row){
      this.reset();
      const ipqcIds = row.ipqcId
      getIpqc(ipqcIds).then(response => {
        this.form = response.data;
        // 库里带时分秒的日期先截成 yyyy-MM-dd，避免日期框回填成空
        if (this.form.inspectDate) this.form.inspectDate = this.normalizeInspectDate(this.form.inspectDate);
        this.open = true;
        this.title = "查看检验单信息";
        this.optType = "view";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.ipqcId != null) {
            updateIpqc(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addIpqc(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.form.ipqcId = response.data; //将返回的ID保存
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    //点击完成：新建时先存草稿再置完成，一次点击走通"建单+放行报工"
    handleFinish(){
      if(this.form.checkResult == null){
        this.$modal.msgError("请选择检测结论！");
        return;
      }
      this.$refs["form"].validate(valid => {
        if (!valid) return;
        const message = this.form.ipqcId == null
          ? '确认保存并完成检验？【完成后将不能更改】'
          : '是否完成检验单编制？【完成后将不能更改】';
        this.$modal.confirm(message).then(() => this.finishInspection()).catch(() => {});
      });
    },
    /** 完成检验：新建先落草稿，再由修改接口置为 FINISHED，联动才会按来源放行报工 */
    async finishInspection(){
      try {
        if(this.form.ipqcId == null){
          const response = await addIpqc(Object.assign({}, this.form, { status: "PREPARE" }));
          this.form.ipqcId = response.data;
        }
        await updateIpqc(Object.assign({}, this.form, { status: "FINISHED" }));
        this.form.status = "FINISHED";
        this.$modal.msgSuccess("检验完成");
        this.open = false;
        this.getList();
      } catch (e) {
        const message = (e && (e.msg || e.message)) || "完成检验失败，请刷新后重试";
        this.$modal.msgError(message);
      }
    },

    /** 删除按钮操作 */
    handleDelete(row) {
      const ipqcIds = row.ipqcId || this.ids;
      this.$modal.confirm('是否确认删除过程检验单编号为"' + ipqcIds + '"的数据项？').then(function() {
        return delIpqc(ipqcIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('mes/qc/ipqc/export', {
        ...this.queryParams
      }, `ipqc_${new Date().getTime()}.xlsx`)
    },
     //选择生产工单
    handleWorkorderSelect(){
      this.$refs.woSelect.handleOpen(this.form.workorderId)
    },
    onWorkorderSelected(row){
      if(row == undefined || row == null) return;
      this.form.workorderId = row.workorderId;
      this.form.workorderCode = row.workorderCode;
      this.form.workorderName = row.workorderName;
      this.form.itemId = row.productId;
      this.form.itemCode = row.productCode;
      this.form.itemName = row.productName;
      this.form.specification = row.productSpc;
      this.form.unitOfMeasure = row.unitOfMeasure;
      this.form.unitName = row.unitName;
      // 换工单就重新带出待检来源，避免沿用上一个工单的工序和数量
      this.clearInspectionSource();
      this.loadInspectionDefaults(row.workorderId);
    },
    /** 清空来源相关字段（换工单时用） */
    clearInspectionSource(){
      Object.assign(this.form, {
        taskId: null,
        taskCode: null,
        taskName: null,
        workstationId: null,
        workstationCode: null,
        workstationName: null,
        processId: null,
        processCode: null,
        processName: null,
        quantityCheck: null,
        quantityQualified: null,
        quantityUnqualified: null,
        quantityLaborScrap: 0,
        quantityMaterialScrap: 0,
        quantityOtherScrap: 0,
        crQuantity: 0,
        majQuantity: 0,
        minQuantity: 0,
        sourceDocType: null,
        sourceDocId: null,
        sourceDocCode: null,
        sourceLineId: null
      });
    },
    /**
     * 按"待检报工 → 最近报工 → 待报工工序"的顺序带出检验单。
     * 有产品产出行时一并写入来源单据：完成检验才会放行报工，否则只是一张孤儿单。
     */
    async loadInspectionDefaults(workorderId){
      const sequence = this._inspectionRequestSequence = (this._inspectionRequestSequence || 0) + 1;
      this.inspectionLoading = true;
      try {
        let feedbacks = [];
        if(workorderId){
          const response = await listFeedback({ workorderId: workorderId, pageNum: 1, pageSize: 50 });
          feedbacks = (response && response.rows) || [];
        }else{
          // 没有工单上下文时，用最近一张待检报工作为默认检验对象
          const response = await listFeedback({ status: "UNCHECK", pageNum: 1, pageSize: 10 });
          feedbacks = (response && response.rows) || [];
        }
        if(sequence !== this._inspectionRequestSequence) return;
        const feedback = pickInspectionFeedback(feedbacks);
        const targetId = workorderId || (feedback && feedback.workorderId) || null;
        if(!targetId){
          this.inspectionTip = { type: "warning", text: "当前没有待检报工：先在生产报工执行报工，或在工单下拉里选工单后手工填写。" };
          return;
        }
        this.form.workorderId = targetId;
        if(feedback){
          this.form.workorderCode = feedback.workorderCode;
          this.form.workorderName = feedback.workorderName;
        }
        let line = null;
        if(feedback && feedback.status === "UNCHECK"){
          const lineResponse = await listProductproduceline({ feedbackId: feedback.recordId });
          if(sequence !== this._inspectionRequestSequence) return;
          line = pickProduceLine((lineResponse && lineResponse.rows) || [], feedback);
          if(line){
            const existingResponse = await listIpqc({ sourceLineId: line.lineId, pageNum: 1, pageSize: 10 });
            if(sequence !== this._inspectionRequestSequence) return;
            const opened = findOpenIpqc((existingResponse && existingResponse.rows) || [], line);
            if(opened){
              // 报工执行时已自动建单：直接打开它，别再建一张放行不了的重复单
              this.form = opened;
              this.optType = "edit";
              this.autoGenFlag = false;
              this.title = "填写过程检验单（系统已自动生成 " + opened.ipqcCode + "）";
              this.fillInspectionResult();
              this.inspectionTip = { type: "success", text: "已打开系统为该报工自动生成的检验单 " + opened.ipqcCode + "，合格数已按检测数量填好，核对后点「完成检验」。" };
              return;
            }
          }
        }
        let task = null;
        if(!feedback){
          const taskResponse = await listProtask({ workorderId: targetId, pageNum: 1, pageSize: 200 });
          if(sequence !== this._inspectionRequestSequence) return;
          const rows = (taskResponse && taskResponse.rows) || [];
          const processes = await this.loadRouteProcessOrder(rows);
          if(sequence !== this._inspectionRequestSequence) return;
          task = pendingTasks(rows, processes)[0] || null;
        }
        Object.assign(this.form, inspectionDefaults({
          feedback: feedback,
          task: task,
          line: line,
          inspector: this.form.inspector,
          today: this.form.inspectDate
        }));
        if(line){
          this.inspectionTip = { type: "success", text: "已按待检报工 " + feedback.feedbackCode + " 带出「" + (this.form.processName || this.form.itemName) + "」与工作站「" + (this.form.workstationName || this.form.workstationCode || "-") + "」，检测数量 " + this.form.quantityCheck + "；核对结论后点「完成检验」。" };
        }else if(task){
          this.inspectionTip = { type: "info", text: "该工单还没有待检报工，已按待报工工序「" + (task.processName || task.taskName) + "」带出检测数量 " + this.form.quantityCheck + "。" };
        }else if(feedback){
          this.inspectionTip = { type: "info", text: "已按报工 " + (feedback.feedbackCode || "-") + " 带出检测数量 " + this.form.quantityCheck + "；该报工没有待检产出行，本次只作为手工检验记录。" };
        }else{
          this.inspectionTip = { type: "info", text: "该工单还没有排产任务，请手工填写工序、工作站和检测数量。" };
        }
      } catch (e) {
        if(sequence === this._inspectionRequestSequence){
          this.inspectionTip = { type: "warning", text: "待检来源读取失败，请刷新后重试，或手工选择工单、工序和数量。" };
        }
      } finally {
        if(sequence === this._inspectionRequestSequence) this.inspectionLoading = false;
      }
    },
    /** 工艺路线顺序，用于挑"第一道待报工工序" */
    async loadRouteProcessOrder(tasks){
      const productId = (tasks[0] || {}).itemId || this.form.itemId;
      if(productId == undefined || productId == null) return [];
      try {
        const response = await listProductprocess(productId);
        return (response && response.data) || [];
      } catch (e) {
        return [];
      }
    },
    /** 打开已有的自动检验单时，把结果类字段补成可直接完成的默认值 */
    fillInspectionResult(){
      const quantityCheck = Number(this.form.quantityCheck) || 0;
      if(!this.form.inspector) this.form.inspector = this.user.nickName || this.user.userName || null;
      this.form.inspectDate = this.normalizeInspectDate(this.form.inspectDate);
      if(!this.form.checkResult) this.form.checkResult = "ACCEPT";
      const qualified = Number(this.form.quantityQualified) || 0;
      const unqualified = Number(this.form.quantityUnqualified) || 0;
      if(qualified + unqualified === 0 && quantityCheck > 0){
        this.form.quantityQualified = quantityCheck;
        this.form.quantityUnqualified = 0;
      }
    },
    /** 日期选择器只认 yyyy-MM-dd，库里带时分秒的值先截断，避免回填后显示空 */
    normalizeInspectDate(value){
      const text = value == null ? "" : String(value);
      const matched = text.match(/^(\d{4})-(\d{2})-(\d{2})/);
      return matched ? matched[1] + "-" + matched[2] + "-" + matched[3] : this.formatDate(new Date(), "yyyy-MM-dd");
    },
    handleWorkstationSelect(){
      this.$refs.wsSelect.handleOpen(this.form.workstationId)
    },
    onWorkstationSelected(row){
      if(row != undefined && row != null){
        this.form.workstationId = row.workstationId;
        this.form.workstationCode = row.workstationCode;
        this.form.workstationName = row.workstationName;
      }
    },
    handleScrapChanged(){
      this.form.quantityUnqualified = (Number(this.form.quantityLaborScrap) || 0) +
        (Number(this.form.quantityMaterialScrap) || 0) +
        (Number(this.form.quantityOtherScrap) || 0);
      this.handleUnqualifiedChanged();
    },
    /** 检测数量变化时按"合格 = 检测 - 不合格"配平，保存和完成都要求三者相等 */
    handleCheckQuantityChanged(){
      this.form.quantityQualified = qualifiedFromCheck(this.form.quantityCheck, this.form.quantityUnqualified);
    },
    handleUnqualifiedChanged(){
      this.form.quantityQualified = qualifiedFromCheck(this.form.quantityCheck, this.form.quantityUnqualified);
    },
    //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        genCode('QC_PQC_CODE').then(response =>{
          this.form.ipqcCode = response;
        });
      }else{
        this.form.ipqcCode = null;
      }
    },
    /** 联动自动生成、尚未完成检验的行：闪烁高亮（attr1 以 AUTO: 开头且状态 PREPARE） */
    isAutoPending(row) {
      if (!row) return false
      return String(row.attr1 || '').indexOf('AUTO:') === 0 && row.status === 'PREPARE'
    },
    rowClassName({ row }) {
      return this.isAutoPending(row) ? 'row-auto-pending' : ''
    }
  }
};
</script>

<style scoped>
.ipqc-auto-hint {
  margin-bottom: 14px;
}

::v-deep .el-table .row-auto-pending > td {
  background-color: #fff8e1 !important;
  animation: auto-pending-flash 1.6s ease-in-out infinite;
}
::v-deep .el-table .row-auto-pending:hover > td {
  background-color: #fff1c9 !important;
}
@keyframes auto-pending-flash {
  0%, 100% { background-color: #fff8e1; }
  50% { background-color: #ffe9a8; }
}
</style>

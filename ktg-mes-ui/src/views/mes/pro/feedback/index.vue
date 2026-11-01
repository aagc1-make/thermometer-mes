<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="报工类型" prop="feedbackType">
        <el-select v-model="queryParams.feedbackType" placeholder="请选择报工类型" clearable>
          <el-option
            v-for="dict in dict.type.mes_feedback_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="工作站名称" prop="workstationName">
        <el-input
          v-model="queryParams.workstationName"
          placeholder="请输入工作站名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="生产工单编号" prop="workorderCode">
        <el-input
          v-model="queryParams.workorderCode"
          placeholder="请输入生产工单编号"
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
      <el-form-item label="报工人" prop="userName">
        <el-input
          v-model="queryParams.userName"
          placeholder="请输入报工人名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="记录人" prop="recordUser">
        <el-input
          v-model="queryParams.recordUser"
          placeholder="请输入记录人"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option
            v-for="dict in dict.type.mes_order_status"
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
          v-hasPermi="['mes:pro:feedback:add']"
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
          v-hasPermi="['mes:pro:feedback:edit']"
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
          v-hasPermi="['mes:pro:feedback:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="feedbackList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="报工单号" align="center" prop="feedbackCode" width="150px">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            @click="handleView(scope.row)"
            v-hasPermi="['mes:pro:feedback:query']"
          >{{scope.row.feedbackCode}}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="报工类型" align="center" prop="feedbackType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.mes_feedback_type" :value="scope.row.feedbackType"/>
        </template>
      </el-table-column>
      <el-table-column label="工作站"  align="center" prop="workstationName" />
      <el-table-column label="工序" width="150px" align="center" prop="processName" />
      <el-table-column label="生产工单编号" width="150px" align="center" prop="workorderCode" />
      <el-table-column label="产品物料编码" width="120px" align="center" prop="itemCode" />
      <el-table-column label="产品物料名称" width="150px" align="center" prop="itemName" />
      <el-table-column label="规格型号" align="center" prop="specification" />
      <el-table-column label="单位" align="center" prop="unitName" />
      <el-table-column label="报工数量" align="center" prop="quantityFeedback" />
      <el-table-column label="报工人" align="center" prop="nickName" />
      <el-table-column label="报工时间" align="center" prop="feedbackTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.feedbackTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="审核人" align="center" prop="recordNick" />
      <el-table-column label="状态" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.mes_feedback_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="100px" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-check"
            @click="handleApprove(scope.row)"
            v-if="scope.row.status =='APPROVING' && scope.row.recordUser == user.userName"
            v-hasPermi="['mes:pro:feedback:approve']"
          >审核</el-button>        
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['mes:pro:feedback:edit']"
            v-if="scope.row.status =='PREPARE'"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['mes:pro:feedback:remove']"
            v-if="scope.row.status =='PREPARE'"
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

    <!-- 添加或修改生产报工记录对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-alert
          v-if="optType === 'add'"
          class="fb-auto-hint"
          :closable="false"
          show-icon
          :type="autoHint.type"
          :title="autoHint.text"
        />
        <el-row>
          <el-col :span="8">
            <el-form-item label="报工类型" prop="feedbackType">
              <el-select v-model="form.feedbackType" placeholder="请选择报工类型">
                <el-option
                  v-for="dict in dict.type.mes_feedback_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="生产工单" prop="workorderCode">
              <el-input v-model="form.workorderCode" readonly="readonly" placeholder="点这里选择生产工单" @click.native="handleWorkorderSelect">
                <el-button slot="append" icon="el-icon-search" @click="handleWorkorderSelect"></el-button>
              </el-input>
            </el-form-item>
            <WorkorderSelect ref="woSelect" @onSelected="onWorkorderSelected"></WorkorderSelect>
          </el-col>
          <el-col :span="8">
            <el-form-item label="报工单号" prop="feedbackCode">
              <el-input v-model="form.feedbackCode" readonly="readonly" placeholder="系统自动生成" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="生产任务" prop="taskId">
              <el-select
                v-if="optType === 'add'"
                v-model="form.taskId"
                class="fb-task-select"
                filterable
                clearable
                :loading="taskLoading"
                :disabled="!form.workorderId"
                :placeholder="form.workorderId ? '自动带出待报工工序' : '请先选择生产工单'"
                @change="onTaskChange"
              >
                <el-option
                  v-for="task in taskOptions"
                  :key="task.taskId"
                  :label="taskLabel(task)"
                  :value="task.taskId"
                />
              </el-select>
              <el-input v-else v-model="form.taskCode" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工作站" prop="workstationName">
              <el-input v-model="form.workstationName" readonly="readonly" placeholder="由生产任务自动带出" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="产品编码" prop="itemCode">
              <el-input v-model="form.itemCode" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="产品名称" prop="itemName">
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
          <el-col :span="24">
            <el-form-item label="规格型号" prop="specification">
              <el-input v-model="form.specification" type="textarea" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="form.isCheck == 'N'">
          <el-col :span="8">
            <el-form-item label="报工数量" prop="quantityFeedback">
              <el-input :value="form.quantityFeedback" inputmode="decimal" placeholder="请输入报工数量" @input="handleFeedbackQuantityInput" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="合格品数量" prop="quantityQualified">
              <el-input-number :min="0" @change="handleQuantityChanged" v-model="form.quantityQualified" placeholder="请输入合格品数量" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="不良品数量" prop="quantityUnquanlified">
              <el-input-number :min="0" @change="handleQuantityChanged" v-model="form.quantityUnquanlified" placeholder="请输入不良品数量" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-else>
          <el-col :span="8">
            <el-form-item label="报工数量" prop="quantityFeedback">
              <el-input-number :min="0" :max="99999" v-model="form.quantityFeedback" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="form.isCheck == 'N' && form.quantityUnquanlified >0">
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
            <el-form-item label="报工人" prop="nickName">
              <el-input v-model="form.nickName" placeholder="请选择报工人" >
                <el-button slot="append" @click="handleUserSelect" icon="el-icon-search"></el-button>
              </el-input>
            </el-form-item>
            <UserSingleSelect ref="userSelect" @onSelected="onUserSelected"></UserSingleSelect>
          </el-col>
          <el-col :span="8">
            <el-form-item label="报工时间" prop="feedbackTime">
              <el-date-picker clearable
                v-model="form.feedbackTime"
                type="datetime"                
                value-format="yyyy-MM-dd HH:mm:ss"
                placeholder="请选择日期时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="审核人" prop="recordNick">
              <el-input v-model="form.recordNick" placeholder="请选择审核人" >
                <el-button slot="append" @click="handleUser2Select" icon="el-icon-search"></el-button>
              </el-input>
            </el-form-item>
            <UserSingleSelect ref="user2Select" @onSelected="onUser2Selected"></UserSingleSelect>
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
      <el-tabs type="border-card"  v-model="activeName"  v-if="form.recordId != null && form.status !='PREPARE' && form.status !='APPROVING' ">
        <el-tab-pane label="BOM物资消耗" name="consume">
          <ItemConsume :feedbackId="form.recordId"></ItemConsume>
        </el-tab-pane>
        <el-tab-pane label="产品产出" name="produce">
          <ProductProduce :feedbackId="form.recordId"></ProductProduce>
        </el-tab-pane>
      </el-tabs>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm" v-if="form.status =='PREPARE' && optType !='view' ">保 存</el-button>
        <el-button type="primary" @click="handleSubmit" v-if="form.status =='PREPARE' && optType !='view' && form.recordId !=null ">提交审批</el-button>
        <el-button type="success" @click="approve" v-if="form.status =='APPROVING'  && form.recordId !=null && form.recordUser == user.userName">审批通过</el-button>
        <el-button type="danger"  @click="handleReject" v-if="form.status =='APPROVING'  && form.recordId !=null && form.recordUser == user.userName">审批不通过</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import pipelineWorkorderContext from '@/mixins/pipelineWorkorderContext'
import { listFeedback, getFeedback, delFeedback, addFeedback, updateFeedback, execute } from "@/api/mes/pro/feedback";
import { listProtask } from "@/api/mes/pro/protask";
import { listProductprocess } from "@/api/mes/pro/routeprocess";
import WorkorderSelect from "@/components/workorderSelect/single.vue"
import UserSingleSelect from "@/components/userSelect/single.vue"
import ProductProduce from './produce.vue';
import ItemConsume from './consume.vue'
import { getUserProfile } from "@/api/system/user";
import { defaultTask, openTaskIds, pendingTasks, quantityDefaults, taskRemaining } from "@/utils/feedbackDefaults";

export default {
  mixins: [pipelineWorkorderContext],
  name: "Feedback",
  components: {WorkorderSelect, UserSingleSelect, ItemConsume, ProductProduce},
  dicts: ['mes_feedback_status', 'mes_feedback_type'],
  data() {
    return {
      user: {}, // 当前登录用户
      activeName: "consume", // 选项卡名称
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
      // 生产报工记录表格数据
      feedbackList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        feedbackType: null,
        workstationId: null,
        workstationCode: null,
        workstationName: null,
        workorderId: null,
        workorderCode: null,
        workorderName: null,
        taskId: null,
        taskCode: null,
        itemId: null,
        itemCode: null,
        itemName: null,
        unitOfMeasure: null,
        specification: null,
        quantity: null,
        quantityFeedback: null,
        quantityQualified: null,
        quantityUnquanlified: null,
        userName: null,
        nickName: null,
        feedbackChannel: null,
        feedbackTime: null,
        recordUser: null,
        recordNick: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 该工单可报工的工序任务；按工艺路线顺序排序，首条自动带出任务与工作站
      taskOptions: [],
      taskLoading: false,
      // 表单校验
      rules: {
        feedbackType: [
          { required: true, message: "报工类型不能为空", trigger: "change" }
        ],
        taskId: [
          { required: true, message: "请选择生产任务", trigger: "change" }
        ],       
        quantityFeedback: [
          { required: true, message: "请填写报工数量", trigger: "blur" },
          { validator: (rule, value, callback) => this.validateFeedbackQuantity(rule, value, callback), trigger: 'blur' }
        ],
        nickName: [
          { required: true, message: "请选择报工人", trigger: "blur"}
        ],
        recordNick: [
          { required: true, message: "请选择审核人", trigger: "blur"}
        ],
        feedbackTime: [
          { required: true, message: "请选择报工时间", trigger: "blur"}
        ],
        remark: [
          { max: 250, message: '长度必须小于250个字符', trigger: 'blur' }
        ]
      }
    };
  },
  computed: {
    // 当前带出的工序任务
    currentTask() {
      return this.taskOptions.find(task => String(task.taskId) === String(this.form.taskId)) || null;
    },
    // 弹窗顶部的自动带出提示：告诉用户"已经替你做完了什么、现在只需要核对什么"
    autoHint() {
      if (!this.form.workorderId) {
        return { type: "info", text: "选择生产工单后，系统按工艺顺序自动带出待报工任务、工作站和数量，核对后直接保存。" };
      }
      if (this.taskLoading) return { type: "info", text: "正在按工艺路线带出待报工任务…" };
      if (!this.taskOptions.length) {
        return { type: "warning", text: "该工单没有可报工的工序任务：可能尚未排产或已全部报工，请先检查生产排产。" };
      }
      if (!this.currentTask) {
        const first = this.taskOptions[0];
        return first.hasOpenFeedback
          ? { type: "warning", text: "「" + (first.processName || first.taskName) + "」已有未完成的报工单，请先在列表处理，避免重复报工。" }
          : { type: "info", text: "该工单还有可报工的工序任务，请在任务下拉里选择。" };
      }
      const task = this.currentTask;
      if (task.hasOpenFeedback) {
        return { type: "warning", text: "「" + (task.processName || task.taskName) + "」已有未完成的报工单，请先在列表处理，避免重复报工。" };
      }
      const unit = task.unitName ? " " + task.unitName : "";
      return { type: "success", text: "已自动带出「" + (task.processName || task.taskName) + "」任务与工作站「" + task.workstationName + "」，剩余可报 " + taskRemaining(task) + unit + "；核对数量后直接保存。" };
    }
  },
  created() {
    this.getList();
    getUserProfile().then(response => {      
      this.user = response.data;
    });
  },
  methods: {
    /** 查询生产报工记录列表 */
    getList() {
      this.loading = true;
      listFeedback(this.queryParams).then(response => {
        this.feedbackList = response.rows;
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
      this.form = {
        recordId: null,
        feedbackCode: null,
        // 默认"自行报工"，免去每次手选；报工人、审核人默认当前登录用户，可在弹窗里改
        feedbackType: "SELF",
        workstationId: null,
        workstationCode: null,
        workstationName: null,
        routeId: null,
        routeCode: null,
        processId: null,
        processCode: null,
        processName: null,
        workorderId: null,
        workorderCode: null,
        workorderName: null,
        taskId: null,
        taskCode: null,
        itemId: null,
        itemCode: null,
        itemName: null,
        specification: null,
        unitOfMeasure: null,
        unitName: null,
        quantityFeedback: 0,
        quantityQualified: 0,
        quantityUnquanlified: 0,
        quantityLaborScrap: 0,
        quantityMaterialScrap: 0,
        quantityOtherScrap: 0,
        userName: null,
        nickName: null,
        feedbackChannel: "PC",
        feedbackTime: this.formatDate(new Date(),"yyyy-MM-dd HH:mm:ss"),
        recordUser: null,
        recordNick: null,
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
      this.form.userId = this.user.userId;
      this.form.userName = this.user.userName;
      this.form.nickName = this.user.nickName;
      // 单人工位/单人值守场景不需要再选两个人：默认由当前用户报工并审核。
      this.form.recordUser = this.user.userName;
      this.form.recordNick = this.user.nickName;
      this.taskOptions = [];
      this.taskLoading = false;
      // 作废还在飞的取任务请求，避免关闭弹窗后旧结果写回新表单
      this._taskRequestSequence = (this._taskRequestSequence || 0) + 1;
      this.resetForm("form");
    },
    handleFeedbackQuantityInput(value) {
      this.form.quantityFeedback = value;
      if (value === '' || !/^(?:\d+(?:\.\d*)?|\.\d+)$/.test(String(value))) return;
      const quantity = Number(value);
      const unqualified = Number(this.form.quantityUnquanlified) || 0;
      if (Number.isFinite(quantity) && quantity >= unqualified) {
        this.form.quantityQualified = Number((quantity - unqualified).toFixed(8));
      }
    },
    validateFeedbackQuantity(rule, value, callback) {
      const text = String(value == null ? '' : value).trim();
      const quantity = Number(text);
      if (!/^(?:\d+(?:\.\d*)?|\.\d+)$/.test(text) || !Number.isFinite(quantity) || quantity <= 0) {
        callback(new Error('报工数量必须大于 0'));
      } else if (this.form.isCheck === 'N' && quantity < (Number(this.form.quantityUnquanlified) || 0)) {
        callback(new Error('报工数量不能小于不良品数量，请先调整不良品数量'));
      } else {
        callback();
      }
    },
    handleQuantityChanged(){
      const qualified = Number(this.form.quantityQualified) || 0;
      const unqualified = Number(this.form.quantityUnquanlified) || 0;
      this.form.quantityFeedback = Number((qualified + unqualified).toFixed(8));
    },
    handleScrapChanged(){
      const labor = Number(this.form.quantityLaborScrap) || 0;
      const material = Number(this.form.quantityMaterialScrap) || 0;
      const other = Number(this.form.quantityOtherScrap) || 0;
      this.form.quantityUnquanlified = Number((labor + material + other).toFixed(8));
      this.handleQuantityChanged();
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
      this.ids = selection.map(item => item.recordId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作：默认值补齐后，只让用户核对数量就能保存 */
    async handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加生产报工记录";
      this.optType = "add";
      // 弹窗会被复用，重置后把报工时间刷成当前时间，避免沿用上次打开的时间
      this.form.feedbackTime = this.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss");
      await this.ensureCurrentUser();
      // 从首页流水线点进来时带着工单上下文，直接按该工单带出待报工工序
      const workorderId = Number(this.queryParams.workorderId);
      if (workorderId > 0) {
        this.form.workorderId = workorderId;
        this.form.workorderCode = this.queryParams.workorderCode || null;
        await this.loadWorkorderTasks(workorderId);
      }
    },
    /** 个人资料还没回来时补一次，避免保存时卡在"请选择报工人/审核人" */
    async ensureCurrentUser() {
      if (this.form.userName && this.form.recordUser) return;
      try {
        const response = await getUserProfile();
        this.user = response.data || this.user;
        this.form.userId = this.user.userId;
        this.form.userName = this.user.userName;
        this.form.nickName = this.user.nickName;
        this.form.recordUser = this.user.userName;
        this.form.recordNick = this.user.nickName;
      } catch (e) {
        // 取不到就保持手工选择，不阻断新增
      }
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const recordId = row.recordId || this.ids
      getFeedback(recordId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改生产报工记录";
        this.optType = "edit";
      });
    },

    handleView(){
      this.reset();
      const recordId = row.recordId || this.ids
      getFeedback(recordId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "查看生产报工记录";
        this.optType = "view";
      });
    },

    /**
     * 审批审核
     */
    handleApprove(row){
      this.reset();
      const recordId = row.recordId || this.ids
      getFeedback(recordId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "生产报工审核";
        this.optType = "edit";
      });
    },

    // 查询明细按钮操作
    handleView(row){
      this.reset();
      const recordId = row.recordId || this.ids;
      getFeedback(recordId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "查看生产报工单信息";
        this.optType = "view";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.recordId != null) {
            updateFeedback(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open=false;
              this.getList();

            });
          } else {
            addFeedback(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open=false;
              this.getList();
            });
          }
        }
      });
    },
    handleSubmit(){
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.form.status = "APPROVING";
          if (this.form.recordId != null) {
            updateFeedback(this.form).then(response => {
              this.$modal.msgSuccess("提交成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    //审批通过执行
    approve(){
      const recordIds = this.form.recordId;
      this.$modal.confirm('确认执行报工？').then(function() {
        return execute(recordIds)//执行报工
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("执行成功");
        this.open = false;
      }).catch(() => {});
    },
    //审批不通过，拒绝
    handleReject(){
      this.form.status = "PREPARE";
      if (this.form.recordId != null) {
        updateFeedback(this.form).then(response => {
          this.$modal.msgSuccess("提交成功");
          this.open = false;
          this.getList();
        });
      }
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const recordIds = row.recordId || this.ids;
      this.$modal.confirm('是否确认删除生产报工记录编号为"' + recordIds + '"的数据项？').then(function() {
        return delFeedback(recordIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('pro/feedback/export', {
        ...this.queryParams
      }, `feedback_${new Date().getTime()}.xlsx`)
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
      // 换工单就重新按工艺顺序带出待报工工序，避免沿用上一个工单的任务
      this.clearTaskFields();
      this.loadWorkorderTasks(row.workorderId);
    },
    /** 按工单带出可报工的工序任务，并默认选中第一道待报工工序 */
    async loadWorkorderTasks(workorderId){
      // 连续换工单时只认最后一次请求，避免旧结果盖掉新工单
      const sequence = this._taskRequestSequence = (this._taskRequestSequence || 0) + 1;
      this.taskOptions = [];
      if(workorderId == undefined || workorderId == null || workorderId === "") return;
      this.taskLoading = true;
      try {
        const [taskResponse, feedbackResponse] = await Promise.all([
          listProtask({ workorderId: workorderId, pageNum: 1, pageSize: 200 }),
          listFeedback({ workorderId: workorderId, pageNum: 1, pageSize: 200 }).catch(() => ({ rows: [] }))
        ]);
        if(sequence !== this._taskRequestSequence) return;
        const tasks = (taskResponse && taskResponse.rows) || [];
        const openIds = openTaskIds((feedbackResponse && feedbackResponse.rows) || []);
        const processes = await this.loadRouteProcessOrder(tasks);
        if(sequence !== this._taskRequestSequence) return;
        this.taskOptions = pendingTasks(tasks, processes).map(task => Object.assign({}, task, {
          hasOpenFeedback: openIds.has(String(task.taskId))
        }));
        const picked = defaultTask(tasks, processes, openIds);
        if(picked){
          this.applyTask(this.taskOptions.find(task => String(task.taskId) === String(picked.taskId)) || picked);
        }
      } catch (e) {
        if(sequence === this._taskRequestSequence) this.taskOptions = [];
        if(this.$modal) this.$modal.msgError("生产任务加载失败，请刷新后重试");
      } finally {
        if(sequence === this._taskRequestSequence) this.taskLoading = false;
      }
    },
    /** 工艺路线顺序：优先按路线工序顺序排，未配路线时按任务创建顺序兜底 */
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
    /** 任务下拉文案：工序 · 任务编号（剩余可报/排产数量） */
    taskLabel(task){
      const unit = task.unitName ? " " + task.unitName : "";
      return (task.processName || task.taskName) + " · " + task.taskCode +
        "（剩余 " + taskRemaining(task) + "/" + (Number(task.quantity) || 0) + unit + "）" +
        (task.hasOpenFeedback ? " · 已有未完成报工单" : "");
    },
    onTaskChange(taskId){
      const task = this.taskOptions.find(row => String(row.taskId) === String(taskId));
      if(task){
        this.applyTask(task);
      } else {
        this.clearTaskFields();
      }
    },
    /** 把任务上的工作站、工序、产品信息和默认数量一次性填好，用户只需核对数量 */
    applyTask(task){
      if(task == undefined || task == null) return;
      this.form.taskId = task.taskId;
      this.form.taskCode = task.taskCode;
      this.form.taskName = task.taskName;
      this.form.workstationId = task.workstationId;
      this.form.workstationCode = task.workstationCode;
      this.form.workstationName = task.workstationName;
      this.form.workorderId = task.workorderId;
      this.form.workorderCode = task.workorderCode;
      this.form.workorderName = task.workorderName;
      this.form.itemId = task.itemId;
      this.form.itemCode = task.itemCode;
      this.form.itemName = task.itemName;
      this.form.specification = task.specification;
      this.form.unitOfMeasure = task.unitOfMeasure;
      this.form.unitName = task.unitName;
      this.form.processId = task.processId;
      this.form.processCode = task.processCode;
      this.form.processName = task.processName;
      this.form.routeId = task.routeId;
      this.form.routeCode = task.routeCode;
      this.form.isCheck = task.isCheck;
      // 默认把该工序剩余可报量一次报完；免检工序同步拆出合格/不良两栏，保证合计等于报工数量
      const defaults = quantityDefaults(task);
      this.form.quantityFeedback = defaults.quantityFeedback;
      this.form.quantityQualified = defaults.quantityQualified;
      this.form.quantityUnquanlified = defaults.quantityUnquanlified;
      this.form.quantityLaborScrap = 0;
      this.form.quantityMaterialScrap = 0;
      this.form.quantityOtherScrap = 0;
    },
    /** 清空任务相关字段：换工单或清空任务时不能把上一张任务的工作站带过去 */
    clearTaskFields(){
      Object.assign(this.form, {
        taskId: null,
        taskCode: null,
        taskName: null,
        workstationId: null,
        workstationCode: null,
        workstationName: null,
        routeId: null,
        routeCode: null,
        processId: null,
        processCode: null,
        processName: null,
        isCheck: null,
        quantityFeedback: 0,
        quantityQualified: 0,
        quantityUnquanlified: 0,
        quantityLaborScrap: 0,
        quantityMaterialScrap: 0,
        quantityOtherScrap: 0
      });
    },
    //点击人员选择按钮
    handleUserSelect(){
        this.$refs.userSelect.showFlag = true;
    },
    //人员选择返回
    onUserSelected(row){
        this.form.nickName = row.nickName;
        this.form.userName = row.userName;
    },
    //点击人员选择按钮
    handleUser2Select(){
        this.$refs.user2Select.showFlag = true;
    },
    //人员选择返回
    onUser2Selected(row){
        this.form.recordUser = row.userName;
        this.form.recordNick = row.nickName;
    },
  }
};
</script>

<style scoped>
.fb-auto-hint {
  margin-bottom: 14px;
}

.fb-task-select {
  width: 100%;
}
</style>

<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="领料单编号" prop="issueCode">
        <el-input
          v-model="queryParams.issueCode"
          placeholder="请输入领料单编号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="领料单名称" prop="issueName">
        <el-input
          v-model="queryParams.issueName"
          placeholder="请输入领料单名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="仓库名称" prop="warehouseName">
        <el-input
          v-model="queryParams.warehouseName"
          placeholder="请输入仓库名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="领料日期" prop="issueDate">
        <el-date-picker clearable
          v-model="queryParams.issueDate"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择领料日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="单据状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择单据状态" clearable>
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
          v-hasPermi="['mes:wm:issueheader:add']"
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
          v-hasPermi="['mes:wm:issueheader:edit']"
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
          v-hasPermi="['mes:wm:issueheader:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['mes:wm:issueheader:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="issueheaderList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="领料单编号" align="center" width="150px" prop="issueCode" >
        <template slot-scope="scope">
          <el-button
            type="text"
            @click="handleView(scope.row)"
            v-hasPermi="['mes:wm:issueheader:query']"
          >{{scope.row.issueCode}}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="领料单名称" align="center" prop="issueName" :show-overflow-tooltip="true"/>
      <el-table-column label="生产工单" align="center" prop="workorderCode" />
      <el-table-column label="工作站" align="center" prop="workstationName" />
      <el-table-column label="客户编号" align="center" prop="clientCode" />
      <el-table-column label="客户名称" align="center" prop="clientName" />
      <el-table-column label="需求时间" align="center" prop="requiredTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.requiredTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="单据状态" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.mes_issue_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="210" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            v-if="['PREPARE', 'APPROVING'].includes(scope.row.status)"
            type="text" size="mini" icon="el-icon-magic-stick"
            :loading="picking && form.issueId === scope.row.issueId" :disabled="picking"
            @click="handleQuickPick(scope.row)" v-hasPermi="['mes:wm:issueheader:edit']"
          >一键拣货</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-video-play"
            v-if="scope.row.status =='APPROVED'"
            @click="handleExecute(scope.row)"
            v-hasPermi="['mes:wm:issueheader:edit']"
          >执行领出</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-shopping-cart-2"
            v-if="['APPROVING', 'APPROVED'].includes(scope.row.status)"
            @click="handleStocking(scope.row)"
            v-hasPermi="['mes:wm:issueheader:edit']"
          >分配详情</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            v-if="scope.row.status =='PREPARE'"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['mes:wm:issueheader:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            v-if="scope.row.status =='PREPARE'"
            @click="handleDelete(scope.row)"
            v-hasPermi="['mes:wm:issueheader:remove']"
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

    <!-- 添加或修改生产领料单头对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="1120px" class="issue-dialog" append-to-body
      :close-on-click-modal="!picking" :close-on-press-escape="!picking" :show-close="!picking">
      <el-form v-if="form.status === 'PREPARE'" ref="form" :model="form" :rules="rules" label-width="100px" :disabled="optType === 'view' || picking">
        <el-row>
          <el-col :span="8">
            <el-form-item label="领料单编号" prop="issueCode">
              <el-input v-model="form.issueCode" :readonly="autoGenFlag" placeholder="自动生成领料单编号" />
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
            <el-form-item label="领料单名称" prop="issueName">
              <el-input v-model="form.issueName" placeholder="选择工单后自动带入" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
             <el-form-item label="需求时间" prop="requiredTime">
              <el-date-picker clearable
                v-model="form.requiredTime"
                type="datetime"                
                value-format="yyyy-MM-dd HH:mm:ss"
                placeholder="请选择需求日期时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="生产工单" prop="workorderCode">
              <el-input v-model="form.workorderCode" readonly placeholder="请选择生产工单" >
                <el-button slot="append" icon="el-icon-search" @click="handleWorkorderSelect"></el-button>
              </el-input>
            </el-form-item>
            <WorkorderSelect ref="woSelect" @onSelected="onWorkorderSelected"></WorkorderSelect>
          </el-col>
          <el-col :span="8">
            <el-form-item label="工作站" prop="workstationCode">
              <el-input v-model="form.workstationCode" readonly placeholder="未指定，可按需选择" >
                <el-button slot="append" icon="el-icon-search" @click="handleWorkstationSelect"></el-button>
              </el-input>
            </el-form-item>
            <WorkstationSelect ref="wsSelect" @onSelected="onWorkstationSelected"> </WorkstationSelect>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="工作站名称" prop="workstationName">
              <el-input v-model="form.workstationName" readonly placeholder="请选择工作站" >
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="客户编号" >
              <el-input v-model="form.clientCode" readonly placeholder="请输入客户编号" >
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="客户名称" >
              <el-input v-model="form.clientName" readonly placeholder="请输入客户名称" >
              </el-input>
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
      <div v-else class="issue-context">
        <div class="issue-context__title">
          <div><span class="issue-eyebrow">生产领料单</span><h3>{{ form.issueName || form.issueCode }}</h3></div>
          <dict-tag :options="dict.type.mes_issue_status" :value="form.status" />
        </div>
        <div class="issue-context__fields">
          <div><span>领料单号</span><strong>{{ form.issueCode }}</strong></div>
          <div><span>生产工单</span><strong>{{ form.workorderCode || '未关联' }}</strong></div>
          <div><span>工作站</span><strong>{{ form.workstationName || '随工单安排' }}</strong></div>
          <div><span>需求时间</span><strong>{{ parseTime(form.requiredTime, '{y}-{m}-{d} {h}:{i}') || '未指定' }}</strong></div>
          <div v-if="form.clientName"><span>客户</span><strong>{{ form.clientName }}</strong></div>
        </div>
      </div>
      <div v-if="form.issueId && optType !== 'view' && ['PREPARE', 'APPROVING', 'APPROVED'].includes(form.status)" class="issue-pick-bar">
        <div class="issue-pick-bar__icon"><i :class="form.status === 'APPROVED' ? 'el-icon-circle-check' : 'el-icon-magic-stick'"></i></div>
        <div class="issue-pick-bar__content">
          <h3>{{ form.status === 'APPROVED' ? '物料已拣齐，可以领出' : '整单物料，一键备齐' }}</h3>
          <p>{{ form.status === 'APPROVED' ? '批次、库位与数量已填写完成，展开物料可核对分配明细。' : '自动按先进先出匹配批次和库位，补足剩余需求，拣齐后自动进入待领出。' }}</p>
        </div>
        <el-button v-if="form.status !== 'APPROVED'" type="primary" icon="el-icon-magic-stick" :loading="picking" @click="handleAutoPick" v-hasPermi="['mes:wm:issueheader:edit']">{{ picking ? '正在匹配整单库存' : '一键拣货' }}</el-button>
        <template v-else>
          <el-button :disabled="picking" icon="el-icon-edit" @click="handleAdjustPick" v-hasPermi="['mes:wm:issueheader:edit']">调整分配</el-button>
          <el-button :disabled="picking" type="primary" icon="el-icon-video-play" @click="handleExecute(form)" v-hasPermi="['mes:wm:issueheader:edit']">执行领出</el-button>
        </template>
      </div>
      <el-alert v-if="pickError" :title="pickError" type="warning" show-icon :closable="false" class="issue-pick-error" />
      <el-divider v-if="form.issueId !=null" content-position="center">物料信息</el-divider>
      <el-card shadow="never" v-if="form.issueId !=null && form.status =='PREPARE'" class="box-card">
        <Issueline ref="line" :issueId="form.issueId" :warehouseId="form.warehouseId" :locationId="form.locationId" :areaId="form.areaId" :optType="optType"></Issueline>
      </el-card>
      <el-card shadow="never" v-if="form.issueId !=null && form.status !='PREPARE'" class="box-card">
        <IssueDetail ref="detail" :issueId="form.issueId" :status="form.status" :optType="optType"></IssueDetail>
      </el-card>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="saveForm" v-if="form.status =='PREPARE' && optType !='view' ">保 存</el-button>
        <el-button type="warning" @click="submitToStock" v-if="form.status =='PREPARE' && form.issueId !=null && optType !='view' ">提 交</el-button>
        <el-button :disabled="picking" @click="submitToExecute" v-if="form.status =='APPROVING' && form.issueId !=null && optType !='view' ">完成手工拣货</el-button>
        <el-button :disabled="picking" type="text" @click="cancel" v-if="['APPROVING', 'APPROVED'].includes(form.status) && optType !='view'">撤销领料单</el-button>
        <el-button :disabled="picking" @click="close">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import pipelineWorkorderContext from '@/mixins/pipelineWorkorderContext'
import { listIssueheader, getIssueheader, checkQuantity, delIssueheader, addIssueheader, updateIssueheader, execute, autoPickIssue } from "@/api/mes/wm/issueheader";
import WorkstationSelect from "@/components/workstationSelect/simpletableSingle.vue"
import WorkorderSelect from "@/components/workorderSelect/single.vue"
import {getTreeList} from "@/api/mes/wm/warehouse"
import {genCode} from "@/api/system/autocode/rule"
import Issueline from "./line.vue";
import IssueDetail from "./detail.vue";
export default {
  mixins: [pipelineWorkorderContext],
  name: "Issueheader",
  dicts: ['mes_issue_status'],
  components: {Issueline,IssueDetail,WorkstationSelect,WorkorderSelect},
  data() {
    return {
      autoGenFlag:false,
      picking: false,
      pickError: '',
      defaultRequiredTime: null,
      optType: undefined,
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
      // 生产领料单头表格数据
      issueheaderList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        issueCode: null,
        issueName: null,
        workstationId: null,
        workstationCode: null,
        workorderId: null,
        workorderCode: null,
        taskId: null,
        taskCode: null,
        warehouseId: null,
        warehouseCode: null,
        warehouseName: null,
        locationId: null,
        locationCode: null,
        locationName: null,
        areaId: null,
        areaCode: null,
        areaName: null,
        issueDate: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        issueCode: [
          { required: true, message: "领料单编号不能为空", trigger: "blur" },
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        issueName: [
          { required: true, message: "领料单名称不能为空", trigger: "blur" },
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        requiredTime: [
          { required: true, message: "请指定需求日期时间", trigger: "blur" }
        ],
        workorderCode: [
        { required: true, message: "请指定生产工单", trigger: "blur" }
        ],
        remark: [
          { max: 250, message: '长度必须小于250个字符', trigger: 'blur' }
        ]
      }
    };
  },
  created() {
    this.getList();
    this.getWarehouseList();
  },
  methods: {
    /** 查询生产领料单头列表 */
    getList() {
      this.loading = true;
      listIssueheader(this.queryParams).then(response => {
        this.issueheaderList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    getWarehouseList(){
      getTreeList().then( response =>{
        if(response.data){
          this.warehouseOptions = response.data.filter((el) =>{
              return el.warehouseCode.indexOf('VIR') == -1;
          });;
        }
        this.warehouseOptions.map(w =>{
          w.children.map(l =>{
                  let lstr =JSON.stringify(l.children).replace(/locationId/g,'lId').replace(/areaId/g, 'pId').replace(/areaName/g,'pName');
                  l.children = JSON.parse(lstr);
          });

          let wstr = JSON.stringify(w.children).replace(/warehouseId/g,'wId').replace(/locationId/g, 'pId').replace(/locationName/g,'pName');
          w.children =  JSON.parse(wstr);

        });
        let ostr=JSON.stringify(this.warehouseOptions).replace(/warehouseId/g,'pId').replace(/warehouseName/g, 'pName');
        this.warehouseOptions = JSON.parse(ostr);
      });
    },
    // 关闭按钮
    close() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.pickError = '';
      this.defaultRequiredTime = this.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss");
      this.form = {
        issueId: null,
        issueCode: null,
        issueName: null,
        workstationId: null,
        workstationCode: null,
        workstationName: null,
        workorderId: null,
        workorderCode: null,
        workorderName: null,
        clientId:null,
        clientCode:null,
        clientName:null,
        taskId: null,
        taskCode: null,
        warehouseId: null,
        warehouseCode: null,
        warehouseName: null,
        locationId: null,
        locationCode: null,
        locationName: null,
        areaId: null,
        areaCode: null,
        areaName: null,
        requiredTime: this.defaultRequiredTime,
        issueDate: this.formatDate(new Date(),"yyyy-MM-dd HH:mm:ss"),
        status: 'PREPARE',
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
      this.ids = selection.map(item => item.issueId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    async handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加生产领料单";
      this.optType = "add";
      this.autoGenFlag = true;
      await this.handleAutoGenChange(true);
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const issueId = row.issueId || this.ids
      getIssueheader(issueId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改生产领料单";
        this.optType = "edit";
      });
    },

    /**
     * 拣货按钮操作
     */
     handleStocking(row){
      this.reset();
      const issueId = row.issueId || this.ids
      getIssueheader(issueId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "领料单拣货";
        this.optType = "edit";
      });
    },

    async handleQuickPick(row) {
      if (this.picking) return;
      this.picking = true;
      try {
        const response = await getIssueheader(row.issueId);
        this.form = response.data;
        this.pickError = '';
        this.optType = 'edit';
        this.title = '生产领料 · 一键拣货';
        this.open = true;
        await this.$nextTick();
      } finally {
        this.picking = false;
      }
      await this.handleAutoPick();
    },

    async handleAutoPick() {
      if (this.picking || !this.form.issueId) return;
      this.picking = true;
      this.pickError = '';
      try {
        if (this.form.status === 'PREPARE') {
          const valid = await new Promise(resolve => this.$refs.form.validate(resolve));
          if (!valid) return;
          await updateIssueheader(this.form);
        }
        const response = await autoPickIssue(this.form.issueId);
        this.form.status = response.data.status;
        this.$modal.msgSuccess(`已完成 ${response.data.lineCount} 项物料拣货，可直接执行领出`);
        this.$nextTick(() => { if (this.$refs.detail) this.$refs.detail.getListWithDetail(); });
        this.getList();
      } catch (error) {
        this.pickError = error && error.message || '拣货未完成，请刷新核对库存后重试';
      } finally {
        this.picking = false;
      }
    },

    async handleAdjustPick() {
      if (this.picking) return;
      this.picking = true;
      this.pickError = '';
      try {
        await updateIssueheader({ issueId: this.form.issueId, status: 'APPROVING' });
        this.form.status = 'APPROVING';
        this.getList();
      } catch (error) {
        this.pickError = error && error.message || '暂时无法调整分配，请刷新后重试';
      } finally {
        this.picking = false;
      }
    },

     // 查询明细按钮操作
    handleView(row){
      this.reset();
      const issueIds = row.issueId
      getIssueheader(issueIds).then(response => {
        this.form = response.data;
        this.warehouseInfo[0] = response.data.warehouseId;
        this.warehouseInfo[1] = response.data.locationId;
        this.warehouseInfo[2] = response.data.areaId;
        this.open = true;
        this.title = "查看领料单信息";
        this.optType = "view";
      });
    },
    /** 保存按钮 */
    saveForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.issueId != null) {
            updateIssueheader(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addIssueheader(this.form).then(response => {
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
        if (valid) {
          this.form.status = 'APPROVING';
          updateIssueheader(this.form).then(response => {
            this.$modal.msgSuccess("提交成功");
            this.open = false;
            this.getList();
          } 
         ).catch(() => {
            this.form.status = 'PREPARE';
         });;          
        }
      });

    },

    submitToExecute(){
      let that = this;
      checkQuantity(this.form.issueId).then( response =>{
        if(response.data){
            that.form.status = 'APPROVED';
            updateIssueheader(that.form).then(response => {
              that.$modal.msgSuccess("提交成功");
              that.open = false;
              that.getList();
            } 
            ).catch(() => {
              that.form.status = 'APPROVING';
            });    
          }else{
            this.$modal.confirm('领料数量与拣货数量不一致，确认提交?').then(function() {
              that.form.status = 'APPROVED';
              updateIssueheader(that.form).then(response => {
                that.$modal.msgSuccess("提交成功");
                that.open = false;
                that.getList();
              } 
              ).catch(() => {
                that.form.status = 'APPROVING';
              });    
            }).catch(() => {

            });
          }
        }
      );   
    },

    //取消
    cancel(){
      let that = this;
      this.$modal.confirm('确认撤销领料单？').then(function() {        
        const oldStatus = that.form.status;
        that.form.status = 'CANCELED';
          updateIssueheader(that.form).then(response => {
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

    //执行出库
    handleExecute(row){
      const issueIds = row.issueId || this.ids;
      this.$modal.confirm('确认执行出库？').then(function() {
        return execute(issueIds)//执行入库
      }).then(() => {
        this.getList();
        if (this.open && this.form.issueId === issueIds) this.form.status = 'FINISHED';
        this.$modal.msgSuccess("出库成功");
      }).catch(() => {});
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const issueIds = row.issueId || this.ids;
      this.$modal.confirm('是否确认删除生产领料单头编号为"' + issueIds + '"的数据项？').then(function() {
        return delIssueheader(issueIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('wm/issueheader/export', {
        ...this.queryParams
      }, `issueheader_${new Date().getTime()}.xlsx`)
    },
    //选择默认的仓库、库区、库位
    handleWarehouseChanged(obj){
      if(obj !=null){
        this.form.warehouseId = obj[0];
        this.form.locationId = obj[1];
        this.form.areaId = obj[2];
      }
    },
    //选择工作站
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
    //选择生产工单
    handleWorkorderSelect(){
      this.$refs.woSelect.handleOpen(this.form.workorderId)
    },
    onWorkorderSelected(row){
      if(row != undefined && row != null){
        const previousName = ((this.form.workorderName || this.form.workorderCode || '') + '领料').slice(0, 100);
        if (!this.form.issueName || this.form.issueName === previousName) {
          this.form.issueName = ((row.workorderName || row.workorderCode || '') + '领料').slice(0, 100);
        }
        if (!this.form.requiredTime || this.form.requiredTime === this.defaultRequiredTime) {
          this.form.requiredTime = row.requestDate ? this.parseTime(row.requestDate, '{y}-{m}-{d} {h}:{i}:{s}') : this.defaultRequiredTime;
          this.defaultRequiredTime = this.form.requiredTime;
        }
        this.form.workorderId = row.workorderId;
        this.form.workorderCode = row.workorderCode;
        this.form.workorderName = row.workorderName;
        this.form.clientId = row.clientId;
        this.form.clientCode = row.clientCode;
        this.form.clientName = row.clientName;
      }
    },
    //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        const target = this.form;
        return genCode('ISSUE_CODE').then(response =>{
          if (target === this.form && this.autoGenFlag) this.form.issueCode = response;
        });
      }else{
        this.form.issueCode = null;
      }
    }
  }
};
</script>

<style scoped>
.issue-context { padding: 0 4px 22px; }
.issue-context__title { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.issue-eyebrow { color: #8492a6; font-size: 12px; }
.issue-context h3 { margin: 8px 0 20px; color: #25344a; font-size: 20px; }
.issue-context__fields { display: flex; flex-wrap: wrap; gap: 20px 36px; }
.issue-context__fields span { display: block; color: #8492a6; font-size: 12px; margin-bottom: 8px; }
.issue-context__fields strong { color: #4a586c; font-size: 13px; font-weight: 500; }
.issue-pick-bar { display: flex; align-items: center; gap: 18px; padding: 22px 24px; background: #f2f7ff; border-radius: 10px; margin: 8px 0 20px; }
.issue-pick-bar__icon { color: #2276d2; font-size: 30px; }
.issue-pick-bar__content { flex: 1; }
.issue-pick-bar h3 { margin: 0 0 8px; color: #25344a; font-size: 17px; }
.issue-pick-bar p { margin: 0; color: #738297; font-size: 13px; line-height: 1.7; }
.issue-pick-error { margin-bottom: 18px; }
.issue-dialog ::v-deep .el-dialog { max-width: calc(100vw - 40px); }
.issue-dialog ::v-deep .el-dialog__body { padding-top: 16px; }
.issue-dialog ::v-deep .el-card__body { padding: 8px; }
@media (max-width: 760px) {
  .issue-pick-bar { flex-wrap: wrap; padding: 18px; }
  .issue-pick-bar__content { min-width: 180px; }
  .issue-pick-bar > .el-button { width: 100%; margin: 0; }
}
</style>

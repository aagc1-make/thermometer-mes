<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="出库单编号" prop="salesCode">
        <el-input
          v-model="queryParams.salesCode"
          placeholder="请输入出库单编号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="销售订单编号" prop="soCode">
        <el-input
          v-model="queryParams.soCode"
          placeholder="请输入销售订单编号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="客户名称" prop="clientName">
        <el-input
          v-model="queryParams.clientName"
          placeholder="请输入客户名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="出库日期" prop="salesDate">
        <el-date-picker clearable
          v-model="queryParams.salesDate"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择出库日期">
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
          v-hasPermi="['mes:wm:productsales:add']"
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
          v-hasPermi="['mes:wm:productsales:edit']"
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
          v-hasPermi="['mes:wm:productsales:remove']"
        >删除</el-button>
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
        <el-button v-if="pipelineBanner.row.status == 'PREPARE'" type="text" size="mini" @click="handleUpdate(pipelineBanner.row)">打开草稿</el-button>
        <el-button v-if="pipelineBanner.row.status == 'UNSTOCK'" type="text" size="mini" @click="handleStocking(pipelineBanner.row)">去拣货</el-button>
        <el-button v-if="pipelineBanner.row.status == 'UNSHIPPING'" type="text" size="mini" @click="handleShipping(pipelineBanner.row)">填写运单</el-button>
        <el-button v-if="pipelineBanner.row.status == 'UNEXECUTE'" type="text" size="mini" @click="handleExecute(pipelineBanner.row)">执行出库</el-button>
        <el-button v-if="pipelineBanner.row.status == 'FINISHED'" type="text" size="mini" @click="handleView(pipelineBanner.row)">查看单据</el-button>
        <el-button v-if="pipelineBanner.showNotice" type="text" size="mini" @click="goSalesNotice">去发货通知</el-button>
      </template>
    </el-alert>

    <el-table
      v-loading="loading"
      :data="productsalesList"
      :row-class-name="rowClassName"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="出库单编号" width="150px" align="center" prop="salesCode" >
        <template slot-scope="scope">
          <el-button
            type="text"
            @click="handleView(scope.row)"
            v-hasPermi="['mes:wm:productsales:query']"
          >{{scope.row.salesCode}}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="出库单名称" width="150px" align="center" prop="salesName" />
      <el-table-column label="发货通知单号" width="150px" align="center" prop="noticeCode" />
      <el-table-column label="销售订单编号" width="150px" align="center" prop="soCode" />
      <el-table-column label="客户编码" align="center" prop="clientCode" />
      <el-table-column label="客户名称" align="center" prop="clientName" />
      <el-table-column label="收货人" align="center" prop="recipient" />
      <el-table-column label="联系方式" align="center" prop="tel" />
      <el-table-column label="收货地址" align="center" prop="address" :show-overflow-tooltip="true"/>
      <el-table-column label="承运商" align="center" prop="carrier" />
      <el-table-column label="运输单号" align="center" prop="shippingNumber" />
      <el-table-column label="出库日期" align="center" prop="salesDate" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.salesDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="单据状态" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.mes_product_sales_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120px" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-if="scope.row.status == 'PREPARE'"
            v-hasPermi="['mes:wm:productsales:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-if="scope.row.status == 'PREPARE'"
            v-hasPermi="['mes:wm:productsales:remove']"
          >删除</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-shopping-cart-full"
            v-if="scope.row.status =='UNSTOCK'"
            @click="handleStocking(scope.row)"
            v-hasPermi="['mes:wm:productsales:edit']"
          >执行拣货</el-button>   
          <el-button
            size="mini"
            type="text"
            icon="el-icon-truck"
            v-if="scope.row.status =='UNSHIPPING'"
            @click="handleShipping(scope.row)"
            v-hasPermi="['mes:wm:productsales:edit']"
          >填写运单</el-button>             
          <el-button
            size="mini"
            type="text"
            icon="el-icon-video-play"
            @click="handleExecute(scope.row)"
            v-if="scope.row.status == 'UNEXECUTE'"
            v-hasPermi="['mes:wm:productsales:edit']"
          >执行出库</el-button>

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

    <!-- 添加或修改销售出库单对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="960px" append-to-body>
      <el-alert
        v-if="form.salesId != null"
        class="mb8"
        :type="salesAlertType"
        :closable="false"
        show-icon
        :title="'出库单 ' + (form.salesCode || '-') + ' · 发货通知 ' + (form.noticeCode || '-') + ' · 订单 ' + (form.soCode || '-') + ' · ' + salesStatusLabel"
      >
        <span>{{ salesFlowTip }}</span>
      </el-alert>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <!--草稿状态下可修改的部分-->
        <div v-if="form.status =='PREPARE'">
        <el-row>
          <el-col :span="8">
            <el-form-item label="出库单编号" prop="salesCode">
              <el-input v-model="form.salesCode" disabled placeholder="保存后由系统自动生成" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出库单名称" prop="salesName">
              <el-input v-model="form.salesName" placeholder="请输入出库单名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="发货通知单" prop="noticeCode">
              <el-input v-model="form.noticeCode" readonly="readonly" placeholder="请选择发货通知单" >
                <el-button slot="append" @click="handleSelectNotice" icon="el-icon-search"></el-button>
              </el-input>
              <NoticeSelect ref="noticeSelect" @onSelected="onNoticeSelected"></NoticeSelect>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="销售订单编号" prop="soCode">
              <el-input v-model="form.soCode" placeholder="请输入销售订单编号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="出库日期" prop="salesDate">
              <el-date-picker clearable
                v-model="form.salesDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择出库日期">
              </el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="客户编码" prop="clientCode">
              <el-input v-model="form.clientCode" placeholder="请输入客户编码" >
                <el-button slot="append" @click="handleSelectClient" icon="el-icon-search"></el-button>
              </el-input>
              <ClientSelect ref="clientSelect" @onSelected="onClientSelected" > </ClientSelect>
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="收货地址" prop="address">
              <el-input v-model="form.address" placeholder="请输入收货地址" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="客户名称" prop="clientName">
              <el-input v-model="form.clientName" readonly="readonly"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="收货人" prop="recipient">
              <el-input v-model="form.recipient" placeholder="请输入收货人" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系方式" prop="tel">
              <el-input v-model="form.tel" placeholder="请输入联系方式" />
            </el-form-item>
          </el-col>
        </el-row>
        </div>
        <div v-else>
          <el-row>
          <el-col :span="8">
            <el-form-item label="出库单编号" prop="salesCode">
              <el-input v-model="form.salesCode" disabled placeholder="保存后由系统自动生成" />
            </el-form-item>
          </el-col>
          <el-col :span="4">

          </el-col>
          <el-col :span="12">
            <el-form-item label="出库单名称" prop="salesName">
              <el-input v-model="form.salesName" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="发货通知单" prop="noticeCode">
              <el-input v-model="form.noticeCode" readonly="readonly" >
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="销售订单编号" prop="soCode">
              <el-input v-model="form.soCode" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="出库日期" disabled prop="salesDate">
              <el-date-picker clearable
                v-model="form.salesDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择出库日期">
              </el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="客户编码" prop="clientCode">
              <el-input v-model="form.clientCode" readonly="readonly" >
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="收货地址" prop="address">
              <el-input v-model="form.address" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="客户名称" prop="clientName">
              <el-input v-model="form.clientName" readonly="readonly"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="收货人" prop="recipient">
              <el-input v-model="form.recipient" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系方式" prop="tel">
              <el-input v-model="form.tel" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        </div>
        <!--待填写运单信息状态下可修改的部分-->

        <el-row v-if="form.status =='UNSHIPPING'||form.status =='UNEXECUTE'||form.status =='FINISHED'">
          <el-col :span="8">
            <el-form-item label="承运商" prop="carrier">
              <el-input v-model="form.carrier" placeholder="请输入承运商" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="运输单号" prop="shippingNumber">
              <el-input v-model="form.shippingNumber" placeholder="请输入运输单号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">

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
      <el-divider v-if="form.salesId !=null" content-position="center">物料信息</el-divider>
        <el-card shadow="always" v-if="form.salesId !=null && form.status =='PREPARE' " class="box-card">
          <Productsalesline ref="line" :salesId="form.salesId" :noticeId="form.noticeId" :optType="optType"></Productsalesline>
        </el-card>
        <el-card shadow="always" v-if="form.salesId !=null && form.status !='PREPARE' " class="box-card">
          <ProductsalesDetail ref="line" :salesId="form.salesId" :optType="optType"></ProductsalesDetail>
        </el-card>
        <div slot="footer" class="dialog-footer">
          <el-button type="primary" @click="saveForm" v-if="(form.status =='PREPARE' || form.status =='UNSHIPPING')&& optType !='view' ">保 存</el-button>
          <el-button type="warning" @click="submitToStock" v-if="form.status =='PREPARE' && form.salesId !=null && optType !='view' ">提 交</el-button>
          <el-button type="warning" @click="submitToShipping" v-if="form.status =='UNSTOCK' && form.salesId !=null && optType !='view' ">提 交</el-button>
          <el-button type="warning" @click="submitToExecute" v-if="form.status =='UNSHIPPING' && form.salesId !=null && optType !='view' ">提 交</el-button>
          <el-button type="danger" @click="cancel"  v-if="form.status !='PREPARE' && form.status !='FINISHED' && optType !='view' " >取 消</el-button>
          <el-button @click="close">关 闭</el-button>
        </div>
    </el-dialog>
  </div>
</template>

<script>
import { listProductsales, getProductsales, delProductsales, addProductsales, updateProductsales, checkQuantity, execute } from "@/api/mes/wm/productsales";
import { nextBizCode } from "@/api/mes/code";
import Productsalesline from "./line.vue"
import ProductsalesDetail from "./detail.vue"
import NoticeSelect from "@/components/noticeSelect/salesNoticeSingle.vue"
import ClientSelect from "@/components/clientSelect/single.vue";
export default {
  name: "Productsales",
  dicts: ['mes_product_sales_status'],
  components: {NoticeSelect,Productsalesline,ProductsalesDetail,ClientSelect},
  data() {
    return {
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
      // 销售出库单表格数据
      productsalesList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        salesCode: null,
        salesName: null,
        oqcId: null,
        oqcCode: null,
        soCode: null,
        clientId: null,
        clientCode: null,
        clientName: null,
        clientNick: null,
        salesDate: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        salesCode: [
          { required: true, message: "出库单编号不能为空", trigger: "blur" },
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        salesName: [
          { required: true, message: "出库单名称不能为空", trigger: "blur" },
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        clientCode: [
          { required: true, message: "请指定客户", trigger: "blur" }
        ],
        salesDate: [
          { required: true, message: "请选择出库日期", trigger: "blur" }
        ],
        remark: [
          { max: 250, message: '长度必须小于250个字符', trigger: 'blur' }
        ]
      }
    };
  },
  computed: {
    /** 当前订单下优先需要处理的出库单 */
    activeSale() {
      const rows = this.productsalesList || [];
      return rows.find(row => row.status === 'UNEXECUTE')
        || rows.find(row => row.status === 'UNSHIPPING')
        || rows.find(row => row.status === 'UNCHECK')
        || rows.find(row => row.status === 'UNSTOCK')
        || rows.find(row => row.status === 'PREPARE')
        || rows.find(row => row.status === 'FINISHED')
        || null;
    },
    /** 按订单过滤时，提示出库单与发货通知的绑定关系及下一步 */
    pipelineBanner() {
      if (this.loading || !this.queryParams.soCode) return null;
      const soCode = this.queryParams.soCode;
      const row = this.activeSale;
      if (!row) {
        return {
          type: 'info',
          title: '订单 ' + soCode + ' 暂无销售出库单',
          text: '发货通知确认后会自动生成销售出库草稿，单号与订单同源（xc + 日期 + 流水），批次同步带出。',
          row: null,
          showNotice: true
        };
      }
      const labels = { PREPARE: '草稿', UNSTOCK: '待拣货', UNCHECK: '待检验', UNSHIPPING: '待填运单', UNEXECUTE: '待执行出库', FINISHED: '已出库', CANCELED: '已取消' };
      const next = {
        PREPARE: '草稿已由发货通知自动生成并带出批次，核对后点击「提交」进入待拣货。',
        UNSTOCK: '待拣货：按库位完成拣货，数量核对一致后提交。',
        UNCHECK: '待检验：需完成出货检验并放行后才能继续出库。',
        UNSHIPPING: '待填写运单：录入承运商与运输单号后提交。',
        UNEXECUTE: '待执行出库：点击「执行出库」完成发货，工单销售环节闭环。',
        FINISHED: '已出库：发货完成，库存与批次流水已同步。'
      }[row.status] || '';
      return {
        type: row.status === 'FINISHED' ? 'success' : (row.status === 'PREPARE' ? 'info' : 'warning'),
        title: '订单 ' + soCode + ' · 出库单 ' + row.salesCode + '（' + (labels[row.status] || row.status) + '）'
          + (row.noticeCode ? ' · 发货通知 ' + row.noticeCode : ''),
        text: next + ' 单号由系统按订单链自动绑定，全局唯一。',
        row,
        showNotice: false
      };
    },
    salesStatusLabel() {
      return { PREPARE: '草稿', UNSTOCK: '待拣货', UNCHECK: '待检验', UNSHIPPING: '待填运单', UNEXECUTE: '待执行出库', FINISHED: '已出库', CANCELED: '已取消' }[this.form.status] || (this.form.status || '');
    },
    salesAlertType() {
      if (this.form.status === 'FINISHED') return 'success';
      return this.form.status === 'PREPARE' ? 'info' : 'warning';
    },
    salesFlowTip() {
      return {
        PREPARE: '草稿已自动绑定发货通知、客户与批次；核对出库行后点击「提交」进入待拣货。',
        UNSTOCK: '待拣货：按库位拣货，数量核对一致后提交。',
        UNCHECK: '待检验：完成出货检验并放行后才能继续出库。',
        UNSHIPPING: '待填写运单：录入承运商与运输单号后提交，进入待执行出库。',
        UNEXECUTE: '待执行出库：点击「执行出库」完成发货。',
        FINISHED: '已出库：发货完成，销售出库单不可再修改。',
        CANCELED: '已取消：如需重新发货，请重新确认发货通知。'
      }[this.form.status] || '';
    }
  },
  watch: {
    // 页面被 keep-alive 复用时，路由上的订单号变化也要重新过滤
    '$route.query.pipelineSoCode'(value) {
      this.queryParams.soCode = value || null;
      this.queryParams.pageNum = 1;
      this._autoOpenedSoCode = null;
      this.getList();
    }
  },
  created() {
    this._autoOpenedSoCode = null;
    // 销售出库按订单号绑定，流水线跳转时用来源订单过滤
    const pipelineSoCode = this.$route && this.$route.query && this.$route.query.pipelineSoCode;
    if (pipelineSoCode) this.queryParams.soCode = pipelineSoCode;
    this.getList();
  },
  methods: {
    goSalesNotice() {
      this.$router.push('/mes/wm/salesnotice').catch(() => {});
    },
    /** 查询销售出库单列表 */
    getList() {
      this.loading = true;
      listProductsales(this.queryParams).then(response => {
        this.productsalesList = response.rows;
        this.total = response.total;
        this.loading = false;
        this.autoOpenPipelineDraft();
      });
    },
    /**
     * 从流水线按订单进入且只有一张可办理出库单时，自动打开对应单据，
     * 避免用户到页面后还要自己找单。同一订单只自动打开一次。
     */
    autoOpenPipelineDraft() {
      const soCode = this.queryParams.soCode;
      if (!soCode || this.open) return;
      if (String(this._autoOpenedSoCode || '') === String(soCode)) return;
      const actionable = (this.productsalesList || []).filter(row =>
        ['UNEXECUTE', 'UNSHIPPING', 'UNSTOCK', 'PREPARE'].includes(row.status));
      if (actionable.length !== 1) return;
      this._autoOpenedSoCode = String(soCode);
      const row = actionable[0];
      if (row.status === 'UNEXECUTE') this.handleView(row);
      else if (row.status === 'UNSHIPPING') this.handleShipping(row);
      else if (row.status === 'UNSTOCK') this.handleStocking(row);
      else this.handleUpdate(row);
    },
    // 关闭按钮
    close() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        salesId: null,
        salesCode: null,
        salesName: null,
        noticeId: null,
        noticeCode: null,
        soCode: null,
        clientId: null,
        clientCode: null,
        clientName: null,
        clientNick: null,
        recipient: null,
        tel: null,
        address: null,
        carrier: null,
        shippingNumber: null,
        salesDate: new Date(),
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
      this._autoOpenedSoCode = null;
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.salesId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    // 查询明细按钮操作
    handleView(row){
      this.reset();
      const salesId = row.salesId
      getProductsales(salesId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "查看出库单信息";
        this.optType = "view";
      });
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      // 从流水线按订单进入时，手工新增也绑定该订单，单号沿订单链的日期与流水
      const soCode = this.queryParams.soCode || (this.$route && this.$route.query.pipelineSoCode) || null;
      // 手工新增后不再自动弹出刚保存的草稿
      if (soCode) this._autoOpenedSoCode = String(soCode);
      if (soCode) this.form.soCode = soCode;
      nextBizCode('XSCK', soCode || undefined).then(res => {
        this.form.salesCode = (res && res.data) || '';
      });
      this.open = true;
      this.title = soCode ? '添加订单 ' + soCode + ' 的销售出库单' : '添加销售出库单';
      this.optType = "add";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const salesId = row.salesId || this.ids
      getProductsales(salesId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改销售出库单";
        this.optType = "edit";
      });
    },

    /**
     * 拣货按钮操作
     */
     handleStocking(row){
      this.reset();
      const salesId = row.salesId || this.ids
      getProductsales(salesId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "出库单拣货";
        this.optType = "edit";
      });
    },

    /**
     * 填写运单信息按钮操作
     */
     handleShipping(row){
      this.reset();
      const salesId = row.salesId || this.ids
      getProductsales(salesId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "出库单填写运单信息";
        this.optType = "shipping";
      });
    },

    /** 保存按钮 */
    saveForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.salesId != null) {
            updateProductsales(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addProductsales(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },

    //提交按钮(提交到待拣货状态)
    submitToStock(){
      this.$refs["form"].validate(valid => {
        if (valid) {
          const code = this.form.salesCode;
          this.form.status = 'UNSTOCK';
          updateProductsales(this.form).then(response => {
            this.$modal.msgSuccess("出库单 " + code + " 已提交，下一步：按库位拣货");
            this.open = false;
            this.getList();
          } 
         ).catch(() => {
            this.form.status = 'PREPARE';
         });;          
        }
      });
    },

    submitToShipping(){
      let that = this;
      checkQuantity(this.form.salesId).then( response =>{
        if(response.data){
            const code = that.form.salesCode;
            that.form.status = 'UNSHIPPING';
            updateProductsales(that.form).then(response => {
              that.$modal.msgSuccess("出库单 " + code + " 拣货数量核对通过，下一步：填写运单信息");
              that.open = false;
              that.getList();
            } 
            ).catch(() => {
              that.form.status = 'UNSTOCK';
            });    
          }else{
            this.$modal.msgError("拣货数量与出库数量不一致!");
          }
        }
      );   
    },

    //提交按钮(提交到待执行出库状态)
    submitToExecute(){
      this.$refs["form"].validate(valid => {
        if (valid) {
          const code = this.form.salesCode;
          this.form.status = 'UNEXECUTE';
          updateProductsales(this.form).then(response => {
            this.$modal.msgSuccess("出库单 " + code + " 已提交，下一步：执行出库");
            this.open = false;
            this.getList();
          } 
         ).catch(() => {
            this.form.status = 'UNSHIPPING';
         });;          
        }
      });
    },

    //取消
    cancel(){
      let that = this;
      this.$modal.confirm('确认撤销出库单？').then(function() {        
        const oldStatus = that.form.status;
        that.form.status = 'CANCELED';
        updateProductsales(that.form).then(response => {
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
      const salesIds = row.salesId || this.ids;
      this.$modal.confirm('是否确认删除销售出库单编号为"' + salesIds + '"的数据项？').then(function() {
        return delProductsales(salesIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('wm/productsales/export', {
        ...this.queryParams
      }, `productsales_${new Date().getTime()}.xlsx`)
    },
    //执行出库
    handleExecute(row){
      const salesIds = row.salesId || this.ids;
      const code = row.salesCode || this.form.salesCode || '';
      this.$modal.confirm('销售出库单 ' + code + ' 确认执行出库？出库后库存与批次流水将同步扣减。')
        .then(() => execute(salesIds))
        .then(() => {
          if (this.open) this.open = false;
          this.getList();
          this.$modal.msgSuccess("出库单 " + code + " 出库成功");
        }).catch(() => {});
    },
    //到货通知单选择
    handleSelectNotice(){
      this.$refs.noticeSelect.handleOpen(this.form.noticeId)
    },
    //到货通知单选择弹出框
    onNoticeSelected(obj){      
      if(obj != undefined && obj != null){
          this.form.noticeId = obj.noticeId;
          this.form.noticeCode = obj.noticeCode;
          // 出库单名称自动带出，避免手工新增时因名称为空保存失败
          if (!this.form.salesName) this.form.salesName = '发货通知' + (obj.noticeCode || '') + '销售出库';
          this.form.soCode = obj.soCode;
          this.form.clientId = obj.clientId;
          this.form.clientCode = obj.clientCode;
          this.form.clientName = obj.clientName;
          this.form.clientNick = obj.clientNick;
          // 新增单据在选定发货通知后重新取链号：前缀为 xc，日期与流水与订单号一致
          if (this.form.salesId == null && obj.soCode) {
            nextBizCode('XSCK', obj.soCode).then(res => {
              this.form.salesCode = (res && res.data) || this.form.salesCode;
            });
          }
        }
    },
    handleSelectClient(){
      this.$refs["form"].clearValidate()
      this.$refs.clientSelect.handleOpen(this.form.clientId)
    },
    //客户选择弹出框
    onClientSelected(obj){
        if(obj != undefined && obj != null){
          this.form.clientId = obj.clientId;
          this.form.clientCode = obj.clientCode;
          this.form.clientName = obj.clientName;
          this.form.clientNick = obj.clientNick;
        }
    },
    /**
     * 发货通知联动生成、尚未执行的销售出库草稿行：闪烁高亮。
     * linkage 建单时 attr1 = AUTO:PRODUCT_SALES:SALES_NOTICE:{noticeId}，状态 PREPARE。
     */
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

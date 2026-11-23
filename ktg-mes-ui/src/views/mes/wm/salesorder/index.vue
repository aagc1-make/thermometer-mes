<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="订单编号" prop="orderCode">
        <el-input v-model="queryParams.orderCode" placeholder="请输入订单编号" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="订单名称" prop="orderName">
        <el-input v-model="queryParams.orderName" placeholder="请输入订单名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="客户名称" prop="clientName">
        <el-input v-model="queryParams.clientName" placeholder="请输入客户名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="订单状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="dict in dict.type.mes_sales_order_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd"
                   v-hasPermi="['mes:wm:salesorder:add']">新增订单</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single"
                   @click="handleUpdate" v-hasPermi="['mes:wm:salesorder:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple"
                   @click="handleDelete" v-hasPermi="['mes:wm:salesorder:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport"
                   v-hasPermi="['mes:wm:salesorder:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="orderList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="订单编号" align="center" width="160" prop="orderCode">
        <template slot-scope="scope">
          <el-button type="text" @click="handleView(scope.row)">{{ scope.row.orderCode }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="订单名称" align="left" min-width="160" prop="orderName" :show-overflow-tooltip="true" />
      <el-table-column label="客户名称" align="left" min-width="140" prop="clientName" :show-overflow-tooltip="true" />
      <el-table-column label="下单日期" align="center" width="100">
        <template slot-scope="scope">{{ parseTime(scope.row.orderDate, '{y}-{m}-{d}') }}</template>
      </el-table-column>
      <el-table-column label="期望交期" align="center" width="100">
        <template slot-scope="scope">{{ parseTime(scope.row.deliveryDate, '{y}-{m}-{d}') }}</template>
      </el-table-column>
      <el-table-column label="数量" align="right" width="80" prop="totalQuantity" />
      <el-table-column label="金额" align="right" width="100">
        <template slot-scope="scope">{{ formatAmount(scope.row.totalAmount) }}</template>
      </el-table-column>
      <el-table-column label="工单号" align="left" min-width="160" prop="workorderCodes" :show-overflow-tooltip="true" />
      <el-table-column label="质检单号" align="left" min-width="150" prop="oqcCodes" :show-overflow-tooltip="true" />
      <el-table-column label="发货单号" align="left" min-width="150" prop="noticeCode" :show-overflow-tooltip="true" />
      <el-table-column label="状态" align="center" width="90">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.mes_sales_order_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="300" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleView(scope.row)">详情</el-button>
          <el-button v-if="scope.row.status === 'PREPARE'" size="mini" type="text" icon="el-icon-edit"
                     @click="handleUpdate(scope.row)" v-hasPermi="['mes:wm:salesorder:edit']">修改</el-button>
          <el-button v-if="scope.row.status === 'PREPARE'" size="mini" type="text" icon="el-icon-check"
                     @click="handleConfirm(scope.row)" v-hasPermi="['mes:wm:salesorder:confirm']">确认下单</el-button>
          <el-button v-if="canGenerate(scope.row)" size="mini" type="text" icon="el-icon-s-order"
                     @click="handleGenWorkorder(scope.row)" v-hasPermi="['mes:wm:salesorder:workorder']">生成工单</el-button>
          <el-button v-if="canGenerate(scope.row)" size="mini" type="text" icon="el-icon-finished"
                     @click="handleGenOqc(scope.row)" v-hasPermi="['mes:wm:salesorder:oqc']">生成质检单</el-button>
          <el-button v-if="canGenerate(scope.row)" size="mini" type="text" icon="el-icon-truck"
                     @click="handleGenNotice(scope.row)" v-hasPermi="['mes:wm:salesorder:notice']">生成发货单</el-button>
          <el-button v-if="scope.row.status === 'PREPARE'" size="mini" type="text" icon="el-icon-delete"
                     @click="handleDelete(scope.row)" v-hasPermi="['mes:wm:salesorder:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum"
                :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 新增 / 修改 / 详情 -->
    <el-dialog :title="title" :visible.sync="open" width="1180px" append-to-body :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="rules" label-width="90px" :disabled="optType === 'view'">
        <el-row>
          <el-col :span="8">
            <el-form-item label="订单编号" prop="orderCode">
              <el-input v-model="form.orderCode" disabled placeholder="保存后由系统自动生成" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="订单名称" prop="orderName">
              <el-input v-model="form.orderName" placeholder="请输入订单名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="客户编码" prop="clientCode">
              <el-input v-model="form.clientCode" readonly placeholder="请选择客户">
                <el-button slot="append" icon="el-icon-search" @click="handleSelectClient" v-if="optType !== 'view'"></el-button>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="客户名称" prop="clientName">
              <el-input v-model="form.clientName" readonly placeholder="请选择客户" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="下单日期" prop="orderDate">
              <el-date-picker v-model="form.orderDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择下单日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="期望交期" prop="deliveryDate">
              <el-date-picker v-model="form.deliveryDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择期望交期" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
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
          <el-col :span="8">
            <el-form-item label="状态" prop="status">
              <el-input :value="statusLabel(form.status)" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="收货地址" prop="address">
              <el-input v-model="form.address" placeholder="请输入收货地址" />
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

      <el-divider content-position="center">订单明细</el-divider>
      <el-row :gutter="10" class="mb8" v-if="optType !== 'view'">
        <el-col :span="1.5">
          <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAddLine">添加明细</el-button>
        </el-col>
        <el-col :span="12">
          <span class="order-tip">数量合计：{{ formTotalQuantity }} ｜ 金额合计：{{ formatAmount(formTotalAmount) }}</span>
        </el-col>
      </el-row>
      <el-table :data="form.lines" size="mini" border max-height="320">
        <el-table-column label="产品编码" width="180" align="center">
          <template slot-scope="scope">
            <el-input v-model="scope.row.itemCode" size="mini" readonly placeholder="选择产品">
              <el-button slot="append" icon="el-icon-search" @click="chooseItem(scope.row)" v-if="optType !== 'view'"></el-button>
            </el-input>
          </template>
        </el-table-column>
        <el-table-column label="产品名称" min-width="150" align="left" prop="itemName" :show-overflow-tooltip="true" />
        <el-table-column label="规格型号" min-width="150" align="left" prop="specification" :show-overflow-tooltip="true" />
        <el-table-column label="单位" width="70" align="center" prop="unitName" />
        <el-table-column label="数量" width="130" align="center">
          <template slot-scope="scope">
            <el-input-number v-model="scope.row.quantity" :min="0.01" :precision="2" :controls="false"
                             size="mini" style="width: 100%" v-if="optType !== 'view'" />
            <span v-else>{{ scope.row.quantity }}</span>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="130" align="center">
          <template slot-scope="scope">
            <el-input-number v-model="scope.row.price" :min="0" :precision="2" :controls="false"
                             size="mini" style="width: 100%" v-if="optType !== 'view'" />
            <span v-else>{{ formatAmount(scope.row.price) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="110" align="right">
          <template slot-scope="scope">{{ formatAmount(lineAmount(scope.row)) }}</template>
        </el-table-column>
        <el-table-column label="生产工单号" width="150" align="center" prop="workorderCode" :show-overflow-tooltip="true" />
        <el-table-column label="质检单号" width="150" align="center" prop="oqcCode" :show-overflow-tooltip="true" />
        <el-table-column label="操作" width="70" align="center" v-if="optType !== 'view'">
          <template slot-scope="scope">
            <el-button size="mini" type="text" icon="el-icon-delete" @click="handleRemoveLine(scope.$index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="order-related" v-if="form.noticeCode || form.workorderCodes || form.oqcCodes">
        <span>关联单据：</span>
        <span v-if="form.workorderCodes">工单 {{ form.workorderCodes }}</span>
        <span v-if="form.oqcCodes">质检 {{ form.oqcCodes }}</span>
        <span v-if="form.noticeCode">发货单 {{ form.noticeCode }}</span>
      </div>

      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm" v-if="optType !== 'view'">保 存</el-button>
        <el-button @click="cancel">关 闭</el-button>
      </div>
    </el-dialog>

    <ClientSelect ref="clientSelect" @onSelected="onClientSelected" />
    <ItemSelect ref="itemSelect" @onSelected="onItemSelected" />
  </div>
</template>

<script>
import {
  listSalesorder, getSalesorder, delSalesorder, addSalesorder, updateSalesorder,
  confirmSalesorder, generateWorkorder, generateOqc, generateNotice
} from '@/api/mes/wm/salesorder'
import { nextBizCode } from '@/api/mes/code'
import ClientSelect from '@/components/clientSelect/single.vue'
import ItemSelect from '@/components/itemSelect/single.vue'

export default {
  name: 'Salesorder',
  dicts: ['mes_sales_order_status'],
  components: { ClientSelect, ItemSelect },
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      orderList: [],
      title: '',
      open: false,
      optType: undefined,
      currentLine: null,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        orderCode: null,
        orderName: null,
        clientName: null,
        status: null
      },
      form: {},
      rules: {
        orderName: [{ required: true, message: '订单名称不能为空', trigger: 'blur' }],
        clientCode: [{ required: true, message: '请选择客户', trigger: 'blur' }],
        orderDate: [{ required: true, message: '请选择下单日期', trigger: 'change' }],
        deliveryDate: [{ required: true, message: '请选择期望交期', trigger: 'change' }]
      }
    }
  },
  computed: {
    formTotalQuantity() {
      return ((this.form && this.form.lines) || []).reduce((sum, line) => sum + Number(line.quantity || 0), 0)
    },
    formTotalAmount() {
      return ((this.form && this.form.lines) || []).reduce((sum, line) => sum + Number(line.quantity || 0) * Number(line.price || 0), 0)
    }
  },
  created() {
    // 流水线跳转过来时按订单号定位
    if (this.$route && this.$route.query && this.$route.query.orderCode) {
      this.queryParams.orderCode = this.$route.query.orderCode
    }
    this.getList()
    // 产品入库不合格提示跳转：直接打开新增订单，备注预填为原工单的二次订单
    if (this.$route && this.$route.query && this.$route.query.reorderRemark) {
      this.$nextTick(this.handleAdd)
    }
  },
  methods: {
    getList() {
      this.loading = true
      listSalesorder(this.queryParams).then(response => {
        this.orderList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    statusLabel(status) {
      const item = (this.dict.type.mes_sales_order_status || []).find(d => d.value === status)
      return item ? item.label : (status || '草稿')
    },
    formatAmount(value) {
      return Number(value || 0).toFixed(2)
    },
    lineAmount(row) {
      return Number(row.quantity || 0) * Number(row.price || 0)
    },
    canGenerate(row) {
      return row.status === 'CONFIRMED' || row.status === 'PRODUCING'
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.optType = undefined
      this.form = {
        orderId: null,
        orderCode: null,
        orderName: null,
        clientId: null,
        clientCode: null,
        clientName: null,
        clientNick: null,
        orderDate: null,
        deliveryDate: null,
        recipient: null,
        tel: null,
        address: null,
        totalQuantity: 0,
        totalAmount: 0,
        status: 'PREPARE',
        noticeId: null,
        noticeCode: null,
        remark: null,
        lines: []
      }
      this.resetForm('form')
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.orderId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleView(row) {
      this.reset()
      getSalesorder(row.orderId).then(response => {
        this.form = response.data || {}
        this.form.lines = this.form.lines || []
        this.optType = 'view'
        this.open = true
        this.title = '订单详情 - ' + (this.form.orderCode || '')
      })
    },
    handleAdd() {
      this.reset()
      nextBizCode('XSDD').then(res => {
        this.form.orderCode = (res && res.data) || ''
      })
      this.form.orderDate = this.parseTime(new Date(), '{y}-{m}-{d}')
      // 从“二次订单”入口进来时备注自动带上原工单号
      const reorderRemark = this.$route && this.$route.query && this.$route.query.reorderRemark
      if (reorderRemark) this.form.remark = reorderRemark
      this.optType = 'add'
      this.open = true
      this.title = '新增客户订单'
    },
    handleUpdate(row) {
      const orderId = row.orderId || this.ids[0]
      this.reset()
      getSalesorder(orderId).then(response => {
        this.form = response.data || {}
        this.form.lines = this.form.lines || []
        this.optType = 'edit'
        this.open = true
        this.title = '修改客户订单'
      })
    },
    handleAddLine() {
      this.form.lines.push({ itemId: null, itemCode: null, itemName: null, specification: null, unitOfMeasure: null, unitName: null, quantity: 1, price: 0 })
    },
    handleRemoveLine(index) {
      this.form.lines.splice(index, 1)
    },
    handleSelectClient() {
      this.$refs.clientSelect.showFlag = true
    },
    onClientSelected(obj) {
      if (!obj) return
      this.form.clientId = obj.clientId
      this.form.clientCode = obj.clientCode
      this.form.clientName = obj.clientName
      this.form.clientNick = obj.clientNick
    },
    chooseItem(row) {
      this.currentLine = row
      this.$refs.itemSelect.handleOpen(row.itemId)
    },
    onItemSelected(obj) {
      if (!obj || !this.currentLine) return
      this.currentLine.itemId = obj.itemId
      this.currentLine.itemCode = obj.itemCode
      this.currentLine.itemName = obj.itemName
      this.currentLine.specification = obj.specification
      this.currentLine.unitOfMeasure = obj.unitOfMeasure
      this.currentLine.unitName = obj.unitName
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) return
        const lines = (this.form.lines || []).filter(line => line.itemId)
        if (!lines.length) {
          this.$modal.msgError('请至少添加一条已选择产品的订单明细')
          return
        }
        const invalid = lines.find(line => !line.quantity || Number(line.quantity) <= 0)
        if (invalid) {
          this.$modal.msgError('订单明细数量必须大于 0')
          return
        }
        const payload = Object.assign({}, this.form, { lines })
        if (this.form.orderId != null) {
          updateSalesorder(payload).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.open = false
            this.getList()
          })
        } else {
          addSalesorder(payload).then(() => {
            this.$modal.msgSuccess('新增成功')
            this.open = false
            this.getList()
          })
        }
      })
    },
    handleDelete(row) {
      const orderIds = row.orderId || this.ids
      this.$modal.confirm('是否确认删除订单编号为"' + orderIds + '"的数据项？').then(() => {
        return delSalesorder(orderIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleConfirm(row) {
      this.$modal.confirm('确认对订单「' + row.orderCode + '」下单？确认后可生成工单、质检单和发货单。').then(() => {
        return confirmSalesorder(row.orderId)
      }).then(() => {
        this.$modal.msgSuccess('已确认下单')
        this.getList()
      }).catch(() => {})
    },
    handleGenWorkorder(row) {
      this.$modal.confirm('按订单明细生成生产工单？已生成过工单的行不会重复创建。').then(() => {
        return generateWorkorder(row.orderId)
      }).then(res => {
        const data = (res && res.data) || {}
        const codes = [].concat(data.createdCodes || [], data.existedCodes || [])
        this.$modal.msgSuccess(codes.length ? '关联工单：' + codes.join('、') : '订单明细已全部生成工单')
        this.getList()
      }).catch(() => {})
    },
    handleGenOqc(row) {
      this.$modal.confirm('按订单明细生成出货检验单？生成的检验单号会回写到订单明细。').then(() => {
        return generateOqc(row.orderId)
      }).then(res => {
        const data = (res && res.data) || {}
        const codes = [].concat(data.createdCodes || [], data.existedCodes || [])
        this.$modal.msgSuccess(codes.length ? '关联质检单：' + codes.join('、') : '订单明细已全部生成质检单')
        this.getList()
      }).catch(() => {})
    },
    handleGenNotice(row) {
      this.$modal.confirm('按订单生成发货通知单？通知单带订单号，确认发货后自动生成销售出库单。').then(() => {
        return generateNotice(row.orderId)
      }).then(res => {
        const data = (res && res.data) || {}
        this.$modal.msgSuccess('发货单：' + (data.noticeCode || '-') + (data.created === false ? '（已存在）' : ''))
        this.getList()
      }).catch(() => {})
    },
    handleExport() {
      this.download('wm/salesorder/export', {
        ...this.queryParams
      }, `salesorder_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>

<style lang="scss" scoped>
.order-tip {
  color: #606266;
  font-size: 12px;
  line-height: 28px;
}

.order-related {
  display: flex;
  gap: 16px;
  margin-top: 10px;
  font-size: 12px;
  color: #606266;
}
</style>

<template>
  <div class="app-container">
    <el-form :model="query" size="small" inline>
      <el-form-item label="订单编号">
        <el-input v-model="query.orderCode" clearable @keyup.enter.native="search" />
      </el-form-item>
      <el-form-item label="客户">
        <el-input v-model="query.clientName" clearable @keyup.enter.native="search" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="search">搜索</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row class="mb8">
      <el-button v-hasPermi="['mes:wm:salesorder:add']" type="primary" plain size="mini" icon="el-icon-plus" @click="add">新增订单</el-button>
    </el-row>
    <el-table v-loading="loading" :data="rows">
      <el-table-column label="订单编号" prop="orderCode" min-width="170" />
      <el-table-column label="订单名称" prop="orderName" min-width="155" show-overflow-tooltip />
      <el-table-column label="客户" prop="clientName" min-width="130" show-overflow-tooltip />
      <el-table-column label="交货日期" prop="deliveryDate" width="110" />
      <el-table-column label="状态" width="96">
        <template slot-scope="{ row }">{{ statuses[row.status] || row.status }}</template>
      </el-table-column>
      <el-table-column label="关联工单" prop="workorderCodes" min-width="180" show-overflow-tooltip />
      <el-table-column label="发货通知" prop="noticeCode" min-width="170" />
      <el-table-column label="操作" width="216" fixed="right" align="center">
        <template slot-scope="{ row }">
          <el-button v-hasPermi="['mes:wm:salesorder:query']" type="text" size="mini" @click="openOrder(row, true)">详情</el-button>
          <template v-if="row.status === 'PREPARE'">
            <el-button v-hasPermi="['mes:wm:salesorder:edit']" type="text" size="mini" @click="openOrder(row, false)">编辑</el-button>
            <el-button v-hasPermi="['mes:wm:salesorder:confirm']" type="text" size="mini" @click="confirm(row)">确认</el-button>
            <el-button v-hasPermi="['mes:wm:salesorder:remove']" type="text" size="mini" @click="remove(row)">删除</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="load" />

    <el-dialog :title="viewOnly ? '销售订单详情' : form.orderId ? '编辑销售订单' : '新增销售订单'"
      :visible.sync="open" width="1000px" append-to-body>
      <el-form ref="orderForm" :model="form" :rules="rules" label-width="100px" :disabled="viewOnly">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="订单编号">
              <el-input :value="form.orderCode" readonly placeholder="保存时由系统生成" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="订单名称" prop="orderName">
              <el-input v-model.trim="form.orderName" maxlength="250" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户" prop="clientId">
              <el-input :value="form.clientName" readonly>
                <el-button v-if="!viewOnly" slot="append" icon="el-icon-search" @click="$refs.client.showFlag = true">选择</el-button>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="交货日期">
              <el-date-picker v-model="form.deliveryDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择交货日期" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="收货人"><el-input v-model.trim="form.recipient" maxlength="100" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话"><el-input v-model.trim="form.tel" maxlength="64" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="收货地址"><el-input v-model.trim="form.address" maxlength="500" /></el-form-item>
          </el-col>
        </el-row>
        <el-button v-if="!viewOnly" class="mb8" icon="el-icon-plus" plain type="primary" size="mini" @click="$refs.item.showFlag = true">添加产品</el-button>
        <el-table :data="form.lines">
          <el-table-column label="产品编码" prop="itemCode" min-width="130" />
          <el-table-column label="产品名称" prop="itemName" min-width="135" show-overflow-tooltip />
          <el-table-column label="数量" min-width="140">
            <template slot-scope="{ row }">
              <span v-if="viewOnly">{{ row.quantity }}</span>
              <el-input-number v-else v-model="row.quantity" :min="0.0001" :precision="4" :controls="false" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="单价" min-width="140">
            <template slot-scope="{ row }">
              <span v-if="viewOnly">{{ row.price }}</span>
              <el-input-number v-else v-model="row.price" :min="0" :precision="4" :controls="false" size="small" />
            </template>
          </el-table-column>
          <el-table-column v-if="viewOnly" label="工单编号" prop="workorderCode" min-width="160" />
          <el-table-column v-if="viewOnly" label="已出库" prop="quantityShipped" width="95" />
          <el-table-column v-if="!viewOnly" label="操作" width="68">
            <template slot-scope="{ $index }">
              <el-button type="text" size="mini" @click="form.lines.splice($index, 1)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <div slot="footer">
        <template v-if="viewOnly && ['CONFIRMED', 'PRODUCING'].includes(form.status)">
          <el-button v-if="!form.workorderCodes" v-hasPermi="['mes:wm:salesorder:workorder']" :loading="busy" @click="generate('generateWorkorder')">生成工单</el-button>
          <el-button v-if="!form.noticeCode" v-hasPermi="['mes:wm:salesorder:notice']" :loading="busy" @click="generate('generateNotice')">生成发货通知</el-button>
        </template>
        <el-button v-if="!viewOnly" type="primary" :loading="busy" @click="save">保存</el-button>
        <el-button @click="open = false">关闭</el-button>
      </div>
    </el-dialog>
    <ClientSelect ref="client" @onSelected="selectClient" />
    <ItemSelect ref="item" @onSelected="selectItem" />
  </div>
</template>

<script>
import ClientSelect from '@/components/clientSelect/single.vue'
import ItemSelect from '@/components/itemSelect/single.vue'
import { previewMesCode } from '@/api/mes/code'
import { listSalesOrders, getSalesOrder, saveSalesOrder, deleteSalesOrder, confirmSalesOrder, generateSalesOrderDocuments } from '@/api/mes/wm/salesorder'

const defaults = () => ({ orderCode: '', orderName: '', clientId: null, clientName: '', deliveryDate: null, lines: [] })

export default {
  name: 'Salesorder',
  components: { ClientSelect, ItemSelect },
  data() {
    return {
      query: { pageNum: 1, pageSize: 10, orderCode: '', clientName: '' },
      rows: [], total: 0, loading: false, busy: false, open: false, viewOnly: false,
      form: defaults(),
      statuses: { PREPARE: '草稿', CONFIRMED: '已确认', PRODUCING: '生产中', FINISHED: '已完成', CANCELED: '已取消' },
      rules: {
        orderName: [{ required: true, message: '请输入订单名称', trigger: 'blur' }],
        clientId: [{ required: true, message: '请选择客户', trigger: 'change' }]
      }
    }
  },
  created() {
    this.load()
  },
  methods: {
    async load() {
      this.loading = true
      try {
        const result = await listSalesOrders(this.query)
        this.rows = result.rows
        this.total = result.total
      } finally {
        this.loading = false
      }
    },
    search() {
      this.query.pageNum = 1
      this.load()
    },
    resetQuery() {
      this.query.orderCode = ''
      this.query.clientName = ''
      this.search()
    },
    add() {
      const draft = defaults()
      this.form = draft
      this.viewOnly = false
      this.open = true
      previewMesCode('XSDD').then(result => {
        if (this.form === draft) draft.orderCode = result.data
      }).catch(() => {})
    },
    async openOrder(row, viewOnly) {
      const result = await getSalesOrder(row.orderId)
      this.form = { ...result.data, lines: result.data.lines || [] }
      this.viewOnly = viewOnly
      this.open = true
    },
    selectClient(client) {
      if (!client) return
      const { clientId, clientCode, clientName, clientNick } = client
      this.form = { ...this.form, clientId, clientCode, clientName, clientNick }
    },
    selectItem(item) {
      const row = Array.isArray(item) ? item[0] : item
      if (!row) return
      this.form.lines.push({
        itemId: row.itemId, itemCode: row.itemCode, itemName: row.itemName,
        specification: row.specification || row.itemSpc, unitOfMeasure: row.unitOfMeasure, unitName: row.unitName,
        quantity: 1, price: 0
      })
    },
    save() {
      this.$refs.orderForm.validate(async valid => {
        if (!valid || this.busy) return
        if (!this.form.lines.length || this.form.lines.some(line => !(line.quantity > 0) || !(line.price >= 0))) {
          this.$modal.msgError('请添加产品，并填写有效的数量和单价')
          return
        }
        this.busy = true
        try {
          await saveSalesOrder(this.form)
          this.$modal.msgSuccess('保存成功')
          this.open = false
          this.load()
        } finally {
          this.busy = false
        }
      })
    },
    confirm(row) {
      this.$modal.confirm('确认订单 ' + row.orderCode + ' 并生成生产工单和发货草稿？')
        .then(() => confirmSalesOrder(row.orderId)).then(() => {
          this.$modal.msgSuccess('订单已确认')
          this.load()
        }).catch(() => {})
    },
    remove(row) {
      this.$modal.confirm('删除草稿订单 ' + row.orderCode + '？')
        .then(() => deleteSalesOrder(row.orderId)).then(() => {
          this.$modal.msgSuccess('删除成功')
          this.load()
        }).catch(() => {})
    },
    async generate(action) {
      this.busy = true
      try {
        await generateSalesOrderDocuments(this.form.orderId, action)
        await this.openOrder(this.form, true)
        this.$modal.msgSuccess('操作成功')
        this.load()
      } finally {
        this.busy = false
      }
    }
  }
}
</script>

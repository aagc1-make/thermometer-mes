import { submitInquiry, getInquiryList } from '../../utils/api.js'
const app = getApp()

Page({
  data: {
    statusTabs: [
      { key: 'PENDING', text: '待提交' },
      { key: 'SUBMITTED', text: '已提交' },
      { key: 'QUOTED', text: '已报价' }
    ],
    status: 'PENDING',
    cart: [],
    totalQty: 0,
    expectDate: '',
    address: '上海市浦东新区张江路 88 号 3 号楼',
    remark: '',
    list: []
  },

  onShow() {
    this.refreshCart()
    if (this.data.status !== 'PENDING') this.loadList()
  },

  refreshCart() {
    const cart = app.getCart()
    const totalQty = cart.reduce((s, x) => s + Number(x.quantity || 0), 0)
    this.setData({ cart, totalQty })
  },

  onStatus(e) {
    const status = e.currentTarget.dataset.k
    this.setData({ status }, () => {
      if (status !== 'PENDING') this.loadList()
    })
  },

  loadList() {
    getInquiryList({ status: this.data.status }).then(res => {
      this.setData({ list: (res.data && res.data.list) || [] })
    })
  },

  step(e) {
    const { i, d } = e.currentTarget.dataset
    const cart = this.data.cart.slice()
    cart[i].quantity = Math.max(1, Number(cart[i].quantity) + Number(d))
    const totalQty = cart.reduce((s, x) => s + Number(x.quantity || 0), 0)
    this.setData({ cart, totalQty })
    app.globalData.cart = cart
    app.saveCart(cart)
    wx.setTabBarBadge({ index: 2, text: String(cart.length) })
  },

  onDate(e) { this.setData({ expectDate: e.detail.value }) },
  onAddress(e) { this.setData({ address: e.detail.value }) },
  onRemark(e) { this.setData({ remark: e.detail.value }) },

  async submit() {
    if (!this.data.cart.length) return
    if (!this.data.expectDate) {
      wx.showToast({ title: '请选择期望交期', icon: 'none' })
      return
    }
    const payload = {
      expectDate: this.data.expectDate,
      address: this.data.address,
      invoiceType: '增值税专票',
      remark: this.data.remark,
      lines: this.data.cart.map((x, i) => ({
        itemId: x.itemId,
        itemCode: x.itemCode,
        itemName: x.itemName,
        specification: x.specification,
        itemType: x.itemType,
        unitName: x.unitName,
        quantity: x.quantity,
        refPrice: x.refPrice,
        sortNo: i + 1
      }))
    }
    const res = await submitInquiry(payload)
    wx.showModal({
      title: '提交成功',
      content: '询价单号：' + ((res.data && res.data.inquiryCode) || '已生成') + '\n销售将在 2 小时内响应',
      showCancel: false
    })
    app.clearCart()
    this.setData({ cart: [], totalQty: 0, status: 'SUBMITTED', remark: '' }, () => this.loadList())
  }
})

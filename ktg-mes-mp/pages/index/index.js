import { getProducts } from '../../utils/api.js'

const app = getApp()

Page({
  data: {
    keyword: '',
    category: '全部',
    categories: ['全部', '成品', '半成品'],
    products: [],
    quick: [
      { key: 'inquiry', icon: '询', text: '批量询价', bg: '#EAF1FF' },
      { key: 'order', icon: '单', text: '我的订单', bg: '#EFF0FF' },
      { key: 'delivery', icon: '交', text: '交付进度', bg: '#E7F8F0' },
      { key: 'sales', icon: '销', text: '联系销售', bg: '#FFF3E8' }
    ]
  },

  onShow() {
    this.load()
    const cart = app.getCart()
    if (cart.length) {
      wx.setTabBarBadge({ index: 2, text: String(cart.length) })
    }
  },

  async load() {
    const res = await getProducts({ category: this.data.category, keyword: this.data.keyword })
    this.setData({ products: (res.data && res.data.list) || [] })
  },

  onKeyword(e) {
    this.setData({ keyword: e.detail.value })
    clearTimeout(this._t)
    this._t = setTimeout(() => this.load(), 300)
  },

  onCategory(e) {
    this.setData({ category: e.currentTarget.dataset.c }, () => this.load())
  },

  onQuick(e) {
    const key = e.currentTarget.dataset.key
    if (key === 'inquiry' || key === 'order') {
      wx.switchTab({ url: '/pages/inquiry/inquiry' })
    } else if (key === 'delivery') {
      wx.showToast({ title: '交付进度见「我的」', icon: 'none' })
    } else {
      wx.showModal({ title: '联系销售', content: '李销售 138-0000-0001（工作日 9:00-18:00）', showCancel: false })
    }
  },

  addCart(e) {
    const item = this.data.products[e.currentTarget.dataset.i]
    const count = app.addToCart(item)
    wx.showToast({ title: '已加入询价单', icon: 'success' })
    wx.setTabBarBadge({ index: 2, text: String(count) })
  },

  goInquiry() {
    wx.switchTab({ url: '/pages/inquiry/inquiry' })
  }
})

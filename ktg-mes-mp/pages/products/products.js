import { getProducts } from '../../utils/api.js'
const app = getApp()

Page({
  data: {
    keyword: '',
    category: '全部',
    categories: ['全部', '成品', '半成品'],
    sorts: ['综合', '交付期', '起订量'],
    sort: '综合',
    products: [],
    raw: []
  },

  onShow() { this.load() },

  async load() {
    const res = await getProducts({ category: this.data.category, keyword: this.data.keyword })
    const list = (res.data && res.data.list) || []
    this.setData({ raw: list })
    this.applySort(list)
  },

  applySort(list) {
    let out = list.slice()
    if (this.data.sort === '交付期') out.sort((a, b) => a.leadDays - b.leadDays)
    if (this.data.sort === '起订量') out.sort((a, b) => a.minOrderQty - b.minOrderQty)
    this.setData({ products: out })
  },

  onKeyword(e) {
    this.setData({ keyword: e.detail.value })
    clearTimeout(this._t)
    this._t = setTimeout(() => this.load(), 300)
  },

  onCategory(e) {
    this.setData({ category: e.currentTarget.dataset.c }, () => this.load())
  },

  onSort(e) {
    this.setData({ sort: e.currentTarget.dataset.s }, () => this.applySort(this.data.raw))
  },

  addCart(e) {
    const item = this.data.products[e.currentTarget.dataset.i]
    const count = app.addToCart(item)
    wx.showToast({ title: '已加入询价单', icon: 'success' })
    wx.setTabBarBadge({ index: 2, text: String(count) })
  }
})

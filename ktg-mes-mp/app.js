App({
  globalData: {
    // 后端地址：本机调试用 127.0.0.1:8080；真机需换成局域网 IP 或 https 域名
    baseUrl: 'http://127.0.0.1:8080',
    token: '',
    clientInfo: null,
    // 询价单草稿（本地暂存，提交时写库）
    cart: []
  },

  onLaunch() {
    const cart = wx.getStorageSync('MES_CART')
    if (cart) this.globalData.cart = cart
  },

  getCart() {
    return this.globalData.cart || []
  },

  addToCart(item) {
    const cart = this.globalData.cart
    const hit = cart.find(x => x.itemCode === item.itemCode)
    if (hit) {
      hit.quantity += item.quantity || 1
    } else {
      cart.push({
        itemId: item.itemId,
        itemCode: item.itemCode,
        itemName: item.itemName,
        specification: item.specification,
        itemType: item.itemType,
        unitName: item.unitName || '台',
        quantity: item.quantity || 1,
        refPrice: item.refPrice || null
      })
    }
    this.saveCart(cart)
    return cart.length
  },

  removeFromCart(itemCode) {
    this.globalData.cart = this.globalData.cart.filter(x => x.itemCode !== itemCode)
    this.saveCart(this.globalData.cart)
  },

  clearCart() {
    this.globalData.cart = []
    this.saveCart([])
  },

  saveCart(cart) {
    wx.setStorageSync('MES_CART', cart)
    // 更新 tabbar 角标
    if (cart.length) {
      wx.setTabBarBadge({ index: 2, text: String(cart.length) })
    } else {
      wx.removeTabBarBadge({ index: 2 })
    }
  }
})

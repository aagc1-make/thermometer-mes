export default defineAppConfig({
  pages: [
    'pages/home/index',
    'pages/product/index',
    'pages/inquiry/index',
    'pages/mine/index',
    'pages/product-detail/index',
    'pages/inquiry-detail/index',
    'pages/order/index',
    'pages/order-detail/index',
    'pages/address/index',
    'pages/invoice/index',
    'pages/contact/index',
    'pages/delivery/index',
    'pages/statement/index',
    'pages/help/index',
    'pages/sales/index',
    'pages/login/index'
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationStyle: 'custom',
    navigationBarBackgroundColor: '#f5f7fc',
    navigationBarTitleText: '企业订购',
    navigationBarTextStyle: 'black',
    backgroundColor: '#f5f7fc'
  },
  tabBar: {
    color: '#8a99b3',
    selectedColor: '#2b6beb',
    backgroundColor: '#ffffff',
    borderStyle: 'white',
    list: [
      {
        pagePath: 'pages/home/index',
        text: '首页',
        iconPath: 'assets/tabbar/home.png',
        selectedIconPath: 'assets/tabbar/home-selected.png'
      },
      {
        pagePath: 'pages/product/index',
        text: '产品',
        iconPath: 'assets/tabbar/product.png',
        selectedIconPath: 'assets/tabbar/product-selected.png'
      },
      {
        pagePath: 'pages/inquiry/index',
        text: '询价单',
        iconPath: 'assets/tabbar/cart.png',
        selectedIconPath: 'assets/tabbar/cart-selected.png'
      },
      {
        pagePath: 'pages/mine/index',
        text: '我的',
        iconPath: 'assets/tabbar/mine.png',
        selectedIconPath: 'assets/tabbar/mine-selected.png'
      }
    ]
  }
})

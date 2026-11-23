export default defineAppConfig({
  pages: [
    'pages/home/index',
    'pages/workorder/index',
    'pages/todo/index',
    'pages/mine/index',
    'pages/workorder-detail/index',
    'pages/feedback/index',
    'pages/qc/index',
    'pages/receipt/index',
    'pages/issue/index',
    'pages/login/index'
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#2b6beb',
    navigationBarTitleText: '车间工作台',
    navigationBarTextStyle: 'white',
    backgroundColor: '#f5f7fc'
  },
  tabBar: {
    color: '#8a99b3',
    selectedColor: '#2b6beb',
    backgroundColor: '#ffffff',
    borderStyle: 'white',
    list: [
      { pagePath: 'pages/home/index', text: '工作台' },
      { pagePath: 'pages/workorder/index', text: '工单' },
      { pagePath: 'pages/todo/index', text: '待办' },
      { pagePath: 'pages/mine/index', text: '我的' }
    ]
  }
});

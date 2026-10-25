import { getMe } from '../../utils/api.js'

Page({
  data: {
    me: {
      company: '上海华东医疗器械经销有限公司',
      contact: '张采购',
      creditCode: '91310115MA1K35XXXX',
      level: 'A 级战略客户',
      sales: '李销售',
      phone: '13800000001',
      stat: { inquiring: 2, waiting: 1, dealed: 6 }
    },
    orders: [
      { key: 'quote', num: 2, label: '待报价' },
      { key: 'confirm', num: 1, label: '待确认' },
      { key: 'producing', num: 3, label: '生产中' },
      { key: 'delivered', num: 6, label: '已交付' }
    ],
    services: [
      { key: 'addr', icon: '址', text: '收货地址', bg: '#EAF1FF' },
      { key: 'bill', icon: '票', text: '开票信息', bg: '#EFF0FF' },
      { key: 'delivery', icon: '交', text: '交付进度', bg: '#E7F8F0' },
      { key: 'contact', icon: '销', text: '联系销售', bg: '#FFF3E8' },
      { key: 'sample', icon: '样', text: '申请样件', bg: '#EEF6FF' },
      { key: 'help', icon: '帮', text: '帮助中心', bg: '#F4F6FA' }
    ]
  },

  onShow() {
    getMe().then(res => {
      if (res && res.data) this.setData({ me: res.data })
    })
  }
})

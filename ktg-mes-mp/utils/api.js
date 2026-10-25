const app = getApp()

/**
 * 统一请求封装
 * opts: { url, method, data, loading, mock }
 * mock: 后端未就绪时返回本地示例数据，便于先看界面
 */
function request(opts) {
  const { url, method = 'GET', data = {}, loading = false, mock = null } = opts
  const baseUrl = (getApp().globalData.baseUrl || '').replace(/\/$/, '')

  if (mock) {
    // 骨架阶段：先用 mock 数据渲染界面
    return Promise.resolve(mock)
  }

  if (loading) wx.showLoading({ title: '加载中', mask: true })

  return new Promise((resolve, reject) => {
    wx.request({
      url: baseUrl + url,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer ' + (getApp().globalData.token || '')
      },
      success(res) {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          resolve(res.data)
        } else if (res.statusCode === 401) {
          wx.showToast({ title: '请先登录', icon: 'none' })
          reject(res.data)
        } else {
          wx.showToast({ title: (res.data && res.data.msg) || '请求失败', icon: 'none' })
          reject(res.data)
        }
      },
      fail(err) {
        wx.showToast({ title: '网络异常，请检查后端是否启动', icon: 'none' })
        reject(err)
      },
      complete() {
        if (loading) wx.hideLoading()
      }
    })
  })
}

/* ---------- 产品目录 ---------- */
// GET /mobile/inquiry/products?category=&keyword=
export function getProducts(params = {}) {
  return request({
    url: '/mobile/inquiry/products',
    data: params,
    mock: mockProducts(params)
  })
}

/* ---------- 询价单 ---------- */
// POST /mobile/inquiry/submit
export function submitInquiry(payload) {
  return request({
    url: '/mobile/inquiry/submit',
    method: 'POST',
    data: payload,
    loading: true,
    mock: { code: 200, data: { inquiryCode: 'XJ' + Date.now() } }
  })
}

// GET /mobile/inquiry/list?status=
export function getInquiryList(params = {}) {
  return request({
    url: '/mobile/inquiry/list',
    data: params,
    mock: mockInquiryList(params)
  })
}

/* ---------- 我的 ---------- */
export function getMe() {
  return request({
    url: '/mobile/me',
    mock: {
      code: 200,
      data: {
        company: '上海华东医疗器械经销有限公司',
        contact: '张采购',
        creditCode: '91310115MA1K35XXXX',
        level: 'A 级战略客户',
        sales: '李销售',
        phone: '13800000001',
        stat: { inquiring: 2, waiting: 1, dealed: 6 }
      }
    }
  })
}

/* ---------- mock 数据（与 md_item 的真实字段对齐） ---------- */
function mockProducts(params) {
  const all = [
    { itemId: 1, itemCode: 'TG-FG-001', itemName: '红外测温枪整机', specification: 'IRG-300 · 32~42.9℃ · 医用级', itemType: '成品', unitName: '台', minOrderQty: 1, leadDays: 15, refPrice: 5800 },
    { itemId: 2, itemCode: 'TG-SF-001', itemName: '测温显示电控预装件', specification: '含主控板 / LCD / 按键', itemType: '半成品', unitName: '套', minOrderQty: 10, leadDays: 7, refPrice: 1200 },
    { itemId: 3, itemCode: 'TG-SF-002', itemName: '扳机与侧键手柄预装件', specification: '含扳机 / 复位弹簧 / 侧键', itemType: '半成品', unitName: '套', minOrderQty: 10, leadDays: 7, refPrice: 860 },
    { itemId: 4, itemCode: 'TG-FG-002', itemName: '红外测温枪（工业增强型）', specification: 'IRG-500 · -30~500℃ · 工业级', itemType: '成品', unitName: '台', minOrderQty: 1, leadDays: 18, refPrice: 7600 },
    { itemId: 5, itemCode: 'TG-FG-003', itemName: '红外测温枪（家用简装版）', specification: 'IRG-100 · 32~42.9℃ · 家用', itemType: '成品', unitName: '台', minOrderQty: 20, leadDays: 12, refPrice: 1980 },
    { itemId: 6, itemCode: 'TG-SF-003', itemName: '外壳注塑组件', specification: 'ABS 阻燃 · 含前后壳 / 电池盖', itemType: '半成品', unitName: '套', minOrderQty: 50, leadDays: 9, refPrice: 320 }
  ]
  let list = all
  if (params.category && params.category !== '全部') {
    list = list.filter(x => x.itemType === params.category)
  }
  if (params.keyword) {
    const k = params.keyword.toLowerCase()
    list = list.filter(x => (x.itemName + x.itemCode).toLowerCase().indexOf(k) > -1)
  }
  return { code: 200, data: { total: list.length, list } }
}

function mockInquiryList(params) {
  const list = [
    {
      inquiryCode: 'XJ-20261024-001', status: 'QUOTED', statusText: '已报价',
      itemCount: 2, totalQty: 30, quoteAmount: 128000, sales: '李销售',
      lines: [
        { itemName: '红外测温枪整机', itemCode: 'TG-FG-001', quantity: 20, quotePrice: 5600 },
        { itemName: '测温显示电控预装件', itemCode: 'TG-SF-001', quantity: 10, quotePrice: 1150 }
      ]
    },
    {
      inquiryCode: 'XJ-20261024-002', status: 'SUBMITTED', statusText: '已提交',
      itemCount: 1, totalQty: 50, quoteAmount: null, sales: '李销售',
      lines: [{ itemName: '红外测温枪整机', itemCode: 'TG-FG-001', quantity: 50, quotePrice: null }]
    }
  ]
  if (params.status && params.status !== 'ALL') {
    return { code: 200, data: { total: 1, list: list.filter(x => x.status === params.status) } }
  }
  return { code: 200, data: { total: list.length, list } }
}

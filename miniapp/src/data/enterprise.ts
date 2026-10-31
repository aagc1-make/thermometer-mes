import type {
  Address,
  Contact,
  CustomerProfile,
  Delivery,
  Faq,
  Invoice,
  InvoiceProfile,
  Order,
  SalesInfo,
  Statement
} from '@/types';

/**
 * 本地回退数据
 * 由服务器 miniapp-api 读取 MES 数据库（j2eedb）同步生成，请勿手工修改
 */

export const CUSTOMER_PROFILE: CustomerProfile = {
  "company": "华瑞医疗器械有限公司",
  "shortName": "华",
  "creditCode": "91320594MA1X***7K",
  "level": "金牌客户",
  "levelDesc": "金牌客户 · 月结 30 天",
  "buyer": "采购员 王工 · 苏州工业园区",
  "region": "苏州工业园区",
  "salesName": "张经理",
  "salesPhone": "138****6621",
  "stats": {
    "consulting": 3,
    "pending": 1,
    "closed": 8
  },
  "orderCounts": {
    "quoted": 1,
    "confirmed": 1,
    "producing": 2,
    "delivered": 2
  },
  "sales": {
    "name": "张经理",
    "phone": "138****6621",
    "region": "苏州工业园区",
    "wechat": "ktg-service",
    "workTime": "周一至周六 08:30 - 18:00",
    "serviceTel": "400-820-6621"
  }
};

export const SALES_FALLBACK: SalesInfo = {
  "name": "张经理",
  "phone": "138****6621",
  "region": "苏州工业园区",
  "wechat": "ktg-service",
  "workTime": "周一至周六 08:30 - 18:00",
  "serviceTel": "400-820-6621"
};

export const ADDRESSES_FALLBACK: Address[] = [
  {
    "id": 1,
    "contactName": "王工",
    "contactTel": "0512-8888 6621",
    "region": "江苏省苏州市",
    "detail": "苏州工业园区星湖街 328 号 B 座 5 楼",
    "tag": "默认",
    "isDefault": true
  },
  {
    "id": 2,
    "contactName": "李敏",
    "contactTel": "139****5528",
    "region": "上海市",
    "detail": "浦东新区张江高科技园区博云路 2 号",
    "tag": "收货点",
    "isDefault": false
  },
  {
    "id": 3,
    "contactName": "陈工",
    "contactTel": "137****9012",
    "region": "江苏省无锡市",
    "detail": "新吴区长江南路 18 号 3 号厂房",
    "tag": "备用",
    "isDefault": false
  }
];

export const ORDERS_FALLBACK: Order[] = [
  {
    "orderId": 1,
    "orderNo": "SO20261020-002",
    "inquiryNo": "XJ20261020-002",
    "status": "quoted",
    "statusName": "待报价",
    "itemCount": 1,
    "totalQty": 60,
    "totalAmount": 0,
    "deliveryAddress": "苏州工业园区星湖街 328 号 B 座 5 楼",
    "invoiceType": "增值税专用发票 13%",
    "expectedDate": "2026-11-03",
    "remark": "等待销售确认单价",
    "createTime": "2026-10-20 14:32",
    "updateTime": "2026-10-20 14:32",
    "lines": [
      {
        "lineNo": 1,
        "itemCode": "IRG-FG-002",
        "itemName": "医用高精度测温枪",
        "specification": "量程 32.0-42.9°C · 精度 ±0.1°C",
        "qty": 60,
        "unitName": "件",
        "unitPrice": 0,
        "amount": 0,
        "art": "precision"
      }
    ]
  },
  {
    "orderId": 2,
    "orderNo": "SO20261018-001",
    "inquiryNo": "XJ20261016-001",
    "status": "confirmed",
    "statusName": "待确认",
    "itemCount": 2,
    "totalQty": 260,
    "totalAmount": 156000,
    "deliveryAddress": "苏州工业园区星湖街 328 号 B 座 5 楼",
    "invoiceType": "增值税专用发票 13%",
    "expectedDate": "2026-11-08",
    "remark": "整机与预装件同批次交付",
    "createTime": "2026-10-18 10:12",
    "updateTime": "2026-10-19 09:20",
    "lines": [
      {
        "lineNo": 1,
        "itemCode": "IRG-FG-001",
        "itemName": "基础款红外测温枪",
        "specification": "量程 32.0-42.9°C · 精度 ±0.2°C",
        "qty": 200,
        "unitName": "件",
        "unitPrice": 432,
        "amount": 86400,
        "art": "thermometer"
      },
      {
        "lineNo": 2,
        "itemCode": "IRG-FG-002",
        "itemName": "医用高精度测温枪",
        "specification": "量程 32.0-42.9°C · 精度 ±0.1°C",
        "qty": 60,
        "unitName": "件",
        "unitPrice": 1160,
        "amount": 69600,
        "art": "precision"
      }
    ]
  },
  {
    "orderId": 3,
    "orderNo": "SO20261015-001",
    "status": "producing",
    "statusName": "生产中",
    "itemCount": 2,
    "totalQty": 700,
    "totalAmount": 182000,
    "deliveryAddress": "苏州工业园区星湖街 328 号 B 座 5 楼",
    "invoiceType": "增值税专用发票 13%",
    "expectedDate": "2026-10-30",
    "remark": "已完成 SMT 贴片，进入整机组装",
    "createTime": "2026-10-15 09:05",
    "updateTime": "2026-10-23 16:40",
    "lines": [
      {
        "lineNo": 1,
        "itemCode": "IRG-SA-CTRL-001",
        "itemName": "测温显示电控预装件",
        "specification": "主控 / 传感 / 显示预装",
        "qty": 500,
        "unitName": "件",
        "unitPrice": 156,
        "amount": 78000,
        "art": "pcb"
      },
      {
        "lineNo": 2,
        "itemCode": "IRG-SA-TRIG-001",
        "itemName": "扳机侧键手柄预装件",
        "specification": "前壳 / 扳机 / 侧键预装",
        "qty": 200,
        "unitName": "件",
        "unitPrice": 520,
        "amount": 104000,
        "art": "trigger"
      }
    ]
  },
  {
    "orderId": 4,
    "orderNo": "SO20261012-001",
    "inquiryNo": "XJ20261012-001",
    "status": "producing",
    "statusName": "生产中",
    "itemCount": 1,
    "totalQty": 500,
    "totalAmount": 128000,
    "deliveryAddress": "上海市浦东新区张江高科技园区博云路 2 号",
    "invoiceType": "增值税专用发票 13%",
    "expectedDate": "2026-10-26",
    "remark": "按客户固件版本定制烧录",
    "createTime": "2026-10-12 09:18",
    "updateTime": "2026-10-22 11:10",
    "lines": [
      {
        "lineNo": 1,
        "itemCode": "IRG-SA-PCBA-001",
        "itemName": "主板贴片组件",
        "specification": "四层板 · 已贴片 · 已烧录",
        "qty": 500,
        "unitName": "件",
        "unitPrice": 256,
        "amount": 128000,
        "art": "mainboard"
      }
    ]
  },
  {
    "orderId": 5,
    "orderNo": "SO20261005-001",
    "status": "delivered",
    "statusName": "已交付",
    "itemCount": 1,
    "totalQty": 200,
    "totalAmount": 86400,
    "deliveryAddress": "苏州工业园区星湖街 328 号 B 座 5 楼",
    "invoiceType": "增值税专用发票 13%",
    "expectedDate": "2026-10-10",
    "remark": "已签收，回单已上传",
    "createTime": "2026-10-05 08:40",
    "updateTime": "2026-10-09 15:30",
    "lines": [
      {
        "lineNo": 1,
        "itemCode": "IRG-FG-001",
        "itemName": "基础款红外测温枪",
        "specification": "量程 32.0-42.9°C · 精度 ±0.2°C",
        "qty": 200,
        "unitName": "件",
        "unitPrice": 432,
        "amount": 86400,
        "art": "thermometer"
      }
    ]
  },
  {
    "orderId": 6,
    "orderNo": "SO20260928-001",
    "status": "delivered",
    "statusName": "已交付",
    "itemCount": 3,
    "totalQty": 900,
    "totalAmount": 236000,
    "deliveryAddress": "江苏省无锡市新吴区长江南路 18 号 3 号厂房",
    "invoiceType": "增值税专用发票 13%",
    "expectedDate": "2026-10-08",
    "remark": "分两批发运，全部签收",
    "createTime": "2026-09-28 15:22",
    "updateTime": "2026-10-08 17:05",
    "lines": [
      {
        "lineNo": 1,
        "itemCode": "IRG-FG-003",
        "itemName": "壁挂式测温终端",
        "specification": "量程 30.0-45.0°C · 支持壁挂安装",
        "qty": 300,
        "unitName": "台",
        "unitPrice": 320,
        "amount": 96000,
        "art": "wall"
      },
      {
        "lineNo": 2,
        "itemCode": "IRG-SA-SENS-001",
        "itemName": "红外传感模组",
        "specification": "数字输出 · 出厂单点校准",
        "qty": 400,
        "unitName": "件",
        "unitPrice": 190,
        "amount": 76000,
        "art": "sensor"
      },
      {
        "lineNo": 3,
        "itemCode": "IRG-ST-FRONT-001",
        "itemName": "前壳注塑件",
        "specification": "ABS 阻燃 · 白色哑光",
        "qty": 200,
        "unitName": "件",
        "unitPrice": 320,
        "amount": 64000,
        "art": "trigger"
      }
    ]
  }
];

export const DELIVERIES_FALLBACK: Delivery[] = [
  {
    "id": 1,
    "deliveryNo": "DL20261015-001",
    "orderNo": "SO20261015-001",
    "status": "producing",
    "statusName": "生产中",
    "totalQty": 700,
    "shippedQty": 0,
    "planDate": "2026-10-30",
    "address": "苏州工业园区星湖街 328 号 B 座 5 楼",
    "progress": 60,
    "remark": "整机组装中，预计 10 月 28 日完工",
    "updateTime": "2026-10-23 16:40"
  },
  {
    "id": 2,
    "deliveryNo": "DL20261012-001",
    "orderNo": "SO20261012-001",
    "status": "producing",
    "statusName": "生产中",
    "totalQty": 500,
    "shippedQty": 0,
    "planDate": "2026-11-02",
    "address": "上海市浦东新区张江高科技园区博云路 2 号",
    "progress": 35,
    "remark": "贴片已完成，等待固件烧录",
    "updateTime": "2026-10-22 11:10"
  },
  {
    "id": 4,
    "deliveryNo": "DL20260928-001",
    "orderNo": "SO20260928-001",
    "status": "delivered",
    "statusName": "已签收",
    "totalQty": 900,
    "shippedQty": 900,
    "planDate": "2026-10-08",
    "shipDate": "2026-10-06",
    "carrier": "德邦物流",
    "trackingNo": "DP9876543210",
    "address": "江苏省无锡市新吴区长江南路 18 号 3 号厂房",
    "progress": 100,
    "remark": "分两批发运，全部签收",
    "updateTime": "2026-10-08 17:05"
  },
  {
    "id": 3,
    "deliveryNo": "DL20261005-001",
    "orderNo": "SO20261005-001",
    "status": "delivered",
    "statusName": "已签收",
    "totalQty": 200,
    "shippedQty": 200,
    "planDate": "2026-10-10",
    "shipDate": "2026-10-07",
    "carrier": "顺丰速运",
    "trackingNo": "SF1234567890123",
    "address": "苏州工业园区星湖街 328 号 B 座 5 楼",
    "progress": 100,
    "remark": "客户已签收，回单已归档",
    "updateTime": "2026-10-09 15:30"
  }
];

export const STATEMENTS_FALLBACK: Statement[] = [
  {
    "id": 2,
    "statementNo": "DZ202610-001",
    "period": "2026 年 10 月",
    "totalAmount": 156000,
    "paidAmount": 60000,
    "unpaidAmount": 96000,
    "status": "partial",
    "statusName": "部分付款",
    "billDate": "2026-10-20",
    "dueDate": "2026-11-19",
    "remark": "预付款 60000 元已到账"
  },
  {
    "id": 1,
    "statementNo": "DZ202609-001",
    "period": "2026 年 9 月",
    "totalAmount": 236000,
    "paidAmount": 236000,
    "unpaidAmount": 0,
    "status": "paid",
    "statusName": "已结清",
    "billDate": "2026-10-01",
    "dueDate": "2026-10-31",
    "remark": "已按合同约定结清"
  }
];

export const INVOICES_FALLBACK: Invoice[] = [
  {
    "id": 2,
    "invoiceNo": "FP20261021-001",
    "invoiceTitle": "华瑞医疗器械有限公司",
    "taxNo": "91320594MA1X***7K",
    "invoiceType": "增值税专用发票 13%",
    "amount": 156000,
    "status": "applied",
    "statusName": "开票中",
    "applyTime": "2026-10-21 09:40",
    "email": "finance@huarui-med.com",
    "remark": "财务审核中，预计 3 个工作日开具"
  },
  {
    "id": 1,
    "invoiceNo": "FP20261015-001",
    "invoiceTitle": "华瑞医疗器械有限公司",
    "taxNo": "91320594MA1X***7K",
    "invoiceType": "增值税专用发票 13%",
    "amount": 128000,
    "status": "issued",
    "statusName": "已开票",
    "applyTime": "2026-10-12 10:00",
    "issueTime": "2026-10-15",
    "email": "finance@huarui-med.com",
    "remark": "纸质发票已寄出"
  }
];

export const INVOICE_PROFILE_FALLBACK: InvoiceProfile = {
  "invoiceTitle": "华瑞医疗器械有限公司",
  "taxNo": "91320594MA1X***7K",
  "invoiceType": "增值税专用发票 13%",
  "email": "finance@huarui-med.com"
};

export const CONTACTS_FALLBACK: Contact[] = [
  {
    "id": 1,
    "name": "王工",
    "dept": "采购经理",
    "tel": "0512-8888 6621",
    "email": "wang@huarui-med.com",
    "isDefault": true
  },
  {
    "id": 2,
    "name": "李敏",
    "dept": "收货主管",
    "tel": "139****5528",
    "email": "li@huarui-med.com",
    "isDefault": false
  },
  {
    "id": 3,
    "name": "陈工",
    "dept": "设备工程师",
    "tel": "137****9012",
    "email": "chen@huarui-med.com",
    "isDefault": false
  }
];

export const FAQS_FALLBACK: Faq[] = [
  {
    "id": 1,
    "category": "询价与报价",
    "question": "询价单提交后多久可以收到报价？",
    "answer": "工作时间内提交的询价单，销售会在 2 小时内联系确认；非工作时间顺延至下一个工作日。报价单有效期 7 天，请在有效期内确认。"
  },
  {
    "id": 2,
    "category": "询价与报价",
    "question": "起订量可以低于产品标称吗？",
    "answer": "成品起订量 50 件、半成品 100 件为标准起订量。小批量打样可与专属销售沟通，按样品政策执行。"
  },
  {
    "id": 3,
    "category": "询价与报价",
    "question": "半成品可以按我们的图纸定制吗？",
    "answer": "电控组件与结构件均支持定制，需提供图纸、BOM 与固件版本，销售会安排工程评估并给出定制交期。"
  },
  {
    "id": 4,
    "category": "交付与物流",
    "question": "如何查看订单的交付进度？",
    "answer": "在「我的 - 交付进度」中可查看每个订单的生产进度、计划交付日期与物流单号，也可联系专属销售获取每日进度。"
  },
  {
    "id": 5,
    "category": "交付与物流",
    "question": "发货后多久可以收到货物？",
    "answer": "江苏省内顺丰次日达，其他地区 2-3 天。大件物流按德邦时效执行，发货后可在交付进度中复制运单号查询。"
  },
  {
    "id": 6,
    "category": "开票与对账",
    "question": "发票什么时候可以开具？",
    "answer": "订单发货并确认收货后可申请开票，财务在 3 个工作日内开具。增值税专用发票需提供完整开票信息。"
  },
  {
    "id": 7,
    "category": "开票与对账",
    "question": "对账单在哪里查看？",
    "answer": "每月 1 日出上个月对账单，可在「我的 - 对账单」查看应结、已付与未付金额，账期为月结 30 天。"
  },
  {
    "id": 8,
    "category": "售后与支持",
    "question": "产品出现测量偏差怎么处理？",
    "answer": "请先确认环境温度与测量距离，仍异常可在「联系销售」中反馈，售后会在 24 小时内响应并提供校准或换货方案。"
  }
];

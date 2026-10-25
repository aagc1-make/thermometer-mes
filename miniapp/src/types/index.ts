// 业务类型定义（企业订购小程序）

/** 产品画稿类型，用于本地 CSS 绘制的产品示意图 */
export type ProductArt =
  | 'thermometer'
  | 'precision'
  | 'wall'
  | 'pcb'
  | 'trigger'
  | 'mainboard'
  | 'sensor';

/** 产品分类：成品 / 半成品 */
export type ProductCategory = 'finished' | 'semi';

/** 产品 */
export interface Product {
  id: string;
  name: string;
  code: string;
  category: ProductCategory;
  categoryName: string;
  /** 半成品细分：电控组件 / 结构件 */
  group?: '电控组件' | '结构件';
  /** 规格参数文案 */
  param: string;
  /** 起订量 */
  moq: number;
  /** 单位 */
  unit: string;
  /** 交期（天） */
  leadDays: number;
  art: ProductArt;
  /** 详情说明 */
  desc: string;
}

/** 询价单中的商品行 */
export interface InquiryItem {
  productId: string;
  name: string;
  code: string;
  categoryName: string;
  art: ProductArt;
  qty: number;
  moq: number;
  unit: string;
}

/** 询价单状态：待报价 / 已报价 */
export type InquiryStatus = 'submitted' | 'quoted';

/** 询价单记录 */
export interface InquiryRecord {
  id: string;
  status: InquiryStatus;
  items: InquiryItem[];
  itemCount: number;
  totalQty: number;
  submitTime: string;
  expectedDate: string;
  address: string;
  invoice: string;
  remark: string;
  /** 已报价金额（元） */
  quoteAmount?: number;
}

/** 询价单基础信息（期望交期/收货地址/开票方式/备注） */
export interface InquiryMeta {
  expectedDate: string;
  address: string;
  invoice: string;
  remark: string;
}

/** 企业客户信息 */
export interface Customer {
  company: string;
  shortName: string;
  creditCode: string;
  level: string;
  levelDesc: string;
  buyer: string;
  region: string;
  salesName: string;
  salesPhone: string;
  stats: {
    consulting: number;
    pending: number;
    closed: number;
  };
}

/** 订单状态 */
export type OrderStatus = 'quoted' | 'confirmed' | 'producing' | 'delivered';

/** 收货地址 */
export interface Address {
  id: number;
  contactName: string;
  contactTel: string;
  region: string;
  detail: string;
  tag?: string;
  isDefault: boolean;
}

/** 新增 / 编辑收货地址入参 */
export interface AddressPayload {
  contactName: string;
  contactTel: string;
  region: string;
  detail: string;
  tag?: string;
  isDefault?: boolean;
}

/** 订单行 */
export interface OrderLine {
  lineNo?: number;
  itemCode: string;
  itemName: string;
  specification?: string;
  qty: number;
  unitName: string;
  unitPrice: number;
  amount: number;
  art?: ProductArt;
}

/** 订单 */
export interface Order {
  orderId: number;
  orderNo: string;
  inquiryNo?: string;
  status: OrderStatus;
  statusName: string;
  itemCount: number;
  totalQty: number;
  totalAmount: number;
  deliveryAddress: string;
  invoiceType: string;
  expectedDate?: string;
  remark?: string;
  createTime: string;
  updateTime?: string;
  lines: OrderLine[];
  delivery?: Delivery | null;
}

/** 交付进度 */
export interface Delivery {
  id?: number;
  deliveryNo: string;
  orderNo: string;
  status: 'waiting' | 'producing' | 'shipped' | 'delivered';
  statusName: string;
  totalQty: number;
  shippedQty: number;
  planDate?: string;
  shipDate?: string;
  carrier?: string;
  trackingNo?: string;
  address?: string;
  progress: number;
  remark?: string;
  updateTime: string;
}

/** 对账单 */
export interface Statement {
  id?: number;
  statementNo: string;
  period: string;
  totalAmount: number;
  paidAmount: number;
  unpaidAmount: number;
  status: 'unpaid' | 'partial' | 'paid';
  statusName: string;
  billDate: string;
  dueDate: string;
  remark?: string;
}

/** 发票 */
export interface Invoice {
  id?: number;
  invoiceNo: string;
  invoiceTitle: string;
  taxNo: string;
  invoiceType: string;
  amount: number;
  status: 'applied' | 'issued';
  statusName: string;
  applyTime: string;
  issueTime?: string;
  email?: string;
  remark?: string;
}

/** 开票信息 / 开票申请入参 */
export interface InvoiceProfile {
  invoiceTitle: string;
  taxNo: string;
  invoiceType: string;
  email?: string;
}

export interface InvoiceApplyPayload {
  amount: number;
  invoiceType: string;
  email?: string;
  remark?: string;
}

/** 联系人 */
export interface Contact {
  id: number;
  name: string;
  dept?: string;
  tel: string;
  email?: string;
  isDefault: boolean;
}

/** 专属销售 */
export interface SalesInfo {
  name: string;
  phone: string;
  region?: string;
  wechat?: string;
  workTime?: string;
  serviceTel?: string;
}

/** 帮助中心 FAQ */
export interface Faq {
  id: number;
  category: string;
  question: string;
  answer: string;
}

/** 客户档案（含订单状态数量与销售信息） */
export interface CustomerProfile extends Customer {
  orderCounts: Record<OrderStatus, number>;
  sales?: SalesInfo;
}

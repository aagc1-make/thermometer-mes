/**
 * 从服务器 miniapp-api（读取 MES 库 j2eedb）同步小程序本地兜底数据
 *
 * 用法：npm run sync:data
 * 可通过环境变量覆盖：MINIAPP_API_BASE / MINIAPP_USER / MINIAPP_PASS
 */
const fs = require('fs');
const path = require('path');

const BASE = process.env.MINIAPP_API_BASE || 'http://101.37.129.105:8081';
const USER = process.env.MINIAPP_USER || 'demo';
const PASS = process.env.MINIAPP_PASS || 'demo123';
const OUT = path.resolve(__dirname, '../src/data');

const ARTS = ['thermometer', 'precision', 'wall', 'pcb', 'trigger', 'mainboard', 'sensor'];

const num = (v, d = 0) => {
  const n = Number(v);
  return Number.isFinite(n) ? n : d;
};
const text = (v, d = '') => (v === null || v === undefined || v === '' ? d : String(v));
const art = (v, finished) => (v && ARTS.includes(v) ? v : finished ? 'thermometer' : 'pcb');

async function api(path, token) {
  const res = await fetch(BASE + path, {
    headers: token ? { Authorization: 'Bearer ' + token } : {}
  });
  const body = await res.json();
  if (body.code !== 200) throw new Error(path + ' => ' + body.msg);
  return body.data !== undefined ? body.data : body.rows;
}

const mapLine = (r) => ({
  lineNo: num(r.lineNo, 1),
  itemCode: text(r.itemCode),
  itemName: text(r.itemName),
  specification: text(r.specification) || undefined,
  qty: num(r.qty),
  unitName: text(r.unitName, '件'),
  unitPrice: num(r.unitPrice),
  amount: num(r.amount),
  art: art(r.art, true)
});

const mapDelivery = (r) => ({
  id: num(r.id),
  deliveryNo: text(r.deliveryNo),
  orderNo: text(r.orderNo),
  status: text(r.status, 'producing'),
  statusName: text(r.statusName, '生产中'),
  totalQty: num(r.totalQty),
  shippedQty: num(r.shippedQty),
  planDate: text(r.planDate) || undefined,
  shipDate: text(r.shipDate) || undefined,
  carrier: text(r.carrier) || undefined,
  trackingNo: text(r.trackingNo) || undefined,
  address: text(r.address) || undefined,
  progress: num(r.progress),
  remark: text(r.remark) || undefined,
  updateTime: text(r.updateTime)
});

const mapOrder = (r) => {
  const lines = Array.isArray(r.lines) ? r.lines : [];
  return {
    orderId: num(r.orderId),
    orderNo: text(r.orderNo),
    inquiryNo: text(r.inquiryNo) || undefined,
    status: text(r.status, 'quoted'),
    statusName: text(r.statusName, '待报价'),
    itemCount: num(r.itemCount, lines.length),
    totalQty: num(r.totalQty),
    totalAmount: num(r.totalAmount),
    deliveryAddress: text(r.deliveryAddress),
    invoiceType: text(r.invoiceType),
    expectedDate: text(r.expectedDate) || undefined,
    remark: text(r.remark) || undefined,
    createTime: text(r.createTime),
    updateTime: text(r.updateTime) || undefined,
    lines: lines.map(mapLine),
    ...(r.delivery ? { delivery: mapDelivery(r.delivery) } : {})
  };
};

const mapAddress = (r) => ({
  id: num(r.id),
  contactName: text(r.contactName),
  contactTel: text(r.contactTel),
  region: text(r.region),
  detail: text(r.detail),
  ...(r.tag ? { tag: text(r.tag) } : {}),
  isDefault: r.isDefault === true || r.isDefault === 1 || r.isDefault === 'Y'
});

const mapStatement = (r) => ({
  id: num(r.id),
  statementNo: text(r.statementNo),
  period: text(r.period),
  totalAmount: num(r.totalAmount),
  paidAmount: num(r.paidAmount),
  unpaidAmount: num(r.unpaidAmount),
  status: text(r.status, 'unpaid'),
  statusName: text(r.statusName, '待付款'),
  billDate: text(r.billDate),
  dueDate: text(r.dueDate),
  ...(text(r.remark) ? { remark: text(r.remark) } : {})
});

const mapInvoice = (r) => ({
  id: num(r.id),
  invoiceNo: text(r.invoiceNo),
  invoiceTitle: text(r.invoiceTitle),
  taxNo: text(r.taxNo),
  invoiceType: text(r.invoiceType, '增值税专用发票 13%'),
  amount: num(r.amount),
  status: text(r.status, 'applied'),
  statusName: text(r.statusName, '开票中'),
  applyTime: text(r.applyTime),
  ...(text(r.issueTime) ? { issueTime: text(r.issueTime) } : {}),
  ...(text(r.email) ? { email: text(r.email) } : {}),
  ...(text(r.remark) ? { remark: text(r.remark) } : {})
});

const mapContact = (r) => ({
  id: num(r.id),
  name: text(r.name),
  ...(text(r.dept) ? { dept: text(r.dept) } : {}),
  tel: text(r.tel),
  ...(text(r.email) ? { email: text(r.email) } : {}),
  isDefault: r.isDefault === true || r.isDefault === 1 || r.isDefault === 'Y'
});

const mapSales = (r) => ({
  name: text(r.name),
  phone: text(r.phone),
  ...(text(r.region) ? { region: text(r.region) } : {}),
  ...(text(r.wechat) ? { wechat: text(r.wechat) } : {}),
  ...(text(r.workTime) ? { workTime: text(r.workTime) } : {}),
  ...(text(r.serviceTel) ? { serviceTel: text(r.serviceTel) } : {})
});

const mapFaq = (r) => ({
  id: num(r.id),
  category: text(r.category),
  question: text(r.question),
  answer: text(r.answer)
});

const d2 = (date) => date.toISOString().slice(0, 10);
const j = (obj) => JSON.stringify(obj, null, 2);

function write(file, content) {
  fs.writeFileSync(path.join(OUT, file), content, 'utf8');
  console.log('written src/data/' + file);
}

(async () => {
  const res = await fetch(BASE + '/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: USER, password: PASS })
  });
  const token = (await res.json()).data.token;

  const [
    profile,
    addresses,
    orders,
    deliveries,
    statements,
    invoices,
    invoiceProfile,
    contacts,
    sales,
    faqs
  ] = await Promise.all([
    api('/api/v1/customer/profile', token),
    api('/api/v1/addresses', token),
    api('/api/v1/orders', token),
    api('/api/v1/deliveries', token),
    api('/api/v1/statements', token),
    api('/api/v1/invoices', token),
    api('/api/v1/invoices/profile', token),
    api('/api/v1/contacts', token),
    api('/api/v1/sales', token),
    api('/api/v1/help/faq', token),
  ]);

  // 产品目录（成品/半成品/物料）以 MES 主数据结构为准，由人工镜像维护，不再由接口覆盖

  const customer = {
    company: text(profile.company),
    shortName: text(profile.shortName),
    creditCode: text(profile.creditCode),
    level: text(profile.level),
    levelDesc: text(profile.levelDesc),
    buyer: text(profile.buyer),
    region: text(profile.region),
    salesName: text(profile.salesName),
    salesPhone: text(profile.salesPhone),
    stats: {
      consulting: num(profile.stats && profile.stats.consulting),
      pending: num(profile.stats && profile.stats.pending),
      closed: num(profile.stats && profile.stats.closed)
    }
  };
  const counts = profile.orderCounts || {};
  const defaultAddress =
    addresses.find((a) => a.isDefault === true || a.isDefault === 1 || a.isDefault === 'Y') ||
    addresses[0] ||
    null;
  const metaAddress = defaultAddress
    ? `${text(defaultAddress.region)} ${text(defaultAddress.detail)}`.trim()
    : '';
  const expected = new Date();
  expected.setDate(expected.getDate() + 7);

  write(
    'customer.ts',
    `import type { Customer, InquiryMeta } from '@/types';

/**
 * 当前登录的企业客户
 * 由服务器 miniapp-api 读取 MES 数据库（j2eedb）同步生成，请勿手工修改
 */
export const CUSTOMER: Customer = ${j(customer)};

/** 询价单默认基础信息 */
export const DEFAULT_INQUIRY_META: InquiryMeta = ${j({
      expectedDate: d2(expected),
      address: metaAddress,
      invoice: text(invoiceProfile.invoiceType, '增值税专用发票 13%'),
      remark: ''
    })};

/** 我的订单状态入口 */
export const ORDER_STATUS_LIST = ${j([
      { key: 'quoted', name: '待报价', count: num(counts.quoted) },
      { key: 'confirmed', name: '待确认', count: num(counts.confirmed) },
      { key: 'producing', name: '生产中', count: num(counts.producing) },
      { key: 'delivered', name: '已交付', count: num(counts.delivered) }
    ])} as const;

/** 常用服务入口 */
export const SERVICE_LIST = [
  { key: 'address', name: '收货地址', icon: 'pin' },
  { key: 'invoice', name: '开票信息', icon: 'doc' },
  { key: 'contact', name: '联系人', icon: 'users' },
  { key: 'delivery', name: '交付进度', icon: 'truck' },
  { key: 'statement', name: '对账单', icon: 'card' },
  { key: 'help', name: '帮助中心', icon: 'help' }
] as const;
`
  );

  const orderCounts = {
    quoted: num(counts.quoted),
    confirmed: num(counts.confirmed),
    producing: num(counts.producing),
    delivered: num(counts.delivered)
  };
  const customerProfile = {
    ...customer,
    orderCounts,
    ...(sales ? { sales: mapSales(sales) } : {})
  };

  write(
    'enterprise.ts',
    `import type {
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

export const CUSTOMER_PROFILE: CustomerProfile = ${j(customerProfile)};

export const SALES_FALLBACK: SalesInfo = ${j(sales ? mapSales(sales) : customer)};

export const ADDRESSES_FALLBACK: Address[] = ${j(addresses.map(mapAddress))};

export const ORDERS_FALLBACK: Order[] = ${j(orders.map(mapOrder))};

export const DELIVERIES_FALLBACK: Delivery[] = ${j(deliveries.map(mapDelivery))};

export const STATEMENTS_FALLBACK: Statement[] = ${j(statements.map(mapStatement))};

export const INVOICES_FALLBACK: Invoice[] = ${j(invoices.map(mapInvoice))};

export const INVOICE_PROFILE_FALLBACK: InvoiceProfile = ${j({
      invoiceTitle: text(invoiceProfile.invoiceTitle, customer.company),
      taxNo: text(invoiceProfile.taxNo, customer.creditCode),
      invoiceType: text(invoiceProfile.invoiceType, '增值税专用发票 13%'),
      ...(text(invoiceProfile.email) ? { email: text(invoiceProfile.email) } : {})
    })};

export const CONTACTS_FALLBACK: Contact[] = ${j(contacts.map(mapContact))};

export const FAQS_FALLBACK: Faq[] = ${j(faqs.map(mapFaq))};
`
  );

  console.log(
    `同步完成：产品 ${products.length} 条、订单 ${orders.length} 条、交付 ${deliveries.length} 条、对账 ${statements.length} 条、发票 ${invoices.length} 条、地址 ${addresses.length} 条`
  );
})().catch((e) => {
  console.error(e);
  process.exit(1);
});

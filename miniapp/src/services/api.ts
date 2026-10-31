import type {
  Address,
  AddressPayload,
  Contact,
  CustomerProfile,
  Delivery,
  Faq,
  Invoice,
  InvoiceApplyPayload,
  InvoiceProfile,
  InquiryRecord,
  Order,
  OrderLine,
  OrderStatus,
  Product,
  ProductArt,
  SalesInfo,
  Statement
} from '@/types';
import { PRODUCTS } from '@/data/products';
import {
  ADDRESSES_FALLBACK,
  CONTACTS_FALLBACK,
  CUSTOMER_PROFILE,
  DELIVERIES_FALLBACK,
  FAQS_FALLBACK,
  INVOICES_FALLBACK,
  INVOICE_PROFILE_FALLBACK,
  ORDERS_FALLBACK,
  SALES_FALLBACK,
  STATEMENTS_FALLBACK
} from '@/data/enterprise';
import { CUSTOMER } from '@/data/customer';
import { request } from './request';

/**
 * 是否启用后端真实数据
 * - true ：优先调用独立后端 /api/v1/**，失败时自动回落到本地演示数据
 * - false：完全使用本地数据
 */
export const USE_REMOTE_API = true;

/** 询价单提交入参 */
export interface InquirySubmitPayload {
  clientId?: number;
  clientName: string;
  buyer: string;
  contactTel?: string;
  expectedDate: string;
  deliveryAddress: string;
  invoiceType: string;
  remark?: string;
  lines: {
    itemId?: number;
    itemCode: string;
    itemName: string;
    specification?: string;
    itemOrProduct?: string;
    qty: number;
    unitName?: string;
    moq?: number;
    leadDays?: number;
  }[];
}

/** 询价单返回结构 */
export interface InquiryResult {
  inquiryId?: number;
  inquiryNo?: string;
  status?: string;
  itemCount?: number;
  totalQty?: number;
  quoteAmount?: number;
  createTime?: string;
}

function clone<T>(data: T): T {
  return JSON.parse(JSON.stringify(data)) as T;
}

function num(value: unknown, fallback = 0): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function text(value: unknown, fallback = ''): string {
  return value === null || value === undefined || value === '' ? fallback : String(value);
}

const ARTS: ProductArt[] = ['thermometer', 'precision', 'wall', 'pcb', 'trigger', 'mainboard', 'sensor'];

/** 订单行 / 询价行的示意图类型 */
function normalizeArt(value: unknown, isFinished: boolean): ProductArt {
  if (value && ARTS.includes(value as ProductArt)) {
    return value as ProductArt;
  }
  return isFinished ? 'thermometer' : 'pcb';
}

function normalizeAddress(row: Record<string, unknown>): Address {
  const flag = row.isDefault;
  return {
    id: num(row.id),
    contactName: text(row.contactName),
    contactTel: text(row.contactTel),
    region: text(row.region),
    detail: text(row.detail),
    tag: row.tag ? text(row.tag) : undefined,
    isDefault: flag === true || flag === 1 || flag === 'Y'
  };
}

function normalizeOrderLine(row: Record<string, unknown>): OrderLine {
  return {
    lineNo: num(row.lineNo, 1),
    itemCode: text(row.itemCode),
    itemName: text(row.itemName),
    specification: text(row.specification) || undefined,
    qty: num(row.qty),
    unitName: text(row.unitName, '件'),
    unitPrice: num(row.unitPrice),
    amount: num(row.amount),
    art: normalizeArt(row.art, true)
  };
}

function normalizeDelivery(row: Record<string, unknown>): Delivery {
  return {
    id: num(row.id),
    deliveryNo: text(row.deliveryNo),
    orderNo: text(row.orderNo),
    status: (text(row.status, 'producing') as Delivery['status']) || 'producing',
    statusName: text(row.statusName, '生产中'),
    totalQty: num(row.totalQty),
    shippedQty: num(row.shippedQty),
    planDate: text(row.planDate) || undefined,
    shipDate: text(row.shipDate) || undefined,
    carrier: text(row.carrier) || undefined,
    trackingNo: text(row.trackingNo) || undefined,
    address: text(row.address) || undefined,
    progress: num(row.progress),
    remark: text(row.remark) || undefined,
    updateTime: text(row.updateTime)
  };
}

function normalizeOrder(row: Record<string, unknown>): Order {
  const lines = Array.isArray(row.lines) ? (row.lines as Record<string, unknown>[]) : [];
  return {
    orderId: num(row.orderId),
    orderNo: text(row.orderNo),
    inquiryNo: text(row.inquiryNo) || undefined,
    status: (text(row.status, 'quoted') as OrderStatus) || 'quoted',
    statusName: text(row.statusName, '待报价'),
    itemCount: num(row.itemCount, lines.length),
    totalQty: num(row.totalQty),
    totalAmount: num(row.totalAmount),
    deliveryAddress: text(row.deliveryAddress),
    invoiceType: text(row.invoiceType),
    expectedDate: text(row.expectedDate) || undefined,
    remark: text(row.remark) || undefined,
    createTime: text(row.createTime),
    updateTime: text(row.updateTime) || undefined,
    lines: lines.map(normalizeOrderLine),
    delivery: row.delivery ? normalizeDelivery(row.delivery as Record<string, unknown>) : null
  };
}

function normalizeStatement(row: Record<string, unknown>): Statement {
  return {
    id: num(row.id),
    statementNo: text(row.statementNo),
    period: text(row.period),
    totalAmount: num(row.totalAmount),
    paidAmount: num(row.paidAmount),
    unpaidAmount: num(row.unpaidAmount),
    status: (text(row.status, 'unpaid') as Statement['status']) || 'unpaid',
    statusName: text(row.statusName, '待付款'),
    billDate: text(row.billDate),
    dueDate: text(row.dueDate),
    remark: text(row.remark) || undefined
  };
}

function normalizeInvoice(row: Record<string, unknown>): Invoice {
  return {
    id: num(row.id),
    invoiceNo: text(row.invoiceNo),
    invoiceTitle: text(row.invoiceTitle),
    taxNo: text(row.taxNo),
    invoiceType: text(row.invoiceType, '增值税专用发票 13%'),
    amount: num(row.amount),
    status: (text(row.status, 'applied') as Invoice['status']) || 'applied',
    statusName: text(row.statusName, '开票中'),
    applyTime: text(row.applyTime),
    issueTime: text(row.issueTime) || undefined,
    email: text(row.email) || undefined,
    remark: text(row.remark) || undefined
  };
}

function normalizeContact(row: Record<string, unknown>): Contact {
  const flag = row.isDefault;
  return {
    id: num(row.id),
    name: text(row.name),
    dept: text(row.dept) || undefined,
    tel: text(row.tel),
    email: text(row.email) || undefined,
    isDefault: flag === true || flag === 1 || flag === 'Y'
  };
}

function normalizeSales(row: Record<string, unknown>): SalesInfo {
  return {
    name: text(row.name, CUSTOMER.salesName),
    phone: text(row.phone, CUSTOMER.salesPhone),
    region: text(row.region) || undefined,
    wechat: text(row.wechat) || undefined,
    workTime: text(row.workTime) || undefined,
    serviceTel: text(row.serviceTel) || undefined
  };
}

// ==================== 登录 ====================

export interface LoginAccount {
  accountId: number;
  clientId: number;
  username: string;
  displayName: string;
}

export interface LoginResult {
  token: string;
  expiresIn: number;
  account: LoginAccount;
}

/** 小程序账号登录：由独立后端签发令牌，后续请求自动携带 Authorization */
export async function login(username: string, password: string): Promise<LoginResult> {
  const row = await request<Record<string, unknown>>({
    url: '/api/v1/auth/login',
    method: 'POST',
    data: { username, password }
  });
  const account = (row.account || {}) as Record<string, unknown>;
  return {
    token: text(row.token),
    expiresIn: num(row.expiresIn),
    account: {
      accountId: num(account.accountId),
      clientId: num(account.clientId),
      username: text(account.username),
      displayName: text(account.displayName)
    }
  };
}

// ==================== 产品目录 ====================

/**
 * 产品目录：直接使用与 MES 一致的本地结构（17 条：成品 1 / 半成品 2 / 物料 14）
 * 说明：miniapp-api 的目录接口只返回 IRG 前缀物料，与 MES 现有 TG 结构不一致，
 *      因此目录以 MES 主数据快照为准，不再被接口覆盖。
 */
export async function loadCatalog(): Promise<Product[]> {
  return PRODUCTS;
}

// ==================== 客户档案 ====================

export async function fetchCustomerProfile(): Promise<CustomerProfile> {
  if (!USE_REMOTE_API) {
    return clone(CUSTOMER_PROFILE);
  }
  try {
    const row = await request<Record<string, unknown>>({ url: '/api/v1/customer/profile', method: 'GET' });
    const stats = (row.stats || {}) as Record<string, unknown>;
    const counts = (row.orderCounts || {}) as Record<string, unknown>;
    return {
      company: text(row.company, CUSTOMER.company),
      shortName: text(row.shortName, CUSTOMER.shortName),
      creditCode: text(row.creditCode, CUSTOMER.creditCode),
      level: text(row.level, CUSTOMER.level),
      levelDesc: text(row.levelDesc, CUSTOMER.levelDesc),
      buyer: text(row.buyer, CUSTOMER.buyer),
      region: text(row.region, CUSTOMER.region),
      salesName: text(row.salesName, CUSTOMER.salesName),
      salesPhone: text(row.salesPhone, CUSTOMER.salesPhone),
      stats: {
        consulting: num(stats.consulting, CUSTOMER.stats.consulting),
        pending: num(stats.pending, CUSTOMER.stats.pending),
        closed: num(stats.closed, CUSTOMER.stats.closed)
      },
      orderCounts: {
        quoted: num(counts.quoted),
        confirmed: num(counts.confirmed),
        producing: num(counts.producing),
        delivered: num(counts.delivered)
      },
      sales: row.sales ? normalizeSales(row.sales as Record<string, unknown>) : clone(SALES_FALLBACK)
    };
  } catch (error) {
    console.info('[Api] 客户档案接口不可用，使用本地数据', error);
    return clone(CUSTOMER_PROFILE);
  }
}

// ==================== 询价单 ====================

/** 提交询价单到独立后端 */
export async function submitInquiryRemote(payload: InquirySubmitPayload): Promise<InquiryResult> {
  return request<InquiryResult>({
    url: '/api/v1/inquiries',
    method: 'POST',
    data: payload as unknown as Record<string, unknown>
  });
}

/** 撤销询价单：服务端把状态置为 CANCELED（保留记录，不物理删除） */
export async function cancelInquiryRemote(inquiryNo: string): Promise<void> {
  await request<Record<string, unknown>>({
    url: `/api/v1/inquiries/${inquiryNo}/cancel`,
    method: 'POST'
  });
}

/** 查询询价单列表（后端返回含明细的询价单头） */
export async function fetchInquiryList(status?: string): Promise<InquiryResult[]> {
  try {
    const rows = await request<Record<string, unknown>[]>({
      url: '/api/v1/inquiries',
      method: 'GET',
      data: status ? { status } : undefined
    });
    return (rows || []).map((row) => ({
      inquiryId: num(row.inquiryId),
      inquiryNo: text(row.inquiryNo),
      status: text(row.status),
      itemCount: num(row.itemCount),
      totalQty: num(row.totalQty),
      quoteAmount: num(row.quoteAmount),
      createTime: text(row.createTime)
    }));
  } catch (error) {
    console.info('[Api] 询价单列表接口不可用', error);
    return [];
  }
}

/** 后端询价单 -> 小程序展示记录 */
function normalizeInquiryRecord(row: Record<string, unknown>): InquiryRecord {
  const lines = Array.isArray(row.lines) ? (row.lines as Record<string, unknown>[]) : [];
  const quoted = row.quoted === true || text(row.status) === 'quoted' || text(row.status) === 'confirmed';
  return {
    id: text(row.inquiryNo),
    status: quoted ? 'quoted' : 'submitted',
    items: lines.map((line) => ({
      productId: String(line.itemId ?? line.itemCode),
      name: text(line.itemName),
      code: text(line.itemCode),
      categoryName: line.itemOrProduct === 'PRODUCT' ? '成品' : '半成品',
      art: normalizeArt(line.art, line.itemOrProduct === 'PRODUCT'),
      qty: num(line.qty),
      moq: num(line.moq),
      unit: text(line.unitName, '件')
    })),
    itemCount: num(row.itemCount, lines.length),
    totalQty: num(row.totalQty),
    submitTime: text(row.createTime),
    expectedDate: text(row.expectedDate),
    address: text(row.deliveryAddress),
    invoice: text(row.invoiceType),
    remark: text(row.remark),
    quoteAmount: num(row.quoteAmount) || undefined
  };
}

/** 查询后端询价单记录（失败返回空数组，由调用方回退本地演示数据） */
export async function fetchInquiryRecords(status?: string): Promise<InquiryRecord[]> {
  try {
    const rows = await request<Record<string, unknown>[]>({
      url: '/api/v1/inquiries',
      method: 'GET',
      data: status ? { status } : undefined
    });
    if (!Array.isArray(rows)) {
      return [];
    }
    // 已撤销（CANCELED）的询价单不在小程序列表展示
    return rows
      .filter((row) => text(row.status).toLowerCase() !== 'canceled')
      .map(normalizeInquiryRecord);
  } catch (error) {
    console.info('[Api] 询价单记录接口不可用', error);
    return [];
  }
}

/** 查询单个后端询价单详情 */
export async function fetchInquiryRecord(inquiryNo: string): Promise<InquiryRecord | null> {
  try {
    const row = await request<Record<string, unknown>>({
      url: `/api/v1/inquiries/${inquiryNo}`,
      method: 'GET'
    });
    return normalizeInquiryRecord(row);
  } catch (error) {
    console.info('[Api] 询价单详情接口不可用', error);
    return null;
  }
}

// ==================== 收货地址 ====================

export async function fetchAddresses(): Promise<Address[]> {
  if (!USE_REMOTE_API) {
    return clone(ADDRESSES_FALLBACK);
  }
  try {
    const rows = await request<Record<string, unknown>[]>({ url: '/api/v1/addresses', method: 'GET' });
    if (!Array.isArray(rows)) {
      return clone(ADDRESSES_FALLBACK);
    }
    return rows.map(normalizeAddress);
  } catch (error) {
    console.info('[Api] 地址接口不可用，使用本地数据', error);
    return clone(ADDRESSES_FALLBACK);
  }
}

export async function createAddress(payload: AddressPayload): Promise<Address> {
  const row = await request<Record<string, unknown>>({
    url: '/api/v1/addresses',
    method: 'POST',
    data: payload as unknown as Record<string, unknown>
  });
  return normalizeAddress(row);
}

export async function updateAddress(id: number, payload: AddressPayload): Promise<Address> {
  const row = await request<Record<string, unknown>>({
    url: `/api/v1/addresses/${id}`,
    method: 'PUT',
    data: payload as unknown as Record<string, unknown>
  });
  return normalizeAddress(row);
}

export async function removeAddress(id: number): Promise<void> {
  await request({ url: `/api/v1/addresses/${id}`, method: 'DELETE' });
}

export async function makeDefaultAddress(id: number): Promise<Address> {
  const row = await request<Record<string, unknown>>({
    url: `/api/v1/addresses/${id}/default`,
    method: 'PUT'
  });
  return normalizeAddress(row);
}

// ==================== 订单 ====================

export async function fetchOrders(status?: OrderStatus): Promise<Order[]> {
  if (!USE_REMOTE_API) {
    const list = clone(ORDERS_FALLBACK);
    return status ? list.filter((item) => item.status === status) : list;
  }
  try {
    const rows = await request<Record<string, unknown>[]>({
      url: '/api/v1/orders',
      method: 'GET',
      data: status ? { status } : undefined
    });
    if (!Array.isArray(rows)) {
      return clone(ORDERS_FALLBACK);
    }
    return rows.map(normalizeOrder);
  } catch (error) {
    console.info('[Api] 订单接口不可用，使用本地数据', error);
    const list = clone(ORDERS_FALLBACK);
    return status ? list.filter((item) => item.status === status) : list;
  }
}

export async function fetchOrder(orderNo: string): Promise<Order | null> {
  if (!USE_REMOTE_API) {
    return clone(ORDERS_FALLBACK).find((item) => item.orderNo === orderNo) || null;
  }
  try {
    const row = await request<Record<string, unknown>>({
      url: `/api/v1/orders/${orderNo}`,
      method: 'GET'
    });
    return normalizeOrder(row);
  } catch (error) {
    console.info('[Api] 订单详情接口不可用，使用本地数据', error);
    return clone(ORDERS_FALLBACK).find((item) => item.orderNo === orderNo) || null;
  }
}

/** 询价单转正式订单 */
export async function createOrderFromInquiry(inquiryNo: string): Promise<Order> {
  const row = await request<Record<string, unknown>>({
    url: `/api/v1/orders/from-inquiry/${inquiryNo}`,
    method: 'POST'
  });
  return normalizeOrder(row);
}

// ==================== 交付 / 对账 / 发票 ====================

export async function fetchDeliveries(orderNo?: string): Promise<Delivery[]> {
  if (!USE_REMOTE_API) {
    const list = clone(DELIVERIES_FALLBACK);
    return orderNo ? list.filter((item) => item.orderNo === orderNo) : list;
  }
  try {
    const rows = await request<Record<string, unknown>[]>({
      url: '/api/v1/deliveries',
      method: 'GET',
      data: orderNo ? { orderNo } : undefined
    });
    if (!Array.isArray(rows)) {
      return clone(DELIVERIES_FALLBACK);
    }
    return rows.map(normalizeDelivery);
  } catch (error) {
    console.info('[Api] 交付接口不可用，使用本地数据', error);
    const list = clone(DELIVERIES_FALLBACK);
    return orderNo ? list.filter((item) => item.orderNo === orderNo) : list;
  }
}

export async function fetchStatements(): Promise<Statement[]> {
  if (!USE_REMOTE_API) {
    return clone(STATEMENTS_FALLBACK);
  }
  try {
    const rows = await request<Record<string, unknown>[]>({ url: '/api/v1/statements', method: 'GET' });
    if (!Array.isArray(rows)) {
      return clone(STATEMENTS_FALLBACK);
    }
    return rows.map(normalizeStatement);
  } catch (error) {
    console.info('[Api] 对账接口不可用，使用本地数据', error);
    return clone(STATEMENTS_FALLBACK);
  }
}

export async function fetchInvoices(): Promise<Invoice[]> {
  if (!USE_REMOTE_API) {
    return clone(INVOICES_FALLBACK);
  }
  try {
    const rows = await request<Record<string, unknown>[]>({ url: '/api/v1/invoices', method: 'GET' });
    if (!Array.isArray(rows)) {
      return clone(INVOICES_FALLBACK);
    }
    return rows.map(normalizeInvoice);
  } catch (error) {
    console.info('[Api] 发票接口不可用，使用本地数据', error);
    return clone(INVOICES_FALLBACK);
  }
}

export async function fetchInvoiceProfile(): Promise<InvoiceProfile> {
  if (!USE_REMOTE_API) {
    return clone(INVOICE_PROFILE_FALLBACK);
  }
  try {
    const row = await request<Record<string, unknown>>({ url: '/api/v1/invoices/profile', method: 'GET' });
    return {
      invoiceTitle: text(row.invoiceTitle, INVOICE_PROFILE_FALLBACK.invoiceTitle),
      taxNo: text(row.taxNo, INVOICE_PROFILE_FALLBACK.taxNo),
      invoiceType: text(row.invoiceType, INVOICE_PROFILE_FALLBACK.invoiceType),
      email: text(row.email) || undefined
    };
  } catch (error) {
    console.info('[Api] 开票信息接口不可用，使用本地数据', error);
    return clone(INVOICE_PROFILE_FALLBACK);
  }
}

export async function applyInvoice(payload: InvoiceApplyPayload): Promise<Invoice> {
  const row = await request<Record<string, unknown>>({
    url: '/api/v1/invoices',
    method: 'POST',
    data: payload as unknown as Record<string, unknown>
  });
  return normalizeInvoice(row);
}

// ==================== 联系人与客服 ====================

export async function fetchContacts(): Promise<Contact[]> {
  if (!USE_REMOTE_API) {
    return clone(CONTACTS_FALLBACK);
  }
  try {
    const rows = await request<Record<string, unknown>[]>({ url: '/api/v1/contacts', method: 'GET' });
    if (!Array.isArray(rows)) {
      return clone(CONTACTS_FALLBACK);
    }
    return rows.map(normalizeContact);
  } catch (error) {
    console.info('[Api] 联系人接口不可用，使用本地数据', error);
    return clone(CONTACTS_FALLBACK);
  }
}

export async function fetchSales(): Promise<SalesInfo> {
  if (!USE_REMOTE_API) {
    return clone(SALES_FALLBACK);
  }
  try {
    const row = await request<Record<string, unknown>>({ url: '/api/v1/sales', method: 'GET' });
    return normalizeSales(row);
  } catch (error) {
    console.info('[Api] 销售接口不可用，使用本地数据', error);
    return clone(SALES_FALLBACK);
  }
}

export async function fetchFaqs(): Promise<Faq[]> {
  if (!USE_REMOTE_API) {
    return clone(FAQS_FALLBACK);
  }
  try {
    const rows = await request<Record<string, unknown>[]>({ url: '/api/v1/help/faq', method: 'GET' });
    if (!Array.isArray(rows)) {
      return clone(FAQS_FALLBACK);
    }
    return rows.map((row) => ({
      id: num(row.id),
      category: text(row.category),
      question: text(row.question),
      answer: text(row.answer)
    }));
  } catch (error) {
    console.info('[Api] 帮助中心接口不可用，使用本地数据', error);
    return clone(FAQS_FALLBACK);
  }
}

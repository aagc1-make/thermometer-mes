import type { Customer, InquiryMeta } from '@/types';

/** 当前登录的企业客户（演示数据，后续由 /mobile/login 与客户接口回填） */
export const CUSTOMER: Customer = {
  company: '华瑞医疗器械有限公司',
  shortName: '华',
  creditCode: '91320594MA1X***7K',
  level: '金牌客户',
  levelDesc: '金牌客户 · 月结 30 天',
  buyer: '采购员 王工 · 苏州工业园区',
  region: '苏州工业园区',
  salesName: '张经理',
  salesPhone: '138****6621',
  stats: {
    consulting: 3,
    pending: 1,
    closed: 8
  }
};

/** 询价单默认基础信息（与设计稿一致） */
export const DEFAULT_INQUIRY_META: InquiryMeta = {
  expectedDate: '2026-11-05',
  address: '苏州工业园区星湖街 328 号',
  invoice: '增值税专用发票 13%',
  remark: '整机与预装件同批次交付'
};

/** 我的订单状态入口 */
export const ORDER_STATUS_LIST = [
  { key: 'quoted', name: '待报价', count: 1 },
  { key: 'confirmed', name: '待确认', count: 1 },
  { key: 'producing', name: '生产中', count: 2 },
  { key: 'delivered', name: '已交付', count: 0 }
] as const;

/** 常用服务入口 */
export const SERVICE_LIST = [
  { key: 'address', name: '收货地址', icon: 'pin' },
  { key: 'invoice', name: '开票信息', icon: 'doc' },
  { key: 'contact', name: '联系人', icon: 'users' },
  { key: 'delivery', name: '交付进度', icon: 'truck' },
  { key: 'statement', name: '对账单', icon: 'card' },
  { key: 'help', name: '帮助中心', icon: 'help' }
] as const;

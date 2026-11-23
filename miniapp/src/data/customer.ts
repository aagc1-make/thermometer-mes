import type { Customer, InquiryMeta } from '@/types';

/**
 * 当前登录的企业客户（离线兜底）
 * 正式数据由 miniapp-api 按登录账号从 miniapp_customer 返回，内容与 md_client 一致；
 * 这里默认演示账号 c-tg-001（华东医疗，client_id 9751）。
 */
export const CUSTOMER: Customer = {
  "company": "华东医疗器械经销有限公司",
  "shortName": "华东医疗",
  "creditCode": "91310115MA1K35XXXX",
  "level": "A 级战略客户",
  "levelDesc": "A 级战略客户 · 月结 30 天",
  "buyer": "张采购",
  "region": "上海浦东新区",
  "salesName": "李销售",
  "salesPhone": "13800000001",
  "stats": {
    "consulting": 2,
    "pending": 1,
    "closed": 6
  }
};

/** 询价单默认基础信息 */
export const DEFAULT_INQUIRY_META: InquiryMeta = {
  "expectedDate": "2026-11-07",
  "address": "上海市浦东新区 张江路 88 号 3 号楼",
  "invoice": "增值税专用发票 13%",
  "remark": ""
};

/** 我的订单状态入口 */
export const ORDER_STATUS_LIST = [
  {
    "key": "quoted",
    "name": "待报价",
    "count": 1
  },
  {
    "key": "confirmed",
    "name": "待确认",
    "count": 1
  },
  {
    "key": "producing",
    "name": "生产中",
    "count": 2
  },
  {
    "key": "delivered",
    "name": "已交付",
    "count": 2
  }
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

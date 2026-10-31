/**
 * 企业账号（与 MES 客户主数据 md_client 一致）
 * 数据来源：MES 客户表，共 3 家企业客户
 *   9751 C-TG-001 华东医疗器械经销有限公司（经销商）
 *   9752 C-TG-002 北京康泰健康科技有限公司（终端企业）
 *   9753 C-TG-003 广州社区健康服务中心（终端企业）
 */
export interface EnterpriseAccount {
  /** 登录账号 */
  username: string;
  /** 登录密码（演示环境统一 123456） */
  password: string;
  clientId: number;
  clientCode: string;
  company: string;
  shortName: string;
  typeName: '经销商' | '终端企业';
}

export const ENTERPRISE_ACCOUNTS: EnterpriseAccount[] = [
  {
    username: 'c-tg-001',
    password: '123456',
    clientId: 9751,
    clientCode: 'C-TG-001',
    company: '华东医疗器械经销有限公司',
    shortName: '华东医疗',
    typeName: '经销商'
  },
  {
    username: 'c-tg-002',
    password: '123456',
    clientId: 9752,
    clientCode: 'C-TG-002',
    company: '北京康泰健康科技有限公司',
    shortName: '北京康泰',
    typeName: '终端企业'
  },
  {
    username: 'c-tg-003',
    password: '123456',
    clientId: 9753,
    clientCode: 'C-TG-003',
    company: '广州社区健康服务中心',
    shortName: '广州社区',
    typeName: '终端企业'
  }
];

/** 按账号查找企业 */
export function findAccount(username: string): EnterpriseAccount | undefined {
  const key = username.trim().toLowerCase();
  return ENTERPRISE_ACCOUNTS.find((item) => item.username.toLowerCase() === key);
}

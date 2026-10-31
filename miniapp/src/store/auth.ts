import { create } from 'zustand';
import Taro from '@tarojs/taro';
import type { EnterpriseAccount } from '@/data/accounts';
import { findAccount } from '@/data/accounts';
import { clearToken, ensureToken } from '@/services/request';

const ACCOUNT_KEY = 'ktg_mes_account';

function readStored(): EnterpriseAccount | null {
  try {
    const raw = Taro.getStorageSync(ACCOUNT_KEY);
    if (!raw) return null;
    return typeof raw === 'string' ? (JSON.parse(raw) as EnterpriseAccount) : (raw as EnterpriseAccount);
  } catch {
    return null;
  }
}

interface AuthState {
  /** 当前登录企业，null 表示未登录 */
  account: EnterpriseAccount | null;
  /** 企业账号登录 */
  login: (username: string, password: string) => Promise<EnterpriseAccount>;
  /** 退出登录并清空本地登录态 */
  logout: () => void;
  /** 冷启动时恢复上次登录的企业 */
  restore: () => EnterpriseAccount | null;
}

export const useAuthStore = create<AuthState>()((set) => ({
  account: readStored(),

  login: async (username, password) => {
    const account = findAccount(username);
    if (!account || account.password !== password.trim()) {
      throw new Error('账号或密码错误');
    }
    try {
      // 换取接口令牌，失败时仍允许登录，页面会回落到本地数据
      await ensureToken();
    } catch (error) {
      console.info('[Auth] 接口令牌获取失败，使用本地数据', error);
    }
    Taro.setStorageSync(ACCOUNT_KEY, JSON.stringify(account));
    set({ account });
    return account;
  },

  logout: () => {
    clearToken();
    Taro.removeStorageSync(ACCOUNT_KEY);
    set({ account: null });
  },

  restore: () => {
    const account = readStored();
    set({ account });
    return account;
  }
}));

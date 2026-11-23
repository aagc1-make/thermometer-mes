import { create } from 'zustand';
import Taro from '@tarojs/taro';
import type { EnterpriseAccount } from '@/data/accounts';
import { findAccount } from '@/data/accounts';
import { clearToken, setToken } from '@/services/request';
import { login as loginRemote } from '@/services/api';

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
    if (!account) {
      throw new Error('账号或密码错误');
    }
    // 以服务端账号为准：换取令牌后所有业务接口按该企业隔离数据
    const result = await loginRemote(username.trim(), password.trim());
    setToken(result.token);
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

import { create } from 'zustand';
import Taro from '@tarojs/taro';
import { clearToken, getToken, setToken } from '@/services/request';
import { getInfo, login as loginRemote, logout as logoutRemote } from '@/services/api';
import type { MesUser } from '@/services/api';

const INFO_KEY = 'ktg_worker_info';

export const ROLE_NAMES: Record<string, string> = {
  admin: '超级管理员',
  workshop_director: '车间主任',
  warehouse: '仓库管理员',
  operator: '生产操作员',
  inspector: '质检员',
  purchaser: '采购员',
  planner: '计划员',
  craftsman: '工艺员',
  teamleader: '班组长'
};

interface AuthState {
  ready: boolean;
  user: MesUser | null;
  roles: string[];
  permissions: string[];
  restore: () => boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  hasPermi: (perm: string) => boolean;
  roleName: () => string;
}

function readCached() {
  const raw = Taro.getStorageSync(INFO_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as { user: MesUser; roles: string[]; permissions: string[] };
  } catch (error) {
    return null;
  }
}

export const useAuthStore = create<AuthState>((set, get) => ({
  ready: false,
  user: null,
  roles: [],
  permissions: [],

  restore: () => {
    const token = getToken();
    const cached = readCached();
    if (!token || !cached) {
      set({ ready: true, user: null, roles: [], permissions: [] });
      return false;
    }
    set({ ready: true, user: cached.user, roles: cached.roles || [], permissions: cached.permissions || [] });
    return true;
  },

  login: async (username, password) => {
    const result = await loginRemote(username.trim(), password.trim());
    setToken(result.token);
    const info = await getInfo();
    Taro.setStorageSync(
      INFO_KEY,
      JSON.stringify({ user: info.user, roles: info.roles || [], permissions: info.permissions || [] })
    );
    set({ ready: true, user: info.user, roles: info.roles || [], permissions: info.permissions || [] });
  },

  logout: async () => {
    try {
      await logoutRemote();
    } catch (error) {
      // 令牌已失效也允许退出
    }
    clearToken();
    Taro.removeStorageSync(INFO_KEY);
    set({ user: null, roles: [], permissions: [] });
  },

  hasPermi: (perm) => {
    const { roles, permissions } = get();
    if (roles.includes('admin')) {
      return true;
    }
    return permissions.includes(perm);
  },

  roleName: () => {
    const roles = get().roles || [];
    const key = roles.find((role) => role !== 'common');
    return key ? ROLE_NAMES[key] || key : '未分配角色';
  }
}));

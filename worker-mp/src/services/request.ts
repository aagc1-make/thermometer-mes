import Taro from '@tarojs/taro';

/**
 * MES 后端地址（与管理端同一套账号、角色、权限）
 * 本地调试默认 8080；体验版构建时注入 TARO_APP_MES_API。
 */
const DEFAULT_BASE_URL = 'http://127.0.0.1:8080';
const ENV_BASE_URL =
  typeof process !== 'undefined' && process.env ? (process.env.TARO_APP_MES_API as string) || '' : '';
export const API_BASE_URL = (ENV_BASE_URL || DEFAULT_BASE_URL).replace(/\/$/, '');

const TOKEN_KEY = 'ktg_worker_token';

export interface ApiResponse<T = unknown> {
  code: number;
  msg: string;
  data?: T;
  rows?: T;
  total?: number;
}

export interface RequestOptions {
  url: string;
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  data?: Record<string, unknown>;
  params?: Record<string, unknown>;
}

export function getToken(): string {
  return Taro.getStorageSync(TOKEN_KEY) || '';
}

export function setToken(token: string): void {
  Taro.setStorageSync(TOKEN_KEY, token);
}

export function clearToken(): void {
  Taro.removeStorageSync(TOKEN_KEY);
}

let redirecting = false;

function redirectToLogin(): void {
  if (redirecting) {
    return;
  }
  const pages = Taro.getCurrentPages();
  const current = pages.length ? pages[pages.length - 1] : null;
  if (current && current.route && current.route.indexOf('pages/login') === 0) {
    return;
  }
  redirecting = true;
  Taro.reLaunch({
    url: '/pages/login/index',
    complete: () => {
      redirecting = false;
    }
  });
}

function buildQuery(params?: Record<string, unknown>): string {
  if (!params) {
    return '';
  }
  const pairs = Object.keys(params)
    .filter((key) => params[key] !== undefined && params[key] !== null && params[key] !== '')
    .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(String(params[key]))}`);
  return pairs.length ? `?${pairs.join('&')}` : '';
}

/** 原始响应：列表接口需要 rows/total */
export async function requestRaw<T = unknown>(options: RequestOptions): Promise<ApiResponse<T>> {
  const { url, method = 'GET', data, params } = options;
  const token = getToken();
  const header: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) {
    header.Authorization = `Bearer ${token}`;
  }
  try {
    const res = await Taro.request<ApiResponse<T>>({
      url: `${API_BASE_URL}${url}${buildQuery(params)}`,
      method,
      data,
      header,
      timeout: 20000
    });
    const body = res.data;
    if (res.statusCode === 401 || body?.code === 401) {
      clearToken();
      redirectToLogin();
      throw new Error(body?.msg || '登录已过期，请重新登录');
    }
    if (!body || body.code !== 200) {
      throw new Error(body?.msg || `请求失败（${res.statusCode}）`);
    }
    return body;
  } catch (error) {
    const message = (error as Error)?.message || '网络异常，请检查后端是否启动';
    throw new Error(message);
  }
}

/** 取 data，没有 data 时取 rows */
export async function request<T = unknown>(options: RequestOptions): Promise<T> {
  const body = await requestRaw<T>(options);
  return (body.data !== undefined ? body.data : body.rows) as T;
}

/** 兼容管理端流水线工具的 request 形态 */
export async function mesRequest(options: {
  url: string;
  method?: string;
  params?: Record<string, unknown>;
}): Promise<ApiResponse<unknown[]>> {
  return requestRaw<unknown[]>({
    url: options.url,
    method: (options.method || 'GET').toUpperCase() as RequestOptions['method'],
    params: options.params
  });
}

export function showError(error: unknown): void {
  const message = (error as Error)?.message || '操作失败';
  Taro.showToast({ title: message.length > 32 ? `${message.slice(0, 32)}…` : message, icon: 'none', duration: 2600 });
}

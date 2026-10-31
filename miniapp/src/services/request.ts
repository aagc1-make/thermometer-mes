import Taro from '@tarojs/taro';

/**
 * 小程序独立后端地址（backend/miniapp-api，端口 8081，与 MES 后端 8080 完全隔离）
 * - 体验版/线上：阿里云 ECS 公网地址 http://101.37.129.105:8081
 * - 本地调试：改回 http://localhost:8081
 */
export const API_BASE_URL = 'http://101.37.129.105:8081';

const TOKEN_KEY = 'ktg_mes_token';

/** 后端统一返回结构 */
export interface ApiResponse<T> {
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
  /** 使用 application/x-www-form-urlencoded 提交（登录接口需要） */
  form?: boolean;
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

/** 演示环境用于换取接口令牌的账号（企业身份由本地账号体系维护） */
const SERVER_USER = 'demo';
const SERVER_PASS = 'demo123';

let tokenTask: Promise<string | null> | null = null;

/**
 * 静默换取接口令牌
 * 并发请求共用同一个任务，避免同时发起多次登录
 */
function fetchServerToken(): Promise<string | null> {
  if (!tokenTask) {
    tokenTask = Taro.request<ApiResponse<{ token?: string }>>({
      url: `${API_BASE_URL}/api/v1/auth/login`,
      method: 'POST',
      data: { username: SERVER_USER, password: SERVER_PASS },
      header: { 'Content-Type': 'application/json' },
      timeout: 10000
    })
      .then((res) => {
        const token = res.data?.data?.token;
        if (token) {
          setToken(token);
          return token;
        }
        return null;
      })
      .catch((error) => {
        console.info('[Api] 接口令牌获取失败', error);
        return null;
      })
      .then((token) => {
        tokenTask = null;
        return token;
      });
  }
  return tokenTask;
}

/** 启动时确保有可用令牌（已有则直接返回） */
export async function ensureToken(): Promise<string> {
  const token = getToken();
  if (token) {
    return token;
  }
  return (await fetchServerToken()) || '';
}

/** 令牌失效时跳转登录页；同一时间只跳一次，且不重复跳转 */
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

/**
 * 统一请求封装，自动携带 JWT
 * 令牌过期时先静默续期并自动重试一次，续期失败才跳登录页
 */
export async function request<T>(options: RequestOptions): Promise<T> {
  return send<T>(options, false);
}

async function send<T>(options: RequestOptions, retried: boolean): Promise<T> {
  const { url, method = 'GET', data, form } = options;
  let token = getToken();
  // 没有令牌时先静默登录，避免首个请求必然 401
  if (!token && !retried) {
    token = (await fetchServerToken()) || '';
  }
  const header: Record<string, string> = {
    'Content-Type': form ? 'application/x-www-form-urlencoded' : 'application/json'
  };
  if (token) {
    header.Authorization = `Bearer ${token}`;
  }

  console.info('[Api] 请求', method, url, data);

  try {
    const res = await Taro.request<ApiResponse<T>>({
      url: `${API_BASE_URL}${url}`,
      method,
      data,
      header,
      timeout: 10000
    });

    const body = res.data;
    if (res.statusCode === 401 || body?.code === 401) {
      if (!retried) {
        const fresh = await fetchServerToken();
        if (fresh) {
          console.info('[Api] 令牌已续期，重试请求', url);
          return send<T>(options, true);
        }
      }
      console.warn('[Api] 登录态失效', url);
      clearToken();
      redirectToLogin();
      throw new Error(body?.msg || '登录已过期，请重新登录');
    }
    if (res.statusCode >= 400) {
      console.error('[Api] 请求失败', url, res.statusCode, body);
      throw new Error(body?.msg || `请求失败（${res.statusCode}）`);
    }
    if (!body || (body.code !== 200 && body.code !== 0)) {
      console.error('[Api] 业务错误', url, body);
      throw new Error(body?.msg || '请求失败');
    }
    return (body.data !== undefined ? body.data : body.rows) as T;
  } catch (error) {
    console.error('[Api] 异常', url, error);
    throw error;
  }
}

import Taro from '@tarojs/taro';

/**
 * 小程序独立后端地址（backend/miniapp-api，端口 8081，与 MES 后端 8080 完全隔离）
 * - H5 预览可直接访问 http://localhost:8081
 * - 小程序真机：改成内网/公网可访问的地址（需在微信后台配置为合法域名）
 */
export const API_BASE_URL = 'http://localhost:8081';

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
 */
export async function request<T>(options: RequestOptions): Promise<T> {
  const { url, method = 'GET', data, form } = options;
  const token = getToken();
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
    if (res.statusCode === 401) {
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

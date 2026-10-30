import Taro from '@tarojs/taro';

/** 获取状态栏高度（px），H5 下返回 0 */
export function getStatusBarHeight(): number {
  try {
    const info = Taro.getSystemInfoSync();
    return info.statusBarHeight || 0;
  } catch (error) {
    console.error('[System] 获取系统信息失败', error);
    return 0;
  }
}

import Taro from '@tarojs/taro';

let lastUrl = '';
let lastAt = 0;

/**
 * 页面跳转（带连点保护）
 * 连续点击同一入口时只跳一次，避免重复入栈导致的页面闪烁 / 白屏
 */
export function navigateTo(url: string, gap = 400): void {
  const now = Date.now();
  if (url === lastUrl && now - lastAt < gap) {
    return;
  }
  lastUrl = url;
  lastAt = now;
  Taro.navigateTo({ url }).catch((error) => {
    console.info('[Nav] 跳转失败', url, error);
  });
}

import Taro from '@tarojs/taro';

const INQUIRY_TAB_INDEX = 2;

/** 参与 tabBar 的页面，只有这些页面才允许设置角标 */
const TAB_PAGES = ['pages/home/index', 'pages/product/index', 'pages/inquiry/index', 'pages/mine/index'];

function isTabPage(): boolean {
  const pages = Taro.getCurrentPages();
  const current = pages.length ? pages[pages.length - 1] : null;
  const route = current && current.route ? current.route : '';
  return TAB_PAGES.indexOf(route) >= 0;
}

/**
 * 同步「询价单」tabBar 角标
 * 非 tabBar 页面（如登录页、产品详情页）调用会报 not TabBar page，直接跳过；
 * 各 tab 页在 useDidShow 时会再同步一次，不会漏更新。
 * H5 预览环境不支持 tabBar 角标，同样忽略异常。
 */
export function syncInquiryBadge(count: number): void {
  if (!isTabPage()) {
    return;
  }
  try {
    if (typeof Taro.setTabBarBadge !== 'function') {
      return;
    }
    if (count > 0) {
      Taro.setTabBarBadge({
        index: INQUIRY_TAB_INDEX,
        text: count > 99 ? '99+' : String(count)
      }).catch((error) => {
        console.info('[TabBar] 设置询价单角标失败', error);
      });
    } else {
      Taro.removeTabBarBadge({ index: INQUIRY_TAB_INDEX }).catch((error) => {
        console.info('[TabBar] 移除询价单角标失败', error);
      });
    }
  } catch (error) {
    console.info('[TabBar] 角标同步异常', error);
  }
}

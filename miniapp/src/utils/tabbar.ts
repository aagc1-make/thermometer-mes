import Taro from '@tarojs/taro';

const INQUIRY_TAB_INDEX = 2;

/**
 * 同步「询价单」tabBar 角标
 * H5 预览环境不支持 tabBar 角标，忽略异常即可
 */
export function syncInquiryBadge(count: number): void {
  try {
    if (typeof Taro.setTabBarBadge !== 'function') {
      return;
    }
    if (count > 0) {
      Taro.setTabBarBadge({
        index: INQUIRY_TAB_INDEX,
        text: count > 99 ? '99+' : String(count)
      }).catch((error) => {
        console.error('[TabBar] 设置询价单角标失败', error);
      });
    } else {
      Taro.removeTabBarBadge({ index: INQUIRY_TAB_INDEX }).catch((error) => {
        console.error('[TabBar] 移除询价单角标失败', error);
      });
    }
  } catch (error) {
    console.error('[TabBar] 角标同步异常', error);
  }
}

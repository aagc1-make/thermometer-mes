import Taro, { useLaunch, useDidShow, useDidHide } from '@tarojs/taro';
import { useAuthStore } from '@/store/auth';
import { useCatalogStore } from '@/store/catalog';
import { ensureToken } from '@/services/request';
// 全局样式
import './app.scss';

function App(props) {
  // 启动即校验登录态并预热产品目录，避免首屏空转和二次跳转造成的白屏
  useLaunch(() => {
    const account = useAuthStore.getState().restore();
    useCatalogStore.getState().load();
    if (!account) {
      Taro.reLaunch({ url: '/pages/login/index' });
      return;
    }
    // 已登录则确保接口令牌可用，避免首屏请求 401
    ensureToken();
  });

  // 对应 onShow
  useDidShow(() => {});

  // 对应 onHide
  useDidHide(() => {});

  return props.children;
}

export default App;

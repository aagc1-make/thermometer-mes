import Taro, { useLaunch, useDidShow, useDidHide } from '@tarojs/taro';
import { useAuthStore } from '@/store/auth';
import { useCatalogStore } from '@/store/catalog';
// 全局样式
import './app.scss';

function App(props) {
  // 启动即恢复登录态并预热产品目录（目录为公开接口，无需令牌）
  useLaunch(() => {
    const account = useAuthStore.getState().restore();
    useCatalogStore.getState().load();
    if (!account) {
      Taro.reLaunch({ url: '/pages/login/index' });
    }
  });

  // 对应 onShow
  useDidShow(() => {});

  // 对应 onHide
  useDidHide(() => {});

  return props.children;
}

export default App;

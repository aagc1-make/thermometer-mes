import Taro, { useLaunch } from '@tarojs/taro';
import { useAuthStore } from '@/store/auth';
import './app.scss';

function App(props) {
  useLaunch(() => {
    const restored = useAuthStore.getState().restore();
    if (!restored) {
      Taro.reLaunch({ url: '/pages/login/index' });
    }
  });

  return props.children;
}

export default App;

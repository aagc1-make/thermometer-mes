import React, { useState } from 'react';
import { View, Text, Input, Button } from '@tarojs/components';
import Taro from '@tarojs/taro';
import { useAuthStore } from '@/store/auth';
import { showError } from '@/services/request';
import styles from './index.module.scss';

const DEMO_ACCOUNTS = [
  { user: 'workshop', role: '车间主任' },
  { user: 'warehouse', role: '仓库管理员' },
  { user: 'operator', role: '生产操作员' },
  { user: 'inspector', role: '质检员' },
  { user: 'purchaser', role: '采购员' }
];

const LoginPage: React.FC = () => {
  const [username, setUsername] = useState('operator');
  const [password, setPassword] = useState('admin123');
  const [loading, setLoading] = useState(false);
  const login = useAuthStore((state) => state.login);

  const handleLogin = async () => {
    if (!username.trim() || !password.trim()) {
      Taro.showToast({ title: '请输入账号和密码', icon: 'none' });
      return;
    }
    setLoading(true);
    try {
      await login(username, password);
      Taro.showToast({ title: '登录成功', icon: 'success' });
      Taro.reLaunch({ url: '/pages/home/index' });
    } catch (error) {
      showError(error);
    } finally {
      setLoading(false);
    }
  };

  const pickAccount = (user: string) => {
    setUsername(user);
    setPassword('admin123');
  };

  return (
    <View className={styles.page}>
      <View className={styles.hero}>
        <Text className={styles.badge}>测温 MES</Text>
        <Text className={styles.title}>车间工作台</Text>
        <Text className={styles.subtitle}>工单 · 报工 · 检验 · 入库，一个入口完成</Text>
      </View>

      <View className={styles.card}>
        <View className={styles.field}>
          <Text className={styles.label}>账号</Text>
          <Input
            className={styles.input}
            value={username}
            placeholder="请输入 MES 账号"
            onInput={(event) => setUsername(event.detail.value)}
          />
        </View>
        <View className={styles.field}>
          <Text className={styles.label}>密码</Text>
          <Input
            className={styles.input}
            password
            value={password}
            placeholder="请输入密码"
            onInput={(event) => setPassword(event.detail.value)}
          />
        </View>
        <Button className={styles.submit} loading={loading} onClick={handleLogin}>
          登录
        </Button>
        <Text className={styles.tip}>账号沿用 MES 用户与角色，权限由管理员分配</Text>
      </View>

      <View className={styles.demo}>
        <Text className={styles.demoTitle}>演示账号（密码 admin123）</Text>
        <View className={styles.demoList}>
          {DEMO_ACCOUNTS.map((item) => (
            <View key={item.user} className={styles.demoItem} onClick={() => pickAccount(item.user)}>
              <Text className={styles.demoUser}>{item.user}</Text>
              <Text className={styles.demoRole}>{item.role}</Text>
            </View>
          ))}
        </View>
      </View>
    </View>
  );
};

export default LoginPage;

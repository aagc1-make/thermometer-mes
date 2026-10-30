import React, { useState } from 'react';
import { Input, Text, View } from '@tarojs/components';
import Taro from '@tarojs/taro';
import PageHeader from '@/components/PageHeader';
import { login } from '@/services/api';
import { setToken } from '@/services/request';
import styles from './index.module.scss';

const LoginPage: React.FC = () => {
  const [username, setUsername] = useState('demo');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const submit = async () => {
    if (!username.trim() || !password.trim()) {
      Taro.showToast({ title: '请输入账号和密码', icon: 'none' });
      return;
    }
    setSubmitting(true);
    try {
      const result = await login(username.trim(), password.trim());
      setToken(result.token);
      Taro.showToast({ title: '登录成功', icon: 'success' });
      setTimeout(() => {
        Taro.switchTab({ url: '/pages/mine/index' }).catch(() => {
          Taro.reLaunch({ url: '/pages/home/index' });
        });
      }, 300);
    } catch (error) {
      const message = error instanceof Error ? error.message : '登录失败';
      Taro.showToast({ title: message, icon: 'none' });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <View className={styles.page}>
      <PageHeader title="企业账号登录" showBack />
      <View className={styles.brand}>
        <Text className={styles.brandTitle}>红外测温枪 MES</Text>
        <Text className={styles.brandSub}>企业订购小程序 · 客户专属入口</Text>
      </View>

      <View className={styles.card}>
        <View className={styles.field}>
          <Text className={styles.label}>登录账号</Text>
          <Input
            className={styles.input}
            value={username}
            placeholder="请输入账号"
            onInput={(event) => setUsername(event.detail.value)}
          />
        </View>
        <View className={styles.divider} />
        <View className={styles.field}>
          <Text className={styles.label}>登录密码</Text>
          <Input
            className={styles.input}
            password
            value={password}
            placeholder="请输入密码"
            onInput={(event) => setPassword(event.detail.value)}
          />
        </View>
      </View>

      <View className={styles.loginBtn} onClick={submit}>
        <Text className={styles.loginBtnText}>{submitting ? '登录中...' : '登 录'}</Text>
      </View>
      <Text className={styles.hint}>演示账号：demo / demo123</Text>
    </View>
  );
};

export default LoginPage;

import React, { useEffect, useState } from 'react';
import { Input, Text, View } from '@tarojs/components';
import Taro from '@tarojs/taro';
import classnames from 'classnames';
import PageHeader from '@/components/PageHeader';
import { ENTERPRISE_ACCOUNTS } from '@/data/accounts';
import { useAuthStore } from '@/store/auth';
import styles from './index.module.scss';

const LoginPage: React.FC = () => {
  const account = useAuthStore((state) => state.account);
  const login = useAuthStore((state) => state.login);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (account) {
      Taro.reLaunch({ url: '/pages/home/index' });
    }
  }, [account]);

  const pickAccount = (name: string, pwd: string) => {
    setUsername(name);
    setPassword(pwd);
  };

  const submit = async () => {
    if (!username.trim() || !password.trim()) {
      Taro.showToast({ title: '请输入账号和密码', icon: 'none' });
      return;
    }
    setSubmitting(true);
    try {
      await login(username, password);
      Taro.showToast({ title: '登录成功', icon: 'success' });
      setTimeout(() => {
        Taro.reLaunch({ url: '/pages/home/index' });
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
      <PageHeader title="企业账号登录" />

      <View className={styles.brand}>
        <Text className={styles.brandTitle}>红外测温枪 MES</Text>
        <Text className={styles.brandSub}>企业订购小程序 · 客户专属入口</Text>
      </View>

      <View className={styles.sectionTitle}>
        <Text className={styles.sectionTitleText}>选择企业账号</Text>
      </View>

      <View className={styles.accountList}>
        {ENTERPRISE_ACCOUNTS.map((item) => (
          <View
            key={item.username}
            className={classnames(
              styles.accountItem,
              username.toLowerCase() === item.username && styles.accountItemActive
            )}
            onClick={() => pickAccount(item.username, item.password)}
          >
            <View className={styles.accountMain}>
              <Text className={styles.accountCompany}>{item.company}</Text>
              <Text className={styles.accountMeta}>
                {item.clientCode} · {item.typeName}
              </Text>
            </View>
            <Text className={styles.accountUser}>{item.username}</Text>
          </View>
        ))}
      </View>

      <View className={styles.card}>
        <View className={styles.field}>
          <Text className={styles.label}>登录账号</Text>
          <Input
            className={styles.input}
            value={username}
            placeholder="请输入企业账号"
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
      <Text className={styles.hint}>演示密码统一为 123456</Text>
    </View>
  );
};

export default LoginPage;

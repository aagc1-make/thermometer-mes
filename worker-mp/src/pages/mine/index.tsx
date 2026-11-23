import React from 'react';
import { View, Text, Button } from '@tarojs/components';
import Taro from '@tarojs/taro';
import { useAuthStore } from '@/store/auth';
import { API_BASE_URL } from '@/services/request';
import styles from './index.module.scss';

const PERMISSION_TIPS: { perm: string; name: string }[] = [
  { perm: 'mes:pro:workorder:edit', name: '工单确认 / 完工' },
  { perm: 'mes:pro:protask:edit', name: '生产排产' },
  { perm: 'mes:pro:feedback:add', name: '生产报工' },
  { perm: 'mes:pro:feedback:approve', name: '报工审核' },
  { perm: 'mes:qc:pending:list', name: '过程检验' },
  { perm: 'mes:wm:issueheader:edit', name: '生产领料' },
  { perm: 'mes:wm:productrecpt:edit', name: '产品入库 / 上架' },
  { perm: 'mes:wm:itemrecpt:edit', name: '采购收货' }
];

const MinePage: React.FC = () => {
  const user = useAuthStore((state) => state.user);
  const roles = useAuthStore((state) => state.roles);
  const permissions = useAuthStore((state) => state.permissions);
  const roleName = useAuthStore((state) => state.roleName);
  const hasPermi = useAuthStore((state) => state.hasPermi);
  const logout = useAuthStore((state) => state.logout);

  const handleLogout = async () => {
    const result = await Taro.showModal({ title: '退出登录', content: '确定退出当前账号？' });
    if (!result.confirm) {
      return;
    }
    await logout();
    Taro.reLaunch({ url: '/pages/login/index' });
  };

  return (
    <View className={styles.page}>
      <View className={styles.profile}>
        <View className={styles.avatar}>
          <Text>{user?.nickName?.slice(0, 1) || '工'}</Text>
        </View>
        <View className={styles.profileMain}>
          <Text className={styles.name}>{user?.nickName || user?.userName}</Text>
          <Text className={styles.role}>{roleName()}</Text>
          <Text className={styles.dept}>{user?.dept?.deptName || user?.deptName || '生产中心'}</Text>
        </View>
      </View>

      <View className={styles.section}>
        <Text className={styles.sectionTitle}>我的权限</Text>
        <View className={styles.permList}>
          {PERMISSION_TIPS.filter((item) => hasPermi(item.perm)).map((item) => (
            <View key={item.perm} className={styles.perm}>
              {item.name}
            </View>
          ))}
          {PERMISSION_TIPS.filter((item) => hasPermi(item.perm)).length === 0 && (
            <Text className={styles.muted}>未分配操作权限，请联系管理员</Text>
          )}
        </View>
        <Text className={styles.permCount}>
          角色：{roles.join('、') || '-'} · 权限项 {permissions.length}
        </Text>
      </View>

      <View className={styles.section}>
        <Text className={styles.sectionTitle}>系统信息</Text>
        <View className={styles.row}>
          <Text>账号</Text>
          <Text className={styles.rowValue}>{user?.userName}</Text>
        </View>
        <View className={styles.row}>
          <Text>后端地址</Text>
          <Text className={styles.rowValue}>{API_BASE_URL}</Text>
        </View>
        <View className={styles.row}>
          <Text>客户端</Text>
          <Text className={styles.rowValue}>工人端 mini v1.0.0</Text>
        </View>
      </View>

      <Button className={styles.logout} onClick={handleLogout}>
        退出登录
      </Button>
    </View>
  );
};

export default MinePage;

import React, { useCallback, useEffect, useState } from 'react';
import { View, Text } from '@tarojs/components';
import Taro, { useDidShow } from '@tarojs/taro';
import { useAuthStore } from '@/store/auth';
import { loadTodoSummary } from '@/services/api';
import { showError } from '@/services/request';
import { WORKORDER_STATUS, statusText } from '@/utils/format';
import styles from './index.module.scss';

interface Summary {
  pendingInspect: number;
  receipts: number;
  issues: number;
  workorders: number;
  recent: { workorderId: number; workorderCode: string; productName?: string; status?: string }[];
}

const EMPTY: Summary = { pendingInspect: 0, receipts: 0, issues: 0, workorders: 0, recent: [] };

const HomePage: React.FC = () => {
  const user = useAuthStore((state) => state.user);
  const roleName = useAuthStore((state) => state.roleName);
  const hasPermi = useAuthStore((state) => state.hasPermi);
  const [summary, setSummary] = useState<Summary>(EMPTY);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    if (!useAuthStore.getState().user) {
      Taro.reLaunch({ url: '/pages/login/index' });
      return;
    }
    setLoading(true);
    try {
      const data = await loadTodoSummary();
      setSummary({
        pendingInspect: data.pendingInspect.length,
        receipts: data.receipts.length,
        issues: data.issues.length,
        workorders: data.workorders.length,
        recent: data.workorders.slice(0, 3).map((item) => ({
          workorderId: item.workorderId,
          workorderCode: item.workorderCode,
          productName: item.productName,
          status: item.status
        }))
      });
    } catch (error) {
      showError(error);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  useDidShow(() => {
    load();
  });

  const openWorkorder = (workorderId: number) => {
    Taro.navigateTo({ url: `/pages/workorder-detail/index?id=${workorderId}` });
  };

  const scan = async () => {
    try {
      const result = await Taro.scanCode({});
      const code = (result.result || '').trim();
      if (!code) {
        return;
      }
      Taro.navigateTo({ url: `/pages/workorder/index?code=${encodeURIComponent(code)}` });
    } catch (error) {
      // 用户取消扫码不提示
    }
  };

  const goTab = (path: string) => Taro.switchTab({ url: path });

  const quickActions = [
    { key: 'scan', name: '扫码工单', desc: '对准工单二维码', icon: '⌗', show: true, action: scan },
    {
      key: 'feedback',
      name: '生产报工',
      desc: '按工序报数量',
      icon: '⛏',
      show: hasPermi('mes:pro:feedback:add'),
      action: () => goTab('/pages/workorder/index')
    },
    {
      key: 'qc',
      name: '过程检验',
      desc: '待检与判定',
      icon: '✓',
      show: hasPermi('mes:qc:pending:list'),
      action: () => Taro.navigateTo({ url: '/pages/qc/index' })
    },
    {
      key: 'receipt',
      name: '产品入库',
      desc: '上架与执行入库',
      icon: '⇥',
      show: hasPermi('mes:wm:productrecpt:edit'),
      action: () => Taro.navigateTo({ url: '/pages/receipt/index' })
    },
    {
      key: 'issue',
      name: '生产领料',
      desc: '拣货与领出',
      icon: '⇤',
      show: hasPermi('mes:wm:issueheader:edit'),
      action: () => Taro.navigateTo({ url: '/pages/issue/index' })
    },
    {
      key: 'todo',
      name: '我的待办',
      desc: '按状态汇总',
      icon: '☰',
      show: true,
      action: () => goTab('/pages/todo/index')
    }
  ];

  return (
    <View className={styles.page}>
      <View className={styles.hero}>
        <View className={styles.heroTop}>
          <View>
            <Text className={styles.hello}>你好，{user?.nickName || user?.userName || '同事'}</Text>
            <Text className={styles.role}>{roleName()} · {user?.dept?.deptName || user?.deptName || '生产中心'}</Text>
          </View>
          <View className={styles.scanBtn} onClick={scan}>
            <Text className={styles.scanIcon}>⌗</Text>
            <Text className={styles.scanText}>扫码</Text>
          </View>
        </View>

        <View className={styles.kpis}>
          <View className={styles.kpi} onClick={() => Taro.navigateTo({ url: '/pages/qc/index' })}>
            <Text className={styles.kpiValue}>{summary.pendingInspect}</Text>
            <Text className={styles.kpiLabel}>待检验</Text>
          </View>
          <View className={styles.kpi} onClick={() => Taro.navigateTo({ url: '/pages/receipt/index' })}>
            <Text className={styles.kpiValue}>{summary.receipts}</Text>
            <Text className={styles.kpiLabel}>待入库</Text>
          </View>
          <View className={styles.kpi} onClick={() => Taro.navigateTo({ url: '/pages/issue/index' })}>
            <Text className={styles.kpiValue}>{summary.issues}</Text>
            <Text className={styles.kpiLabel}>待领料</Text>
          </View>
          <View className={styles.kpi} onClick={() => goTab('/pages/workorder/index')}>
            <Text className={styles.kpiValue}>{summary.workorders}</Text>
            <Text className={styles.kpiLabel}>在制工单</Text>
          </View>
        </View>
      </View>

      <View className={styles.section}>
        <View className={styles.sectionHead}>
          <Text className={styles.sectionTitle}>快捷操作</Text>
          <Text className={styles.sectionSub}>按角色显示可用动作</Text>
        </View>
        <View className={styles.grid}>
          {quickActions
            .filter((item) => item.show)
            .map((item) => (
              <View key={item.key} className={styles.quick} onClick={item.action}>
                <Text className={styles.quickIcon}>{item.icon}</Text>
                <Text className={styles.quickName}>{item.name}</Text>
                <Text className={styles.quickDesc}>{item.desc}</Text>
              </View>
            ))}
        </View>
      </View>

      <View className={styles.section}>
        <View className={styles.sectionHead}>
          <Text className={styles.sectionTitle}>在制工单</Text>
          <Text className={styles.sectionMore} onClick={() => goTab('/pages/workorder/index')}>
            查看全部 ›
          </Text>
        </View>
        {summary.recent.length === 0 ? (
          <View className={styles.empty}>{loading ? '加载中…' : '暂无已确认工单'}</View>
        ) : (
          summary.recent.map((item) => (
            <View key={item.workorderId} className={styles.woItem} onClick={() => openWorkorder(item.workorderId)}>
              <View className={styles.woMain}>
                <Text className={styles.woCode}>{item.workorderCode}</Text>
                <Text className={styles.woProduct}>{item.productName || '-'}</Text>
              </View>
              <View className={styles.woRight}>
                <Text className={styles.woStatus}>{statusText(WORKORDER_STATUS, item.status)}</Text>
                <Text className={styles.woArrow}>›</Text>
              </View>
            </View>
          ))
        )}
      </View>
    </View>
  );
};

export default HomePage;

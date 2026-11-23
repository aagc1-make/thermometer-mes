import React, { useCallback, useEffect, useState } from 'react';
import { View, Text } from '@tarojs/components';
import Taro, { useDidShow } from '@tarojs/taro';
import { loadTodoSummary } from '@/services/api';
import type { IssueHeader, PendingInspect, ProductRecpt, Workorder } from '@/services/api';
import { showError } from '@/services/request';
import { ISSUE_STATUS, RECPT_STATUS, WORKORDER_STATUS, statusText, num } from '@/utils/format';
import styles from './index.module.scss';

const TodoPage: React.FC = () => {
  const [pending, setPending] = useState<PendingInspect[]>([]);
  const [receipts, setReceipts] = useState<ProductRecpt[]>([]);
  const [issues, setIssues] = useState<IssueHeader[]>([]);
  const [workorders, setWorkorders] = useState<Workorder[]>([]);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await loadTodoSummary();
      setPending(data.pendingInspect);
      setReceipts(data.receipts);
      setIssues(data.issues);
      setWorkorders(data.workorders);
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

  const total = pending.length + receipts.length + issues.length + workorders.length;

  return (
    <View className={styles.page}>
      <View className={styles.summary}>
        <Text className={styles.summaryValue}>{total}</Text>
        <Text className={styles.summaryLabel}>待处理事项</Text>
      </View>

      <View className={styles.group}>
        <View className={styles.groupHead}>
          <Text className={styles.groupTitle}>待检验</Text>
          <Text className={styles.groupCount}>{pending.length}</Text>
        </View>
        {pending.length === 0 && <Text className={styles.none}>暂无待检报工</Text>}
        {pending.slice(0, 5).map((row, index) => (
          <View
            key={`${row.sourceDocId}-${index}`}
            className={styles.item}
            onClick={() => Taro.navigateTo({ url: '/pages/qc/index' })}
          >
            <View className={styles.itemMain}>
              <Text className={styles.itemCode}>{row.sourceDocCode}</Text>
              <Text className={styles.itemDesc}>{row.itemName} · {row.taskName || row.taskCode}</Text>
            </View>
            <Text className={styles.itemMeta}>待检 {num(row.quantityCheck)}</Text>
          </View>
        ))}
      </View>

      <View className={styles.group}>
        <View className={styles.groupHead}>
          <Text className={styles.groupTitle}>待入库</Text>
          <Text className={styles.groupCount}>{receipts.length}</Text>
        </View>
        {receipts.length === 0 && <Text className={styles.none}>暂无待处理入库单</Text>}
        {receipts.slice(0, 5).map((row) => (
          <View
            key={row.recptId}
            className={styles.item}
            onClick={() => Taro.navigateTo({ url: '/pages/receipt/index' })}
          >
            <View className={styles.itemMain}>
              <Text className={styles.itemCode}>{row.recptCode}</Text>
              <Text className={styles.itemDesc}>{row.workorderCode || '无工单'} · {row.itemName}</Text>
            </View>
            <Text className={styles.itemMeta}>{statusText(RECPT_STATUS, row.status)}</Text>
          </View>
        ))}
      </View>

      <View className={styles.group}>
        <View className={styles.groupHead}>
          <Text className={styles.groupTitle}>待领料</Text>
          <Text className={styles.groupCount}>{issues.length}</Text>
        </View>
        {issues.length === 0 && <Text className={styles.none}>暂无待处理领料单</Text>}
        {issues.slice(0, 5).map((row) => (
          <View
            key={row.issueId}
            className={styles.item}
            onClick={() => Taro.navigateTo({ url: '/pages/issue/index' })}
          >
            <View className={styles.itemMain}>
              <Text className={styles.itemCode}>{row.issueCode}</Text>
              <Text className={styles.itemDesc}>{row.workorderCode || '无工单'}</Text>
            </View>
            <Text className={styles.itemMeta}>{statusText(ISSUE_STATUS, row.status)}</Text>
          </View>
        ))}
      </View>

      <View className={styles.group}>
        <View className={styles.groupHead}>
          <Text className={styles.groupTitle}>在制工单</Text>
          <Text className={styles.groupCount}>{workorders.length}</Text>
        </View>
        {workorders.length === 0 && <Text className={styles.none}>{loading ? '加载中…' : '暂无已确认工单'}</Text>}
        {workorders.map((row) => (
          <View
            key={row.workorderId}
            className={styles.item}
            onClick={() => Taro.navigateTo({ url: `/pages/workorder-detail/index?id=${row.workorderId}` })}
          >
            <View className={styles.itemMain}>
              <Text className={styles.itemCode}>{row.workorderCode}</Text>
              <Text className={styles.itemDesc}>{row.productName}</Text>
            </View>
            <Text className={styles.itemMeta}>{statusText(WORKORDER_STATUS, row.status)}</Text>
          </View>
        ))}
      </View>
    </View>
  );
};

export default TodoPage;

import React, { useCallback, useEffect, useState } from 'react';
import { View, Text, Button } from '@tarojs/components';
import Taro, { useRouter } from '@tarojs/taro';
import { autoPickIssue, executeIssue, listIssues } from '@/services/api';
import type { IssueHeader } from '@/services/api';
import { showError } from '@/services/request';
import { ISSUE_STATUS, statusText, dateOnly } from '@/utils/format';
import styles from '../receipt/index.module.scss';

const IssuePage: React.FC = () => {
  const router = useRouter();
  const workorderId = Number(router.params.workorderId || 0);
  const [rows, setRows] = useState<IssueHeader[]>([]);
  const [loading, setLoading] = useState(false);
  const [busyId, setBusyId] = useState<number | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [approving, approved] = await Promise.all([
        listIssues({ status: 'APPROVING', workorderId: workorderId || undefined }),
        listIssues({ status: 'APPROVED', workorderId: workorderId || undefined })
      ]);
      setRows([...approving, ...approved]);
    } catch (error) {
      showError(error);
    } finally {
      setLoading(false);
    }
  }, [workorderId]);

  useEffect(() => {
    load();
  }, [load]);

  const pick = async (row: IssueHeader) => {
    const result = await Taro.showModal({
      title: '自动拣货',
      content: `领料单 ${row.issueCode} 将按库存自动匹配批次与库位，拣齐后转为待领出。是否继续？`
    });
    if (!result.confirm) {
      return;
    }
    setBusyId(row.issueId);
    try {
      await autoPickIssue(row.issueId);
      Taro.showToast({ title: `${row.issueCode} 已完成拣货`, icon: 'none' });
      load();
    } catch (error) {
      showError(error);
    } finally {
      setBusyId(null);
    }
  };

  const execute = async (row: IssueHeader) => {
    const result = await Taro.showModal({
      title: '执行领出',
      content: `执行领出后，领料单 ${row.issueCode} 的物料将从仓库扣减。是否继续？`
    });
    if (!result.confirm) {
      return;
    }
    setBusyId(row.issueId);
    try {
      await executeIssue(row.issueId);
      Taro.showToast({ title: `${row.issueCode} 已领出`, icon: 'none' });
      load();
    } catch (error) {
      showError(error);
    } finally {
      setBusyId(null);
    }
  };

  return (
    <View className={styles.page}>
      <View className={styles.tip}>共 {rows.length} 张待处理领料单（待拣货 + 已拣货待领出）</View>

      {rows.length === 0 && !loading && <View className={styles.empty}>暂无待处理领料单</View>}

      {rows.map((row) => (
        <View key={row.issueId} className={styles.card}>
          <View className={styles.cardHead}>
            <Text className={styles.code}>{row.issueCode}</Text>
            <Text className={`${styles.status} ${row.status === 'APPROVED' ? styles.statusReady : ''}`}>
              {statusText(ISSUE_STATUS, row.status)}
            </Text>
          </View>
          <Text className={styles.item}>{row.workorderCode || '无工单'}</Text>
          <View className={styles.meta}>
            <Text>领料日期 {dateOnly(row.issueDate)}</Text>
            <Text>{row.status === 'APPROVING' ? '待拣货' : '已拣货'}</Text>
          </View>
          {row.status === 'APPROVING' && (
            <Button className={styles.action} loading={busyId === row.issueId} onClick={() => pick(row)}>
              自动拣货
            </Button>
          )}
          {row.status === 'APPROVED' && (
            <Button className={styles.actionPrimary} loading={busyId === row.issueId} onClick={() => execute(row)}>
              执行领出
            </Button>
          )}
        </View>
      ))}
    </View>
  );
};

export default IssuePage;

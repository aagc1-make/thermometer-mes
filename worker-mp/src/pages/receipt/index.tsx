import React, { useCallback, useEffect, useState } from 'react';
import { View, Text, Button } from '@tarojs/components';
import Taro, { useRouter } from '@tarojs/taro';
import { confirmStocking, executeProductRecpt, listProductRecpts } from '@/services/api';
import type { ProductRecpt } from '@/services/api';
import { showError } from '@/services/request';
import { RECPT_STATUS, statusText, num, dateOnly } from '@/utils/format';
import styles from './index.module.scss';

const ReceiptPage: React.FC = () => {
  const router = useRouter();
  const workorderId = Number(router.params.workorderId || 0);
  const [rows, setRows] = useState<ProductRecpt[]>([]);
  const [loading, setLoading] = useState(false);
  const [busyId, setBusyId] = useState<number | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [approving, approved] = await Promise.all([
        listProductRecpts({ status: 'APPROVING', workorderId: workorderId || undefined }),
        listProductRecpts({ status: 'APPROVED', workorderId: workorderId || undefined })
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

  const stock = async (row: ProductRecpt) => {
    const result = await Taro.showModal({
      title: '确认上架',
      content: `入库单 ${row.recptCode} 确认上架后进入已上架状态，库位需已分配。是否继续？`
    });
    if (!result.confirm) {
      return;
    }
    setBusyId(row.recptId);
    try {
      await confirmStocking(row.recptId);
      Taro.showToast({ title: `${row.recptCode} 已上架`, icon: 'none' });
      load();
    } catch (error) {
      showError(error);
    } finally {
      setBusyId(null);
    }
  };

  const execute = async (row: ProductRecpt) => {
    const result = await Taro.showModal({
      title: '执行入库',
      content: `执行入库 ${row.recptCode} 后成品库存立即增加，且不可撤销。是否继续？`
    });
    if (!result.confirm) {
      return;
    }
    setBusyId(row.recptId);
    try {
      await executeProductRecpt(row.recptId);
      Taro.showToast({ title: `${row.recptCode} 已入库`, icon: 'none' });
      load();
    } catch (error) {
      showError(error);
    } finally {
      setBusyId(null);
    }
  };

  return (
    <View className={styles.page}>
      <View className={styles.tip}>共 {rows.length} 张待处理入库单（待上架 + 已上架待入库）</View>

      {rows.length === 0 && !loading && <View className={styles.empty}>暂无待处理入库单</View>}

      {rows.map((row) => (
        <View key={row.recptId} className={styles.card}>
          <View className={styles.cardHead}>
            <Text className={styles.code}>{row.recptCode}</Text>
            <Text className={`${styles.status} ${row.status === 'APPROVED' ? styles.statusReady : ''}`}>
              {statusText(RECPT_STATUS, row.status)}
            </Text>
          </View>
          <Text className={styles.item}>{row.itemName || '-'}</Text>
          <View className={styles.meta}>
            <Text>{row.workorderCode || '无工单'} · 数量 {num(row.quantity)}</Text>
            <Text>{dateOnly(row.recptDate)}</Text>
          </View>
          {row.status === 'APPROVING' && (
            <Button className={styles.action} loading={busyId === row.recptId} onClick={() => stock(row)}>
              确认上架
            </Button>
          )}
          {row.status === 'APPROVED' && (
            <Button className={styles.actionPrimary} loading={busyId === row.recptId} onClick={() => execute(row)}>
              执行入库
            </Button>
          )}
        </View>
      ))}
    </View>
  );
};

export default ReceiptPage;

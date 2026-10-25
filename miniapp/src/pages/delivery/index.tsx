import React, { useEffect, useMemo, useState } from 'react';
import { Text, View } from '@tarojs/components';
import Taro, { useRouter } from '@tarojs/taro';
import classnames from 'classnames';
import PageHeader from '@/components/PageHeader';
import Icon from '@/components/Icon';
import { fetchDeliveries } from '@/services/api';
import type { Delivery } from '@/types';
import styles from './index.module.scss';

const DeliveryPage: React.FC = () => {
  const router = useRouter();
  const orderNo = router.params.orderNo || '';
  const [deliveries, setDeliveries] = useState<Delivery[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      const list = await fetchDeliveries(orderNo || undefined);
      setDeliveries(list);
      setLoading(false);
    };
    load();
  }, [orderNo]);

  const summary = useMemo(() => {
    const producing = deliveries.filter((item) => item.status === 'producing' || item.status === 'waiting').length;
    const shipped = deliveries.filter((item) => item.status === 'shipped').length;
    const delivered = deliveries.filter((item) => item.status === 'delivered').length;
    return { producing, shipped, delivered };
  }, [deliveries]);

  const copyTrack = (value?: string) => {
    if (!value) {
      return;
    }
    Taro.setClipboardData({ data: value });
    Taro.showToast({ title: '运单号已复制', icon: 'none' });
  };

  return (
    <View className={styles.page}>
      <PageHeader title={orderNo ? '订单交付进度' : '交付进度'} showBack />

      <View className={styles.summary}>
        <View className={styles.summaryItem}>
          <Text className={styles.summaryValue}>{summary.producing}</Text>
          <Text className={styles.summaryLabel}>生产中</Text>
        </View>
        <View className={styles.summaryDivider} />
        <View className={styles.summaryItem}>
          <Text className={styles.summaryValue}>{summary.shipped}</Text>
          <Text className={styles.summaryLabel}>已发货</Text>
        </View>
        <View className={styles.summaryDivider} />
        <View className={styles.summaryItem}>
          <Text className={styles.summaryValue}>{summary.delivered}</Text>
          <Text className={styles.summaryLabel}>已签收</Text>
        </View>
      </View>

      {orderNo && (
        <View className={styles.filterBar}>
          <Icon name="doc" size={24} color="#2b6beb" />
          <Text className={styles.filterText}>当前仅显示订单 {orderNo} 的交付记录</Text>
          <View className={styles.filterReset} onClick={() => Taro.navigateBack()}>
            <Text className={styles.filterResetText}>查看全部</Text>
            <Icon name="chevronRight" size={18} color="#5a6b87" />
          </View>
        </View>
      )}

      {loading && (
        <View className={styles.empty}>
          <Text className={styles.emptyText}>加载中...</Text>
        </View>
      )}

      {!loading &&
        deliveries.map((item) => (
          <View key={item.deliveryNo} className={styles.card}>
            <View className={styles.head}>
              <View>
                <Text className={styles.deliveryNo}>{item.deliveryNo}</Text>
                <Text className={styles.orderNo}>订单 {item.orderNo}</Text>
              </View>
              <Text className={classnames(styles.status, styles[`status_${item.status}`])}>
                {item.statusName}
              </Text>
            </View>

            <View className={styles.progressWrap}>
              <View className={styles.progressBar}>
                <View
                  className={classnames(styles.progressFill, item.status === 'delivered' && styles.progressDone)}
                  style={{ width: `${Math.min(100, Math.max(0, item.progress))}%` }}
                />
              </View>
              <View className={styles.progressMeta}>
                <Text className={styles.progressText}>生产进度 {item.progress}%</Text>
                <Text className={styles.progressText}>
                  已发货 {item.shippedQty} / {item.totalQty} 件
                </Text>
              </View>
            </View>

            <View className={styles.dashed} />

            <View className={styles.row}>
              <Text className={styles.rowLabel}>计划交付</Text>
              <Text className={styles.rowValue}>{item.planDate || '待排产'}</Text>
            </View>
            {item.shipDate && (
              <View className={styles.row}>
                <Text className={styles.rowLabel}>实际发货</Text>
                <Text className={styles.rowValue}>{item.shipDate}</Text>
              </View>
            )}
            <View className={styles.row}>
              <Text className={styles.rowLabel}>收货地址</Text>
              <Text className={styles.rowValue}>{item.address || '—'}</Text>
            </View>
            {item.carrier && (
              <View className={styles.row}>
                <Text className={styles.rowLabel}>物流信息</Text>
                <Text className={styles.rowValue}>
                  {item.carrier} · {item.trackingNo}
                </Text>
                <View className={styles.rowAction} onClick={() => copyTrack(item.trackingNo)}>
                  <Icon name="copy" size={20} color="#2b6beb" className={styles.rowActionIcon} />
                  <Text className={styles.rowActionText}>复制</Text>
                </View>
              </View>
            )}
            {item.remark && <Text className={styles.remark}>{item.remark}</Text>}
            <Text className={styles.updateTime}>更新于 {item.updateTime}</Text>
          </View>
        ))}

      {!loading && deliveries.length === 0 && (
        <View className={styles.emptyCard}>
          <Icon name="truck" size={52} color="#2b6beb" />
          <Text className={styles.emptyTitle}>暂无交付记录</Text>
          <Text className={styles.emptyDesc}>订单确认并排产后，会在此展示生产与发运进度</Text>
        </View>
      )}
    </View>
  );
};

export default DeliveryPage;

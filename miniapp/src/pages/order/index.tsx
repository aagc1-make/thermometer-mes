import React, { useEffect, useMemo, useState } from 'react';
import { ScrollView, Text, View } from '@tarojs/components';
import Taro from '@tarojs/taro';
import classnames from 'classnames';
import PageHeader from '@/components/PageHeader';
import Icon from '@/components/Icon';
import ProductArt from '@/components/ProductArt';
import { fetchOrders } from '@/services/api';
import type { Order, OrderStatus } from '@/types';
import styles from './index.module.scss';

type TabKey = 'all' | OrderStatus;

const TABS: { key: TabKey; name: string }[] = [
  { key: 'all', name: '全部' },
  { key: 'quoted', name: '待报价' },
  { key: 'confirmed', name: '待确认' },
  { key: 'producing', name: '生产中' },
  { key: 'delivered', name: '已交付' }
];

const OrderPage: React.FC = () => {
  const [tab, setTab] = useState<TabKey>('all');
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    const list = await fetchOrders();
    setOrders(list);
    setLoading(false);
  };

  useEffect(() => {
    const params = Taro.getCurrentInstance().router?.params;
    const status = params?.status as TabKey | undefined;
    if (status && TABS.some((item) => item.key === status)) {
      setTab(status);
    }
    load();
  }, []);

  const visible = useMemo(
    () => (tab === 'all' ? orders : orders.filter((item) => item.status === tab)),
    [orders, tab]
  );

  const openDetail = (order: Order) => {
    Taro.navigateTo({ url: `/pages/order-detail/index?orderNo=${order.orderNo}` });
  };

  const copyNo = (orderNo: string) => {
    Taro.setClipboardData({ data: orderNo });
  };

  return (
    <View className={styles.page}>
      <PageHeader title="我的订单" showBack />

      <ScrollView className={styles.tabs} scrollX>
        <View className={styles.tabRow}>
          {TABS.map((item) => (
            <View key={item.key} className={styles.tabItem} onClick={() => setTab(item.key)}>
              <Text className={classnames(styles.tabText, tab === item.key && styles.tabTextActive)}>
                {item.name}
              </Text>
              {tab === item.key && <View className={styles.tabLine} />}
            </View>
          ))}
        </View>
      </ScrollView>

      <View className={styles.summary}>
        <View className={styles.summaryItem}>
          <Text className={styles.summaryValue}>{orders.length}</Text>
          <Text className={styles.summaryLabel}>全部订单</Text>
        </View>
        <View className={styles.summaryDivider} />
        <View className={styles.summaryItem}>
          <Text className={styles.summaryValue}>
            {orders.filter((item) => item.status === 'producing').length}
          </Text>
          <Text className={styles.summaryLabel}>生产中</Text>
        </View>
        <View className={styles.summaryDivider} />
        <View className={styles.summaryItem}>
          <Text className={styles.summaryValue}>
            {orders.filter((item) => item.status === 'delivered').length}
          </Text>
          <Text className={styles.summaryLabel}>已交付</Text>
        </View>
      </View>

      {loading && (
        <View className={styles.empty}>
          <Text className={styles.emptyText}>加载中...</Text>
        </View>
      )}

      {!loading &&
        visible.map((order) => (
          <View key={order.orderNo} className={styles.card} onClick={() => openDetail(order)}>
            <View className={styles.head}>
              <View className={styles.headLeft} onClick={() => copyNo(order.orderNo)}>
                <Text className={styles.orderNo}>{order.orderNo}</Text>
                <Icon name="doc" size={24} color="#c3ccdd" />
              </View>
              <Text className={classnames(styles.status, styles[`status_${order.status}`])}>
                {order.statusName}
              </Text>
            </View>

            <View className={styles.dashed} />

            {order.lines.slice(0, 2).map((line) => (
              <View key={`${order.orderNo}-${line.itemCode}-${line.lineNo}`} className={styles.lineRow}>
                <View className={styles.thumb}>
                  <ProductArt art={line.art || 'thermometer'} itemCode={line.itemCode} size={88} />
                </View>
                <View className={styles.lineInfo}>
                  <Text className={styles.lineName}>{line.itemName}</Text>
                  <Text className={styles.lineMeta}>
                    {line.itemCode} · {line.specification || '规格以订单为准'}
                  </Text>
                </View>
                <Text className={styles.lineQty}>×{line.qty}</Text>
              </View>
            ))}
            {order.lines.length > 2 && (
              <Text className={styles.moreLine}>等 {order.lines.length} 个品项</Text>
            )}

            <View className={styles.foot}>
              <View className={styles.footLeft}>
                <Text className={styles.footMeta}>
                  {order.itemCount} 个品项 · 共 {order.totalQty} 件
                </Text>
                <Text className={styles.footDate}>期望交期 {order.expectedDate || '待确认'}</Text>
              </View>
              <View className={styles.footRight}>
                <Text className={styles.amountLabel}>订单金额</Text>
                <Text className={styles.amount}>
                  {order.totalAmount > 0 ? `¥${order.totalAmount.toLocaleString()}` : '待报价'}
                </Text>
              </View>
            </View>

            <View className={styles.actions}>
              <View className={styles.ghostBtn} onClick={() => Taro.navigateTo({ url: '/pages/sales/index' })}>
                <Icon name="headset" size={24} color="#2b6beb" className={styles.btnIcon} />
                联系销售
              </View>
              <View className={styles.primaryBtn} onClick={() => openDetail(order)}>
                查看详情
                <Icon name="chevronRight" size={20} color="#ffffff" className={styles.btnIconRight} />
              </View>
            </View>
          </View>
        ))}

      {!loading && visible.length === 0 && (
        <View className={styles.emptyCard}>
          <View className={styles.emptyIcon}>
            <Icon name="box" size={52} color="#2b6beb" />
          </View>
          <Text className={styles.emptyTitle}>暂无该状态的订单</Text>
          <Text className={styles.emptyDesc}>提交询价并确认报价后，即可在此查看正式订单</Text>
          <View className={styles.emptyBtn} onClick={() => Taro.switchTab({ url: '/pages/product/index' })}>
            <Icon name="plus" size={24} color="#ffffff" className={styles.btnIcon} />
            <Text className={styles.emptyBtnText}>去选择产品</Text>
          </View>
        </View>
      )}
    </View>
  );
};

export default OrderPage;

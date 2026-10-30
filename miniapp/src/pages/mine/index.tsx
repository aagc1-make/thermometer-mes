import React, { useEffect, useState } from 'react';
import { View, Text } from '@tarojs/components';
import Taro, { useDidShow } from '@tarojs/taro';
import Icon from '@/components/Icon';
import type { IconName } from '@/components/Icon';
import { SERVICE_LIST } from '@/data/customer';
import { CUSTOMER_PROFILE } from '@/data/enterprise';
import { fetchCustomerProfile } from '@/services/api';
import { useInquiryStore } from '@/store/inquiry';
import { syncInquiryBadge } from '@/utils/tabbar';
import { getStatusBarHeight } from '@/utils/system';
import type { CustomerProfile, OrderStatus } from '@/types';
import styles from './index.module.scss';

const ORDER_ICONS: Record<string, IconName> = {
  quoted: 'clock',
  confirmed: 'doc',
  producing: 'box',
  delivered: 'check'
};

const ORDER_ENTRIES: { key: OrderStatus; name: string }[] = [
  { key: 'quoted', name: '待报价' },
  { key: 'confirmed', name: '待确认' },
  { key: 'producing', name: '生产中' },
  { key: 'delivered', name: '已交付' }
];

const MinePage: React.FC = () => {
  const [customer, setCustomer] = useState<CustomerProfile>(CUSTOMER_PROFILE);

  const loadProfile = async () => {
    const profile = await fetchCustomerProfile();
    setCustomer(profile);
  };

  useEffect(() => {
    loadProfile();
  }, []);

  useDidShow(() => {
    syncInquiryBadge(useInquiryStore.getState().items.length);
    loadProfile();
  });

  const openOrder = (status: string) => {
    Taro.navigateTo({ url: `/pages/order/index?status=${status}` });
  };

  const openService = (key: string) => {
    Taro.navigateTo({ url: `/pages/${key}/index` });
  };

  return (
    <View className={styles.page}>
      <View className={styles.header} style={{ paddingTop: `${getStatusBarHeight()}px` }}>
        <View className={styles.decorLarge} />
        <View className={styles.decorSmall} />
        <View className={styles.headerRow}>
          <View className={styles.avatar}>
            <Text className={styles.avatarText}>{customer.shortName}</Text>
          </View>
          <View className={styles.companyInfo}>
            <Text className={styles.company}>{customer.company}</Text>
            <View className={styles.badge}>
              <Icon name="check" size={16} color="#ffffff" />
              <Text className={styles.badgeText}>企业认证 · {customer.level}</Text>
            </View>
            <Text className={styles.buyer}>{customer.buyer}</Text>
          </View>
          <View
            className={styles.setting}
            onClick={() => Taro.showToast({ title: '企业信息修改请联系专属销售', icon: 'none' })}
          >
            <Icon name="sliders" size={36} color="#ffffff" />
          </View>
        </View>
      </View>

      <View className={styles.statsCard}>
        <View className={styles.statItem}>
          <Text className={styles.statValue}>{customer.stats.consulting}</Text>
          <Text className={styles.statLabel}>询价中</Text>
        </View>
        <View className={styles.statDivider} />
        <View className={styles.statItem}>
          <Text className={styles.statValue}>{customer.stats.pending}</Text>
          <Text className={styles.statLabel}>待确认</Text>
        </View>
        <View className={styles.statDivider} />
        <View className={styles.statItem}>
          <Text className={styles.statValue}>{customer.stats.closed}</Text>
          <Text className={styles.statLabel}>已成交</Text>
        </View>
      </View>

      <View className={styles.card}>
        <View className={styles.cardHead}>
          <Text className={styles.cardTitle}>我的订单</Text>
          <View className={styles.cardMore} onClick={() => Taro.navigateTo({ url: '/pages/order/index' })}>
            <Text className={styles.cardMoreText}>全部订单</Text>
            <Icon name="chevronRight" size={18} color="#8a99b3" />
          </View>
        </View>
        <View className={styles.orderRow}>
          {ORDER_ENTRIES.map((item) => {
            const count = customer.orderCounts[item.key];
            return (
              <View key={item.key} className={styles.orderItem} onClick={() => openOrder(item.key)}>
                <View className={styles.orderIconWrap}>
                  <View className={styles.orderIcon}>
                    <Icon name={ORDER_ICONS[item.key]} size={40} color="#2b6beb" />
                  </View>
                  {count > 0 && (
                    <View className={styles.orderBadge}>
                      <Text className={styles.orderBadgeText}>{count}</Text>
                    </View>
                  )}
                </View>
                <Text className={styles.orderName}>{item.name}</Text>
              </View>
            );
          })}
        </View>
      </View>

      <View className={styles.card}>
        <View className={styles.cardHead}>
          <Text className={styles.cardTitle}>常用服务</Text>
        </View>
        <View className={styles.serviceGrid}>
          {SERVICE_LIST.map((item) => (
            <View key={item.key} className={styles.serviceItem} onClick={() => openService(item.key)}>
              <View className={styles.serviceIcon}>
                <Icon name={item.icon as IconName} size={40} color="#2b6beb" />
              </View>
              <Text className={styles.serviceName}>{item.name}</Text>
            </View>
          ))}
        </View>
      </View>

      <View className={styles.card}>
        <View className={styles.cardHead}>
          <Text className={styles.cardTitle}>企业信息</Text>
          <View
            className={styles.cardMore}
            onClick={() => Taro.showToast({ title: '请联系专属销售修改企业信息', icon: 'none' })}
          >
            <Text className={styles.cardMoreText}>编辑</Text>
            <Icon name="chevronRight" size={18} color="#8a99b3" />
          </View>
        </View>
        <View className={styles.infoRow}>
          <Text className={styles.infoLabel}>统一社会信用代码</Text>
          <Text className={styles.infoValue}>{customer.creditCode}</Text>
        </View>
        <View className={styles.infoDivider} />
        <View className={styles.infoRow}>
          <Text className={styles.infoLabel}>客户等级</Text>
          <Text className={styles.infoValue}>{customer.levelDesc}</Text>
        </View>
        <View className={styles.infoDivider} />
        <View className={styles.infoRow} onClick={() => Taro.navigateTo({ url: '/pages/sales/index' })}>
          <Text className={styles.infoLabel}>专属销售</Text>
          <Text className={styles.infoValue}>
            {customer.salesName} {customer.salesPhone}
          </Text>
          <Icon name="chevronRight" size={18} color="#c3ccdd" />
        </View>
      </View>

      <Text className={styles.footer}>红外测温枪 MES · 企业订购 v1.0.0</Text>
    </View>
  );
};

export default MinePage;

import React, { useEffect, useState } from 'react';
import { Text, View } from '@tarojs/components';
import Taro from '@tarojs/taro';
import PageHeader from '@/components/PageHeader';
import Icon from '@/components/Icon';
import { fetchSales } from '@/services/api';
import type { SalesInfo } from '@/types';
import styles from './index.module.scss';

const SalesPage: React.FC = () => {
  const [sales, setSales] = useState<SalesInfo | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      const info = await fetchSales();
      setSales(info);
      setLoading(false);
    };
    load();
  }, []);

  const safeCall = (phone?: string) => {
    if (!phone) {
      Taro.showToast({ title: '暂无可用号码', icon: 'none' });
      return;
    }
    if (phone.includes('*')) {
      Taro.showToast({ title: '号码已脱敏，请通过微信联系', icon: 'none' });
      return;
    }
    Taro.makePhoneCall({ phoneNumber: phone.replace(/[^\d+]/g, '') }).catch(() => undefined);
  };

  const copy = (value?: string, tip = '已复制') => {
    if (!value) {
      return;
    }
    Taro.setClipboardData({ data: value });
    Taro.showToast({ title: tip, icon: 'none' });
  };

  return (
    <View className={styles.page}>
      <PageHeader title="联系销售" showBack />

      {loading && (
        <View className={styles.empty}>
          <Text className={styles.emptyText}>加载中...</Text>
        </View>
      )}

      {!loading && sales && (
        <>
          <View className={styles.hero}>
            <View className={styles.avatar}>
              <Text className={styles.avatarText}>{sales.name.slice(0, 1)}</Text>
            </View>
            <View className={styles.heroInfo}>
              <Text className={styles.name}>{sales.name}</Text>
              <Text className={styles.role}>专属客户经理{sales.region ? ` · ${sales.region}` : ''}</Text>
            </View>
            <View className={styles.onlineTag}>
              <View className={styles.onlineDot} />
              <Text className={styles.onlineText}>在线</Text>
            </View>
          </View>

          <View className={styles.card}>
            <View className={styles.row} onClick={() => copy(sales.phone, '号码已复制')}>
              <View className={styles.rowIcon}>
                <Icon name="headset" size={30} color="#2b6beb" />
              </View>
              <View className={styles.rowInfo}>
                <Text className={styles.rowLabel}>联系电话</Text>
                <Text className={styles.rowValue}>{sales.phone}</Text>
              </View>
              <View className={styles.rowAction}>
                <Icon name="copy" size={20} color="#2b6beb" className={styles.rowActionIcon} />
                <Text className={styles.rowActionText}>复制</Text>
              </View>
            </View>
            <View className={styles.divider} />
            <View className={styles.row} onClick={() => copy(sales.wechat, '微信号已复制')}>
              <View className={styles.rowIcon}>
                <Icon name="users" size={30} color="#2b6beb" />
              </View>
              <View className={styles.rowInfo}>
                <Text className={styles.rowLabel}>企业微信</Text>
                <Text className={styles.rowValue}>{sales.wechat || '未开通'}</Text>
              </View>
              <View className={styles.rowAction}>
                <Icon name="copy" size={20} color="#2b6beb" className={styles.rowActionIcon} />
                <Text className={styles.rowActionText}>复制</Text>
              </View>
            </View>
            <View className={styles.divider} />
            <View className={styles.row}>
              <View className={styles.rowIcon}>
                <Icon name="clock" size={30} color="#2b6beb" />
              </View>
              <View className={styles.rowInfo}>
                <Text className={styles.rowLabel}>服务时间</Text>
                <Text className={styles.rowValue}>{sales.workTime || '工作日 08:30 - 18:00'}</Text>
              </View>
            </View>
          </View>

          <View className={styles.actions}>
            <View className={styles.ghostBtn} onClick={() => safeCall(sales.phone)}>
              <Icon name="headset" size={24} color="#2b6beb" className={styles.btnIcon} />
              <Text className={styles.ghostText}>拨打电话</Text>
            </View>
            <View className={styles.primaryBtn} onClick={() => safeCall(sales.serviceTel || sales.phone)}>
              <Icon name="help" size={24} color="#ffffff" className={styles.btnIcon} />
              <Text className={styles.primaryText}>售后客服</Text>
            </View>
          </View>

          <View className={styles.tipCard}>
            <Icon name="help" size={28} color="#2b6beb" />
            <Text className={styles.tipText}>
              询价、加急排产、开票与售后问题都可直接联系专属销售；号码脱敏时请通过企业微信或 400 客服热线沟通。
            </Text>
          </View>

          <View className={styles.quickCard}>
            <Text className={styles.quickTitle}>常用入口</Text>
            <View className={styles.quickRow}>
              <View
                className={styles.quickItem}
                onClick={() => Taro.navigateTo({ url: '/pages/delivery/index' })}
              >
                <View className={styles.quickIcon}>
                  <Icon name="truck" size={32} color="#2b6beb" />
                </View>
                <Text className={styles.quickName}>交付进度</Text>
              </View>
              <View className={styles.quickItem} onClick={() => Taro.navigateTo({ url: '/pages/invoice/index' })}>
                <View className={styles.quickIcon}>
                  <Icon name="doc" size={32} color="#2b6beb" />
                </View>
                <Text className={styles.quickName}>开票信息</Text>
              </View>
              <View className={styles.quickItem} onClick={() => Taro.navigateTo({ url: '/pages/statement/index' })}>
                <View className={styles.quickIcon}>
                  <Icon name="card" size={32} color="#2b6beb" />
                </View>
                <Text className={styles.quickName}>对账单</Text>
              </View>
              <View className={styles.quickItem} onClick={() => Taro.navigateTo({ url: '/pages/help/index' })}>
                <View className={styles.quickIcon}>
                  <Icon name="help" size={32} color="#2b6beb" />
                </View>
                <Text className={styles.quickName}>帮助中心</Text>
              </View>
            </View>
          </View>
        </>
      )}
    </View>
  );
};

export default SalesPage;

import React, { useEffect, useState } from 'react';
import { Text, View } from '@tarojs/components';
import Taro from '@tarojs/taro';
import PageHeader from '@/components/PageHeader';
import Icon from '@/components/Icon';
import { fetchContacts } from '@/services/api';
import type { Contact } from '@/types';
import styles from './index.module.scss';

const ContactPage: React.FC = () => {
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      const list = await fetchContacts();
      setContacts(list);
      setLoading(false);
    };
    load();
  }, []);

  const call = (contact: Contact) => {
    if (contact.tel.includes('*')) {
      Taro.showToast({ title: '该号码已脱敏，请联系专属销售', icon: 'none' });
      return;
    }
    Taro.makePhoneCall({ phoneNumber: contact.tel.replace(/[^\d+]/g, '') }).catch((error) => {
      console.info('[Contact] 拨号取消', error);
    });
  };

  const copy = (value: string, tip: string) => {
    Taro.setClipboardData({ data: value });
    Taro.showToast({ title: tip, icon: 'none' });
  };

  return (
    <View className={styles.page}>
      <PageHeader title="联系人" showBack />

      <View className={styles.tipCard}>
        <View className={styles.tipIcon}>
          <Icon name="users" size={34} color="#2b6beb" />
        </View>
        <View className={styles.tipContent}>
          <Text className={styles.tipTitle}>企业常用联系人</Text>
          <Text className={styles.tipDesc}>收货、对账与设备问题可直接联系对应负责人，变更请联系专属销售。</Text>
        </View>
      </View>

      {loading && (
        <View className={styles.empty}>
          <Text className={styles.emptyText}>加载中...</Text>
        </View>
      )}

      {!loading &&
        contacts.map((item) => (
          <View key={item.id} className={styles.card}>
            <View className={styles.head}>
              <View className={styles.avatar}>
                <Text className={styles.avatarText}>{item.name.slice(0, 1)}</Text>
              </View>
              <View className={styles.info}>
                <View className={styles.nameRow}>
                  <Text className={styles.name}>{item.name}</Text>
                  {item.isDefault && <Text className={styles.defaultTag}>主要联系人</Text>}
                </View>
                <Text className={styles.dept}>{item.dept || '—'}</Text>
              </View>
            </View>

            <View className={styles.dashed} />

            <View className={styles.row}>
              <Text className={styles.rowLabel}>电话</Text>
              <Text className={styles.rowValue}>{item.tel}</Text>
              <View className={styles.rowAction} onClick={() => copy(item.tel, '号码已复制')}>
                <Icon name="copy" size={20} color="#2b6beb" className={styles.rowActionIcon} />
                <Text className={styles.rowActionText}>复制</Text>
              </View>
            </View>
            <View className={styles.row}>
              <Text className={styles.rowLabel}>邮箱</Text>
              <Text className={styles.rowValue}>{item.email || '—'}</Text>
              {item.email && (
                <View className={styles.rowAction} onClick={() => copy(item.email || '', '邮箱已复制')}>
                  <Icon name="copy" size={20} color="#2b6beb" className={styles.rowActionIcon} />
                  <Text className={styles.rowActionText}>复制</Text>
                </View>
              )}
            </View>

            <View className={styles.footer}>
              <View className={styles.callBtn} onClick={() => call(item)}>
                <Icon name="headset" size={28} color="#ffffff" />
                <Text className={styles.callText}>拨打电话</Text>
              </View>
            </View>
          </View>
        ))}

      {!loading && contacts.length === 0 && (
        <View className={styles.emptyCard}>
          <Icon name="users" size={52} color="#2b6beb" />
          <Text className={styles.emptyTitle}>暂无联系人</Text>
          <Text className={styles.emptyDesc}>请联系专属销售补充企业联系人信息</Text>
        </View>
      )}
    </View>
  );
};

export default ContactPage;

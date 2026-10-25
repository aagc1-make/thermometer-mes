import React, { useEffect, useMemo, useState } from 'react';
import { Input, Text, View } from '@tarojs/components';
import Taro from '@tarojs/taro';
import classnames from 'classnames';
import PageHeader from '@/components/PageHeader';
import Icon from '@/components/Icon';
import { fetchFaqs } from '@/services/api';
import type { Faq } from '@/types';
import styles from './index.module.scss';

const QUICK_LINKS = [
  { key: 'address', name: '收货地址', icon: 'pin' },
  { key: 'invoice', name: '开票信息', icon: 'doc' },
  { key: 'contact', name: '联系人', icon: 'users' },
  { key: 'delivery', name: '交付进度', icon: 'truck' }
] as const;

const HelpPage: React.FC = () => {
  const [faqs, setFaqs] = useState<Faq[]>([]);
  const [keyword, setKeyword] = useState('');
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      const list = await fetchFaqs();
      setFaqs(list);
      setLoading(false);
    };
    load();
  }, []);

  const visibleFaqs = useMemo(() => {
    const key = keyword.trim();
    if (!key) {
      return faqs;
    }
    return faqs.filter((item) => item.question.includes(key) || item.answer.includes(key) || item.category.includes(key));
  }, [faqs, keyword]);

  const categories = useMemo(() => Array.from(new Set(visibleFaqs.map((item) => item.category))), [visibleFaqs]);

  return (
    <View className={styles.page}>
      <PageHeader title="帮助中心" showBack />

      <View className={styles.searchWrap}>
        <View className={styles.search}>
          <Icon name="search" size={28} color="#8a99b3" />
          <Input
            className={styles.searchInput}
            value={keyword}
            placeholder="搜索问题，如：开票 / 交期 / 起订量"
            onInput={(event) => setKeyword(event.detail.value)}
          />
          {keyword && (
            <Text className={styles.searchClear} onClick={() => setKeyword('')}>
              清除
            </Text>
          )}
        </View>
      </View>

      <View className={styles.quickGrid}>
        {QUICK_LINKS.map((item) => (
          <View
            key={item.key}
            className={styles.quickItem}
            onClick={() => Taro.navigateTo({ url: `/pages/${item.key}/index` })}
          >
            <View className={styles.quickIcon}>
              <Icon name={item.icon} size={34} color="#2b6beb" />
            </View>
            <Text className={styles.quickName}>{item.name}</Text>
          </View>
        ))}
      </View>

      {loading && (
        <View className={styles.empty}>
          <Text className={styles.emptyText}>加载中...</Text>
        </View>
      )}

      {!loading &&
        categories.map((category) => (
          <View key={category} className={styles.group}>
            <Text className={styles.groupTitle}>{category}</Text>
            <View className={styles.groupCard}>
              {visibleFaqs
                .filter((item) => item.category === category)
                .map((item, index) => {
                  const expanded = expandedId === item.id;
                  return (
                    <View key={item.id} className={styles.faq}>
                      {index > 0 && <View className={styles.faqDivider} />}
                      <View className={styles.faqHead} onClick={() => setExpandedId(expanded ? null : item.id)}>
                        <Text className={classnames(styles.question, expanded && styles.questionActive)}>
                          {item.question}
                        </Text>
                        <Icon
                          name="chevronRight"
                          size={22}
                          color="#c3ccdd"
                        />
                      </View>
                      {expanded && <Text className={styles.answer}>{item.answer}</Text>}
                    </View>
                  );
                })}
            </View>
          </View>
        ))}

      {!loading && visibleFaqs.length === 0 && (
        <View className={styles.emptyCard}>
          <Icon name="search" size={52} color="#2b6beb" />
          <Text className={styles.emptyTitle}>未找到相关问题</Text>
          <Text className={styles.emptyDesc}>换个关键词，或直接联系专属销售获取解答</Text>
        </View>
      )}

      <View className={styles.serviceCard}>
        <View>
          <Text className={styles.serviceTitle}>仍未解决？</Text>
          <Text className={styles.serviceDesc}>工作日 08:30 - 18:00 在线，售后 24 小时内响应</Text>
        </View>
        <View className={styles.serviceActions}>
          <View
            className={styles.ghostBtn}
            onClick={() => Taro.makePhoneCall({ phoneNumber: '4008206621' }).catch(() => undefined)}
          >
            <Icon name="headset" size={22} color="#2b6beb" className={styles.btnIcon} />
            客服电话
          </View>
          <View className={styles.primaryBtn} onClick={() => Taro.navigateTo({ url: '/pages/sales/index' })}>
            <Icon name="users" size={22} color="#ffffff" className={styles.btnIcon} />
            <Text className={styles.primaryText}>联系销售</Text>
          </View>
        </View>
      </View>
    </View>
  );
};

export default HelpPage;

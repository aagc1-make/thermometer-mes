import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { View, Text, Input } from '@tarojs/components';
import Taro, { useDidShow } from '@tarojs/taro';
import classnames from 'classnames';
import PageHeader from '@/components/PageHeader';
import Icon from '@/components/Icon';
import type { IconName } from '@/components/Icon';
import ProductRow from '@/components/ProductRow';
import { useCatalogStore } from '@/store/catalog';
import { useInquiryStore } from '@/store/inquiry';
import { syncInquiryBadge } from '@/utils/tabbar';
import { navigateTo } from '@/utils/navigate';
import type { Product } from '@/types';
import styles from './index.module.scss';

type HomeTab = 'all' | 'finished' | 'semi' | 'material';

const TABS: { key: HomeTab; name: string }[] = [
  { key: 'all', name: '全部' },
  { key: 'finished', name: '成品' },
  { key: 'semi', name: '半成品' },
  { key: 'material', name: '物料' }
];

const QUICK_ENTRIES: { key: string; name: string; icon: IconName }[] = [
  { key: 'batch', name: '批量询价', icon: 'doc' },
  { key: 'order', name: '我的订单', icon: 'box' },
  { key: 'delivery', name: '交付进度', icon: 'truck' },
  { key: 'sales', name: '联系销售', icon: 'headset' }
];

const HomePage: React.FC = () => {
  const [keyword, setKeyword] = useState('');
  const [tab, setTab] = useState<HomeTab>('all');
  const addItem = useInquiryStore((state) => state.addItem);
  const products = useCatalogStore((state) => state.products);
  const loadCatalog = useCatalogStore((state) => state.load);

  useEffect(() => {
    loadCatalog();
  }, [loadCatalog]);

  useDidShow(() => {
    syncInquiryBadge(useInquiryStore.getState().items.length);
  });

  const visibleProducts = useMemo(() => {
    const base =
      tab === 'all'
        ? products.slice(0, 3)
        : products.filter((product) => product.category === tab).slice(0, 3);
    const kw = keyword.trim().toLowerCase();
    if (!kw) {
      return base;
    }
    return base.filter(
      (product) =>
        product.name.toLowerCase().includes(kw) || product.code.toLowerCase().includes(kw)
    );
  }, [products, tab, keyword]);

  const handleAdd = useCallback(
    (product: Product) => {
      addItem(product);
      syncInquiryBadge(useInquiryStore.getState().items.length);
      Taro.showToast({ title: '已加入询价单', icon: 'none' });
    },
    [addItem]
  );

  const handleOpenProduct = useCallback((product: Product) => {
    navigateTo(`/pages/product-detail/index?id=${product.id}`);
  }, []);

  const goInquiry = () => {
    Taro.switchTab({ url: '/pages/inquiry/index' });
  };

  const handleQuick = (key: string) => {
    switch (key) {
      case 'batch':
        goInquiry();
        break;
      case 'order':
        Taro.navigateTo({ url: '/pages/order/index' });
        break;
      case 'delivery':
        Taro.navigateTo({ url: '/pages/delivery/index' });
        break;
      case 'sales':
        Taro.navigateTo({ url: '/pages/sales/index' });
        break;
      default:
        break;
    }
  };

  return (
    <View className={styles.page}>
      <PageHeader
        title="企业订购"
        rightIcon="bell"
        onRightClick={() => Taro.showToast({ title: '暂无新消息', icon: 'none' })}
      />

      <View className={styles.search}>
        <Icon name="search" size={32} color="#8a99b3" />
        <Input
          className={styles.searchInput}
          placeholder="搜索产品名称或编码"
          placeholderStyle="color: #8a99b3"
          value={keyword}
          onInput={(event) => setKeyword(event.detail.value)}
        />
      </View>

      <View className={styles.hero}>
        <View className={styles.heroLeft}>
          <Text className={styles.heroTitle}>成品 · 半成品 · 物料</Text>
          <Text className={styles.heroTitle}>批量订购</Text>
          <Text className={styles.heroSub}>按需选择，提交采购需求</Text>
          <View className={styles.heroBtn} onClick={goInquiry}>
            <Text className={styles.heroBtnText}>发起询价</Text>
            <Icon name="chevronRight" size={20} color="#ffffff" className={styles.heroArrow} />
          </View>
        </View>
        <View className={styles.heroArt}>
          <View className={styles.heroGlow} />
          <View className={styles.heroDevice}>
            <View className={styles.heroScreen}>
              <Text className={styles.heroScreenText}>36.6</Text>
            </View>
            <View className={styles.heroLens} />
            <View className={styles.heroButton} />
            <View className={styles.heroHandle} />
          </View>
          <View className={styles.heroPlatform} />
        </View>
      </View>

      <View className={styles.quickCard}>
        {QUICK_ENTRIES.map((entry) => (
          <View key={entry.key} className={styles.quickItem} onClick={() => handleQuick(entry.key)}>
            <View className={styles.quickIcon}>
              <Icon name={entry.icon} size={40} color="#2b6beb" />
            </View>
            <Text className={styles.quickName}>{entry.name}</Text>
          </View>
        ))}
      </View>

      <View className={styles.sectionHeader}>
        <Text className={styles.sectionTitle}>可订购产品</Text>
        <View className={styles.sectionMore} onClick={() => Taro.switchTab({ url: '/pages/product/index' })}>
          <Text className={styles.sectionMoreText}>全部产品</Text>
          <Icon name="chevronRight" size={20} color="#2b6beb" />
        </View>
      </View>

      <View className={styles.tabs}>
        {TABS.map((item) => (
          <View key={item.key} className={styles.tabItem} onClick={() => setTab(item.key)}>
            <Text className={classnames(styles.tabText, tab === item.key && styles.tabTextActive)}>
              {item.name}
            </Text>
            {tab === item.key && <View className={styles.tabLine} />}
          </View>
        ))}
      </View>

      <View className={styles.list}>
        {visibleProducts.map((product) => (
          <ProductRow
            key={product.id}
            product={product}
            onAdd={handleAdd}
            onClick={handleOpenProduct}
          />
        ))}
        {visibleProducts.length === 0 && (
          <View className={styles.empty}>
            <Text className={styles.emptyText}>没有找到相关产品</Text>
          </View>
        )}
      </View>

      <View className={styles.banner} onClick={() => Taro.navigateTo({ url: '/pages/sales/index' })}>
        <View className={styles.bannerIcon}>
          <Icon name="headset" size={36} color="#2b6beb" />
        </View>
        <View className={styles.bannerInfo}>
          <Text className={styles.bannerTitle}>大批量采购需求</Text>
          <Text className={styles.bannerSub}>提交数量与交期，销售确认报价</Text>
        </View>
        <View className={styles.bannerLink}>
          <Text className={styles.bannerLinkText}>立即咨询</Text>
          <Icon name="chevronRight" size={20} color="#2b6beb" />
        </View>
      </View>

      <View className={styles.bottomSpace} />
    </View>
  );
};

export default HomePage;

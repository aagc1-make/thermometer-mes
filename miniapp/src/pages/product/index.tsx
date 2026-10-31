import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { View, Text, Input, ScrollView } from '@tarojs/components';
import Taro, { useDidShow } from '@tarojs/taro';
import classnames from 'classnames';
import PageHeader from '@/components/PageHeader';
import Icon from '@/components/Icon';
import ProductTile from '@/components/ProductTile';
import { PRODUCT_FILTERS } from '@/data/products';
import { useCatalogStore } from '@/store/catalog';
import { useInquiryStore } from '@/store/inquiry';
import { syncInquiryBadge } from '@/utils/tabbar';
import { navigateTo } from '@/utils/navigate';
import type { Product } from '@/types';
import styles from './index.module.scss';

type FilterKey = (typeof PRODUCT_FILTERS)[number];
type SortKey = 'default' | 'lead' | 'moq';

const SORTS: { key: SortKey; name: string }[] = [
  { key: 'default', name: '综合' },
  { key: 'lead', name: '交付期' },
  { key: 'moq', name: '起订量' }
];

const ProductPage: React.FC = () => {
  const [filter, setFilter] = useState<FilterKey>('全部');
  const [sort, setSort] = useState<SortKey>('default');
  const [keyword, setKeyword] = useState('');
  const [searchFocus, setSearchFocus] = useState(false);
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
    const kw = keyword.trim().toLowerCase();
    let list = products.filter((product) => {
      if (kw && !product.name.toLowerCase().includes(kw) && !product.code.toLowerCase().includes(kw)) {
        return false;
      }
      if (filter === '成品') {
        return product.category === 'finished';
      }
      if (filter === '半成品') {
        return product.category === 'semi';
      }
      if (filter === '物料') {
        return product.category === 'material';
      }
      if (filter === '电子件' || filter === '结构件' || filter === '包装辅料') {
        return product.group === filter;
      }
      return true;
    });

    if (sort === 'lead') {
      list = [...list].sort((a, b) => a.leadDays - b.leadDays);
    } else if (sort === 'moq') {
      list = [...list].sort((a, b) => a.moq - b.moq);
    }
    return list;
  }, [products, filter, sort, keyword]);

  const handleAdd = useCallback(
    (product: Product) => {
      addItem(product);
      syncInquiryBadge(useInquiryStore.getState().items.length);
      Taro.showToast({ title: '已加入询价单', icon: 'none' });
    },
    [addItem]
  );

  const handleOpen = useCallback((product: Product) => {
    navigateTo(`/pages/product-detail/index?id=${product.id}`);
  }, []);

  return (
    <View className={styles.page}>
      <PageHeader title="产品目录" rightIcon="search" onRightClick={() => setSearchFocus(true)} />

      <View className={styles.search}>
        <Icon name="search" size={32} color="#8a99b3" />
        <Input
          className={styles.searchInput}
          placeholder="搜索产品名称或编码"
          placeholderStyle="color: #8a99b3"
          focus={searchFocus}
          value={keyword}
          onInput={(event) => setKeyword(event.detail.value)}
          onBlur={() => setSearchFocus(false)}
        />
      </View>

      <ScrollView scrollX className={styles.chips}>
        {PRODUCT_FILTERS.map((item) => (
          <View
            key={item}
            className={classnames(styles.chip, filter === item && styles.chipActive)}
            onClick={() => setFilter(item)}
          >
            <Text className={classnames(styles.chipText, filter === item && styles.chipTextActive)}>
              {item}
            </Text>
          </View>
        ))}
      </ScrollView>

      <View className={styles.filterRow}>
        {SORTS.map((item) => (
          <View key={item.key} className={styles.sortItem} onClick={() => setSort(item.key)}>
            <Text className={classnames(styles.sortText, sort === item.key && styles.sortTextActive)}>
              {item.name}
            </Text>
            <Icon
              name="chevronDown"
              size={18}
              color={sort === item.key ? '#2b6beb' : '#8a99b3'}
            />
          </View>
        ))}
        <Text className={styles.total}>共 {visibleProducts.length} 款</Text>
      </View>

      <View className={styles.grid}>
        {visibleProducts.map((product) => (
          <ProductTile key={product.id} product={product} onAdd={handleAdd} onClick={handleOpen} />
        ))}
      </View>

      {visibleProducts.length === 0 && (
        <View className={styles.empty}>
          <Text className={styles.emptyText}>没有找到相关产品</Text>
        </View>
      )}

      <View className={styles.sample} onClick={() => Taro.navigateTo({ url: '/pages/help/index' })}>
        <View className={styles.sampleIcon}>
          <Icon name="truck" size={36} color="#2b6beb" />
        </View>
        <View className={styles.sampleInfo}>
          <Text className={styles.sampleTitle}>样品与试产订单</Text>
          <Text className={styles.sampleSub}>小批量试产支持 5 件起订</Text>
        </View>
        <View className={styles.sampleLink}>
          <Text className={styles.sampleLinkText}>查看说明</Text>
          <Icon name="chevronRight" size={20} color="#2b6beb" />
        </View>
      </View>

      <View className={styles.bottomSpace} />
    </View>
  );
};

export default ProductPage;

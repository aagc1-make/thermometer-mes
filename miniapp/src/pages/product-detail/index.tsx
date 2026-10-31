import React, { useEffect } from 'react';
import { View, Text } from '@tarojs/components';
import Taro, { useRouter } from '@tarojs/taro';
import classnames from 'classnames';
import PageHeader from '@/components/PageHeader';
import Icon from '@/components/Icon';
import ProductArt from '@/components/ProductArt';
import { useCatalogStore } from '@/store/catalog';
import { useInquiryStore } from '@/store/inquiry';
import { bomOf, parentsOf } from '@/data/bom';
import { syncInquiryBadge } from '@/utils/tabbar';
import type { Product } from '@/types';
import styles from './index.module.scss';

const ProductDetailPage: React.FC = () => {
  const router = useRouter();
  const id = router.params.id;
  const products = useCatalogStore((state) => state.products);
  const loadCatalog = useCatalogStore((state) => state.load);
  const product = products.find((item) => item.id === id);
  const addItem = useInquiryStore((state) => state.addItem);

  useEffect(() => {
    loadCatalog();
  }, [loadCatalog]);

  if (!product) {
    return (
      <View className={styles.page}>
        <PageHeader title="产品详情" showBack />
        <View className={styles.empty}>
          <Text className={styles.emptyText}>产品不存在或已下架</Text>
        </View>
      </View>
    );
  }

  const handleAdd = () => {
    addItem(product);
    syncInquiryBadge(useInquiryStore.getState().items.length);
    Taro.showToast({ title: '已加入询价单', icon: 'none' });
  };

  const handleInquiry = () => {
    addItem(product);
    syncInquiryBadge(useInquiryStore.getState().items.length);
    Taro.switchTab({ url: '/pages/inquiry/index' });
  };

  const bom = bomOf(product.code);
  const usedInNames = parentsOf(product.code)
    .map((code) => products.find((item) => item.code === code))
    .filter((item): item is Product => Boolean(item))
    .map((item) => `${item.name}（${item.code}）`);

  return (
    <View className={styles.page}>
      <PageHeader title="产品详情" showBack />

      <View className={styles.heroCard}>
        <View className={styles.heroArt}>
          <ProductArt art={product.art} itemCode={product.code} size={320} />
        </View>
        <View className={styles.tag}>
          <Text className={styles.tagText}>{product.categoryName}</Text>
        </View>
        <Text className={styles.name}>{product.name}</Text>
        <Text className={styles.code}>{product.code}</Text>
      </View>

      <View className={styles.card}>
        <Text className={styles.cardTitle}>订购参数</Text>
        <View className={styles.paramRow}>
          <Text className={styles.paramLabel}>物料类型</Text>
          <Text className={styles.paramValue}>{product.group || product.categoryName}</Text>
        </View>
        <View className={styles.divider} />
        <View className={styles.paramRow}>
          <Text className={styles.paramLabel}>规格参数</Text>
          <Text className={styles.paramValue}>{product.param}</Text>
        </View>
        <View className={styles.divider} />
        <View className={styles.paramRow}>
          <Text className={styles.paramLabel}>起订量</Text>
          <Text className={styles.paramValue}>
            {product.moq} {product.unit}
          </Text>
        </View>
        <View className={styles.divider} />
        <View className={styles.paramRow}>
          <Text className={styles.paramLabel}>参考交期</Text>
          <Text className={styles.paramValue}>{product.leadDays} 个工作日</Text>
        </View>
      </View>

      <View className={styles.card}>
        <Text className={styles.cardTitle}>产品说明</Text>
        <Text className={styles.desc}>{product.desc}</Text>
      </View>

      {bom.length > 0 && (
        <View className={styles.card}>
          <Text className={styles.cardTitle}>BOM 组成清单</Text>
          <Text className={styles.cardHint}>单台用量 · 物料编码与 MES 主数据一致</Text>
          {bom.map((line) => (
            <React.Fragment key={line.itemCode}>
              <View className={styles.bomRow}>
                <View className={styles.bomMain}>
                  <Text className={styles.bomName}>{line.itemName}</Text>
                  <Text className={styles.bomCode}>
                    {line.itemCode} · {line.kind === 'semi' ? '半成品' : '物料'}
                  </Text>
                </View>
                <Text className={styles.bomQty}>
                  {line.qty} {line.unit}
                </Text>
              </View>
              {(line.children || []).map((child) => (
                <View key={child.itemCode} className={classnames(styles.bomRow, styles.bomRowChild)}>
                  <View className={styles.bomMain}>
                    <Text className={styles.bomName}>{child.itemName}</Text>
                    <Text className={styles.bomCode}>{child.itemCode}</Text>
                  </View>
                  <Text className={styles.bomQty}>
                    {child.qty} {child.unit}
                  </Text>
                </View>
              ))}
            </React.Fragment>
          ))}
        </View>
      )}

      {usedInNames.length > 0 && (
        <View className={styles.card}>
          <Text className={styles.cardTitle}>应用机型</Text>
          {usedInNames.map((name) => (
            <Text key={name} className={styles.usedIn}>
              {name}
            </Text>
          ))}
        </View>
      )}

      <View className={styles.bottomSpace} />

      <View className={styles.actionBar}>
        <View className={styles.addBtn} onClick={handleAdd}>
          <Icon name="plus" size={28} color="#2b6beb" className={styles.btnIcon} />
          <Text className={styles.addBtnText}>加入询价</Text>
        </View>
        <View className={styles.inquiryBtn} onClick={handleInquiry}>
          <Icon name="doc" size={28} color="#ffffff" className={styles.btnIcon} />
          <Text className={styles.inquiryBtnText}>立即询价</Text>
        </View>
      </View>
    </View>
  );
};

export default ProductDetailPage;

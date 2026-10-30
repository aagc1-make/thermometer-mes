import React, { useEffect } from 'react';
import { View, Text } from '@tarojs/components';
import Taro, { useRouter } from '@tarojs/taro';
import PageHeader from '@/components/PageHeader';
import Icon from '@/components/Icon';
import ProductArt from '@/components/ProductArt';
import { useCatalogStore } from '@/store/catalog';
import { useInquiryStore } from '@/store/inquiry';
import { syncInquiryBadge } from '@/utils/tabbar';
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
          <Text className={styles.paramLabel}>产品类型</Text>
          <Text className={styles.paramValue}>
            {product.categoryName}
            {product.group ? ` · ${product.group}` : ''}
          </Text>
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

import React from 'react';
import { View, Text } from '@tarojs/components';
import type { Product } from '@/types';
import ProductArt from '@/components/ProductArt';
import Icon from '@/components/Icon';
import styles from './index.module.scss';

interface ProductRowProps {
  product: Product;
  onAdd: (product: Product) => void;
  onClick: (product: Product) => void;
}

/** 首页「可订购产品」横向商品行 */
const ProductRow: React.FC<ProductRowProps> = ({ product, onAdd, onClick }) => {
  return (
    <View className={styles.card} onClick={() => onClick(product)}>
      <View className={styles.thumb}>
        <ProductArt art={product.art} itemCode={product.code} size={128} />
      </View>
      <View className={styles.info}>
        <Text className={styles.tag}>{product.categoryName}</Text>
        <Text className={styles.name}>{product.name}</Text>
        <Text className={styles.code}>{product.code}</Text>
        <Text className={styles.param}>{product.param}</Text>
      </View>
      <View
        className={styles.addBtn}
        onClick={(event) => {
          event.stopPropagation();
          onAdd(product);
        }}
      >
        <Icon name="plus" size={22} color="#2b6beb" className={styles.addIcon} />
        <Text className={styles.addText}>加入询价</Text>
      </View>
    </View>
  );
};

export default React.memo(ProductRow);

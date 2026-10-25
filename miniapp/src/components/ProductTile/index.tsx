import React from 'react';
import { View, Text } from '@tarojs/components';
import type { Product } from '@/types';
import ProductArt from '@/components/ProductArt';
import Icon from '@/components/Icon';
import styles from './index.module.scss';

interface ProductTileProps {
  product: Product;
  onAdd: (product: Product) => void;
  onClick: (product: Product) => void;
}

/** 产品目录双列网格卡片 */
const ProductTile: React.FC<ProductTileProps> = ({ product, onAdd, onClick }) => {
  return (
    <View className={styles.card} onClick={() => onClick(product)}>
      <View className={styles.thumb}>
        <ProductArt art={product.art} size={200} />
      </View>
      <Text className={styles.tag}>{product.categoryName}</Text>
      <Text className={styles.name}>{product.name}</Text>
      <Text className={styles.code}>{product.code}</Text>
      <View className={styles.footer}>
        <Text className={styles.moq}>
          起订 {product.moq} {product.unit}
        </Text>
        <View
          className={styles.add}
          onClick={(event) => {
            event.stopPropagation();
            onAdd(product);
          }}
        >
          <Icon name="plus" size={28} color="#ffffff" />
        </View>
      </View>
    </View>
  );
};

export default ProductTile;

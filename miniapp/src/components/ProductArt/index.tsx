import React from 'react';
import { View, Text } from '@tarojs/components';
import classnames from 'classnames';
import type { ProductArt as ProductArtType } from '@/types';
import styles from './index.module.scss';

interface ProductArtProps {
  art: ProductArtType;
  /** 缩略图基准尺寸（rpx），用于换算内部文字大小 */
  size?: number;
  className?: string;
}

/**
 * 产品示意图（纯 CSS 绘制，避免依赖外部图片资源）
 */
const ProductArt: React.FC<ProductArtProps> = ({ art, size = 128, className }) => {
  const renderArt = () => {
    switch (art) {
      case 'thermometer':
        return (
          <View className={styles.stage}>
            <View className={styles.thermoDevice}>
              <View className={styles.thermoBody} />
              <View className={styles.thermoScreen}>
                <Text className={styles.screenText}>36.6</Text>
              </View>
              <View className={styles.thermoLens} />
              <View className={styles.thermoButton} />
              <View className={styles.thermoHandle} />
            </View>
            <View className={styles.base} />
          </View>
        );
      case 'precision':
        return (
          <View className={styles.stage}>
            <View className={styles.precisionDevice}>
              <View className={styles.thermoBody} />
              <View className={styles.thermoScreen}>
                <Text className={styles.screenText}>36.6</Text>
              </View>
              <View className={styles.precisionLensBig} />
              <View className={styles.precisionLensSmall} />
              <View className={styles.precisionLine} />
              <View className={styles.thermoHandle} />
            </View>
            <View className={styles.base} />
          </View>
        );
      case 'wall':
        return (
          <View className={styles.stage}>
            <View className={styles.wallDevice}>
              <View className={styles.wallBody} />
              <View className={styles.wallScreen}>
                <View className={styles.wallBars}>
                  <View className={classnames(styles.wallBar, styles.wallBar1)} />
                  <View className={classnames(styles.wallBar, styles.wallBar2)} />
                  <View className={classnames(styles.wallBar, styles.wallBar3)} />
                </View>
                <Text className={styles.wallText}>36.6</Text>
              </View>
              <View className={styles.wallDot} />
            </View>
            <View className={styles.base} />
          </View>
        );
      case 'pcb':
        return (
          <View className={styles.stage}>
            <View className={styles.pcbBoard}>
              <View className={styles.pcbScreen}>
                <Text className={styles.pcbText}>36.6</Text>
              </View>
              <View className={styles.pcbChip1} />
              <View className={styles.pcbChip2} />
              <View className={styles.pcbLed} />
            </View>
          </View>
        );
      case 'trigger':
        return (
          <View className={styles.stage}>
            <View className={styles.triggerBody}>
              <View className={styles.triggerDot} />
              <View className={styles.triggerSlot} />
            </View>
            <View className={styles.base} />
          </View>
        );
      case 'mainboard':
        return (
          <View className={styles.stage}>
            <View className={styles.mainBoard}>
              <View className={styles.mainChip1} />
              <View className={styles.mainChip2} />
              <View className={styles.mainChip3} />
              <View className={styles.mainPin} />
            </View>
          </View>
        );
      case 'sensor':
        return (
          <View className={styles.stage}>
            <View className={styles.sensorBoard}>
              <View className={styles.sensorDome} />
              <View className={styles.sensorPin} />
            </View>
          </View>
        );
      default:
        return null;
    }
  };

  return (
    <View
      className={classnames(styles.art, className)}
      style={{ fontSize: `${Math.round(size * 0.16)}rpx` }}
    >
      {renderArt()}
    </View>
  );
};

export default ProductArt;

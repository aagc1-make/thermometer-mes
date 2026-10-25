import React from 'react';
import { View, Text } from '@tarojs/components';
import PageHeader from '@/components/PageHeader';
import Icon from '@/components/Icon';
import styles from './index.module.scss';

interface PlaceholderProps {
  title: string;
  tip?: string;
  showBack?: boolean;
}

/** 二级页面占位视图 */
const Placeholder: React.FC<PlaceholderProps> = ({ title, tip, showBack = true }) => {
  return (
    <View className={styles.page}>
      <PageHeader title={title} showBack={showBack} />
      <View className={styles.body}>
        <View className={styles.iconWrap}>
          <Icon name="help" size={56} color="#2b6beb" />
        </View>
        <Text className={styles.text}>{tip || '功能正在开发中...'}</Text>
      </View>
    </View>
  );
};

export default Placeholder;

import React, { useEffect, useState } from 'react';
import { View, Text } from '@tarojs/components';
import Taro from '@tarojs/taro';
import classnames from 'classnames';
import Icon from '@/components/Icon';
import type { IconName } from '@/components/Icon';
import styles from './index.module.scss';

interface PageHeaderProps {
  title: string;
  /** 显示返回按钮 */
  showBack?: boolean;
  /** 右侧文字按钮（如：管理） */
  rightText?: string;
  /** 右侧图标按钮（如：铃铛、搜索、设置） */
  rightIcon?: IconName;
  onRightClick?: () => void;
  className?: string;
}

const PageHeader: React.FC<PageHeaderProps> = ({
  title,
  showBack = false,
  rightText,
  rightIcon,
  onRightClick,
  className
}) => {
  const [statusBarHeight, setStatusBarHeight] = useState(0);

  useEffect(() => {
    try {
      const info = Taro.getSystemInfoSync();
      setStatusBarHeight(info.statusBarHeight || 0);
    } catch (error) {
      console.error('[Header] 获取状态栏高度失败', error);
    }
  }, []);

  const handleBack = () => {
    Taro.navigateBack().catch((error) => {
      console.error('[Header] 返回失败', error);
      Taro.switchTab({ url: '/pages/home/index' });
    });
  };

  return (
    <View className={classnames(styles.header, className)} style={{ paddingTop: `${statusBarHeight}px` }}>
      <View className={styles.bar}>
        {showBack && (
          <View className={styles.left} onClick={handleBack}>
            <Icon name="back" size={36} color="#17233d" />
          </View>
        )}
        <Text className={styles.title}>{title}</Text>
        {(rightText || rightIcon) && (
          <View className={styles.right} onClick={onRightClick}>
            {rightIcon && (
              <Icon
                name={rightIcon}
                size={rightText ? 30 : 40}
                color={rightText ? '#8a99b3' : '#17233d'}
                className={rightText ? styles.rightIcon : undefined}
              />
            )}
            {rightText && <Text className={styles.rightText}>{rightText}</Text>}
          </View>
        )}
      </View>
    </View>
  );
};

export default PageHeader;

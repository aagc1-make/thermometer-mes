import React from 'react';
import { View, Text } from '@tarojs/components';
import classnames from 'classnames';
import styles from './index.module.scss';

export type IconName =
  | 'search'
  | 'bell'
  | 'sliders'
  | 'plus'
  | 'minus'
  | 'chevronRight'
  | 'chevronDown'
  | 'back'
  | 'doc'
  | 'truck'
  | 'headset'
  | 'clock'
  | 'box'
  | 'check'
  | 'pin'
  | 'users'
  | 'card'
  | 'help'
  | 'edit'
  | 'trash'
  | 'close'
  | 'copy';

interface IconProps {
  name: IconName;
  /** 图标尺寸（rpx），默认 40 */
  size?: number;
  /** 图标颜色，默认继承父级文字颜色 */
  color?: string;
  className?: string;
}

const Icon: React.FC<IconProps> = ({ name, size = 40, color, className }) => {
  // H5 不支持内联 rpx，按 375 设计稿换算为 px；小程序端仍使用 rpx
  const dimension = process.env.TARO_ENV === 'h5' ? `${size / 2}px` : `${size}rpx`;

  const renderShape = () => {
    switch (name) {
      case 'search':
        return (
          <>
            <View className={styles.searchCircle} />
            <View className={styles.searchHandle} />
          </>
        );
      case 'bell':
        return (
          <>
            <View className={styles.bellBody} />
            <View className={styles.bellBase} />
            <View className={styles.bellClapper} />
          </>
        );
      case 'sliders':
        return (
          <>
            <View className={classnames(styles.sliderLine, styles.sliderLineTop)} />
            <View className={classnames(styles.sliderLine, styles.sliderLineBottom)} />
            <View className={classnames(styles.sliderKnob, styles.sliderKnobTop)} />
            <View className={classnames(styles.sliderKnob, styles.sliderKnobBottom)} />
          </>
        );
      case 'plus':
        return (
          <>
            <View className={styles.plusH} />
            <View className={styles.plusV} />
          </>
        );
      case 'minus':
        return <View className={styles.plusH} />;
      case 'chevronRight':
        return <View className={styles.chevronRight} />;
      case 'chevronDown':
        return <View className={styles.chevronDown} />;
      case 'back':
        return <View className={styles.chevronLeft} />;
      case 'doc':
        return (
          <>
            <View className={styles.docBody} />
            <View className={classnames(styles.docLine, styles.docLine1)} />
            <View className={classnames(styles.docLine, styles.docLine2)} />
            <View className={classnames(styles.docLine, styles.docLine3)} />
          </>
        );
      case 'truck':
        return (
          <>
            <View className={styles.truckBox} />
            <View className={styles.truckCab} />
            <View className={classnames(styles.truckWheel, styles.truckWheelLeft)} />
            <View className={classnames(styles.truckWheel, styles.truckWheelRight)} />
          </>
        );
      case 'headset':
        return (
          <>
            <View className={styles.headsetArc} />
            <View className={classnames(styles.headsetPad, styles.headsetPadLeft)} />
            <View className={classnames(styles.headsetPad, styles.headsetPadRight)} />
          </>
        );
      case 'clock':
        return (
          <>
            <View className={styles.clockRing} />
            <View className={styles.clockHour} />
            <View className={styles.clockMinute} />
          </>
        );
      case 'box':
        return (
          <>
            <View className={styles.boxBody} />
            <View className={styles.boxLine} />
            <View className={styles.boxLid} />
          </>
        );
      case 'check':
        return (
          <>
            <View className={styles.checkRing} />
            <View className={styles.checkLine1} />
            <View className={styles.checkLine2} />
          </>
        );
      case 'pin':
        return (
          <>
            <View className={styles.pinRing} />
            <View className={styles.pinDot} />
            <View className={styles.pinTail} />
          </>
        );
      case 'users':
        return (
          <>
            <View className={styles.userHead1} />
            <View className={styles.userBody1} />
            <View className={styles.userHead2} />
            <View className={styles.userBody2} />
          </>
        );
      case 'card':
        return (
          <>
            <View className={styles.cardBody} />
            <View className={styles.cardLine} />
            <View className={styles.cardChip} />
          </>
        );
      case 'help':
        return (
          <>
            <View className={styles.helpRing} />
            <Text className={styles.helpText}>?</Text>
          </>
        );
      case 'edit':
        return (
          <>
            <View className={styles.editBody} />
            <View className={styles.editTip} />
          </>
        );
      case 'trash':
        return (
          <>
            <View className={styles.trashHandle} />
            <View className={styles.trashLid} />
            <View className={styles.trashBody} />
            <View className={classnames(styles.trashLine, styles.trashLineLeft)} />
            <View className={classnames(styles.trashLine, styles.trashLineRight)} />
          </>
        );
      case 'close':
        return (
          <>
            <View className={styles.closeLine1} />
            <View className={styles.closeLine2} />
          </>
        );
      case 'copy':
        return (
          <>
            <View className={styles.copyBack} />
            <View className={styles.copyFront} />
          </>
        );
      default:
        return null;
    }
  };

  return (
    <View
      className={classnames(styles.icon, className)}
      style={{ width: dimension, height: dimension, color }}
    >
      {renderShape()}
    </View>
  );
};

export default Icon;

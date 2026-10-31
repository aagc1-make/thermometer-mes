import React, { useEffect, useState } from 'react';
import { View, Text } from '@tarojs/components';
import Taro, { useRouter } from '@tarojs/taro';
import classnames from 'classnames';
import PageHeader from '@/components/PageHeader';
import Icon from '@/components/Icon';
import ProductArt from '@/components/ProductArt';
import { createOrderFromInquiry, fetchInquiryRecord } from '@/services/api';
import { useInquiryStore } from '@/store/inquiry';
import type { InquiryRecord } from '@/types';
import styles from './index.module.scss';

const InquiryDetailPage: React.FC = () => {
  const router = useRouter();
  const recordId = router.params.id || '';
  const submitted = useInquiryStore((state) => state.submitted);
  const quoted = useInquiryStore((state) => state.quoted);
  const cancelRecord = useInquiryStore((state) => state.cancelRecord);
  const localRecord = [...submitted, ...quoted].find((item) => item.id === recordId);
  const [record, setRecord] = useState<InquiryRecord | undefined>(localRecord);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const load = async () => {
      if (!recordId) {
        return;
      }
      const remote = await fetchInquiryRecord(recordId);
      if (remote) {
        setRecord(remote);
      }
    };
    load();
  }, [recordId]);

  if (!record) {
    return (
      <View className={styles.page}>
        <PageHeader title="询价单详情" showBack />
        <View className={styles.empty}>
          <Text className={styles.emptyText}>询价单不存在</Text>
        </View>
      </View>
    );
  }

  const isQuoted = record.status === 'quoted';

  const handleCancel = async () => {
    try {
      const res = await Taro.showModal({
        title: '撤销询价单',
        content: `撤销后 ${record.id} 将置为已取消，已报价的询价单不可撤销。`,
        confirmText: '撤销',
        confirmColor: '#f04438'
      });
      if (!res.confirm) return;
      await cancelRecord(record.id);
      Taro.showToast({ title: '询价单已撤销', icon: 'none' });
      setTimeout(() => Taro.navigateBack(), 400);
    } catch (error) {
      console.info('[Inquiry] 取消撤销操作', error);
    }
  };

  const handleConfirmOrder = async () => {
    if (submitting) {
      return;
    }
    const confirm = await Taro.showModal({
      title: '确认下单',
      content: `将以报价金额 ¥${(record.quoteAmount || 0).toLocaleString()} 生成正式订单，销售会同步排产计划。`,
      confirmText: '确认下单',
      confirmColor: '#2b6beb'
    });
    if (!confirm.confirm) {
      return;
    }
    setSubmitting(true);
    try {
      const order = await createOrderFromInquiry(record.id);
      Taro.showToast({ title: '订单已生成', icon: 'success' });
      setTimeout(() => {
        Taro.navigateTo({ url: `/pages/order-detail/index?orderNo=${order.orderNo}` });
      }, 600);
    } catch (error) {
      console.info('[Inquiry] 下单失败', error);
      Taro.showToast({ title: '下单失败，请联系专属销售', icon: 'none' });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <View className={styles.page}>
      <PageHeader title="询价单详情" showBack />

      <View className={styles.card}>
        <View className={styles.head}>
          <Text className={styles.id}>{record.id}</Text>
          <View className={classnames(styles.tag, isQuoted && styles.tagQuoted)}>
            <Text className={classnames(styles.tagText, isQuoted && styles.tagTextQuoted)}>
              {isQuoted ? '已报价' : '待报价'}
            </Text>
          </View>
        </View>
        <View className={styles.dashed} />
        {record.items.map((item) => (
          <View key={`${record.id}-${item.productId}-${item.code}`} className={styles.itemRow}>
            <View className={styles.thumb}>
              <ProductArt art={item.art} itemCode={item.code} size={96} />
            </View>
            <View className={styles.info}>
              <Text className={styles.name}>{item.name}</Text>
              <Text className={styles.meta}>
                {item.code} · {item.categoryName}
              </Text>
            </View>
            <Text className={styles.qty}>×{item.qty}</Text>
          </View>
        ))}
      </View>

      <View className={styles.card}>
        <View className={styles.infoRow}>
          <Text className={styles.label}>提交时间</Text>
          <Text className={styles.value}>{record.submitTime || '—'}</Text>
        </View>
        <View className={styles.divider} />
        <View className={styles.infoRow}>
          <Text className={styles.label}>期望交期</Text>
          <Text className={styles.value}>{record.expectedDate || '待确认'}</Text>
        </View>
        <View className={styles.divider} />
        <View className={styles.infoRow}>
          <Text className={styles.label}>收货地址</Text>
          <Text className={styles.value}>{record.address || '—'}</Text>
        </View>
        <View className={styles.divider} />
        <View className={styles.infoRow}>
          <Text className={styles.label}>开票方式</Text>
          <Text className={styles.value}>{record.invoice || '—'}</Text>
        </View>
        <View className={styles.divider} />
        <View className={styles.infoRow}>
          <Text className={styles.label}>备注</Text>
          <Text className={styles.value}>{record.remark || '—'}</Text>
        </View>
      </View>

      <View className={styles.totalCard}>
        <View className={styles.totalLeft}>
          <Text className={styles.totalText}>
            {record.itemCount} 个品项 · 共 {record.totalQty} 件
          </Text>
          {isQuoted && record.quoteAmount ? (
            <Text className={styles.quote}>报价金额 ¥{record.quoteAmount.toLocaleString()}</Text>
          ) : (
            <Text className={styles.totalTip}>销售将在 2 小时内联系确认单价与交期</Text>
          )}
        </View>
        {isQuoted ? (
          <View className={classnames(styles.actionBtn, submitting && styles.actionDisabled)} onClick={handleConfirmOrder}>
            <Icon name="check" size={24} color="#ffffff" className={styles.btnIcon} />
            <Text className={styles.actionText}>{submitting ? '下单中...' : '确认下单'}</Text>
          </View>
        ) : (
          <View className={styles.callBtn} onClick={() => Taro.navigateTo({ url: '/pages/sales/index' })}>
            <Icon name="headset" size={30} color="#2b6beb" />
            <Text className={styles.callText}>联系销售</Text>
          </View>
        )}
      </View>

      {!isQuoted && (
        <View className={styles.cancelRow} onClick={handleCancel}>
          <Text className={styles.cancelText}>撤销该询价单</Text>
        </View>
      )}
    </View>
  );
};

export default InquiryDetailPage;

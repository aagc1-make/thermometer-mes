import React, { useEffect, useState } from 'react';
import { Text, View } from '@tarojs/components';
import Taro, { useRouter } from '@tarojs/taro';
import classnames from 'classnames';
import PageHeader from '@/components/PageHeader';
import Icon from '@/components/Icon';
import ProductArt from '@/components/ProductArt';
import { fetchOrder } from '@/services/api';
import { useCatalogStore } from '@/store/catalog';
import { useInquiryStore } from '@/store/inquiry';
import type { Order, OrderStatus } from '@/types';
import styles from './index.module.scss';

const STEPS: { key: OrderStatus; name: string }[] = [
  { key: 'quoted', name: '待报价' },
  { key: 'confirmed', name: '待确认' },
  { key: 'producing', name: '生产中' },
  { key: 'delivered', name: '已交付' }
];

const OrderDetailPage: React.FC = () => {
  const router = useRouter();
  const orderNo = router.params.orderNo || '';
  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      const detail = await fetchOrder(orderNo);
      setOrder(detail);
      setLoading(false);
    };
    load();
  }, [orderNo]);

  if (loading) {
    return (
      <View className={styles.page}>
        <PageHeader title="订单详情" showBack />
        <View className={styles.empty}>
          <Text className={styles.emptyText}>加载中...</Text>
        </View>
      </View>
    );
  }

  if (!order) {
    return (
      <View className={styles.page}>
        <PageHeader title="订单详情" showBack />
        <View className={styles.empty}>
          <Text className={styles.emptyText}>订单不存在或已关闭</Text>
        </View>
      </View>
    );
  }

  const activeIndex = STEPS.findIndex((step) => step.key === order.status);

  const copy = (value: string, tip = '已复制') => {
    Taro.setClipboardData({ data: value });
    Taro.showToast({ title: tip, icon: 'none' });
  };

  const addToInquiry = () => {
    const products = useCatalogStore.getState().products;
    const addItem = useInquiryStore.getState().addItem;
    let count = 0;
    order.lines.forEach((line) => {
      const product = products.find((item) => item.code === line.itemCode);
      if (product) {
        addItem(product, line.qty);
        count += 1;
      }
    });
    if (count === 0) {
      Taro.showToast({ title: '产品目录中未找到对应物料', icon: 'none' });
      return;
    }
    Taro.showToast({ title: `已加入 ${count} 个品项`, icon: 'success' });
    setTimeout(() => Taro.switchTab({ url: '/pages/inquiry/index' }), 600);
  };

  return (
    <View className={styles.page}>
      <PageHeader title="订单详情" showBack />

      <View className={styles.hero}>
        <View className={styles.heroHead}>
          <View>
            <Text className={styles.orderNo}>{order.orderNo}</Text>
            <Text className={styles.orderStatus}>{order.statusName}</Text>
          </View>
          <View className={styles.copyBtn} onClick={() => copy(order.orderNo)}>
            <Icon name="copy" size={20} color="#ffffff" className={styles.copyIcon} />
            <Text className={styles.copyText}>复制单号</Text>
          </View>
        </View>

        <View className={styles.steps}>
          {STEPS.map((step, index) => (
            <View key={step.key} className={styles.step}>
              <View className={styles.stepTop}>
                <View
                  className={classnames(
                    styles.dot,
                    index <= activeIndex && styles.dotActive,
                    index === activeIndex && styles.dotCurrent
                  )}
                >
                  {index < activeIndex && <Icon name="check" size={20} color="#ffffff" />}
                </View>
                {index < STEPS.length - 1 && (
                  <View className={classnames(styles.stepLine, index < activeIndex && styles.stepLineActive)} />
                )}
              </View>
              <Text className={classnames(styles.stepName, index <= activeIndex && styles.stepNameActive)}>
                {step.name}
              </Text>
            </View>
          ))}
        </View>
      </View>

      <View className={styles.card}>
        <View className={styles.cardHead}>
          <Text className={styles.cardTitle}>产品明细</Text>
          <Text className={styles.cardMeta}>
            {order.itemCount} 个品项 · 共 {order.totalQty} 件
          </Text>
        </View>
        {order.lines.map((line) => (
          <View key={`${line.itemCode}-${line.lineNo}`} className={styles.lineRow}>
            <View className={styles.thumb}>
              <ProductArt art={line.art || 'thermometer'} itemCode={line.itemCode} size={96} />
            </View>
            <View className={styles.lineInfo}>
              <Text className={styles.lineName}>{line.itemName}</Text>
              <Text className={styles.lineMeta}>
                {line.itemCode} · {line.specification || '规格以合同为准'}
              </Text>
              <Text className={styles.linePrice}>
                {line.unitPrice > 0
                  ? `¥${line.unitPrice.toLocaleString()} / ${line.unitName}`
                  : '单价待销售确认'}
              </Text>
            </View>
            <View className={styles.lineRight}>
              <Text className={styles.lineQty}>×{line.qty}</Text>
              <Text className={styles.lineAmount}>
                {line.amount > 0 ? `¥${line.amount.toLocaleString()}` : '—'}
              </Text>
            </View>
          </View>
        ))}
        <View className={styles.totalRow}>
          <Text className={styles.totalLabel}>订单金额</Text>
          <Text className={styles.totalValue}>
            {order.totalAmount > 0 ? `¥${order.totalAmount.toLocaleString()}` : '待报价'}
          </Text>
        </View>
      </View>

      {order.delivery && (
        <View className={styles.card}>
          <View className={styles.cardHead}>
            <Text className={styles.cardTitle}>交付进度</Text>
            <Text className={styles.deliveryStatus}>{order.delivery.statusName}</Text>
          </View>
          <View className={styles.progressBar}>
            <View className={styles.progressFill} style={{ width: `${order.delivery.progress}%` }} />
          </View>
          <Text className={styles.progressText}>
            已完成 {order.delivery.progress}% · 已发货 {order.delivery.shippedQty} / {order.delivery.totalQty}
          </Text>
          {order.delivery.carrier && (
            <View className={styles.deliveryRow} onClick={() => copy(order.delivery?.trackingNo || '')}>
              <Text className={styles.deliveryLabel}>{order.delivery.carrier}</Text>
              <Text className={styles.deliveryValue}>{order.delivery.trackingNo}</Text>
              <Text className={styles.deliveryCopy}>复制</Text>
            </View>
          )}
        </View>
      )}

      <View className={styles.card}>
        <View className={styles.infoRow}>
          <Text className={styles.infoLabel}>下单时间</Text>
          <Text className={styles.infoValue}>{order.createTime}</Text>
        </View>
        <View className={styles.divider} />
        <View className={styles.infoRow}>
          <Text className={styles.infoLabel}>期望交期</Text>
          <Text className={styles.infoValue}>{order.expectedDate || '待确认'}</Text>
        </View>
        <View className={styles.divider} />
        <View className={styles.infoRow}>
          <Text className={styles.infoLabel}>收货地址</Text>
          <Text className={styles.infoValue}>{order.deliveryAddress}</Text>
        </View>
        <View className={styles.divider} />
        <View className={styles.infoRow}>
          <Text className={styles.infoLabel}>开票方式</Text>
          <Text className={styles.infoValue}>{order.invoiceType}</Text>
        </View>
        {order.inquiryNo && (
          <>
            <View className={styles.divider} />
            <View className={styles.infoRow}>
              <Text className={styles.infoLabel}>来源询价单</Text>
              <Text className={styles.infoValue}>{order.inquiryNo}</Text>
            </View>
          </>
        )}
        <View className={styles.divider} />
        <View className={styles.infoRow}>
          <Text className={styles.infoLabel}>备注</Text>
          <Text className={styles.infoValue}>{order.remark || '—'}</Text>
        </View>
      </View>

      <View className={styles.actions}>
        <View className={styles.ghostBtn} onClick={() => Taro.navigateTo({ url: '/pages/sales/index' })}>
          <Icon name="headset" size={24} color="#2b6beb" className={styles.btnIcon} />
          联系销售
        </View>
        <View
          className={styles.ghostBtn}
          onClick={() => Taro.navigateTo({ url: `/pages/delivery/index?orderNo=${order.orderNo}` })}
        >
          <Icon name="truck" size={24} color="#2b6beb" className={styles.btnIcon} />
          交付进度
        </View>
        <View className={styles.primaryBtn} onClick={addToInquiry}>
          <Icon name="doc" size={24} color="#ffffff" className={styles.btnIcon} />
          再次询价
        </View>
      </View>
    </View>
  );
};

export default OrderDetailPage;

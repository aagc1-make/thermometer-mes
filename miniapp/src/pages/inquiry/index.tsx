import React, { useState } from 'react';
import { View, Text } from '@tarojs/components';
import Taro, { useDidShow } from '@tarojs/taro';
import classnames from 'classnames';
import PageHeader from '@/components/PageHeader';
import Icon from '@/components/Icon';
import ProductArt from '@/components/ProductArt';
import { fetchInquiryRecords } from '@/services/api';
import { useInquiryStore } from '@/store/inquiry';
import { syncInquiryBadge } from '@/utils/tabbar';
import type { InquiryRecord } from '@/types';
import styles from './index.module.scss';

type TabKey = 'pending' | 'submitted' | 'quoted';

interface InfoRowProps {
  label: string;
  value: string;
  onClick?: () => void;
}

const InfoRow: React.FC<InfoRowProps> = ({ label, value, onClick }) => (
  <View className={styles.infoRow} onClick={onClick}>
    <Text className={styles.infoLabel}>{label}</Text>
    <Text className={styles.infoValue}>{value}</Text>
    <Icon name="chevronRight" size={18} color="#c3ccdd" />
  </View>
);

const InquiryPage: React.FC = () => {
  const [tab, setTab] = useState<TabKey>('pending');
  const [remoteRecords, setRemoteRecords] = useState<InquiryRecord[]>([]);
  const items = useInquiryStore((state) => state.items);
  const meta = useInquiryStore((state) => state.meta);
  const draftId = useInquiryStore((state) => state.draftId);
  const submitted = useInquiryStore((state) => state.submitted);
  const quoted = useInquiryStore((state) => state.quoted);
  const setQty = useInquiryStore((state) => state.setQty);
  const clearDraft = useInquiryStore((state) => state.clearDraft);
  const submit = useInquiryStore((state) => state.submit);

  const refresh = async () => {
    const records = await fetchInquiryRecords();
    setRemoteRecords(records);
  };

  useDidShow(() => {
    syncInquiryBadge(useInquiryStore.getState().items.length);
    refresh();
  });

  const totalQty = items.reduce((sum, item) => sum + item.qty, 0);

  const handleSubmit = async () => {
    if (items.length === 0) {
      Taro.showToast({ title: '询价单暂无商品', icon: 'none' });
      return;
    }
    const result = await Taro.showModal({
      title: '提交询价',
      content: `共 ${items.length} 个品项、${totalQty} 件，提交后销售将在 2 小时内联系确认单价与交期。`,
      confirmText: '确认提交',
      confirmColor: '#2b6beb'
    });
    if (!result.confirm) {
      return;
    }
    const record = await submit();
    if (!record) {
      return;
    }
    syncInquiryBadge(useInquiryStore.getState().items.length);
    await refresh();
    setTab('submitted');
    Taro.showToast({ title: '询价单已提交', icon: 'success' });
  };

  const handleManage = () => {
    if (tab !== 'pending') {
      Taro.showToast({ title: '历史询价单由销售统一管理', icon: 'none' });
      return;
    }
    if (items.length === 0) {
      Taro.showToast({ title: '询价单暂无商品', icon: 'none' });
      return;
    }
    Taro.showActionSheet({
      itemList: ['清空询价单'],
      itemColor: '#f04438'
    })
      .then((res) => {
        if (res.tapIndex === 0) {
          clearDraft();
          syncInquiryBadge(0);
          Taro.showToast({ title: '已清空', icon: 'none' });
        }
      })
      .catch((error) => {
        console.info('[Inquiry] 取消管理操作', error);
      });
  };

  const openRecord = (record: InquiryRecord) => {
    Taro.navigateTo({ url: `/pages/inquiry-detail/index?id=${record.id}` });
  };

  const remoteSubmitted = remoteRecords.filter((item) => item.status === 'submitted');
  const remoteQuoted = remoteRecords.filter((item) => item.status === 'quoted');
  // 后端有数据时以真实询价单为准，后端不可用时回退本地演示记录
  const submittedList = remoteRecords.length > 0 ? remoteSubmitted : submitted;
  const quotedList = remoteRecords.length > 0 ? remoteQuoted : quoted;

  const tabItems: { key: TabKey; name: string; count: number }[] = [
    { key: 'pending', name: '待提交', count: items.length },
    { key: 'submitted', name: '已提交', count: submittedList.length },
    { key: 'quoted', name: '已报价', count: quotedList.length }
  ];

  const renderRecords = (records: InquiryRecord[], emptyText: string) => (
    <View className={styles.records}>
      {records.map((record) => (
        <View key={record.id} className={styles.recordCard} onClick={() => openRecord(record)}>
          <View className={styles.recordHead}>
            <Text className={styles.recordId}>{record.id}</Text>
            <View className={classnames(styles.statusTag, record.status === 'quoted' && styles.statusTagQuoted)}>
              <Text
                className={classnames(
                  styles.statusText,
                  record.status === 'quoted' && styles.statusTextQuoted
                )}
              >
                {record.status === 'quoted' ? '已报价' : '待报价'}
              </Text>
            </View>
          </View>
          {record.items.map((item) => (
            <View key={item.productId} className={styles.recordItem}>
              <View className={styles.recordThumb}>
                <ProductArt art={item.art} size={96} />
              </View>
              <View className={styles.recordInfo}>
                <Text className={styles.recordName}>{item.name}</Text>
                <Text className={styles.recordMeta}>
                  {item.code} · {item.categoryName}
                </Text>
              </View>
              <Text className={styles.recordQty}>×{item.qty}</Text>
            </View>
          ))}
          <View className={styles.recordFoot}>
            <Text className={styles.recordFootText}>
              {record.itemCount} 个品项 · 共 {record.totalQty} 件
            </Text>
            <Text className={styles.recordTime}>{record.submitTime} 提交</Text>
          </View>
        </View>
      ))}
      {records.length === 0 && (
        <View className={styles.empty}>
          <Text className={styles.emptyText}>{emptyText}</Text>
        </View>
      )}
    </View>
  );

  return (
    <View className={styles.page}>
      <PageHeader title="询价单" rightText="管理" rightIcon="sliders" onRightClick={handleManage} />

      <View className={styles.tabs}>
        {tabItems.map((item) => (
          <View key={item.key} className={styles.tabItem} onClick={() => setTab(item.key)}>
            <Text className={classnames(styles.tabText, tab === item.key && styles.tabTextActive)}>
              {item.name}
            </Text>
            <Text className={classnames(styles.tabCount, tab === item.key && styles.tabCountActive)}>
              {item.count}
            </Text>
            {tab === item.key && <View className={styles.tabLine} />}
          </View>
        ))}
      </View>

      <View className={classnames(styles.content, tab === 'pending' && styles.contentWithBar)}>
        {tab === 'pending' && (
          <>
            {items.length > 0 ? (
              <>
                <View className={styles.draftCard}>
                  <View className={styles.draftHead}>
                    <Text className={styles.draftId}>{draftId}</Text>
                    <View className={styles.editable}>
                      <Text className={styles.editableText}>可修改</Text>
                    </View>
                  </View>
                  <View className={styles.dashedLine} />
                  {items.map((item) => (
                    <View key={item.productId} className={styles.itemRow}>
                      <View className={styles.itemThumb}>
                        <ProductArt art={item.art} size={96} />
                      </View>
                      <View className={styles.itemInfo}>
                        <Text className={styles.itemName}>{item.name}</Text>
                        <Text className={styles.itemMeta}>
                          {item.code} · {item.categoryName}
                        </Text>
                      </View>
                      <View className={styles.stepper}>
                        <View
                          className={styles.stepBtn}
                          onClick={() => setQty(item.productId, item.qty - 10)}
                        >
                          <Icon name="minus" size={18} color="#5a6b87" />
                        </View>
                        <Text className={styles.stepQty}>{item.qty}</Text>
                        <View
                          className={styles.stepBtn}
                          onClick={() => setQty(item.productId, item.qty + 10)}
                        >
                          <Icon name="plus" size={18} color="#2b6beb" />
                        </View>
                      </View>
                    </View>
                  ))}
                </View>

                <View className={styles.infoCard}>
                  <InfoRow
                    label="期望交期"
                    value={meta.expectedDate}
                    onClick={() => Taro.showToast({ title: '交期将在报价时确认', icon: 'none' })}
                  />
                  <View className={styles.infoDivider} />
                  <InfoRow
                    label="收货地址"
                    value={meta.address}
                    onClick={() => Taro.navigateTo({ url: '/pages/address/index' })}
                  />
                  <View className={styles.infoDivider} />
                  <InfoRow
                    label="开票方式"
                    value={meta.invoice}
                    onClick={() => Taro.navigateTo({ url: '/pages/invoice/index' })}
                  />
                  <View className={styles.infoDivider} />
                  <InfoRow
                    label="备注"
                    value={meta.remark}
                    onClick={() => Taro.showToast({ title: '备注将在报价时确认', icon: 'none' })}
                  />
                </View>

                <View className={styles.hint}>
                  <Text className={styles.hintText}>
                    提交后销售将在 2 小时内联系确认单价与交期；报价单有效期 7 天，确认后转为正式订单并生成生产工单。
                  </Text>
                </View>
              </>
            ) : (
              <View className={styles.emptyCard}>
                <View className={styles.emptyIcon}>
                  <Icon name="doc" size={48} color="#2b6beb" />
                </View>
                <Text className={styles.emptyTitle}>询价单还是空的</Text>
                <Text className={styles.emptyDesc}>去产品目录挑选成品或半成品，加入询价单</Text>
                <View
                  className={styles.emptyBtn}
                  onClick={() => Taro.switchTab({ url: '/pages/product/index' })}
                >
                  <Icon name="plus" size={24} color="#ffffff" className={styles.btnIcon} />
                  <Text className={styles.emptyBtnText}>去选择产品</Text>
                </View>
              </View>
            )}
          </>
        )}

        {tab === 'submitted' && renderRecords(submittedList, '暂无已提交的询价单')}
        {tab === 'quoted' && renderRecords(quotedList, '暂无已报价的询价单')}
      </View>

      {tab === 'pending' && items.length > 0 && (
        <View className={styles.submitBar}>
          <View className={styles.submitInner}>
            <View className={styles.submitSummary}>
              <Text className={styles.submitTotal}>
                {items.length} 个品项 · 共 {totalQty} 件
              </Text>
              <Text className={styles.submitTip}>预计交期 7 个工作日</Text>
            </View>
            <View className={styles.submitBtn} onClick={handleSubmit}>
              <Icon name="doc" size={26} color="#ffffff" className={styles.btnIcon} />
              <Text className={styles.submitBtnText}>提交询价</Text>
            </View>
          </View>
        </View>
      )}
    </View>
  );
};

export default InquiryPage;

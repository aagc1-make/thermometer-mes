import React, { useEffect, useState } from 'react';
import { Input, Picker, Text, View } from '@tarojs/components';
import Taro from '@tarojs/taro';
import classnames from 'classnames';
import dayjs from 'dayjs';
import PageHeader from '@/components/PageHeader';
import Icon from '@/components/Icon';
import { applyInvoice, fetchInvoiceProfile, fetchInvoices } from '@/services/api';
import type { Invoice, InvoiceProfile } from '@/types';
import styles from './index.module.scss';

const INVOICE_TYPES = ['增值税专用发票 13%', '增值税普通发票 6%', '增值税普通发票 3%'];

const InvoicePage: React.FC = () => {
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [profile, setProfile] = useState<InvoiceProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [formVisible, setFormVisible] = useState(false);
  const [amount, setAmount] = useState('');
  const [typeIndex, setTypeIndex] = useState(0);
  const [email, setEmail] = useState('');
  const [remark, setRemark] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const load = async () => {
    setLoading(true);
    const [list, info] = await Promise.all([fetchInvoices(), fetchInvoiceProfile()]);
    setInvoices(list);
    setProfile(info);
    setEmail(info.email || '');
    setLoading(false);
  };

  useEffect(() => {
    load();
  }, []);

  const openForm = () => {
    setFormVisible((prev) => !prev);
  };

  const handleApply = async () => {
    const value = Number(amount);
    if (!Number.isFinite(value) || value <= 0) {
      Taro.showToast({ title: '请输入正确的开票金额', icon: 'none' });
      return;
    }
    if (submitting) {
      return;
    }
    setSubmitting(true);
    const invoiceType = INVOICE_TYPES[typeIndex];
    try {
      const created = await applyInvoice({
        amount: value,
        invoiceType,
        email: email.trim() || undefined,
        remark: remark.trim() || undefined
      });
      setInvoices((prev) => [created, ...prev]);
      Taro.showToast({ title: '开票申请已提交', icon: 'success' });
    } catch (error) {
      console.info('[Invoice] 后端不可用，本地记录开票申请', error);
      const local: Invoice = {
        invoiceNo: `FP${dayjs().format('YYYYMMDD')}-${String(invoices.length + 1).padStart(3, '0')}`,
        invoiceTitle: profile?.invoiceTitle || '华瑞医疗器械有限公司',
        taxNo: profile?.taxNo || '',
        invoiceType,
        amount: value,
        status: 'applied',
        statusName: '开票中',
        applyTime: dayjs().format('YYYY-MM-DD HH:mm'),
        email: email.trim() || undefined,
        remark: remark.trim() || undefined
      };
      setInvoices((prev) => [local, ...prev]);
      Taro.showToast({ title: '已本地记录开票申请', icon: 'none' });
    } finally {
      setSubmitting(false);
      setAmount('');
      setRemark('');
      setFormVisible(false);
    }
  };

  const copyNo = (invoiceNo: string) => {
    Taro.setClipboardData({ data: invoiceNo });
    Taro.showToast({ title: '发票单号已复制', icon: 'none' });
  };

  return (
    <View className={styles.page}>
      <PageHeader title="开票信息" showBack rightText={formVisible ? '收起' : '申请开票'} onRightClick={openForm} />

      {profile && (
        <View className={styles.profileCard}>
          <View className={styles.profileHead}>
            <View className={styles.profileIcon}>
              <Icon name="doc" size={36} color="#2b6beb" />
            </View>
            <View className={styles.profileInfo}>
              <Text className={styles.profileTitle}>{profile.invoiceTitle}</Text>
              <Text className={styles.profileTax}>税号 {profile.taxNo}</Text>
            </View>
            <Text className={styles.profileTag}>默认</Text>
          </View>
          <View className={styles.profileDivider} />
          <View className={styles.profileRow}>
            <Text className={styles.profileLabel}>发票类型</Text>
            <Text className={styles.profileValue}>{profile.invoiceType}</Text>
          </View>
          <View className={styles.profileRow}>
            <Text className={styles.profileLabel}>接收邮箱</Text>
            <Text className={styles.profileValue}>{profile.email || '未设置'}</Text>
          </View>
          <Text className={styles.profileTip}>开票信息如需变更，请联系专属销售提交工商变更资料。</Text>
        </View>
      )}

      {formVisible && (
        <View className={styles.formCard}>
          <Text className={styles.formTitle}>开票申请</Text>
          <View className={styles.field}>
            <Text className={styles.fieldLabel}>开票金额</Text>
            <Input
              className={styles.input}
              type="digit"
              value={amount}
              placeholder="请输入金额（元）"
              onInput={(event) => setAmount(event.detail.value)}
            />
          </View>
          <Picker
            mode="selector"
            range={INVOICE_TYPES}
            value={typeIndex}
            onChange={(event) => setTypeIndex(Number(event.detail.value))}
          >
            <View className={styles.field}>
              <Text className={styles.fieldLabel}>发票类型</Text>
              <Text className={styles.pickerValue}>{INVOICE_TYPES[typeIndex]}</Text>
              <Icon name="chevronRight" size={20} color="#c3ccdd" />
            </View>
          </Picker>
          <View className={styles.field}>
            <Text className={styles.fieldLabel}>接收邮箱</Text>
            <Input
              className={styles.input}
              value={email}
              placeholder="用于接收电子发票"
              onInput={(event) => setEmail(event.detail.value)}
            />
          </View>
          <View className={styles.field}>
            <Text className={styles.fieldLabel}>备注</Text>
            <Input
              className={styles.input}
              value={remark}
              placeholder="如：订单号 / 邮寄要求"
              onInput={(event) => setRemark(event.detail.value)}
            />
          </View>
          <View className={classnames(styles.submitBtn, submitting && styles.submitDisabled)} onClick={handleApply}>
            <Icon name="check" size={24} color="#ffffff" className={styles.btnIcon} />
            <Text className={styles.submitText}>{submitting ? '提交中...' : '提交开票申请'}</Text>
          </View>
        </View>
      )}

      <View className={styles.listHead}>
        <Text className={styles.listTitle}>开票记录</Text>
        <Text className={styles.listCount}>{invoices.length} 条</Text>
      </View>

      {loading && (
        <View className={styles.empty}>
          <Text className={styles.emptyText}>加载中...</Text>
        </View>
      )}

      {!loading &&
        invoices.map((item) => (
          <View key={item.invoiceNo} className={styles.card}>
            <View className={styles.cardHead}>
              <View className={styles.invoiceNoWrap} onClick={() => copyNo(item.invoiceNo)}>
                <Text className={styles.invoiceNo}>{item.invoiceNo}</Text>
                <Icon name="copy" size={20} color="#8a99b3" className={styles.copyIcon} />
              </View>
              <Text className={classnames(styles.status, item.status === 'issued' ? styles.statusIssued : styles.statusApplied)}>
                {item.statusName}
              </Text>
            </View>
            <View className={styles.dashed} />
            <View className={styles.row}>
              <Text className={styles.rowLabel}>开票金额</Text>
              <Text className={styles.rowAmount}>¥{item.amount.toLocaleString()}</Text>
            </View>
            <View className={styles.row}>
              <Text className={styles.rowLabel}>发票类型</Text>
              <Text className={styles.rowValue}>{item.invoiceType}</Text>
            </View>
            <View className={styles.row}>
              <Text className={styles.rowLabel}>发票抬头</Text>
              <Text className={styles.rowValue}>{item.invoiceTitle}</Text>
            </View>
            <View className={styles.row}>
              <Text className={styles.rowLabel}>申请时间</Text>
              <Text className={styles.rowValue}>{item.applyTime}</Text>
            </View>
            {item.issueTime && (
              <View className={styles.row}>
                <Text className={styles.rowLabel}>开票时间</Text>
                <Text className={styles.rowValue}>{item.issueTime}</Text>
              </View>
            )}
            {item.remark && <Text className={styles.remark}>{item.remark}</Text>}
          </View>
        ))}

      {!loading && invoices.length === 0 && (
        <View className={styles.emptyCard}>
          <Icon name="doc" size={52} color="#2b6beb" />
          <Text className={styles.emptyTitle}>暂无开票记录</Text>
          <Text className={styles.emptyDesc}>订单发货确认后，可在此提交开票申请</Text>
        </View>
      )}
    </View>
  );
};

export default InvoicePage;

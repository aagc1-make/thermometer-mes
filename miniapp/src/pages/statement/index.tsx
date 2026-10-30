import React, { useEffect, useMemo, useState } from 'react';
import { Text, View } from '@tarojs/components';
import classnames from 'classnames';
import PageHeader from '@/components/PageHeader';
import Icon from '@/components/Icon';
import { fetchStatements } from '@/services/api';
import type { Statement } from '@/types';
import styles from './index.module.scss';

const StatementPage: React.FC = () => {
  const [statements, setStatements] = useState<Statement[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      const list = await fetchStatements();
      setStatements(list);
      setLoading(false);
    };
    load();
  }, []);

  const totals = useMemo(() => {
    return statements.reduce(
      (acc, item) => ({
        total: acc.total + item.totalAmount,
        paid: acc.paid + item.paidAmount,
        unpaid: acc.unpaid + item.unpaidAmount
      }),
      { total: 0, paid: 0, unpaid: 0 }
    );
  }, [statements]);

  return (
    <View className={styles.page}>
      <PageHeader title="对账单" showBack />

      <View className={styles.hero}>
        <Text className={styles.heroLabel}>当前待结金额（元）</Text>
        <Text className={styles.heroValue}>{totals.unpaid.toLocaleString()}</Text>
        <View className={styles.heroMetaRow}>
          <View className={styles.heroMeta}>
            <Text className={styles.heroMetaLabel}>应结</Text>
            <Text className={styles.heroMetaValue}>¥{totals.total.toLocaleString()}</Text>
          </View>
          <View className={styles.heroMetaDivider} />
          <View className={styles.heroMeta}>
            <Text className={styles.heroMetaLabel}>已付</Text>
            <Text className={styles.heroMetaValue}>¥{totals.paid.toLocaleString()}</Text>
          </View>
          <View className={styles.heroMetaDivider} />
          <View className={styles.heroMeta}>
            <Text className={styles.heroMetaLabel}>账期</Text>
            <Text className={styles.heroMetaValue}>月结 30 天</Text>
          </View>
        </View>
      </View>

      <View className={styles.listHead}>
        <Text className={styles.listTitle}>账单明细</Text>
        <Text className={styles.listCount}>{statements.length} 期</Text>
      </View>

      {loading && (
        <View className={styles.empty}>
          <Text className={styles.emptyText}>加载中...</Text>
        </View>
      )}

      {!loading &&
        statements.map((item) => (
          <View key={item.statementNo} className={styles.card}>
            <View className={styles.head}>
              <View>
                <Text className={styles.period}>{item.period}</Text>
                <Text className={styles.statementNo}>对账单号 {item.statementNo}</Text>
              </View>
              <Text className={classnames(styles.status, styles[`status_${item.status}`])}>{item.statusName}</Text>
            </View>

            <View className={styles.amountRow}>
              <View className={styles.amountItem}>
                <Text className={styles.amountLabel}>应结金额</Text>
                <Text className={styles.amountValue}>¥{item.totalAmount.toLocaleString()}</Text>
              </View>
              <View className={styles.amountItem}>
                <Text className={styles.amountLabel}>已付金额</Text>
                <Text className={styles.amountValue}>¥{item.paidAmount.toLocaleString()}</Text>
              </View>
              <View className={styles.amountItem}>
                <Text className={styles.amountLabel}>未付金额</Text>
                <Text className={classnames(styles.amountValue, styles.unpaid)}>
                  ¥{item.unpaidAmount.toLocaleString()}
                </Text>
              </View>
            </View>

            <View className={styles.dashed} />

            <View className={styles.row}>
              <Text className={styles.rowLabel}>出账日期</Text>
              <Text className={styles.rowValue}>{item.billDate}</Text>
            </View>
            <View className={styles.row}>
              <Text className={styles.rowLabel}>到期日期</Text>
              <Text className={styles.rowValue}>{item.dueDate}</Text>
            </View>
            {item.remark && <Text className={styles.remark}>{item.remark}</Text>}
          </View>
        ))}

      {!loading && statements.length === 0 && (
        <View className={styles.emptyCard}>
          <Icon name="card" size={52} color="#2b6beb" />
          <Text className={styles.emptyTitle}>暂无对账单</Text>
          <Text className={styles.emptyDesc}>每月 1 日生成上月对账单，账期月结 30 天</Text>
        </View>
      )}

      <View className={styles.tipCard}>
        <Icon name="help" size={28} color="#2b6beb" />
        <Text className={styles.tipText}>
          对账单金额以实际发货明细为准，如与贵司账面存在差异，请在到期日前联系专属销售核对。
        </Text>
      </View>
    </View>
  );
};

export default StatementPage;

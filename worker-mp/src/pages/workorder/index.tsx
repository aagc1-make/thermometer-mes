import React, { useCallback, useEffect, useState } from 'react';
import { View, Text, Input, ScrollView } from '@tarojs/components';
import Taro, { useRouter } from '@tarojs/taro';
import { listWorkorders } from '@/services/api';
import type { Workorder } from '@/services/api';
import { showError } from '@/services/request';
import { WORKORDER_STATUS, statusText, dateOnly, num } from '@/utils/format';
import styles from './index.module.scss';

const FILTERS = [
  { key: '', name: '全部' },
  { key: 'CONFIRMED', name: '已确认' },
  { key: 'FINISHED', name: '已完工' }
];

const WorkorderPage: React.FC = () => {
  const router = useRouter();
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState('');
  const [rows, setRows] = useState<Workorder[]>([]);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [loading, setLoading] = useState(false);

  const load = useCallback(
    async (nextPage: number, replace: boolean) => {
      setLoading(true);
      try {
        const data = await listWorkorders({
          pageNum: nextPage,
          pageSize: 10,
          workorderCode: keyword.trim() || undefined,
          status: status || undefined
        });
        setRows((old) => (replace ? data.rows : [...old, ...data.rows]));
        setTotal(data.total);
        setPageNum(nextPage);
      } catch (error) {
        showError(error);
      } finally {
        setLoading(false);
      }
    },
    [keyword, status]
  );

  useEffect(() => {
    const code = router.params.code ? decodeURIComponent(router.params.code) : '';
    if (code) {
      setKeyword(code);
      listWorkorders({ pageNum: 1, pageSize: 10, workorderCode: code })
        .then((data) => {
          if (data.rows.length === 1) {
            Taro.redirectTo({ url: `/pages/workorder-detail/index?id=${data.rows[0].workorderId}` });
            return;
          }
          setRows(data.rows);
          setTotal(data.total);
        })
        .catch(showError);
      return;
    }
    load(1, true);
  }, []);

  const refresh = () => load(1, true);
  const loadMore = () => {
    if (!loading && rows.length < total) {
      load(pageNum + 1, false);
    }
  };

  return (
    <View className={styles.page}>
      <View className={styles.searchBar}>
        <Input
          className={styles.search}
          value={keyword}
          placeholder="输入工单号 / 产品名"
          confirmType="search"
          onInput={(event) => setKeyword(event.detail.value)}
          onConfirm={refresh}
        />
        <Text className={styles.searchBtn} onClick={refresh}>
          搜索
        </Text>
      </View>

      <ScrollView scrollX className={styles.filters}>
        {FILTERS.map((item) => (
          <View
            key={item.key}
            className={`${styles.chip} ${status === item.key ? styles.chipActive : ''}`}
            onClick={() => {
              setStatus(item.key);
              setTimeout(() => load(1, true), 0);
            }}
          >
            {item.name}
          </View>
        ))}
      </ScrollView>

      <View className={styles.list}>
        {rows.length === 0 && !loading && <View className={styles.empty}>没有匹配的工单</View>}
        {rows.map((item) => {
          const plan = num(item.quantity);
          const produced = num(item.quantityProduced);
          const percent = plan > 0 ? Math.min(Math.round((produced / plan) * 100), 100) : 0;
          return (
            <View
              key={item.workorderId}
              className={styles.card}
              onClick={() => Taro.navigateTo({ url: `/pages/workorder-detail/index?id=${item.workorderId}` })}
            >
              <View className={styles.cardHead}>
                <Text className={styles.code}>{item.workorderCode}</Text>
                <Text className={styles.status}>{statusText(WORKORDER_STATUS, item.status)}</Text>
              </View>
              <Text className={styles.product}>{item.productName || '-'} {item.specification || ''}</Text>
              <View className={styles.meta}>
                <Text>计划 {plan}{item.unitName || ''} · 已产 {produced}</Text>
                <Text>{dateOnly(item.requestDate)}</Text>
              </View>
              <View className={styles.progress}>
                <View className={styles.progressInner} style={{ width: `${percent}%` }} />
              </View>
            </View>
          );
        })}
        {rows.length > 0 && rows.length < total && (
          <View className={styles.more} onClick={loadMore}>
            {loading ? '加载中…' : '加载更多'}
          </View>
        )}
      </View>
    </View>
  );
};

export default WorkorderPage;

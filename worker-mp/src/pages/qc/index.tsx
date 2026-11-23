import React, { useCallback, useEffect, useState } from 'react';
import { View, Text, Input, Textarea, Button } from '@tarojs/components';
import Taro, { useRouter } from '@tarojs/taro';
import { addIpqc, listPendingInspects, nextBizCode, updateIpqc } from '@/services/api';
import type { PendingInspect } from '@/services/api';
import { showError } from '@/services/request';
import { useAuthStore } from '@/store/auth';
import { today, num } from '@/utils/format';
import styles from './index.module.scss';

const QcPage: React.FC = () => {
  const router = useRouter();
  const workorderId = Number(router.params.workorderId || 0);
  const user = useAuthStore((state) => state.user);
  const [rows, setRows] = useState<PendingInspect[]>([]);
  const [selected, setSelected] = useState<PendingInspect | null>(null);
  const [result, setResult] = useState<'ACCEPT' | 'REJECT'>('ACCEPT');
  const [cr, setCr] = useState('0');
  const [maj, setMaj] = useState('0');
  const [min, setMin] = useState('0');
  const [remark, setRemark] = useState('');
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await listPendingInspects(workorderId ? { workOrderId: workorderId } : {});
      setRows(data);
    } catch (error) {
      showError(error);
    } finally {
      setLoading(false);
    }
  }, [workorderId]);

  useEffect(() => {
    load();
  }, [load]);

  const submit = async () => {
    if (!selected || !user) {
      return;
    }
    const quantity = num(selected.quantityCheck);
    setSubmitting(true);
    try {
      const code = await nextBizCode('IPQC', selected.workOrderCode);
      const payload: Record<string, unknown> = {
        ipqcCode: code,
        ipqcName: `${selected.workOrderCode || ''} 过程检验`,
        ipqcType: selected.qcDetailType || 'IPQC',
        sourceDocId: selected.sourceDocId,
        sourceDocType: selected.sourceDocType,
        sourceDocCode: selected.sourceDocCode,
        sourceLineId: selected.sourceLineId,
        workorderId: selected.workOrderId,
        workorderCode: selected.workOrderCode,
        workorderName: selected.workOrderName,
        taskId: selected.taskId,
        taskCode: selected.taskCode,
        workstationId: selected.workstationId,
        workstationName: selected.workstationName,
        itemId: selected.itemId,
        itemCode: selected.itemCode,
        itemName: selected.itemName,
        specification: selected.specification,
        unitOfMeasure: selected.unitOfMeasure,
        unitName: selected.unitName,
        batchCode: selected.batchCode,
        quantityCheck: quantity,
        quantityQualified: result === 'ACCEPT' ? quantity : 0,
        quantityUnqualified: result === 'REJECT' ? quantity : 0,
        checkResult: result,
        crQuantity: num(cr),
        majQuantity: num(maj),
        minQuantity: num(min),
        inspectDate: today(),
        inspector: user.nickName,
        status: 'PREPARE',
        remark
      };
      const ipqcId = await addIpqc(payload);
      await updateIpqc({ ...payload, ipqcId: Number(ipqcId), status: 'FINISHED' });
      await Taro.showModal({
        title: result === 'ACCEPT' ? '检验已通过' : '检验不合格',
        content:
          result === 'ACCEPT'
            ? `检验单 ${code} 已完成，下一步可继续报工或完工。`
            : `检验单 ${code} 已完成。该批不进入下道工序，请按「${selected.workOrderCode} 的二次订单」重新下单。`,
        showCancel: false
      });
      setSelected(null);
      setRemark('');
      setCr('0');
      setMaj('0');
      setMin('0');
      load();
    } catch (error) {
      showError(error);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <View className={styles.page}>
      <View className={styles.tip}>共 {rows.length} 条待检报工，按报工批次逐条判定</View>

      {rows.length === 0 && !loading && <View className={styles.empty}>暂无待检任务</View>}

      {rows.map((row, index) => (
        <View key={`${row.sourceDocId}-${index}`} className={styles.card}>
          <View className={styles.cardHead}>
            <Text className={styles.code}>{row.sourceDocCode || '-'}</Text>
            <Text className={styles.qty}>待检 {num(row.quantityCheck)}</Text>
          </View>
          <Text className={styles.item}>{row.itemName || '-'} {row.specification || ''}</Text>
          <View className={styles.meta}>
            <Text>{row.workOrderCode} · {row.taskCode}</Text>
            <Text>{row.workstationName || '-'}</Text>
          </View>
          <View className={styles.meta}>
            <Text>批次 {row.batchCode || '-'}</Text>
            <Text>工序 {row.taskName || '-'}</Text>
          </View>
          <Button
            className={styles.action}
            onClick={() => {
              setSelected(row);
              setResult('ACCEPT');
            }}
          >
            开始检验
          </Button>
        </View>
      ))}

      {selected && (
        <View className={styles.sheet}>
          <View className={styles.sheetHead}>
            <Text className={styles.sheetTitle}>检验判定 · {selected.sourceDocCode}</Text>
            <Text className={styles.sheetClose} onClick={() => setSelected(null)}>关闭</Text>
          </View>
          <View className={styles.resultRow}>
            <View
              className={`${styles.resultBtn} ${result === 'ACCEPT' ? styles.resultPass : ''}`}
              onClick={() => setResult('ACCEPT')}
            >
              合格 {num(selected.quantityCheck)}
            </View>
            <View
              className={`${styles.resultBtn} ${result === 'REJECT' ? styles.resultFail : ''}`}
              onClick={() => setResult('REJECT')}
            >
              不合格
            </View>
          </View>
          <View className={styles.defects}>
            <View className={styles.defect}>
              <Text className={styles.label}>致命</Text>
              <Input className={styles.smallInput} type="number" value={cr} onInput={(e) => setCr(e.detail.value)} />
            </View>
            <View className={styles.defect}>
              <Text className={styles.label}>严重</Text>
              <Input className={styles.smallInput} type="number" value={maj} onInput={(e) => setMaj(e.detail.value)} />
            </View>
            <View className={styles.defect}>
              <Text className={styles.label}>轻微</Text>
              <Input className={styles.smallInput} type="number" value={min} onInput={(e) => setMin(e.detail.value)} />
            </View>
          </View>
          <Textarea
            className={styles.textarea}
            value={remark}
            placeholder="不良描述 / 处理意见（可不填）"
            onInput={(event) => setRemark(event.detail.value)}
          />
          <Button className={styles.submit} loading={submitting} onClick={submit}>
            提交检验结果
          </Button>
        </View>
      )}
    </View>
  );
};

export default QcPage;

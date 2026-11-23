import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { View, Text, Input, Textarea, Button } from '@tarojs/components';
import Taro, { useRouter } from '@tarojs/taro';
import { addFeedback, getWorkorder, listFeedbacks, listTasks } from '@/services/api';
import type { Feedback, ProTask, Workorder } from '@/services/api';
import { showError } from '@/services/request';
import { useAuthStore } from '@/store/auth';
import { nowText, num } from '@/utils/format';
import styles from './index.module.scss';

const FeedbackPage: React.FC = () => {
  const router = useRouter();
  const workorderId = Number(router.params.workorderId || 0);
  const user = useAuthStore((state) => state.user);
  const [wo, setWo] = useState<Workorder | null>(null);
  const [tasks, setTasks] = useState<ProTask[]>([]);
  const [feedbacks, setFeedbacks] = useState<Feedback[]>([]);
  const [taskId, setTaskId] = useState<number | null>(null);
  const [quantity, setQuantity] = useState('1');
  const [qualified, setQualified] = useState('1');
  const [unqualified, setUnqualified] = useState('0');
  const [remark, setRemark] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(false);

  const reportedOf = (id: number, rows: Feedback[]) =>
    rows
      .filter((fb) => fb.taskId === id && fb.status !== 'CANCELED')
      .reduce((total, fb) => total + num(fb.quantityFeedback), 0);

  const load = useCallback(async () => {
    if (!workorderId) {
      return;
    }
    setLoading(true);
    try {
      const [detail, taskRows, fbRows] = await Promise.all([
        getWorkorder(workorderId),
        listTasks(workorderId),
        listFeedbacks(workorderId)
      ]);
      setWo(detail);
      setTasks(taskRows);
      setFeedbacks(fbRows);
      const remain = taskRows.find((task) => {
        const done = fbRows
          .filter((fb) => fb.taskId === task.taskId && fb.status !== 'CANCELED')
          .reduce((total, fb) => total + num(fb.quantityFeedback), 0);
        return num(task.quantity) - done > 0;
      });
      if (remain) {
        setTaskId(remain.taskId);
        setQuantity(String(Math.max(num(remain.quantity) - reportedOf(remain.taskId, fbRows), 1)));
      }
    } catch (error) {
      showError(error);
    } finally {
      setLoading(false);
    }
  }, [workorderId]);

  useEffect(() => {
    load();
  }, [load]);

  const selectedTask = useMemo(() => tasks.find((task) => task.taskId === taskId) || null, [tasks, taskId]);
  const remaining = selectedTask ? Math.max(num(selectedTask.quantity) - reportedOf(selectedTask.taskId, feedbacks), 0) : 0;
  const needCheck = selectedTask?.isCheck === 'Y';

  const pickTask = (task: ProTask) => {
    setTaskId(task.taskId);
    const remain = Math.max(num(task.quantity) - reportedOf(task.taskId, feedbacks), 0);
    setQuantity(String(remain > 0 ? remain : 1));
    setQualified(String(remain > 0 ? remain : 1));
    setUnqualified('0');
  };

  const syncQuantity = (value: string, setter: (next: string) => void, other: string) => {
    setter(value);
    setQuantity(String(num(value) + num(other)));
  };

  const submit = async () => {
    if (!selectedTask || !user) {
      Taro.showToast({ title: '请选择报工工序', icon: 'none' });
      return;
    }
    const q = num(quantity);
    const good = needCheck ? q : num(qualified);
    const bad = needCheck ? 0 : num(unqualified);
    if (q <= 0 || (!needCheck && good + bad <= 0)) {
      Taro.showToast({ title: '请填写正确的报工数量', icon: 'none' });
      return;
    }
    if (q > remaining + 0.000001) {
      Taro.showToast({ title: `本工序剩余可报 ${remaining}`, icon: 'none' });
      return;
    }
    const confirm = await Taro.showModal({
      title: '提交报工',
      content: `工单 ${selectedTask.workorderCode} · ${selectedTask.processName || selectedTask.taskCode}，报工 ${q}，确认提交？`
    });
    if (!confirm.confirm) {
      return;
    }
    setSubmitting(true);
    try {
      const payload = {
        feedbackType: 'SELF',
        workorderId: selectedTask.workorderId,
        workorderCode: selectedTask.workorderCode,
        workorderName: wo?.workorderName,
        taskId: selectedTask.taskId,
        taskCode: selectedTask.taskCode,
        workstationId: selectedTask.workstationId,
        workstationName: selectedTask.workstationName,
        routeId: selectedTask.routeId,
        processId: selectedTask.processId,
        itemId: selectedTask.itemId,
        itemCode: selectedTask.itemCode,
        itemName: selectedTask.itemName,
        specification: selectedTask.specification,
        unitOfMeasure: selectedTask.unitOfMeasure,
        unitName: selectedTask.unitName,
        isCheck: selectedTask.isCheck,
        quantityFeedback: needCheck ? q : good + bad,
        quantityQualified: good,
        quantityUnquanlified: bad,
        quantityLaborScrap: 0,
        quantityMaterialScrap: 0,
        quantityOtherScrap: 0,
        userName: user.userName,
        nickName: user.nickName,
        feedbackTime: nowText(),
        remark
      };
      const recordId = await addFeedback(payload);
      const latest = await listFeedbacks(workorderId);
      const created = latest.find((item) => item.recordId === Number(recordId));
      await Taro.showModal({
        title: '报工已提交',
        content: created?.feedbackCode
          ? `报工单 ${created.feedbackCode} 已提交，下一步：${needCheck ? '完成过程检验后继续生产' : '等待工单流转'}`
          : '报工已提交，等待后续流转',
        showCancel: false
      });
      setRemark('');
      load();
    } catch (error) {
      showError(error);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <View className={styles.page}>
      <View className={styles.woCard}>
        <Text className={styles.woCode}>{wo?.workorderCode || '加载中…'}</Text>
        <Text className={styles.woProduct}>{wo?.productName || ''} {wo?.specification || ''}</Text>
      </View>

      <View className={styles.section}>
        <Text className={styles.sectionTitle}>选择工序</Text>
        {tasks.length === 0 && <View className={styles.empty}>{loading ? '加载中…' : '该工单暂无生产任务'}</View>}
        {tasks.map((task) => {
          const remain = Math.max(num(task.quantity) - reportedOf(task.taskId, feedbacks), 0);
          const active = task.taskId === taskId;
          return (
            <View
              key={task.taskId}
              className={`${styles.task} ${active ? styles.taskActive : ''} ${remain <= 0 ? styles.taskDone : ''}`}
              onClick={() => remain > 0 && pickTask(task)}
            >
              <View className={styles.taskMain}>
                <Text className={styles.taskName}>{task.processName || task.taskName}</Text>
                <Text className={styles.taskCode}>{task.taskCode} · {task.workstationName || '-'}</Text>
              </View>
              <Text className={styles.taskRemain}>{remain > 0 ? `待报 ${remain}` : '已报满'}</Text>
            </View>
          );
        })}
      </View>

      {selectedTask && (
        <View className={styles.section}>
          <Text className={styles.sectionTitle}>报工数量</Text>
          <View className={styles.infoRow}>
            <Text>检验工序</Text>
            <Text>{needCheck ? '是，报工后需完成检验' : '否'}</Text>
          </View>
          <View className={styles.infoRow}>
            <Text>剩余可报</Text>
            <Text>{remaining} {selectedTask.unitName || ''}</Text>
          </View>
          {needCheck ? (
            <View className={styles.field}>
              <Text className={styles.label}>报工数量</Text>
              <Input
                className={styles.input}
                type="digit"
                value={quantity}
                onInput={(event) => setQuantity(event.detail.value)}
              />
            </View>
          ) : (
            <>
              <View className={styles.field}>
                <Text className={styles.label}>合格品数量</Text>
                <Input
                  className={styles.input}
                  type="digit"
                  value={qualified}
                  onInput={(event) => syncQuantity(event.detail.value, setQualified, unqualified)}
                />
              </View>
              <View className={styles.field}>
                <Text className={styles.label}>不良品数量</Text>
                <Input
                  className={styles.input}
                  type="digit"
                  value={unqualified}
                  onInput={(event) => syncQuantity(event.detail.value, setUnqualified, qualified)}
                />
              </View>
              <View className={styles.infoRow}>
                <Text>报工合计</Text>
                <Text>{num(qualified) + num(unqualified)}</Text>
              </View>
            </>
          )}
          <View className={styles.field}>
            <Text className={styles.label}>备注</Text>
            <Textarea
              className={styles.textarea}
              value={remark}
              placeholder="可不填，异常情况请说明"
              onInput={(event) => setRemark(event.detail.value)}
            />
          </View>
          <Button className={styles.submit} loading={submitting} onClick={submit}>
            提交报工
          </Button>
        </View>
      )}
    </View>
  );
};

export default FeedbackPage;

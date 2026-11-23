import React, { useCallback, useEffect, useState } from 'react';
import { View, Text, ScrollView, Button } from '@tarojs/components';
import Taro, { useRouter } from '@tarojs/taro';
import {
  confirmWorkorder,
  finishWorkorder,
  getWorkorder
} from '@/services/api';
import type { Workorder } from '@/services/api';
import { mesRequest, showError } from '@/services/request';
import { buildWorkorderPipeline, loadWorkorderPipeline } from '@/utils/workorderPipeline';
import { useAuthStore } from '@/store/auth';
import { WORKORDER_STATUS, statusText, num } from '@/utils/format';
import styles from './index.module.scss';

interface Step {
  key: string;
  short: string;
  state: string;
  stateText: string;
  docCode?: string;
  guide?: string;
  action?: string;
}

const DETAIL_ROUTES: Record<string, string> = {
  feedback: '/pages/feedback/index',
  ipqc: '/pages/qc/index',
  fgIn: '/pages/receipt/index',
  issue: '/pages/issue/index'
};

const WorkorderDetailPage: React.FC = () => {
  const router = useRouter();
  const workorderId = Number(router.params.id || 0);
  const hasPermi = useAuthStore((state) => state.hasPermi);
  const [wo, setWo] = useState<Workorder | null>(null);
  const [steps, setSteps] = useState<Step[]>([]);
  const [nextStep, setNextStep] = useState<Step | null>(null);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const load = useCallback(async () => {
    if (!workorderId) {
      return;
    }
    setLoading(true);
    setErrorMsg('');
    try {
      const detail = await getWorkorder(workorderId);
      setWo(detail);
      const data = await loadWorkorderPipeline(mesRequest, detail);
      const built = buildWorkorderPipeline(detail, data) as Step[];
      setSteps(built);
      setNextStep(built.find((step) => step.state === 'doing') || null);
    } catch (error) {
      setErrorMsg((error as Error)?.message || '流水线加载失败');
    } finally {
      setLoading(false);
    }
  }, [workorderId]);

  useEffect(() => {
    load();
  }, [load]);

  const confirm = async () => {
    if (!wo) {
      return;
    }
    const result = await Taro.showModal({
      title: '确认工单',
      content: `确认工单 ${wo.workorderCode}？确认后自动排产并生成领料草稿，且不能再修改。`
    });
    if (!result.confirm) {
      return;
    }
    try {
      await confirmWorkorder(wo);
      Taro.showToast({ title: '工单已确认', icon: 'success' });
      load();
    } catch (error) {
      showError(error);
    }
  };

  const finish = async () => {
    if (!wo) {
      return;
    }
    const result = await Taro.showModal({
      title: '完成工单',
      content: `完成工单 ${wo.workorderCode}？完成后不能再报工，系统自动生成产品入库草稿。`
    });
    if (!result.confirm) {
      return;
    }
    try {
      await finishWorkorder(wo.workorderId);
      Taro.showToast({ title: '工单已完工', icon: 'success' });
      load();
    } catch (error) {
      showError(error);
    }
  };

  const goStep = (step: Step) => {
    const route = DETAIL_ROUTES[step.key];
    if (!route) {
      Taro.showToast({ title: step.guide || '该环节请到管理端办理', icon: 'none' });
      return;
    }
    const query = step.key === 'feedback' ? `?workorderId=${workorderId}` : `?workorderId=${workorderId}`;
    Taro.navigateTo({ url: `${route}${query}` });
  };

  if (!wo) {
    return <View className={styles.page}><View className={styles.empty}>{loading ? '加载中…' : errorMsg || '工单不存在'}</View></View>;
  }

  const plan = num(wo.quantity);
  const produced = num(wo.quantityProduced);
  const percent = plan > 0 ? Math.min(Math.round((produced / plan) * 100), 100) : 0;

  return (
    <View className={styles.page}>
      <View className={styles.header}>
        <View className={styles.headerTop}>
          <Text className={styles.code}>{wo.workorderCode}</Text>
          <Text className={styles.status}>{statusText(WORKORDER_STATUS, wo.status)}</Text>
        </View>
        <Text className={styles.product}>{wo.productName || '-'} {wo.specification || ''}</Text>
        <View className={styles.metaRow}>
          <Text>计划 {plan}{wo.unitName || ''}</Text>
          <Text>已产 {produced}</Text>
          <Text>{wo.clientName || '无客户订单'}</Text>
        </View>
        <View className={styles.progress}>
          <View className={styles.progressInner} style={{ width: `${percent}%` }} />
        </View>
      </View>

      <View className={styles.nextCard}>
        {nextStep ? (
          <>
            <Text className={styles.nextLabel}>下一步 · 第 {steps.indexOf(nextStep) + 1} / {steps.length} 步</Text>
            <Text className={styles.nextTitle}>{nextStep.short}</Text>
            <Text className={styles.nextGuide}>{nextStep.action || nextStep.guide}</Text>
          </>
        ) : (
          <Text className={styles.nextTitle}>工单流程已完成</Text>
        )}
        {errorMsg ? <Text className={styles.nextError}>{errorMsg}</Text> : null}
      </View>

      <View className={styles.section}>
        <View className={styles.sectionHead}>
          <Text className={styles.sectionTitle}>工单流水线</Text>
          <Text className={styles.sectionSub}>左右滑动查看全部环节</Text>
        </View>
        <ScrollView scrollX className={styles.steps}>
          {steps.map((step, index) => (
            <View
              key={step.key}
              className={`${styles.step} ${step.state === 'done' ? styles.stepDone : ''} ${
                step.state === 'doing' ? styles.stepDoing : ''
              } ${step.state === 'skipped' ? styles.stepSkipped : ''}`}
              onClick={() => goStep(step)}
            >
              <View className={styles.stepTop}>
                <Text className={styles.stepIndex}>{index + 1}</Text>
                <Text className={styles.stepName}>{step.short}</Text>
              </View>
              <Text className={styles.stepState}>{step.stateText}</Text>
              <Text className={styles.stepDoc}>{step.docCode || '-'}</Text>
              <Text className={styles.stepGuide}>{step.action || step.guide}</Text>
            </View>
          ))}
        </ScrollView>
      </View>

      <View className={styles.actions}>
        {hasPermi('mes:pro:feedback:add') && wo.status === 'CONFIRMED' && (
          <Button className={styles.actionPrimary} onClick={() => Taro.navigateTo({ url: `/pages/feedback/index?workorderId=${workorderId}` })}>
            生产报工
          </Button>
        )}
        {hasPermi('mes:qc:pending:list') && (
          <Button className={styles.actionGhost} onClick={() => Taro.navigateTo({ url: `/pages/qc/index?workorderId=${workorderId}` })}>
            过程检验
          </Button>
        )}
        {hasPermi('mes:wm:productrecpt:edit') && (
          <Button className={styles.actionGhost} onClick={() => Taro.navigateTo({ url: `/pages/receipt/index?workorderId=${workorderId}` })}>
            产品入库
          </Button>
        )}
        {hasPermi('mes:wm:issueheader:edit') && (
          <Button className={styles.actionGhost} onClick={() => Taro.navigateTo({ url: `/pages/issue/index?workorderId=${workorderId}` })}>
            生产领料
          </Button>
        )}
        {hasPermi('mes:pro:workorder:edit') && wo.status === 'PREPARE' && (
          <Button className={styles.actionPrimary} onClick={confirm}>
            确认工单
          </Button>
        )}
        {hasPermi('mes:pro:workorder:edit') && wo.status === 'CONFIRMED' && (
          <Button className={styles.actionDanger} onClick={finish}>
            完成工单
          </Button>
        )}
      </View>
    </View>
  );
};

export default WorkorderDetailPage;

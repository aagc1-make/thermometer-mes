<template>
  <el-dialog class="business-chain" title="全流程贯通 · 首批生产" :visible.sync="visible" width="min(1120px, 94vw)" append-to-body :close-on-click-modal="false">
    <div class="chain-intro">
      <div><span class="chain-eyebrow">红外测温枪 · 采购到发货</span><h3>首批 10 台，从采购到发货一次走通</h3><p>按 BOM 与工艺路线自动建单并推进流程，生成真实单据、变更库存；检验按整批合格放行，执行记录可追溯。</p></div>
      <div class="chain-counts"><div><b>10</b><span>台生产</span></div><div><b>6</b><span>台销售</span></div><div><b>4</b><span>台结存</span></div></div>
    </div>
    <div class="chain-controls">
      <el-button v-if="canStart" type="primary" icon="el-icon-video-play" @click="start">{{ state.status === 'idle' ? '开始执行' : '重新执行' }}</el-button>
      <el-button v-if="state.status === 'running'" icon="el-icon-video-pause" @click="runner.pause()">暂停</el-button>
      <el-button v-if="canResume" type="primary" icon="el-icon-video-play" @click="resume">{{ state.status === 'paused' ? '继续执行' : '核对原单据并继续' }}</el-button>
      <el-button v-if="canStop" icon="el-icon-close" :disabled="state.status === 'stopping'" @click="runner.stop()">终止执行</el-button>
      <el-tag :type="statusType">{{ statusText }}</el-tag>
      <span class="chain-run" v-if="state.runId">执行号 {{ state.runId }}</span>
    </div>
    <el-alert v-if="state.error" :title="state.error" type="error" :closable="false" show-icon class="chain-error" :description="'已完成单据保留。继续时先查询原单据状态，避免重复创建与过账；也可停止本轮后重新开始。'" />
    <div class="chain-progress-head"><strong>{{ state.current || '准备就绪，点击开始即可执行完整主链' }}</strong><span>{{ completed }} / {{ state.steps.length }} 个阶段</span></div>
    <el-progress :percentage="progress" :stroke-width="8" :status="state.status === 'completed' ? 'success' : undefined" :show-text="false" />
    <div class="chain-body">
      <ol class="chain-steps">
        <li v-for="(step, index) in state.steps" :key="step.title" :class="'chain-step--' + step.status">
          <span class="chain-step-number"><i v-if="step.status === 'completed'" class="el-icon-check" /><i v-else-if="step.status === 'running'" class="el-icon-loading" /><template v-else>{{ index + 1 }}</template></span>
          <div><strong>{{ step.title }}</strong><p>{{ step.description }}</p></div>
        </li>
      </ol>
      <div class="chain-detail">
        <div v-if="state.summary" class="chain-result"><i class="el-icon-circle-check" /><div><strong>业务闭环核对通过</strong><p>本轮生产 {{ state.summary.produced }} 台、入库 {{ state.summary.received }} 台、销售 {{ state.summary.sold }} 台，成品批次结存 {{ state.summary.remaining }} 台。</p><span>批次：{{ state.summary.batches.join('、') }}</span></div></div>
        <h4>本轮关联单据 <span>{{ state.documents.length }}</span></h4>
        <div class="chain-documents" v-if="state.documents.length">
          <button v-for="d in state.documents" :key="d.type + ':' + d.id" class="chain-document" @click="openDocument(d)"><span>{{ d.type }}</span><strong>{{ d.code }}</strong><small>{{ documentStatus(d.status) }} <i class="el-icon-top-right" /></small></button>
        </div>
        <div v-else class="chain-empty"><i class="el-icon-document-copy" /><p>执行后会在这里汇集本轮单据</p><span>点击单据可进入原业务页面核对，执行进度会保留。</span></div>
        <h4 class="chain-log-title">最近操作</h4>
        <ul class="chain-log"><li v-for="(event, index) in recentEvents" :key="index"><time>{{ time(event.time) }}</time><span>{{ event.label }}</span></li><li v-if="!recentEvents.length">开始后逐步显示实际执行记录。</li></ul>
      </div>
    </div>
    <div slot="footer" class="chain-footer"><span>暂停与终止在当前请求完成后生效；刷新后可回到这里核对并继续。</span><el-button @click="visible = false">收起面板</el-button></div>
  </el-dialog>
</template>

<script>
import { getBusinessChainSession } from '@/utils/businessChainSession'

export default {
  name: 'BusinessChain',
  data() { return { visible: false, runner: null, state: { status: 'idle', steps: [], documents: [], events: [] } } },
  computed: {
    canStart() { return ['idle', 'stopped', 'completed'].includes(this.state.status) },
    canResume() { return ['paused', 'failed', 'interrupted'].includes(this.state.status) },
    canStop() { return !['idle', 'stopped', 'completed'].includes(this.state.status) },
    completed() { return this.state.steps.filter(s => s.status === 'completed').length },
    progress() { return this.state.steps.length ? Math.round(this.completed * 100 / this.state.steps.length) : 0 },
    recentEvents() { return this.state.events.slice(-8).reverse() },
    statusText() { return ({ idle: '等待开始', running: '自动执行中', pausing: '正在暂停', paused: '已暂停', stopping: '正在停止', stopped: '已停止', failed: '需要处理', interrupted: '已中断，待核对', completed: '执行完成' })[this.state.status] || this.state.status },
    statusType() { return this.state.status === 'completed' ? 'success' : ['failed', 'interrupted'].includes(this.state.status) ? 'danger' : this.state.status === 'running' ? '' : 'info' }
  },
  created() {
    const session = getBusinessChainSession(permission => this.$auth.hasPermi(permission))
    // Keep the runner out of Vue's deep observation; only immutable UI snapshots are reactive.
    Object.defineProperty(this, 'runner', { value: session.runner, configurable: true })
    this.unsubscribe = session.subscribe(state => { this.state = JSON.parse(JSON.stringify(state)) })
  },
  beforeDestroy() { if (this.unsubscribe) this.unsubscribe() },
  methods: {
    open() { this.visible = true },
    async start() { try { await this.runner.start() } catch (e) { this.$message.error(e.message || '执行未能启动') } },
    async resume() { try { await this.runner.resume() } catch (e) { this.$message.error(e.message || '执行未能继续') } },
    openDocument(doc) { this.visible = false; this.$router.push(doc.path).catch(() => {}) },
    documentStatus(status) { return ({ PREPARE: '草稿', APPROVING: '处理中', APPROVED: '待执行', CONFIRMED: '已确认', UNCHECK: '待检验', FINISHED: '已完成' })[status] || status },
    time(value) { return new Date(value).toLocaleTimeString('zh-CN', { hour12: false }) }
  }
}
</script>

<style scoped lang="scss">
.business-chain ::v-deep .el-dialog__body { padding: 12px 24px 0; }
.chain-intro { display: flex; align-items: center; justify-content: space-between; gap: 24px; padding: 20px 22px; background: #f0f6ff; border: 1px solid #dbe8fa; border-radius: 10px; }
.chain-eyebrow { color: #2563eb; font-size: 12px; font-weight: 600; }
.chain-intro h3 { margin: 7px 0; font-size: 21px; color: #172b4d; }
.chain-intro p { margin: 0; font-size: 12px; line-height: 1.8; color: #576980; }
.chain-counts { display: flex; gap: 27px; text-align: center; white-space: nowrap; }
.chain-counts div { display: flex; flex-direction: column; gap: 4px; }
.chain-counts b { color: #245bd1; font-size: 28px; }
.chain-counts span { font-size: 12px; color: #576980; }
.chain-controls { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; padding: 18px 0; }
.chain-controls .el-button { margin-left: 0; }
.chain-run { margin-left: auto; font-size: 11px; color: #718198; }
.chain-error { margin-bottom: 14px; }
.chain-progress-head { display: flex; justify-content: space-between; gap: 12px; padding-bottom: 10px; font-size: 12px; color: #52647c; }
.chain-progress-head strong { overflow-wrap: anywhere; }
.chain-progress-head span { flex-shrink: 0; }
.chain-body { display: grid; grid-template-columns: 310px minmax(0, 1fr); max-height: 50vh; overflow: auto; margin-top: 20px; }
.chain-steps { padding: 0 22px 0 0; margin: 0; list-style: none; border-right: 1px solid #e7edf5; }
.chain-steps li { display: flex; gap: 12px; padding: 0 0 19px; color: #8290a4; }
.chain-step-number { width: 26px; height: 26px; border: 1px solid #d8e0ec; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; font-size: 12px; background: white; }
.chain-steps strong { display: block; font-size: 13px; line-height: 26px; }
.chain-steps p { margin: 3px 0 0; font-size: 11px; line-height: 1.7; }
.chain-step--running { color: #2563eb !important; }
.chain-step--running .chain-step-number { border-color: #2563eb; background: #eff6ff; }
.chain-step--completed .chain-step-number { background: #eaf8f1; border-color: #bce4cf; color: #22845a; }
.chain-step--completed strong { color: #334a63; }
.chain-step--failed { color: #bd4141 !important; }
.chain-detail { min-width: 0; padding-left: 24px; padding-bottom: 20px; }
.chain-detail h4 { margin: 0 0 12px; color: #2d405a; }
.chain-detail h4 span { font-weight: 400; color: #8290a4; padding-left: 6px; }
.chain-documents { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; max-height: 290px; overflow: auto; }
.chain-document { display: flex; flex-direction: column; text-align: left; gap: 5px; padding: 11px 13px; border: 1px solid #e3eaf3; background: #fff; border-radius: 6px; cursor: pointer; min-width: 0; }
.chain-document:hover { border-color: #7da8ed; background: #f7faff; }
.chain-document span, .chain-document small { font-size: 11px; color: #718198; }
.chain-document strong { font-size: 12px; color: #2f64b2; overflow-wrap: anywhere; }
.chain-empty { text-align: center; padding: 42px 0; color: #8290a4; background: #f8fafc; border-radius: 8px; }
.chain-empty > i { font-size: 32px; color: #b0c0d5; }
.chain-empty p { font-size: 13px; }
.chain-empty span { font-size: 11px; }
.chain-detail .chain-log-title { margin-top: 24px; }
.chain-log { list-style: none; padding: 0; margin: 0; font-size: 11px; color: #6e7d91; }
.chain-log li { display: flex; gap: 10px; padding: 6px 0; border-bottom: 1px solid #f1f4f8; }
.chain-log time { color: #9aa7b9; flex-shrink: 0; }
.chain-log span { overflow-wrap: anywhere; }
.chain-result { display: flex; gap: 12px; background: #effaf4; border: 1px solid #c9ebd8; padding: 15px; border-radius: 8px; margin-bottom: 20px; color: #27764f; }
.chain-result > i { font-size: 25px; }
.chain-result p, .chain-result span { font-size: 12px; line-height: 1.8; }
.chain-footer { display: flex; align-items: center; justify-content: space-between; gap: 14px; }
.chain-footer span { color: #8290a4; font-size: 11px; text-align: left; }
@media (max-width: 760px) { .chain-intro { flex-direction: column; align-items: flex-start; }.chain-body { grid-template-columns: 1fr; }.chain-steps { border-right: 0; }.chain-detail { padding-left: 0; }.chain-documents { grid-template-columns: 1fr; }.chain-run { width: 100%; margin: 0; } }
</style>

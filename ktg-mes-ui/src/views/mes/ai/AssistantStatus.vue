<template>
  <el-popover placement="bottom-end" width="296" trigger="click" @show="$emit('refresh')">
    <div class="assistant-engine-settings">
      <div v-for="engine in engines" :key="engine.key" class="assistant-engine-row">
        <span>{{ engine.label }}</span>
        <span>{{ labels[engine.state] || '未检测' }}</span>
      </div>
      <div v-if="admin" class="assistant-channel-options">
        <label>模拟本地模型离线 <el-switch :value="skipLocal" aria-label="模拟本地模型离线" @change="$emit('simulate', $event)" /></label>
      </div>
      <div v-if="externalAvailable" class="assistant-channel-options">
        <label>允许外部 API 兜底 <el-switch :value="allowExternal" aria-label="允许外部 API 兜底" @change="$emit('external', $event)" /></label>
      </div>
      <p v-if="admin || externalAvailable">本地模型 → 已授权的外部 API → 仅检索</p>
      <el-button v-if="admin" size="mini" type="text" @click="$emit('tools')">查看 AI 工具清单</el-button>
    </div>
    <button slot="reference" class="assistant-engine-button" type="button" aria-label="查看引擎状态">
      <span v-for="engine in engines" :key="engine.key">
        <i class="engine-dot" :class="'engine-dot--' + engine.state" aria-hidden="true" /> {{ engine.label }}
      </span>
    </button>
  </el-popover>
</template>

<script>
const labels = { online: '在线', offline: '离线', disabled: '已关闭', configured: '已配置待验证',
  not_configured: '未配置', not_connected: '待接入', simulated: '演练离线' }

export default {
  name: 'AssistantStatus',
  props: {
    status: { type: Object, default: () => ({}) },
    admin: Boolean,
    skipLocal: Boolean,
    allowExternal: Boolean
  },
  data() { return { labels } },
  computed: {
    externalAvailable() { return this.status.externalApi === 'configured' },
    engines() {
      return [
        { key: 'localModel', label: '本地 7B', state: this.skipLocal ? 'simulated' : this.status.localModel },
        { key: 'externalApi', label: '外部 API', state: this.status.externalApi },
        { key: 'vectorDb', label: '向量库', state: this.status.vectorDb },
        { key: 'graphDb', label: '图谱', state: this.status.graphDb },
        { key: 'dify', label: 'Dify', state: this.status.dify }
      ]
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/assets/styles/mes-palette";
.assistant-engine-button {
  display: flex;
  gap: 9px;
  align-items: center;
  flex-wrap: wrap;
  padding: 6px 9px;
  border-radius: 8px;
  border: 1px solid $mes-border;
  background: $mes-card;
  color: $mes-text-sub;
  cursor: pointer;
  font: inherit;
  font-size: 12px;
}
.assistant-engine-button:hover { border-color: $mes-marker; }
.engine-dot { display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: #9aa7b6; vertical-align: 1px; }
.engine-dot--online { background: $mes-success; }
.engine-dot--configured { background: $mes-warning; }
.engine-dot--offline, .engine-dot--simulated { background: $mes-danger; }
.assistant-engine-row, .assistant-channel-options label { display: flex; justify-content: space-between; gap: 8px; align-items: center; padding: 5px 0; }
.assistant-engine-row span:last-child, .assistant-engine-settings p { color: $mes-text-sub; font-size: 12px; }
.assistant-channel-options { border-top: 1px solid $mes-border; margin-top: 8px; padding-top: 6px; }
.assistant-engine-settings p { margin-bottom: 0; }
</style>

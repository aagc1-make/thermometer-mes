<template>
  <aside class="assistant-inspector" aria-label="溯源与活动">
    <header>
      <h2>溯源与活动</h2>
      <button type="button" title="关闭溯源" aria-label="关闭溯源" @click="$emit('close')"><i class="el-icon-close" aria-hidden="true" /></button>
    </header>
    <el-tabs v-model="section" class="inspector-tabs">
      <el-tab-pane v-if="message.type !== 'personalKnowledge'" label="引用原文" name="sources">
        <div v-if="toolResults.length" class="citation-list">
          <section v-for="result in toolResults" :key="result.queriedAt + result.code" class="inspector-chunk">
            <h3>{{ result.label }}</h3>
            <p>{{ result.source }}</p>
            <p>显示 {{ result.rows.length }} / {{ result.total }} 条 · {{ queryTime(result.queriedAt) }}</p>
          </section>
        </div>
        <div v-if="sources.length" class="citation-list">
          <button v-for="(source, index) in sources" :key="index" type="button"
            :aria-pressed="selected === index" @click="$emit('select', index)">
            <span>[{{ index + 1 }}]</span>
            <span><strong>{{ source.title }}</strong><small>{{ source.version }} · {{ source.sectionLabel }}</small></span>
          </button>
        </div>
        <p v-else-if="!toolResults.length" class="inspector-note">{{ message.source || '这条回答没有文档引用。' }}</p>
        <div v-if="current" v-loading="busy" class="inspector-original">
          <p v-if="error" class="inspector-note" role="alert">{{ error }}</p>
          <section v-for="chunk in chunks" :key="chunk.chunkIndex" class="inspector-chunk"
            :class="{ 'is-highlighted': chunk.chunkIndex === targetChunk }" :data-chunk="chunk.chunkIndex">
            <h3>{{ chunk.sectionLabel }}</h3>
            <pre>{{ chunk.content }}</pre>
          </section>
          <el-pagination v-if="total > pageSize" small layout="prev, pager, next" :total="total"
            :page-size="pageSize" :current-page="page" @current-change="$emit('page', $event)" />
        </div>
      </el-tab-pane>
      <el-tab-pane v-if="message.type !== 'personalKnowledge'" label="图谱子图" name="graph">
        <template v-if="impact && impact.nodes.length">
          <div class="subgraph-summary">
            <span class="inspector-note">{{ impactSection }} · {{ impact.nodes.length }} 个实体</span>
            <el-button size="mini" icon="el-icon-share" @click="openImpact">查看影响路径</el-button>
          </div>
          <div class="citation-list">
            <button v-for="segment in impactSegments" :key="segment.edge.id" type="button" @click="openImpact">
              <span><strong>{{ segment.child.name }} → {{ segment.parent.name }}</strong>
                <small>{{ quantityText(segment.edge.quantity) }} {{ segment.edge.childUnit }}/{{ segment.edge.parentUnit }} · BOM #{{ segment.edge.bomId }}</small></span>
            </button>
          </div>
          <p v-if="impact.limited" class="inspector-note">已达到查询上限，只显示已确认的路径。</p>
          <p v-else-if="impact.paths && impact.paths.length > 1" class="inspector-note">
            这里是首条已确认路径；另有 {{ impact.paths.length - 1 }} 条，可在「查看影响路径」里切换。
          </p>
        </template>
        <template v-else-if="graph && graph.nodes.length">
          <div class="subgraph-summary">
            <span class="inspector-note">{{ graph.nodes.length }} 个实体 · {{ graph.edges.length }} 条关系</span>
            <el-button size="mini" icon="el-icon-full-screen" @click="showSubgraph">展开图谱</el-button>
          </div>
          <div class="citation-list">
            <button v-for="edge in graph.edges" :key="edge.source + edge.kind + edge.target" type="button"
              @click="openGraph(edge.source)">
              <span><strong>{{ edge.fromName }} → {{ graphRelations[edge.kind] || edge.kind }} → {{ edge.toName }}</strong></span>
            </button>
          </div>
        </template>
        <p v-else class="inspector-note">这条回答没有匹配的图谱关系。</p>
      </el-tab-pane>
      <el-tab-pane label="执行轨迹" name="activity">
        <section v-for="run in difyRuns" :key="run.runId" class="inspector-run">
          <strong>Dify 编排{{ run.route ? ' · ' + run.route : '' }}</strong>
          <code>{{ run.runId }}</code>
          <a v-if="run.runUrl" :href="run.runUrl" target="_blank" rel="noopener noreferrer">查看 Dify 编排 <i class="el-icon-top-right" aria-hidden="true" /></a>
        </section>
        <p v-if="!difyRuns.length && !message.streaming" class="inspector-note">
          {{ difyFallback ? difyFallback.detail : '本条回答没有 Dify 运行记录。' }}
        </p>
        <ol v-if="trace.length" class="inspector-activity">
          <li v-for="(event, index) in trace" :key="index">
            <strong>{{ event.label }}</strong><time>{{ (event.elapsedMs / 1000).toFixed(1) }} 秒</time>
            <p>{{ event.detail }}</p>
            <details v-if="event.arguments">
              <summary>查询参数与耗时</summary>
              <pre>{{ JSON.stringify(event.arguments, null, 2) }}</pre>
              <p>数据库查询 {{ event.queryMs }} 毫秒</p>
            </details>
          </li>
        </ol>
        <p v-else class="inspector-note">这条回答没有执行记录。</p>
      </el-tab-pane>
    </el-tabs>
    <el-dialog v-if="graph && graph.nodes.length" title="回答关联图谱" :visible.sync="subgraphVisible"
      append-to-body width="min(1100px, 92vw)" top="8vh">
      <div v-if="subgraphVisible" class="subgraph-canvas">
        <knowledge-graph-canvas :nodes="graph.nodes" :edges="graph.edges" :layout="subgraphLayout"
          :selected-id="subgraphNode" @select="subgraphNode = $event" />
      </div>
      <div slot="footer" class="subgraph-footer">
        <div v-if="subgraphEntity" class="subgraph-selection">
          <strong>{{ subgraphEntity.name }}</strong>
          <code v-if="subgraphEntity.code">{{ subgraphEntity.code }}</code>
        </div>
        <el-button v-if="subgraphEntity" size="small" @click="openGraph(subgraphEntity.id)">在知识图谱中定位 <i class="el-icon-top-right" /></el-button>
      </div>
    </el-dialog>
  </aside>
</template>

<script>
import { graphRelations } from '@/utils/kbGraph'
import { arrangeGraph } from '@/utils/kbGraphLayout'
import KnowledgeGraphCanvas from './KnowledgeGraphCanvas.vue'

export default {
  name: 'AssistantInspector',
  components: { KnowledgeGraphCanvas },
  data() { return { graphRelations, subgraphVisible: false, subgraphNode: '' } },
  props: {
    message: { type: Object, required: true },
    tab: { type: String, required: true },
    selected: { type: Number, required: true },
    chunks: { type: Array, default: () => [] },
    error: { type: String, default: '' },
    busy: Boolean,
    total: { type: Number, default: 0 },
    page: { type: Number, default: 1 },
    pageSize: { type: Number, default: 20 }
  },
  computed: {
    section: { get() { return this.tab }, set(value) { this.$emit('tab', value) } },
    sources() { return this.message.sources || [] },
    toolResults() { return this.message.toolResults || [] },
    trace() { return this.message.trace || [] },
    difyRuns() {
      return this.trace.filter(event => event.label === 'Dify 编排' && /^[A-Za-z0-9_-]{1,64}$/.test(event.runId || ''))
        .map(event => ({ ...event, runUrl: /^https?:\/\//i.test(event.runUrl || '') ? event.runUrl : '' }))
    },
    difyFallback() { return this.trace.find(event => event.label === 'Dify 兜底') },
    graph() { return this.message.graph },
    impact() { return this.message.impact },
    impactSection() {
      const paths = (this.impact && this.impact.paths) || []
      return paths.length ? paths.length + ' 条路径 · ' + this.impactSegments.length + ' 段装配' : '影响路径'
    },
    // 只展示首条（已到达整机且最短）路径，避免把多条路径的段混在一起
    impactSegments() {
      const paths = (this.impact && this.impact.paths) || []
      if (!paths.length) return []
      const nodes = new Map(((this.impact && this.impact.nodes) || []).map(node => [node.id, node]))
      const edges = new Map(((this.impact && this.impact.edges) || []).map(edge => [edge.id, edge]))
      return paths[0].edgeIds.map(id => edges.get(id)).filter(Boolean).map(edge => ({
        edge, child: nodes.get(edge.source) || {}, parent: nodes.get(edge.target) || {}
      }))
    },
    subgraphLayout() { return arrangeGraph(this.graph.nodes, this.graph.edges) },
    subgraphEntity() {
      if (!this.graph) return null
      return this.graph.nodes.find(node => node.id === this.subgraphNode)
    },
    current() { return this.sources[this.selected] },
    targetChunk() { return this.current && (this.current.chunkIndex == null ? this.current.sectionIndex : this.current.chunkIndex) }
  },
  methods: {
    queryTime(value) { return new Date(value).toLocaleString('zh-CN', { hour12: false }) },
    quantityText(value) {
      const text = value === null || value === undefined ? '' : String(value)
      return text.includes('.') ? text.replace(/(\.\d*?)0+$/, '$1').replace(/\.$/, '') : text
    },
    openImpact() {
      const itemCode = this.impact && this.impact.itemCode
      if (!itemCode) return
      this.$router.push({
        path: '/mes/ai/graph',
        query: { libraryId: String(this.message.graphLibraryId || this.impact.libraryId), mode: 'impact', itemCode }
      })
    },
    showSubgraph() {
      this.subgraphNode = this.subgraphLayout.root
      this.subgraphVisible = true
    },
    openGraph(node) {
      this.subgraphVisible = false
      this.$router.push({ path: '/mes/ai/graph', query: { libraryId: String(this.message.graphLibraryId), node } })
    }
  },
  watch: {
    message() {
      this.subgraphVisible = false
      this.subgraphNode = ''
    },
    chunks() {
      this.$nextTick(() => {
        const marker = this.$el.querySelector('.is-highlighted')
        if (marker) marker.scrollIntoView({ block: 'nearest' })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/assets/styles/mes-palette";
.assistant-inspector {
  display: flex;
  flex-direction: column;
  width: 380px;
  min-height: 0;
  padding: 0 16px;
  background: $mes-card;
  border-left: 1px solid $mes-border;
  color: $mes-text;
}
header { padding: 14px 0 6px; display: flex; justify-content: space-between; align-items: center; gap: 8px; }
header h2 { margin: 0; font-size: 16px; }
header button { width: 28px; height: 28px; border: 0; border-radius: 6px; background: $mes-primary-soft; color: $mes-primary; cursor: pointer; }
.inspector-tabs { overflow: auto; }
.inspector-tabs ::v-deep .el-tabs__item { padding: 0 10px; height: 38px; line-height: 38px; }
.citation-list { display: grid; gap: 6px; margin: 0 0 14px; }
.citation-list button {
  display: flex; align-items: flex-start; gap: 8px; width: 100%; padding: 8px 10px;
  text-align: left; font: inherit; border: 1px solid $mes-border; border-radius: 8px; background: $mes-card; color: $mes-text; cursor: pointer;
}
.citation-list button[aria-pressed='true'] { border-color: $mes-marker; background: $mes-primary-soft; }
.citation-list strong, .citation-list small { display: block; overflow-wrap: anywhere; }
.citation-list small, .inspector-note { color: $mes-text-sub; font-size: 12px; line-height: 1.6; }
.subgraph-summary { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin: 2px 0 12px; }
.subgraph-summary .el-button { color: #514987; border-color: #d9d4e8; background: #f7f5fb; }
.subgraph-canvas { position: relative; height: clamp(300px, 61vh, 640px); border: 1px solid #d9d6e4; border-radius: 12px; overflow: hidden; }
.subgraph-footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; text-align: left; }
.subgraph-selection { display: flex; align-items: baseline; flex-wrap: wrap; gap: 7px 10px; color: $mes-text; }
.subgraph-selection code { color: $mes-text-sub; font-size: 12px; }
.inspector-original { min-height: 36px; padding-bottom: 12px; }
.inspector-chunk, .inspector-activity li { margin-bottom: 8px; padding: 10px; border-radius: 6px; background: $mes-primary-soft; }
.inspector-chunk.is-highlighted { border-left: 3px solid $mes-primary; background: #edf4ff; }
.inspector-chunk h3 { margin: 0 0 6px; color: $mes-primary; font-size: 12px; }
.inspector-chunk pre { margin: 0; font: inherit; line-height: 1.65; white-space: pre-wrap; overflow-wrap: anywhere; }
.inspector-activity { padding: 0; margin: 0; list-style: none; }
.inspector-run {
  display: grid;
  gap: 5px;
  margin-bottom: 10px;
  padding: 10px;
  border: 1px solid $mes-border;
  border-radius: 6px;
}
.inspector-run code { font-size: 12px; overflow-wrap: anywhere; }
.inspector-run a { width: fit-content; color: $mes-primary; text-decoration: underline; }
.inspector-activity time { float: right; font-size: 12px; color: $mes-text-sub; }
.inspector-activity p { margin: 5px 0 0; color: $mes-text-sub; }
.inspector-activity details { margin-top: 6px; }
.inspector-activity summary { cursor: pointer; font-size: 12px; color: $mes-primary; }
.inspector-activity pre { white-space: pre-wrap; overflow-wrap: anywhere; font-size: 12px; }
</style>

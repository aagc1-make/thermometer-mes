<template>
  <div class="kb-graph">
    <header class="graph-heading">
      <div class="graph-title"><span class="graph-emblem" aria-hidden="true"><i class="el-icon-connection" /></span><h1>知识图谱</h1></div>
      <div class="graph-heading__actions">
        <el-select v-if="libraries.length" v-model="libraryId" :popper-append-to-body="false" size="small" aria-label="部门知识库" @change="changeLibrary">
          <i slot="prefix" class="el-input__icon el-icon-collection" />
          <el-option v-for="library in libraries" :key="library.libraryId" :label="library.name" :value="library.libraryId" />
        </el-select>
        <button type="button" class="library-link" @click="$router.push('/mes/ai/knowledge')">知识库管理 <i class="el-icon-top-right" /></button>
        <button v-if="canPresent" type="button" class="action-button primary presentation-button" :aria-pressed="presenting" @click="togglePresentation">
          <i class="el-icon-full-screen" aria-hidden="true" />{{ presenting ? '退出全屏' : '全屏演示' }}
        </button>
      </div>
    </header>

    <section class="graph-workspace" aria-label="图谱工作台">
      <div v-if="inImpact" class="graph-toolbar impact-toolbar">
        <button type="button" class="back-link" @click="leaveImpact"><i class="el-icon-arrow-left" aria-hidden="true" /> 返回关系图</button>
        <strong class="impact-title">物料影响路径</strong>
        <el-select v-if="impactPaths.length > 1" v-model="impactPathIndex" :popper-append-to-body="false" size="small"
          aria-label="装配路径" @change="selectImpactPath">
          <el-option v-for="(path, index) in impactPaths" :key="index" :label="pathLabel(path)" :value="index" />
        </el-select>
        <span v-if="impact && !impactError" class="graph-count">
          <b>{{ impactPaths.length }}</b> 条路径<span>·</span><b>{{ impactSegments.length }}</b> 段装配
        </span>
      </div>
      <div v-else class="graph-toolbar">
        <el-input v-model.trim="search" size="small" clearable maxlength="100" aria-label="搜索名称或 MES 编码"
          placeholder="搜索名称或 MES 编码" prefix-icon="el-icon-search" @keyup.enter.native="searchGraph" @clear="searchGraph">
          <button slot="append" type="button" title="搜索" aria-label="搜索" @click="searchGraph"><i class="el-icon-right" /></button>
        </el-input>
        <el-select v-model="types" :popper-append-to-body="false" size="small" multiple collapse-tags placeholder="实体类型" aria-label="实体类型" @change="load">
          <el-option v-for="(item, id) in entityTypes" :key="id" :label="item.label" :value="id" />
        </el-select>
        <el-select v-model="relation" :popper-append-to-body="false" size="small" clearable placeholder="关系类型" aria-label="关系类型" @change="load">
          <el-option v-for="(label, id) in graphRelations" :key="id" :label="label" :value="id" />
        </el-select>
        <span v-if="!busy && !error" class="graph-count"><b>{{ graph.nodes.length }}</b> 个实体<span>·</span><b>{{ graph.edges.length }}</b> 条关系</span>
      </div>

      <div class="graph-body" :class="{ 'impact-body': inImpact }" v-loading="inImpact ? impactBusy && !impact : busy">
        <section class="graph-stage" aria-label="关系网络">
          <header class="stage-heading">
            <span v-if="inImpact"><i class="el-icon-right" aria-hidden="true" /> 从子件向上查父件</span>
            <span v-else><i class="el-icon-share" aria-hidden="true" /> {{ focus ? '邻居视图' : '关系全览' }}</span>
            <button v-if="!inImpact && focus" type="button" class="leave-focus" @click="clearFocus">{{ focusName || focus }} <i class="el-icon-close" /></button>
          </header>
          <knowledge-graph-canvas v-show="!inImpact" ref="canvas" :nodes="graph.nodes" :edges="graph.edges" :layout="layout"
            :selected-id="selectedId" @select="select" />
          <knowledge-graph-canvas v-show="inImpact" ref="impactCanvas" variant="impact" :nodes="impactNodes"
            :edges="impactEdges" :layout="impactLayout" :selected-edge-id="impactEdgeId" :origin-id="impactOriginId"
            :edge-label="impactEdgeLabel" :size="{ width: 210, height: 82 }" @select-edge="selectSegment" />
          <div v-if="!inImpact && !busy && !graph.nodes.length" class="graph-empty" role="status">
            <i :class="error ? 'el-icon-warning-outline' : 'el-icon-connection'" aria-hidden="true" />
            <h2>{{ error || (hasFilters ? '没有匹配的实体' : '暂时没有可浏览的图谱') }}</h2>
            <p v-if="!error">{{ hasFilters ? '清除筛选后可以重新查看当前知识库。' : '上传文档时开启「纳入知识图谱」，建图完成后即可查看。' }}</p>
            <div class="graph-empty__actions">
              <button v-if="error || hasFilters" type="button" class="action-button" @click="error ? retryLoad() : resetFilters()">{{ error ? '重新加载' : '清除筛选' }}</button>
              <button v-if="codeQuery" type="button" class="action-button primary" @click="enterImpact(codeQuery)">按此编码查影响路径</button>
            </div>
          </div>
          <div v-if="inImpact && !impactBusy && !impactError && !impactNodes.length" class="graph-empty" role="status">
            <i class="el-icon-warning-outline" aria-hidden="true" />
            <h2>{{ impactItemCode }} 当前追溯到此</h2>
            <p>MES 物料清单里没有它的启用上级装配记录，不能据此判断它装入了哪个产品。</p>
            <div class="graph-empty__actions">
              <button type="button" class="action-button" @click="leaveImpact">返回关系图</button>
            </div>
          </div>
          <div v-if="inImpact && impactError && !impactBusy" class="graph-empty" role="status">
            <i class="el-icon-warning-outline" aria-hidden="true" />
            <h2>{{ impactError }}</h2>
            <div class="graph-empty__actions">
              <button type="button" class="action-button" @click="leaveImpact">返回关系图</button>
              <button type="button" class="action-button primary" @click="loadImpact">重试</button>
            </div>
          </div>
        </section>

        <aside class="graph-inspector" aria-label="图谱详情">
          <template v-if="inImpact">
            <div class="panel-bar"><strong>装配依据</strong>
              <span class="impact-progress">{{ impactProgress }}</span>
            </div>
            <div class="panel-scroll impact-panel">
              <p v-if="impactBusy && !impact" class="graph-hint">正在向上查启用的物料清单…</p>
              <template v-else-if="impact && impactPaths.length">
                <ul class="impact-segments" aria-label="装配段"
                  @keydown.up.prevent="moveSegment(-1)" @keydown.down.prevent="moveSegment(1)">
                  <li v-for="segment in impactSegments" :key="segment.id">
                    <button type="button" :aria-pressed="segment.id === impactEdgeId" :class="{ selected: segment.id === impactEdgeId }"
                      @click="selectSegment(segment.id)">
                      <span class="segment-index">{{ segment.index + 1 }}</span>
                      <span class="segment-body">
                        <span class="segment-head">
                          <strong>{{ segment.child.name }}</strong>
                          <span class="segment-qty">{{ segment.label }}</span>
                        </span>
                        <span class="segment-parent"><i class="el-icon-right" aria-hidden="true" />{{ segment.parent.name }}</span>
                      </span>
                    </button>
                  </li>
                </ul>
                <p v-if="impact.limited" class="graph-hint">已达到查询上限，只显示已确认的路径，不表示已找全。</p>
                <p v-if="!impactEdgeId" class="graph-hint">点击画布上的箭头或用量标签，或选择上面任一段，查看该段的 BOM 行与文档佐证。</p>
                <p v-else-if="impactEdgeBusy" class="graph-hint">正在重新读取这一段启用的 BOM 行…</p>
                <section v-else-if="impactEdge" class="impact-detail">
                  <h2>{{ impactEdge.bomRow.childName }}装入{{ impactEdge.bomRow.parentName }}</h2>
                  <p class="impact-meta">MES 物料清单 · BOM 行 #{{ impactEdge.bomRow.bomId }}</p>
                  <div class="usage-box">
                    <span>每 1 {{ impactEdge.bomRow.parentUnit }}使用</span>
                    <strong>{{ quantityText(impactEdge.bomRow.quantity) }} {{ impactEdge.bomRow.childUnit }}</strong>
                  </div>
                  <dl class="impact-pairs">
                    <dt>父件</dt>
                    <dd><strong>{{ impactEdge.bomRow.parentName }}</strong><code>{{ impactEdge.bomRow.parentCode }}</code></dd>
                    <dt>子件</dt>
                    <dd><strong>{{ impactEdge.bomRow.childName }}</strong><code>{{ impactEdge.bomRow.childCode }}</code></dd>
                  </dl>
                  <section class="impact-evidence">
                    <h3>文档佐证 <small>{{ evidenceSources.length }} 处</small></h3>
                    <template v-if="impactEdge.evidenceStatus === 'unavailable'">
                      <p class="graph-hint" role="alert">文档依据暂不可读取，BOM 行仍可对照。</p>
                      <button type="button" class="action-button" @click="loadImpactEdge(impactEdgeId)">重试读取原文</button>
                    </template>
                    <template v-else>
                      <button v-for="(source, index) in evidenceSources" :key="index" type="button" class="evidence-card"
                        @click="openImpactSource(source)">
                        <strong><i class="el-icon-document" aria-hidden="true" /> {{ source.title }}</strong>
                        <small>{{ source.version }} · {{ source.clauseLabel || source.sectionLabel }}</small>
                        <span>{{ source.excerpt }}</span>
                        <span v-if="source.quantityMismatch" class="evidence-warning">原文用量 {{ source.documentQuantity }} 与当前 BOM 用量不同，以 BOM 为准</span>
                        <em>查看原文 <i class="el-icon-top-right" aria-hidden="true" /></em>
                      </button>
                      <p v-if="!evidenceSources.length" class="graph-hint">当前知识库暂无可引用条款。</p>
                    </template>
                  </section>
                </section>
                <template v-else>
                  <p class="graph-hint" role="alert">{{ impactEdgeError }}</p>
                  <button type="button" class="action-button primary" @click="refreshImpact">刷新路径</button>
                </template>
              </template>
              <template v-else-if="!impactBusy">
                <p class="graph-hint">{{ impactError || impactItemCode + ' 当前追溯到此：MES 物料清单里没有它的启用上级装配记录。' }}</p>
                <button type="button" class="action-button" @click="leaveImpact">返回关系图</button>
              </template>
            </div>
          </template>
          <template v-else>
            <div class="panel-bar"><strong>{{ selected ? '实体详情' : '实体目录' }}</strong>
              <button v-if="selected" type="button" class="quiet-button" @click="select('')"><i class="el-icon-menu" /> 目录</button>
            </div>
            <div v-if="selected" :key="selected.id" class="panel-scroll">
              <div class="identity">
                <span class="identity-symbol" :style="swatch(selected.type)"><i :class="typeInfo(selected.type).icon" /></span>
                <span class="identity-type">{{ typeInfo(selected.type).label }}</span>
                <h2>{{ selected.name }}</h2>
                <code v-if="selected.code">{{ selected.code }}</code>
              </div>
              <div class="neighbor-control">
                <button type="button" class="action-button primary" @click="expand"><i class="el-icon-share" /> 展开邻居</button>
                <div role="group" aria-label="邻居跳数" class="hop-switch">
                  <button v-for="n in [1,2]" :key="n" type="button" :aria-pressed="hops === n" @click="changeHops(n)">{{ n }} 跳</button>
                </div>
              </div>
              <button v-if="impactAllowed" type="button" class="impact-entry" @click="enterImpact(selected.code)">
                <i class="el-icon-share" aria-hidden="true" /> 查看影响路径 <i class="el-icon-arrow-right" aria-hidden="true" />
              </button>
              <button v-if="stockAllowed" type="button" class="stock-button" @click="openStockQuery"><i class="el-icon-data-analysis" /> 查实时库存 <i class="el-icon-right" /></button>
              <section class="panel-section">
                <h3>来源文档 <small>{{ (selected.sources || []).length }} 处引用</small></h3>
                <button v-for="source in visibleSources" :key="source.versionId + ':' + source.chunkIndex"
                  type="button" class="source-note" @click="openSource(source)">
                  <strong><i class="el-icon-document" /> {{ source.title }} <i class="el-icon-top-right" /></strong>
                  <small>{{ source.sectionLabel }}</small><span v-if="source.excerpt">{{ source.excerpt }}</span>
                </button>
                <button v-if="selected.sources && selected.sources.length > 1" class="more-sources" type="button"
                  :aria-expanded="allSources" @click="allSources = !allSources">{{ allSources ? '收起其余引用' : '查看其余 ' + (selected.sources.length - 1) + ' 处引用' }} <i :class="allSources ? 'el-icon-arrow-up' : 'el-icon-arrow-down'" /></button>
                <p v-if="!selected.sources || !selected.sources.length" class="graph-hint">当前实体没有可查看的来源片段。</p>
              </section>
              <section class="panel-section">
                <h3>相邻实体 <small>{{ connections.length }}</small></h3>
                <ul v-if="connections.length" class="entity-rows">
                  <li v-for="row in connections" :key="row.key">
                    <button type="button" @click="inspect(row.node.id)">
                      <i class="row-symbol" :class="typeInfo(row.node.type).icon" :style="swatch(row.node.type)" />
                      <span class="relation-sentence" :title="row.description">{{ row.prefix }}<strong>{{ row.node.name }}</strong>{{ row.suffix }}</span>
                      <i class="el-icon-arrow-right neighbor-arrow" aria-hidden="true" />
                    </button>
                  </li>
                </ul>
                <p v-else class="graph-hint">当前视图没有相邻实体。</p>
              </section>
            </div>
            <div v-else class="panel-scroll directory">
              <p v-if="graph.limited" class="graph-hint">已达到显示上限，可筛选名称或实体类型。</p>
              <ul v-if="graph.nodes.length" class="entity-rows">
                <li v-for="node in sortedNodes" :key="node.id"><button type="button" @click="inspect(node.id)">
                  <i class="row-symbol" :class="typeInfo(node.type).icon" :style="swatch(node.type)" />
                  <span class="directory-word"><strong>{{ node.name }}</strong><small>{{ node.code || typeInfo(node.type).label }}</small></span>
                  <i class="el-icon-arrow-right" /></button></li>
              </ul>
              <p v-else class="graph-hint">图谱中的实体会显示在这里。</p>
            </div>
          </template>
        </aside>
      </div>
      <footer v-if="inImpact" class="graph-legend" aria-label="图例">
        <strong>图例</strong>
        <span><i class="legend-line" />当前装配路径</span>
        <span><i class="legend-line solid" />选中的 BOM 行</span>
        <small v-if="impact && impact.limited">当前为部分路径</small>
      </footer>
      <footer v-else-if="legend.length" class="graph-legend" aria-label="实体图例">
        <strong>图例</strong><span v-for="type in legend" :key="type"><i :style="{ backgroundColor: typeInfo(type).color }" />{{ typeInfo(type).label }}</span>
        <span v-if="relationLegend.length" class="relation-legend">
          <span v-for="item in relationLegend" :key="item.label"><i :class="['legend-line', { dashed: item.dashed }]" />{{ item.label }}</span>
        </span>
        <small v-if="graph.limited">当前为部分图谱</small>
      </footer>
    </section>
    <el-drawer :key="presenting ? 'fullscreen' : 'standard'" :title="preview.title" :visible.sync="sourceVisible"
      size="440px" :append-to-body="!presenting" :modal-append-to-body="!presenting">
      <section v-loading="sourceBusy" class="graph-preview">
        <p v-if="previewError" role="alert">{{ previewError }}</p>
        <template v-else-if="preview.chunk">
          <blockquote v-if="preview.excerpt" class="preview-quote">{{ preview.excerpt }}</blockquote>
          <h3>{{ preview.clauseLabel || preview.chunk.sectionLabel }}</h3>
          <pre>{{ preview.chunk.content }}</pre>
        </template>
      </section>
    </el-drawer>
  </div>
</template>

<script>
import KnowledgeGraphCanvas from './KnowledgeGraphCanvas.vue'
import { arrangeGraph, arrangeOrderedPath } from '@/utils/kbGraphLayout'
import {
  getImpactEdge, getImpactPath, getLibraryGraph, listAdminDepartmentLibraries, listDepartmentLibraries, previewLibraryDocument
} from '@/api/mes/knowledgeLibrary'
import { entityStyle, entityTypes, graphRelations } from '@/utils/kbGraph'

const relationWording = {
  MENTIONS: [['提及', ''], ['被', '提及']],
  USED_IN: [['用于', ''], ['用到', '']],
  HAS_PROCESS: [['包含工序', ''], ['属于', '的工序']],
  CHECKED_BY: [['由', '检验'], ['检验', '']],
  REQUIRES: [['需要', ''], ['供', '使用']],
  MAY_CAUSE: [['可能涉及', ''], ['与', '可能相关']]
}

const emptyGraph = () => ({ nodes: [], edges: [], limited: false })
const CODE_QUERY = /^[A-Za-z0-9][A-Za-z0-9._-]{2,63}$/

export default {
  name: 'KnowledgeGraph',
  components: { KnowledgeGraphCanvas },
  data() {
    return {
      entityTypes, graphRelations, libraries: [], libraryId: null, graph: emptyGraph(),
      search: '', types: [], relation: '', hops: 1, focus: '', focusName: '', selectedId: '',
      error: '', busy: true, graphRequest: 0, sourceRequest: 0, sourceVisible: false, sourceBusy: false,
      preview: {}, previewError: '', allSources: false, canPresent: false, presenting: false,
      // 影响路径：浏览/影响路径两种状态，请求序号保证迟到响应不覆盖新库或新选边
      mode: 'browse', impact: null, impactItemCode: '', impactPathIndex: 0, impactEdgeId: '',
      impactEdge: null, impactEdgeBusy: false, impactEdgeError: '', impactError: '', impactBusy: false,
      impactRequest: 0, impactEdgeRequest: 0
    }
  },
  computed: {
    inImpact() { return this.mode === 'impact' },
    layout() { return arrangeGraph(this.graph.nodes, this.graph.edges, this.focus) },
    legend() { return Array.from(new Set(this.graph.nodes.map(node => node.type))) },
    relationLegend() {
      const items = []
      if (this.graph.edges.some(edge => edge.kind === 'MENTIONS')) items.push({ label: '文档提及', dashed: true })
      if (this.graph.edges.some(edge => edge.kind !== 'MENTIONS')) items.push({ label: '业务关系', dashed: false })
      return items
    },
    hasFilters() { return Boolean(this.search || this.focus || this.types.length || this.relation) },
    codeQuery() { return CODE_QUERY.test(this.search) ? this.search : '' },
    connections() {
      if (!this.selected) return []
      const nodes = new Map(this.graph.nodes.map(node => [node.id, node]))
      return this.graph.edges.filter(edge => edge.source === this.selectedId || edge.target === this.selectedId)
        .map(edge => {
          const incoming = edge.target === this.selectedId
          const label = graphRelations[edge.kind] || edge.kind
          const words = relationWording[edge.kind]
          const [prefix, suffix] = words ? words[incoming ? 1 : 0] : [incoming ? '关联自' : '关联到', '']
          return { key: JSON.stringify([edge.source, edge.kind, edge.target]), prefix, suffix,
            node: nodes.get(incoming ? edge.source : edge.target), description: [edge.fromName, label, edge.toName].join(' ') }
        }).filter(row => row.node)
    },
    visibleSources() {
      const sources = (this.selected && this.selected.sources) || []
      return this.allSources ? sources : sources.slice(0, 1)
    },
    selected() { return this.graph.nodes.find(node => node.id === this.selectedId) },
    sortedNodes() { return this.graph.nodes.slice().sort((a, b) => (a.type === 'Document') - (b.type === 'Document')) },
    stockAllowed() {
      return this.selected && ['Item', 'Product'].includes(this.selected.type) && this.selected.code
        && this.$auth.hasPermi('mes:wm:wmstock:list')
    },
    impactAllowed() {
      return Boolean(this.selected && this.selected.code && ['Item', 'Product'].includes(this.selected.type)
        && this.$auth.hasPermi('mes:md:mditem:list'))
    },
    impactPaths() { return (this.impact && this.impact.paths) || [] },
    impactPath() { return this.impactPaths[this.impactPathIndex] || null },
    impactOriginId() { return (this.impact && this.impact.originId) || '' },
    impactEdges() {
      const edges = new Map(((this.impact && this.impact.edges) || []).map(edge => [edge.id, edge]))
      return (this.impactPath && this.impactPath.edgeIds || []).map(id => edges.get(id)).filter(Boolean)
    },
    impactNodes() {
      const nodes = new Map(((this.impact && this.impact.nodes) || []).map(node => [node.id, node]))
      const order = []
      if (this.impactEdges.length) {
        order.push(this.impactEdges[0].source)
        this.impactEdges.forEach(edge => order.push(edge.target))
      } else if (this.impactOriginId) {
        order.push(this.impactOriginId)
      }
      return order.map(id => nodes.get(id)).filter(Boolean)
    },
    impactLayout() { return arrangeOrderedPath(this.impactNodes, { width: 210, height: 82 }) },
    impactSegments() {
      const nodes = new Map(((this.impact && this.impact.nodes) || []).map(node => [node.id, node]))
      return this.impactEdges.map((edge, index) => ({
        id: edge.id, index, edge,
        child: nodes.get(edge.source) || {}, parent: nodes.get(edge.target) || {},
        label: this.quantityText(edge.quantity) + ' ' + (edge.childUnit || '') + '/' + (edge.parentUnit || '')
      }))
    },
    impactProgress() {
      if (!this.impactSegments.length) return ''
      const index = this.impactSegments.findIndex(segment => segment.id === this.impactEdgeId)
      return index < 0 ? '共 ' + this.impactSegments.length + ' 段' : '第 ' + (index + 1) + ' 段 · 共 ' + this.impactSegments.length + ' 段'
    },
    evidenceSources() { return (this.impactEdge && this.impactEdge.sources) || [] }
  },
  mounted() {
    this.canPresent = Boolean(document.fullscreenEnabled && this.$el.requestFullscreen)
    document.addEventListener('fullscreenchange', this.syncPresentation)
    this.loadLibraries()
  },
  beforeDestroy() {
    document.removeEventListener('fullscreenchange', this.syncPresentation)
    this.graphRequest++
    this.sourceRequest++
    this.impactRequest++
    this.impactEdgeRequest++
  },
  methods: {
    syncPresentation() { this.presenting = document.fullscreenElement === this.$el },
    async togglePresentation() {
      try {
        if (document.fullscreenElement === this.$el) await document.exitFullscreen()
        else await this.$el.requestFullscreen()
      } catch (error) {
        this.$message.warning('浏览器暂时无法切换全屏模式。')
      }
    },
    typeInfo: entityStyle,
    swatch(type) {
      const { color, tint } = entityStyle(type)
      return { color, backgroundColor: tint }
    },
    quantityText(value) {
      if (value === null || value === undefined || value === '') return ''
      const text = String(value)
      return text.includes('.') ? text.replace(/(\.\d*?)0+$/, '$1').replace(/\.$/, '') : text
    },
    impactEdgeLabel(edge) {
      return this.quantityText(edge.quantity) + ' ' + (edge.childUnit || '') + '/' + (edge.parentUnit || '')
    },
    pathLabel(path) {
      const nodes = new Map((this.impact.nodes || []).map(node => [node.id, node]))
      const edges = new Map((this.impact.edges || []).map(edge => [edge.id, edge]))
      const codes = path.edgeIds.map(id => {
        const edge = edges.get(id)
        const node = edge && nodes.get(edge.source)
        return node && node.code
      }).filter(Boolean)
      const terminal = nodes.get(path.terminalId)
      if (terminal && terminal.code) codes.push(terminal.code)
      return codes.join(' → ')
    },
    retryLoad() { return this.libraryId ? this.load() : this.loadLibraries() },
    resetFilters() {
      this.search = ''
      this.types = []
      this.relation = ''
      this.clearFocus()
    },
    async loadLibraries() {
      this.busy = true
      this.error = ''
      try {
        const { data } = await (this.$auth.hasRole('admin') ? listAdminDepartmentLibraries() : listDepartmentLibraries())
        this.libraries = data.filter(library => library.status === 'enabled')
        const query = this.$route.query
        const requested = this.libraries.find(library => String(library.libraryId) === query.libraryId)
        this.libraryId = (requested || this.libraries[0] || {}).libraryId
        this.focus = requested && typeof query.node === 'string' ? query.node : ''
        this.selectedId = this.focus
        if (this.libraryId) await this.load()
        if (query.mode === 'impact' && typeof query.itemCode === 'string' && query.itemCode.trim()) {
          await this.enterImpact(query.itemCode)
        }
      } catch (error) {
        this.error = error.message || '部门库暂不可用'
      } finally {
        this.busy = false
      }
    },
    changeLibrary() {
      this.sourceRequest++
      this.sourceVisible = false
      this.sourceBusy = false
      this.preview = {}
      this.graph = emptyGraph()
      this.hops = 1
      this.clearImpact()
      this.resetFilters()
    },
    searchGraph() {
      this.focus = ''
      this.focusName = ''
      this.select('')
      this.load()
    },
    async load() {
      if (!this.libraryId) return
      const generation = ++this.graphRequest
      this.error = ''
      this.busy = true
      this.allSources = false
      try {
        const { data } = await getLibraryGraph(this.libraryId, {
          search: this.search, focus: this.focus, hops: this.hops, types: this.types.join(','), relation: this.relation
        })
        if (generation !== this.graphRequest) return
        this.graph = data
        if (this.focus) this.focusName = (data.nodes.find(node => node.id === this.focus) || {}).name || this.focus
        if (!this.focus && this.search && !this.selected) {
          const query = this.search.toLowerCase()
          const matches = data.nodes.filter(node => [node.name, node.code].some(value => value && value.toLowerCase().includes(query)))
          if (matches.length === 1) this.selectedId = matches[0].id
        }
        if (!this.selected) this.selectedId = this.layout.root
      } catch (error) {
        if (generation !== this.graphRequest) return
        this.graph = emptyGraph()
        this.error = error.message || '图谱加载失败'
        this.selectedId = ''
      } finally {
        if (generation === this.graphRequest) this.busy = false
      }
    },
    select(id) { this.selectedId = id; this.allSources = false },
    inspect(id) {
      this.select(id)
      this.$refs.canvas.reveal(id, true)
    },
    changeHops(hops) {
      if (this.hops === hops && this.focus === this.selectedId) return
      this.hops = hops
      this.expand()
    },
    expand() {
      this.focus = this.selected.id
      this.focusName = this.selected.name
      this.load()
    },
    clearFocus() {
      this.focus = ''
      this.focusName = ''
      this.selectedId = ''
      this.load()
    },
    openStockQuery() {
      this.$router.push({ path: '/mes/ai/chat', query: { stockItem: this.selected.code } })
    },
    async openSource(source) {
      const generation = ++this.sourceRequest
      this.preview = { title: source.title }
      this.previewError = ''
      this.sourceVisible = true
      this.sourceBusy = true
      try {
        const { data } = await previewLibraryDocument(source.libraryId, source.versionId, Math.floor(source.chunkIndex / 20) + 1)
        if (generation !== this.sourceRequest) return
        const chunk = data.chunks.find(row => row.chunkIndex === source.chunkIndex)
        this.preview = { title: source.title, chunk }
        if (!chunk) this.previewError = '此版本的来源分块已不可用。'
      } catch (error) {
        if (generation === this.sourceRequest) this.previewError = error.message || '来源暂不可读取'
      } finally {
        if (generation === this.sourceRequest) this.sourceBusy = false
      }
    },

    // ---------------------------------------------------------------- 影响路径

    clearImpact() {
      this.impactRequest++
      this.impactEdgeRequest++
      this.mode = 'browse'
      this.impact = null
      this.impactItemCode = ''
      this.impactPathIndex = 0
      this.impactEdgeId = ''
      this.impactEdge = null
      this.impactEdgeError = ''
      this.impactEdgeBusy = false
      this.impactError = ''
      this.impactBusy = false
    },
    async enterImpact(itemCode) {
      const code = String(itemCode || '').trim()
      if (!this.libraryId || !code) return
      this.mode = 'impact'
      this.impactItemCode = code
      this.impactPathIndex = 0
      this.impactEdgeId = ''
      this.impactEdge = null
      this.impactEdgeError = ''
      await this.loadImpact()
    },
    async loadImpact() {
      if (!this.libraryId || !this.impactItemCode) return
      const generation = ++this.impactRequest
      this.impactBusy = true
      this.impactError = ''
      this.impactEdgeId = ''
      this.impactEdge = null
      this.impactEdgeError = ''
      this.impactEdgeRequest++
      try {
        const { data } = await getImpactPath(this.libraryId, this.impactItemCode)
        if (generation !== this.impactRequest) return
        this.impact = data
        if (this.impactPathIndex >= (data.paths || []).length) this.impactPathIndex = 0
        this.$nextTick(() => { if (this.$refs.impactCanvas) this.$refs.impactCanvas.fit() })
      } catch (error) {
        if (generation !== this.impactRequest) return
        this.impact = null
        this.impactError = error.message || '影响路径暂不可读取'
      } finally {
        if (generation === this.impactRequest) this.impactBusy = false
      }
    },
    refreshImpact() { return this.loadImpact() },
    leaveImpact() {
      const wasImpact = this.inImpact
      this.clearImpact()
      if (wasImpact && this.$route.query.mode === 'impact') {
        const query = { ...this.$route.query }
        delete query.mode
        delete query.itemCode
        this.$router.replace({ path: '/mes/ai/graph', query }).catch(() => {})
      }
    },
    selectImpactPath(index) {
      this.impactPathIndex = Number(index) || 0
      this.impactEdgeId = ''
      this.impactEdge = null
      this.impactEdgeError = ''
      this.impactEdgeRequest++
      this.$nextTick(() => { if (this.$refs.impactCanvas) this.$refs.impactCanvas.fit() })
    },
    async selectSegment(edgeId) {
      if (!edgeId) return
      this.impactEdgeId = edgeId
      await this.loadImpactEdge(edgeId)
    },
    // 键盘选段：上下键在段列表内移动，焦点跟随
    moveSegment(step) {
      if (!this.impactSegments.length) return
      const current = this.impactSegments.findIndex(segment => segment.id === this.impactEdgeId)
      const next = Math.min(this.impactSegments.length - 1, Math.max(0, current + step))
      this.selectSegment(this.impactSegments[next].id)
      this.$nextTick(() => {
        const buttons = this.$el.querySelectorAll('.impact-segments button')
        if (buttons[next]) buttons[next].focus()
      })
    },
    async loadImpactEdge(edgeId) {
      const index = this.impactSegments.findIndex(segment => segment.id === edgeId)
      if (index < 0 || !this.impactPath) return
      const generation = ++this.impactEdgeRequest
      const via = this.impactEdges.slice(0, index + 1).map(edge => edge.bomId).join(',')
      const parent = this.impactSegments[index].parent
      this.impactEdgeBusy = true
      this.impactEdgeError = ''
      try {
        const { data } = await getImpactEdge(this.libraryId, this.impactSegments[index].edge.bomId, {
          itemCode: this.impactItemCode, via, parentCode: parent.code
        })
        if (generation !== this.impactEdgeRequest) return
        this.impactEdge = data
        this.applyLiveRow(data.bomRow)
      } catch (error) {
        if (generation !== this.impactEdgeRequest) return
        this.impactEdge = null
        this.impactEdgeError = error.message || '这一段暂不可读取'
      } finally {
        if (generation === this.impactEdgeRequest) this.impactEdgeBusy = false
      }
    },
    // 同一行用量改变时，画布标签与右栏同步用实时值
    applyLiveRow(row) {
      if (!row || !this.impact) return
      const edge = (this.impact.edges || []).find(item => String(item.bomId) === String(row.bomId))
        || (this.impact.edges || []).find(item => item.id === this.impactEdgeId)
      if (!edge) return
      edge.quantity = row.quantity
      edge.childUnit = row.childUnit
      edge.parentUnit = row.parentUnit
    },
    async openImpactSource(source) {
      const generation = ++this.sourceRequest
      this.preview = { title: source.title, excerpt: source.excerpt, clauseLabel: source.clauseLabel }
      this.previewError = ''
      this.sourceVisible = true
      this.sourceBusy = true
      try {
        const { data } = await previewLibraryDocument(source.libraryId, source.versionId,
          Math.floor(source.chunkIndex / 20) + 1)
        if (generation !== this.sourceRequest) return
        const chunk = data.chunks.find(item => item.chunkIndex === Number(source.chunkIndex))
        this.preview = { title: source.title, chunk, excerpt: source.excerpt, clauseLabel: source.clauseLabel }
        if (!chunk) this.previewError = '此版本的来源分块已不可用。'
      } catch (error) {
        if (generation === this.sourceRequest) this.previewError = error.message || '来源暂不可读取'
      } finally {
        if (generation === this.sourceRequest) this.sourceBusy = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/assets/styles/mes-palette";
.kb-graph { container-type: inline-size; padding: 22px clamp(12px, 2vw, 30px) 20px; background: var(--color-bg); color: var(--color-text); font-family: $mes-font; }
.kb-graph *, .graph-preview * { box-sizing: border-box; }
.kb-graph button { font-family: inherit; cursor: pointer; }
.kb-graph button:focus-visible { outline: 3px solid var(--color-primary); outline-offset: 2px; }
.graph-heading, .graph-title, .graph-heading__actions, .graph-toolbar, .graph-count, .panel-bar, .neighbor-control, .graph-legend { display: flex; align-items: center; }
.graph-heading { justify-content: space-between; gap: 16px; margin-bottom: 18px; }
.graph-title { gap: 11px; }
.graph-emblem { display: grid; place-items: center; width: 38px; height: 38px; border-radius: 11px; background: var(--button-primary-fill); color: white; font-size: 23px; }
.graph-heading h1 { margin: 0; color: var(--color-text); font-size: clamp(23px, 2vw, 29px); font-weight: 650; letter-spacing: -.04em; line-height: 1.25; }
.graph-heading__actions { gap: 16px; }
.graph-heading__actions .el-select { width: 174px; }
.library-link, .quiet-button { border: 0; background: transparent; padding: 6px 0; color: var(--color-primary); font-size: 13px; }
.library-link i { font-size: 12px; margin-left: 3px; }
.graph-workspace { display: flex; flex-direction: column; height: max(535px, calc(100vh - 182px)); border: 1px solid var(--color-border); border-radius: 15px; overflow: hidden; background: var(--color-card); box-shadow: 0 4px 16px rgba(var(--color-primary-rgb), .04); }
.graph-toolbar { flex-wrap: wrap; gap: 8px; padding: 12px 17px; border-bottom: 1px solid var(--color-border); }
.graph-toolbar > .el-input { width: clamp(195px, 25%, 280px); }
.graph-toolbar > .el-select { width: 120px; }
.graph-toolbar ::v-deep .el-input__inner, .graph-heading__actions ::v-deep .el-input__inner { border-color: var(--color-border-strong); border-radius: 7px; color: var(--color-text); }
.graph-toolbar ::v-deep .el-input__inner::placeholder { color: var(--color-text-secondary); }
.graph-toolbar ::v-deep .el-input__inner:focus, .graph-heading__actions ::v-deep .el-input__inner:focus { border-color: var(--color-primary); }
.graph-toolbar ::v-deep .el-input-group__append { border-color: var(--color-border-strong); padding: 0; background: var(--color-page); border-radius: 0 7px 7px 0; }
.graph-toolbar ::v-deep .el-input-group__append button { color: var(--color-text-secondary); border: 0; background: transparent; padding: 5px 8px; }
.graph-toolbar ::v-deep .el-input-group--append .el-input__inner { border-radius: 7px 0 0 7px; }
.graph-count { margin-left: auto; gap: 4px; color: var(--color-text-secondary); font-size: 12px; white-space: nowrap; }
.graph-count b, .panel-section h3 small { font-variant-numeric: tabular-nums; }
.graph-count b { color: var(--color-text); font-size: 14px; }
.graph-count span { margin: 0 4px; color: var(--color-border-strong); }
.graph-body { min-height: 0; flex: 1; display: grid; grid-template-columns: minmax(0,1fr) clamp(248px, 25%, 318px); }
.graph-body.impact-body { grid-template-columns: minmax(0,1fr) clamp(300px, 27%, 372px); }
.graph-stage {
  position: relative;
  min-width: 0;
  overflow: hidden;
  isolation: isolate;
}
.stage-heading { display: flex; align-items: center; gap: 8px; position: absolute; top: 15px; left: 16px; right: 12px; z-index: 2; pointer-events: none; }
.stage-heading > span { display: inline-flex; align-items: center; gap: 7px; padding: 5px 8px; color: var(--color-text-secondary); font-size: 12px; font-weight: 600; background: var(--color-page); border-radius: 4px; }
.stage-heading > span i { color: var(--color-primary); }
.leave-focus { max-width: 65%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: block; padding: 4px 9px; border-radius: 5px; border: 1px solid var(--color-border); color: var(--color-primary); background: var(--color-primary-bg); pointer-events: auto; }
.graph-inspector { min-height: 0; min-width: 0; display: flex; flex-direction: column; overflow: hidden; background: var(--color-card); border-left: 1px solid var(--color-border); }
.panel-bar { justify-content: space-between; flex-shrink: 0; min-height: 47px; padding: 0 18px; border-bottom: 1px solid var(--color-border); }
.panel-bar strong { font-size: 13px; font-weight: 650; color: var(--color-text); }
.quiet-button { color: var(--color-text-secondary); font-size: 12px; }
.quiet-button i { margin-right: 2px; }
.panel-scroll { overflow: auto; padding: 16px 18px 20px; scrollbar-width: thin; scrollbar-color: var(--color-border-strong) transparent; }
.identity { display: grid; grid-template-columns: 41px minmax(0,1fr); column-gap: 10px; align-items: center; }
.identity-symbol { grid-row: 1 / 3; width: 41px; height: 44px; border-radius: 10px; display: grid; place-items: center; font-size: 23px; }
.identity-type { color: var(--color-text-secondary); font-size: 11px; line-height: 1.4; }
.identity h2 { margin: 2px 0 0; color: var(--color-text); font-weight: 650; line-height: 1.35; font-size: 21px; overflow-wrap: anywhere; }
.identity code { grid-column: 2; color: var(--color-text-secondary); font: 12px Consolas,monospace; margin-top: 3px; overflow-wrap: anywhere; }
.neighbor-control { gap: 7px; margin: 16px 0 0; flex-wrap: wrap; }
.action-button { display: inline-flex; justify-content: center; align-items: center; gap: 6px; min-height: 32px; padding: 0 10px; border: 1px solid var(--color-border-strong); border-radius: 7px; background: var(--color-card); color: var(--color-primary); font: inherit; font-size: 12px; }
.action-button.primary { background: var(--color-primary); border-color: var(--color-primary); color: var(--color-card); }
.action-button.primary:hover { background: var(--color-primary-hover); }
.hop-switch { padding: 2px; background: var(--color-page); border: 1px solid var(--color-border); border-radius: 7px; display: flex; }
.hop-switch button { padding: 4px 7px; min-height: 24px; background: transparent; border: 0; border-radius: 4px; color: var(--color-text-secondary); font-size: 12px; }
.hop-switch button[aria-pressed=true] { color: var(--color-primary); background: var(--color-card); box-shadow: 0 1px 3px rgba(var(--color-primary-rgb), .1); }
.stock-button { display: flex; align-items: center; gap: 6px; width: 100%; margin-top: 10px; padding: 7px 0 0; border: 0; background: transparent; color: var(--color-primary); font-size: 12px; }
.stock-button i:last-child { margin-left: auto; }
.impact-entry { display: flex; align-items: center; gap: 6px; width: 100%; margin-top: 10px; padding: 8px 10px; border: 1px solid var(--color-border); border-radius: 7px; background: var(--color-page); color: var(--color-primary); font-size: 12px; }
.impact-entry:hover { border-color: var(--color-border-strong); background: var(--color-primary-bg); }
.impact-entry i:last-child { margin-left: auto; }
.panel-section { margin-top: 17px; padding-top: 14px; border-top: 1px solid var(--color-border); }
.panel-section h3 { display: flex; align-items: center; justify-content: space-between; margin: 0 0 9px; color: var(--color-text); font-size: 13px; font-weight: 650; }
.panel-section h3 small { font-size: 11px; color: var(--color-text-secondary); font-weight: 450; }
.entity-rows { margin: 0; padding: 0; list-style: none; }
.entity-rows button { width: 100%; display: flex; align-items: center; gap: 8px; padding: 6px 3px; border: 0; background: transparent; color: var(--color-text); border-radius: 5px; text-align: left; }
.entity-rows button:hover, .quiet-button:hover { background: var(--color-page); }
.entity-rows strong { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; font-weight: 500; }
.row-symbol { display: grid; place-items: center; width: 24px; height: 24px; border-radius: 6px; flex-shrink: 0; font-size: 14px; }
.relation-sentence { flex: 1; min-width: 0; color: var(--color-text-secondary); font-size: 12px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.relation-sentence strong { color: var(--color-text); margin: 0 3px; }
.neighbor-arrow { font-size: 11px; color: var(--color-text-secondary); }
.more-sources { padding: 4px 2px; border: 0; background: transparent; color: var(--color-primary); font-size: 12px; }
.source-note { display: block; width: 100%; margin: 6px 0; text-align: left; padding: 10px; border-radius: 7px; border: 1px solid var(--color-border); background: var(--color-page); color: var(--color-text-secondary); }
.source-note:hover { border-color: var(--color-border-strong); }
.source-note > strong { display: flex; gap: 6px; align-items: baseline; color: var(--color-primary); font-size: 12px; overflow-wrap: anywhere; }
.source-note strong i:last-child { margin-left: auto; flex: none; }
.source-note > small { display: block; margin: 5px 0 2px; color: var(--color-text-secondary); font-size: 11px; }
.source-note > span { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; color: var(--color-text-secondary); font-size: 12px; line-height: 1.6; overflow-wrap: anywhere; }
.directory { padding-top: 9px; }
.directory .entity-rows button { padding: 9px 2px; }
.directory-word { min-width: 0; flex: 1; }
.directory-word strong, .directory-word small { display: block; }
.directory-word small { margin-top: 3px; color: var(--color-text-secondary); font: 11px Consolas,"Microsoft YaHei",monospace; }
.graph-legend { flex: none; flex-wrap: wrap; gap: 6px 15px; padding: 10px 17px; border-top: 1px solid var(--color-border); font-size: 11px; color: var(--color-text-secondary); }
.graph-legend strong { padding-right: 12px; border-right: 1px solid var(--color-border); color: var(--color-text-secondary); font-weight: 600; }
.graph-legend span { display: inline-flex; align-items: center; gap: 5px; }
.graph-legend i { width: 7px; height: 7px; border-radius: 2px; }
.graph-legend .relation-legend { gap: 12px; padding-left: 12px; border-left: 1px solid var(--color-border); }
.graph-legend .legend-line { width: 16px; height: 0; border-top: 2px solid var(--color-primary); border-radius: 0; }
.graph-legend .legend-line.dashed { border-top-style: dashed; }
.graph-legend .legend-line.solid { border-top-width: 3px; border-top-color: var(--color-primary-hover); }
.graph-legend small { margin-left: auto; font-size: 11px; }
.graph-empty { position: absolute; inset: 50px 16px; display: grid; place-content: center; justify-items: center; text-align: center; }
.graph-empty > i { width: 52px; height: 52px; display: grid; place-items: center; border: 1px solid var(--color-border); background: var(--color-primary-bg); color: var(--color-primary); font-size: 26px; border-radius: 14px; }
.graph-empty h2 { margin: 12px 0 4px; font-size: 17px; }
.graph-empty p, .graph-hint { font-size: 12px; color: var(--color-text-secondary); line-height: 1.65; }
.graph-empty__actions { display: flex; gap: 8px; margin-top: 10px; }
.graph-preview { padding: 0 20px 24px; }
.graph-preview pre { font: inherit; white-space: pre-wrap; line-height: 1.75; overflow-wrap: anywhere; }
.graph-preview .preview-quote { margin: 0 0 12px; padding: 10px 12px; border-left: 3px solid var(--color-primary); background: var(--color-primary-bg); color: var(--color-text); font-size: 13px; line-height: 1.7; }

/* 影响路径工具栏：用路径工具栏替换普通图谱筛选 */
.impact-toolbar { gap: 10px; }
.back-link { display: inline-flex; align-items: center; gap: 4px; padding: 5px 8px 5px 4px; border: 0; background: transparent; color: var(--color-primary); font-size: 13px; }
.back-link:hover { color: #3c3470; }
.impact-title { padding-left: 10px; border-left: 1px solid #dfe2ea; color: #2c2f42; font-size: 14px; font-weight: 650; }
.impact-toolbar > .el-select { width: clamp(200px, 26%, 330px); }
.impact-progress { color: #6a7182; font-size: 12px; font-variant-numeric: tabular-nums; }

/* 右栏：分段列表 + 选段的 BOM 行与文档佐证 */
.impact-panel { padding-top: 12px; }
.impact-segments { margin: 0 0 12px; padding: 0; list-style: none; display: grid; gap: 6px; }
.impact-segments button { width: 100%; display: grid; grid-template-columns: 18px minmax(0, 1fr); align-items: start; gap: 8px;
  padding: 9px 10px; border: 1px solid #e0e2ec; border-radius: 9px; background: var(--color-card); color: #33394c; text-align: left; }
.impact-segments button:hover { border-color: #b7b0d4; background: #faf9fd; }
.impact-segments button.selected { border-color: var(--color-primary); background: var(--color-primary-bg); box-shadow: 0 0 0 2px #695ba41f; }
.segment-index { display: grid; place-items: center; width: 18px; height: 18px; border-radius: 5px; background: var(--color-primary-bg); color: #565086; font-size: 11px; font-weight: 650; }
.segment-body { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.segment-head { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; min-width: 0; }
.segment-head strong { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; font-weight: 500; color: #353a4d; }
.segment-parent { display: flex; align-items: center; gap: 3px; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #5d6376; font-size: 12px; }
.segment-parent i { flex: none; color: #9399a8; font-size: 10px; }
.segment-qty { flex: none; color: #5a5f74; font-size: 11px; font-variant-numeric: tabular-nums; white-space: nowrap; }
.impact-detail h2 { margin: 2px 0 4px; color: #272a3d; font-size: 18px; font-weight: 650; line-height: 1.45; overflow-wrap: anywhere; }
.impact-meta { margin: 0 0 12px; color: #6b7287; font-size: 12px; }
.usage-box { display: flex; align-items: baseline; justify-content: space-between; gap: 10px; padding: 12px 14px; border-radius: 9px; background: #efecf7; }
.usage-box span { color: #5b5f74; font-size: 12px; }
.usage-box strong { color: #32305c; font-size: 17px; font-weight: 700; font-variant-numeric: tabular-nums; }
.impact-pairs { display: grid; grid-template-columns: 40px minmax(0, 1fr); gap: 10px 10px; margin: 14px 0 0; }
.impact-pairs dt { color: #6b7287; font-size: 12px; line-height: 1.6; }
.impact-pairs dd { margin: 0; min-width: 0; }
.impact-pairs dd strong { display: block; color: #2c3040; font-size: 13px; font-weight: 600; line-height: 1.5; overflow-wrap: anywhere; }
.impact-pairs dd code { display: block; margin-top: 1px; color: #697185; font: 11px Consolas,monospace; }
.impact-evidence { margin-top: 16px; padding-top: 13px; border-top: 1px solid var(--color-border); }
.impact-evidence h3 { display: flex; justify-content: space-between; align-items: center; margin: 0 0 9px; color: var(--color-text); font-size: 13px; font-weight: 650; }
.impact-evidence h3 small { color: var(--color-text-secondary); font-size: 11px; font-weight: 450; }
.evidence-card { display: block; width: 100%; margin: 0 0 8px; padding: 11px 12px; border: 1px solid #dedeeb; border-radius: 9px; background: #fafafd; color: var(--color-text-secondary); text-align: left; }
.evidence-card:hover { border-color: var(--color-border-strong); background: var(--color-card); }
.evidence-card strong { display: flex; align-items: baseline; gap: 6px; color: #423d6b; font-size: 12.5px; overflow-wrap: anywhere; }
.evidence-card small { display: block; margin: 4px 0 6px; color: var(--color-text-secondary); font-size: 11px; }
.evidence-card > span { display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;
  color: #525a6d; font-size: 12px; line-height: 1.6; overflow-wrap: anywhere; }
.evidence-card .evidence-warning { margin-top: 6px; color: #9a5b1f; }
.evidence-card em { display: inline-flex; align-items: center; gap: 3px; margin-top: 8px; color: var(--color-primary); font-style: normal; font-size: 12px; }
@container (max-width: 760px) { .kb-graph {
  .graph-body { grid-template-columns: minmax(0,1fr) 226px; }
  .graph-body.impact-body { grid-template-columns: minmax(0,1fr) 258px; }
  .panel-bar, .panel-scroll { padding-left: 13px; padding-right: 13px; }
  .graph-toolbar { padding: 10px; gap: 6px; }
  .graph-toolbar > .el-select { width: 103px; }
  .graph-count { font-size: 11px; }
}
}
.kb-graph:fullscreen { display: flex; flex-direction: column; width: 100%; height: 100%; padding: 22px 26px; }
.kb-graph:fullscreen .graph-workspace { flex: 1; height: auto; min-height: 0; }
.kb-graph:fullscreen .graph-body { grid-template-columns: minmax(0,1fr) clamp(300px, 25%, 430px); }
.kb-graph:fullscreen .graph-body.impact-body { grid-template-columns: minmax(0,1fr) clamp(330px, 26%, 440px); }
.kb-graph:fullscreen .source-note > span, .kb-graph:fullscreen .relation-sentence,
.kb-graph:fullscreen .entity-rows strong, .kb-graph:fullscreen .source-note > strong { font-size: 14px; }
.kb-graph:fullscreen .panel-section h3 { font-size: 15px; }
.kb-graph:fullscreen .source-note > small { font-size: 12px; }
</style>

<template>
  <div class="app-container kb-graph">
    <header class="graph-heading">
      <div>
        <h1 class="mes-page-heading">知识图谱</h1>
        <p>浏览部门文档中的 MES 实体、关系和来源。</p>
      </div>
      <div class="graph-heading__actions">
        <el-select v-if="libraries.length" v-model="libraryId" size="small" aria-label="部门知识库" @change="changeLibrary">
          <el-option v-for="library in libraries" :key="library.libraryId" :label="library.name" :value="library.libraryId" />
        </el-select>
        <el-button size="small" icon="el-icon-reading" @click="$router.push('/mes/ai/knowledge')">知识库管理</el-button>
      </div>
    </header>

    <div class="graph-workspace" v-loading="busy">
      <section class="graph-stage" aria-label="图谱画布">
        <div class="graph-filters">
          <el-input v-model.trim="search" size="small" clearable label="搜索实体" placeholder="搜索名称或 MES 编码"
            prefix-icon="el-icon-search" maxlength="100" @keyup.enter.native="searchGraph" @clear="searchGraph">
            <el-button slot="append" size="small" @click="searchGraph">搜索</el-button>
          </el-input>
          <el-select v-model="types" size="small" multiple collapse-tags placeholder="实体类型" aria-label="实体类型" @change="load">
            <el-option v-for="(item, id) in entityTypes" :key="id" :label="item.label" :value="id" />
          </el-select>
          <el-select v-model="relation" size="small" clearable placeholder="关系类型" aria-label="关系类型" @change="load">
            <el-option v-for="(label, id) in graphRelations" :key="id" :label="label" :value="id" />
          </el-select>
        </div>

        <div ref="chart" class="graph-chart" />
        <div v-if="!busy && !graph.nodes.length" class="graph-empty" role="status">
          <i class="el-icon-share" aria-hidden="true" />
          <h2>{{ error || (search || types.length || relation || focus ? '当前筛选没有匹配的实体' : '暂时没有可浏览的图谱') }}</h2>
          <p v-if="!error && !search && !types.length && !relation && !focus">可在知识库上传文档，并开启「纳入知识图谱」。</p>
          <el-button v-if="error" size="mini" @click="libraryId ? load() : loadLibraries()">重新加载</el-button>
        </div>

        <div class="graph-bottom">
          <div class="graph-zoom" aria-label="画布控制">
            <el-button-group>
              <el-button size="mini" icon="el-icon-plus" title="放大" aria-label="放大" @click="zoom(1.25)" />
              <el-button size="mini" icon="el-icon-minus" title="缩小" aria-label="缩小" @click="zoom(0.8)" />
              <el-button size="mini" icon="el-icon-aim" title="居中复位" aria-label="居中复位" @click="draw" />
            </el-button-group>
            <el-radio-group v-model="hops" size="mini" aria-label="邻居跳数" @change="focus && load()">
              <el-radio-button :label="1">1 跳</el-radio-button>
              <el-radio-button :label="2">2 跳</el-radio-button>
            </el-radio-group>
          </div>
          <span v-if="focus" class="graph-focus">当前聚焦 {{ focusName }} <button type="button" @click="clearFocus">取消聚焦</button></span>
        </div>
      </section>

      <aside class="graph-inspector" aria-label="图谱详情">
        <template v-if="selected">
          <div class="graph-panel-head">
            <span class="graph-type"><i :style="{ backgroundColor: typeInfo(selected.type).color }" />{{ typeInfo(selected.type).label }}</span>
            <el-button size="mini" type="text" icon="el-icon-close" aria-label="关闭节点详情" @click="select('')" />
          </div>
          <h2>{{ selected.name }}</h2>
          <p v-if="selected.code" class="graph-code">MES 编码 <code>{{ selected.code }}</code></p>
          <div class="graph-node-actions">
            <el-button size="small" icon="el-icon-share" @click="expand">展开邻居</el-button>
            <el-button v-if="stockAllowed" size="small" type="primary" icon="el-icon-data-analysis" @click="openStockQuery">查实时库存</el-button>
          </div>
          <section class="graph-sources">
            <h3>来源分块</h3>
            <button v-for="source in selected.sources" :key="source.versionId + ':' + source.chunkIndex" type="button"
              class="graph-source" @click="openSource(source)">
              <strong><i class="el-icon-document" /> {{ source.title }}</strong>
              <small>{{ source.sectionLabel }}</small>
              <span>{{ source.excerpt }}</span>
            </button>
          </section>
        </template>
        <template v-else>
          <div class="graph-panel-head"><h2>实体</h2><span>{{ graph.nodes.length }} 个节点 · {{ graph.edges.length }} 条关系</span></div>
          <p v-if="graph.limited" class="graph-hint">达到显示上限，可按名称或实体类型缩小范围。</p>
          <div v-if="graph.nodes.length" class="graph-entity-list">
            <button v-for="node in sortedNodes" :key="node.id" type="button" @click="select(node.id)">
              <i :style="{ backgroundColor: typeInfo(node.type).color }" />
              <span><strong>{{ node.name }}</strong><small>{{ node.code || typeInfo(node.type).label }}</small></span>
              <i class="el-icon-arrow-right" aria-hidden="true" />
            </button>
          </div>
          <p v-else class="graph-hint">有图谱数据后，可以在这里选择实体。</p>
        </template>
      </aside>
    </div>

    <el-drawer :title="preview.title" :visible.sync="sourceVisible" size="min(440px, 100vw)" append-to-body>
      <section v-loading="sourceBusy" class="graph-preview">
        <p v-if="previewError">{{ previewError }}</p>
        <template v-else-if="preview.chunk">
          <h3>{{ preview.chunk.sectionLabel }}</h3>
          <pre>{{ preview.chunk.content }}</pre>
        </template>
      </section>
    </el-drawer>
  </div>
</template>

<script>
import echarts from 'echarts'
import { getLibraryGraph, listAdminDepartmentLibraries, listDepartmentLibraries, previewLibraryDocument } from '@/api/mes/knowledgeLibrary'
import { entityTypes, graphRelations } from '@/utils/kbGraph'

const emptyGraph = () => ({ nodes: [], edges: [], limited: false })

export default {
  name: 'KnowledgeGraph',
  data() {
    return {
      entityTypes, graphRelations, libraries: [], libraryId: null, graph: emptyGraph(),
      search: '', types: [], relation: '', hops: 1, focus: '', focusName: '', selectedId: '',
      error: '', busy: true, graphRequest: 0, sourceRequest: 0, sourceVisible: false, sourceBusy: false,
      preview: {}, previewError: ''
    }
  },
  computed: {
    selected() { return this.graph.nodes.find(node => node.id === this.selectedId) },
    sortedNodes() { return this.graph.nodes.slice().sort((a, b) => (a.type === 'Document') - (b.type === 'Document')) },
    stockAllowed() {
      return this.selected && ['Item', 'Product'].includes(this.selected.type) && this.selected.code
        && this.$auth.hasPermi('mes:wm:wmstock:list')
    }
  },
  mounted() {
    this.chart = echarts.init(this.$refs.chart)
    this.chart.on('click', event => { if (event.dataType === 'node') this.select(event.data.id) })
    this.resizeObserver = new ResizeObserver(() => this.chart.resize())
    this.resizeObserver.observe(this.$refs.chart)
    this.loadLibraries()
  },
  beforeDestroy() {
    this.graphRequest++
    this.sourceRequest++
    this.resizeObserver.disconnect()
    this.chart.dispose()
  },
  methods: {
    typeInfo(type) { return entityTypes[type] || { label: type, color: '#798b9e' } },
    async loadLibraries() {
      this.busy = true
      this.error = ''
      try {
        const { data } = await (this.$auth.hasRole('admin') ? listAdminDepartmentLibraries() : listDepartmentLibraries())
        this.libraries = data.filter(library => library.status === 'enabled')
        const requested = this.libraries.find(library => String(library.libraryId) === this.$route.query.libraryId)
        this.libraryId = (requested || this.libraries[0] || {}).libraryId
        this.focus = requested && typeof this.$route.query.node === 'string' ? this.$route.query.node : ''
        this.selectedId = this.focus
        if (this.libraryId) await this.load()
      } catch (error) {
        this.error = error.message || '部门库暂不可用'
      } finally {
        this.busy = false
      }
    },
    changeLibrary() {
      this.search = ''
      this.types = []
      this.relation = ''
      this.clearFocus()
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
      try {
        const { data } = await getLibraryGraph(this.libraryId, {
          search: this.search, focus: this.focus, hops: this.hops, types: this.types.join(','), relation: this.relation
        })
        if (generation !== this.graphRequest) return
        this.graph = data
        if (this.focus && this.selected) this.focusName = this.selected.name
        if (!this.focus && this.search && !this.selected) {
          const query = this.search.toLowerCase()
          const matches = data.nodes.filter(node => [node.name, node.code].some(value => value && value.toLowerCase().includes(query)))
          if (matches.length === 1) this.selectedId = matches[0].id
        }
        this.draw()
      } catch (error) {
        if (generation !== this.graphRequest) return
        this.graph = emptyGraph()
        this.error = error.message || '图谱加载失败'
        this.draw()
      } finally {
        if (generation === this.graphRequest) this.busy = false
      }
    },
    select(id) {
      this.selectedId = id
      this.chart.dispatchAction({ type: 'downplay', seriesIndex: 0 })
      const dataIndex = this.graph.nodes.findIndex(node => node.id === id)
      if (dataIndex !== -1) this.chart.dispatchAction({ type: 'highlight', seriesIndex: 0, dataIndex })
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
    draw() {
      if (!this.chart) return
      const data = this.graph.nodes.map(node => ({
        id: node.id, name: node.name, symbol: node.type === 'Document' ? 'roundRect' : 'circle',
        symbolSize: node.id === this.selectedId ? 25 : 17, itemStyle: { color: this.typeInfo(node.type).color }
      }))
      this.chart.setOption({
        animationDurationUpdate: 200,
        series: [{
          type: 'graph', layout: 'force', roam: true, draggable: true, focusNodeAdjacency: true,
          force: { initLayout: 'circular', repulsion: 520, edgeLength: [130, 190] },
          scaleLimit: { min: 0.45, max: 3.5 }, zoom: 1, center: null, data,
          links: this.graph.edges.map(edge => ({ ...edge, name: graphRelations[edge.kind] || edge.kind })),
          label: { show: data.length <= 35, position: 'bottom', color: '#283c4f', fontSize: 12, distance: 5 },
          emphasis: { label: { show: true, fontWeight: 600 } },
          edgeLabel: {
            show: this.graph.edges.length <= 20, formatter: event => graphRelations[event.data.kind] || event.data.kind,
            color: '#576e82', fontSize: 11
          },
          edgeSymbol: ['none', 'arrow'], edgeSymbolSize: 7,
          lineStyle: { color: '#8ba0b5', width: 1.25, opacity: 0.72, curveness: 0.12 }
        }]
      }, true)
      this.select(this.selectedId)
    },
    zoom(factor) {
      const series = this.chart.getOption().series[0]
      if (series) this.chart.setOption({ series: [{ zoom: Math.max(0.45, Math.min(3.5, series.zoom * factor)) }] })
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/assets/styles/mes-palette";
.kb-graph { padding: 22px 26px; color: $mes-text; }
.graph-heading, .graph-heading__actions, .graph-panel-head, .graph-bottom, .graph-zoom { display: flex; align-items: center; gap: 10px; }
.graph-heading, .graph-panel-head { justify-content: space-between; }
.graph-heading { padding-bottom: 16px; }
.graph-heading h1 { margin: 0; }
.graph-heading p, .graph-code, .graph-hint { margin: 6px 0 0; font-size: 13px; line-height: 1.5; color: $mes-text-sub; }
.graph-workspace { display: grid; grid-template-columns: minmax(0, 1fr) 310px; min-height: 490px; height: calc(100vh - 185px); border: 1px solid $mes-border; border-radius: 12px; overflow: hidden; background: $mes-card; }
.graph-stage { position: relative; min-width: 0; background: radial-gradient(circle, #bdccdb 0.8px, transparent 0.9px) 1px 1px / 17px 17px, #f8fafc; }
.graph-chart { position: absolute; inset: 0; }
.graph-filters, .graph-bottom { position: absolute; left: 14px; z-index: 2; }
.graph-filters { top: 14px; right: 14px; display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
.graph-filters > .el-input { width: 264px; }
.graph-filters > .el-select { width: 128px; }
.graph-bottom { bottom: 14px; right: 14px; flex-wrap: wrap; }
.graph-focus { padding: 6px 9px; border: 1px solid $mes-border; border-radius: 6px; background: $mes-card; font-size: 12px; }
.graph-focus button { background: none; border: 0; margin-left: 5px; color: $mes-primary; cursor: pointer; }
.graph-empty { position: absolute; z-index: 1; inset: 0; display: grid; place-content: center; justify-items: center; text-align: center; padding: 75px 24px; }
.graph-empty > i { font-size: 28px; color: $mes-primary; }
.graph-empty h2 { font-size: 16px; margin-bottom: 4px; }
.graph-empty p { color: $mes-text-sub; font-size: 13px; }
.graph-inspector { min-width: 0; overflow: auto; padding: 16px; border-left: 1px solid $mes-border; }
.graph-inspector h2 { font-size: 17px; margin: 6px 0 4px; line-height: 1.35; overflow-wrap: anywhere; }
.graph-panel-head > span { color: $mes-text-sub; font-size: 12px; }
.graph-type { display: inline-flex; gap: 6px; align-items: center; }
.graph-type > i, .graph-entity-list button > i:first-child { display: inline-block; width: 8px; height: 8px; flex: 0 0 8px; border-radius: 50%; }
.graph-code code { color: $mes-primary; padding-left: 6px; }
.graph-node-actions { display: flex; flex-wrap: wrap; gap: 8px; margin: 15px 0; }
.graph-node-actions .el-button + .el-button { margin-left: 0; }
.graph-sources h3 { font-size: 13px; margin: 18px 0 8px; }
.graph-source, .graph-entity-list button { font: inherit; color: inherit; display: block; width: 100%; text-align: left; cursor: pointer; background: $mes-card; }
.graph-source { padding: 9px; margin: 0 0 7px; border: 1px solid $mes-border; border-radius: 7px; }
.graph-source > strong, .graph-source > small, .graph-source > span { display: block; overflow-wrap: anywhere; }
.graph-source > strong { color: $mes-primary; font-size: 13px; }
.graph-source > small, .graph-source > span, .graph-entity-list small { color: $mes-text-sub; font-size: 12px; line-height: 1.45; margin-top: 3px; }
.graph-entity-list { margin: 10px 0; }
.graph-entity-list button { display: flex; align-items: center; gap: 9px; padding: 9px 3px; border: 0; border-bottom: 1px solid $mes-border; }
.graph-entity-list button > span { flex: 1; overflow-wrap: anywhere; }
.graph-entity-list strong, .graph-entity-list small { display: block; font-size: 12px; }
.graph-source:hover, .graph-entity-list button:hover { background: $mes-primary-soft; }
.graph-preview { padding: 0 20px 24px; }
.graph-preview pre { font: inherit; white-space: pre-wrap; line-height: 1.65; overflow-wrap: anywhere; }
@media (max-width: 920px) { .graph-workspace { grid-template-columns: minmax(0, 1fr) 255px; } }
@media (max-width: 680px) {
  .kb-graph { padding: 12px; }
  .graph-heading { align-items: flex-start; flex-direction: column; }
  .graph-workspace { display: block; height: auto; }
  .graph-stage { min-height: 440px; }
  .graph-inspector { border-left: 0; border-top: 1px solid $mes-border; max-height: 400px; }
}
</style>

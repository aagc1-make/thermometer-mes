<template>
  <el-dialog
    :title="pathOnly ? '本次个人问答图谱' : '我的知识图谱'"
    :visible.sync="visible"
    width="min(1060px, calc(100vw - 24px))"
    append-to-body
    @opened="loadGraph"
    @close="disposeChart"
  >
    <div v-loading="loading" class="knowledge-graph">
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon><el-button type="text" @click="loadGraph">重新加载</el-button></el-alert>
      <el-alert v-else-if="pathNotice" :title="pathNotice" type="warning" :closable="false" show-icon />
      <p v-else-if="!loading && !graph.nodes.length" class="knowledge-graph__hint">当前没有可显示的图谱关系。</p>
      <div v-if="graph.nodes.length" class="knowledge-graph__tools">
        <span>{{ graph.nodes.length }} 个节点 · {{ graph.links.length }} 条关系</span>
        <label>查看节点
          <select :value="selectedNode ? selectedNode.id : ''" @change="selectNode($event.target.value)">
            <option value="">请选择节点</option>
            <option v-for="node in graph.nodes" :key="node.id" :value="node.id">{{ node.name }}{{ node.kind ? ' · ' + node.kind : '' }}</option>
          </select>
        </label>
      </div>
      <div v-if="graph.nodes.length" ref="canvas" class="knowledge-graph__canvas" aria-hidden="true" />

      <section v-if="selectedNode" class="knowledge-graph__detail">
        <h4>{{ selectedNode.name }}<span v-if="selectedNode.kind"> · {{ selectedNode.kind }}</span></h4>
        <p v-if="!adjacentLinks.length">没有相邻关系。</p>
        <ul v-else>
          <li v-for="link in adjacentLinks" :key="link.id">
            <el-button type="text" @click="selectLink(link)">{{ relationText(link) }}</el-button>
            <span v-if="link.fileName"> · {{ link.fileName }}</span>
          </li>
        </ul>
      </section>
      <section v-else-if="selectedLink" class="knowledge-graph__detail">
        <h4>{{ relationText(selectedLink) }}</h4>
        <p>{{ selectedLink.fileName }} · 版本 {{ selectedLink.version }}<span v-if="selectedLink.location"> · {{ selectedLink.location }}</span></p>
        <blockquote>{{ selectedLink.evidence }}</blockquote>
      </section>
      <p v-else-if="!loading && graph.nodes.length" class="knowledge-graph__hint">点击节点查看相邻关系，点击连线查看原文出处。</p>
    </div>
  </el-dialog>
</template>

<script>
import * as echarts from 'echarts'
import request from '@/utils/request'

export default {
  name: 'PersonalKnowledgeGraph',
  props: { userId: { type: [String, Number], default: '' } },
  watch: {
    userId() {
      this.visible = false
      this.disposeChart()
      this.graph = { nodes: [], links: [] }
      this.selectedNode = null
      this.selectedLink = null
    }
  },
  data() {
    return {
      visible: false,
      loading: false,
      error: '',
      factIds: [],
      graph: { nodes: [], links: [] },
      selectedNode: null,
      selectedLink: null
    }
  },
  computed: {
    pathOnly() {
      return this.factIds.length > 0
    },
    pathNotice() {
      if (!this.pathOnly || this.loading || this.error) return ''
      if (!this.graph.links.length) return '这次的关系已全部不在当前版本'

      const returnedIds = new Set(this.graph.links.map(link => link.id))
      return this.factIds.some(id => !returnedIds.has(id)) ? '这次的部分关系已不在当前版本' : ''
    },
    nodesById() {
      return new Map(this.graph.nodes.map(node => [node.id, node]))
    },
    adjacentLinks() {
      if (!this.selectedNode) return []
      const id = this.selectedNode.id
      return this.graph.links.filter(link => link.source === id || link.target === id)
    }
  },
  created() {
    this.chart = null
    this.requestId = 0
  },
  beforeDestroy() {
    this.disposeChart()
  },
  methods: {
    open(factIds = []) {
      const wasVisible = this.visible
      this.disposeChart()
      this.factIds = factIds
      this.graph = { nodes: [], links: [] }
      this.selectedNode = null
      this.selectedLink = null
      this.error = ''
      this.loading = true
      this.visible = true
      if (wasVisible) this.loadGraph()
    },
    async loadGraph() {
      this.error = ''
      this.loading = true
      const requestId = ++this.requestId
      try {
        const { data } = await request({
          url: '/mes/knowledge/graph',
          method: 'get',
          params: this.pathOnly ? { factIds: this.factIds.join(',') } : undefined
        })
        if (requestId !== this.requestId || !this.visible) return
        if (!data || !Array.isArray(data.nodes) || !Array.isArray(data.links)) {
          throw new Error('知识图谱接口返回了无效的数据')
        }

        this.graph = data
        await this.$nextTick()
        if (requestId === this.requestId && this.visible && this.$refs.canvas) {
          this.renderGraph(data)
        }
      } catch (error) {
        if (requestId === this.requestId && this.visible) {
          this.error = (error && error.message) || '读取知识图谱失败'
        }
      } finally {
        if (requestId === this.requestId) this.loading = false
      }
    },
    renderGraph(graph) {
      const nodeIndex = new Map(graph.nodes.map((node, index) => [node.id, index]))
      const linksById = new Map(graph.links.map(link => [link.id, link]))

      const chart = echarts.init(this.$refs.canvas)
      this.chart = chart
      this.compactChart = chart.getWidth() < 480
      chart.setOption({
        animation: !window.matchMedia('(prefers-reduced-motion: reduce)').matches,
        color: ['#a2462e', '#76654f', '#94724b', '#546b65'],
        series: [{
          type: 'graph',
          layout: 'force',
          roam: true,
          draggable: true,
          ...this.graphLayout(),
          lineStyle: { color: '#8a796b', opacity: .9 },
          edgeLabel: { show: true, formatter: params => params.data.relationLabel },
          edgeSymbol: ['none', 'arrow'],
          autoCurveness: true,
          data: graph.nodes.map(node => ({ id: node.id, name: node.name })),
          links: graph.links.map(link => ({
            id: link.id,
            source: nodeIndex.get(link.source),
            target: nodeIndex.get(link.target),
            factId: link.id,
            relationLabel: link.label,
            lineStyle: { width: this.pathOnly ? 3 : 1.5 }
          }))
        }]
      })
      chart.on('click', event => {
        if (event.dataType === 'node') {
          this.selectNode(event.data.id)
        } else if (event.dataType === 'edge') {
          this.selectLink(linksById.get(event.data.factId))
        }
      })
      window.addEventListener('resize', this.resizeChart)
    },
    graphLayout() {
      const compact = this.compactChart
      return {
        zoom: compact ? 0.8 : 1,
        symbolSize: compact ? 28 : 44,
        label: {
          show: true,
          position: compact ? 'bottom' : 'right',
          color: '#28251f',
          formatter: ({ name }) => compact && name.length > 8 ? `${name.slice(0, 8)}…` : name
        },
        force: { repulsion: compact ? 110 : 280, edgeLength: compact ? 84 : 150 }
      }
    },
    relationText(link) {
      const source = this.nodesById.get(link.source)
      const target = this.nodesById.get(link.target)
      return `${source ? source.name : link.source} — ${link.label} → ${target ? target.name : link.target}`
    },
    selectNode(id) {
      this.selectedNode = this.nodesById.get(id) || null
      this.selectedLink = null
    },
    selectLink(link) {
      this.selectedLink = link || null
      this.selectedNode = null
    },
    resizeChart() {
      if (!this.chart) return
      this.chart.resize()
      const compact = this.chart.getWidth() < 480
      if (compact !== this.compactChart) {
        this.compactChart = compact
        this.chart.setOption({ series: [this.graphLayout()] })
      }
    },
    disposeChart() {
      this.requestId++
      if (this.chart) {
        window.removeEventListener('resize', this.resizeChart)
        this.chart.dispose()
        this.chart = null
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.knowledge-graph {
  min-height: 84px;

  &__tools {
    display: flex; align-items: center; justify-content: space-between; gap: 8px; flex-wrap: wrap; margin: 10px 0;
    label { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
    select { min-height: 38px; max-width: 100%; border: 1px solid var(--color-border-strong); border-radius: 6px; padding: 0 6px; background: var(--color-bg); color: var(--color-text); }
  }
  &__canvas { width: 100%; height: min(460px, 55vh); border: 1px solid var(--color-border); border-radius: 10px; background: var(--color-primary-soft); }
  @media (max-width: 540px) {
    &__canvas { height: 280px; }
  }
  &__hint { color: var(--color-text-secondary); margin: 12px 0; }
  &__detail {
    padding: 12px 16px;
    border-top: 1px solid var(--color-border);
    overflow-wrap: anywhere;

    h4 { margin: 0 0 8px; }
    ul { margin: 8px 0 0; padding-left: 20px; }
    .el-button { white-space: normal; text-align: left; }
    blockquote {
      margin: 8px 0 0;
      padding: 10px 14px;
      border: 1px solid var(--color-border);
      background: var(--color-primary-soft);
      white-space: pre-wrap;
    }
  }
}
</style>

<template>
  <div class="atlas-canvas" :class="{ impact: variant === 'impact' }">
    <div ref="surface" class="atlas-surface" role="region" aria-label="知识图谱画布" tabindex="0"
      @pointerdown="startDrag" @pointermove="moveDrag" @pointerup="finishDrag" @pointercancel="finishDrag"
      @lostpointercapture="finishDrag" @wheel.prevent="wheel" @keydown.self="navigate">
      <div class="atlas-world" :style="{ transform: 'translate(' + camera.x + 'px,' + camera.y + 'px) scale(' + camera.scale + ')' }">
        <svg width="1" height="1" class="atlas-lines" aria-hidden="true">
          <defs>
            <marker v-for="mark in ['normal', 'active']" :id="markerId + mark" :key="mark" viewBox="0 0 8 8"
              refX="8" refY="4" markerWidth="7" markerHeight="7" orient="auto" markerUnits="userSpaceOnUse">
              <path d="M 0 0 L 8 4 L 0 8 Z" :fill="mark === 'active' ? 'var(--color-primary)' : 'var(--color-text)'" />
            </marker>
          </defs>
          <g v-for="wire in wires" :key="wire.key" class="atlas-wire" :class="{ active: wire.active, muted: wire.muted }">
            <path class="wire-line" :d="wire.path" fill="none" :stroke-dasharray="wire.kind === 'MENTIONS' ? '4 4' : null"
              :marker-end="'url(#' + markerId + (wire.active ? 'active' : 'normal') + ')'" />
            <path v-if="edgeSelectable" class="wire-hit" :d="wire.path" :data-edge="wire.id" @click.stop="chooseEdge(wire.id)" />
            <g v-if="wire.showLabel" class="wire-label" :data-edge="wire.id" :transform="'translate(' + wire.x + ',' + wire.y + ')'"
              @click.stop="chooseEdge(wire.id)">
              <rect :x="-wire.labelWidth / 2" y="-10" :width="wire.labelWidth" height="20" rx="5" />
              <text y="4" text-anchor="middle">{{ wire.label }}</text>
            </g>
          </g>
        </svg>
        <button v-for="card in cards" :key="card.node.id" type="button" class="atlas-node"
          :data-node="card.node.id" :aria-pressed="card.node.id === selectedId || endpoints.has(card.node.id)"
          :title="card.node.name"
          :class="{ selected: card.node.id === selectedId, peripheral: selectedId && !adjacent.has(card.node.id),
            endpoint: endpoints.has(card.node.id), detail: detailed }"
          :style="{ left: card.x + 'px', top: card.y + 'px', width: metrics.width + 'px', height: metrics.height + 'px',
            '--ink': card.style.color, '--wash': card.style.tint }"
          @click="choose(card.node.id, $event)" @focus="reveal(card.node.id)">
          <i class="node-symbol" :class="card.style.icon" aria-hidden="true" />
          <span class="node-word">
            <small v-if="detailed" class="node-category">{{ category(card.node) }}</small>
            <strong>{{ card.node.name }}</strong>
            <code v-if="detailed && card.node.code">{{ card.node.code }}</code>
          </span>
        </button>
      </div>
    </div>
    <div v-if="nodes.length" class="atlas-zoom" aria-label="画布控制">
      <button type="button" title="缩小" aria-label="缩小" :disabled="camera.scale <= minScale" @click="zoomBy(1 / 1.25)"><i class="el-icon-minus" /></button>
      <output aria-label="缩放比例">{{ Math.round(camera.scale * 100) }}%</output>
      <button type="button" title="放大" aria-label="放大" :disabled="camera.scale >= 2.5" @click="zoomBy(1.25)"><i class="el-icon-plus" /></button>
      <button type="button" class="fit" title="适应画布" aria-label="适应画布" @click="fit"><i class="el-icon-full-screen" /></button>
    </div>
  </div>
</template>

<script>
import { entityStyle, graphRelations } from '@/utils/kbGraph'
import { cardHeight, cardWidth, connectCards } from '@/utils/kbGraphLayout'

export default {
  name: 'KnowledgeGraphCanvas',
  props: {
    nodes: { type: Array, required: true },
    edges: { type: Array, required: true },
    layout: { type: Object, required: true },
    selectedId: { type: String, default: '' },
    // graph：普通关系网络；impact：物料影响路径，卡片带类型与编码，可点选一段装配
    variant: { type: String, default: 'graph' },
    selectedEdgeId: { type: String, default: '' },
    originId: { type: String, default: '' },
    edgeLabel: { type: Function, default: null },
    size: { type: Object, default: () => ({}) }
  },
  data() {
    return { camera: { x: 0, y: 0, scale: 1 }, movedNodes: {}, minScale: 0.15, fitted: false, wasHidden: false }
  },
  computed: {
    markerId() { return 'atlas-tip-' + this._uid + '-' },
    detailed() { return this.variant === 'impact' },
    edgeSelectable() { return this.variant === 'impact' },
    metrics() {
      return this.detailed
        ? { width: this.size.width || 236, height: this.size.height || 86 }
        : { width: this.size.width || cardWidth, height: this.size.height || cardHeight }
    },
    cards() {
      return this.nodes.map(node => ({
        node, style: entityStyle(node.type), ...(this.movedNodes[node.id] || this.layout.positions.get(node.id))
      }))
    },
    adjacent() {
      const ids = new Set([this.selectedId])
      this.edges.forEach(edge => {
        if (edge.source === this.selectedId) ids.add(edge.target)
        if (edge.target === this.selectedId) ids.add(edge.source)
      })
      return ids
    },
    endpoints() {
      const ids = new Set()
      if (!this.selectedEdgeId) return ids
      this.edges.forEach(edge => {
        if (edge.id === this.selectedEdgeId) {
          ids.add(edge.source)
          ids.add(edge.target)
        }
      })
      return ids
    },
    wires() {
      const cards = new Map(this.cards.map(card => [card.node.id, card]))
      const groups = new Map()
      this.edges.forEach(edge => {
        const pair = JSON.stringify([edge.source, edge.target].sort())
        if (!groups.has(pair)) groups.set(pair, [])
        groups.get(pair).push(edge)
      })
      const output = []
      groups.forEach(group => group.forEach((edge, index) => {
        const from = cards.get(edge.source)
        const to = cards.get(edge.target)
        if (!from || !to) return
        const active = this.detailed
          ? edge.id === this.selectedEdgeId
          : edge.source === this.selectedId || edge.target === this.selectedId
        const muted = !this.detailed && Boolean(this.selectedId) && !active
        const label = (this.edgeLabel && this.edgeLabel(edge)) || graphRelations[edge.kind] || edge.kind
        const geometry = edge.source === edge.target
          ? { path: `M ${from.x + 18} ${from.y - 33} c 32 -48 -68 -48 -36 0`, x: from.x, y: from.y - 68 }
          : connectCards(from, to, (index - (group.length - 1) / 2) * 42, this.metrics)
        const labelWidth = Math.max(38, label.length * 12 + 12)
        const touchesCard = this.cards.some(card => Math.abs(card.x - geometry.x) < (this.metrics.width + labelWidth) / 2 + 2 &&
          Math.abs(card.y - geometry.y) < this.metrics.height / 2 + 12)
        const touchesLabel = output.some(other => other.showLabel && Math.abs(other.x - geometry.x) < (other.labelWidth + labelWidth) / 2 &&
          Math.abs(other.y - geometry.y) < 22)
        output.push({ ...geometry, key: edge.id || JSON.stringify([edge.source, edge.target, edge.kind]),
          id: edge.id || JSON.stringify([edge.source, edge.target, edge.kind]), kind: edge.kind, active, muted, label, labelWidth,
          showLabel: this.detailed || (!touchesCard && !touchesLabel && (this.edges.length <= 10 || (active && this.edges.length <= 35))) })
      }))
      return output
    }
  },
  watch: {
    layout() { this.movedNodes = {}; this.$nextTick(this.fit) }
  },
  mounted() {
    this.observer = new ResizeObserver(() => this.onResize())
    this.observer.observe(this.$refs.surface)
    this.onResize()
  },
  beforeDestroy() { this.observer.disconnect() },
  methods: {
    // 切到影响路径时画布只是隐藏；恢复时保留原先的缩放与位置，不做自动适应
    onResize() {
      const box = this.$refs.surface
      if (!box) return
      if (!box.clientWidth || !box.clientHeight) {
        if (this.fitted) this.wasHidden = true
        return
      }
      if (this.wasHidden) {
        this.wasHidden = false
        if (this.fitted) return
      }
      this.fit()
    },
    category(node) {
      const label = node.categoryName || entityStyle(node.type).label
      return node.id === this.originId ? label + ' · 起点' : label
    },
    fit() {
      if (!this.cards.length || !this.$refs.surface) return
      const { clientWidth: w, clientHeight: h } = this.$refs.surface
      if (!w || !h) return
      const xs = this.cards.map(card => card.x)
      const ys = this.cards.map(card => card.y)
      const left = Math.min(...xs) - this.metrics.width / 2
      const top = Math.min(...ys) - this.metrics.height / 2
      const width = Math.max(...xs) + this.metrics.width / 2 - left
      const height = Math.max(...ys) + this.metrics.height / 2 - top
      const scale = Math.max(0.025, Math.min(1.16, (w - 55) / width, (h - 102) / height))
      this.minScale = Math.min(0.25, scale)
      this.camera = { scale, x: (w - width * scale) / 2 - left * scale, y: (h - height * scale) / 2 - top * scale - 8 }
      this.fitted = true
    },
    zoomBy(factor, px, py) {
      const box = this.$refs.surface
      const x = px == null ? box.clientWidth / 2 : px
      const y = py == null ? box.clientHeight / 2 : py
      const scale = Math.min(2.5, Math.max(this.minScale, this.camera.scale * factor))
      const ratio = scale / this.camera.scale
      this.camera = { scale, x: x - (x - this.camera.x) * ratio, y: y - (y - this.camera.y) * ratio }
    },
    wheel(event) {
      const rect = this.$refs.surface.getBoundingClientRect()
      this.zoomBy(Math.exp(-Math.sign(event.deltaY) * 0.11), event.clientX - rect.left, event.clientY - rect.top)
    },
    reveal(id, readable = false) {
      const card = this.cards.find(row => row.node.id === id)
      if (!card) return
      const { clientWidth: w, clientHeight: h } = this.$refs.surface
      const scale = this.camera.scale
      const x = card.x * scale + this.camera.x
      const y = card.y * scale + this.camera.y
      if (x < this.metrics.width * scale / 2 || x > w - this.metrics.width * scale / 2 || y < 35 || y > h - 55 || (readable && scale < 0.72)) {
        const next = readable ? Math.max(0.82, scale) : scale
        this.camera = { scale: next, x: w / 2 - card.x * next, y: h / 2 - card.y * next }
      }
    },
    startDrag(event) {
      if (event.button !== 0 || !event.isPrimary) return
      // 命中装配段（箭头或用量标签）时不拖动画布，交给点击选段
      if (this.edgeSelectable && event.target.closest('[data-edge]')) return
      const el = event.target.closest('[data-node]')
      const id = el && el.dataset.node
      const origin = id ? this.cards.find(card => card.node.id === id) : this.camera
      this.drag = { id, pointer: event.pointerId, startX: event.clientX, startY: event.clientY, x: origin.x, y: origin.y, moved: false }
      this.suppressClick = false
    },
    moveDrag(event) {
      const drag = this.drag
      if (!drag || drag.pointer !== event.pointerId) return
      if (event.buttons === 0) { this.finishDrag(event); return }
      const dx = event.clientX - drag.startX
      const dy = event.clientY - drag.startY
      if (!drag.moved && Math.hypot(dx, dy) < 4) return
      if (!drag.moved) this.$refs.surface.setPointerCapture(drag.pointer)
      drag.moved = true
      if (drag.id) {
        this.$set(this.movedNodes, drag.id, { x: drag.x + dx / this.camera.scale, y: drag.y + dy / this.camera.scale })
      } else {
        this.camera.x = drag.x + dx
        this.camera.y = drag.y + dy
      }
    },
    finishDrag(event) {
      if (!this.drag || event.pointerId !== this.drag.pointer) return
      this.suppressClick = this.drag.moved
      this.drag = null
    },
    choose(id, event) {
      if (!this.suppressClick || event.detail === 0) this.$emit('select', id)
      this.suppressClick = false
    },
    chooseEdge(id) {
      if (this.edgeSelectable) this.$emit('select-edge', id)
    },
    navigate(event) {
      const offsets = { ArrowLeft: [45, 0], ArrowRight: [-45, 0], ArrowUp: [0, 45], ArrowDown: [0, -45] }
      if (offsets[event.key]) {
        event.preventDefault()
        this.camera.x += offsets[event.key][0]
        this.camera.y += offsets[event.key][1]
      } else if (event.key === '+' || event.key === '=') this.zoomBy(1.25)
      else if (event.key === '-') this.zoomBy(0.8)
      else if (event.key === 'Home') { event.preventDefault(); this.fit() }
    }
  }
}
</script>

<style lang="scss" scoped>
.atlas-canvas, .atlas-surface { position: absolute; inset: 0; }
.atlas-canvas {
  background-color: var(--color-page);
  background-image:
    radial-gradient(circle, rgba(var(--color-primary-rgb), .18) .7px, transparent 1px),
    radial-gradient(ellipse at 99% 1%, var(--color-primary-bg) 0%, transparent 49%),
    radial-gradient(ellipse at 1% 99%, var(--color-rail) 0%, transparent 48%),
    linear-gradient(123deg, var(--color-page), var(--color-card) 46%, var(--color-bg));
  background-size: 24px 24px, cover, cover, cover;
  background-position: 12px 12px, center, center, center;
  background-repeat: repeat, no-repeat, no-repeat, no-repeat;
}
.atlas-canvas::before {
  content: '';
  position: absolute;
  inset: 0;
  background: url("~@/assets/images/knowledge-map-contours.png") center / 100% 100% no-repeat;
  opacity: .25;
  pointer-events: none;
}
.atlas-surface { overflow: hidden; touch-action: none; cursor: grab; outline-offset: -3px; }
.atlas-surface:active { cursor: grabbing; }
.atlas-world { position: absolute; top: 0; left: 0; transform-origin: 0 0; }
.atlas-lines { overflow: visible; position: absolute; pointer-events: none; }
.atlas-wire > .wire-line { stroke: var(--color-text); stroke-width: 2.2; }
.atlas-wire.active > .wire-line { stroke: var(--color-primary); stroke-width: 2.7; }
.atlas-wire rect { fill: var(--color-card); stroke: var(--color-border); stroke-width: 0.8; }
.atlas-wire text { font-size: 11px; fill: var(--color-text-secondary); }
.atlas-wire.active text { fill: var(--color-primary-hover); }
.atlas-wire.muted { opacity: 0.62; }
/* 透明的描边点击区域：命中线时由画布交给选段处理，不触发拖拽 */
.atlas-canvas .atlas-wire > path.wire-hit { stroke: transparent; stroke-width: 20; pointer-events: stroke; cursor: pointer; }
.atlas-canvas .atlas-wire .wire-label { pointer-events: auto; cursor: pointer; }
.atlas-node { position: absolute; transform: translate(-50%, -50%); box-sizing: border-box; width: 174px; height: 66px;
  border: 1px solid var(--color-border-strong); background: var(--color-card); color: var(--color-text); border-radius: 12px; display: flex; gap: 10px;
  align-items: center; padding: 0 12px; text-align: left; cursor: grab; font: inherit; user-select: none;
  box-shadow: 0 2px 4px rgba(var(--color-primary-rgb), .05), 0 5px 13px rgba(var(--color-primary-rgb), .03); }
.atlas-node:active { cursor: grabbing; }
.atlas-node:focus-visible, .atlas-zoom button:focus-visible { outline: 3px solid var(--color-primary); outline-offset: 3px; }
.atlas-node:hover { border-color: var(--ink); }
.node-symbol { flex: none; width: 32px; height: 34px; border-radius: 8px; display: grid; place-items: center;
  color: var(--ink); background: var(--wash); font-size: 18px; }
.node-word { min-width: 0; }
.node-word strong { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.node-word strong { font-size: 14px; font-weight: 650; line-height: 1.5; }
.atlas-node.selected { background: var(--color-primary); border-color: var(--color-primary-hover); color: #fff;
  box-shadow: 0 0 0 4px rgba(var(--color-primary-rgb), .13), 0 5px 12px rgba(var(--color-primary-rgb), .15); z-index: 1; }
.selected .node-symbol { color: white; background: #ffffff22; }
.atlas-node.peripheral { background: var(--color-bg); }
/* 影响路径：卡片多一行类型、多一行编码，两端描边表示选中的装配段 */
.atlas-node.detail { flex-direction: column; align-items: flex-start; justify-content: center; gap: 1px; padding: 0 15px;
  border-radius: 11px; border-color: var(--color-border); }
.atlas-node.detail .node-symbol { display: none; }
.atlas-node.detail .node-category { display: block; color: var(--color-text-secondary); font-size: 11px; line-height: 1.6; }
.atlas-node.detail .node-word strong { font-size: 14.5px; font-weight: 650; line-height: 1.5; }
.atlas-node.detail .node-word code { display: block; color: var(--color-text-secondary); font: 11px Consolas, "Microsoft YaHei", monospace; }
.atlas-node.detail.endpoint { border-color: var(--color-primary); box-shadow: 0 0 0 2px rgba(var(--color-primary-rgb), .2), 0 5px 13px rgba(var(--color-primary-rgb), .03); }
.atlas-canvas.impact .atlas-wire > path.wire-line { stroke-width: 2.4; }
.atlas-canvas.impact .atlas-wire.active > path.wire-line { stroke-width: 3; }
.atlas-canvas.impact .atlas-wire.active rect { fill: var(--color-primary); stroke: var(--color-primary-hover); }
.atlas-canvas.impact .atlas-wire.active text { fill: #fff; font-weight: 600; }
.atlas-zoom { position: absolute; bottom: 16px; left: 16px; display: inline-flex; align-items: center; padding: 3px;
  border: 1px solid var(--color-border); background: var(--color-card); border-radius: 9px; box-shadow: 0 2px 6px rgba(var(--color-primary-rgb), .06); }
.atlas-zoom button { display: grid; place-items: center; width: 30px; height: 29px; padding: 0; border: 0; border-radius: 5px;
  background: transparent; color: var(--color-text-secondary); font-size: 14px; cursor: pointer; }
.atlas-zoom button:disabled { opacity: .38; cursor: default; }
.atlas-zoom button:hover:enabled { background: var(--color-primary-bg); color: var(--color-primary-hover); }
.atlas-zoom output { min-width: 42px; text-align: center; font: 12px Consolas,monospace; color: var(--color-text-secondary); }
.atlas-zoom .fit { border-left: 1px solid var(--color-border); border-radius: 0 5px 5px 0; margin-left: 2px; width: 34px; }
</style>

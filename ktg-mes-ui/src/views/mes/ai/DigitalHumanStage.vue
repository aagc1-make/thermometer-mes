<template>
  <div id="assistant-avatar-stage" class="avatar-stage"
    :style="{ backgroundImage: 'url(' + assets + 'night.jpg)' }">
    <div v-if="failed" class="avatar-stage__fallback">
      <img v-if="!offlinePortrait" :src="assets + 'mao.png'" alt="数字人米粒" @error="offlinePortrait = true">
      <i v-else class="el-icon-user" aria-hidden="true" />
    </div>
    <div ref="canvas" class="avatar-stage__canvas" :class="{ 'is-interactive': ready }"
      :role="ready ? 'button' : null" :tabindex="ready ? 0 : null"
      :aria-label="ready ? '和米粒互动' : null" :title="ready ? '点击米粒的头部或身体' : null"
      @click="tapModel" @pointermove="followPointer" @pointerleave="resetFocus" @pointercancel="resetFocus"
      @keydown.enter.prevent="greet" @keydown.space.prevent="greet"
      @webglcontextlost.capture.prevent="handleContextLost" />
  </div>
</template>

<script>
import { AVATAR_ASSETS, loadAvatarRuntime } from './avatarRuntime'

export default {
  name: 'DigitalHumanStage',
  props: {
    mouthLevel: { type: Function, required: true }
  },
  data() {
    return { assets: AVATAR_ASSETS, ready: false, failed: false, offlinePortrait: false }
  },
  mounted() {
    this.mountModel()
  },
  beforeDestroy() {
    this._disposed = true
    this.releaseModel()
  },
  methods: {
    async mountModel() {
      this.$emit('state', 'loading')
      this.failed = false
      let loadingTimer
      try {
        const PIXI = await loadAvatarRuntime()
        if (this._disposed) return
        const app = new PIXI.Application({
          backgroundAlpha: 0,
          autoDensity: true,
          resolution: Math.min(window.devicePixelRatio || 1, 2)
        })
        this._app = app
        const modelPromise = PIXI.live2d.Live2DModel.from(this.assets + 'mao/mao_pro.model3.json', {
          autoInteract: false
        })
        modelPromise.then(model => {
          if (this._app !== app) model.destroy()
        }).catch(() => {})
        const model = await Promise.race([
          modelPromise,
          new Promise((resolve, reject) => {
            loadingTimer = window.setTimeout(() => reject(new Error('Mao model load timed out')), 15000)
          })
        ])
        if (this._app !== app) return

        const internal = model.internalModel
        // Mao's model lists both drawable IDs with empty names, so the default name map merges them.
        for (const area of internal.getHitAreaDefs()) {
          if (area.index >= 0) internal.hitAreas[area.id] = area
        }
        internal.on('beforeModelUpdate', () => {
          internal.coreModel.setParameterValueById('ParamA', this.mouthLevel())
        })
        model.anchor.set(0.5)
        this._nativeWidth = model.width
        this._nativeHeight = model.height
        app.stage.addChild(model)
        this._model = model
        this.$refs.canvas.appendChild(app.view)
        this._resizeObserver = new ResizeObserver(() => this.resizeModel())
        this._resizeObserver.observe(this.$el)
        this.resizeModel()
        this.ready = true
        this.$emit('state', 'ready')
      } catch (error) {
        if (!this._disposed) this.showFallback()
      } finally {
        window.clearTimeout(loadingTimer)
      }
    },
    getPointerPosition(event) {
      const bounds = this.$refs.canvas.getBoundingClientRect()
      return [event.clientX - bounds.left, event.clientY - bounds.top]
    },
    tapModel(event) {
      if (!this._model) return
      const areas = this._model.hitTest(...this.getPointerPosition(event))
      if (areas.includes('HitAreaHead')) return this.greet()
      if (areas.includes('HitAreaBody')) return this._model.motion('', 2, 2).catch(() => {})
    },
    greet() {
      if (this._model) return this._model.motion('', 1, 2, { expression: 'exp_04' }).catch(() => {})
    },
    followPointer(event) {
      if (this._model) this._model.focus(...this.getPointerPosition(event))
    },
    resetFocus() {
      if (this._model) this._model.internalModel.focusController.focus(0, 0)
    },
    resizeModel() {
      const width = this.$el.clientWidth
      const height = this.$el.clientHeight
      if (!this._model || !width || !height) return
      this._app.renderer.resize(width, height)
      this._model.scale.set(Math.min(width * 0.95 / this._nativeWidth, height * 0.96 / this._nativeHeight))
      this._model.position.set(width / 2, height / 2)
    },
    handleContextLost() {
      if (!this._disposed) this.showFallback()
    },
    showFallback() {
      this.releaseModel()
      this.ready = false
      this.failed = true
      this.$emit('state', 'error')
    },
    releaseModel() {
      if (this._resizeObserver) this._resizeObserver.disconnect()
      if (this._app) this._app.destroy(true, { children: true })
      this._app = null
      this._model = null
    }
  }
}
</script>

<style lang="scss" scoped>
.avatar-stage {
  position: fixed;
  inset: var(--mes-header-height) auto 0 0;
  z-index: 9;
  width: calc(var(--mes-nav-width) + var(--assistant-avatar-slot));
  display: grid;
  background-color: #12263e;
  background-position: center;
  background-size: cover;
  overflow: hidden;
  pointer-events: none;
  animation: avatar-appear 200ms ease-out;
}
.avatar-stage__canvas, .avatar-stage__fallback { grid-area: 1 / 1; min-height: 0; min-width: 0; }
.avatar-stage__canvas { overflow: hidden; }
.avatar-stage__canvas.is-interactive { pointer-events: auto; cursor: pointer; }
.avatar-stage__canvas:focus-visible { outline: 2px solid var(--color-primary); outline-offset: -4px; }
.avatar-stage__fallback { display: grid; place-items: center; color: #d9e6f1; font-size: 34px; }
.avatar-stage__fallback img { width: 64%; max-width: 230px; border-radius: 18px; box-shadow: 0 16px 38px rgba(0, 0, 0, .28); }
@keyframes avatar-appear { from { opacity: 0; } to { opacity: 1; } }
@media (prefers-reduced-motion: reduce) { .avatar-stage { animation: none; } }
</style>

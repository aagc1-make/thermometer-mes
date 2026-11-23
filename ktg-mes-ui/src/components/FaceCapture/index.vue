<template>
  <el-dialog
    :title="title"
    :visible.sync="dialogVisible"
    width="440px"
    append-to-body
    :close-on-click-modal="false"
    custom-class="face-dialog"
    @closed="stop"
  >
    <div class="face-box" :class="'is-' + status">
      <video ref="video" class="face-video" autoplay muted playsinline></video>
      <div class="face-guide"></div>
      <span class="face-chip">{{ statusText }}</span>
      <div v-if="status === 'loading'" class="face-loading">
        <i class="el-icon-loading"></i>
        <span>正在加载人脸模型…</span>
      </div>
      <div v-if="status === 'done'" class="face-done">
        <i class="el-icon-success"></i>
      </div>
    </div>

    <div class="face-progress"><span :style="{ width: progress + '%' }"></span></div>
    <p class="face-hint">{{ hint }}</p>
    <p v-if="tip" class="face-tip">{{ tip }}</p>
    <p v-if="error" class="face-error">{{ error }}</p>

    <div class="face-actions">
      <el-button size="small" @click="close">取消</el-button>
      <el-button v-if="retryable" type="primary" size="small" @click="restart">重新识别</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { loadFaceEngine, detectOnce } from '@/utils/faceEngine'
import {
  averageDescriptors,
  euclideanDistance,
  isValidDescriptor,
  landmarkEar,
  noseRatio,
  createLivenessTracker
} from '@/utils/faceMath'

const CAPTURE_SAMPLES = 3
const TOTAL_TIMEOUT = 30000
const SAME_FACE_LIMIT = 0.45

export default {
  name: 'FaceCapture',
  props: {
    visible: { type: Boolean, default: false },
    title: { type: String, default: '人脸识别' },
    tip: { type: String, default: '' }
  },
  data() {
    return {
      status: 'idle',
      hint: '正在准备摄像头…',
      error: '',
      progress: 0,
      retryable: false,
      engine: null,
      stream: null,
      running: false,
      busy: false,
      timer: null,
      startedAt: 0,
      lostFrames: 0,
      samples: [],
      liveness: null
    }
  },
  computed: {
    dialogVisible: {
      get() { return this.visible },
      set(value) { this.$emit('update:visible', value) }
    },
    statusText() {
      const map = {
        idle: '准备中',
        loading: '加载模型',
        live: '活体检测',
        collecting: '采集特征',
        done: '识别成功',
        error: '识别失败'
      }
      return map[this.status] || ''
    }
  },
  watch: {
    visible(value) {
      if (value) this.start()
      else this.stop()
    }
  },
  beforeDestroy() {
    this.stop()
  },
  methods: {
    async start() {
      this.reset()
      this.status = 'loading'
      this.hint = '正在打开摄像头…'
      try {
        if (!window.isSecureContext) {
          throw new Error('摄像头只能在 HTTPS 或 localhost 下使用，请换用安全地址访问')
        }
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
          throw new Error('当前浏览器不支持摄像头采集，请使用 Chrome 或 Edge')
        }
        const stream = await navigator.mediaDevices.getUserMedia({
          video: {
            facingMode: 'user',
            width: { ideal: 640 },
            height: { ideal: 480 },
            frameRate: { ideal: 20 }
          },
          audio: false
        })
        this.stream = stream
        const video = await this.waitForVideo()
        if (!video) throw new Error('采集窗口未就绪，请重试')
        video.srcObject = stream
        await video.play().catch(() => {})

        this.hint = '正在加载人脸模型…'
        this.engine = await loadFaceEngine()
        this.liveness = createLivenessTracker()
        this.status = 'live'
        this.hint = '请正对屏幕，保持脸部在取景框内，然后眨一下眼睛或轻轻左右转头'
        this.running = true
        this.startedAt = Date.now()
        this.tick()
      } catch (error) {
        this.fail(this.friendly(error))
      }
    },
    async waitForVideo() {
      for (let i = 0; i < 40; i++) {
        if (this.$refs.video) return this.$refs.video
        await new Promise(resolve => setTimeout(resolve, 50))
      }
      return null
    },
    tick() {
      if (!this.running) return
      this.timer = setTimeout(async () => {
        if (!this.running) return
        await this.frame()
        this.tick()
      }, 130)
    },
    async frame() {
      if (this.busy || !this.running) return
      if (Date.now() - this.startedAt > TOTAL_TIMEOUT) {
        this.fail('识别超时，请重新开始')
        return
      }
      const video = this.$refs.video
      if (!video || video.readyState < 2) return
      this.busy = true
      try {
        if (this.status === 'collecting') await this.collectFrame(video)
        else await this.liveFrame(video)
      } catch (error) {
        console.warn('[人脸采集] 单帧检测失败', error)
      } finally {
        this.busy = false
      }
    },
    async liveFrame(video) {
      const result = await detectOnce(this.engine, video, false)
      if (!result || !result.detection || !result.landmarks) {
        this.lostFrames += 1
        this.progress = 0
        if (this.lostFrames >= 8) {
          this.status = 'live'
          this.hint = '没有检测到人脸，请靠近一点、正对镜头并保证光线充足'
        }
        return
      }
      this.lostFrames = 0
      this.status = 'live'
      const ear = landmarkEar(result.landmarks.positions)
      const nose = noseRatio(result.detection.box, result.landmarks.positions)
      const live = this.liveness.push({ ear, nose })
      this.progress = Math.round(live.progress * 55)
      if (live.passed) {
        this.status = 'collecting'
        this.samples = []
        this.hint = '活体通过，正在采集人脸特征…'
      } else {
        this.hint = '请眨一下眼睛，或轻轻左右转头（不要只动手机）'
      }
    },
    async collectFrame(video) {
      const result = await detectOnce(this.engine, video, true)
      if (!result || !result.detection || !result.descriptor) return
      const descriptor = Array.from(result.descriptor)
      if (!isValidDescriptor(descriptor)) return
      if (this.samples.length && euclideanDistance(this.samples[0].descriptor, descriptor) > SAME_FACE_LIMIT) {
        this.samples = []
        this.progress = 55
        this.hint = '画面中人脸变化较大，请保持同一人后重试'
        return
      }
      this.samples.push({ descriptor, score: result.detection.score || 0 })
      this.progress = 55 + Math.round((this.samples.length / CAPTURE_SAMPLES) * 45)
      this.hint = `正在采集特征 ${Math.min(this.samples.length, CAPTURE_SAMPLES)}/${CAPTURE_SAMPLES}，请保持不动`
      if (this.samples.length >= CAPTURE_SAMPLES) {
        this.finish()
      }
    },
    finish() {
      const average = averageDescriptors(this.samples.map(item => item.descriptor))
      if (!isValidDescriptor(average)) {
        this.fail('采集到的特征无效，请调整光线后重试')
        return
      }
      const quality = this.samples.reduce((sum, item) => sum + item.score, 0) / this.samples.length
      this.status = 'done'
      this.progress = 100
      this.hint = '识别成功'
      this.running = false
      this.stopCamera()
      this.$emit('captured', {
        descriptor: average,
        samples: this.samples.length,
        quality: Math.round(Math.max(0, Math.min(1, quality)) * 10000) / 10000
      })
    },
    fail(message) {
      this.status = 'error'
      this.error = message
      this.hint = ''
      this.progress = 0
      this.retryable = true
      this.running = false
      this.stopCamera()
    },
    friendly(error) {
      const name = error && error.name
      if (name === 'NotAllowedError' || name === 'PermissionDeniedError') {
        return '摄像头权限被拒绝。请在浏览器地址栏的权限设置中允许摄像头后重试'
      }
      if (name === 'NotFoundError' || name === 'DevicesNotFoundError') {
        return '没有找到可用的摄像头，请连接摄像头后重试'
      }
      if (name === 'NotReadableError' || name === 'TrackStartError') {
        return '摄像头被其他程序占用，请关闭占用程序后重试'
      }
      if (name === 'SecurityError') {
        return '当前页面不是安全环境，请通过 HTTPS 或 localhost 访问'
      }
      return (error && error.message) || '摄像头启动失败，请重试'
    },
    restart() {
      this.stop()
      this.start()
    },
    close() {
      this.dialogVisible = false
    },
    stopCamera() {
      if (this.stream) {
        this.stream.getTracks().forEach(track => {
          try { track.stop() } catch (error) { /* 已停止 */ }
        })
        this.stream = null
      }
      const video = this.$refs.video
      if (video) video.srcObject = null
    },
    stop() {
      this.running = false
      if (this.timer) {
        clearTimeout(this.timer)
        this.timer = null
      }
      this.stopCamera()
    },
    reset() {
      this.stop()
      this.status = 'idle'
      this.hint = '正在准备摄像头…'
      this.error = ''
      this.progress = 0
      this.retryable = false
      this.lostFrames = 0
      this.samples = []
      this.busy = false
    }
  }
}
</script>

<style scoped>
.face-box {
  position: relative;
  width: 100%;
  height: 300px;
  border-radius: 14px;
  overflow: hidden;
  background: #0f172a;
  display: flex;
  align-items: center;
  justify-content: center;
}
.face-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scaleX(-1);
  background: #0f172a;
}
.face-guide {
  position: absolute;
  width: 168px;
  height: 216px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border: 2px dashed rgba(255, 255, 255, 0.55);
  border-radius: 50%;
  pointer-events: none;
}
.face-box.is-done .face-guide {
  border-color: #22c55e;
}
.face-chip {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: #fff;
  background: rgba(15, 23, 42, 0.6);
}
.face-loading,
.face-done {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #e2e8f0;
  font-size: 13px;
  background: rgba(15, 23, 42, 0.55);
}
.face-done {
  font-size: 46px;
  color: #22c55e;
  background: rgba(15, 23, 42, 0.25);
}
.face-progress {
  margin-top: 14px;
  height: 6px;
  border-radius: 999px;
  background: #e2e8f0;
  overflow: hidden;
}
.face-progress span {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #2563eb, #22c55e);
  transition: width 0.2s ease;
}
.face-hint {
  margin: 10px 0 0;
  font-size: 13px;
  color: #475569;
  text-align: center;
  min-height: 20px;
}
.face-tip {
  margin: 6px 0 0;
  font-size: 12px;
  color: #94a3b8;
  text-align: center;
}
.face-error {
  margin: 8px 0 0;
  font-size: 13px;
  color: #dc2626;
  text-align: center;
  line-height: 1.5;
}
.face-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 14px;
}
</style>

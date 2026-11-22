<template>
  <el-dialog
    :title="title"
    :visible.sync="dialogVisible"
    width="min(440px, calc(100vw - 28px))"
    append-to-body
    :close-on-click-modal="false"
    custom-class="face-dialog"
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

    <div class="face-progress" role="progressbar" aria-label="人脸采集进度"
      aria-valuemin="0" aria-valuemax="100" :aria-valuenow="progress">
      <span :style="{ width: progress + '%' }"></span>
    </div>
    <p class="face-hint" role="status">{{ hint }}</p>
    <p v-if="tip" class="face-tip">{{ tip }}</p>
    <p v-if="error" class="face-error">{{ error }}</p>

    <div class="face-actions">
      <el-button size="small" @click="close">取消</el-button>
      <el-button v-if="retryable" type="primary" size="small" @click="restart">重新识别</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { loadFaceEngine, detectFaces, describeFace } from '@/utils/faceEngine'
import {
  averageDescriptors,
  euclideanDistance,
  isValidDescriptor,
  noseRatio,
  createActiveLivenessTracker
} from '@/utils/faceMath'

const CAPTURE_SAMPLES = 3
const TOTAL_TIMEOUT = 45000
const SAMPLE_TIMEOUT = 10000
const SAME_FACE_LIMIT = 0.45
/** 动作顺序固定：向左转头、向右转头，每步回正即可 */
const ACTION_ORDER = ['turn-left', 'turn-right']
const ACTION_HINTS = {
  'turn-left': { action: '请把脸缓缓转向左侧', return: '请回正面向镜头' },
  'turn-right': { action: '再请把脸转向右侧', return: '请回正面向镜头' }
}

function releaseStream(stream) {
  if (stream) stream.getTracks().forEach(track => track.stop())
}

function overlaps(previous, current) {
  const width = Math.max(0, Math.min(previous.x + previous.width, current.x + current.width) - Math.max(previous.x, current.x))
  const height = Math.max(0, Math.min(previous.y + previous.height, current.y + current.height) - Math.max(previous.y, current.y))
  return width * height >= 0.3 * Math.max(previous.width * previous.height, current.width * current.height)
}

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
      captureRun: 0,
      timer: null,
      startedAt: 0,
      passedAt: 0,
      frameSurface: null,
      previousBox: null,
      anchor: null,
      actionStep: 0,
      misses: 0,
      boxMisses: 0,
      anchorMisses: 0,
      samples: [],
      liveness: null
    }
  },
  computed: {
    dialogVisible: {
      get() { return this.visible },
      set(value) {
        if (!value) this.stop()
        this.$emit('update:visible', value)
      }
    },
    statusText() {
      const map = {
        idle: '准备中',
        loading: '加载模型',
        live: this.liveness ? `动作 ${this.actionStep + 1}/${ACTION_ORDER.length}` : '活体检测',
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
      const run = this.captureRun
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
        if (!this.isCurrent(run)) {
          releaseStream(stream)
          return
        }
        this.stream = stream
        const video = await this.waitForVideo(run)
        if (!this.isCurrent(run)) return
        if (!video) throw new Error('采集窗口未就绪，请重试')
        video.srcObject = stream
        await video.play()
        if (!this.isCurrent(run)) return

        this.hint = '正在加载人脸模型…'
        const engine = await loadFaceEngine()
        if (!this.isCurrent(run)) return
        this.engine = engine
        this.frameSurface = document.createElement('canvas')
        this.status = 'live'
        this.hint = '请正对镜头，将一张脸放在取景框中央'
        this.running = true
        this.startedAt = performance.now()
        this.tick(run)
      } catch (error) {
        if (this.isCurrent(run)) this.fail(this.friendly(error))
      }
    },
    isCurrent(run) {
      return this.visible && this.captureRun === run
    },
    async waitForVideo(run) {
      for (let i = 0; i < 40 && this.isCurrent(run); i++) {
        if (this.$refs.video) return this.$refs.video
        await new Promise(resolve => setTimeout(resolve, 50))
      }
      return null
    },
    tick(run) {
      if (!this.running || !this.isCurrent(run)) return
      this.timer = setTimeout(async () => {
        if (!this.running || !this.isCurrent(run)) return
        await this.frame(run)
        this.tick(run)
      }, 55)
    },
    async frame(run) {
      const now = performance.now()
      if (now - this.startedAt > TOTAL_TIMEOUT) {
        this.fail('识别超时，请重新开始')
        return
      }
      const video = this.$refs.video
      if (!video || video.readyState < 2 || !video.videoWidth || !video.videoHeight) {
        if (this.anchor) this.resetChallenge('摄像头画面中断，请重新正对镜头')
        return
      }
      try {
        const image = this.frameSurface
        if (image.width !== video.videoWidth || image.height !== video.videoHeight) {
          image.width = video.videoWidth
          image.height = video.videoHeight
        }
        image.getContext('2d', { willReadFrequently: true }).drawImage(video, 0, 0)
        const faces = await detectFaces(this.engine, image)
        if (!this.isCurrent(run)) return
        if (faces.length !== 1) {
          // 眨眼、转头时检测可能瞬断一两帧，连续丢失才判定为异常
          this.misses += 1
          if (this.misses >= 3) {
            this.resetChallenge(faces.length ? '检测到多人，请在画面中只保留一人' : '请回到取景框中央，并保证光线充足')
          } else if (!faces.length) {
            this.hint = '画面有点模糊，请保持不动'
          }
          return
        }
        this.misses = 0
        const face = faces[0]
        const box = face.detection.box
        const minimumSize = Math.min(image.width, image.height) * 0.18
        if (box.width < minimumSize || box.height < minimumSize || box.x < 2 || box.y < 2
          || box.x + box.width > image.width - 2 || box.y + box.height > image.height - 2) {
          this.resetChallenge('请调整距离，让整张脸进入取景框')
          return
        }
        if (this.previousBox && !overlaps(this.previousBox, box)) {
          // 转头会让人脸框快速位移，短暂不重叠不算换人，连续多帧才重置
          this.boxMisses += 1
          if (this.boxMisses >= 4) {
            this.resetChallenge('人脸位置变化过大，请正对镜头重新开始')
          }
          return
        }
        this.boxMisses = 0
        this.previousBox = box
        if (this.status === 'collecting') await this.collectFrame(run, image, face, now)
        else await this.liveFrame(run, image, face, now)
      } catch {
        if (this.isCurrent(run)) this.resetChallenge('暂时无法读取人脸，请调整距离和光线')
      }
    },
    async liveFrame(run, image, face, now) {
      const positions = face.landmarks.positions
      const nose = noseRatio(face.detection.box, positions)
      if (!Number.isFinite(nose)) {
        this.resetChallenge('请让脸部清晰可见，并正对镜头')
        return
      }
      if (!this.liveness) {
        if (Math.abs(nose - 0.5) > 0.12) {
          this.hint = '请正对镜头'
          return
        }
        const anchor = Array.from(await describeFace(this.engine, image, face.landmarks))
        if (!this.isCurrent(run)) return
        if (!isValidDescriptor(anchor)) {
          this.resetChallenge('未能提取清晰的特征，请调整光线')
          return
        }
        const order = ACTION_ORDER.slice()
        this.anchor = anchor
        this.liveness = createActiveLivenessTracker({ order, baselineNose: nose })
        this.progress = 5
        this.hint = ACTION_HINTS[order[0]].action
        return
      }

      const live = this.liveness.push({ nose, at: now })
      if (live.phase === 'expired') {
        this.resetChallenge('动作超时，正对镜头后重新开始')
        return
      }
      if (live.step > this.actionStep) {
        // 刚完成动作时人脸可能还在回正中，允许连续几帧特征波动再做判定
        const descriptor = Array.from(await describeFace(this.engine, image, face.landmarks))
        if (!this.isCurrent(run)) return
        if (this.sameFace(descriptor)) {
          this.anchorMisses = 0
          this.actionStep = live.step
        } else if (++this.anchorMisses >= 3) {
          this.resetChallenge('画面中的人脸发生变化，请由同一人重新完成动作')
          return
        }
      }
      if (live.passed) {
        this.status = 'collecting'
        this.passedAt = performance.now()
        this.progress = 65
        this.hint = '动作已完成，请正对镜头，保持不动'
      } else {
        this.progress = 5 + live.step * 30 + (live.phase === 'return' ? 8 : 0)
        this.hint = ACTION_HINTS[live.action][live.phase]
      }
    },
    async collectFrame(run, image, face, now) {
      if (now - this.passedAt > SAMPLE_TIMEOUT) {
        this.resetChallenge('采集超时，正对镜头后重新开始')
        return
      }
      const descriptor = Array.from(await describeFace(this.engine, image, face.landmarks))
      if (!this.isCurrent(run) || !this.matchesAnchor(descriptor)) return
      this.samples.push({ descriptor, score: face.detection.score || 0 })
      this.progress = 65 + Math.round((this.samples.length / CAPTURE_SAMPLES) * 35)
      this.hint = `正在采集特征 ${Math.min(this.samples.length, CAPTURE_SAMPLES)}/${CAPTURE_SAMPLES}，请保持不动`
      if (this.samples.length >= CAPTURE_SAMPLES) {
        this.finish()
      }
    },
    matchesAnchor(descriptor) {
      if (!this.sameFace(descriptor)) {
        this.resetChallenge('画面中的人脸发生变化，请由同一人重新完成动作')
        return false
      }
      return true
    },
    sameFace(descriptor) {
      return isValidDescriptor(descriptor)
        && euclideanDistance(this.anchor, descriptor) <= SAME_FACE_LIMIT
    },
    resetChallenge(message) {
      this.status = 'live'
      this.hint = message
      this.progress = 0
      this.liveness = null
      this.anchor = null
      this.actionStep = 0
      this.misses = 0
      this.boxMisses = 0
      this.anchorMisses = 0
      this.previousBox = null
      this.samples = []
    },
    finish() {
      const average = averageDescriptors(this.samples.map(item => item.descriptor))
      if (!isValidDescriptor(average)) {
        this.fail('采集到的特征无效，请调整光线后重试')
        return
      }
      const samples = this.samples.length
      const quality = this.samples.reduce((sum, item) => sum + item.score, 0) / samples
      this.status = 'done'
      this.progress = 100
      this.hint = '识别成功'
      this.stop()
      this.$emit('captured', {
        descriptor: average,
        samples,
        quality: Math.round(Math.max(0, Math.min(1, quality)) * 10000) / 10000
      })
    },
    fail(message) {
      this.status = 'error'
      this.error = message
      this.hint = ''
      this.progress = 0
      this.retryable = true
      this.stop()
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
      this.start()
    },
    close() {
      this.dialogVisible = false
    },
    stopCamera() {
      releaseStream(this.stream)
      this.stream = null
      const video = this.$refs.video
      if (video) video.srcObject = null
    },
    stop() {
      this.captureRun += 1
      this.running = false
      if (this.timer) {
        clearTimeout(this.timer)
        this.timer = null
      }
      this.stopCamera()
      this.frameSurface = null
      this.previousBox = null
      this.liveness = null
      this.anchor = null
      this.samples = []
    },
    reset() {
      this.stop()
      this.status = 'idle'
      this.hint = '正在准备摄像头…'
      this.error = ''
      this.progress = 0
      this.retryable = false
      this.actionStep = 0
      this.misses = 0
      this.boxMisses = 0
      this.anchorMisses = 0
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

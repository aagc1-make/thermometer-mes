<template>
  <div class="face-profile">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="人脸登录使用说明"
    >
      <div class="face-profile__note">
        人脸照片只在浏览器内处理。系统加密保存特征，录入或解绑时需验证登录密码。
      </div>
    </el-alert>

    <div class="face-profile__panel">
      <div v-if="status" class="face-profile__state" aria-live="polite">
        <i :class="status.enrolled ? 'el-icon-success face-profile__ok' : 'el-icon-warning-outline face-profile__off'" aria-hidden="true"></i>
        <div>
          <div class="face-profile__headline">
            <p class="face-profile__title">{{ status.enrolled ? '已录入人脸' : '尚未录入人脸' }}</p>
            <el-tag v-if="loginState" size="small" :type="loginState.type">{{ loginState.label }}</el-tag>
          </div>
          <p class="face-profile__desc" v-if="status.enrolled">
            样本 {{ status.samples || 0 }} 帧 · 模板 {{ status.templateCount || 1 }} 个 ·
            更新于 {{ formatTime(status.updateTime) }} · 算法 {{ status.algorithm || 'face-api-0.22.2' }}
          </p>
          <p v-if="status && status.needReenroll" class="face-profile__desc face-profile__warn">
            当前模板由旧版算法录入，识别可能不准，请重新录入一次。
          </p>
          <p v-if="statusHint" class="face-profile__desc">{{ statusHint }}</p>
        </div>
      </div>
      <div v-else aria-live="polite">
        <p class="face-profile__title">{{ statusLoading ? '正在读取人脸状态…' : '人脸状态读取失败' }}</p>
        <p v-if="!statusLoading" class="face-profile__desc">可继续使用密码登录。</p>
      </div>
      <div v-if="status" class="face-profile__buttons">
        <el-button type="primary" size="small" icon="el-icon-camera" :disabled="!canEnroll" @click="openEnroll('enroll')">
          {{ status.enrolled ? '更新人脸' : '录入人脸' }}
        </el-button>
        <el-button v-if="canAppend" size="small" icon="el-icon-plus" :disabled="!canEnroll" @click="openEnroll('append')">
          追加角度
        </el-button>
        <el-button v-if="status.enrolled" type="danger" plain size="small" icon="el-icon-delete"
          :disabled="statusLoading || unbinding || enrolling" @click="openUnbind">
          解绑人脸
        </el-button>
      </div>
      <el-button v-else-if="!statusLoading" size="small" @click="loadStatus">重新加载</el-button>
    </div>

    <el-dialog
      :title="enrollMode === 'append' ? '追加一个角度' : '录入人脸'"
      :visible.sync="enrollVisible"
      width="440px"
      append-to-body
      :close-on-click-modal="false"
    >
      <div class="face-profile__form">
        <p class="face-profile__hint">
          请确认：人脸特征仅用于本系统登录比对，不保存照片，可随时解绑。需要再次输入登录密码以确认是本人操作。
        </p>
        <p v-if="enrollMode === 'append'" class="face-profile__hint">
          追加角度用于改善侧脸、戴眼镜等场景的识别效果，主模板保持不变，最多保存 3 个。
        </p>
        <el-input
          v-model="password"
          type="password"
          show-password
          placeholder="请输入当前登录密码"
          autocomplete="current-password"
        />
        <el-checkbox v-model="agree" class="face-profile__agree">
          我已阅读并同意上述说明，自愿录入人脸特征
        </el-checkbox>
      </div>
      <span slot="footer">
        <el-button size="small" @click="enrollVisible = false">取消</el-button>
        <el-button
          type="primary"
          size="small"
          :disabled="!agree || !password || !canEnroll"
          :loading="enrolling"
          @click="startCapture"
        >
          开始采集
        </el-button>
      </span>
    </el-dialog>

    <el-dialog
      title="解绑人脸"
      :visible.sync="unbindVisible"
      width="420px"
      append-to-body
      :close-on-click-modal="false"
    >
      <div class="face-profile__form">
        <p class="face-profile__hint">解绑后将立即删除人脸特征，之后只能使用密码登录；如需恢复，请重新录入。</p>
        <el-input
          v-model="unbindPassword"
          type="password"
          show-password
          placeholder="请输入当前登录密码"
          autocomplete="current-password"
        />
      </div>
      <span slot="footer">
        <el-button size="small" @click="unbindVisible = false">取消</el-button>
        <el-button type="danger" size="small" :disabled="!unbindPassword" :loading="unbinding" @click="handleUnbind">
          确认解绑
        </el-button>
      </span>
    </el-dialog>

    <face-capture
      :visible.sync="captureVisible"
      title="录入人脸"
      tip="采集只在本机浏览器完成，上传的只有特征向量"
      @captured="handleCaptured"
    />
  </div>
</template>

<script>
import { getFaceStatus, enrollFace, appendFace, unbindFace } from '@/api/face'
import FaceCapture from '@/components/FaceCapture'

export default {
  name: 'ProfileFaceLogin',
  components: { FaceCapture },
  props: {
    active: { type: Boolean, default: true }
  },
  data() {
    return {
      status: null,
      statusLoading: false,
      statusRequest: 0,
      enrollVisible: false,
      unbindVisible: false,
      captureVisible: false,
      enrollMode: 'enroll',
      password: '',
      unbindPassword: '',
      agree: false,
      enrolling: false,
      unbinding: false
    }
  },
  computed: {
    canEnroll() {
      return Boolean(this.status && this.status.enrollmentEnabled && !this.statusLoading && !this.enrolling && !this.unbinding)
    },
    canAppend() {
      return Boolean(this.status && this.status.enrolled && (this.status.templateCount || 1) < 4)
    },
    loginState() {
      if (!this.status) return null
      if (!this.status.enrollmentEnabled) return { label: '人脸登录暂不可用', type: 'info' }
      if (!this.status.loginAllowed) return { label: '人脸登录已停用', type: 'warning' }
      return this.status.enrolled ? { label: '可使用人脸登录', type: 'success' } : null
    },
    statusHint() {
      if (!this.status) return ''
      if (!this.status.enrollmentEnabled || !this.status.loginAllowed) return '可继续使用密码登录。'
      return this.status.enrolled ? '' : '录入后可在登录页使用人脸登录。'
    }
  },
  watch: {
    active: {
      immediate: true,
      handler(active) {
        if (active) this.loadStatus()
      }
    }
  },
  methods: {
    async loadStatus() {
      const request = ++this.statusRequest
      this.statusLoading = true
      try {
        const response = await getFaceStatus()
        if (request === this.statusRequest) this.status = response.data || null
      } catch {
        if (request === this.statusRequest) this.status = null
      } finally {
        if (request === this.statusRequest) this.statusLoading = false
      }
    },
    openEnroll(mode) {
      if (!this.canEnroll) return
      this.enrollMode = mode === 'append' ? 'append' : 'enroll'
      this.password = ''
      this.agree = false
      this.enrollVisible = true
    },
    openUnbind() {
      this.unbindPassword = ''
      this.unbindVisible = true
    },
    startCapture() {
      if (!this.agree || !this.password || !this.canEnroll) return
      this.captureVisible = true
    },
    async handleCaptured(payload) {
      this.captureVisible = false
      if (!this.canEnroll) return
      this.enrolling = true
      const appending = this.enrollMode === 'append'
      const request = appending ? appendFace : enrollFace
      try {
        await request({
          password: this.password,
          descriptor: Array.from(payload.descriptor),
          samples: payload.samples,
          quality: payload.quality,
          agree: true
        })
        this.$message.success(appending ? '已追加一个角度' : '人脸已录入')
        this.enrollVisible = false
        this.password = ''
        this.agree = false
        this.enrollMode = 'enroll'
        this.loadStatus()
      } catch (error) {
        // 请求拦截器已提示服务端返回的原因，保留弹窗便于重试
      } finally {
        this.enrolling = false
      }
    },
    async handleUnbind() {
      this.unbinding = true
      try {
        await unbindFace({ password: this.unbindPassword })
        this.$message.success('人脸已解绑')
        this.unbindVisible = false
        this.unbindPassword = ''
        this.loadStatus()
      } catch (error) {
        // 同上
      } finally {
        this.unbinding = false
      }
    },
    formatTime(value) {
      if (!value) return '—'
      return String(value).replace('T', ' ').slice(0, 19)
    }
  }
}
</script>

<style scoped>
.face-profile__note {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.7;
  color: #64748b;
}
.face-profile__panel {
  margin-top: 18px;
  padding: 18px 20px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}
.face-profile__state {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.face-profile__headline {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.face-profile__ok {
  color: #16a34a;
  font-size: 26px;
  line-height: 1.2;
}
.face-profile__off {
  color: #f59e0b;
  font-size: 26px;
  line-height: 1.2;
}
.face-profile__title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}
.face-profile__desc {
  margin: 4px 0 0;
  font-size: 12px;
  color: #64748b;
}
.face-profile__warn {
  color: #b45309;
}
.face-profile__buttons {
  display: flex;
  gap: 8px;
}
.face-profile__form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.face-profile__hint {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: #475569;
}
.face-profile__agree {
  font-size: 13px;
  color: #334155;
}
</style>

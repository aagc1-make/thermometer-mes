<template>
  <div class="face-profile">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="人脸登录使用说明"
    >
      <div class="face-profile__note">
        录入后可在登录页选择「人脸登录」，账号 + 人脸 1:1 比对通过后发放同样的登录令牌。<br>
        服务端只保存 128 维特征向量，不保存照片；录入与解绑都需要再次输入登录密码，可随时解绑。
      </div>
    </el-alert>

    <div class="face-profile__panel">
      <div class="face-profile__state">
        <i :class="status.enrolled ? 'el-icon-success face-profile__ok' : 'el-icon-warning-outline face-profile__off'"></i>
        <div>
          <p class="face-profile__title">{{ status.enrolled ? '已录入人脸' : '尚未录入人脸' }}</p>
          <p class="face-profile__desc" v-if="status.enrolled">
            样本 {{ status.samples || 0 }} 帧 · 更新于 {{ formatTime(status.updateTime) }} · 算法 {{ status.algorithm || 'face-api-0.22.2' }}
          </p>
          <p class="face-profile__desc" v-else>录入后即可在登录页使用人脸登录；未录入时只能使用密码登录。</p>
        </div>
      </div>
      <div class="face-profile__buttons">
        <el-button type="primary" size="small" icon="el-icon-camera" @click="openEnroll">
          {{ status.enrolled ? '更新人脸' : '录入人脸' }}
        </el-button>
        <el-button v-if="status.enrolled" type="danger" plain size="small" icon="el-icon-delete" @click="openUnbind">
          解绑人脸
        </el-button>
      </div>
    </div>

    <el-dialog
      title="录入人脸"
      :visible.sync="enrollVisible"
      width="440px"
      append-to-body
      :close-on-click-modal="false"
    >
      <div class="face-profile__form">
        <p class="face-profile__hint">
          请确认：人脸特征仅用于本系统登录比对，不保存照片，可随时解绑。需要再次输入登录密码以确认是本人操作。
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
          :disabled="!agree || !password"
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
import { getFaceStatus, enrollFace, unbindFace } from '@/api/face'
import FaceCapture from '@/components/FaceCapture'

export default {
  name: 'ProfileFaceLogin',
  components: { FaceCapture },
  data() {
    return {
      status: { enrolled: false, samples: 0, updateTime: null, algorithm: '' },
      enrollVisible: false,
      unbindVisible: false,
      captureVisible: false,
      password: '',
      unbindPassword: '',
      agree: false,
      enrolling: false,
      unbinding: false
    }
  },
  created() {
    this.loadStatus()
  },
  methods: {
    loadStatus() {
      getFaceStatus().then(res => {
        this.status = (res && res.data) || { enrolled: false }
      }).catch(() => {})
    },
    openEnroll() {
      this.password = ''
      this.agree = false
      this.enrollVisible = true
    },
    openUnbind() {
      this.unbindPassword = ''
      this.unbindVisible = true
    },
    startCapture() {
      if (!this.agree || !this.password) return
      this.captureVisible = true
    },
    async handleCaptured(payload) {
      this.captureVisible = false
      this.enrolling = true
      try {
        await enrollFace({
          password: this.password,
          descriptor: Array.from(payload.descriptor),
          samples: payload.samples,
          quality: payload.quality,
          agree: true
        })
        this.$message.success('人脸已录入，可在登录页使用人脸登录')
        this.enrollVisible = false
        this.password = ''
        this.agree = false
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

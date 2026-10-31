<template>
  <div class="login-layout">
    <div class="background-orb orb-left" />
    <div class="background-orb orb-right" />
    <div class="grid-overlay" />

    <div class="login-card">
      <aside class="brand-panel">
        <div class="brand-grid">
          <div class="brand-visual brand-visual-primary brand-image-card brand-image-card-1" />

          <div class="brand-stat brand-stat-accent">
            <span class="brand-stat-kicker">持续值守</span>
            <span class="brand-stat-value">24h</span>
            <span class="brand-stat-label">风险巡检持续在线</span>
            <span class="brand-stat-note">多源监测链路保持实时同步</span>
          </div>

          <div class="brand-visual brand-visual-secondary">
            <div class="brand-mark">
              <div class="brand-icon">
                <img src="/Logo.png" :alt="`${business.brand} Logo`" class="brand-icon-image">
              </div>
              <div class="brand-name-group">
                <div class="brand-name">{{ business.brand }}</div>
                <div class="brand-sub">{{ isDrilling ? '井场监测与工业资产控制台' : '反诈智能检测助手' }}</div>
              </div>
            </div>
            <div class="brand-rule" />
            <div class="brand-points">
              <div class="brand-point">
                <span class="brand-point-title">实时监测</span>
                <span class="brand-point-text">{{ isDrilling ? '聚合井场风险、传感器与告警状态' : '聚合核心风险事件与账号动态' }}</span>
              </div>
              <div class="brand-point">
                <span class="brand-point-title">分析研判</span>
                <span class="brand-point-text">让高风险线索快速进入判断流程</span>
              </div>
              <div class="brand-point">
                <span class="brand-point-title">处置协同</span>
                <span class="brand-point-text">统一记录流转状态与处置结果</span>
              </div>
            </div>
          </div>

          <div class="brand-stat brand-stat-muted brand-image-card brand-image-card-4" />
        </div>
      </aside>

      <main class="form-panel">
        <div class="form-shell">
          <div class="form-topline">
            <span class="form-topline-text">管理后台登录</span>
          </div>

          <div class="form-container">
            <div class="form-header">
              <div class="form-badge">欢迎回来</div>
              <h2 class="form-title">登录{{ business.brand }}后台</h2>
              <p class="form-subtitle">请输入管理员账户信息，继续访问监测控制台。</p>
            </div>

            <el-form
              ref="formRef"
              :model="form"
              :rules="rules"
              label-width="0"
              @submit.prevent="handleLogin"
            >
              <el-form-item prop="username">
                <label class="field-label">用户名</label>
                <el-input
                  v-model="form.username"
                  placeholder="请输入用户名"
                  size="large"
                  :prefix-icon="User"
                />
              </el-form-item>

              <el-form-item prop="password">
                <label class="field-label">密码</label>
                <el-input
                  v-model="form.password"
                  type="password"
                  placeholder="请输入密码"
                  size="large"
                  :prefix-icon="Lock"
                  show-password
                  @keyup.enter="handleLogin"
                />
              </el-form-item>

              <div class="form-actions">
                <span class="form-hint">仅限授权管理员使用</span>
              </div>

              <el-form-item>
                <el-button
                  type="primary"
                  size="large"
                  :loading="loading"
                  class="login-btn"
                  @click="handleLogin"
                >
                  登录
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { login } from '@/api/auth'
import { business, isDrilling } from '@/config/business'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login({ username: form.username, password: form.password })
    const loginData = res.data.data
    authStore.setToken(loginData.token)
    authStore.setUserInfo(loginData.user)
    ElMessage.success('登录成功')

    const redirect = (router.currentRoute.value.query.redirect as string) || '/dashboard'
    await router.push(redirect)
  } catch (error: any) {
    const isNetworkError = !error?.response
    const message = isNetworkError
      ? '无法连接管理后台服务，请检查 8011 服务与跨域配置'
      : error?.response?.data?.detail || error?.response?.data?.message || '用户名或密码错误'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-layout {
  position: relative;
  display: flex;
  min-height: 100vh;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding: 18px 28px;
  background: linear-gradient(135deg, #edf3f8 0%, #e7eef6 100%);
}

.background-orb {
  position: absolute;
  border-radius: 999px;
  filter: blur(110px);
  pointer-events: none;
}

.orb-left {
  top: -14%;
  left: -8%;
  width: 360px;
  height: 360px;
  background: rgba(92, 191, 255, 0.18);
}

.orb-right {
  right: -12%;
  bottom: -18%;
  width: 420px;
  height: 420px;
  background: rgba(12, 179, 255, 0.1);
}

.grid-overlay {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(rgba(136, 171, 205, 0.16) 0.6px, transparent 0.6px);
  background-size: 24px 24px;
  opacity: 0.3;
  pointer-events: none;
}

.login-card {
  position: relative;
  z-index: 1;
  display: flex;
  width: min(1180px, 100%);
  min-height: 660px;
  overflow: hidden;
  border: 1px solid rgba(186, 204, 224, 0.55);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 28px 90px rgba(39, 71, 104, 0.14);
  backdrop-filter: blur(18px);
}

.brand-panel,
.form-panel {
  position: relative;
}

.brand-panel {
  flex: 0 0 56%;
  padding: 20px;
  background: linear-gradient(180deg, rgba(244, 248, 252, 0.92) 0%, rgba(232, 239, 247, 0.98) 100%);
}

.brand-grid {
  display: grid;
  grid-template-columns: 1.35fr 0.85fr;
  grid-template-rows: minmax(210px, 1.08fr) minmax(180px, 0.88fr);
  gap: 14px;
  height: 100%;
}

.brand-visual,
.brand-stat {
  position: relative;
  overflow: hidden;
  border-radius: 14px;
}

.brand-image-card {
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
}

.brand-image-card-1 {
  background: transparent;
  background-image: url('/assets/login1.png');
  background-size: cover;
  background-repeat: no-repeat;
  background-position: center;
}

.brand-image-card-2 {
  background-image: url('/assets/login2.png');
}

.brand-image-card-3 {
  background-image: url('/assets/login3.png');
}

.brand-image-card-4 {
  background-image: url('/assets/login4.jpg');
}

.brand-visual {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  border: 1px solid rgba(191, 205, 220, 0.62);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.88);
}

.brand-visual-primary {
  padding: 28px;
  color: #eef7ff;
}

.visual-badge {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  width: fit-content;
  min-height: 30px;
  padding: 0 12px;
  border: 1px solid rgba(130, 192, 241, 0.24);
  border-radius: 999px;
  color: #d8efff;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.visual-copy {
  position: relative;
  z-index: 1;
  max-width: 420px;
}

.visual-kicker {
  margin: 0 0 14px;
  color: rgba(162, 211, 248, 0.84);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.visual-title {
  margin: 0;
  font-size: 38px;
  line-height: 1.2;
  font-weight: 700;
}

.visual-description {
  margin: 18px 0 0;
  color: rgba(210, 228, 242, 0.76);
  font-size: 15px;
  line-height: 1.8;
}

.visual-dashboard {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-end;
  gap: 14px;
  margin-top: 28px;
}

.brand-stat > * ,
.brand-visual-secondary > * {
  position: relative;
  z-index: 1;
}

.visual-panel {
  backdrop-filter: blur(18px);
}

.visual-panel-main {
  flex: 1;
  min-width: 0;
  padding: 18px 18px 16px;
  border: 1px solid rgba(158, 212, 252, 0.14);
  border-radius: 18px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.visual-panel-kicker {
  display: block;
  color: rgba(188, 225, 249, 0.74);
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.visual-panel-value {
  margin-top: 10px;
  color: #ffffff;
  font-size: 34px;
  line-height: 1;
  font-weight: 700;
}

.visual-panel-text {
  margin-top: 8px;
  color: rgba(222, 236, 247, 0.76);
  font-size: 13px;
}

.visual-panel-bars {
  display: flex;
  gap: 8px;
  margin-top: 16px;
}

.visual-bar {
  display: block;
  height: 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
}

.bar-strong {
  width: 84px;
  background: linear-gradient(90deg, #70cbff 0%, #25a8ff 100%);
}

.bar-mid {
  width: 44px;
}

.bar-soft {
  width: 26px;
}

.visual-panel-float {
  width: 150px;
  padding: 16px 16px 14px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 18px;
  box-shadow: 0 18px 40px rgba(2, 11, 22, 0.28);
}

.visual-chip {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  color: #ffd4b4;
  font-size: 11px;
  font-weight: 600;
}

.visual-float-value {
  display: block;
  margin-top: 14px;
  font-size: 28px;
  line-height: 1;
  font-weight: 700;
  color: #ffffff;
}

.visual-float-text {
  display: block;
  margin-top: 8px;
  color: rgba(207, 226, 241, 0.74);
  font-size: 12px;
  line-height: 1.6;
}

.brand-visual-secondary {
  padding: 26px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94) 0%, rgba(245, 249, 252, 0.96) 100%);
}

.brand-mark {
  display: flex;
  align-items: center;
  gap: 16px;
}

.brand-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 58px;
  height: 58px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(92, 191, 255, 0.18), rgba(12, 179, 255, 0.24));
  box-shadow: 0 14px 28px rgba(92, 191, 255, 0.12);
  flex-shrink: 0;
}

.brand-icon-image {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

.brand-name-group {
  min-width: 0;
}

.brand-name {
  color: #10243a;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.brand-sub {
  margin-top: 6px;
  color: #6e8296;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.brand-rule {
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, rgba(128, 167, 203, 0.44) 0%, rgba(128, 167, 203, 0.12) 64%, transparent 100%);
}

.brand-points {
  display: grid;
  gap: 6px;
}

.brand-point {
  display: grid;
  gap: 6px;
}

.brand-point-title {
  color: #18324d;
  font-size: 14px;
  font-weight: 700;
}

.brand-point-text {
  color: #708397;
  font-size: 13px;
  line-height: 1.7;
}

.brand-stat {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 22px 24px;
  color: #fff;
}

.brand-stat-accent {
  background: linear-gradient(180deg, #ff8f42 0%, #ff7c33 100%);
  box-shadow: 0 18px 44px rgba(255, 143, 66, 0.24);
}

.brand-stat-muted {
  box-shadow: 0 18px 44px rgba(22, 135, 114, 0.18);
}

.brand-stat-value {
  position: relative;
  z-index: 1;
  margin-top: 18px;
  font-size: 44px;
  line-height: 1;
  font-weight: 700;
}

.brand-stat-label {
  position: relative;
  z-index: 1;
  margin-top: 10px;
  font-size: 14px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.88);
}

.brand-stat-note {
  position: relative;
  z-index: 1;
  margin-top: 12px;
  color: rgba(255, 255, 255, 0.68);
  font-size: 12px;
  line-height: 1.7;
}

.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30px 34px;
  background: rgba(255, 255, 255, 0.92);
}

.form-shell {
  width: 100%;
  max-width: 410px;
}

.form-topline {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 28px;
}

.form-topline-text {
  color: #6e7d8d;
  font-size: 13px;
  letter-spacing: 0.08em;
}

.form-container {
  padding: 0;
}

.form-header {
  margin-bottom: 30px;
}

.form-badge {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  margin-bottom: 16px;
  border-radius: 999px;
  background: rgba(92, 191, 255, 0.12);
  color: #0a7ec2;
  font-size: 12px;
  font-weight: 700;
}

.form-title {
  margin: 0 0 10px;
  color: #10243a;
  font-size: 32px;
  line-height: 1.2;
  font-weight: 700;
}

.form-subtitle {
  margin: 0;
  color: #708294;
  font-size: 14px;
  line-height: 1.8;
}

.field-label {
  display: block;
  margin-bottom: 8px;
  color: #27425a;
  font-size: 13px;
  font-weight: 600;
}

:deep(.el-form-item) {
  margin-bottom: 22px;
}

:deep(.el-input__wrapper) {
  min-height: 48px;
  border-radius: 10px;
  background: rgba(248, 251, 254, 0.96);
  box-shadow: 0 0 0 1px rgba(163, 186, 208, 0.26) inset;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(92, 191, 255, 0.34) inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #0cb3ff inset, 0 0 0 4px rgba(92, 191, 255, 0.12);
}

.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: -4px 0 22px;
}

.form-hint {
  color: #7a8998;
  font-size: 13px;
}

.login-btn {
  width: 100%;
  height: 48px;
  margin-top: 2px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(90deg, #2f8fff 0%, #1677ff 100%);
  box-shadow: 0 14px 30px rgba(22, 119, 255, 0.22);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.login-btn:hover,
.login-btn:focus {
  background: linear-gradient(90deg, #439bff 0%, #2a86ff 100%);
  box-shadow: 0 16px 34px rgba(22, 119, 255, 0.26);
}

@media (max-width: 1180px) {
  .login-card {
    min-height: 620px;
  }

  .brand-panel {
    flex-basis: 52%;
    padding: 18px;
  }

  .brand-grid {
    grid-template-columns: 1.2fr 0.8fr;
  }

  .visual-title {
    font-size: 32px;
  }

  .form-panel {
    padding: 26px 28px;
  }
}

@media (max-width: 980px) {
  .login-layout {
    padding: 20px;
  }

  .login-card {
    flex-direction: column;
    min-height: auto;
  }

  .brand-panel {
    flex: none;
  }

  .brand-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    grid-template-rows: auto;
  }

  .brand-visual-primary {
    min-height: 240px;
  }

  .form-panel {
    padding: 32px 26px 34px;
  }

  .form-shell {
    max-width: 100%;
  }
}

@media (max-width: 640px) {
  .login-layout {
    padding: 14px;
  }

  .login-card {
    border-radius: 14px;
  }

  .brand-panel {
    padding: 14px;
  }

  .brand-grid {
    grid-template-columns: 1fr;
  }

  .brand-visual-primary,
  .brand-visual-secondary,
  .brand-stat {
    min-height: auto;
  }

  .brand-visual-primary,
  .brand-visual-secondary {
    padding: 22px;
  }

  .visual-title {
    font-size: 26px;
  }

  .brand-name {
    font-size: 24px;
  }

  .brand-stat-value {
    font-size: 36px;
  }

  .form-panel {
    padding: 26px 18px 28px;
  }

  .form-topline {
    justify-content: flex-start;
    margin-bottom: 18px;
  }

  .form-title {
    font-size: 28px;
  }

  :deep(.el-input__wrapper) {
    border-radius: 8px;
  }

  .login-btn {
    border-radius: 10px;
  }
}
</style>

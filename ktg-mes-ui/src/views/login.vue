<template>
  <div class="entry" :class="{ 'entry--dark': loginTheme === 'dark' }">
    <header class="entry__top">
      <div class="entry__brand">
        <span class="entry__mark"><i class="el-icon-cpu" aria-hidden="true" /></span>
        <span>温衡智造 <small>MES</small></span>
      </div>
      <button type="button" class="entry__theme" :aria-pressed="loginTheme === 'dark'"
        @click="loginTheme = loginTheme === 'light' ? 'dark' : 'light'">
        <i :class="loginTheme === 'light' ? 'el-icon-moon' : 'el-icon-sunny'" aria-hidden="true" />
        {{ loginTheme === 'light' ? '深色模式' : '浅色模式' }}
      </button>
    </header>

    <figure class="entry__showcase">
      <div class="entry__photo" role="img" aria-label="质检人员在工作台上检查红外测温枪"></div>
      <figcaption>红外测温枪质检场景示意</figcaption>
    </figure>

    <main class="entry__access">
      <div class="entry__card">
        <h1>登录生产工作台</h1>
        <p class="entry__intro">登录后可查看工单、生产进度和质检记录。</p>

        <div class="entry__modes" role="group" aria-label="登录方式">
          <button v-for="mode in loginModes" :key="mode.id" type="button"
            :aria-pressed="loginTab === mode.id" :disabled="loading" @click="loginTab = mode.id">
            <i :class="mode.icon" aria-hidden="true" />{{ mode.label }}
          </button>
        </div>

        <form v-if="loginTab === 'password'" id="login-form-password" class="entry__form" @submit.prevent="handleLogin">
          <div class="entry__field">
            <label for="login-username">账号</label>
            <input id="login-username" v-model="loginForm.username" autocomplete="username"
              placeholder="请输入账号" aria-describedby="login-error-password">
          </div>
          <div class="entry__field">
            <label for="login-password">密码</label>
            <div class="entry__password">
              <input id="login-password" v-model="loginForm.password" :type="showPassword ? 'text' : 'password'"
                autocomplete="current-password" placeholder="请输入密码" aria-describedby="login-error-password">
              <button id="toggle-password" type="button" @click="showPassword = !showPassword">
                {{ showPassword ? '隐藏' : '显示' }}<span class="entry__sr">密码</span>
              </button>
            </div>
          </div>
          <div class="entry__options">
            <label for="remember-me"><input id="remember-me" v-model="loginForm.rememberMe" type="checkbox"> 记住账号和密码</label>
            <button type="button" class="entry__link" @click="forgotPassword">忘记密码</button>
          </div>
          <p id="login-error-password" class="entry__error" aria-live="polite">{{ loginError }}</p>
          <button id="login-button-password" type="submit" class="entry__submit" :disabled="loading">
            {{ loading ? '登录中…' : '登录' }} <i class="el-icon-right" aria-hidden="true" />
          </button>
        </form>

        <form v-else-if="loginTab === 'face'" id="login-form-face" class="entry__form" @submit.prevent="startFaceLogin">
          <div class="entry__field">
            <label for="login-face-username">账号</label>
            <input id="login-face-username" v-model="loginForm.username" autocomplete="username"
              placeholder="请输入已录入人脸的账号" aria-describedby="face-login-hint login-error-face">
          </div>
          <p id="face-login-hint" class="entry__notice">首次使用，请先用密码登录并在个人中心录入人脸。系统仅保存特征向量。</p>
          <p id="login-error-face" class="entry__error" aria-live="polite">{{ faceError }}</p>
          <button id="login-button-face" type="submit" class="entry__submit" :disabled="loading">
            <i class="el-icon-camera" aria-hidden="true" /> {{ loading ? '正在验证…' : '开始人脸识别' }}
          </button>
        </form>

        <div v-else id="login-form-sms" class="entry__form">
          <p class="entry__notice">短信验证码登录暂未开通，请使用密码或人脸登录。</p>
          <button type="button" class="entry__submit" @click="loginTab = 'password'">返回密码登录</button>
        </div>

        <div class="entry__support">
          <button type="button" class="entry__link" @click="openLoginHelp">登录帮助</button>
          <button type="button" class="entry__link" @click="goToRegister">开通账号</button>
          <button type="button" class="entry__link" @click="productVisible = true">了解产品</button>
        </div>
      </div>
    </main>

    <el-dialog title="登录验证" :visible.sync="captchaVisible" width="min(400px, calc(100vw - 32px))"
      :close-on-click-modal="false" custom-class="entry-dialog" @opened="focusCaptchaInput">
      <form class="entry__form" @submit.prevent="handleLogin">
        <div class="entry__field">
          <label for="login-captcha">请输入图形验证码</label>
          <div class="entry__verification">
            <input id="login-captcha" ref="captchaInput" v-model="loginForm.code" autocomplete="off"
              aria-describedby="login-captcha-error" placeholder="验证码">
            <button type="button" class="entry__captcha" aria-label="刷新验证码" @click="getCode">
              <img v-if="codeUrl" :src="codeUrl" alt="图形验证码"><span v-else>刷新验证码</span>
            </button>
          </div>
        </div>
        <p id="login-captcha-error" class="entry__error" role="alert">{{ captchaError }}</p>
        <button type="submit" class="entry__submit" :disabled="loading || !captchaReady">{{ loading ? '登录中…' : '验证并登录' }}</button>
      </form>
    </el-dialog>
    <el-dialog title="登录帮助" :visible.sync="helpVisible" width="min(440px, calc(100vw - 32px))" custom-class="entry-dialog">
      <p class="entry__intro">如需重置密码，请联系系统管理员。</p>
      <div class="entry__field">
        <label for="login-printer">打印机客户端（可选）</label>
        <el-select id="login-printer" v-model="clientId" clearable @change="clientChanged = true" placeholder="请选择打印机客户端">
          <el-option v-for="client in clientList" :key="client.clientId" :label="client.clientName" :value="client.clientId" />
        </el-select>
      </div>
    </el-dialog>
    <el-dialog title="温衡智造" :visible.sync="productVisible" width="min(480px, calc(100vw - 32px))" custom-class="entry-dialog">
      <p class="entry__intro">红外测温枪智能制造平台，覆盖主数据、仓储、生产、质量、设备和班组管理。</p>
    </el-dialog>
    <face-capture :visible.sync="faceVisible" title="人脸登录" tip="人脸特征只用于本次身份核验，不会上传照片" @captured="handleFaceCaptured" />
  </div>
</template>

<script>
import { getCodeImg } from '@/api/login'
import { getAll } from '@/api/print/client'
import FaceCapture from '@/components/FaceCapture'
import Cookies from 'js-cookie'
import { encrypt, decrypt } from '@/utils/jsencrypt'

export default {
  name: 'Login',
  components: { FaceCapture },
  data() {
    return {
      loginModes: [
        { id: 'password', label: '密码', icon: 'el-icon-lock' },
        { id: 'face', label: '人脸', icon: 'el-icon-camera' },
        { id: 'sms', label: '短信未开通', icon: 'el-icon-message' }
      ],
      loginTab: 'password', showPassword: false, loginTheme: 'light',
      loginForm: { username: '', password: '', rememberMe: false, code: '', uuid: '' },
      loading: false, loginError: '', captchaOnOff: true, captchaReady: false,
      captchaVisible: false, captchaError: '', codeUrl: '',
      helpVisible: false, productVisible: false, clientId: null, clientList: [], clientChanged: false,
      faceVisible: false, faceError: '',
      register: false, redirect: undefined
    }
  },
  watch: {
    $route: { immediate: true, handler(route) { this.redirect = route.query && route.query.redirect } }
  },
  created() { this.getCookie(); this.getCode() },
  methods: {
    focusCaptchaInput() {
      if (this.$refs.captchaInput) this.$refs.captchaInput.focus()
    },
    getCookie() {
      const username = Cookies.get('username')
      const password = Cookies.get('password')
      if (username !== undefined) this.loginForm.username = username
      if (password !== undefined) this.loginForm.password = decrypt(password)
      this.loginForm.rememberMe = Cookies.get('rememberMe') === 'true'
    },
    async getCode() {
      this.captchaReady = false
      this.captchaError = ''
      this.loginForm.code = ''
      try {
        const res = await getCodeImg()
        this.captchaOnOff = res.captchaOnOff === undefined ? true : res.captchaOnOff
        this.codeUrl = this.captchaOnOff ? 'data:image/gif;base64,' + res.img : ''
        this.loginForm.uuid = this.captchaOnOff ? res.uuid : ''
        this.captchaReady = true
      } catch (e) { this.captchaError = '验证码加载失败，请点击刷新重试。' }
    },
    async handleLogin() {
      if (this.loading) return
      this.loginError = ''
      if (!this.loginForm.username.trim() || !this.loginForm.password) {
        this.loginError = '请输入用户名和密码'
        return
      }
      if (!this.captchaReady || (this.captchaOnOff && (!this.captchaVisible || !this.loginForm.code.trim()))) {
        if (this.captchaVisible && this.captchaReady) this.captchaError = '请输入验证码'
        this.captchaVisible = true
        this.$nextTick(this.focusCaptchaInput)
        return
      }
      this.loading = true
      this.captchaError = ''
      try {
        await this.$store.dispatch('Login', this.loginForm)
        if (this.loginForm.rememberMe) {
          Cookies.set('username', this.loginForm.username, { expires: 30 })
          Cookies.set('password', encrypt(this.loginForm.password), { expires: 30 })
          Cookies.set('rememberMe', 'true', { expires: 30 })
        } else {
          Cookies.remove('username'); Cookies.remove('password'); Cookies.remove('rememberMe')
        }
        const client = this.clientList.find(c => String(c.clientId) === String(this.clientId))
        if (this.clientChanged) {
          if (client) localStorage.setItem('defaultClient', JSON.stringify(client))
          else localStorage.removeItem('defaultClient')
        }
        this.captchaVisible = false
        await this.$router.push({ path: this.redirect || '/' }).catch(() => {})
      } catch (e) {
        this.loginError = (e && e.message) || '登录失败，请检查用户名、密码及验证码'
        if (this.captchaOnOff) { await this.getCode(); this.captchaError = this.loginError }
      } finally { this.loading = false }
    },
    startFaceLogin() {
      this.faceError = ''
      if (this.loading) return
      if (!this.loginForm.username.trim()) {
        this.faceError = '请先输入用户账号'
        return
      }
      this.faceVisible = true
    },
    async handleFaceCaptured(payload) {
      this.faceVisible = false
      this.loading = true
      this.faceError = ''
      try {
        await this.$store.dispatch('FaceLogin', {
          username: this.loginForm.username.trim(),
          descriptor: Array.from(payload.descriptor)
        })
        Cookies.set('username', this.loginForm.username.trim(), { expires: 30 })
        await this.$router.push({ path: this.redirect || '/' }).catch(() => {})
      } catch (error) {
        this.faceError = (error && error.message) || '人脸登录失败，请重试或使用密码登录'
      } finally {
        this.loading = false
      }
    },
    forgotPassword() { this.$alert('请联系系统管理员核验身份并重置密码。', '忘记密钥', { confirmButtonText: '知道了' }) },
    goToRegister() {
      if (this.register) this.$router.push('/register')
      else this.$message.info('自助注册暂未开放，请联系管理员开通账户。')
    },
    async openLoginHelp() {
      this.helpVisible = true
      try {
        const res = await getAll()
        this.clientList = res.data || []
        const saved = JSON.parse(localStorage.getItem('defaultClient') || 'null')
        if (saved) this.clientId = saved.clientId
      } catch (e) { /* Login remains available if optional printer settings cannot be read. */ }
    }
  }
}

</script>

<style lang="scss" scoped src="@/assets/styles/mes-entry.scss"></style>

<template>
  <div class="login-root" :class="{ 'theme-green': loginTheme === 'green' }">
    <!-- Data Grid Overlay -->
    <div class="fixed inset-0 data-grid-overlay pointer-events-none z-1"></div>

    <!-- Animated Gradient Orbs -->
    <div class="fixed -top-[20%] -left-[15%] w-[55%] h-[55%] rounded-full bg-gradient-to-br from-primary/15 to-primary/5 blur-[120px] z-0 orb-1"></div>
    <div class="fixed -bottom-[20%] -right-[15%] w-[55%] h-[55%] rounded-full bg-gradient-to-tl from-primary/10 to-primary/5 blur-[120px] z-0 orb-2"></div>
    <div class="fixed top-[40%] left-[10%] w-[30%] h-[40%] rounded-full bg-primary/5 blur-[80px] z-0" style="animation: orbFloat 25s ease-in-out infinite reverse;"></div>

    <!-- Floating Decorative Particles -->
    <div id="float-particles" class="fixed inset-0 pointer-events-none z-1 overflow-hidden"></div>

    <!-- Theme Switcher (top-right corner) -->
    <div class="fixed top-6 right-6 z-50 flex items-center gap-2">
      <div id="theme-switcher-mount" class="flex items-center gap-1"><button type="button" class="theme-switcher-btn" aria-label="切换主题" title="切换主题" @click="loginTheme = loginTheme === 'light' ? 'green' : 'light'"><span class="material-symbols-outlined text-xl">{{ loginTheme === 'light' ? 'light_mode' : 'eco' }}</span></button></div>
    </div>

    <!-- Left Part — decorative showcase -->
    <div class="leftPart">
      <!-- 四角 bracket -->
      <div class="leftPart-corner-bl"></div>
      <div class="leftPart-corner-br"></div>
      <!-- 装饰圆环群 -->
      <div class="leftPart-ring pulse" style="width:480px;height:480px;top:-140px;right:-160px;border-color:rgba(0,200,83,0.15);"></div>
      <div class="leftPart-ring" style="width:300px;height:300px;top:-60px;right:-90px;border-color:rgba(0,200,83,0.10);"></div>
      <div class="leftPart-ring pulse" style="width:220px;height:220px;bottom:80px;left:-80px;animation-delay:-2s;"></div>
      <div class="leftPart-ring" style="width:140px;height:140px;bottom:40px;left:-40px;border-color:rgba(0,200,83,0.14);"></div>
      <div class="leftPart-ring pulse" style="width:80px;height:80px;top:42%;right:36px;animation-delay:-1s;"></div>
      <div class="leftPart-ring" style="width:40px;height:40px;top:28%;right:22%;border-color:rgba(0,200,83,0.22);"></div>
      <!-- 顶底细线 -->
      <div class="leftPart-hairline top"></div>
      <div class="leftPart-hairline bottom"></div>
      <!-- 右侧分割线 -->
      <div class="leftPart-divider"></div>
      <!-- 图片轮播 -->
      <div v-if="isDrilling" class="imgDiv">
        <img id="drillIntroImg" :src="introImages[slideIndex]" alt="红外测温枪智能制造宣传图">
      </div>
      <div v-else class="imgDiv">
        <img id="introImg" :src="introImages[slideIndex]" alt="宣传图">
      </div>
    </div>

    <!-- 右侧登录区域 -->
    <div class="login-center-wrapper">
      <div class="login-card anim-fade-right">
        <div class="space-y-5">
          <!-- Logo -->
          <div class="flex justify-center mb-4">
            <img :src="logo" alt="Logo" class="w-[90px] h-[90px]">
          </div>
          <h2 class="text-center text-4xl font-extrabold leading-tight tracking-tight text-on-surface dingtalk-font">
            {{ business.brand }}<br>
            <span style="color:var(--color-primary);-webkit-background-clip:text;background-clip:text;">红外测温枪智能制造平台</span>
          </h2>
          <p class="text-on-surface-variant text-base leading-relaxed font-light text-center">
            贯通物料、生产、质量与设备管理，让红外测温枪制造过程清晰可控、全程可追溯。
          </p>
        </div>

        <!-- 登录模式 Tab -->
        <div class="mb-6 anim-fade-up anim-delay-1">
          <div class="mt-4 flex items-center justify-center gap-0">
            <div class="login-tab-line login-tab-line-left"></div>
            <button class="login-tab-btn" :class="{ active: loginTab === 'password' }" @click="loginTab = 'password'" data-tab="password" type="button">密码登录</button>
            <div class="login-tab-divider"></div>
            <button class="login-tab-btn" :class="{ active: loginTab === 'sms' }" @click="loginTab = 'sms'" data-tab="sms" type="button">验证码登录</button>
            <div class="login-tab-line login-tab-line-right"></div>
          </div>
        </div>

        <!-- 密码登录表单 -->
        <form class="space-y-4" id="login-form-password" v-show="loginTab === 'password'" @submit.prevent="handleLogin">
          <div class="space-y-1 form-field anim-fade-up anim-delay-2">
            <label class="font-label text-[0.6875rem] uppercase tracking-widest text-primary/80 ml-1 font-semibold">用户名 / 识别码</label>
            <div class="relative group">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant group-focus-within:text-primary transition-all duration-300 text-xl">account_circle</span>
              <input
                id="login-username" v-model="loginForm.username" aria-label="用户名 / 识别码"
                class="w-full bg-surface-container-lowest border border-outline-variant/60 focus:ring-primary/50 focus:border-primary/50 input-border py-3.5 pl-12 pr-4 text-on-surface placeholder:text-on-surface-variant/40 transition-all duration-300 login-input"
                placeholder="Intelligence Officer ID"
                type="text"
                autocomplete="username"
              >
              <div class="absolute right-4 top-1/2 -translate-y-1/2 opacity-0 group-focus-within:opacity-100 transition-opacity duration-300">
                <span class="material-symbols-outlined text-primary text-sm">edit</span>
              </div>
            </div>
          </div>

          <div class="space-y-1 form-field anim-fade-up anim-delay-3">
            <label class="font-label text-[0.6875rem] uppercase tracking-widest text-primary/80 ml-1 font-semibold">安全密钥 / 密码</label>
            <div class="relative group">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant group-focus-within:text-primary transition-all duration-300 text-xl">key</span>
              <input
                id="login-password" v-model="loginForm.password" :type="showPassword ? 'text' : 'password'" aria-label="安全密钥 / 密码"
                class="w-full bg-surface-container-lowest border border-outline-variant/60 ring-1 ring-transparent focus:ring-primary/50 focus:border-primary/50 input-border py-3.5 pl-12 pr-12 text-on-surface placeholder:text-on-surface-variant/40 transition-all duration-300 login-input"
                placeholder="••••••••••••"
                autocomplete="current-password"
              >
              <button type="button" id="toggle-password" @click="showPassword = !showPassword" :aria-label="showPassword ? '隐藏密码' : '显示密码'" class="absolute right-4 top-1/2 -translate-y-1/2 text-on-surface-variant hover:text-primary transition-colors duration-200">
                <span class="material-symbols-outlined text-xl" id="eye-icon">{{ showPassword ? 'visibility_off' : 'visibility' }}</span>
              </button>
            </div>
          </div>

          <div class="flex items-center justify-between py-0.5 anim-fade-up anim-delay-4">
            <label class="flex items-center space-x-2.5 cursor-pointer group">
              <input class="rounded border-outline-variant/40 text-primary focus:ring-2 focus:ring-primary/40 focus:ring-offset-0 transition-all duration-200 w-4 h-4 accent-[var(--color-primary)]" type="checkbox" id="remember-me" v-model="loginForm.rememberMe">
              <span class="text-xs text-on-surface-variant group-hover:text-on-surface transition-colors duration-200">保持登录状态</span>
            </label>
            <a class="text-xs text-primary/80 hover:text-primary transition-all duration-200 underline-offset-4 hover:underline" href="#" @click.prevent="forgotPassword">忘记密钥？</a>
          </div>

          <button
            id="login-button-password" :disabled="loading"
            class="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-3.5 rounded-lg shadow-md transition-all transform hover:-translate-y-0.5 flex items-center justify-center space-x-2 group btn-press relative overflow-hidden anim-fade-up anim-delay-5"
            type="submit"
          >
            <span class="relative z-10 flex items-center gap-2">
              <span id="btn-text-password">{{ loading ? '登录中…' : '登录' }}</span>
              <span class="material-symbols-outlined text-xl group-hover:translate-x-1 transition-transform duration-300" style="font-variation-settings:'FILL'1">arrow_forward</span>
            </span>
            <span id="btn-spinner-password" class="hidden absolute inset-0 items-center justify-center">
              <span class="flex gap-1">
                <span class="w-1.5 h-1.5 bg-white rounded-full animate-bounce" style="animation-delay:0ms;"></span>
                <span class="w-1.5 h-1.5 bg-white rounded-full animate-bounce" style="animation-delay:150ms;"></span>
                <span class="w-1.5 h-1.5 bg-white rounded-full animate-bounce" style="animation-delay:300ms;"></span>
              </span>
            </span>
          </button>

          <p class="text-sm text-error mt-0.5 min-h-5 text-center" id="login-error-password" aria-live="polite">{{ loginError }}</p>
        </form>

        <!-- 验证码登录表单 -->
        <form class="space-y-4" id="login-form-sms" v-show="loginTab === 'sms'" @submit.prevent="smsUnavailable">
          <div class="space-y-1 form-field anim-fade-up anim-delay-2">
            <label class="font-label text-[0.6875rem] uppercase tracking-widest text-primary/80 ml-1 font-semibold">手机号码</label>
            <div class="relative group">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant group-focus-within:text-primary transition-all duration-300 text-xl">smartphone</span>
              <input
                id="login-phone"
                class="w-full bg-surface-container-lowest border border-outline-variant/60 ring-1 ring-transparent focus:ring-primary/50 focus:border-primary/50 input-border py-3.5 pl-12 pr-4 text-on-surface placeholder:text-on-surface-variant/40 transition-all duration-300 login-input"
                placeholder="请输入手机号"
                type="tel"
                maxlength="11"
                autocomplete="tel"
              >
            </div>
          </div>

          <div class="space-y-1 form-field anim-fade-up anim-delay-3">
            <label class="font-label text-[0.6875rem] uppercase tracking-widest text-primary/80 ml-1 font-semibold">短信验证码</label>
            <div class="relative group">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant group-focus-within:text-primary transition-all duration-300 text-xl">mark_email_unread</span>
              <input
                id="login-sms-code"
                class="w-full bg-surface-container-lowest border border-outline-variant/60 ring-1 ring-transparent focus:ring-primary/50 focus:border-primary/50 input-border py-3.5 pl-12 pr-28 text-on-surface placeholder:text-on-surface-variant/40 transition-all duration-300 login-input"
                placeholder="请输入验证码"
                type="text"
                maxlength="8"
                autocomplete="one-time-code"
              >
              <button type="button" id="send-code-btn" @click="smsUnavailable" class="absolute right-2 top-1/2 -translate-y-1/2 px-3 py-1.5 rounded-lg bg-blue-600 text-white text-xs font-bold border border-blue-600 hover:bg-blue-700 hover:border-blue-700 transition-all duration-200">
                获取验证码
              </button>
            </div>
          </div>

          <button
            id="login-button-sms"
            class="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-3.5 rounded-lg shadow-md transition-all transform hover:-translate-y-0.5 flex items-center justify-center space-x-2 group btn-press relative overflow-hidden anim-fade-up anim-delay-5"
            type="submit"
          >
            <span class="relative z-10 flex items-center gap-2">
              <span id="btn-text-sms">登录</span>
              <span class="material-symbols-outlined text-xl group-hover:translate-x-1 transition-transform duration-300" style="font-variation-settings:'FILL'1">arrow_forward</span>
            </span>
            <span id="btn-spinner-sms" class="hidden absolute inset-0 items-center justify-center">
              <span class="flex gap-1">
                <span class="w-1.5 h-1.5 bg-white rounded-full animate-bounce" style="animation-delay:0ms;"></span>
                <span class="w-1.5 h-1.5 bg-white rounded-full animate-bounce" style="animation-delay:150ms;"></span>
                <span class="w-1.5 h-1.5 bg-white rounded-full animate-bounce" style="animation-delay:300ms;"></span>
              </span>
            </span>
          </button>

          <p class="text-sm text-error mt-0.5 min-h-5 text-center" id="login-error-sms"></p>
        </form>

        <!-- 第三方认证 & 注册链接 -->
        <div class="mt-6 pt-5 border-t border-outline-variant/30 flex flex-col items-center space-y-4 anim-fade-up anim-delay-6">
          <div class="flex items-center space-x-3">
            <button class="w-10 h-10 rounded-xl border border-outline-variant/40 flex items-center justify-center hover:bg-primary/10 hover:border-primary/40 hover:text-primary transition-all duration-300 group" title="指纹识别" type="button" @click="alternativeLogin">
              <span class="material-symbols-outlined text-lg group-hover:scale-110 transition-transform">fingerprint</span>
            </button>
            <button class="w-10 h-10 rounded-xl border border-outline-variant/40 flex items-center justify-center hover:bg-primary/10 hover:border-primary/40 hover:text-primary transition-all duration-300 group" title="扫码登录" type="button" @click="alternativeLogin">
              <span class="material-symbols-outlined text-lg group-hover:scale-110 transition-transform">qr_code_scanner</span>
            </button>
            <button class="w-10 h-10 rounded-xl border border-outline-variant/40 flex items-center justify-center hover:bg-tertiary/10 hover:border-tertiary/40 hover:text-tertiary transition-all duration-300 group" title="安全问题" type="button" @click="openLoginHelp">
              <span class="material-symbols-outlined text-lg group-hover:scale-110 transition-transform">quiz</span>
            </button>
          </div>
          <div class="w-full flex justify-center gap-4 flex-wrap">
              <p class="text-sm text-on-surface-variant">
              账户未注册？<a class="text-blue-600 font-bold hover:text-blue-700 transition-colors duration-200 underline-offset-4 hover:underline cursor-pointer" @click="goToRegister">立即注册</a>
            </p>
            <a class="text-sm text-primary font-bold hover:text-tertiary transition-colors duration-200 underline-offset-4 hover:underline cursor-pointer" href="#" @click.prevent="productVisible = true">
              了解产品
            </a>
          </div>
        </div>

        <div class="mt-5 flex items-center justify-center space-x-2 opacity-40">
          <span class="material-symbols-outlined text-xs">verified_user</span>
        </div>
      </div>
    </div>

    <!-- Bottom decorative text -->
    <div class="fixed bottom-6 left-6 flex items-center space-x-3 opacity-20 pointer-events-none z-10">
      <div class="h-px w-10 bg-on-surface"></div>
      <span class="font-label text-[0.625rem] tracking-[0.3em] uppercase">Core Revision 4.0.2</span>
    </div>

    <el-dialog title="登录验证" :visible.sync="captchaVisible" width="380px" :close-on-click-modal="false" class="login-verification">
      <form @submit.prevent="handleLogin">
        <p class="verification-hint">请输入图形验证码完成登录</p>
        <div class="verification-row">
          <input ref="captchaInput" v-model="loginForm.code" class="login-input" aria-label="图形验证码" autocomplete="off" placeholder="请输入验证码">
          <button type="button" class="verification-image" aria-label="刷新验证码" @click="getCode"><img v-if="codeUrl" :src="codeUrl" alt="验证码"><span v-else>刷新验证码</span></button>
        </div>
        <p class="verification-error" role="alert">{{ captchaError }}</p>
        <button type="submit" class="verification-submit" :disabled="loading || !captchaReady">{{ loading ? '登录中…' : '验证并登录' }}</button>
      </form>
    </el-dialog>
    <el-dialog title="登录帮助" :visible.sync="helpVisible" width="420px" class="login-verification">
      <p class="verification-hint">如需重置密码，请联系系统管理员。</p>
      <label class="printer-label">打印机客户端（可选）</label>
      <el-select v-model="clientId" clearable @change="clientChanged = true" placeholder="请选择打印机客户端" style="width:100%"><el-option v-for="client in clientList" :key="client.clientId" :label="client.clientName" :value="client.clientId" /></el-select>
    </el-dialog>
    <el-dialog title="温衡智造" :visible.sync="productVisible" width="520px" class="login-verification">
      <p class="verification-hint">红外测温枪智能制造平台</p>
      <p>贯通物料、生产、质量与设备管理，让红外测温枪制造过程清晰可控、全程可追溯。</p>
    </el-dialog>
  </div>
</template>

<script>
import { getCodeImg } from '@/api/login'
import { getAll } from '@/api/print/client'
import Cookies from 'js-cookie'
import { encrypt, decrypt } from '@/utils/jsencrypt'

const introImages = [1, 2, 3, 4, 5, 6].map(n => require('@/assets/images/p' + n + '.png'))

export default {
  name: 'Login',
  data() {
    return {
      business: { brand: '温衡智造' }, isDrilling: true,
      logo: require('@/assets/logo/75638C1949384F71E4BBB1A939C126A3.png'), introImages, slideIndex: 0,
      loginTab: 'password', showPassword: false, loginTheme: 'light',
      loginForm: { username: '', password: '', rememberMe: false, code: '', uuid: '' },
      loading: false, loginError: '', captchaOnOff: true, captchaReady: false,
      captchaVisible: false, captchaError: '', codeUrl: '',
      helpVisible: false, productVisible: false, clientId: null, clientList: [], clientChanged: false,
      register: false, redirect: undefined
    }
  },
  watch: {
    $route: { immediate: true, handler(route) { this.redirect = route.query && route.query.redirect } }
  },
  created() { this.getCookie(); this.getCode() },
  mounted() { this.introTimer = setInterval(() => { this.slideIndex = (this.slideIndex + 1) % this.introImages.length }, 4000) },
  beforeDestroy() { clearInterval(this.introTimer) },
  methods: {
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
        this.$nextTick(() => { if (this.$refs.captchaInput) this.$refs.captchaInput.focus() })
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
    smsUnavailable() { this.$message.info('短信登录暂未开通，请使用密码登录。') },
    alternativeLogin() { this.$message.info('该认证方式暂未开通，请使用密码登录。') },
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

<style scoped src="@/assets/styles/user-login-reference.css"></style>

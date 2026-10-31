<template>
  <div class="login-root">
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
      <div id="theme-switcher-mount" class="flex items-center gap-1"></div>
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
        <img id="drillIntroImg" src="/assets/drilling/drill-intro1.jpg" alt="钻井系统宣传图">
      </div>
      <div v-else class="imgDiv">
        <img id="introImg" :src="introSrc" alt="宣传图">
      </div>
    </div>

    <!-- 右侧登录区域 -->
    <div class="login-center-wrapper">
      <div class="login-card anim-fade-right">
        <div class="space-y-5">
          <!-- Logo -->
          <div class="flex justify-center mb-4">
            <img :src="isDrilling ? '/assets/drilling/wellan-logo.png' : '/assets/Logo.png'" alt="Logo" class="w-[90px] h-[90px]">
          </div>
          <h2 class="text-center text-4xl font-extrabold leading-tight tracking-tight text-on-surface dingtalk-font">
            {{ business.brand }}<br>
            <span style="color:var(--color-primary);-webkit-background-clip:text;background-clip:text;">{{ isDrilling ? '多源感知与井喷风险预警' : '安全检测平台' }}</span>
          </h2>
          <p class="text-on-surface-variant text-base leading-relaxed font-light text-center">
            {{ isDrilling ? '汇集七类传感器数据，提供确定性风险分析、专家远程协同和工业资产关联。' : '利用AI智能引导、RAG、多模态融合识别检测您的受骗风险，通过分级预警、检测报告和监护人联动为您的安全护航。' }}
          </p>
        </div>

        <!-- 登录模式 Tab -->
        <div class="mb-6 anim-fade-up anim-delay-1">
          <div class="mt-4 flex items-center justify-center gap-0">
            <div class="login-tab-line login-tab-line-left"></div>
            <button class="login-tab-btn active" data-tab="password" type="button">密码登录</button>
            <div class="login-tab-divider"></div>
            <button class="login-tab-btn" data-tab="sms" type="button">验证码登录</button>
            <div class="login-tab-line login-tab-line-right"></div>
          </div>
        </div>

        <!-- 密码登录表单 -->
        <form class="space-y-4" id="login-form-password">
          <div class="space-y-1 form-field anim-fade-up anim-delay-2">
            <label class="font-label text-[0.6875rem] uppercase tracking-widest text-primary/80 ml-1 font-semibold">用户名 / 识别码</label>
            <div class="relative group">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant group-focus-within:text-primary transition-all duration-300 text-xl">account_circle</span>
              <input
                id="login-username"
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
                id="login-password"
                class="w-full bg-surface-container-lowest border border-outline-variant/60 ring-1 ring-transparent focus:ring-primary/50 focus:border-primary/50 input-border py-3.5 pl-12 pr-12 text-on-surface placeholder:text-on-surface-variant/40 transition-all duration-300 login-input"
                placeholder="••••••••••••"
                type="password"
                autocomplete="current-password"
              >
              <button type="button" id="toggle-password" class="absolute right-4 top-1/2 -translate-y-1/2 text-on-surface-variant hover:text-primary transition-colors duration-200">
                <span class="material-symbols-outlined text-xl" id="eye-icon">visibility</span>
              </button>
            </div>
          </div>

          <div class="flex items-center justify-between py-0.5 anim-fade-up anim-delay-4">
            <label class="flex items-center space-x-2.5 cursor-pointer group">
              <input class="rounded border-outline-variant/40 text-primary focus:ring-2 focus:ring-primary/40 focus:ring-offset-0 transition-all duration-200 w-4 h-4 accent-[var(--color-primary)]" type="checkbox" id="remember-me">
              <span class="text-xs text-on-surface-variant group-hover:text-on-surface transition-colors duration-200">保持登录状态</span>
            </label>
            <a class="text-xs text-primary/80 hover:text-primary transition-all duration-200 underline-offset-4 hover:underline" href="#">忘记密钥？</a>
          </div>

          <button
            id="login-button-password"
            class="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-3.5 rounded-lg shadow-md transition-all transform hover:-translate-y-0.5 flex items-center justify-center space-x-2 group btn-press relative overflow-hidden anim-fade-up anim-delay-5"
            type="submit"
          >
            <span class="relative z-10 flex items-center gap-2">
              <span id="btn-text-password">登录</span>
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

          <p class="text-sm text-error mt-0.5 min-h-5 text-center" id="login-error-password"></p>
        </form>

        <!-- 验证码登录表单 -->
        <form class="space-y-4 hidden" id="login-form-sms">
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
              <button type="button" id="send-code-btn" class="absolute right-2 top-1/2 -translate-y-1/2 px-3 py-1.5 rounded-lg bg-blue-600 text-white text-xs font-bold border border-blue-600 hover:bg-blue-700 hover:border-blue-700 transition-all duration-200">
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
            <button class="w-10 h-10 rounded-xl border border-outline-variant/40 flex items-center justify-center hover:bg-primary/10 hover:border-primary/40 hover:text-primary transition-all duration-300 group" title="指纹识别">
              <span class="material-symbols-outlined text-lg group-hover:scale-110 transition-transform">fingerprint</span>
            </button>
            <button class="w-10 h-10 rounded-xl border border-outline-variant/40 flex items-center justify-center hover:bg-primary/10 hover:border-primary/40 hover:text-primary transition-all duration-300 group" title="扫码登录">
              <span class="material-symbols-outlined text-lg group-hover:scale-110 transition-transform">qr_code_scanner</span>
            </button>
            <button class="w-10 h-10 rounded-xl border border-outline-variant/40 flex items-center justify-center hover:bg-tertiary/10 hover:border-tertiary/40 hover:text-tertiary transition-all duration-300 group" title="安全问题">
              <span class="material-symbols-outlined text-lg group-hover:scale-110 transition-transform">quiz</span>
            </button>
          </div>
          <div class="w-full flex justify-center gap-4 flex-wrap">
              <p class="text-sm text-on-surface-variant">
              账户未注册？<a class="text-blue-600 font-bold hover:text-blue-700 transition-colors duration-200 underline-offset-4 hover:underline cursor-pointer" @click="goToRegister">立即注册</a>
            </p>
            <a class="text-sm text-primary font-bold hover:text-tertiary transition-colors duration-200 underline-offset-4 hover:underline cursor-pointer" href="/#/agent-info">
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { mountThemeSwitcher } from '@/api/theme-switcher'
import { business, isDrilling } from '@/config/business'

const router = useRouter()

function goToRegister() {
  router.push('/register1')
}

const introSrc = ref('/assets/intro1.png')

onMounted(() => {
  // ── 主题初始化 ──
  const html = document.documentElement
  const saved = localStorage.getItem('sentinel-theme') || 'dark'
  if (saved === 'light') html.classList.add('theme-light')
  else if (saved === 'pink') html.classList.add('theme-pink')

  // ── 主题切换按钮挂载 ──
  const mountEl = document.getElementById('theme-switcher-mount')
  if (mountEl) mountThemeSwitcher(mountEl)

  // ── Tab 切换逻辑 ──
  const tabBtns = document.querySelectorAll('.login-tab-btn')
  const passwordForm = document.getElementById('login-form-password')
  const smsForm = document.getElementById('login-form-sms')

  tabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const tab = btn.getAttribute('data-tab')
      tabBtns.forEach(b => {
        if (b.getAttribute('data-tab') === tab) {
          b.classList.add('active')
          b.style.fontWeight = '700'
        } else {
          b.classList.remove('active')
          b.style.fontWeight = '400'
        }
      })
      if (tab === 'password') {
        passwordForm.classList.remove('hidden')
        smsForm.classList.add('hidden')
      } else {
        passwordForm.classList.add('hidden')
        smsForm.classList.remove('hidden')
      }
    })
  })

  // ── 密码显示/隐藏切换 ──
  const toggleBtn = document.getElementById('toggle-password')
  const passwordIn = document.getElementById('login-password')
  const eyeIcon = document.getElementById('eye-icon')
  if (toggleBtn && passwordIn && eyeIcon) {
    toggleBtn.addEventListener('click', () => {
      const isPwd = passwordIn.type === 'password'
      passwordIn.type = isPwd ? 'text' : 'password'
      eyeIcon.textContent = isPwd ? 'visibility_off' : 'visibility'
    })
  }

  // ── 密码表单提交 ──
  const pForm = document.getElementById('login-form-password')
  const btnTextP = document.getElementById('btn-text-password')
  const spinnerP = document.getElementById('btn-spinner-password')
  const errorP = document.getElementById('login-error-password')

  if (pForm) {
    pForm.addEventListener('submit', async e => {
      e.preventDefault()
      errorP.textContent = ''

      const username = document.getElementById('login-username').value.trim()
      const password = document.getElementById('login-password').value.trim()

      if (!username || !password) {
        errorP.textContent = '请输入用户名和密码'
        return
      }

      btnTextP.classList.add('hidden')
      spinnerP.classList.remove('hidden')
      spinnerP.classList.add('flex')
      document.getElementById('login-button-password').disabled = true

      try {
        const res = await fetch('/auth/login/json', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username, password })
        })
        const data = await res.json()

        if (res.ok && data.access_token) {
          localStorage.setItem('access_token', data.access_token)
          window.location.href = '/#/home'
        } else {
          errorP.textContent = data.message || '登录失败，请检查用户名和密码'
        }
      } catch {
        errorP.textContent = '网络错误，请稍后重试'
      } finally {
        btnTextP.classList.remove('hidden')
        spinnerP.classList.add('hidden')
        spinnerP.classList.remove('flex')
        document.getElementById('login-button-password').disabled = false
      }
    })
  }

  // ── 验证码：发送按钮倒计时 ──
  const sendCodeBtn = document.getElementById('send-code-btn')
  let countdownTimer = null

  function startCountdown(btn) {
    let seconds = 60
    btn.disabled = true
    btn.textContent = '60秒后重发'
    btn.classList.add('opacity-60', 'cursor-not-allowed')
    countdownTimer = setInterval(() => {
      seconds--
      if (seconds <= 0) {
        clearInterval(countdownTimer)
        countdownTimer = null
        btn.disabled = false
        btn.textContent = '获取验证码'
        btn.classList.remove('opacity-60', 'cursor-not-allowed')
      } else {
        btn.textContent = seconds + '秒后重发'
      }
    }, 1000)
  }

  // ── 验证码：发送 ──
  if (sendCodeBtn) {
    sendCodeBtn.addEventListener('click', async () => {
      const phone = document.getElementById('login-phone').value.trim()
      if (!/^1\d{10}$/.test(phone)) {
        document.getElementById('login-error-sms').textContent = '请输入正确的11位手机号'
        return
      }
      document.getElementById('login-error-sms').textContent = ''
      if (countdownTimer) return
      startCountdown(sendCodeBtn)

      try {
        const res = await fetch('/auth/sms/send?phone=' + encodeURIComponent(phone), { method: 'POST' })
        const data = await res.json()
        if (!res.ok) {
          document.getElementById('login-error-sms').textContent = data.detail || '发送失败，请稍后重试'
          clearInterval(countdownTimer)
          countdownTimer = null
          sendCodeBtn.disabled = false
          sendCodeBtn.textContent = '获取验证码'
          sendCodeBtn.classList.remove('opacity-60', 'cursor-not-allowed')
        }
      } catch {
        document.getElementById('login-error-sms').textContent = '网络错误，请稍后重试'
        clearInterval(countdownTimer)
        countdownTimer = null
        sendCodeBtn.disabled = false
        sendCodeBtn.textContent = '获取验证码'
        sendCodeBtn.classList.remove('opacity-60', 'cursor-not-allowed')
      }
    })
  }

  // ── 验证码登录表单提交 ──
  const sForm = document.getElementById('login-form-sms')
  const btnTextS = document.getElementById('btn-text-sms')
  const spinnerS = document.getElementById('btn-spinner-sms')
  const errorS = document.getElementById('login-error-sms')

  if (sForm) {
    sForm.addEventListener('submit', async e => {
      e.preventDefault()
      errorS.textContent = ''

      const phone = document.getElementById('login-phone').value.trim()
      const code = document.getElementById('login-sms-code').value.trim()

      if (!/^1\d{10}$/.test(phone)) {
        errorS.textContent = '请输入正确的11位手机号'
        return
      }
      if (!code || code.length < 4) {
        errorS.textContent = '请输入验证码'
        return
      }

      btnTextS.classList.add('hidden')
      spinnerS.classList.remove('hidden')
      spinnerS.classList.add('flex')
      document.getElementById('login-button-sms').disabled = true

      try {
        const res = await fetch('/auth/sms/verify', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ phone, code })
        })
        const data = await res.json()

        if (res.ok && data.access_token) {
          localStorage.setItem('access_token', data.access_token)
          window.location.href = '/home'
        } else {
          errorS.textContent = data.detail || '验证码错误'
        }
      } catch {
        errorS.textContent = '网络错误，请稍后重试'
      } finally {
        btnTextS.classList.remove('hidden')
        spinnerS.classList.add('hidden')
        spinnerS.classList.remove('flex')
        document.getElementById('login-button-sms').disabled = false
      }
    })
  }

  // ── 左侧图片轮播（每4秒切换一张） ──
  ;(function initIntroSlider() {
    const TOTAL = isDrilling ? 5 : 6
    const PREFIX = isDrilling ? '/assets/drilling/drill-intro' : '/assets/intro'
    const EXT = isDrilling ? '.jpg' : '.png'
    const img = document.querySelector(isDrilling ? '#drillIntroImg' : '#introImg')
    if (!img) return
    let current = 1
    setInterval(() => {
      current = current % TOTAL + 1
      img.src = PREFIX + current + EXT
    }, 4000)
  })()
})
</script>

<style>
.material-symbols-outlined {
  font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
}

body {
  font-family: 'Inter', sans-serif;
  background-color: var(--color-surface);
  color: var(--color-on-surface);
  overflow: hidden;
}

input::selection,
textarea::selection {
  background: transparent;
}

input:focus,
textarea:focus {
  outline: none;
  box-shadow: none;
  -webkit-box-shadow: none;
  background-color: var(--color-surface-container-lowest) !important;
}

.headline-font {
  font-family: 'Space Grotesk', sans-serif;
}

@font-face {
  font-family: 'DingTalk';
  src: url('https://at.alicdn.com/t/webfont_1g7k4ndq89r.woff2') format('woff2');
  font-weight: 400;
  font-display: swap;
}

.dingtalk-font {
  font-family: "DingTalk JinBuTi", "PingFang SC", "Microsoft YaHei", sans-serif !important;
}

.data-grid-overlay {
  background-image: radial-gradient(var(--color-outline-variant) 0.5px, transparent 0.5px);
  background-size: 24px 24px;
  opacity: 0.04;
}

.glass-panel {
  background: white;
}

.theme-switcher-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border-radius: 0.375rem;
  color: var(--color-on-surface);
  opacity: 0.7;
  transition: opacity 0.2s, color 0.2s, background 0.2s;
  cursor: pointer;
}
.theme-switcher-btn:hover {
  opacity: 1;
  color: var(--color-tertiary);
  background: rgba(var(--rgb-tertiary), 0.1);
}

@keyframes orbFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25%       { transform: translate(30px, -20px) scale(1.05); }
  50%       { transform: translate(-20px, 15px) scale(0.95); }
  75%       { transform: translate(15px, 25px) scale(1.02); }
}
@keyframes orbFloat2 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33%       { transform: translate(-25px, 30px) scale(1.08); }
  66%       { transform: translate(20px, -15px) scale(0.92); }
}
.orb-1 { animation: orbFloat 18s ease-in-out infinite; }
.orb-2 { animation: orbFloat2 22s ease-in-out infinite; }

@keyframes floatY {
  0%   { transform: translateY(0px); opacity: 0.5; }
  50%  { transform: translateY(-18px); opacity: 1; }
  100% { transform: translateY(0px); opacity: 0.5; }
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(30px); }
  to   { opacity: 1; transform: translateY(0); }
}
@keyframes fadeInLeft {
  from { opacity: 0; transform: translateX(-30px); }
  to   { opacity: 1; transform: translateX(0); }
}
@keyframes fadeInRight {
  from { opacity: 0; transform: translateX(30px); }
  to   { opacity: 1; transform: translateX(0); }
}
@keyframes scaleIn {
  from { opacity: 0; transform: scale(0.85); }
  to   { opacity: 1; transform: scale(1); }
}
@keyframes shimmer {
  0%   { background-position: -200% center; }
  100% { background-position: 200% center; }
}
@keyframes pulseGlow {
  0%, 100% { box-shadow: 0 0 15px rgba(var(--rgb-primary), 0.15); }
  50%       { box-shadow: 0 0 30px rgba(var(--rgb-primary), 0.35); }
}
@keyframes scanLine {
  0%   { transform: translateY(-100%); opacity: 0; }
  10%  { opacity: 1; }
  90%  { opacity: 1; }
  100% { transform: translateY(400%); opacity: 0; }
}
@keyframes inputGlow {
  0%, 100% { box-shadow: 0 0 0 rgba(var(--rgb-primary), 0); }
  50%       { box-shadow: 0 0 12px rgba(var(--rgb-primary), 0.2); }
}

.float-particle {
  position: absolute;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  pointer-events: none;
  animation: floatY 4s ease-in-out infinite;
}

.anim-fade-up    { animation: fadeInUp 0.6s ease-out both; }
.anim-fade-left  { animation: fadeInLeft 0.7s ease-out both; }
.anim-fade-right { animation: fadeInRight 0.7s ease-out both; }
.anim-scale-in   { animation: scaleIn 0.5s ease-out both; }

.anim-delay-1 { animation-delay: 0.1s; }
.anim-delay-2 { animation-delay: 0.2s; }
.anim-delay-3 { animation-delay: 0.3s; }
.anim-delay-4 { animation-delay: 0.4s; }
.anim-delay-5 { animation-delay: 0.5s; }
.anim-delay-6 { animation-delay: 0.6s; }

.shield-badge {
  animation: pulseGlow 3s ease-in-out infinite;
}

input:-webkit-autofill,
input:-webkit-autofill:hover,
input:-webkit-autofill:focus,
input:-webkit-autofill:active {
  -webkit-box-shadow: 0 0 0 1000px var(--color-surface-container-lowest) inset !important;
  -webkit-text-fill-color: var(--color-on-surface) !important;
  transition: background-color 5000s ease-in-out 0s;
}

.form-field {
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}
.form-field:hover {
  transform: translateY(-1px);
}

.btn-press:active {
  transform: scale(0.97);
}

.shimmer-text {
  background: linear-gradient(
    105deg,
    var(--color-primary) 0%,
    var(--color-tertiary) 40%,
    var(--color-primary) 60%,
    var(--color-primary) 100%
  );
  background-size: 200% auto;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: shimmer 4s linear infinite;
}

.scan-line {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(var(--rgb-primary), 0.4),
    transparent
  );
  animation: scanLine 4s ease-in-out infinite;
  pointer-events: none;
}

.card-border-glow {
  position: relative;
}
.card-border-glow::before {
  content: '';
  position: absolute;
  inset: -1px;
  border-radius: inherit;
  background: linear-gradient(
    135deg,
    rgba(var(--rgb-primary), 0.3),
    transparent 50%,
    rgba(var(--rgb-tertiary), 0.2)
  );
  z-index: -1;
  opacity: 0;
  transition: opacity 0.4s ease;
}
.card-border-glow:hover::before {
  opacity: 1;
}

.nav-link-animated {
  position: relative;
}
.nav-link-animated::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 50%;
  width: 0;
  height: 2px;
  background: var(--color-primary);
  transition: width 0.3s ease, left 0.3s ease;
  border-radius: 1px;
}
.nav-link-animated:hover::after {
  width: 100%;
  left: 0;
}

.particle-d1 { animation-delay: 0s; }
.particle-d2 { animation-delay: 0.8s; }
.particle-d3 { animation-delay: 1.6s; }
.particle-d4 { animation-delay: 2.4s; }
.particle-d5 { animation-delay: 3.2s; }

.corner-deco {
  position: absolute;
  width: 20px;
  height: 20px;
  opacity: 0.25;
}
.corner-tl { top: 8px; left: 8px; border-top: 2px solid var(--color-primary); border-left: 2px solid var(--color-primary); }
.corner-tr { top: 8px; right: 8px; border-top: 2px solid var(--color-primary); border-right: 2px solid var(--color-primary); }
.corner-bl { bottom: 8px; left: 8px; border-bottom: 2px solid var(--color-primary); border-left: 2px solid var(--color-primary); }
.corner-br { bottom: 8px; right: 8px; border-bottom: 2px solid var(--color-primary); border-right: 2px solid var(--color-primary); }

.logo-spin:hover .shield-icon {
  animation: spin 0.6s ease-in-out;
}
@keyframes spin {
  0%   { transform: rotateY(0deg); }
  100% { transform: rotateY(360deg); }
}

@keyframes picLift {
  0%   { top: 10%; transform: scale(0.6); }
  5%   { top: 25%; transform: scale(0.6); }
  10%  { transform: scale(1); }
  20%  { top: 25%; }
  90%  { top: 25%; transform: scale(1); }
  100% { top: -400%; transform: scale(0.2); }
}

.login-root {
  min-height: 100vh;
  overflow: hidden;
  display: flex;
}

.leftPart {
  position: relative;
  overflow: hidden;
  background: #F0F4F9;
  width: 44vw;
  height: 100vh;
}

.leftPart::before,
.leftPart::after {
  content: "";
  position: absolute;
  width: 48px;
  height: 48px;
  pointer-events: none;
  z-index: 5;
}
.leftPart::before {
  top: 28px; left: 28px;
  border-top: 3px solid var(--color-primary);
  border-left: 3px solid var(--color-primary);
  opacity: 0.7;
}
.leftPart::after {
  top: 28px; right: 28px;
  border-top: 3px solid var(--color-primary);
  border-right: 3px solid var(--color-primary);
  opacity: 0.7;
}

.leftPart-corner-bl {
  position: absolute;
  bottom: 28px; left: 28px;
  width: 48px; height: 48px;
  border-bottom: 3px solid var(--color-primary);
  border-left: 3px solid var(--color-primary);
  opacity: 0.7;
  z-index: 5;
}

.leftPart-corner-br {
  position: absolute;
  bottom: 28px; right: 28px;
  width: 48px; height: 48px;
  border-bottom: 3px solid var(--color-primary);
  border-right: 3px solid var(--color-primary);
  opacity: 0.7;
  z-index: 5;
}

.leftPart-ring {
  position: absolute;
  border-radius: 50%;
  border: 1.5px solid rgba(0,200,83,0.18);
  pointer-events: none;
  z-index: 2;
}
.leftPart-ring.pulse {
  animation: ringPulse 4s ease-in-out infinite;
}

@keyframes ringPulse {
  0%, 100% { opacity: 0.5; transform: scale(1); }
  50%       { opacity: 1;   transform: scale(1.04); }
}

.leftPart-divider {
  position: absolute;
  right: 0; top: 8%; bottom: 8%;
  width: 2px;
  background: linear-gradient(to bottom,
    transparent 0%,
    rgba(0,200,83,0.45) 15%,
    rgba(0,200,83,0.45) 85%,
    transparent 100%
  );
  pointer-events: none;
  z-index: 4;
}

.leftPart-hairline {
  position: absolute;
  left: 8%; right: 8%;
  height: 1px;
  background: linear-gradient(to right, transparent, rgba(0,200,83,0.3), transparent);
  pointer-events: none;
  z-index: 3;
}
.leftPart-hairline.top    { top: 90px; }
.leftPart-hairline.bottom { bottom: 90px; }

.imgDiv {
  width: 28vw;
  height: 56vh;
  position: absolute;
  top: 25%;
  left: 18%;
  animation: picLift 4s ease-in-out infinite;
}

.imgDiv img {
  position: absolute;
  border-radius: 40px;
  transition: opacity 0.3s ease;
  width: 100%;
  height: 100%;
  object-fit: cover;
  box-shadow:
    0 2vw 2vw rgba(12, 12, 12, 0.18),
    0 2px 8px rgba(23, 26, 24, 0.1),
    0 0 0 1px rgba(36, 41, 38, 0.06);
}

/* 钻井模式宣传图为竖版完整构图，使用 contain 避免标题被裁切 */
#drillIntroImg {
  object-fit: contain;
  background: #F0F4F9;
}
.imgDiv video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.login-center-wrapper {
  width: 56vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #ffffff;
}

.login-card {
  width: 100%;
  max-width: 520px;
  background-color: #ffffff;
  border-radius: 20px;
  padding: 44px 40px;
  position: relative;
  overflow: hidden;
}

.login-input {
  border: 0.5px solid black;
}
.login-input:focus {
  border-color: #424242 !important;
  outline: none !important;
  box-shadow: none !important;
}
.login-input:focus-visible {
  outline: none !important;
  box-shadow: none !important;
}

/* Login Tab Styles */
.login-tab-line {
  height: 1px;
  width: 24px;
  background: linear-gradient(to right, transparent, var(--color-primary), transparent);
  opacity: 0.5;
}
.login-tab-line-left { margin-right: 8px; }
.login-tab-line-right { margin-left: 8px; }
.login-tab-divider {
  width: 1px;
  height: 16px;
  background: var(--color-outline-variant);
  margin: 0 4px;
}
.login-tab-btn {
  padding: 6px 16px;
  font-size: 0.75rem;
  font-weight: 700;
  border-radius: 0.375rem;
  transition: all 0.2s;
  color: var(--color-on-surface-variant);
  background: transparent;
  border: none;
  cursor: pointer;
}
.login-tab-btn.active {
  font-weight: 700;
}
.login-tab-btn:not(.active):hover {
  background: rgba(var(--rgb-primary), 0.08);
  color: var(--color-primary);
}
#login-button-sms{
  margin-top:52px !important;
}
</style>

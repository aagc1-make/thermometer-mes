import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { initTheme } from './api/theme-switcher.js'

import './styles/_variables.scss'
import './styles/theme.css'
import './styles/login.css'

document.title = '井安智联-钻井多源感知与井喷风险智能预警系统'
initTheme()

createApp(App).use(router).mount('#app')


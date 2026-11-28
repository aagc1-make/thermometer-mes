<template>
  <span class="avatar-voice-input">
    <button type="button" :disabled="busy || !supported || phase === 'finishing'"
      :title="supported ? '通过浏览器识别语音，结果会填入输入框' : '此浏览器不支持语音输入'"
      :aria-pressed="phase !== 'idle'" @click="toggle">
      <i class="el-icon-microphone" aria-hidden="true" />
      {{ phase === 'idle' ? '语音输入' : '结束听写' }}
    </button>
    <span v-if="status" role="status">{{ status }}</span>
  </span>
</template>

<script>
const errors = {
  'not-allowed': '请允许麦克风权限后重试',
  'service-not-allowed': '浏览器的语音识别服务不可用',
  'audio-capture': '没有可用的麦克风',
  'no-speech': '没有听清，请再说一次',
  network: '语音识别连接失败，请重试或输入文字'
}

export default {
  name: 'AvatarVoiceInput',
  props: { busy: Boolean },
  data() {
    return { phase: 'idle', status: '' }
  },
  computed: {
    supported() {
      return window.isSecureContext !== false && !!(window.SpeechRecognition || window.webkitSpeechRecognition)
    }
  },
  watch: {
    busy(value) { if (value) this.cancel() }
  },
  beforeDestroy() { this.cancel() },
  methods: {
    toggle() {
      if (this.busy || this.phase === 'finishing') return
      if (this._recognition) {
        try {
          this.phase = 'finishing'
          this.status = '正在识别'
          this._recognition.stop()
        } catch (error) {
          this.cancel()
        }
        return
      }

      const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition
      if (!Recognition || window.isSecureContext === false) return
      const recognition = new Recognition()
      this._recognition = recognition
      recognition.lang = 'zh-CN'
      recognition.continuous = false
      recognition.interimResults = false
      let transcript = ''
      this.phase = 'starting'
      this.status = '正在打开麦克风'
      recognition.onstart = () => {
        if (this._recognition !== recognition || this.phase !== 'starting') return
        this.phase = 'listening'
        this.status = '正在听，请说话'
      }
      recognition.onresult = event => {
        if (this._recognition !== recognition) return
        transcript = Array.from(event.results)
          .filter(result => result.isFinal)
          .map(result => result[0].transcript.trim())
          .filter(Boolean).join(' ')
      }
      recognition.onerror = event => {
        if (this._recognition !== recognition) return
        this._recognition = null
        this.phase = 'idle'
        this.status = errors[event.error] || '语音识别暂不可用'
      }
      recognition.onend = () => {
        if (this._recognition !== recognition) return
        this._recognition = null
        this.phase = 'idle'
        this.status = transcript ? '' : errors['no-speech']
        if (transcript) this.$emit('transcript', transcript)
      }

      this.$emit('listen')
      try {
        recognition.start()
      } catch (error) {
        this.cancel()
        this.status = '无法启动语音识别，请检查浏览器'
      }
    },
    cancel() {
      const recognition = this._recognition
      this._recognition = null
      this.phase = 'idle'
      this.status = ''
      if (recognition) recognition.abort()
    }
  }
}
</script>

<style lang="scss" scoped>
.avatar-voice-input { display: inline-flex; flex-wrap: wrap; align-items: center; gap: 6px; }
.avatar-voice-input button {
  padding: 4px 8px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: var(--color-card);
  color: var(--color-primary);
  font: inherit;
  cursor: pointer;
}
.avatar-voice-input button[aria-pressed='true'] { background: var(--color-primary-bg); }
.avatar-voice-input button:disabled { opacity: .6; cursor: default; }
.avatar-voice-input button:focus-visible { outline: 2px solid var(--color-primary); outline-offset: 1px; }
</style>

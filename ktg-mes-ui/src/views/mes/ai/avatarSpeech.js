const SPEECH_ENDPOINT = '/mes-avatar/speech'
const MESSAGE_TYPES = ['text', 'knowledge', 'rag', 'toolAnswer']
const MAX_SPOKEN_CHARACTERS = 280
const STREAM_MIME = 'audio/mpeg'
// 起播前先攒 0.2 秒音频，既不用等整段合成，也不会一开口就断。
const PREBUFFER_SECONDS = 0.2

function stockSummary(message) {
  const result = (message.toolResults || []).find(tool => tool.code === 'stock')
  if (!result || result.total !== 1 || !result.rows || result.rows.length !== 1) return ''

  const { itemCode, itemName, balance, unit } = result.rows[0]
  if (!(itemName || itemCode) || balance == null || balance === '') return ''
  const { from, to } = result.arguments || {}
  const period = from && to ? from + '至' + to + '的期末库存' : '当前库存'
  return (itemName || itemCode) + '的' + period + '为 ' + balance + (unit ? ' ' + unit : '') + '。'
}

function cleanSpeechText(text) {
  const paragraph = (text || '').split(/\r?\n\s*\r?\n/)
    .map(part => part
      .split(/\r?\n/)
      .filter(line => !/^\s*(?:\||#{1,6}\s)/.test(line))
      .join(' ')
      .replace(/\[(?:\d+[、,，\s]*)+\]|\x60/g, '')
      .replace(/(\*\*|__)(.+?)\1/g, '$2')
      .replace(/^\s*[-•]\s*/, '')
      .trim())
    .find(Boolean)
  if (!paragraph || paragraph.length <= MAX_SPOKEN_CHARACTERS) return paragraph || ''
  const clipped = paragraph.slice(0, MAX_SPOKEN_CHARACTERS)
  const punctuation = Math.max(clipped.lastIndexOf('。'), clipped.lastIndexOf('！'), clipped.lastIndexOf('？'))
  return punctuation > 0 ? clipped.slice(0, punctuation + 1) : clipped.replace(/[A-Za-z\d.-]+$/, '') + '。'
}

// Use the latest completed turn, including one loaded from chat history.
export function avatarAnswerText(messages) {
  for (let index = messages.length - 1; index >= 0; index--) {
    const message = messages[index]
    if (message.role === 'user') break
    if (message.role !== 'bot' || !MESSAGE_TYPES.includes(message.type)) continue
    if (message.streaming || message.speechIncomplete) return ''
    const text = message.type === 'toolAnswer' ? stockSummary(message) || message.answer : message.text
    if (text) return cleanSpeechText(text)
  }
  return ''
}

export class AvatarSpeech {
  async unlock() {
    if (!this.context) {
      const Context = window.AudioContext || window.webkitAudioContext
      if (!Context) throw new Error('Web Audio is unavailable')
      this.context = new Context()
      this.analyser = this.context.createAnalyser()
      this.analyser.fftSize = 256
      this.samples = new Uint8Array(this.analyser.fftSize)
      this.analyser.connect(this.context.destination)
    }
    if (this.context.state !== 'running') await this.context.resume()
  }

  // 呼出数字人时先热一条到语音服务的连接，第一条回答就不用付握手成本。
  // 请求体只有一个字，音频直接丢掉；失败无所谓，朗读时会自然重试。
  warm() {
    if (this.warming || typeof window.fetch !== 'function') return
    this.warming = true
    fetch(SPEECH_ENDPOINT, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text: '好' })
    })
      .then(response => response.arrayBuffer())
      .catch(() => {})
      .then(() => { this.warming = false })
  }

  async play(text, setState) {
    this.stop()
    const request = new AbortController()
    this.request = request
    setState('loading')
    this.armTimeout(request, setState)
    if (!this.context) {
      try {
        await this.unlock()
      } catch (error) {
        if (this.request !== request) return
        this.stop()
        setState('error')
        return
      }
    } else if (this.context.state !== 'running') {
      // 已经在呼出数字人时解锁过；这里只做补充唤醒，不阻塞首包播放。
      this.context.resume().catch(() => {})
    }

    // 边合成边播：首包到就开口，不必等整段音频生成完。
    // 若浏览器不支持音频流式播放（或流式路径出错），退回整段解码。
    if (this.canStream()) {
      try {
        await this.playStream(text, request, setState)
        return
      } catch (error) {
        if (this.request !== request) return
        this.streamDisabled = true
        this.releasePlayback()
        setState('loading')
        this.armTimeout(request, setState)
      }
    }

    try {
      await this.playBuffered(text, request, setState)
    } catch (error) {
      if (this.request !== request) return
      this.stop()
      setState('error')
    }
  }

  armTimeout(request, setState) {
    window.clearTimeout(this.timeout)
    this.timeout = window.setTimeout(() => {
      if (this.request !== request) return
      this.stop()
      setState('error')
    }, 30000)
  }

  canStream() {
    return !this.streamDisabled &&
      typeof window.MediaSource === 'function' &&
      typeof window.MediaSource.isTypeSupported === 'function' &&
      window.MediaSource.isTypeSupported(STREAM_MIME) &&
      typeof window.ReadableStream === 'function'
  }

  async playStream(text, request, setState) {
    const current = () => this.request === request
    const mediaSource = new window.MediaSource()
    const audio = new Audio()
    const objectUrl = URL.createObjectURL(mediaSource)
    audio.preload = 'auto'
    audio.src = objectUrl
    this.audio = audio
    this.objectUrl = objectUrl
    this.mediaSource = mediaSource
    this.elementNode = this.context.createMediaElementSource(audio)
    this.elementNode.connect(this.analyser)

    const queue = []
    let sourceBuffer = null
    let drained = null
    let started = false

    const flushDrained = () => {
      if (drained && sourceBuffer && !sourceBuffer.updating && !queue.length) {
        const resolve = drained
        drained = null
        resolve()
      }
    }
    const feed = () => {
      if (!sourceBuffer || sourceBuffer.updating || !queue.length) {
        flushDrained()
        return
      }
      sourceBuffer.appendBuffer(queue.shift())
    }
    const tryStart = () => {
      if (started || !current() || !audio.buffered.length) return
      if (audio.buffered.end(audio.buffered.length - 1) - audio.currentTime < PREBUFFER_SECONDS) return
      started = true
      audio.play().catch(() => {
        if (!current()) return
        this.stop()
        setState('error')
      })
    }

    audio.addEventListener('playing', () => {
      if (!current()) return
      window.clearTimeout(this.timeout)
      setState('playing')
    }, { once: true })

    await Promise.race([
      new Promise(resolve => mediaSource.addEventListener('sourceopen', resolve, { once: true })),
      new Promise((resolve, reject) => {
        window.setTimeout(() => reject(new Error('MediaSource did not open')), 5000)
      })
    ])
    if (!current()) return
    sourceBuffer = mediaSource.addSourceBuffer(STREAM_MIME)
    sourceBuffer.addEventListener('updateend', () => {
      tryStart()
      feed()
    })
    sourceBuffer.addEventListener('error', () => {
      if (current()) {
        this.stop()
        setState('error')
      }
    })

    const response = await fetch(SPEECH_ENDPOINT, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text }),
      signal: request.signal
    })
    if (!current()) return
    if (!response.ok || !(response.headers.get('Content-Type') || '').startsWith('audio/') || !response.body) {
      throw new Error('Speech service did not return audio')
    }

    const reader = response.body.getReader()
    for (;;) {
      const { done, value } = await reader.read()
      if (done) break
      if (!current()) {
        reader.cancel().catch(() => {})
        return
      }
      if (value && value.length) {
        queue.push(value)
        feed()
        tryStart()
      }
    }
    if (!current()) return

    await new Promise(resolve => {
      drained = resolve
      feed()
      flushDrained()
    })
    if (!current()) return
    if (mediaSource.readyState === 'open') mediaSource.endOfStream()

    await new Promise((resolve, reject) => {
      audio.addEventListener('ended', resolve, { once: true })
      audio.addEventListener('error', () => {
        if (current()) reject(new Error('Audio playback failed'))
      })
    })
    if (!current()) return
    this.stop()
    setState('idle')
  }

  async playBuffered(text, request, setState) {
    const current = () => this.request === request
    // One silent retry: the local voice service may still be booting after a project restart.
    for (let attempt = 0; ; attempt++) {
      try {
        const [, response] = await Promise.all([
          this.unlock(),
          fetch(SPEECH_ENDPOINT, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ text }),
            signal: request.signal
          })
        ])
        if (!current()) return
        if (!response.ok || !(response.headers.get('Content-Type') || '').startsWith('audio/')) {
          throw new Error('Speech service did not return audio')
        }
        const audio = await response.arrayBuffer()
        if (!current()) return
        const decoded = await this.context.decodeAudioData(audio)
        if (!current()) return

        const source = this.context.createBufferSource()
        source.buffer = decoded
        source.connect(this.analyser)
        source.onended = () => {
          if (!current()) return
          this.stop()
          setState('idle')
        }
        source.start()
        this.source = source
        window.clearTimeout(this.timeout)
        setState('playing')
        return
      } catch (error) {
        if (!current() || attempt >= 1) throw error
        await new Promise(resolve => window.setTimeout(resolve, 1200))
        if (!current()) return
      }
    }
  }

  mouthLevel() {
    if (!this.analyser) return 0
    this.analyser.getByteTimeDomainData(this.samples)
    let energy = 0
    for (const sample of this.samples) {
      const amplitude = (sample - 128) / 128
      energy += amplitude * amplitude
    }
    return Math.min(1, Math.max(0, Math.sqrt(energy / this.samples.length) * 6 - 0.05))
  }

  stop() {
    window.clearTimeout(this.timeout)
    const request = this.request
    this.request = null
    if (request) request.abort()
    this.releasePlayback()
  }

  releasePlayback() {
    if (this.source) {
      this.source.onended = null
      try { this.source.stop() } catch (error) { /* already stopped */ }
      try { this.source.disconnect() } catch (error) { /* already detached */ }
      this.source = null
    }
    if (this.elementNode) {
      try { this.elementNode.disconnect() } catch (error) { /* already detached */ }
      this.elementNode = null
    }
    if (this.audio) {
      const audio = this.audio
      this.audio = null
      try { audio.pause() } catch (error) { /* not started */ }
      try {
        audio.removeAttribute('src')
        audio.load()
      } catch (error) { /* already detached */ }
    }
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl)
      this.objectUrl = null
    }
    this.mediaSource = null
  }

  dispose() {
    this.stop()
    if (this.context) this.context.close().catch(() => {})
    this.context = null
    this.analyser = null
  }
}

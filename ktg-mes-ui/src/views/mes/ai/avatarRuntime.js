export const AVATAR_ASSETS = (process.env.BASE_URL || '/') + 'mes-avatar-assets/'

const scripts = new Map()

function loadScript(file) {
  if (scripts.has(file)) return scripts.get(file)
  const script = document.createElement('script')
  script.src = AVATAR_ASSETS + 'vendor/' + file

  const promise = new Promise((resolve, reject) => {
    const timeout = window.setTimeout(fail, 10000)
    function fail() {
      window.clearTimeout(timeout)
      script.onload = null
      script.onerror = null
      scripts.delete(file)
      script.remove()
      reject(new Error('Missing avatar runtime: ' + file))
    }
    script.onerror = fail
    script.onload = () => {
      window.clearTimeout(timeout)
      resolve()
    }
    document.head.appendChild(script)
  })
  scripts.set(file, promise)
  return promise
}

export async function loadAvatarRuntime() {
  if (!window.Live2DCubismCore) await loadScript('live2dcubismcore.min.js')
  if (!window.PIXI) await loadScript('pixi.min.js')
  if (!window.PIXI || !window.PIXI.live2d) await loadScript('cubism4.min.js')
  const { PIXI } = window
  if (!PIXI || !PIXI.live2d || !PIXI.live2d.Live2DModel) throw new Error('Live2D runtime unavailable')
  PIXI.live2d.Live2DModel.registerTicker(PIXI.Ticker)
  return PIXI
}

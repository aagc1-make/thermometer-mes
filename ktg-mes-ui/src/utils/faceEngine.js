/**
 * face-api.js 的懒加载封装。
 *
 * 设计取舍：
 * 1. 库和模型都放在 public/face-api 下，不走 CDN，断网也能演示；
 * 2. 首次打开采集窗口才加载（约 6.8MB 模型），登录页不背这个体积；
 * 3. 模型只在本机浏览器运行，照片和视频帧不出浏览器，只把 128 维特征发给后端。
 */

const BASE = (process.env.BASE_URL || '/').replace(/\/?$/, '/')
export const LIB_URL = `${BASE}face-api/face-api.min.js`
export const MODEL_URL = `${BASE}face-api/models`

let scriptPromise = null
let enginePromise = null

function loadScript() {
  if (window.faceapi) return Promise.resolve(window.faceapi)
  if (!scriptPromise) {
    scriptPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = LIB_URL
      script.async = true
      script.onload = () => {
        if (window.faceapi) resolve(window.faceapi)
        else reject(new Error('人脸识别库未能初始化'))
      }
      script.onerror = () => reject(new Error('人脸识别库加载失败，请检查网络后重试'))
      document.head.appendChild(script)
    }).catch(error => {
      scriptPromise = null
      throw error
    })
  }
  return scriptPromise
}

/** 加载库与三个模型：检测、68 点关键点、128 维特征 */
export function loadFaceEngine() {
  if (!enginePromise) {
    enginePromise = loadScript()
      .then(api => Promise.all([
        api.nets.tinyFaceDetector.loadFromUri(MODEL_URL),
        api.nets.faceLandmark68Net.loadFromUri(MODEL_URL),
        api.nets.faceRecognitionNet.loadFromUri(MODEL_URL)
      ]).then(() => api))
      .catch(error => {
        enginePromise = null
        throw new Error('人脸模型加载失败：' + (error && error.message ? error.message : '浏览器不支持或资源缺失'))
      })
  }
  return enginePromise
}

/** 小模型检测器：320 输入兼顾速度与精度，适合登录场景 */
export function createDetector(api) {
  return new api.TinyFaceDetectorOptions({ inputSize: 320, scoreThreshold: 0.5 })
}

/** 对当前视频帧做一次检测；withDescriptor 为 true 时同时计算 128 维特征 */
export function detectOnce(api, video, withDescriptor) {
  const task = api.detectSingleFace(video, createDetector(api)).withFaceLandmarks()
  return withDescriptor ? task.withFaceDescriptor() : task
}

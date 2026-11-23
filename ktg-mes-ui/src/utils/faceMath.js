/**
 * 人脸登录的纯计算逻辑：特征校验、1:1 距离、多帧平均、眨眼/转头活体判定。
 *
 * 这里不依赖 DOM、摄像头或 Vue，便于在 Node 里直接做单元测试。
 * 判定阈值与后端 mes.face.threshold 保持一致；改阈值时两边一起改。
 */

export const FACE_DIMENSION = 128
export const FACE_THRESHOLD = 0.55

/** 眼睛开合阈值（EAR = 上下眼睑距离 / 眼睛宽度） */
export const EYE_OPEN_RATIO = 0.23
export const EYE_CLOSED_RATIO = 0.17

/** 鼻尖相对人脸框的水平位移，超过该比例视为转头 */
export const HEAD_TURN_RATIO = 0.12

/** 活体窗口：只保留最近约 2 秒的鼻尖位置，避免慢速漂移误判 */
export const LIVENESS_WINDOW = 20

/** 128 维特征是否可用于比对：维度正确、有限、非常量、数值范围合理 */
export function isValidDescriptor(values) {
  if (!Array.isArray(values) || values.length !== FACE_DIMENSION) return false
  let min = Infinity
  let max = -Infinity
  let sum = 0
  let sumSquare = 0
  for (let i = 0; i < values.length; i++) {
    const value = values[i]
    if (typeof value !== 'number' || !Number.isFinite(value) || Math.abs(value) > 100) return false
    min = Math.min(min, value)
    max = Math.max(max, value)
    sum += value
    sumSquare += value * value
  }
  const mean = sum / FACE_DIMENSION
  const variance = sumSquare / FACE_DIMENSION - mean * mean
  return max > min && variance > 1e-6
}

/** 欧氏距离；不同人的距离通常显著大于 0.55 */
export function euclideanDistance(left, right) {
  if (!isValidDescriptor(left) || !isValidDescriptor(right)) {
    throw new Error('人脸特征维度或数值不合法')
  }
  let sum = 0
  for (let i = 0; i < FACE_DIMENSION; i++) {
    const diff = left[i] - right[i]
    sum += diff * diff
  }
  return Math.sqrt(sum)
}

/** 多帧平均，降低单帧抖动；录入时使用 */
export function averageDescriptors(list) {
  if (!Array.isArray(list) || !list.length) return null
  const valid = list.filter(isValidDescriptor)
  if (!valid.length) return null
  const average = new Array(FACE_DIMENSION).fill(0)
  valid.forEach(values => {
    for (let i = 0; i < FACE_DIMENSION; i++) average[i] += values[i]
  })
  for (let i = 0; i < FACE_DIMENSION; i++) average[i] /= valid.length
  return average
}

/** 单只眼睛的宽高比：points 为 68 点中的 6 个眼睑点 */
export function eyeAspectRatio(points) {
  if (!Array.isArray(points) || points.length < 6) return 0
  const distance = (a, b) => Math.hypot(a.x - b.x, a.y - b.y)
  const vertical = distance(points[1], points[5]) + distance(points[2], points[4])
  const horizontal = 2 * distance(points[0], points[3])
  return horizontal === 0 ? 0 : vertical / horizontal
}

/** 68 点 landmark 的双眼平均开合度 */
export function landmarkEar(positions) {
  if (!Array.isArray(positions) || positions.length < 68) return 0
  const left = eyeAspectRatio(positions.slice(36, 42))
  const right = eyeAspectRatio(positions.slice(42, 48))
  return (left + right) / 2
}

/** 鼻尖在人脸框内的相对位置，用于识别左右转头 */
export function noseRatio(box, positions) {
  if (!box || !positions || positions.length < 48) return NaN
  const nose = positions[30]
  if (!nose || !box.width) return NaN
  return (nose.x - box.x) / box.width
}

/**
 * 活体状态机：满足"眨一次眼"或"左右转头"任一条件即通过。
 *
 * 传入每一帧的 ear 与 nose，返回 { passed, blinked, turned, progress, state }。
 * 静默照片不会眨眼、也不会产生足够横向位移，因此无法通过。
 */
export function createLivenessTracker(options = {}) {
  const open = options.open || EYE_OPEN_RATIO
  const closed = options.closed || EYE_CLOSED_RATIO
  const turn = options.turn || HEAD_TURN_RATIO
  const windowSize = options.windowSize || LIVENESS_WINDOW
  const minClosedFrames = options.minClosedFrames || 1

  let state = 'waiting'
  let closedFrames = 0
  let blinked = false
  let turned = false
  const noseWindow = []

  const computeProgress = () => {
    if (blinked || turned) return 0.85
    if (state === 'closed') return 0.55
    if (state === 'open') return 0.3
    return 0.1
  }

  return {
    push(frame = {}) {
      const ear = frame.ear
      if (Number.isFinite(ear) && ear > 0) {
        if (state === 'waiting') {
          if (ear >= open) state = 'open'
        } else if (state === 'open') {
          if (ear <= closed) {
            state = 'closed'
            closedFrames = 1
          }
        } else if (state === 'closed') {
          if (ear <= closed) {
            closedFrames += 1
          } else if (ear >= open) {
            if (closedFrames >= minClosedFrames) {
              blinked = true
              state = 'done'
            } else {
              state = 'open'
            }
          }
        }
      }
      const nose = frame.nose
      if (Number.isFinite(nose)) {
        noseWindow.push(nose)
        if (noseWindow.length > windowSize) noseWindow.shift()
        if (noseWindow.length >= 4 && Math.max.apply(null, noseWindow) - Math.min.apply(null, noseWindow) >= turn) {
          turned = true
        }
      }
      return {
        passed: blinked || turned,
        blinked,
        turned,
        progress: computeProgress(),
        state
      }
    },
    reset() {
      state = 'waiting'
      closedFrames = 0
      blinked = false
      turned = false
      noseWindow.length = 0
    }
  }
}

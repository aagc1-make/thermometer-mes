/**
 * 人脸登录的纯计算逻辑：特征校验、1:1 距离、多帧平均、左右转头活体判定。
 *
 * 这里不依赖 DOM、摄像头或 Vue，便于在 Node 里直接做单元测试。
 * 判定阈值与后端 mes.face.threshold 保持一致；改阈值时两边一起改。
 */

export const FACE_DIMENSION = 128
export const FACE_THRESHOLD = 0.55

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

/** 鼻尖在人脸框内的相对位置，用于识别左右转头 */
export function noseRatio(box, positions) {
  if (!box || !positions || positions.length < 48) return NaN
  const nose = positions[30]
  if (!nose || !box.width) return NaN
  return (nose.x - box.x) / box.width
}

/** 活体动作：向左转头、向右转头。 */
export const LIVENESS_ACTIONS = ['turn-left', 'turn-right']

/** 验证按指定顺序完成的左右转头，每步都要转到位并回正。 */
export function createActiveLivenessTracker({ order, baselineNose } = {}) {
  if (!Array.isArray(order) || order.length < 2
    || order.some(action => LIVENESS_ACTIONS.indexOf(action) < 0)) {
    throw new Error('活体动作需由左右转头组成')
  }
  if (!Number.isFinite(baselineNose)) {
    throw new Error('活体检测缺少正脸基准')
  }

  const actions = order.slice()
  const turnDistance = 0.09
  const centerDistance = 0.07
  const actionTimeout = { 'turn-left': 6000, 'turn-right': 6000 }
  const minimumDuration = 100
  let step = 0
  let phase = 'action'
  let neutralSeen = false
  let actionStartedAt = null
  let movementAt = null
  let lastFrameAt = null

  const state = () => ({
    action: phase === 'done' || phase === 'expired' ? null : actions[step],
    phase,
    step,
    total: actions.length,
    passed: phase === 'done'
  })

  return {
    push({ nose, at } = {}) {
      if (phase === 'done' || phase === 'expired') return state()
      if (!Number.isFinite(at) || (lastFrameAt !== null && at <= lastFrameAt)) return state()
      lastFrameAt = at
      if (actionStartedAt === null) actionStartedAt = at
      if (at - actionStartedAt >= actionTimeout[actions[step]]) {
        phase = 'expired'
        return state()
      }
      if (!Number.isFinite(nose)) return state()

      const action = actions[step]
      // 带符号位移：用户向左转头时，画面中鼻尖偏向图像右侧，偏移为正
      const offset = nose - baselineNose
      const centered = Math.abs(offset) <= centerDistance

      if (!neutralSeen) {
        neutralSeen = centered
        return state()
      }

      if (phase === 'action') {
        const moved = action === 'turn-left' ? offset >= turnDistance : offset <= -turnDistance
        if (moved) {
          phase = 'return'
          movementAt = at
        }
        return state()
      }

      if (!centered) return state()
      if (at - movementAt < minimumDuration) {
        phase = 'action'
        neutralSeen = centered
        movementAt = null
        return state()
      }

      step += 1
      phase = step === actions.length ? 'done' : 'action'
      neutralSeen = false
      actionStartedAt = at
      movementAt = null
      return state()
    }
  }
}

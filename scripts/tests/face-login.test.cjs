const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')

function engine() {
  const file = 'ktg-mes-ui/src/utils/faceMath.js'
  assert.ok(fs.existsSync(file), '需要实现人脸特征与活体算法模块')
  const source = fs.readFileSync(file, 'utf8').replace(/export (const|function) /g, '$1 ')
  const box = { exports: {}, console }
  vm.createContext(box)
  vm.runInContext(source + '\nexports={FACE_DIMENSION,FACE_THRESHOLD,EYE_OPEN_RATIO,EYE_CLOSED_RATIO,isValidDescriptor,euclideanDistance,averageDescriptors,eyeAspectRatio,landmarkEar,noseRatio,createLivenessTracker};', box)
  return box.exports
}

function descriptor(seed = 1) {
  const values = []
  for (let i = 0; i < 128; i++) values.push(Math.sin(seed + i * 0.37) * 0.3)
  return values
}

function eye(open) {
  const dy = open ? 1 : 0.1
  return [
    { x: 0, y: 0 }, { x: 1, y: dy }, { x: 2, y: dy },
    { x: 3, y: 0 }, { x: 2, y: -dy }, { x: 1, y: -dy }
  ]
}

function landmarks(earOpen) {
  const points = []
  for (let i = 0; i < 68; i++) points.push({ x: i, y: i % 7 })
  const eyePoints = eye(earOpen)
  for (let i = 0; i < 6; i++) {
    points[36 + i] = eyePoints[i]
    points[42 + i] = { x: eyePoints[i].x + 10, y: eyePoints[i].y }
  }
  points[30] = { x: 60, y: 50 }
  return points
}

test('特征校验拒绝错误维度、NaN 与常量向量', () => {
  const api = engine()
  assert.equal(api.FACE_DIMENSION, 128)
  assert.equal(api.isValidDescriptor(descriptor()), true)
  assert.equal(api.isValidDescriptor(descriptor().slice(0, 64)), false)
  assert.equal(api.isValidDescriptor(new Array(128).fill(0)), false)
  const withNaN = descriptor()
  withNaN[7] = NaN
  assert.equal(api.isValidDescriptor(withNaN), false)
  assert.equal(api.isValidDescriptor('face'), false)
})

test('同一人距离远小于阈值，不同人距离超过阈值', () => {
  const api = engine()
  const enrolled = descriptor(3.1)
  assert.equal(api.euclideanDistance(enrolled, enrolled), 0)
  const samePerson = enrolled.map(value => value + 0.01)
  assert.ok(api.euclideanDistance(enrolled, samePerson) < api.FACE_THRESHOLD)
  assert.ok(api.euclideanDistance(enrolled, descriptor(9.7)) > api.FACE_THRESHOLD)
})

test('多帧平均用于降低单帧抖动', () => {
  const api = engine()
  const first = descriptor(0.5)
  const second = descriptor(0.9)
  const average = api.averageDescriptors([first, second])
  assert.equal(average.length, 128)
  assert.ok(Math.abs(average[0] - (first[0] + second[0]) / 2) < 1e-12)
  assert.equal(api.averageDescriptors([first, [1, 2, 3]]).length, 128)
  assert.equal(api.averageDescriptors([]), null)
})

test('EAR 能区分睁眼与闭眼', () => {
  const api = engine()
  assert.ok(api.eyeAspectRatio(eye(true)) > api.EYE_OPEN_RATIO)
  assert.ok(api.eyeAspectRatio(eye(false)) < api.EYE_CLOSED_RATIO)
  assert.ok(api.landmarkEar(landmarks(true)) > api.EYE_OPEN_RATIO)
  assert.ok(api.landmarkEar(landmarks(false)) < api.EYE_CLOSED_RATIO)
})

test('眨一次眼即通过活体，静止睁眼不通过', () => {
  const api = engine()
  const tracker = api.createLivenessTracker()
  const staticFace = tracker.push({ ear: 0.3 })
  for (let i = 0; i < 12; i++) tracker.push({ ear: 0.3 })
  assert.equal(staticFace.passed, false)
  assert.equal(tracker.push({ ear: 0.3 }).passed, false)

  tracker.push({ ear: 0.1 })
  const blinked = tracker.push({ ear: 0.3 })
  assert.equal(blinked.passed, true)
  assert.equal(blinked.blinked, true)
})

test('左右转头同样可以通过活体', () => {
  const api = engine()
  const tracker = api.createLivenessTracker()
  tracker.push({ ear: 0.3, nose: 0.42 })
  tracker.push({ ear: 0.3, nose: 0.44 })
  tracker.push({ ear: 0.3, nose: 0.52 })
  const turned = tracker.push({ ear: 0.3, nose: 0.56 })
  assert.equal(turned.passed, true)
  assert.equal(turned.turned, true)
})

test('活体状态可重置，鼻尖位置按人脸框归一化', () => {
  const api = engine()
  const tracker = api.createLivenessTracker()
  tracker.push({ ear: 0.3 })
  tracker.push({ ear: 0.1 })
  assert.equal(tracker.push({ ear: 0.3 }).passed, true)
  tracker.reset()
  assert.equal(tracker.push({ ear: 0.3 }).passed, false)
  assert.equal(api.noseRatio({ x: 10, width: 100 }, landmarks(true)), 0.5)
})

test('前端阈值与后端配置保持一致', () => {
  const api = engine()
  const yml = fs.readFileSync('backend/ktg-mes/ktg-admin/src/main/resources/application.yml', 'utf8')
  const matched = yml.match(/face:[\s\S]*?threshold:\s*([\d.]+)/)
  assert.ok(matched, 'application.yml 需要配置 mes.face.threshold')
  assert.equal(Number(matched[1]), api.FACE_THRESHOLD)
  assert.ok(fs.existsSync('sql/face_login.sql'), '需要人脸模板建表脚本')
  const ddl = fs.readFileSync('sql/face_login.sql', 'utf8')
  assert.match(ddl, /sys_user_face/)
  assert.match(ddl, /descriptor/)
  assert.doesNotMatch(ddl, /photo|image|avatar/i, '只保存特征，不允许出现照片字段')
})

test('登录页、个人中心与接口都已接入人脸流程', () => {
  const login = fs.readFileSync('ktg-mes-ui/src/views/login.vue', 'utf8')
  assert.match(login, /face-capture/)
  assert.match(login, /startFaceLogin/)
  assert.match(login, /handleFaceCaptured/)
  const profile = fs.readFileSync('ktg-mes-ui/src/views/system/user/profile/faceLogin.vue', 'utf8')
  assert.match(profile, /enrollFace/)
  assert.match(profile, /agree/)
  const api = fs.readFileSync('ktg-mes-ui/src/api/login.js', 'utf8')
  assert.match(api, /\/faceLogin/)
  assert.match(api, /descriptor/)
})

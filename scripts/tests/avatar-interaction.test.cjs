const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')

const directory = 'ktg-mes-ui/src/views/mes/ai/'

function loadComponent(name, bindings = {}) {
  const path = directory + name + '.vue'
  if (!fs.existsSync(path)) return {}
  const source = fs.readFileSync(path, 'utf8').match(/<script>([\s\S]*?)<\/script>/)[1]
  return vm.runInNewContext(source.replace(/^import .*$/mg, '').replace('export default', 'component ='), {
    component: null,
    ...bindings
  })
}

test('米粒区分头部和身体的命中，并且点击背景时不播放动作', async () => {
  const stage = loadComponent('DigitalHumanStage')
  let hits = []
  const motions = []
  const model = {
    hitTest(x, y) {
      assert.deepEqual([x, y], [30, 40])
      return hits
    },
    motion(group, index, priority, options) {
      motions.push([group, index, priority, options && options.expression])
      return Promise.resolve(true)
    }
  }
  const ctx = { ...stage.methods, _model: model, $refs: { canvas: { getBoundingClientRect: () => ({ left: 10, top: 20 }) } } }
  const tap = { clientX: 40, clientY: 60 }
  await ctx.tapModel(tap)
  assert.deepEqual(motions, [])
  hits = ['HitAreaHead', 'HitAreaBody']
  await ctx.tapModel(tap)
  hits = ['HitAreaBody']
  await ctx.tapModel(tap)
  assert.deepEqual(motions, [['', 1, 2, 'exp_04'], ['', 2, 2, undefined]])
})

function voice(Recognition) {
  const events = []
  const component = loadComponent('AvatarVoiceInput', { window: { SpeechRecognition: Recognition } })
  const ctx = {
    ...component.data(),
    ...component.methods,
    $emit: (...args) => events.push(args)
  }
  return { component, ctx, events }
}

class FakeRecognition {
  start() { this.onstart() }
  stop() { this.stopped = true }
  abort() { this.aborted = true }
  result(text) {
    this.onresult({ results: [{ 0: { transcript: text }, isFinal: true }] })
  }
}

test('听写先打断朗读，最终结果只写入草稿一次', () => {
  const { ctx, events } = voice(FakeRecognition)
  ctx.toggle()
  const recognition = ctx._recognition
  assert.equal(recognition.lang, 'zh-CN')
  assert.equal(ctx.phase, 'listening')
  assert.deepEqual(events, [['listen']])
  ctx.toggle()
  assert.equal(recognition.stopped, true)
  recognition.result('  查询库存  ')
  recognition.onend()
  assert.equal(ctx.phase, 'idle')
  assert.deepEqual(events, [['listen'], ['transcript', '查询库存']])
})

test('离开数字人后结束听写，迟到的结果不会写入新对话', () => {
  const { component, ctx, events } = voice(FakeRecognition)
  ctx.toggle()
  const recognition = ctx._recognition
  component.beforeDestroy.call(ctx)
  recognition.result('这条不应进入新对话')
  recognition.onend()
  assert.equal(recognition.aborted, true)
  assert.deepEqual(events, [['listen']])
})

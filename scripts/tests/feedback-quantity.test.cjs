const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')

function component() {
  const source = fs.readFileSync('ktg-mes-ui/src/views/mes/pro/feedback/index.vue', 'utf8')
  const code = source.match(/<script>([\s\S]*?)<\/script>/)[1]
    .replace(/^import .*$/mg, '').replace('export default', 'component =')
  const box = { component: null, pipelineWorkorderContext: {}, WorkorderSelect: {}, WorkstationSelect: {}, UserSingleSelect: {}, ProtaskSelect: {}, ProductProduce: {}, ItemConsume: {} }
  vm.createContext(box)
  vm.runInContext(code, box)
  const c = { ...box.component.data(), ...box.component.methods }
  c.form = { isCheck: 'N', quantityFeedback: 1110, quantityQualified: 0, quantityUnquanlified: 1110 }
  return { c, source }
}

test('免检报工数量可清空和重新输入，并保留已有不良品数量', () => {
  const { c, source } = component()
  assert.equal(source.includes('<el-input readonly="readonly" v-model="form.quantityFeedback"'), false)
  c.handleFeedbackQuantityInput('')
  assert.equal(c.form.quantityFeedback, '')
  assert.equal(c.form.quantityUnquanlified, 1110)
  c.handleFeedbackQuantityInput('1200')
  assert.equal(c.form.quantityFeedback, '1200')
  assert.equal(c.form.quantityQualified, 90)
  assert.equal(c.form.quantityUnquanlified, 1110)
})

test('报工数量小于不良品数量时明确阻止提交，调整后可通过', () => {
  const { c } = component()
  c.handleFeedbackQuantityInput('10')
  let error
  c.validateFeedbackQuantity({}, c.form.quantityFeedback, value => { error = value })
  assert.match(error.message, /不良品数量/)
  assert.equal(c.form.quantityUnquanlified, 1110)
  c.form.quantityUnquanlified = 2
  c.handleFeedbackQuantityInput('10')
  c.validateFeedbackQuantity({}, c.form.quantityFeedback, value => { error = value })
  assert.equal(error, undefined)
  assert.equal(c.form.quantityQualified, 8)
})

test('修改合格品和不良品数量继续同步总数，处理空值和小数', () => {
  const { c } = component()
  c.form.quantityQualified = 0.1; c.form.quantityUnquanlified = 0.2
  c.handleQuantityChanged()
  assert.equal(c.form.quantityFeedback, 0.3)
  c.form.quantityQualified = undefined
  c.handleQuantityChanged()
  assert.equal(c.form.quantityFeedback, 0.2)
  c.form.quantityQualified = 8
  c.form.quantityLaborScrap = 1; c.form.quantityMaterialScrap = 2; c.form.quantityOtherScrap = 0
  c.handleScrapChanged()
  assert.equal(c.form.quantityUnquanlified, 3)
  assert.equal(c.form.quantityFeedback, 11)
})

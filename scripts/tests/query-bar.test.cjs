const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')

/**
 * 列表页统一查询区 QueryBar：二级条件直接由 more 插槽里的 el-form-item 推导，
 * 页面不再重复声明一遍字段；已填写的内容要能回显成 chip 并可单条/整体清除。
 */
function createBar(model, advancedItems) {
  const source = fs.readFileSync('ktg-mes-ui/src/components/QueryBar/index.vue', 'utf8')
  const code = source.match(/<script>([\s\S]*?)<\/script>/)[1]
    .replace(/^import .*$/mg, '').replace('export default', 'component =')
  const box = { component: null }
  vm.createContext(box)
  vm.runInContext(code, box)

  const item = (prop, label, options) => ({
    componentOptions: {
      propsData: { prop, label },
      children: options ? [{ componentOptions: { children: options.map(([value, text]) => ({ componentOptions: { propsData: { value, label: text } } })) } }] : []
    }
  })
  const ctx = Object.assign({}, box.component.data(), box.component.methods)
  ctx.model = model
  ctx.$slots = { more: advancedItems.map(([prop, label, options]) => item(prop, label, options)) }
  ctx.events = []
  ctx.$emit = name => ctx.events.push(name)
  ctx.$set = (target, key, value) => { target[key] = value }
  Object.keys(box.component.computed).forEach(key => {
    Object.defineProperty(ctx, key, { get: () => box.component.computed[key].call(ctx), configurable: true })
  })
  return ctx
}

test('二级条件从插槽里的 el-form-item 推导出字段与标签', () => {
  const bar = createBar({ clientCode: null, sourceCode: null }, [['clientCode', '客户编码'], ['sourceCode', '来源单据编号']])
  assert.deepEqual([...bar.advancedFields.map(f => f.prop)], ['clientCode', 'sourceCode'])
  assert.deepEqual([...bar.advancedFields.map(f => f.label)], ['客户编码', '来源单据编号'])
})

test('没有二级条件时不渲染更多筛选', () => {
  const bar = createBar({ a: null }, [])
  assert.equal(bar.advancedFields.length, 0)
  assert.equal(bar.activeCount, 0)
})

test('只有填了值的二级条件回显 chip', () => {
  const bar = createBar({ clientCode: '华为', sourceCode: '', itemCode: null, unitName: '台' },
    [['clientCode', '客户编码'], ['sourceCode', '来源单据编号'], ['itemCode', '物料编码'], ['unitName', '单位']])
  assert.equal(bar.activeCount, 2)
  assert.deepEqual([...bar.activeChips.map(c => c.prop)], ['clientCode', 'unitName'])
})

test('空数组（日期区间）视为未填写', () => {
  const bar = createBar({ requestDateRange: [] }, [['requestDateRange', '需求日期']])
  assert.equal(bar.activeCount, 0)
})

test('移除单条 chip 只清空该字段并触发重查', () => {
  const model = { clientCode: '华为', sourceCode: 'SO01' }
  const bar = createBar(model, [['clientCode', '客户编码'], ['sourceCode', '来源单据编号']])
  bar.clearField('clientCode')
  assert.equal(model.clientCode, null)
  assert.equal(model.sourceCode, 'SO01')
  assert.deepEqual(bar.events, ['search'])
})

test('清除全部只清空二级条件，不动一级条件', () => {
  const model = { workorderCode: 'MO1', clientCode: '华为', sourceCode: 'SO01' }
  const bar = createBar(model, [['clientCode', '客户编码'], ['sourceCode', '来源单据编号']])
  bar.clearAdvanced()
  assert.equal(model.clientCode, null)
  assert.equal(model.sourceCode, null)
  assert.equal(model.workorderCode, 'MO1')
  assert.deepEqual(bar.events, ['search'])
})

test('下拉条件回显字典标签而不是编码，查不到时退回原值', () => {
  const options = [['PREPARE', '待处理'], ['CONFIRMED', '已确认']]
  const bar = createBar({ status: 'CONFIRMED', businessType: 2 },
    [['status', '单据状态', options], ['businessType', '业务类型', options]])
  const values = [...bar.activeChips.map(c => c.value)]
  assert.equal(values[0], '已确认')
  // 值在选项里找不到时按原值显示，不会显示成空
  assert.equal(values[1], 2)
})

test('resetFields 转发给内部的 el-form，兼容 resetForm("queryForm")', () => {
  const bar = createBar({}, [])
  let called = 0
  bar.$refs = { form: { resetFields: () => { called += 1 } } }
  bar.resetFields()
  assert.equal(called, 1)
  bar.$refs = {}
  bar.resetFields()
  assert.equal(called, 1)
})

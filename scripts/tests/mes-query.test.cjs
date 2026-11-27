const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')

function component(request = () => {}, file = 'ktg-mes-ui/src/views/mes/ai/index.vue') {
  const source = fs.readFileSync(file, 'utf8')
  const code = source.match(/<script>([\s\S]*?)<\/script>/)[1]
    .replace(/^import .*$/mg, '').replace('export default', 'component =')
  // 导入行被剥掉后，组件里仍会被 data()/computed 用到的常量与类要由沙箱补上（数字人素材路径等）。
  const box = { component: null, request, Date, console, process: {env:{}},
    echarts: require('../../ktg-mes-ui/node_modules/echarts'),
    AVATAR_ASSETS: '', avatarAnswerText: () => '', AvatarSpeech: class { stop() {} } }
  vm.createContext(box); vm.runInContext(code, box)
  const ctx = { ...box.component.data(), ...box.component.methods, answers: [],
    $auth: { hasPermi: () => true },
    push(m) { this.answer = m; this.answers.push(m) } }
  return ctx
}

test('安全库存问题进入已有预警查询', () => {
  assert.equal(component().matchRule('现在哪些物料低于安全库存'), 'queryAlert')
})
test('显式年份不得变成当前年份', () => {
  assert.equal(component().parseTime('2024年10月份库存').from, '2024-10-01')
})
test('汇总查询读取第二页，不截断第501条', async () => {
  const ctx = component(async config => ({ rows: config.params.pageNum === 1 ? Array.from({length:500}, (_,i)=>({id:i})) : [{id:500}], total:501 }))
  assert.equal((await ctx.get('/test')).rows.length, 501)
})
test('库存以当前余额反推历史期末和非零期初', async () => {
  const ctx = component()
  ctx.parseTime = () => ({from:'2026-10-01',to:'2026-10-31',label:'10月'})
  ctx.matchItem = async () => ({item:{itemId:1,itemCode:'A',itemName:'A'},items:[{itemId:1,itemCode:'A',itemName:'A'}]})
  const tx = [{itemCode:'A',batchCode:'B',warehouseCode:'RAW',transactionDate:'2026-10-02 10:00:00',transactionQuantity:10}, {itemCode:'A',batchCode:'B',warehouseCode:'RAW',transactionDate:'2026-10-03 10:00:00',transactionQuantity:-3}]
  ctx.get = async url => ({rows:url.includes('wmstock') ? [{itemCode:'A',batchCode:'B',warehouseCode:'RAW',quantityOnhand:107}] : tx})
  await ctx.queryStock('A库存')
  const card = ctx.answers.find(m => m.type === 'stock')
  assert.equal(card.begin,100); assert.equal(card.end,107)
})
test('采购入库不包括内部转仓，并使用后端日期参数', async () => {
  const ctx=component(); let sent
  ctx.parseTime=()=>({from:'2026-10-01',to:'2026-10-31',label:'10月'})
  ctx.matchItem=async()=>({item:null,items:[],rows:[]})
  ctx.get=async (url,params)=>{sent=params;return {rows:[{itemCode:'A',transactionQuantity:5,transactionType:'ITEM_RECPT'},{itemCode:'A',transactionQuantity:8,transactionType:'ITEM_ISSUE_IN'}]}}
  await ctx.queryFlow('本月采购入库','IN')
  assert.equal(ctx.answers.find(m => m.type === 'list').rows[0].qty,5)
  assert.equal(sent.params.beginTime,'2026-10-01 00:00:00')
  assert.equal(sent.params.endTime,'2026-10-31 23:59:59')
})
test('过程检验只统计已完成检验，不把待检算为合格', async () => {
  const ctx=component();ctx.parseTime=()=>({from:'2026-10-01',to:'2026-10-31',label:'10月'})
  ctx.get=async()=>({rows:[{status:'FINISHED',inspectDate:'2026-10-02',quantityCheck:10,quantityUnqualified:2},{status:'PREPARE',createTime:'2026-10-02',quantityCheck:100,quantityUnqualified:0}]})
  await ctx.queryInspection('10月合格率')
  assert.match(ctx.answer.text,/80%/); assert.equal(ctx.answers.find(m => m.type === 'list').rows.length,1)
})
test('驾驶舱包含生产使用的虚拟线边仓且不合并不同单位', async () => {
  const ctx=component(undefined,'ktg-mes-ui/src/views/mes/dashboard/index.vue'); let option
  ctx.charts={stock:{setOption:o=>{option=o}}}
  ctx.get=async url=>({rows:url.includes('mditem') ? [] : [
    {itemCode:'A',warehouseCode:'XBK_VIRTUAL',warehouseName:'虚拟线边库',quantityOnhand:10,unitName:'个'},
    {itemCode:'B',warehouseCode:'WH-RAW',warehouseName:'原料仓',quantityOnhand:2,unitName:'包'}]})
  await ctx.loadStock()
  const categoryLabels = (option.xAxis.type === 'category' ? option.xAxis : option.yAxis).data
  assert.ok(categoryLabels.some(n=>n.includes('线边')))
  assert.ok(option.series[0].data.includes(10))
  assert.ok(categoryLabels.some(n=>n.includes('包')))
})
test('指定工单号不能返回其他工单', async () => {
  const ctx=component();ctx.get=async()=>({rows:[{workorderCode:'WO-001',quantity:10,quantityProduced:2},{workorderCode:'WO-002',quantity:10,quantityProduced:3}]})
  await ctx.queryWorkorder('工单号 WO-002 的进度')
  const card = ctx.answers.find(m => m.type === 'list')
  assert.equal(card.rows.length,1);assert.equal(card.rows[0].workorderCode,'WO-002')
})
test('多个品类匹配时库存查询应澄清而非悄悄取第一个', async () => {
  const ctx=component();ctx.matchItem=async()=>({item:{itemCode:'A'},items:[{itemCode:'A',itemName:'电子A'},{itemCode:'B',itemName:'电子B'}]})
  ctx.get=async()=>{throw new Error('ambiguous item must not query arbitrary stock')}
  await ctx.queryStock('电子件库存')
  assert.equal(ctx.answer.type,'ask')
})

test('“成品是什么/生产什么”走产品目录而不是工单进度', () => {
  const c = component()
  for (const q of ['我们工业生产的成品是什么', '我们要生产什么', '本厂生产的产品有哪些', '公司产品有哪些']) {
    assert.equal(c.matchRule(q), 'queryProducts', q)
  }
  assert.equal(c.matchRule('哪些产品用上壳'), null)
  assert.equal(c.matchRule('成品库存有多少'), 'queryStock')
  assert.equal(c.matchRule('这个月成品入库多少'), 'queryFlowIn')
})
test('产品目录只列自制产品且整机成品在前', async () => {
  const c = component()
  c.get = async () => ({ rows: [
    { itemCode: 'TG-EL-001', itemName: '主控PCBA', itemOrProduct: 'ITEM', itemTypeName: '电子件', unitName: '个', enableFlag: 'Y' },
    { itemCode: 'TG-SF-002', itemName: '扳机与侧键手柄预装件', itemOrProduct: 'PRODUCT', itemTypeName: '自制半成品', unitName: '套', enableFlag: 'Y' },
    { itemCode: 'TG-SF-001', itemName: '测温显示电控预装件', itemOrProduct: 'PRODUCT', itemTypeName: '自制半成品', unitName: '套', enableFlag: 'Y' },
    { itemCode: 'TG-FG-001', itemName: '红外测温枪整机', itemOrProduct: 'PRODUCT', itemTypeName: '整机成品', unitName: '台', enableFlag: 'Y' }
  ] })
  await c.queryProducts()
  const card = c.answers.find(m => m.type === 'list')
  assert.deepEqual(card.rows.map(r => r.itemCode), ['TG-FG-001', 'TG-SF-001', 'TG-SF-002'])
  assert.match(card.source, /md_item/)
  assert.match(c.answer.text, /红外测温枪整机（TG-FG-001）/)
  assert.match(c.answer.text, /整机成品 1 项/)
  assert.match(c.answer.text, /自制半成品 2 项/)
})
test('停用的产品不计入成品清单', async () => {
  const c = component()
  c.get = async () => ({ rows: [
    { itemCode: 'TG-FG-001', itemName: '旧测温枪', itemOrProduct: 'PRODUCT', itemTypeName: '整机成品', enableFlag: 'N' }
  ] })
  await c.queryProducts()
  assert.equal(c.answers.find(m => m.type === 'list').rows.length, 0)
  assert.match(c.answer.text, /没有查到启用的产品档案/)
})
test('没有产品档案权限时不查数据并说明', async () => {
  const c = component()
  c.$auth = { hasPermi: () => false }
  c.get = async () => { throw new Error('没有权限时不得查询产品档案') }
  await c.queryProducts()
  assert.match(c.answer.text, /权限/)
})

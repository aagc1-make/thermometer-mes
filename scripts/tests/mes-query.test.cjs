const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')

function component(request = () => {}, file = 'ktg-mes-ui/src/views/mes/ai/index.vue') {
  const source = fs.readFileSync(file, 'utf8')
  const code = source.match(/<script>([\s\S]*?)<\/script>/)[1]
    .replace(/^import .*$/mg, '').replace('export default', 'component =')
  const box = { component: null, request, Date, console, process: {env:{}} }
  vm.createContext(box); vm.runInContext(code, box)
  const ctx = { ...box.component.data(), ...box.component.methods, push(m) { this.answer = m } }
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
  assert.equal(ctx.answer.begin,100); assert.equal(ctx.answer.end,107)
})
test('采购入库不包括内部转仓，并使用后端日期参数', async () => {
  const ctx=component(); let sent
  ctx.parseTime=()=>({from:'2026-10-01',to:'2026-10-31',label:'10月'})
  ctx.matchItem=async()=>({item:null,items:[],rows:[]})
  ctx.get=async (url,params)=>{sent=params;return {rows:[{itemCode:'A',transactionQuantity:5,transactionType:'ITEM_RECPT'},{itemCode:'A',transactionQuantity:8,transactionType:'ITEM_ISSUE_IN'}]}}
  await ctx.queryFlow('本月采购入库','IN')
  assert.equal(ctx.answer.rows[0].qty,5)
  assert.equal(sent.params.beginTime,'2026-10-01 00:00:00')
  assert.equal(sent.params.endTime,'2026-10-31 23:59:59')
})
test('过程检验只统计已完成检验，不把待检算为合格', async () => {
  const ctx=component();ctx.parseTime=()=>({from:'2026-10-01',to:'2026-10-31',label:'10月'})
  ctx.get=async()=>({rows:[{status:'FINISHED',inspectDate:'2026-10-02',quantityCheck:10,quantityUnqualified:2},{status:'PREPARE',createTime:'2026-10-02',quantityCheck:100,quantityUnqualified:0}]})
  await ctx.queryInspection('10月合格率')
  assert.match(ctx.answer.title,/80%/); assert.equal(ctx.answer.rows.length,1)
})
test('驾驶舱包含生产使用的虚拟线边仓且不合并不同单位', async () => {
  const ctx=component(undefined,'ktg-mes-ui/src/views/mes/dashboard/index.vue'); let option
  ctx.charts={stock:{setOption:o=>{option=o}}}
  ctx.get=async url=>({rows:url.includes('mditem') ? [] : [
    {itemCode:'A',warehouseCode:'XBK_VIRTUAL',warehouseName:'虚拟线边库',quantityOnhand:10,unitName:'个'},
    {itemCode:'B',warehouseCode:'WH-RAW',warehouseName:'原料仓',quantityOnhand:2,unitName:'包'}]})
  await ctx.loadStock()
  assert.ok(option.xAxis.data.some(n=>n.includes('线边')))
  assert.ok(option.series[0].data.includes(10))
  assert.ok(option.xAxis.data.some(n=>n.includes('包')))
})
test('指定工单号不能返回其他工单', async () => {
  const ctx=component();ctx.get=async()=>({rows:[{workorderCode:'WO-001',quantity:10,quantityProduced:2},{workorderCode:'WO-002',quantity:10,quantityProduced:3}]})
  await ctx.queryWorkorder('工单号 WO-002 的进度')
  assert.equal(ctx.answer.rows.length,1);assert.equal(ctx.answer.rows[0].workorderCode,'WO-002')
})
test('多个品类匹配时库存查询应澄清而非悄悄取第一个', async () => {
  const ctx=component();ctx.matchItem=async()=>({item:{itemCode:'A'},items:[{itemCode:'A',itemName:'电子A'},{itemCode:'B',itemName:'电子B'}]})
  ctx.get=async()=>{throw new Error('ambiguous item must not query arbitrary stock')}
  await ctx.queryStock('电子件库存')
  assert.equal(ctx.answer.type,'ask')
})

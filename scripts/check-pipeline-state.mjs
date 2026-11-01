/* 只读核查工单主线，与首页共享加载和状态计算。
   用法：设置 TOKEN、WO，执行 node scripts/check-pipeline-state.mjs。
*/
import { readFile } from 'node:fs/promises'
const source = await readFile(new URL('../ktg-mes-ui/src/utils/workorderPipeline.js', import.meta.url), 'utf8')
const { buildWorkorderPipeline, loadWorkorderPipeline } = await import('data:text/javascript;base64,' + Buffer.from(source).toString('base64'))
const API = process.env.API || 'http://127.0.0.1:8081'
const TOKEN = process.env.TOKEN
const WO = process.env.WO
if (!TOKEN || !WO || !/^\d+$/.test(WO)) {
  console.error('需要 TOKEN 和有效的 WO 环境变量')
  process.exit(2)
}
async function request({ url, params = {} }) {
  const response = await fetch(API + url + '?' + new URLSearchParams(params), {
    headers: { Authorization: 'Bearer ' + TOKEN }
  })
  const result = await response.json()
  if (!response.ok || (result.code != null && Number(result.code) !== 200)) throw new Error(result.msg || '接口读取失败')
  return result
}
try {
  const wo = (await request({ url: '/mes/pro/workorder/' + WO })).data
  if (!wo || String(wo.workorderId) !== WO) throw new Error('未找到指定工单')
  const snapshot = await loadWorkorderPipeline(request, wo)
  const steps = buildWorkorderPipeline(wo, snapshot)
  console.log('工单 ' + wo.workorderCode + ' · ' + wo.status)
  steps.forEach((step, index) => console.log(`${index + 1}. [${step.stateText}] ${step.name}：${step.docCode}\n   ${step.text}`))
  const next = steps.find(step => step.state === 'doing')
  console.log('下一步：' + (next ? next.name : '工单流程已完成'))
} catch (error) {
  console.error(error.message)
  process.exitCode = 1
}

export function getBusinessConfig(domain = 'antifraud') {
  if (!['antifraud', 'drilling'].includes(domain)) throw new Error(`未知业务模式：${domain}`)
  const drilling = domain === 'drilling'
  const labels = drilling
    ? ['井场监测', '事件分析', '井控知识', '专家协同', '风险事件记录', '产品首页']
    : ['首页', '反诈检测', '反诈教育', '监护管理', '检测历史', '官网首页']
  return Object.freeze({
    domain, drilling, brand: drilling ? '井安智联' : '安辨',
    homePath: '/home', analysisPath: '/detectagent',
    nav: ['/home', '/detectagent', '/education', '/guardian', '/reporthistory', '/landing']
      .map((path, index) => ({ path, label: labels[index] })),
  })
}

export const business = getBusinessConfig(import.meta.env?.VITE_BUSINESS_DOMAIN || 'antifraud')
export const isDrilling = business.drilling

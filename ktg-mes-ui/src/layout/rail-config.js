/**
 * 左栏「总」导航配置（严格对标需求书 §2.3–2.9）
 *
 * 8 个域 = 首页 + 7 个需求域；每个域下按 path 前缀把现有菜单重组成「母目录」。
 * 说明：只做前端展示分组，不改 sys_menu 数据、不改权限标识，
 *      删掉本文件即可回到原始的平铺菜单。
 *
 * 字段：
 *   key      唯一标识（用于选中态）
 *   title    左栏文字
 *   icon     element-ui 图标类名
 *   roots    归属该域的顶级路由 path 前缀（对应 sys_menu 顶级目录的 path）
 *   groups   右栏母目录分组：[组名, 该组包含的路由 path 片段]
 */
export const railDomains = [
  {
    key: 'home',
    title: '首页',
    icon: 'el-icon-house',
    roots: ['/index'],
    groups: []
  },
  {
    key: 'md',
    title: '主数据',
    icon: 'el-icon-files',
    roots: ['/mes/md'],
    groups: [
      ['物料资料', ['mditem', 'itemtype', 'unitmeasure']],
      ['业务档案', ['client', 'vendor']],
      ['生产基础', ['workshop', 'workstation']]
    ]
  },
  {
    key: 'wm',
    title: '仓储',
    icon: 'el-icon-box',
    roots: ['/mes/wm'],
    groups: [
      ['库存管理', ['wmstock', 'transaction', 'batch']],
      ['入库管理', ['itemrecpt', 'arrivalnotice', 'productrecpt', 'miscrecpt']],
      ['出库管理', ['issue', 'productsales', 'salesnotice', 'miscissue', 'transfer']],
      ['退料退货', ['rtissue', 'rtvendor', 'rtsales']],
      ['其他业务', ['stocktaking', 'package', 'barcode', 'sn', 'outsourceissue', 'outsourcerecpt']]
    ]
  },
  {
    key: 'pro',
    title: '生产',
    icon: 'el-icon-s-data',
    roots: ['/mes/pro'],
    groups: [
      ['驾驶舱', ['dashboard']],
      ['计划', ['workorder', 'proschedule', 'schedule', 'andonrecord']],
      ['工艺', ['process', 'proroute']],
      ['执行', ['feedback', 'procard']]
    ]
  },
  {
    key: 'qc',
    title: '质量',
    icon: 'el-icon-s-claim',
    roots: ['/mes/qc'],
    groups: [
      ['质量基础', ['qcindex', 'qctemplate', 'qcdefect']],
      ['检验业务', ['iqc', 'ipqc', 'oqc', 'rqc', 'pendinginspect']],
      ['质量记录', ['defectrecord', 'batchtrace']]
    ]
  },
  {
    key: 'dv',
    title: '设备',
    icon: 'el-icon-s-platform',
    roots: ['/mes/dv', '/tm'],
    groups: [
      ['设备台账', ['machinerytype', 'machinery']],
      ['点检保养', ['dvsubject', 'checkplan', 'checkrecord', 'maintenrecord']],
      ['设备维修', ['repair']],
      ['工装夹具', ['tooltype', 'tool']]
    ]
  },
  {
    key: 'cal',
    title: '班组排班',
    icon: 'el-icon-user',
    roots: ['/cal'],
    groups: [
      ['班组与人员', ['team']],
      ['排班管理', ['plan', 'holiday', 'calendar', 'shift']]
    ]
  },
  {
    key: 'sys',
    title: '系统',
    icon: 'el-icon-s-tools',
    roots: ['/system', '/monitor', '/tool', '/user', '/report', '/print', '/iot'],
    groups: [
      ['用户权限', ['user', 'role', 'menu', 'dept', 'post']],
      ['基础配置', ['dict', 'config', 'notice', 'autocodeRule', 'message']],
      ['日志监控', ['operlog', 'logininfor', 'online', 'job', 'druid', 'server', 'cache']],
      ['扩展功能', ['report', 'print', 'iot', 'chart', 'designer']]
    ]
  },
  {
    key: 'ai',
    title: '智能助手',
    icon: 'el-icon-chat-dot-round',
    roots: ['/mes/ai'],
    groups: [
      ['智能问答', ['chat']]
    ]
  }
]

/** 根据当前路由 path 找出所属的域 */
export function findDomainByPath(path) {
  if (!path) return railDomains[0]
  // 优先精确前缀匹配，其次做长度倒序避免 /mes/md 抢到 /mes/pro 的子路径
  const sorted = [...railDomains].sort((a, b) => b.roots.join('').length - a.roots.join('').length)
  const hit = sorted.find(d => d.roots.some(r => path === r || path.indexOf(r + '/') === 0))
  return hit || railDomains[0]
}

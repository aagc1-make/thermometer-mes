import type { Product } from '@/types';

/**
 * 产品目录（与 MES 数据库 j2eedb 完全一致）
 *
 * 数据来源：MES 物料主数据 md_item，共 17 条
 * - 整机成品 1 条：TG-FG-001
 * - 自制半成品 2 条：TG-SF-001 / TG-SF-002
 * - 外购物料 14 条：电子件 6 / 结构件 6 / 包装辅料 2
 *
 * 编码、名称、规格、单位、备注均照搬 MES，未做改写；
 * 起订量 / 交期为小程序展示用的订货参数，MES 中未维护。
 */
export const PRODUCTS: Product[] = [
  {
    id: '9201',
    name: '红外测温枪整机',
    code: 'TG-FG-001',
    category: 'finished',
    categoryName: '成品',
    group: '整机成品',
    param: 'TG-300 便携式红外测温枪（32~42.9℃）',
    moq: 10,
    unit: '台',
    leadDays: 15,
    art: 'thermometer',
    desc: '本期唯一成品，演示生产 10 台'
  },
  {
    id: '9202',
    name: '测温显示电控预装件',
    code: 'TG-SF-001',
    category: 'semi',
    categoryName: '半成品',
    group: '自制半成品',
    param: '电控预装件（PCBA+传感器+LCD+蜂鸣器+透镜）',
    moq: 50,
    unit: '套',
    leadDays: 10,
    art: 'pcb',
    desc: '半成品，需总装前预装并过程检验'
  },
  {
    id: '9203',
    name: '扳机与侧键手柄预装件',
    code: 'TG-SF-002',
    category: 'semi',
    categoryName: '半成品',
    group: '自制半成品',
    param: '手柄预装件（三键按键板+电池+扳机组件）',
    moq: 50,
    unit: '套',
    leadDays: 10,
    art: 'trigger',
    desc: '半成品，需过程检验后供总装'
  },
  {
    id: '9204',
    name: '主控PCBA',
    code: 'TG-EL-001',
    category: 'material',
    categoryName: '物料',
    group: '电子件',
    param: 'TG-MAIN-V1.2 主控板（已贴片）',
    moq: 500,
    unit: '个',
    leadDays: 5,
    art: 'mainboard',
    desc: '按整体外购，BOM 不展开内部器件'
  },
  {
    id: '9205',
    name: '红外传感器模块',
    code: 'TG-EL-002',
    category: 'material',
    categoryName: '物料',
    group: '电子件',
    param: 'MLX90614 兼容 TO-39 红外测温传感器',
    moq: 500,
    unit: '个',
    leadDays: 5,
    art: 'sensor',
    desc: '测温核心器件'
  },
  {
    id: '9206',
    name: '显示组件',
    code: 'TG-EL-003',
    category: 'material',
    categoryName: '物料',
    group: '电子件',
    param: '1.0 寸段码 LCD 显示模组',
    moq: 500,
    unit: '个',
    leadDays: 5,
    art: 'pcb',
    desc: '显示测温结果与状态'
  },
  {
    id: '9207',
    name: '三键按键板',
    code: 'TG-EL-004',
    category: 'material',
    categoryName: '物料',
    group: '电子件',
    param: '上/下/设置 三键薄膜按键板',
    moq: 500,
    unit: '个',
    leadDays: 5,
    art: 'pcb',
    desc: '手柄预装件组成'
  },
  {
    id: '9208',
    name: '蜂鸣器',
    code: 'TG-EL-005',
    category: 'material',
    categoryName: '物料',
    group: '电子件',
    param: '5V 有源蜂鸣器',
    moq: 500,
    unit: '个',
    leadDays: 5,
    art: 'pcb',
    desc: '超温与按键提示音'
  },
  {
    id: '9209',
    name: '电池',
    code: 'TG-EL-006',
    category: 'material',
    categoryName: '物料',
    group: '电子件',
    param: 'AAA 1.5V 碱性电池',
    moq: 1000,
    unit: '节',
    leadDays: 3,
    art: 'pcb',
    desc: '手柄预装件组成'
  },
  {
    id: '9210',
    name: '上壳',
    code: 'TG-ST-001',
    category: 'material',
    categoryName: '物料',
    group: '结构件',
    param: 'ABS 白色 上壳',
    moq: 500,
    unit: '个',
    leadDays: 5,
    art: 'trigger',
    desc: '整机结构件'
  },
  {
    id: '9211',
    name: '下壳',
    code: 'TG-ST-002',
    category: 'material',
    categoryName: '物料',
    group: '结构件',
    param: 'ABS 白色 下壳',
    moq: 500,
    unit: '个',
    leadDays: 5,
    art: 'trigger',
    desc: '整机结构件'
  },
  {
    id: '9212',
    name: '扳机组件',
    code: 'TG-ST-003',
    category: 'material',
    categoryName: '物料',
    group: '结构件',
    param: '扳机+复位弹簧组件',
    moq: 500,
    unit: '个',
    leadDays: 5,
    art: 'trigger',
    desc: '手柄预装件组成'
  },
  {
    id: '9213',
    name: '红外透镜',
    code: 'TG-ST-004',
    category: 'material',
    categoryName: '物料',
    group: '结构件',
    param: 'HDPE 红外聚焦透镜',
    moq: 500,
    unit: '个',
    leadDays: 5,
    art: 'sensor',
    desc: '电控预装件组成'
  },
  {
    id: '9214',
    name: '螺丝包',
    code: 'TG-ST-005',
    category: 'material',
    categoryName: '物料',
    group: '结构件',
    param: 'M2×6 自攻螺丝 6 颗/包',
    moq: 1000,
    unit: '包',
    leadDays: 3,
    art: 'pcb',
    desc: '整机装配紧固件'
  },
  {
    id: '9215',
    name: '防震泡棉',
    code: 'TG-ST-006',
    category: 'material',
    categoryName: '物料',
    group: '结构件',
    param: 'EVA 防震泡棉 30×20mm',
    moq: 1000,
    unit: '片',
    leadDays: 3,
    art: 'pcb',
    desc: '电控件缓冲固定'
  },
  {
    id: '9216',
    name: '说明书',
    code: 'TG-PK-001',
    category: 'material',
    categoryName: '物料',
    group: '包装辅料',
    param: '中文版使用说明书（8 页）',
    moq: 1000,
    unit: '份',
    leadDays: 3,
    art: 'pcb',
    desc: '整机包装附件'
  },
  {
    id: '9217',
    name: '彩盒包装',
    code: 'TG-PK-002',
    category: 'material',
    categoryName: '物料',
    group: '包装辅料',
    param: '350×200×80mm 彩盒',
    moq: 1000,
    unit: '个',
    leadDays: 3,
    art: 'pcb',
    desc: '整机包装附件'
  }
];

export const HOME_PRODUCTS = ['9201'];

export const PRODUCT_FILTERS = [
  '全部',
  '成品',
  '半成品',
  '物料',
  '电子件',
  '结构件',
  '包装辅料'
] as const;

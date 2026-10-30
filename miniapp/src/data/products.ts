import type { Product } from '@/types';

/**
 * 可订购产品目录（演示数据，与界面设计稿保持一致）
 * 后续接入后端 /mobile/md/item 时，可保持同一数据结构映射
 */
export const PRODUCTS: Product[] = [
  {
    id: 'p001',
    name: '基础款红外测温枪',
    code: 'IRG-FG-001',
    category: 'finished',
    categoryName: '成品',
    param: '量程 32.0-42.9°C · 精度 ±0.2°C',
    moq: 50,
    unit: '件',
    leadDays: 7,
    art: 'thermometer',
    desc: '基础款红外测温枪，采用高灵敏红外传感器与低功耗主控方案，适用于医院、学校、园区等场景的快速体温筛查。整机通过出厂校准，支持批量贴牌与中性包装。'
  },
  {
    id: 'p002',
    name: '医用高精度测温枪',
    code: 'IRG-FG-002',
    category: 'finished',
    categoryName: '成品',
    param: '量程 32.0-42.9°C · 精度 ±0.1°C',
    moq: 50,
    unit: '件',
    leadDays: 9,
    art: 'precision',
    desc: '医用级高精度红外测温枪，双点校准工艺，精度可达 ±0.1°C，适用于门诊、发热门诊等对测量精度要求较高的场景。'
  },
  {
    id: 'p003',
    name: '壁挂式测温终端',
    code: 'IRG-FG-003',
    category: 'finished',
    categoryName: '成品',
    param: '量程 30.0-45.0°C · 支持壁挂安装',
    moq: 20,
    unit: '台',
    leadDays: 12,
    art: 'wall',
    desc: '壁挂式测温终端，支持连续测温与异常提示，可安装于入口通道，配合管理平台实现出入记录留存。'
  },
  {
    id: 'p004',
    name: '测温显示电控预装件',
    code: 'IRG-SA-CTRL-001',
    category: 'semi',
    categoryName: '半成品',
    group: '电控组件',
    param: '主控 / 传感 / 显示预装',
    moq: 100,
    unit: '件',
    leadDays: 6,
    art: 'pcb',
    desc: '主控板、红外传感与显示模组已完成预装与联调，可直接用于整机组装，减少下游装配与调试工序。'
  },
  {
    id: 'p005',
    name: '扳机侧键手柄预装件',
    code: 'IRG-SA-TRIG-001',
    category: 'semi',
    categoryName: '半成品',
    group: '结构件',
    param: '前壳 / 扳机 / 侧键预装',
    moq: 100,
    unit: '件',
    leadDays: 6,
    art: 'trigger',
    desc: '前壳、扳机与侧键已完成预装，按键手感统一，装配公差稳定，适用于整机产线的快速组装。'
  },
  {
    id: 'p006',
    name: '主板贴片组件',
    code: 'IRG-SA-PCBA-001',
    category: 'semi',
    categoryName: '半成品',
    group: '电控组件',
    param: '四层板 · 已贴片 · 已烧录',
    moq: 200,
    unit: '件',
    leadDays: 8,
    art: 'mainboard',
    desc: '四层板贴片组件，已完成贴片、检测与基础固件烧录，可按客户固件版本批量定制。'
  },
  {
    id: 'p007',
    name: '红外传感模组',
    code: 'IRG-SA-SENS-001',
    category: 'semi',
    categoryName: '半成品',
    group: '电控组件',
    param: '数字输出 · 出厂单点校准',
    moq: 300,
    unit: '件',
    leadDays: 10,
    art: 'sensor',
    desc: '红外传感模组，数字信号输出，出厂完成单点校准，可显著降低整机厂的校准工时。'
  },
  {
    id: 'p008',
    name: '显示驱动板',
    code: 'IRG-SA-DISP-001',
    category: 'semi',
    categoryName: '半成品',
    group: '电控组件',
    param: '段码屏驱动 · 低功耗',
    moq: 200,
    unit: '件',
    leadDays: 8,
    art: 'mainboard',
    desc: '段码屏显示驱动板，低功耗设计，支持定制显示内容与背光方案。'
  },
  {
    id: 'p009',
    name: '前壳注塑件',
    code: 'IRG-ST-FRONT-001',
    category: 'semi',
    categoryName: '半成品',
    group: '结构件',
    param: 'ABS 阻燃 · 白色哑光',
    moq: 500,
    unit: '件',
    leadDays: 5,
    art: 'trigger',
    desc: '前壳注塑件，ABS 阻燃材料，白色哑光表面，模具稳定，支持丝印与配色定制。'
  },
  {
    id: 'p010',
    name: '侧键按键组件',
    code: 'IRG-ST-KEY-001',
    category: 'semi',
    categoryName: '半成品',
    group: '结构件',
    param: '硅胶按键 · 手感一致',
    moq: 500,
    unit: '件',
    leadDays: 5,
    art: 'trigger',
    desc: '侧键按键组件，硅胶材质，按压手感一致，寿命测试通过 10 万次。'
  },
  {
    id: 'p011',
    name: '医用高精度测温枪 Pro',
    code: 'IRG-FG-004',
    category: 'finished',
    categoryName: '成品',
    param: '量程 32.0-42.9°C · 精度 ±0.1°C',
    moq: 30,
    unit: '件',
    leadDays: 10,
    art: 'precision',
    desc: '医用高精度测温枪 Pro 版本，在标准版基础上增加环境温度补偿与数据存储能力。'
  },
  {
    id: 'p012',
    name: '家用红外测温枪',
    code: 'IRG-FG-005',
    category: 'finished',
    categoryName: '成品',
    param: '量程 32.0-42.9°C · 精度 ±0.2°C',
    moq: 60,
    unit: '件',
    leadDays: 7,
    art: 'thermometer',
    desc: '家用红外测温枪，轻巧便携，一键测量，适合家庭与社区场景使用。'
  }
];

/** 首页展示的可订购产品（按设计稿顺序） */
export const HOME_PRODUCTS = ['p001', 'p004', 'p005'];

/** 产品页分类筛选 */
export const PRODUCT_FILTERS = ['全部', '成品', '半成品', '电控组件', '结构件'] as const;

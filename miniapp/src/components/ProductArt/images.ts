/**
 * 产品实拍图映射：按物料编码（itemCode）索引。
 * 图片通过 taro copy.patterns 原样拷贝到产物 assets/products 目录，
 * 运行时按绝对路径引用（微信小程序与 H5 均可用）。
 * 图片来自「产品图/产品图-实物」，按语义匹配 MES 物料：
 *   TG-FG-001 整机 ← 基础款红外测温枪实拍
 *   TG-SF-001 电控预装件 ← 测温显示电控预装件
 *   TG-SF-002 手柄预装件 ← 扳机侧键手柄预装件
 *   TG-EL-001 主控PCBA ← 主板贴片组件
 *   TG-EL-002 传感器模块 ← 红外传感模组
 *   TG-EL-003 显示组件 ← 显示驱动板
 *   TG-EL-004 三键按键板 ← 侧键按键组件
 *   TG-EL-005 蜂鸣器 / TG-EL-006 电池 / TG-ST-003 扳机组件 / TG-ST-004 红外透镜
 *   TG-ST-005 螺丝包 / TG-ST-006 防震泡棉 / TG-PK-001 说明书 / TG-PK-002 彩盒包装
 *   TG-ST-001 上壳 ← 前壳注塑件 / TG-ST-002 下壳 ← 整机实拍
 * 没有对应实拍图的物料继续使用 ProductArt 的纯 CSS 默认示意图。
 */
const PREFIX = '/assets/products/';

const CODES = [
  'TG-FG-001',
  'TG-SF-001',
  'TG-SF-002',
  'TG-EL-001',
  'TG-EL-002',
  'TG-EL-003',
  'TG-EL-004',
  'TG-EL-005',
  'TG-EL-006',
  'TG-ST-001',
  'TG-ST-002',
  'TG-ST-003',
  'TG-ST-004',
  'TG-ST-005',
  'TG-ST-006',
  'TG-PK-001',
  'TG-PK-002'
] as const;

const PRODUCT_IMAGES: Record<string, string> = CODES.reduce(
  (acc, code) => ({ ...acc, [code]: `${PREFIX}${code}.jpg` }),
  {},
);

/**
 * 历史询价单 / 订单中仍存在 IRG 编码（服务端单据数据），
 * 展示时按语义映射到当前 TG 物料的实拍图，保证与产品目录一致。
 */
const LEGACY_ALIAS: Record<string, string> = {
  'IRG-FG-001': 'TG-FG-001',
  'IRG-FG-002': 'TG-FG-001',
  'IRG-FG-003': 'TG-FG-001',
  'IRG-FG-004': 'TG-FG-001',
  'IRG-FG-005': 'TG-FG-001',
  'IRG-SA-CTRL-001': 'TG-SF-001',
  'IRG-SA-TRIG-001': 'TG-SF-002',
  'IRG-SA-PCBA-001': 'TG-EL-001',
  'IRG-SA-SENS-001': 'TG-EL-002',
  'IRG-SA-DISP-001': 'TG-EL-003',
  'IRG-ST-FRONT-001': 'TG-ST-001',
  'IRG-ST-KEY-001': 'TG-EL-004'
};

export function productImage(itemCode?: string): string | undefined {
  if (!itemCode) return undefined;
  if (PRODUCT_IMAGES[itemCode]) {
    return PRODUCT_IMAGES[itemCode];
  }
  const alias = LEGACY_ALIAS[itemCode];
  return alias ? PRODUCT_IMAGES[alias] : undefined;
}

export default PRODUCT_IMAGES;

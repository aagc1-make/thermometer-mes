import type { BomLine } from '@/types';

/**
 * 产品 BOM 组成关系（与 MES 数据库 j2eedb.md_product_bom 完全一致，共 16 行）
 *
 * TG-FG-001 红外测温枪整机 ← 8 行：
 *   TG-SF-001 测温显示电控预装件 ×1 套
 *   TG-SF-002 扳机与侧键手柄预装件 ×1 套
 *   TG-ST-001 上壳 ×1 个 / TG-ST-002 下壳 ×1 个
 *   TG-ST-006 防震泡棉 ×1 片 / TG-ST-005 螺丝包 ×1 包
 *   TG-PK-001 说明书 ×1 份 / TG-PK-002 彩盒包装 ×1 个
 * TG-SF-001 测温显示电控预装件 ← 5 行：TG-EL-001 / 002 / 003 / 005、TG-ST-004
 * TG-SF-002 扳机与侧键手柄预装件 ← 3 行：TG-EL-004、TG-EL-006 ×2、TG-ST-003
 */

/** 外购物料行 */
const m = (itemCode: string, itemName: string, qty: number, unit: string): BomLine => ({
  itemCode,
  itemName,
  qty,
  unit,
  kind: 'material'
});

/** 自制半成品行（可继续展开其下层物料） */
const s = (
  itemCode: string,
  itemName: string,
  qty: number,
  unit: string,
  children: BomLine[]
): BomLine => ({ itemCode, itemName, qty, unit, kind: 'semi', children });

/** TG-SF-001 测温显示电控预装件 的 5 行组成 */
const SF001_PARTS: BomLine[] = [
  m('TG-EL-001', '主控PCBA', 1, '个'),
  m('TG-EL-002', '红外传感器模块', 1, '个'),
  m('TG-EL-003', '显示组件', 1, '个'),
  m('TG-EL-005', '蜂鸣器', 1, '个'),
  m('TG-ST-004', '红外透镜', 1, '个')
];

/** TG-SF-002 扳机与侧键手柄预装件 的 3 行组成 */
const SF002_PARTS: BomLine[] = [
  m('TG-EL-004', '三键按键板', 1, '个'),
  m('TG-EL-006', '电池', 2, '节'),
  m('TG-ST-003', '扳机组件', 1, '个')
];

/** 编码 → BOM 组成行 */
export const BOM_MAP: Record<string, BomLine[]> = {
  'TG-FG-001': [
    s('TG-SF-001', '测温显示电控预装件', 1, '套', SF001_PARTS),
    s('TG-SF-002', '扳机与侧键手柄预装件', 1, '套', SF002_PARTS),
    m('TG-ST-001', '上壳', 1, '个'),
    m('TG-ST-002', '下壳', 1, '个'),
    m('TG-ST-006', '防震泡棉', 1, '片'),
    m('TG-ST-005', '螺丝包', 1, '包'),
    m('TG-PK-001', '说明书', 1, '份'),
    m('TG-PK-002', '彩盒包装', 1, '个')
  ],
  'TG-SF-001': SF001_PARTS,
  'TG-SF-002': SF002_PARTS
};

/** 取某个编码的组成清单，没有维护时返回空数组 */
export function bomOf(itemCode?: string): BomLine[] {
  if (!itemCode) return [];
  return BOM_MAP[itemCode] || [];
}

/** 该半成品 / 物料被哪些成品使用 */
export function parentsOf(itemCode: string): string[] {
  return Object.keys(BOM_MAP)
    .filter((code) => code.startsWith('TG-FG'))
    .filter((code) =>
      BOM_MAP[code].some(
        (line) =>
          line.itemCode === itemCode ||
          (line.children || []).some((child) => child.itemCode === itemCode)
      )
    );
}

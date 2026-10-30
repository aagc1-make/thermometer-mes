/**
 * 产品实拍图映射：按物料编码（itemCode）索引。
 * 图片通过 taro copy.patterns 原样拷贝到产物 assets/products 目录，
 * 运行时按绝对路径引用（微信小程序与 H5 均可用）。
 * 没有对应图片的产品继续使用 ProductArt 的纯 CSS 默认示意图。
 */
const PREFIX = '/assets/products/';

const CODES = [
  'IRG-FG-001',
  'IRG-FG-002',
  'IRG-FG-003',
  'IRG-FG-004',
  'IRG-FG-005',
  'IRG-SA-CTRL-001',
  'IRG-SA-DISP-001',
  'IRG-SA-PCBA-001',
  'IRG-SA-SENS-001',
  'IRG-SA-TRIG-001',
  'IRG-ST-FRONT-001',
  'IRG-ST-KEY-001',
] as const;

const PRODUCT_IMAGES: Record<string, string> = CODES.reduce(
  (acc, code) => ({ ...acc, [code]: `${PREFIX}${code}.jpg` }),
  {},
);

export function productImage(itemCode?: string): string | undefined {
  if (!itemCode) return undefined;
  return PRODUCT_IMAGES[itemCode];
}

export default PRODUCT_IMAGES;

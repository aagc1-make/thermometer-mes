-- ----------------------------
-- 小程序「企业订购」测温枪产品主数据（md_item）
-- 编码统一以 IRG 开头，供 /mobile/inquiry/products 查询
-- attr1=分组（电控组件/结构件） attr2=示意图类型 attr3=起订量 attr4=参考交期(天)
-- 可重复执行（按 item_code 判重）
-- ----------------------------
INSERT INTO md_item (
  item_code, item_name, specification, unit_of_measure, unit_name,
  item_or_product, item_type_id, item_type_code, item_type_name,
  enable_flag, batch_flag, remark, attr1, attr2, attr3, attr4, create_by, create_time
)
SELECT v.item_code, v.item_name, v.specification, v.unit_of_measure, v.unit_name,
       v.item_or_product, v.item_type_id, v.item_type_code, v.item_type_name,
       'Y', 'Y', v.remark, v.attr1, v.attr2, v.attr3, v.attr4, 'inquiry-init', sysdate()
FROM (
  SELECT 'IRG-FG-001' AS item_code, '基础款红外测温枪' AS item_name,
         '量程 32.0-42.9°C · 精度 ±0.2°C' AS specification, '件' AS unit_of_measure, '件' AS unit_name,
         'PRODUCT' AS item_or_product, 277 AS item_type_id, 'ITEM_TYPE_0092' AS item_type_code, '产成品' AS item_type_name,
         '基础款红外测温枪，适用于医院、学校、园区等场景的快速体温筛查，支持批量贴牌与中性包装。' AS remark,
         NULL AS attr1, 'thermometer' AS attr2, 50 AS attr3, 7 AS attr4
  UNION ALL SELECT 'IRG-FG-002', '医用高精度测温枪',
         '量程 32.0-42.9°C · 精度 ±0.1°C', '件', '件',
         'PRODUCT', 277, 'ITEM_TYPE_0092', '产成品',
         '医用级高精度红外测温枪，双点校准工艺，适用于门诊、发热门诊等精度要求较高的场景。',
         NULL, 'precision', 50, 9
  UNION ALL SELECT 'IRG-FG-003', '壁挂式测温终端',
         '量程 30.0-45.0°C · 支持壁挂安装', '台', '台',
         'PRODUCT', 277, 'ITEM_TYPE_0092', '产成品',
         '壁挂式测温终端，支持连续测温与异常提示，可安装于入口通道。',
         NULL, 'wall', 20, 12
  UNION ALL SELECT 'IRG-FG-004', '医用高精度测温枪 Pro',
         '量程 32.0-42.9°C · 精度 ±0.1°C', '件', '件',
         'PRODUCT', 277, 'ITEM_TYPE_0092', '产成品',
         '医用高精度测温枪 Pro 版本，增加环境温度补偿与数据存储能力。',
         NULL, 'precision', 30, 10
  UNION ALL SELECT 'IRG-FG-005', '家用红外测温枪',
         '量程 32.0-42.9°C · 精度 ±0.2°C', '件', '件',
         'PRODUCT', 277, 'ITEM_TYPE_0092', '产成品',
         '家用红外测温枪，轻巧便携，一键测量，适合家庭与社区场景。',
         NULL, 'thermometer', 60, 7
  UNION ALL SELECT 'IRG-SA-CTRL-001', '测温显示电控预装件',
         '主控 / 传感 / 显示预装', '件', '件',
         'ITEM', 276, 'ITEM_TYPE_0091', '半成品',
         '主控板、红外传感与显示模组已完成预装与联调，可直接用于整机组装。',
         '电控组件', 'pcb', 100, 6
  UNION ALL SELECT 'IRG-SA-TRIG-001', '扳机侧键手柄预装件',
         '前壳 / 扳机 / 侧键预装', '件', '件',
         'ITEM', 276, 'ITEM_TYPE_0091', '半成品',
         '前壳、扳机与侧键已完成预装，按键手感统一，装配公差稳定。',
         '结构件', 'trigger', 100, 6
  UNION ALL SELECT 'IRG-SA-PCBA-001', '主板贴片组件',
         '四层板 · 已贴片 · 已烧录', '件', '件',
         'ITEM', 276, 'ITEM_TYPE_0091', '半成品',
         '四层板贴片组件，已完成贴片、检测与基础固件烧录，可按客户固件版本定制。',
         '电控组件', 'mainboard', 200, 8
  UNION ALL SELECT 'IRG-SA-SENS-001', '红外传感模组',
         '数字输出 · 出厂单点校准', '件', '件',
         'ITEM', 276, 'ITEM_TYPE_0091', '半成品',
         '红外传感模组，数字信号输出，出厂完成单点校准，可降低整机厂校准工时。',
         '电控组件', 'sensor', 300, 10
  UNION ALL SELECT 'IRG-SA-DISP-001', '显示驱动板',
         '段码屏驱动 · 低功耗', '件', '件',
         'ITEM', 276, 'ITEM_TYPE_0091', '半成品',
         '段码屏显示驱动板，低功耗设计，支持定制显示内容与背光方案。',
         '电控组件', 'mainboard', 200, 8
  UNION ALL SELECT 'IRG-ST-FRONT-001', '前壳注塑件',
         'ABS 阻燃 · 白色哑光', '件', '件',
         'ITEM', 276, 'ITEM_TYPE_0091', '半成品',
         '前壳注塑件，ABS 阻燃材料，白色哑光表面，支持丝印与配色定制。',
         '结构件', 'trigger', 500, 5
  UNION ALL SELECT 'IRG-ST-KEY-001', '侧键按键组件',
         '硅胶按键 · 手感一致', '件', '件',
         'ITEM', 276, 'ITEM_TYPE_0091', '半成品',
         '侧键按键组件，硅胶材质，按压手感一致，寿命测试通过 10 万次。',
         '结构件', 'trigger', 500, 5
) v
WHERE NOT EXISTS (SELECT 1 FROM md_item m WHERE m.item_code = v.item_code);

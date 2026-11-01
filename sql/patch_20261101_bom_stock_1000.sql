-- 演示数据补足：TG-FG-001 BOM 基本材料在「原料仓」各留 1000（2026-11-01）
-- 说明：仅更新每种材料在原料仓数量最大的那一行（主库存行），其他批次/库位行不动。
-- 执行环境：j2eedb_it（8081 演示库）

START TRANSACTION;

-- 执行前原值（备份/对照）
SELECT material_stock_id, item_code, warehouse_name, location_name, area_name, quantity_onhand
FROM wm_material_stock
WHERE material_stock_id IN (211,212,213,214,215,216,217,218,219,220,221,222,223,224);

UPDATE wm_material_stock SET quantity_onhand = 1000, update_by = 'admin', update_time = NOW()
WHERE material_stock_id IN (211,212,213,214,215,216,217,218,219,220,221,222,223,224);

-- 核对：14 个 BOM 基本材料的主库存行应全部为 1000
SELECT material_stock_id, item_code, item_name, location_name, quantity_onhand
FROM wm_material_stock
WHERE material_stock_id BETWEEN 211 AND 224
ORDER BY item_code;

COMMIT;

-- ================= 回滚 =================
-- 原值（2026-11-01 执行前）：
-- 211 TG-EL-001 主控PCBA        108
-- 212 TG-EL-002 红外传感器模块   72
-- 213 TG-EL-003 显示组件         68
-- 214 TG-EL-004 三键按键板       55
-- 215 TG-EL-005 蜂鸣器           70
-- 216 TG-EL-006 电池            160
-- 217 TG-ST-004 红外透镜         50
-- 218 TG-ST-001 上壳             74
-- 219 TG-ST-002 下壳             34
-- 220 TG-ST-003 扳机组件         38
-- 221 TG-ST-005 螺丝包           45
-- 222 TG-ST-006 防震泡棉         37
-- 223 TG-PK-001 说明书          113
-- 224 TG-PK-002 彩盒包装        111
--
-- 回滚 SQL（按需执行）：
-- UPDATE wm_material_stock SET quantity_onhand = 108 WHERE material_stock_id = 211;
-- UPDATE wm_material_stock SET quantity_onhand = 72  WHERE material_stock_id = 212;
-- UPDATE wm_material_stock SET quantity_onhand = 68  WHERE material_stock_id = 213;
-- UPDATE wm_material_stock SET quantity_onhand = 55  WHERE material_stock_id = 214;
-- UPDATE wm_material_stock SET quantity_onhand = 70  WHERE material_stock_id = 215;
-- UPDATE wm_material_stock SET quantity_onhand = 160 WHERE material_stock_id = 216;
-- UPDATE wm_material_stock SET quantity_onhand = 50  WHERE material_stock_id = 217;
-- UPDATE wm_material_stock SET quantity_onhand = 74  WHERE material_stock_id = 218;
-- UPDATE wm_material_stock SET quantity_onhand = 34  WHERE material_stock_id = 219;
-- UPDATE wm_material_stock SET quantity_onhand = 38  WHERE material_stock_id = 220;
-- UPDATE wm_material_stock SET quantity_onhand = 45  WHERE material_stock_id = 221;
-- UPDATE wm_material_stock SET quantity_onhand = 37  WHERE material_stock_id = 222;
-- UPDATE wm_material_stock SET quantity_onhand = 113 WHERE material_stock_id = 223;
-- UPDATE wm_material_stock SET quantity_onhand = 111 WHERE material_stock_id = 224;

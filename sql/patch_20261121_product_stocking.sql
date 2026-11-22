-- =============================================================================
-- 产品入库：库区/库位容量、上架混放策略与状态字典
-- 文件：sql/patch_20261121_product_stocking.sql
-- 目标库：j2eedb（MySQL 8.4）
--
-- 背景：
--   1) wm_storage_location / wm_storage_area 只有面积(area)与最大载重量(max_loa)，
--      没有可计算“余量”的数量上限字段，入库时无法给出库区余量，也无法做超量拦截。
--   2) 库位表已有 product_mixing / batch_mixing 开关（默认 Y），但没有全局兜底策略，
--      服务端也从未校验过混放。
--   3) 产品入库状态沿用通用 mes_order_status，展示为“审批中/已审批”，
--      与“待上架/已上架/已入库”的业务语义不一致。
--
-- 内容：
--   1) wm_storage_location 增加 max_qty（库区最大存放数量，NULL=不限）
--   2) wm_storage_area     增加 max_qty（库位最大存放数量，NULL=不限）
--   3) 为现有库区/库位回填初始容量（仅在该列为 NULL 时写入，不覆盖人工维护值）：
--        成品仓(FG)   500
--        原料仓(RAW) 5000
--        线边仓(LINE)1000
--        其它        10000
--      库位默认继承所属库区容量。
--   4) sys_config 增加 mes.wm.mixing.enforce（默认 true）：
--        为 true 时按库位 product_mixing / batch_mixing 开关校验混放；
--        false 时关闭服务端混放校验（保留前端提示）。
--   5) 新增字典 mes_product_recpt_status：待上架/已上架/已入库。
--
-- 幂等性：
--   * 加列前查 information_schema.columns，已存在则跳过；
--   * 容量回填只更新 max_qty IS NULL 的行，重复执行不会覆盖之后人工调整的容量；
--   * 字典/参数使用 WHERE NOT EXISTS 插入，重复执行不产生重复行。
--
-- 执行方式：
--   mysql --host=127.0.0.1 --port=3306 --user=root --password=*** \
--         --database=j2eedb --default-character-set=utf8mb4 \
--         -e "source E:/ktg/program/thermometer-mes-linkage/sql/patch_20261121_product_stocking.sql"
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- Stage 1：加列（幂等）
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS patch_add_column_if_absent;
DELIMITER $$
CREATE PROCEDURE patch_add_column_if_absent(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition VARCHAR(500)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = p_column
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL patch_add_column_if_absent('wm_storage_location', 'max_qty',
    'DECIMAL(18,3) NULL DEFAULT NULL COMMENT ''库区最大存放数量（按数量计），NULL=不限'' AFTER `area`');
CALL patch_add_column_if_absent('wm_storage_area', 'max_qty',
    'DECIMAL(18,3) NULL DEFAULT NULL COMMENT ''库位最大存放数量（按数量计），NULL=不限'' AFTER `max_loa`');

DROP PROCEDURE IF EXISTS patch_add_column_if_absent;

-- -----------------------------------------------------------------------------
-- Stage 2：回填初始容量（只填 NULL，不覆盖已配置值）
--   说明：这里的数值是可维护的初始配置，不是业务数据；后续在
--   “库区设置 / 库位设置”页面直接修改 max_qty 即可。
-- -----------------------------------------------------------------------------
UPDATE wm_storage_location l
JOIN wm_warehouse w ON w.warehouse_id = l.warehouse_id
SET l.max_qty = CASE w.warehouse_type
                    WHEN 'FG'   THEN 500
                    WHEN 'RAW'  THEN 5000
                    WHEN 'LINE' THEN 1000
                    ELSE 10000
                END
WHERE l.max_qty IS NULL;

UPDATE wm_storage_area a
JOIN wm_storage_location l ON l.location_id = a.location_id
SET a.max_qty = l.max_qty
WHERE a.max_qty IS NULL AND l.max_qty IS NOT NULL;

-- -----------------------------------------------------------------------------
-- Stage 3：混放策略开关（服务端读取；默认开启校验）
-- -----------------------------------------------------------------------------
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '产品上架-是否启用库位混放校验', 'mes.wm.mixing.enforce', 'true', 'N', 'system', NOW(),
       'true=按库位 product_mixing/batch_mixing 开关校验；false=关闭服务端混放校验'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'mes.wm.mixing.enforce');

-- -----------------------------------------------------------------------------
-- Stage 4：产品入库专用状态字典
-- -----------------------------------------------------------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '产品入库单状态', 'mes_product_recpt_status', '0', 'system', NOW(), '待上架/已上架/已入库'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mes_product_recpt_status');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '草稿',   'PREPARE',  'mes_product_recpt_status', 'info',    'Y', '0', 'system', NOW(), '可编辑入库行'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_product_recpt_status' AND dict_value = 'PREPARE');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '待上架', 'APPROVING','mes_product_recpt_status', 'warning', 'N', '0', 'system', NOW(), '需在库位完成上架'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_product_recpt_status' AND dict_value = 'APPROVING');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '已上架', 'APPROVED', 'mes_product_recpt_status', 'primary', 'N', '0', 'system', NOW(), '上架数量已齐，可过账入库'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_product_recpt_status' AND dict_value = 'APPROVED');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '已入库', 'FINISHED', 'mes_product_recpt_status', 'success', 'N', '0', 'system', NOW(), '已完成过账'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_product_recpt_status' AND dict_value = 'FINISHED');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '已取消', 'CANCELED', 'mes_product_recpt_status', 'danger',  'N', '0', 'system', NOW(), '已撤销'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_product_recpt_status' AND dict_value = 'CANCELED');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time, remark)
SELECT 6, '已确认', 'CONFIRMED','mes_product_recpt_status', 'success', 'N', '0', 'system', NOW(), '历史单据状态（保留展示）'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_product_recpt_status' AND dict_value = 'CONFIRMED');

-- -----------------------------------------------------------------------------
-- Stage 5：校验输出
-- -----------------------------------------------------------------------------
SELECT l.location_id, l.location_code, l.location_name, w.warehouse_type,
       l.max_qty,
       IFNULL((SELECT SUM(s.quantity_onhand) FROM wm_material_stock s WHERE s.location_id = l.location_id), 0) AS occupied,
       l.max_qty - IFNULL((SELECT SUM(s.quantity_onhand) FROM wm_material_stock s WHERE s.location_id = l.location_id), 0) AS remaining
FROM wm_storage_location l
JOIN wm_warehouse w ON w.warehouse_id = l.warehouse_id
ORDER BY l.location_id;

SELECT a.area_id, a.area_code, a.area_name, a.max_qty,
       IFNULL((SELECT SUM(s.quantity_onhand) FROM wm_material_stock s WHERE s.area_id = a.area_id), 0) AS occupied,
       a.max_qty - IFNULL((SELECT SUM(s.quantity_onhand) FROM wm_material_stock s WHERE s.area_id = a.area_id), 0) AS remaining
FROM wm_storage_area a
ORDER BY a.area_id;

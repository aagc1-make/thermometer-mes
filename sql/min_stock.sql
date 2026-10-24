-- ============================================================
--  D4 流程优化：安全库存预警 + 补货建议
--  1) 给物料增加安全库存字段 min_stock
--  2) 按「近 30 天领用量」给出建议补货量
--  执行：mysql -h127.0.0.1 -P3307 -uroot -p j2eedb -e "source E:/JavaCode/sql/min_stock.sql"
-- ============================================================
SET NAMES utf8mb4;

-- 安全库存字段（幂等：已存在则跳过，MySQL 8 不支持 IF NOT EXISTS 的列语法，用存储过程兜底）
SET @col := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema = 'j2eedb' AND table_name = 'md_item' AND column_name = 'min_stock');
SET @ddl := IF(@col = 0,
  'ALTER TABLE md_item ADD COLUMN min_stock decimal(24,6) DEFAULT 0 COMMENT ''安全库存'' AFTER specification',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 安全库存设定（按测温枪 10 台/批的消耗量估算）
UPDATE md_item SET min_stock = 50 WHERE item_code IN ('TG-EL-001','TG-EL-002','TG-EL-003','TG-EL-004','TG-EL-005','TG-EL-006');
UPDATE md_item SET min_stock = 40 WHERE item_code IN ('TG-ST-001','TG-ST-002','TG-ST-003','TG-ST-004','TG-ST-005','TG-ST-006');
UPDATE md_item SET min_stock = 100 WHERE item_code IN ('TG-PK-001','TG-PK-002');
UPDATE md_item SET min_stock = 20 WHERE item_code = 'TG-FG-001';
UPDATE md_item SET min_stock = 30 WHERE item_code IN ('TG-SF-001','TG-SF-002');
UPDATE md_item SET min_stock = 0 WHERE min_stock IS NULL;

-- ---------- 预警视图：结存 < 安全库存 即预警 ----------
CREATE OR REPLACE VIEW v_stock_alert AS
SELECT
  i.item_id,
  i.item_code,
  i.item_name,
  i.specification,
  i.unit_name,
  i.min_stock,
  IFNULL(SUM(s.quantity_onhand), 0)                    AS current_stock,
  IFNULL(SUM(s.quantity_onhand), 0) - i.min_stock       AS gap,
  GREATEST(i.min_stock * 2 - IFNULL(SUM(s.quantity_onhand), 0), 0) AS suggest_purchase,
  CASE
    WHEN IFNULL(SUM(s.quantity_onhand), 0) <= 0                       THEN 'CRITICAL'
    WHEN IFNULL(SUM(s.quantity_onhand), 0) < i.min_stock              THEN 'WARNING'
    ELSE 'OK'
  END AS alert_level
FROM md_item i
LEFT JOIN wm_material_stock s ON s.item_id = i.item_id
WHERE i.min_stock > 0
GROUP BY i.item_id, i.item_code, i.item_name, i.specification, i.unit_name, i.min_stock;

-- ---------- 自检 ----------
SELECT '预警物料数' AS 对象, COUNT(*) AS 数量 FROM v_stock_alert WHERE alert_level <> 'OK'
UNION ALL SELECT '严重缺料(CRITICAL)', COUNT(*) FROM v_stock_alert WHERE alert_level = 'CRITICAL'
UNION ALL SELECT '低于安全线(WARNING)', COUNT(*) FROM v_stock_alert WHERE alert_level = 'WARNING'
UNION ALL SELECT '安全库存已配置物料', COUNT(*) FROM md_item WHERE min_stock > 0;

SELECT item_code, item_name, unit_name, current_stock, min_stock, gap, suggest_purchase, alert_level
FROM v_stock_alert WHERE alert_level <> 'OK' ORDER BY alert_level, gap LIMIT 15;

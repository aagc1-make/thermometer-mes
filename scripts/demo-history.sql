-- ============================================================================
--  驾驶舱演示历史数据（可重复执行，所有造数行都以 HIST 前缀/备注标记）
--  目的：补足「时间跨度」与「状态多样性」，让 4 张图表有真实厚度
--  执行：mysql --host=... --database=j2eedb -e "source E:/JavaCode/scripts/demo-history.sql"
-- ============================================================================
SET NAMES utf8mb4;

-- ---------- 0. 清理历史造数 ----------
DELETE FROM pro_workorder WHERE workorder_code LIKE 'HIST-%';
DELETE FROM pro_feedback   WHERE feedback_code LIKE 'HIST-%';
DELETE FROM wm_material_stock WHERE batch_code LIKE 'HIST-%';

-- ---------- 1. 设备状态：制造 生产中 / 维修中 / 停机 / 保养中 四种分布 ----------
UPDATE dv_machinery SET status = 'WORKING' WHERE machinery_code = 'TG-MC-001';
UPDATE dv_machinery SET status = 'REPAIR'  WHERE machinery_code = 'TG-MC-002';
UPDATE dv_machinery SET status = 'WORKING' WHERE machinery_code = 'TG-MC-003';
UPDATE dv_machinery SET status = 'WORKING' WHERE machinery_code = 'TG-MC-004';
UPDATE dv_machinery SET status = 'STOP'    WHERE machinery_code = 'TG-MC-005';

-- ---------- 2. 工单：4 种状态、不同日期，供「工单进度」图分类 ----------
SET @wo := (SELECT workorder_id FROM pro_workorder ORDER BY workorder_id LIMIT 1);

INSERT INTO pro_workorder
  (workorder_code, workorder_name, order_source, source_code, product_id, product_code, product_name,
   product_spc, unit_of_measure, unit_name, quantity, quantity_produced, quantity_changed, quantity_scheduled,
   client_id, client_code, client_name, request_date, parent_id, ancestors, status, remark,
   create_by, create_time, workorder_type, batch_code)
SELECT CONCAT('HIST-', v.code), CONCAT(v.pname, ' ', v.qty, ' ', v.unit),
       p.order_source, v.code, p.product_id, p.product_code, v.pname,
       p.product_spc, p.unit_of_measure, p.unit_name, v.qty, v.done, 0, 0,
       p.client_id, p.client_code, p.client_name,
       DATE_SUB(CURDATE(), INTERVAL v.d DAY), p.parent_id, p.ancestors, v.st, 'HIST演示历史工单',
       'admin', DATE_SUB(NOW(), INTERVAL v.d DAY), p.workorder_type, CONCAT('HISTB-', v.code)
FROM pro_workorder p
JOIN (
  SELECT 'WO01' code, '测温显示电控预装件' pname, 20 qty, 20 done, 'FINISHED' st, 12 d, '套' unit
  UNION ALL SELECT 'WO02', '扳机与侧键手柄预装件', 20, 20, 'FINISHED', 10, '套'
  UNION ALL SELECT 'WO03', '红外测温枪整机', 15, 15, 'FINISHED', 8, '台'
  UNION ALL SELECT 'WO04', '红外测温枪整机', 30,  0, 'PREPARE', 5, '台'
  UNION ALL SELECT 'WO05', '测温显示电控预装件', 25,  6, 'CONFIRMED', 3, '套'
  UNION ALL SELECT 'WO06', '红外测温枪整机', 10,  0, 'CANCELED', 2, '台'
) v
WHERE p.workorder_id = @wo;

-- ---------- 3. 报工：近 14 天，每天 2 条，合格数有起伏，供「产量趋势」图 ----------
SET @fb := (SELECT record_id FROM pro_feedback ORDER BY record_id LIMIT 1);

INSERT INTO pro_feedback
  (feedback_type, feedback_code, workstation_id, workstation_code, workstation_name,
   workorder_id, workorder_code, workorder_name, route_id, route_code,
   process_id, process_code, process_name, task_id, task_code,
   item_id, item_code, item_name, unit_of_measure, unit_name, specification,
   quantity, quantity_feedback, quantity_qualified, quantity_unquanlified, quantity_uncheck,
   quantity_labor_scrap, quantity_material_scrap, quantity_other_scrap,
   user_name, nick_name, feedback_channel, feedback_time, record_user, record_nick, status, remark,
   create_by, create_time)
SELECT 'SELF', CONCAT('HIST-FB-', v.n, '-', DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL v.d DAY), '%m%d')),
       f.workstation_id, f.workstation_code, f.workstation_name,
       f.workorder_id, f.workorder_code, f.workorder_name, f.route_id, f.route_code,
       f.process_id, f.process_code, f.process_name, f.task_id, f.task_code,
       f.item_id, f.item_code, f.item_name, f.unit_of_measure, f.unit_name, f.specification,
       v.q, v.q, v.q, 0, 0, 0, 0, 0,
       'operator', '赵操作', 'PC',
       TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL v.d DAY), '10:30:00'),
       'operator', '赵操作', 'FINISHED', 'HIST演示历史报工',
       'admin', DATE_SUB(NOW(), INTERVAL v.d DAY)
FROM pro_feedback f
JOIN (
  SELECT 1 d, 46 q, 1 n
  UNION ALL SELECT 1, 34, 2
  UNION ALL SELECT 2, 52, 1
  UNION ALL SELECT 3, 38, 1
  UNION ALL SELECT 4, 41, 1
  UNION ALL SELECT 5, 55, 1
  UNION ALL SELECT 6, 33, 1
  UNION ALL SELECT 7, 60, 1
  UNION ALL SELECT 8, 47, 1
  UNION ALL SELECT 9, 42, 1
  UNION ALL SELECT 10, 51, 1
  UNION ALL SELECT 11, 36, 1
  UNION ALL SELECT 12, 58, 1
  UNION ALL SELECT 13, 44, 1
) v
WHERE f.record_id = @fb;

-- ---------- 4. 库存：原料仓补足常用件，成品仓留一批待发库存 ----------
UPDATE wm_material_stock
SET quantity_onhand = CAST(CASE item_code
      WHEN 'TG-EL-001' THEN 46 WHEN 'TG-EL-002' THEN 42 WHEN 'TG-EL-003' THEN 38
      WHEN 'TG-EL-004' THEN 35 WHEN 'TG-EL-005' THEN 40 WHEN 'TG-EL-006' THEN 80
      WHEN 'TG-ST-001' THEN 33 WHEN 'TG-ST-002' THEN 31 WHEN 'TG-ST-003' THEN 28
      WHEN 'TG-ST-004' THEN 30 WHEN 'TG-ST-005' THEN 52 WHEN 'TG-ST-006' THEN 44
      WHEN 'TG-PK-001' THEN 60 WHEN 'TG-PK-002' THEN 58 ELSE quantity_onhand END AS DECIMAL(24,6))
WHERE warehouse_code = 'WH-RAW';

-- 成品仓：克隆整机库存行，留 24 台未发货（供库存水位与预警展示）
INSERT INTO wm_material_stock
  (item_id, item_code, item_name, specification, unit_of_measure, unit_name, batch_code,
   warehouse_id, warehouse_code, warehouse_name, location_id, location_code, location_name,
   area_id, area_code, area_name, quantity_onhand, frozen_flag, create_by, create_time, production_date)
SELECT item_id, item_code, item_name, specification, unit_of_measure, unit_name, 'HIST-FG-01',
       9703, 'WH-FG', '成品仓', 9718, 'L-FG', '成品区', 9728, 'FG-01', '成品库位',
       24, 'N', 'admin', NOW(), DATE_SUB(CURDATE(), INTERVAL 2 DAY)
FROM wm_material_stock
WHERE item_code = 'TG-FG-001'
LIMIT 1;

-- ---------- 5. 自检 ----------
SELECT '历史工单' AS 对象, COUNT(*) AS 数量 FROM pro_workorder WHERE workorder_code LIKE 'HIST-%'
UNION ALL SELECT '历史报工', COUNT(*) FROM pro_feedback WHERE feedback_code LIKE 'HIST-%'
UNION ALL SELECT '成品仓库存', COUNT(*) FROM wm_material_stock WHERE batch_code = 'HIST-FG-01'
UNION ALL SELECT '工单状态种类', COUNT(DISTINCT status) FROM pro_workorder
UNION ALL SELECT '报工日期跨度', COUNT(DISTINCT DATE(feedback_time)) FROM pro_feedback;

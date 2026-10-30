-- =====================================================================
-- 出货检验（OQC）隔离库全链验证夹具
-- 目标：在【隔离库】中准备一条"销售单已拣货待检"的可执行数据，
--       用于验证 completeOqc 的并发、事务回滚、数量缩减与库存流水。
-- 前置：使用独立 schema（建议 j2eedb_it），由 sql/deploy/01_schema.sql 建库，
--       不要在当前业务库 j2eedb 上执行。
-- 编码：UTF-8；MySQL 8。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 0. 硬性护栏（第10轮复审修正）
--    要求：库名与依赖校验必须发生在**任何持久 DDL 之前**，
--          且不得在业务库里创建存储过程。
--    实现：只用会话级临时表 + CHECK 约束触发失败，不产生任何持久对象。
--          （临时表随会话结束自动消失，业务库不会留下过程/表）
-- ---------------------------------------------------------------------

-- 0.1 先打印诊断信息（失败时用于判断原因）
SELECT DATABASE() AS 当前库;

SELECT '客户' AS 项, COUNT(*) AS 数量 FROM md_client WHERE client_id = 9751
UNION ALL SELECT '成品物料', COUNT(*) FROM md_item WHERE item_id = 9201
UNION ALL SELECT '成品仓', COUNT(*) FROM wm_warehouse WHERE warehouse_id = 9703
UNION ALL SELECT '成品库区', COUNT(*) FROM wm_storage_location WHERE location_id = 9718
UNION ALL SELECT '成品库位', COUNT(*) FROM wm_storage_area WHERE area_id = 9728
UNION ALL SELECT 'OQC检验模板(按产品)', COUNT(*) FROM qc_template_product WHERE item_id = 9201;

-- 0.2 硬性拦截：任一条件不满足 → CHECK 约束报错，脚本立即中止
DROP TEMPORARY TABLE IF EXISTS it_fixture_guard;
CREATE TEMPORARY TABLE it_fixture_guard (
    ok TINYINT NOT NULL,
    CONSTRAINT ck_it_fixture_only_isolated_db_with_master_data CHECK (ok = 1)
);

INSERT INTO it_fixture_guard (ok)
SELECT IF(
         DATABASE() = 'j2eedb_it'
         AND (SELECT COUNT(*) FROM md_client WHERE client_id = 9751) > 0
         AND (SELECT COUNT(*) FROM md_item WHERE item_id = 9201) > 0
         AND (SELECT COUNT(*) FROM wm_warehouse WHERE warehouse_id = 9703) > 0
         AND (SELECT COUNT(*) FROM wm_storage_location WHERE location_id = 9718) > 0
         AND (SELECT COUNT(*) FROM wm_storage_area WHERE area_id = 9728) > 0
         AND (SELECT COUNT(*) FROM qc_template_product WHERE item_id = 9201) > 0,
         1, 0);

DROP TEMPORARY TABLE it_fixture_guard;

SELECT DATABASE() AS 已确认隔离库且主数据齐全;


-- ---------------------------------------------------------------------
-- 1. 清理上次夹具（幂等，可反复执行）
--    仅删除本脚本命名空间内的数据：代码前缀 IT-
-- ---------------------------------------------------------------------
DELETE d FROM wm_product_sales_detail d
  JOIN wm_product_sales_line l ON l.line_id = d.line_id
  JOIN wm_product_sales s ON s.sales_id = l.sales_id
 WHERE s.sales_code LIKE 'IT-%';

DELETE l FROM wm_product_sales_line l
  JOIN wm_product_sales s ON s.sales_id = l.sales_id
 WHERE s.sales_code LIKE 'IT-%';

DELETE FROM wm_product_sales WHERE sales_code LIKE 'IT-%';

DELETE ol FROM qc_oqc_line ol
  JOIN qc_oqc o ON o.oqc_id = ol.oqc_id
 WHERE o.oqc_code LIKE 'IT-OQC-%';
DELETE FROM qc_oqc WHERE oqc_code LIKE 'IT-OQC-%';

DELETE FROM wm_transaction WHERE source_doc_code LIKE 'IT-%';

DELETE FROM wm_material_stock WHERE batch_code LIKE 'IT-BAT-%';
DELETE FROM wm_batch WHERE batch_code LIKE 'IT-BAT-%';


-- ---------------------------------------------------------------------
-- 2. 依赖主数据复核
--    第 0.2 步已用临时表 + CHECK 硬校验并中止；这里只是装载前的可视化确认
-- ---------------------------------------------------------------------
SELECT '客户' AS 项, COUNT(*) AS 数量 FROM md_client WHERE client_id = 9751
UNION ALL SELECT '成品物料', COUNT(*) FROM md_item WHERE item_id = 9201
UNION ALL SELECT '成品仓', COUNT(*) FROM wm_warehouse WHERE warehouse_id = 9703
UNION ALL SELECT '成品库区', COUNT(*) FROM wm_storage_location WHERE location_id = 9718
UNION ALL SELECT '成品库位', COUNT(*) FROM wm_storage_area WHERE area_id = 9728
UNION ALL SELECT 'OQC检验模板(按产品)', COUNT(*) FROM qc_template_product WHERE item_id = 9201;


-- ---------------------------------------------------------------------
-- 3. 批次 + 成品库存（20 台，分两个批次，便于验证"保留顺序"）
-- ---------------------------------------------------------------------
INSERT INTO wm_batch (batch_id, batch_code, item_id, item_code, item_name,
                      produce_date, expire_date, lot_number, remark, create_by, create_time)
VALUES (990001, 'IT-BAT-001', 9201, 'TG-FG-001', '红外测温枪整机',
        '2026-10-01', '2028-10-01', 'IT-LOT-001', 'OQC夹具批次A', 'fixture', NOW()),
       (990002, 'IT-BAT-002', 9201, 'TG-FG-001', '红外测温枪整机',
        '2026-10-10', '2028-10-10', 'IT-LOT-002', 'OQC夹具批次B', 'fixture', NOW());

INSERT INTO wm_material_stock
  (material_stock_id, item_id, item_code, item_name, specification,
   unit_of_measure, unit_name, batch_id, batch_code,
   warehouse_id, warehouse_code, warehouse_name,
   location_id, location_code, location_name,
   area_id, area_code, area_name,
   quantity_onhand, create_by, create_time)
VALUES
  (990001, 9201, 'TG-FG-001', '红外测温枪整机', '红外测温枪 工业级',
   'PCS', '台', 990001, 'IT-BAT-001',
   9703, 'WH-FG', '成品仓', 9718, 'LOC-FG-01', '成品区A',
   9728, 'AREA-FG-01', '成品存货区', 12.0000, 'fixture', NOW()),
  (990002, 9201, 'TG-FG-001', '红外测温枪整机', '红外测温枪 工业级',
   'PCS', '台', 990002, 'IT-BAT-002',
   9703, 'WH-FG', '成品仓', 9718, 'LOC-FG-01', '成品区A',
   9728, 'AREA-FG-01', '成品存货区', 8.0000, 'fixture', NOW());


-- ---------------------------------------------------------------------
-- 4. 销售单（状态 UNSTOCK＝待拣货，已拣货待提交待检）
-- ---------------------------------------------------------------------
INSERT INTO wm_product_sales
  (sales_id, sales_code, sales_name, client_id, client_code, client_name,
   sales_date, status, remark, create_by, create_time)
VALUES (990001, 'IT-SO-001', 'OQC隔离库验证销售单', 9751, 'C-TG-001', '华东医疗器械经销有限公司',
        '2026-10-27', 'UNSTOCK', 'OQC夹具：可反复执行', 'fixture', NOW());

-- 出库行：20 台，需出货检验
-- 注意：wm_product_sales_line 只有物料/批次字段，没有仓库库位字段
--      （库位信息在拣货明细 wm_product_sales_detail 上）
INSERT INTO wm_product_sales_line
  (line_id, sales_id, material_stock_id, item_id, item_code, item_name, specification,
   unit_of_measure, unit_name, quantity_sales, batch_id, batch_code,
   oqc_check, quality_status, remark, create_by, create_time)
VALUES (990001, 990001, 990001, 9201, 'TG-FG-001', '红外测温枪整机', '红外测温枪 工业级',
        'PCS', '台', 20.0000, 990001, 'IT-BAT-001',
        'Y', NULL, 'OQC夹具行', 'fixture', NOW());

-- 拣货明细：批次A 12 台 + 批次B 8 台（顺序即保留顺序，detail_id 升序）
INSERT INTO wm_product_sales_detail
  (detail_id, line_id, sales_id, material_stock_id, item_id, item_code, item_name,
   specification, unit_of_measure, unit_name, quantity,
   batch_id, batch_code, warehouse_id, warehouse_code, warehouse_name,
   location_id, location_code, location_name, area_id, area_code, area_name,
   remark, create_by, create_time)
VALUES
  (990001, 990001, 990001, 990001, 9201, 'TG-FG-001', '红外测温枪整机',
   '红外测温枪 工业级', 'PCS', '台', 12.0000,
   990001, 'IT-BAT-001', 9703, 'WH-FG', '成品仓',
   9718, 'LOC-FG-01', '成品区A', 9728, 'AREA-FG-01', '成品存货区',
   'OQC夹具拣货A', 'fixture', NOW()),
  (990002, 990001, 990001, 990002, 9201, 'TG-FG-001', '红外测温枪整机',
   '红外测温枪 工业级', 'PCS', '台', 8.0000,
   990002, 'IT-BAT-002', 9703, 'WH-FG', '成品仓',
   9718, 'LOC-FG-01', '成品区A', 9728, 'AREA-FG-01', '成品存货区',
   'OQC夹具拣货B', 'fixture', NOW());


-- ---------------------------------------------------------------------
-- 5. OQC 草稿（来源＝上面这张销售单与行）
-- ---------------------------------------------------------------------
INSERT INTO qc_oqc
  (oqc_id, oqc_code, oqc_name, template_id,
   source_doc_id, source_doc_code, source_doc_type, source_line_id,
   client_id, client_code, client_name,
   item_id, item_code, item_name, specification,
   unit_of_measure,
   quantity_out, quantity_check, quantity_qualified, quantity_unqualified,
   check_result, status, remark, create_by, create_time)
SELECT 990001, 'IT-OQC-001', 'OQC隔离库验证单', tp.template_id,
       990001, 'IT-SO-001', 'PRODUCT_SALES', 990001,
       9751, 'C-TG-001', '华东医疗器械经销有限公司',
       9201, 'TG-FG-001', '红外测温枪整机', '红外测温枪 工业级',
       'PCS',
       20.0000, 20.0000, 0.0000, 0.0000,
       NULL, 'PREPARE', 'OQC夹具：数量待填写', 'fixture', NOW()
  FROM qc_template_product tp
 WHERE tp.item_id = 9201
 LIMIT 1;


-- ---------------------------------------------------------------------
-- 6. 夹具自检（期望：库存 20 / 行 20 / 拣货合计 20 / OQC 1 张 PREPARE）
-- ---------------------------------------------------------------------
SELECT '成品库存' AS 项,
       (SELECT SUM(quantity_onhand) FROM wm_material_stock WHERE material_stock_id IN (990001,990002)) AS 值
UNION ALL SELECT '销售行数量',
       (SELECT quantity_sales FROM wm_product_sales_line WHERE line_id = 990001)
UNION ALL SELECT '拣货合计',
       (SELECT SUM(quantity) FROM wm_product_sales_detail WHERE line_id = 990001)
UNION ALL SELECT 'OQC草稿',
       (SELECT COUNT(*) FROM qc_oqc WHERE oqc_code = 'IT-OQC-001' AND status = 'PREPARE')
UNION ALL SELECT '销售单状态',
       (SELECT COUNT(*) FROM wm_product_sales WHERE sales_code = 'IT-SO-001' AND status = 'UNSTOCK');

-- =====================================================================
-- 验证步骤（第11轮复审修正：按真实接口路径与真实顺序）
--
-- 夹具装载后状态：销售单 IT-SO-001 = UNSTOCK（已拣货待提交）
--
-- 接口约定（与源码一致）：
--   编辑销售单   PUT /mes/wm/productsales            （无 ID 路径，body 带 salesId/status）
--   完成出货检验 PUT /mes/qc/oqc
--   执行销售出库 PUT /mes/wm/productsales/{salesId}  （路径带 ID，如 /mes/wm/productsales/990001）
--
-- 正确顺序（不可颠倒）：
--   UNSTOCK ──①提交待检──> UNCHECK ──②完成OQC──> UNSHIPPING ──③置待执行──> UNEXECUTE ──④执行──> FINISHED
--
--   ① 到 ② 之间不能跳过：completeOqc 只接受来源销售单为 UNCHECK，
--      直接置 UNEXECUTE 会抛"来源销售单当前不在待检状态"。
--
-- 本次隔离库实测覆盖：OQC 完成 → 行质量回写 → 数量缩减 → 库存流水
-- 本次未覆盖：发货通知单、运单（发运）环节
--
-- ---------------------------------------------------------------------
-- 步骤 1｜提交待检（UNSTOCK → UNCHECK）
--   PUT /mes/wm/productsales
--   {
--     "salesId": 990001,
--     "status": "UNSHIPPING"
--   }
--   预期：控制器调用 submitForInspection(990001)
--         → 返回 1（存在需检行）
--         → 主单 UNSTOCK → UNCHECK
--         → 出库行 quality_status: NULL → NT
--   校验 SQL：
--     SELECT status FROM wm_product_sales WHERE sales_code='IT-SO-001';
--     SELECT quality_status FROM wm_product_sales_line WHERE line_id=990001;
--
-- 步骤 2｜完成 OQC（UNCHECK → UNSHIPPING / CANCELED，分四个用例，每个用例前重装夹具）
--   PUT /mes/qc/oqc
--   { "oqcId": 990001, "oqcCode": "IT-OQC-001",
--     "sourceDocId": 990001, "sourceLineId": 990001, "itemId": 9201,
--     "status": "FINISHED", "checkResult": "ACCEPT",
--     "quantityCheck": 20, "quantityQualified": 5, "quantityUnqualified": 15 }
--
--   用例 1｜部分合格（20 检、5 合格、15 不合格、ACCEPT）
--     实测结果：销售行 20→5；只保留 detail_id=990001 的 5，detail_id=990002 删除；
--              checkQuantity 通过；主单 → UNSHIPPING
--   用例 2｜全 NG（20 检、0 合格、20 不合格、REJECT）
--     实测结果：行 quality_status=NG；主单 → CANCELED；行与拣货数量均保持 20
--   用例 3｜0 合格却判 ACCEPT（20 检、0 合格、20 不合格、ACCEPT）
--     实测结果：抛"合格数量为 0，不能按合格放行…"；qc_oqc 仍 PREPARE，数据不变
--   用例 4｜并发完成（同时发两次用例 1 的请求）
--     实测结果：恰好一次成功，另一次抛"已被其他请求完成"
--
-- 步骤 3｜置待执行（UNSHIPPING → UNEXECUTE，仅用例 1 合格后）
--   PUT /mes/wm/productsales            （无 ID 路径）
--   {
--     "salesId": 990001,
--     "status": "UNEXECUTE"
--   }
--   预期：主单 → UNEXECUTE，允许随后执行出库
--   校验 SQL：SELECT status FROM wm_product_sales WHERE sales_code='IT-SO-001';
--
-- 步骤 4｜执行销售出库（PUT 路径带 ID）
--   PUT /mes/wm/productsales/990001
--   实测结果：批次 A 库存 12→7，批次 B 保持 8，新增 1 条库存流水
--   校验 SQL：
--     SELECT batch_code, quantity_onhand FROM wm_material_stock
--      WHERE material_stock_id IN (990001,990002);
--     SELECT transaction_type, item_code, transaction_quantity, source_doc_code
--       FROM wm_transaction WHERE source_doc_code='IT-SO-001';
--
-- 步骤 5｜重装夹具进入下一个用例
--   重复执行本脚本即可（自带清理，幂等）
-- =====================================================================

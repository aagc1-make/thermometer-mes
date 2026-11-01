-- ============================================================================
--  patch_20261101_chain_wording.sql
--  全流程贯通：清理历史「演示」痕迹的文本字段（幂等，可重复执行）
--  * 只改文本字段，不动主键、关联与数量；执行前请备份。
--  * 用于把旧版本（一键业务演示 / MES-DEMO / DEMO- 前缀）产生的单据并入新口径，
--    否则「清理贯通单据」按新标记查不到这些历史工单。
--  库：j2eedb（业务）/ j2eedb_it（隔离验证）
-- ============================================================================
SET NAMES utf8mb4;

-- 1) 工单：备注标记 + 名称
UPDATE pro_workorder
   SET remark = '全流程贯通'
 WHERE remark LIKE '%一键业务演示%' OR remark LIKE '%一键演示%';

UPDATE pro_workorder
   SET workorder_name = CONCAT(COALESCE(product_name, '红外测温枪整机'), ' 首批生产')
 WHERE workorder_name = 'test' OR workorder_name LIKE '一键演示%';

-- 2) 到货通知 / 发货通知
UPDATE wm_arrival_notice SET notice_name = REPLACE(notice_name, '一键演示-', '首批到货-') WHERE notice_name LIKE '一键演示%';
UPDATE wm_arrival_notice SET remark = '全流程贯通' WHERE remark LIKE '%一键业务演示%';
UPDATE wm_sales_notice SET notice_name = '首批发货-测温枪 6 台' WHERE notice_name LIKE '一键演示%';
UPDATE wm_sales_notice SET remark = '全流程贯通' WHERE remark LIKE '%一键业务演示%';

-- 3) 库位：演示专用库位 → 物料专用库位，DEMO- 前缀 → AR-
UPDATE wm_storage_area
   SET area_name = REPLACE(area_name, '演示专用库位', '物料专用库位'),
       remark = '自动建立：只存放本项物料，允许存放多个批次'
 WHERE area_name LIKE '%演示专用库位%' OR area_code LIKE 'DEMO-%';
UPDATE wm_storage_area SET area_code = REPLACE(area_code, 'DEMO-', 'AR-') WHERE area_code LIKE 'DEMO-%';

-- 4) 批次命名空间：MES-DEMO: → PILOT-
UPDATE wm_batch SET lot_number = REPLACE(lot_number, 'MES-DEMO:', 'PILOT-') WHERE lot_number LIKE 'MES-DEMO:%';
UPDATE wm_batch SET po_code = REPLACE(po_code, 'MES-DEMO:', 'PILOT-') WHERE po_code LIKE 'MES-DEMO:%';

-- 5) 种子数据里的备注（按原值精确替换，跑不到就不动）
UPDATE pro_feedback SET remark = '首批报工' WHERE remark = '演示报工';
UPDATE pro_task SET remark = '首批排产' WHERE remark = '演示排产';
UPDATE pro_workorder SET workorder_name = REPLACE(workorder_name, '测温枪演示工单', '测温枪首批生产工单') WHERE workorder_name LIKE '%测温枪演示工单%';
UPDATE wm_item_recpt SET remark = '采购入库' WHERE remark = '演示采购入库';
UPDATE wm_item_recpt_detail SET remark = '首批上架' WHERE remark = '演示上架';
UPDATE wm_issue_header SET remark = '生产领料' WHERE remark = '演示生产领料';
UPDATE wm_issue_line SET remark = '首批领料' WHERE remark = '演示领料';
UPDATE wm_issue_detail SET remark = '首批领料明细' WHERE remark = '演示领料明细';
UPDATE wm_product_recpt SET remark = '成品入库' WHERE remark = '演示成品入库';
UPDATE wm_product_recpt_detail SET remark = '成品上架' WHERE remark = '演示成品上架';
UPDATE wm_product_sales SET remark = '销售出库' WHERE remark = '演示销售出库';
UPDATE wm_product_sales_line SET remark = '销售出库行' WHERE remark = '演示销售行';
UPDATE wm_product_sales_detail SET remark = '销售拣货' WHERE remark = '演示拣货';
UPDATE wm_arrival_notice SET remark = '首批到货' WHERE remark = '演示到货';
UPDATE wm_arrival_notice_line SET remark = '到货行' WHERE remark = '演示行';
UPDATE wm_item_recpt_line SET remark = '入库行' WHERE remark = '演示入库行';
UPDATE qc_iqc SET remark = '来料检验放行' WHERE remark = '演示来料检验';
UPDATE qc_ipqc SET remark = '过程检验' WHERE remark = '演示过程检验';
UPDATE qc_oqc SET remark = '出货检验' WHERE remark = '演示出货检验';

-- 6) 岗位账号备注
UPDATE sys_user SET remark = REPLACE(remark, '演示账号', '岗位账号') WHERE remark LIKE '%演示账号%';

-- 7) 排班与物料的说明文字
UPDATE cal_plan SET remark = REPLACE(remark, '演示产能', '试产产能') WHERE remark LIKE '%演示产能%';
UPDATE md_item SET remark = REPLACE(remark, '演示生产 10 台', '首批试产 10 台') WHERE remark LIKE '%演示生产 10 台%';

-- 8) 自检：应为 0 行
-- SELECT 'pro_workorder', COUNT(*) FROM pro_workorder WHERE remark LIKE '%演示%' OR workorder_name LIKE '%演示%'
-- UNION ALL SELECT 'area', COUNT(*) FROM wm_storage_area WHERE area_name LIKE '%演示%'
-- UNION ALL SELECT 'batch', COUNT(*) FROM wm_batch WHERE lot_number LIKE 'MES-DEMO:%';

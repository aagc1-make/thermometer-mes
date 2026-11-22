-- =============================================================================
-- 成品批次按工单绑定
-- 文件：sql/patch_20261121_batch_workorder_binding.sql
-- 目标库：j2eedb（MySQL 8.4）
--
-- 背景：
--   成品 TG-FG-001 的批次配置未把工单作为命名维度，导致同一天多个工单的产出
--   共用同一个批次（如 pc2026112100001），工单、入库、发货、出库的批次全部串在一起。
--   以工单为核心后，产出批次必须按工单区分，批次号沿用链号（pc2026112100002）。
--
-- 内容：
--   1) 成品 TG-FG-001（item_id=9201）批次配置打开 workorder_flag
--   2) 校验结果
--
-- 幂等性：固定更新，重复执行无副作用。
-- =============================================================================

SET NAMES utf8mb4;

UPDATE md_item_batch_config
SET workorder_flag = 'Y'
WHERE item_id = 9201 AND workorder_flag <> 'Y';

SELECT config_id, item_id, workorder_flag, produce_date_flag, co_code_flag
FROM md_item_batch_config
WHERE item_id = 9201;

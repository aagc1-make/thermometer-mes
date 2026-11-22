-- =============================================================================
-- 工单行回填 + 库存事务幂等键补丁
-- 文件：sql/patch_20261105_workorder_line_biz_key.sql
-- 目标库：j2eedb（MySQL 8.4）
-- 依据：doc/数据库设计/工单行与库存幂等键_20261105.md
--
-- 内容：
--   1) 为 pro_workorder_line 补 (workorder_id, line_id) 唯一键，供复合外键引用
--   2) 幂等确认 wm_transaction.biz_key 唯一索引 uk_txn_biz_key
--   3) 为“一工单一行项目”的历史工单补建默认行（幂等，已有行不重复建）
--   4) 回填 wm_batch.workorder_line_id：仅回填能由“工单+物料”唯一判定的历史批次
--   5) 增加复合外键 fk_batch_wo_line_pair，约束
--      wm_batch.workorder_id 必须与 workorder_line_id 所属工单一致
--   6) 输出无法唯一判定的清单（脚本可重复执行，第二次执行不会新增数据）
--
-- 说明：
--   * 历史 wm_transaction.biz_key 不做回填：旧库 transaction_quantity 为 double，
--     与 Java 端 BigDecimal 归一化后的字符串无法保证逐字一致，强行回填可能产生
--     “看似同键、实际不同”的伪幂等；这些历史事务保持 NULL 并在脚本末尾输出数量。
--   * 不创建触发器；幂等键由应用层 WmTransactionServiceImpl#buildBizKey 统一生成。
--
-- 执行方式：
--   mysql --host=127.0.0.1 --port=3306 --user=root --password=*** \
--         --database=j2eedb --default-character-set=utf8mb4 \
--         -e "source E:/ktg/program/thermometer-mes-linkage/sql/patch_20261105_workorder_line_biz_key.sql"
-- =============================================================================

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ktg2_add_unique;
DROP PROCEDURE IF EXISTS ktg2_add_fk;

DELIMITER $$

CREATE PROCEDURE ktg2_add_unique(IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_columns TEXT)
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = p_table AND index_name = p_index) = 0 THEN
    SET @ktg_ddl = CONCAT('ALTER TABLE `', p_table, '` ADD UNIQUE KEY `', p_index, '` (', p_columns, ')');
    PREPARE ktg_stmt FROM @ktg_ddl;
    EXECUTE ktg_stmt;
    DEALLOCATE PREPARE ktg_stmt;
  END IF;
END$$

CREATE PROCEDURE ktg2_add_fk(IN p_table VARCHAR(64), IN p_name VARCHAR(64)
                             , IN p_columns VARCHAR(200), IN p_ref_table VARCHAR(64)
                             , IN p_ref_columns VARCHAR(200), IN p_action VARCHAR(64))
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.referential_constraints
      WHERE constraint_schema = DATABASE() AND constraint_name = p_name) = 0 THEN
    SET @ktg_ddl = CONCAT('ALTER TABLE `', p_table, '` ADD CONSTRAINT `', p_name,
                          '` FOREIGN KEY (', p_columns, ') REFERENCES `', p_ref_table,
                          '` (', p_ref_columns, ') ', p_action);
    PREPARE ktg_stmt FROM @ktg_ddl;
    EXECUTE ktg_stmt;
    DEALLOCATE PREPARE ktg_stmt;
  END IF;
END$$

DELIMITER ;

-- =============================================================================
-- Stage 1：索引与唯一键
-- =============================================================================
-- 供复合外键引用的唯一键（line_id 本身是主键，因此该唯一键一定成立）
CALL ktg2_add_unique('pro_workorder_line', 'uk_wo_line_pair', '`workorder_id`, `line_id`');

-- 幂等键唯一索引（patch_20261102 已创建，此处仅做存在性确认）
CALL ktg2_add_unique('wm_transaction', 'uk_txn_biz_key', '`biz_key`');

SELECT 'STAGE 1 done: indexes ready' AS stage_result;

-- =============================================================================
-- Stage 2：为一工单一行项目的历史工单补建默认行
--   仅处理 product_id 有效且当前没有任何行的工单；可重复执行。
-- =============================================================================
INSERT INTO `pro_workorder_line`
  (`workorder_id`, `line_no`, `item_id`, `item_code`, `item_name`, `item_spc`
   , `unit_of_measure`, `unit_name`, `plan_quantity`
   , `produced_quantity`, `qualified_quantity`, `scrapped_quantity`
   , `batch_strategy`, `status`, `remark`, `create_by`, `create_time`)
SELECT w.`workorder_id`
     , 1
     , w.`product_id`
     , w.`product_code`
     , w.`product_name`
     , w.`product_spc`
     , w.`unit_of_measure`
     , w.`unit_name`
     , IFNULL(w.`quantity`, 0)
     , 0, 0, 0
     , 'SINGLE'
     , CASE w.`status`
         WHEN 'FINISHED' THEN 'FINISHED'
         WHEN 'CANCELED' THEN 'CANCELLED'
         ELSE 'PREPARE'
       END
     , '存量回填：一工单一行项目'
     , 'backfill'
     , NOW()
FROM `pro_workorder` w
WHERE w.`product_id` IS NOT NULL
  AND w.`product_id` <> 0
  AND NOT EXISTS (SELECT 1 FROM `pro_workorder_line` l WHERE l.`workorder_id` = w.`workorder_id`);

SELECT 'STAGE 2 done: default workorder lines backfilled' AS stage_result;

-- =============================================================================
-- Stage 3：回填 wm_batch.workorder_line_id
--   仅当“同一工单 + 同一物料”只有一条工单行时回填，其余保持 NULL。
--   可重复执行：只处理 workorder_line_id IS NULL 的行。
-- =============================================================================
UPDATE `wm_batch` b
JOIN (
    SELECT l.`workorder_id`
         , l.`item_id`
         , MIN(l.`line_id`) AS `line_id`
    FROM `pro_workorder_line` l
    WHERE l.`status` <> 'CANCELLED'
    GROUP BY l.`workorder_id`, l.`item_id`
    HAVING COUNT(*) = 1
) m ON m.`workorder_id` = b.`workorder_id` AND m.`item_id` = b.`item_id`
SET b.`workorder_line_id` = m.`line_id`
WHERE b.`workorder_line_id` IS NULL
  AND b.`workorder_id` IS NOT NULL;

SELECT 'STAGE 3 done: batch workorder_line_id backfilled' AS stage_result;

-- =============================================================================
-- Stage 4：复合外键（工单 + 工单行 必须成对匹配）
--   CHECK 约束无法跨表引用，这里用复合外键表达：
--   workorder_line_id 非空时，(workorder_id, workorder_line_id) 必须能在
--   pro_workorder_line(workorder_id, line_id) 中找到，即“行属于该工单”。
--   任一列为 NULL 时按 MATCH SIMPLE 语义跳过校验，兼容无工单的采购批次。
-- =============================================================================
CALL ktg2_add_fk('wm_batch', 'fk_batch_wo_line_pair'
                , '`workorder_id`, `workorder_line_id`'
                , 'pro_workorder_line', '`workorder_id`, `line_id`'
                , 'ON DELETE RESTRICT');

SELECT 'STAGE 4 done: composite fk ready' AS stage_result;

-- =============================================================================
-- Stage 5：无法唯一判定的清单（只读输出，不做强猜）
-- =============================================================================
SELECT 'BATCH_WITHDRAW_ORDER_WITHOUT_LINE' AS issue_type
     , b.`batch_id`
     , b.`batch_code`
     , b.`workorder_id`
     , b.`item_id`
FROM `wm_batch` b
WHERE b.`workorder_id` IS NOT NULL
  AND b.`workorder_line_id` IS NULL
ORDER BY b.`batch_id`;

SELECT 'WORKORDER_WITHOUT_LINE' AS issue_type
     , w.`workorder_id`
     , w.`workorder_code`
     , w.`product_id`
FROM `pro_workorder` w
WHERE NOT EXISTS (SELECT 1 FROM `pro_workorder_line` l WHERE l.`workorder_id` = w.`workorder_id`)
ORDER BY w.`workorder_id`;

SELECT 'AMBIGUOUS_OPEN_LINE' AS issue_type
     , l.`workorder_id`
     , l.`item_id`
     , COUNT(*) AS line_count
FROM `pro_workorder_line` l
WHERE l.`status` NOT IN ('FINISHED', 'CANCELLED')
GROUP BY l.`workorder_id`, l.`item_id`
HAVING COUNT(*) > 1
ORDER BY l.`workorder_id`, l.`item_id`;

SELECT 'TXN_WITHOUT_BIZ_KEY' AS issue_type
     , COUNT(*) AS txn_count
FROM `wm_transaction`
WHERE `biz_key` IS NULL;

-- =============================================================================
-- Stage 6：汇总
-- =============================================================================
SELECT 'SUMMARY' AS stage_result
     , (SELECT COUNT(*) FROM `pro_workorder` WHERE `product_id` IS NOT NULL AND `product_id` <> 0) AS workorder_with_product
     , (SELECT COUNT(*) FROM `pro_workorder_line`) AS workorder_line_total
     , (SELECT COUNT(*) FROM `wm_batch` WHERE `workorder_id` IS NOT NULL) AS batch_with_workorder
     , (SELECT COUNT(*) FROM `wm_batch` WHERE `workorder_line_id` IS NOT NULL) AS batch_with_line
     , (SELECT COUNT(*) FROM `wm_transaction` WHERE `biz_key` IS NOT NULL) AS txn_with_biz_key;

DROP PROCEDURE IF EXISTS ktg2_add_unique;
DROP PROCEDURE IF EXISTS ktg2_add_fk;

SELECT 'PATCH DONE' AS stage_result;

-- =============================================================================
-- 工单确认自动排产补丁：任务表幂等约束
-- 文件：sql/patch_20261121_workorder_auto_schedule.sql
-- 目标库：j2eedb（MySQL 8.4）
--
-- 背景：
--   工单确认时由 ProAutoSchedulingServiceImpl 按工艺路线一次性生成全部工序任务，
--   任务编号规则改为「工单号-两位工序序号」（如 gd2026111400002-01）。
--   为防止重复确认/并发确认生成重复任务，给 pro_task 增加
--   (workorder_id, process_id) 唯一约束；同一工单同一工序只允许一条任务。
--
-- 幂等性：
--   * 仅新增索引，不修改任何历史任务数据；
--   * 历史 rw2026... 任务编号保留不变，与新编号共存；
--   * 重复执行不会报错（索引已存在时跳过）。
--
-- 注意：
--   * 若历史库已存在同一工单同一工序的多条任务，本脚本会因唯一键创建失败而中止，
--     这是预期行为：请先人工确认这些重复任务的处理方式，不要静默删除历史数据。
--   * 执行前可先跑文末「重复数据检查」确认结果为空。
--
-- 执行方式：
--   mysql --host=127.0.0.1 --port=3306 --user=root --password=*** \
--         --database=j2eedb --default-character-set=utf8mb4 \
--         -e "source E:/ktg/program/thermometer-mes-linkage/sql/patch_20261121_workorder_auto_schedule.sql"
-- =============================================================================

SET NAMES utf8mb4;

-- 重复数据检查：期望 0 行。若非空，请先人工处理后再执行下面的索引创建。
SELECT workorder_id, process_id, COUNT(*) AS duplicate_count
FROM pro_task
GROUP BY workorder_id, process_id
HAVING COUNT(*) > 1;

DROP PROCEDURE IF EXISTS ktg_sched_add_index;

DELIMITER $$

CREATE PROCEDURE ktg_sched_add_index(IN p_table VARCHAR(64),
                                     IN p_index VARCHAR(64),
                                     IN p_columns TEXT,
                                     IN p_unique TINYINT)
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = p_table
        AND index_name = p_index) = 0 THEN
    SET @ktg_ddl = CONCAT('ALTER TABLE `', p_table, '` ADD ',
                          IF(p_unique = 1, 'UNIQUE ', ''), 'KEY `', p_index, '` (', p_columns, ')');
    PREPARE ktg_stmt FROM @ktg_ddl;
    EXECUTE ktg_stmt;
    DEALLOCATE PREPARE ktg_stmt;
  END IF;
END$$

DELIMITER ;

-- 幂等核心：同一工单同一工序只能存在一条任务
CALL ktg_sched_add_index('pro_task', 'uk_pro_task_wo_process', '`workorder_id`,`process_id`', 1);

-- 查询性能：按工单查任务（工单详情「生产任务」页签、报工下拉）
CALL ktg_sched_add_index('pro_task', 'idx_pro_task_workorder', '`workorder_id`', 0);

DROP PROCEDURE IF EXISTS ktg_sched_add_index;

-- -----------------------------------------------------------------------------
-- 单号校验放开「父工单号-工序序号」「父工单号-报工流水」两种复合格式
-- 原校验 fn_doc_code_valid 只允许 2位小写拼音+8位日期+5位流水，会把新单号判为非法。
-- 这里不改全局函数，只给 pro_task / pro_feedback 各建一个扩展校验函数并重建触发器。
-- -----------------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_pro_task_code_bi;
DROP TRIGGER IF EXISTS trg_pro_task_code_bu;
DROP TRIGGER IF EXISTS trg_pro_feedback_code_bi;
DROP TRIGGER IF EXISTS trg_pro_feedback_code_bu;
DROP FUNCTION IF EXISTS fn_task_code_valid;
DROP FUNCTION IF EXISTS fn_feedback_code_valid;

DELIMITER $$

-- 任务号：兼容历史 rw...，并允许「工单号-两位工序序号」，如 gd2026111400002-01
CREATE FUNCTION fn_task_code_valid(p_code VARCHAR(64)) RETURNS tinyint
    DETERMINISTIC
BEGIN
  IF fn_doc_code_valid(p_code) = 1 THEN
    RETURN 1;
  END IF;
  IF p_code COLLATE utf8mb4_bin REGEXP '^[a-z]{2}[0-9]{13}-[0-9]{2}$' THEN
    RETURN 1;
  END IF;
  RETURN 0;
END$$

-- 报工单号：兼容历史 bg...，并允许「工单号-报工流水」，如 gd2026111400002-bg2026112100006
CREATE FUNCTION fn_feedback_code_valid(p_code VARCHAR(64)) RETURNS tinyint
    DETERMINISTIC
BEGIN
  IF fn_doc_code_valid(p_code) = 1 THEN
    RETURN 1;
  END IF;
  IF p_code COLLATE utf8mb4_bin REGEXP '^[a-z]{2}[0-9]{13}-[a-z]{2}[0-9]{13}$' THEN
    RETURN 1;
  END IF;
  RETURN 0;
END$$

CREATE TRIGGER trg_pro_task_code_bi BEFORE INSERT ON pro_task
FOR EACH ROW
BEGIN
  IF fn_task_code_valid(NEW.task_code) = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '任务编号格式错误：历史格式或“工单号-两位工序序号”';
  END IF;
END$$

CREATE TRIGGER trg_pro_task_code_bu BEFORE UPDATE ON pro_task
FOR EACH ROW
BEGIN
  IF NOT (OLD.task_code <=> NEW.task_code) AND fn_task_code_valid(NEW.task_code) = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '任务编号格式错误：历史格式或“工单号-两位工序序号”';
  END IF;
END$$

CREATE TRIGGER trg_pro_feedback_code_bi BEFORE INSERT ON pro_feedback
FOR EACH ROW
BEGIN
  IF fn_feedback_code_valid(NEW.feedback_code) = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '报工单号格式错误：历史格式或“工单号-报工流水”';
  END IF;
END$$

CREATE TRIGGER trg_pro_feedback_code_bu BEFORE UPDATE ON pro_feedback
FOR EACH ROW
BEGIN
  IF NOT (OLD.feedback_code <=> NEW.feedback_code) AND fn_feedback_code_valid(NEW.feedback_code) = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '报工单号格式错误：历史格式或“工单号-报工流水”';
  END IF;
END$$

DELIMITER ;

-- 验证：应看到 uk_pro_task_wo_process 与 idx_pro_task_workorder
SELECT index_name, non_unique, seq_in_index, column_name
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'pro_task'
  AND index_name IN ('uk_pro_task_wo_process', 'idx_pro_task_workorder')
ORDER BY index_name, seq_in_index;

-- 验证：应看到两个扩展校验函数与四个触发器
SELECT routine_name FROM information_schema.routines
WHERE routine_schema = DATABASE() AND routine_name IN ('fn_task_code_valid', 'fn_feedback_code_valid');
SELECT trigger_name FROM information_schema.triggers
WHERE trigger_schema = DATABASE()
  AND trigger_name IN ('trg_pro_task_code_bi', 'trg_pro_task_code_bu', 'trg_pro_feedback_code_bi', 'trg_pro_feedback_code_bu');

-- 采购退供：把“一个来源只能有一张退供单”改为“一个来源同时只有一张进行中退供单”。
-- 目的：支持未入库不合格品的部分退供（4 + 6）与取消/删除后重新生成，同时保留并发防重。
-- 兼容：只新增生成列与索引；不改写历史单据。历史多张已完成退供单在新唯一键下为 NULL，不冲突。
-- 执行前：确认 DATABASE() 为隔离库（本脚本拒绝在库名为 j2eedb 时执行）。
-- 注意：MySQL DDL 隐式提交，无法整体回滚；回滚请执行 patch_20261029_vendor_return_partial_rollback.sql。

-- 防误执行（第一条语句）：库名不满足隔离库命名（*_it）时立即报错，
-- 不创建任何存储过程/表/索引，避免在业务库留下持久对象。
SELECT CASE WHEN DATABASE() REGEXP '_it$' THEN 1 ELSE (SELECT 1 UNION SELECT 2) END AS db_guard;

SELECT DATABASE() AS target_database;

DROP PROCEDURE IF EXISTS apply_vendor_return_partial_20261029;
DELIMITER $$
CREATE PROCEDURE apply_vendor_return_partial_20261029()
BEGIN
  IF DATABASE() IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '请先选择目标数据库';
  END IF;
  IF DATABASE() = 'j2eedb' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '禁止在主库 j2eedb 执行，请切换到隔离库';
  END IF;

  -- 前置检查：同一来源存在多张进行中单据时，唯一键无法建立，先人工核对
  IF EXISTS (
      SELECT attr1 FROM wm_rt_vendor
       WHERE status IN ('PREPARE','APPROVING','APPROVED')
         AND (attr1 LIKE 'AUTO:RTV:IQC:%' OR attr1 LIKE 'SRC:RECEIPT:%')
       GROUP BY attr1 HAVING COUNT(*) > 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '存在同一来源的多张进行中退供单，请先核对后再执行';
  END IF;

  -- 旧的“一个 IQC 永久一张”约束（若已应用）替换为“进行中唯一”
  IF EXISTS (SELECT 1 FROM information_schema.statistics
              WHERE table_schema = DATABASE() AND table_name = 'wm_rt_vendor'
                AND index_name = 'uk_vendor_iqc_return') THEN
    ALTER TABLE wm_rt_vendor DROP INDEX uk_vendor_iqc_return;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns
              WHERE table_schema = DATABASE() AND table_name = 'wm_rt_vendor'
                AND column_name = 'auto_iqc_return_key') THEN
    ALTER TABLE wm_rt_vendor DROP COLUMN auto_iqc_return_key;
  END IF;

  -- 进行中唯一键：PREPARE/APPROVING/APPROVED 时取来源键，其余状态为 NULL（可无限历史）
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                  WHERE table_schema = DATABASE() AND table_name = 'wm_rt_vendor'
                    AND column_name = 'auto_open_return_key') THEN
    ALTER TABLE wm_rt_vendor ADD COLUMN auto_open_return_key varchar(64)
      GENERATED ALWAYS AS (
        CASE WHEN status IN ('PREPARE','APPROVING','APPROVED')
                  AND (attr1 LIKE 'AUTO:RTV:IQC:%' OR attr1 LIKE 'SRC:RECEIPT:%')
             THEN attr1 ELSE NULL END) STORED;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                  WHERE table_schema = DATABASE() AND table_name = 'wm_rt_vendor'
                    AND index_name = 'uk_vendor_open_return') THEN
    ALTER TABLE wm_rt_vendor ADD UNIQUE KEY uk_vendor_open_return (auto_open_return_key);
  END IF;

  -- 来源检索索引（未入库按 IQC、已入库按入库单）
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                  WHERE table_schema = DATABASE() AND table_name = 'wm_rt_vendor'
                    AND index_name = 'idx_vendor_return_attr1') THEN
    ALTER TABLE wm_rt_vendor ADD KEY idx_vendor_return_attr1 (attr1(48), status);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                  WHERE table_schema = DATABASE() AND table_name = 'wm_rt_vendor_line'
                    AND index_name = 'idx_vendor_return_source') THEN
    ALTER TABLE wm_rt_vendor_line ADD KEY idx_vendor_return_source (attr2(64), rt_id, batch_id);
  END IF;
END$$
DELIMITER ;

CALL apply_vendor_return_partial_20261029();
DROP PROCEDURE apply_vendor_return_partial_20261029;

-- 复核
SELECT index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS cols
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'wm_rt_vendor'
 GROUP BY index_name ORDER BY index_name;

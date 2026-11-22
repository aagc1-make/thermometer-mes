-- 回滚 patch_20261029_vendor_return_partial.sql：恢复“一个 IQC 永久一张退供单”的唯一约束。
-- 前置检查：如果同一 IQC 已存在多张退供单（部分退供产生），旧唯一键无法建立，脚本会中止且不改动任何数据。
-- 执行顺序：先回滚应用代码（PreStockVendorReturnService/VendorReturnWorkflowService），再执行本脚本。

-- 防误执行（第一条语句）：库名不满足隔离库命名（*_it）时立即报错，
-- 不创建任何存储过程/表/索引，避免在业务库留下持久对象。
SELECT CASE WHEN DATABASE() REGEXP '_it$' THEN 1 ELSE (SELECT 1 UNION SELECT 2) END AS db_guard;

SELECT DATABASE() AS target_database;

DROP PROCEDURE IF EXISTS rollback_vendor_return_partial_20261029;
DELIMITER $$
CREATE PROCEDURE rollback_vendor_return_partial_20261029()
BEGIN
  IF DATABASE() IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '请先选择目标数据库';
  END IF;
  IF DATABASE() = 'j2eedb' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '禁止在主库 j2eedb 执行，请切换到隔离库';
  END IF;

  -- 旧约束要求“一个 IQC 只有一张”，先检查是否与现有数据冲突
  IF EXISTS (
      SELECT attr1 FROM wm_rt_vendor
       WHERE attr1 LIKE 'AUTO:RTV:IQC:%'
       GROUP BY attr1 HAVING COUNT(*) > 1) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = '同一 IQC 存在多张退供单（部分退供数据），无法恢复旧唯一键；请先归档多余单据';
  END IF;

  IF EXISTS (SELECT 1 FROM information_schema.statistics
              WHERE table_schema = DATABASE() AND table_name = 'wm_rt_vendor'
                AND index_name = 'uk_vendor_open_return') THEN
    ALTER TABLE wm_rt_vendor DROP INDEX uk_vendor_open_return;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns
              WHERE table_schema = DATABASE() AND table_name = 'wm_rt_vendor'
                AND column_name = 'auto_open_return_key') THEN
    ALTER TABLE wm_rt_vendor DROP COLUMN auto_open_return_key;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                  WHERE table_schema = DATABASE() AND table_name = 'wm_rt_vendor'
                    AND column_name = 'auto_iqc_return_key') THEN
    ALTER TABLE wm_rt_vendor ADD COLUMN auto_iqc_return_key varchar(64)
      GENERATED ALWAYS AS (CASE WHEN attr1 LIKE 'AUTO:RTV:IQC:%' THEN attr1 ELSE NULL END) STORED;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                  WHERE table_schema = DATABASE() AND table_name = 'wm_rt_vendor'
                    AND index_name = 'uk_vendor_iqc_return') THEN
    ALTER TABLE wm_rt_vendor ADD UNIQUE KEY uk_vendor_iqc_return (auto_iqc_return_key);
  END IF;
END$$
DELIMITER ;

CALL rollback_vendor_return_partial_20261029();
DROP PROCEDURE rollback_vendor_return_partial_20261029;

SELECT index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS cols
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'wm_rt_vendor'
 GROUP BY index_name ORDER BY index_name;

-- 采购退供增量约束。先备份并检查重复来源；不要在未确认的库执行。
-- 此脚本只增加生成列与索引，不删除或修改业务数据。DDL 在 MySQL 中会隐式提交。
SELECT DATABASE() AS target_database;
SELECT attr1, COUNT(*) AS duplicate_count FROM wm_rt_vendor
 WHERE attr1 LIKE 'AUTO:RTV:IQC:%' GROUP BY attr1 HAVING COUNT(*) > 1;

DROP PROCEDURE IF EXISTS apply_vendor_return_20261029;
DELIMITER $$
CREATE PROCEDURE apply_vendor_return_20261029()
BEGIN
  IF DATABASE() IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='请先选择目标数据库'; END IF;
  IF EXISTS (SELECT attr1 FROM wm_rt_vendor WHERE attr1 LIKE 'AUTO:RTV:IQC:%' GROUP BY attr1 HAVING COUNT(*) > 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='存在重复 IQC 退供来源，请先核对，不自动修改数据';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='wm_rt_vendor' AND column_name='auto_iqc_return_key') THEN
    ALTER TABLE wm_rt_vendor ADD COLUMN auto_iqc_return_key varchar(64)
      GENERATED ALWAYS AS (CASE WHEN attr1 LIKE 'AUTO:RTV:IQC:%' THEN attr1 ELSE NULL END) STORED;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='wm_rt_vendor' AND index_name='uk_vendor_iqc_return') THEN
    ALTER TABLE wm_rt_vendor ADD UNIQUE KEY uk_vendor_iqc_return (auto_iqc_return_key);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='wm_rt_vendor_line' AND index_name='idx_vendor_return_source') THEN
    ALTER TABLE wm_rt_vendor_line ADD KEY idx_vendor_return_source (attr2(64),rt_id,batch_id);
  END IF;
END$$
DELIMITER ;
CALL apply_vendor_return_20261029();
DROP PROCEDURE apply_vendor_return_20261029;

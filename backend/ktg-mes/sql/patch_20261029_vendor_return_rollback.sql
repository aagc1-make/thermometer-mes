-- 回滚仅移除本补丁的索引与生成列；不会删除退供单或业务数据。
SELECT DATABASE() AS target_database;
DROP PROCEDURE IF EXISTS rollback_vendor_return_20261029;
DELIMITER $$
CREATE PROCEDURE rollback_vendor_return_20261029()
BEGIN
  IF DATABASE() IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='请先选择目标数据库'; END IF;
  IF EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='wm_rt_vendor_line' AND index_name='idx_vendor_return_source') THEN
    ALTER TABLE wm_rt_vendor_line DROP INDEX idx_vendor_return_source;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='wm_rt_vendor' AND index_name='uk_vendor_iqc_return') THEN
    ALTER TABLE wm_rt_vendor DROP INDEX uk_vendor_iqc_return;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='wm_rt_vendor' AND column_name='auto_iqc_return_key') THEN
    ALTER TABLE wm_rt_vendor DROP COLUMN auto_iqc_return_key;
  END IF;
END$$
DELIMITER ;
CALL rollback_vendor_return_20261029();
DROP PROCEDURE rollback_vendor_return_20261029;

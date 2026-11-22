-- =============================================================================
-- 工单主链 v2：补充编码规则种子（幂等，可重复执行）
-- 文件：sql/patch_20261102_code_rules_ext.sql
--
-- 说明：
--   patch_20261102_workorder_chain_v2.sql 已播种 GD/PCGD/PCCG/PCWS/RKZJGD/RKCG 六条规则。
--   应用层改造把「来料检验 qc_iqc」和「库存事务 wm_transaction」也切到新取号器，
--   这两条规则在上一版脚本中没有，这里单独补种，不改动已交付脚本。
-- =============================================================================

SET NAMES utf8mb4;

INSERT INTO `sys_code_rule`
  (`rule_code`,`rule_name`,`biz_object`,`prefix`,`date_format`,`serial_length`,`serial_start`,`serial_step`,`reset_cycle`,`enable_flag`,`remark`)
VALUES
  ('IQC', '来料检验单号', 'QC_IQC',      'iqc', 'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '来料检验 iqc2026110200001'),
  ('TXN', '库存事务号',   'WM_TRANSACTION', 'txn', 'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '库存事务 txn2026110200001')
ON DUPLICATE KEY UPDATE
  rule_name     = VALUES(rule_name),
  biz_object    = VALUES(biz_object),
  prefix        = VALUES(prefix),
  date_format   = VALUES(date_format),
  serial_length = VALUES(serial_length),
  serial_start  = VALUES(serial_start),
  serial_step   = VALUES(serial_step),
  reset_cycle   = VALUES(reset_cycle),
  enable_flag   = VALUES(enable_flag),
  remark        = VALUES(remark);

SELECT rule_code, rule_name, prefix, date_format, serial_length, reset_cycle
FROM `sys_code_rule`
ORDER BY rule_code;

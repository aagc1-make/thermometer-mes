-- =============================================================================
-- 单据编码统一切换到 sys_code_sequence 取号器（幂等，可重复执行）
-- 文件：sql/patch_20261114_code_sequence_docs.sql
-- 目标库：j2eedb（MySQL 8.4）
--
-- 背景：
--   patch_20261102_workorder_chain_v2.sql 已为工单/批次/采购入库/来料检验/库存事务
--   播种规则（GD/PCGD/PCCG/PCWS/RKZJGD/RKCG/IQC/TXN），应用侧也已切到
--   CodeSequenceService。剩余仍走旧 AutoCodeUtil（sys_auto_code_rule）的单据，
--   本脚本补规则数据并给业务表加唯一索引兜底。
--
-- 内容：
--   1) 播种 20 条单据编码规则（rule_code / prefix / 日期段 / 定长流水）
--   2) 为对应单据号列补 UNIQUE 索引（并发取号 + 业务唯一双保险）
--   3) 输出规则与索引校验结果
--
-- 说明：
--   * 不修改、不删除 sys_auto_code_rule / sys_auto_code_part（主数据编码仍在用旧规则）。
--   * 历史单据不重编号：本脚本只加约束与配置，不 UPDATE 任何业务表。
--   * 目标表当前均为空表（2026-11-14 核对），因此加 UNIQUE 不会与历史数据冲突；
--     若未来在非空库执行，遇到重复值会直接失败，请先人工清理后再重跑。
--
-- 执行方式：
--   mysql --host=127.0.0.1 --port=3306 --user=root --password=*** \
--         --database=j2eedb --default-character-set=utf8mb4 \
--         -e "source E:/ktg/program/thermometer-mes-linkage/sql/patch_20261114_code_sequence_docs.sql"
-- =============================================================================

SET NAMES utf8mb4;

-- =============================================================================
-- Stage 1：单据编码规则（rule_code 与 UserConstants.CODE_RULE_* 一一对应）
--   单号形态：prefix + yyyyMMdd + 5 位流水，如 ll2026111400001
-- =============================================================================
INSERT INTO `sys_code_rule`
  (`rule_code`,`rule_name`,`biz_object`,`prefix`,`date_format`,`serial_length`,`serial_start`,`serial_step`,`reset_cycle`,`enable_flag`,`remark`)
VALUES
  ('DHTZ', '到货通知单号',   'WM_ARRIVAL_NOTICE',    'dhtz', 'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '到货通知单 dhtz2026111400001'),
  ('LL',   '生产领料单号',   'WM_ISSUE',             'll',   'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '生产领料单 ll2026111400001'),
  ('TL',   '生产退料单号',   'WM_RT_ISSUE',          'tl',   'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '生产退料单 tl2026111400001'),
  ('CTH',  '采购退货单号',   'WM_RT_VENDOR',         'cth',  'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '采购退货单 cth2026111400001'),
  ('WXLL', '外协领用单号',   'WM_OUTSOURCE_ISSUE',   'wxll', 'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '外协领用单 wxll2026111400001'),
  ('WXRK', '外协入库单号',   'WM_OUTSOURCE_RECPT',   'wxrk', 'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '外协入库单 wxrk2026111400001'),
  ('CPRK', '产品入库单号',   'WM_PRODUCT_RECPT',     'cprk', 'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '产品入库单 cprk2026111400001'),
  ('XSCK', '销售出库单号',   'WM_PRODUCT_SALES',     'xsck', 'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '销售出库单 xsck2026111400001'),
  ('XSTH', '销售退货单号',   'WM_RT_SALES',          'xsth', 'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '销售退货单 xsth2026111400001'),
  ('FHTZ', '发货通知单号',   'WM_SALES_NOTICE',      'fhtz', 'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '发货通知单 fhtz2026111400001'),
  ('YKD',  '移库单号',       'WM_TRANSFER',          'ykd',  'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '移库单 ykd2026111400001'),
  ('PD',   '库存盘点单号',   'WM_STOCK_TAKING',      'pd',   'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '盘点单 pd2026111400001'),
  ('BG',   '生产报工单号',   'PRO_FEEDBACK',         'bg',   'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '报工单 bg2026111400001'),
  ('IPQC', '过程检验单号',   'QC_IPQC',              'ipqc', 'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '过程检验单 ipqc2026111400001'),
  ('OQC',  '出货检验单号',   'QC_OQC',               'oqc',  'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '出货检验单 oqc2026111400001'),
  ('JYYP', '检验样品编号',   'QC_RESULT',            'jyyp', 'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '检验样品 jyyp2026111400001'),
  ('RW',   '生产任务号',     'PRO_TASK',             'rw',   'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '生产任务 rw2026111400001'),
  ('LZK',  '流转卡号',       'PRO_TRANS_ORDER',      'lzk',  'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '流转卡 lzk2026111400001'),
  ('SCK',  '生产卡号',       'PRO_CARD',             'sck',  'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '生产卡 sck2026111400001'),
  ('WXD',  '设备维修单号',   'DV_REPAIR',            'wxd',  'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '维修单 wxd2026111400001')
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

SELECT 'STAGE 1 done: code rules seeded' AS stage_result;

-- =============================================================================
-- Stage 2：单据号唯一索引（取号器保证不重号，唯一索引作为最后防线）
--   已存在同名索引则跳过；表为空表，重复值风险已核对。
-- =============================================================================
DROP PROCEDURE IF EXISTS ktg2_add_unique;

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

DELIMITER ;

CALL ktg2_add_unique('wm_arrival_notice',  'uk_wm_arrival_notice_notice_code',  '`notice_code`');
CALL ktg2_add_unique('wm_issue_header',    'uk_wm_issue_header_issue_code',     '`issue_code`');
CALL ktg2_add_unique('wm_rt_issue',        'uk_wm_rt_issue_rt_code',            '`rt_code`');
CALL ktg2_add_unique('wm_rt_vendor',       'uk_wm_rt_vendor_rt_code',           '`rt_code`');
CALL ktg2_add_unique('wm_outsource_issue', 'uk_wm_outsource_issue_issue_code',  '`issue_code`');
CALL ktg2_add_unique('wm_outsource_recpt', 'uk_wm_outsource_recpt_recpt_code',  '`recpt_code`');
CALL ktg2_add_unique('wm_product_recpt',   'uk_wm_product_recpt_recpt_code',    '`recpt_code`');
CALL ktg2_add_unique('wm_product_sales',   'uk_wm_product_sales_sales_code',    '`sales_code`');
CALL ktg2_add_unique('wm_rt_sales',        'uk_wm_rt_sales_rt_code',            '`rt_code`');
CALL ktg2_add_unique('wm_sales_notice',    'uk_wm_sales_notice_notice_code',    '`notice_code`');
CALL ktg2_add_unique('wm_transfer',        'uk_wm_transfer_transfer_code',      '`transfer_code`');
CALL ktg2_add_unique('wm_stock_taking',    'uk_wm_stock_taking_taking_code',    '`taking_code`');
CALL ktg2_add_unique('pro_feedback',       'uk_pro_feedback_feedback_code',     '`feedback_code`');
CALL ktg2_add_unique('qc_ipqc',            'uk_qc_ipqc_ipqc_code',              '`ipqc_code`');
CALL ktg2_add_unique('qc_oqc',             'uk_qc_oqc_oqc_code',                '`oqc_code`');
CALL ktg2_add_unique('qc_result',          'uk_qc_result_result_code',          '`result_code`');
CALL ktg2_add_unique('pro_task',           'uk_pro_task_task_code',             '`task_code`');
CALL ktg2_add_unique('pro_trans_order',    'uk_pro_trans_order_code',           '`trans_order_code`');
CALL ktg2_add_unique('pro_card',           'uk_pro_card_card_code',             '`card_code`');
CALL ktg2_add_unique('dv_repair',          'uk_dv_repair_repair_code',          '`repair_code`');

DROP PROCEDURE IF EXISTS ktg2_add_unique;

SELECT 'STAGE 2 done: unique indexes ready' AS stage_result;

-- =============================================================================
-- Stage 3：校验输出
-- =============================================================================
SELECT rule_code, rule_name, prefix, date_format, serial_length, reset_cycle, enable_flag
FROM `sys_code_rule`
WHERE rule_code IN ('DHTZ','LL','TL','CTH','WXLL','WXRK','CPRK','XSCK','XSTH','FHTZ',
                    'YKD','PD','BG','IPQC','OQC','JYYP','RW','LZK','SCK','WXD')
ORDER BY rule_code;

SELECT table_name, index_name, group_concat(column_name ORDER BY seq_in_index) AS cols
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND index_name IN ('uk_wm_arrival_notice_notice_code','uk_wm_issue_header_issue_code',
                     'uk_wm_rt_issue_rt_code','uk_wm_rt_vendor_rt_code',
                     'uk_wm_outsource_issue_issue_code','uk_wm_outsource_recpt_recpt_code',
                     'uk_wm_product_recpt_recpt_code','uk_wm_product_sales_sales_code',
                     'uk_wm_rt_sales_rt_code','uk_wm_sales_notice_notice_code',
                     'uk_wm_transfer_transfer_code','uk_wm_stock_taking_taking_code',
                     'uk_pro_feedback_feedback_code','uk_qc_ipqc_ipqc_code',
                     'uk_qc_oqc_oqc_code','uk_qc_result_result_code',
                     'uk_pro_task_task_code','uk_pro_trans_order_code',
                     'uk_pro_card_card_code','uk_dv_repair_repair_code')
GROUP BY table_name, index_name
ORDER BY table_name;

SELECT 'SUMMARY: 20 rules + 20 unique indexes (idempotent)' AS summary;

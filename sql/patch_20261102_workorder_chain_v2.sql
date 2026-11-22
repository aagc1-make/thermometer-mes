-- =============================================================================
-- 工单主链 v2 增量脚本（只新增，不删除、不重命名）
-- 文件：sql/patch_20261102_workorder_chain_v2.sql
-- 目标库：j2eedb（MySQL 8.4）
-- 依据：doc/数据库设计/工单主链设计v1_20261102.md（D1-D8 定案）
--
-- 特性：
--   1) 可重复执行：CREATE TABLE IF NOT EXISTS + information_schema 预检后再 ALTER
--   2) 不改任何现有表名/列名/索引名，只 ADD COLUMN / ADD INDEX / ADD CONSTRAINT
--   3) 不创建触发器（D1 定案：应用层原子取号 + 唯一索引兜底）
--
-- 执行方式：
--   mysql --host=127.0.0.1 --port=3306 --user=root --password=*** \
--         --database=j2eedb --default-character-set=utf8mb4 \
--         -e "source E:/ktg/program/thermometer-mes-linkage/sql/patch_20261102_workorder_chain_v2.sql"
-- =============================================================================

SET NAMES utf8mb4;
SET @script_start = NOW(3);

-- =============================================================================
-- Stage 0：幂等辅助过程（脚本末尾统一 DROP）
-- =============================================================================

DROP PROCEDURE IF EXISTS ktg_add_column;
DROP PROCEDURE IF EXISTS ktg_add_index;
DROP PROCEDURE IF EXISTS ktg_add_unique;
DROP PROCEDURE IF EXISTS ktg_add_fk;
DROP PROCEDURE IF EXISTS ktg_add_check;

DELIMITER $$

CREATE PROCEDURE ktg_add_column(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_definition TEXT)
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = p_column) = 0 THEN
    SET @ktg_ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
    PREPARE ktg_stmt FROM @ktg_ddl;
    EXECUTE ktg_stmt;
    DEALLOCATE PREPARE ktg_stmt;
  END IF;
END$$

CREATE PROCEDURE ktg_add_index(IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_columns TEXT)
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = p_table AND index_name = p_index) = 0 THEN
    SET @ktg_ddl = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '` (', p_columns, ')');
    PREPARE ktg_stmt FROM @ktg_ddl;
    EXECUTE ktg_stmt;
    DEALLOCATE PREPARE ktg_stmt;
  END IF;
END$$

CREATE PROCEDURE ktg_add_unique(IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_columns TEXT)
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = p_table AND index_name = p_index) = 0 THEN
    SET @ktg_ddl = CONCAT('ALTER TABLE `', p_table, '` ADD UNIQUE KEY `', p_index, '` (', p_columns, ')');
    PREPARE ktg_stmt FROM @ktg_ddl;
    EXECUTE ktg_stmt;
    DEALLOCATE PREPARE ktg_stmt;
  END IF;
END$$

CREATE PROCEDURE ktg_add_fk(IN p_table VARCHAR(64), IN p_name VARCHAR(64),
                            IN p_columns VARCHAR(200), IN p_ref_table VARCHAR(64),
                            IN p_ref_columns VARCHAR(200), IN p_action VARCHAR(64))
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

CREATE PROCEDURE ktg_add_check(IN p_table VARCHAR(64), IN p_name VARCHAR(64), IN p_expression TEXT)
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.table_constraints
      WHERE constraint_schema = DATABASE() AND table_name = p_table
        AND constraint_name = p_name AND constraint_type = 'CHECK') = 0 THEN
    SET @ktg_ddl = CONCAT('ALTER TABLE `', p_table, '` ADD CONSTRAINT `', p_name, '` CHECK (', p_expression, ')');
    PREPARE ktg_stmt FROM @ktg_ddl;
    EXECUTE ktg_stmt;
    DEALLOCATE PREPARE ktg_stmt;
  END IF;
END$$

DELIMITER ;

SELECT 'STAGE 0 done: helpers ready' AS stage_result;

-- =============================================================================
-- Stage 1：编码规则表 + 取号器
-- =============================================================================

CREATE TABLE IF NOT EXISTS `sys_code_rule` (
  `rule_id`       bigint       NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `rule_code`     varchar(64)  NOT NULL                COMMENT '规则编码，如 GD/PCGD/RKZJGD',
  `rule_name`     varchar(128) NOT NULL                COMMENT '规则名称',
  `biz_object`    varchar(64)  NOT NULL                COMMENT '业务对象 WORKORDER/BATCH/INBOUND_QC/ITEM_RECPT',
  `prefix`        varchar(16)  NOT NULL                COMMENT '单号前缀，如 gd/pcgd/rkzjgd',
  `date_format`   varchar(16)  DEFAULT 'yyyyMMdd'      COMMENT '日期段格式；NULL 或空 = 无日期段',
  `serial_length` tinyint      NOT NULL DEFAULT 5      COMMENT '流水段定长',
  `serial_start`  bigint       NOT NULL DEFAULT 1      COMMENT '起始流水号',
  `serial_step`   int          NOT NULL DEFAULT 1      COMMENT '流水步长',
  `reset_cycle`   varchar(8)   NOT NULL DEFAULT 'DAY'  COMMENT '流水重置周期 NONE/YEAR/MONTH/DAY',
  `enable_flag`   char(1)      NOT NULL DEFAULT 'Y'    COMMENT '是否启用 Y/N',
  `remark`        varchar(500) DEFAULT ''              COMMENT '备注',
  `create_by`     varchar(64)  DEFAULT ''              COMMENT '创建者',
  `create_time`   datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_by`     varchar(64)  DEFAULT ''              COMMENT '更新者',
  `update_time`   datetime     DEFAULT NULL            COMMENT '更新时间',
  PRIMARY KEY (`rule_id`),
  UNIQUE KEY `uk_code_rule_code` (`rule_code`),
  KEY `idx_code_rule_biz` (`biz_object`),
  CONSTRAINT `ck_code_rule_reset` CHECK (`reset_cycle` IN ('NONE','YEAR','MONTH','DAY')),
  CONSTRAINT `ck_code_rule_enable` CHECK (`enable_flag` IN ('Y','N'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编码规则表';

CREATE TABLE IF NOT EXISTS `sys_code_sequence` (
  `rule_code`  varchar(64) NOT NULL COMMENT '规则编码，FK -> sys_code_rule.rule_code',
  `period_key` varchar(16) NOT NULL COMMENT '周期键：20261102 / 202611 / 2026 / 空串',
  `current_no` bigint      NOT NULL DEFAULT 0 COMMENT '该周期已发出的最大流水号',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '取号时间',
  PRIMARY KEY (`rule_code`, `period_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编码取号器表';

SELECT 'STAGE 1 done: sys_code_rule / sys_code_sequence created' AS stage_result;

-- 规则种子（幂等）。前缀按 D4 定案：gd / pcgd / pccg / pcws / rkzjgd / rkcg
INSERT INTO `sys_code_rule`
  (`rule_code`,`rule_name`,`biz_object`,`prefix`,`date_format`,`serial_length`,`serial_start`,`serial_step`,`reset_cycle`,`enable_flag`,`remark`)
VALUES
  ('GD',     '生产工单号',   'WORKORDER',   'gd',     'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '工单号 gd2026110200001'),
  ('PCGD',   '工单产出版次号','BATCH',       'pcgd',   'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '工单产出批次 pcgd2026110200001'),
  ('PCCG',   '采购来料批次号','BATCH',       'pccg',   'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '采购来料批次 pccg2026110200001'),
  ('PCWS',   '外协入库批次号','BATCH',       'pcws',   'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '外协入库批次 pcws2026110200001'),
  ('RKZJGD', '成品入库质检号','INBOUND_QC',  'rkzjgd', 'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '入库质检 rkzjgd2026110200001'),
  ('RKCG',   '采购入库单号',  'ITEM_RECPT',  'rkcg',   'yyyyMMdd', 5, 1, 1, 'DAY', 'Y', '采购入库 rkcg2026110200001')
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

SELECT 'STAGE 1b done: code rules seeded' AS stage_result;

-- =============================================================================
-- Stage 2：新建工单主链表（不含外键，外键统一在 Stage 5 追加，便于增量启用）
-- =============================================================================

-- 2.1 工单行（成品产出维度）
CREATE TABLE IF NOT EXISTS `pro_workorder_line` (
  `line_id`            bigint        NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `workorder_id`       bigint        NOT NULL              COMMENT '工单ID',
  `line_no`            int           NOT NULL DEFAULT 1    COMMENT '工单内行号',
  `item_id`            bigint        NOT NULL              COMMENT '产出物料ID',
  `item_code`          varchar(64)   DEFAULT NULL          COMMENT '产出物料编码（快照）',
  `item_name`          varchar(255)  DEFAULT NULL          COMMENT '产出物料名称（快照）',
  `item_spc`           varchar(255)  DEFAULT NULL          COMMENT '规格型号（快照）',
  `unit_of_measure`    varchar(64)   DEFAULT NULL          COMMENT '计量单位（快照）',
  `unit_name`          varchar(64)   DEFAULT NULL          COMMENT '单位名称（快照）',
  `plan_quantity`      decimal(18,4) NOT NULL DEFAULT 0    COMMENT '计划数量',
  `produced_quantity`  decimal(18,4) NOT NULL DEFAULT 0    COMMENT '已产数量',
  `qualified_quantity` decimal(18,4) NOT NULL DEFAULT 0    COMMENT '合格数量',
  `scrapped_quantity`  decimal(18,4) NOT NULL DEFAULT 0    COMMENT '报废数量',
  `batch_strategy`     varchar(8)    NOT NULL DEFAULT 'SINGLE' COMMENT '批次策略 SINGLE/MULTI/NONE',
  `route_id`           bigint        DEFAULT NULL          COMMENT '工艺路线ID',
  `status`             varchar(16)   NOT NULL DEFAULT 'PREPARE' COMMENT '状态 PREPARE/IN_PROGRESS/FINISHED/CANCELLED',
  `remark`             varchar(500)  DEFAULT ''            COMMENT '备注',
  `create_by`          varchar(64)   DEFAULT ''            COMMENT '创建者',
  `create_time`        datetime      DEFAULT NULL          COMMENT '创建时间',
  `update_by`          varchar(64)   DEFAULT ''            COMMENT '更新者',
  `update_time`        datetime      DEFAULT NULL          COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  UNIQUE KEY `uk_wo_line_no` (`workorder_id`, `line_no`),
  KEY `idx_wo_line_item` (`item_id`),
  KEY `idx_wo_line_route` (`route_id`),
  CONSTRAINT `ck_wo_line_status` CHECK (`status` IN ('PREPARE','IN_PROGRESS','FINISHED','CANCELLED')),
  CONSTRAINT `ck_wo_line_batch_strategy` CHECK (`batch_strategy` IN ('SINGLE','MULTI','NONE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工单行（成品产出维度）';

-- 2.2 工序计划
CREATE TABLE IF NOT EXISTS `pro_workorder_operation` (
  `operation_id`         bigint        NOT NULL AUTO_INCREMENT COMMENT '工序ID',
  `line_id`              bigint        NOT NULL              COMMENT '工单行ID',
  `workorder_id`         bigint        NOT NULL              COMMENT '工单ID（冗余，便于按工单查）',
  `operation_no`         int           NOT NULL DEFAULT 1    COMMENT '行内工序顺序',
  `process_id`           bigint        NOT NULL              COMMENT '工序ID',
  `process_code`         varchar(64)   DEFAULT NULL          COMMENT '工序编码（快照）',
  `process_name`         varchar(255)  DEFAULT NULL          COMMENT '工序名称（快照）',
  `workstation_id`       bigint        DEFAULT NULL          COMMENT '默认工作站ID',
  `workstation_code`     varchar(64)   DEFAULT NULL          COMMENT '工作站编码（快照）',
  `workstation_name`     varchar(255)  DEFAULT NULL          COMMENT '工作站名称（快照）',
  `plan_quantity`        decimal(18,4) NOT NULL DEFAULT 0    COMMENT '计划数量',
  `completed_quantity`   decimal(18,4) NOT NULL DEFAULT 0    COMMENT '完成数量',
  `qualified_quantity`   decimal(18,4) NOT NULL DEFAULT 0    COMMENT '合格数量',
  `scrapped_quantity`    decimal(18,4) NOT NULL DEFAULT 0    COMMENT '报废数量',
  `status`               varchar(16)   NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/READY/RUNNING/DONE/SKIPPED',
  `remark`               varchar(500)  DEFAULT ''            COMMENT '备注',
  `create_by`            varchar(64)   DEFAULT ''            COMMENT '创建者',
  `create_time`          datetime      DEFAULT NULL          COMMENT '创建时间',
  `update_by`            varchar(64)   DEFAULT ''            COMMENT '更新者',
  `update_time`          datetime      DEFAULT NULL          COMMENT '更新时间',
  PRIMARY KEY (`operation_id`),
  UNIQUE KEY `uk_wo_op_no` (`line_id`, `operation_no`),
  KEY `idx_wo_op_wo` (`workorder_id`),
  KEY `idx_wo_op_process` (`process_id`),
  KEY `idx_wo_op_workstation` (`workstation_id`),
  CONSTRAINT `ck_wo_op_status` CHECK (`status` IN ('PENDING','READY','RUNNING','DONE','SKIPPED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工单工序计划';

-- 2.3 派工/执行任务（承接 pro_task，pro_task 保持只读过渡）
CREATE TABLE IF NOT EXISTS `pro_workorder_task` (
  `task_id`               bigint        NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `task_code`             varchar(32)   NOT NULL              COMMENT '任务编号（服务端生成）',
  `operation_id`          bigint        NOT NULL              COMMENT '工序ID',
  `workorder_id`          bigint        NOT NULL              COMMENT '工单ID（冗余）',
  `line_id`               bigint        NOT NULL              COMMENT '工单行ID（冗余）',
  `process_id`            bigint        DEFAULT NULL          COMMENT '工序ID（快照）',
  `process_code`          varchar(64)   DEFAULT NULL          COMMENT '工序编码（快照）',
  `process_name`          varchar(255)  DEFAULT NULL          COMMENT '工序名称（快照）',
  `workstation_id`        bigint        DEFAULT NULL          COMMENT '工作站ID',
  `workstation_code`      varchar(64)   DEFAULT NULL          COMMENT '工作站编码（快照）',
  `workstation_name`      varchar(255)  DEFAULT NULL          COMMENT '工作站名称（快照）',
  `assignee_id`           bigint        DEFAULT NULL          COMMENT '派工对象ID',
  `assignee_name`         varchar(128)  DEFAULT NULL          COMMENT '派工对象名称',
  `plan_quantity`         decimal(18,4) NOT NULL DEFAULT 0    COMMENT '计划数量',
  `completed_quantity`    decimal(18,4) NOT NULL DEFAULT 0    COMMENT '完成数量',
  `qualified_quantity`    decimal(18,4) NOT NULL DEFAULT 0    COMMENT '合格数量',
  `unqualified_quantity`  decimal(18,4) NOT NULL DEFAULT 0    COMMENT '不合格数量',
  `batch_id`              bigint        DEFAULT NULL          COMMENT '产出批次ID',
  `batch_code`            varchar(32)   DEFAULT NULL          COMMENT '产出批次号（快照）',
  `start_time`            datetime      DEFAULT NULL          COMMENT '开始时间',
  `end_time`              datetime      DEFAULT NULL          COMMENT '结束时间',
  `duration`              int           DEFAULT NULL          COMMENT '时长（分钟）',
  `status`                varchar(16)   NOT NULL DEFAULT 'NORMAL' COMMENT '状态 NORMAL/RUNNING/PAUSED/DONE/CANCELLED',
  `remark`                varchar(500)  DEFAULT ''            COMMENT '备注',
  `create_by`             varchar(64)   DEFAULT ''            COMMENT '创建者',
  `create_time`           datetime      DEFAULT NULL          COMMENT '创建时间',
  `update_by`             varchar(64)   DEFAULT ''            COMMENT '更新者',
  `update_time`           datetime      DEFAULT NULL          COMMENT '更新时间',
  PRIMARY KEY (`task_id`),
  UNIQUE KEY `uk_wo_task_code` (`task_code`),
  KEY `idx_wo_task_op` (`operation_id`),
  KEY `idx_wo_task_wo` (`workorder_id`),
  KEY `idx_wo_task_assignee` (`assignee_id`),
  KEY `idx_wo_task_batch` (`batch_id`),
  CONSTRAINT `ck_wo_task_status` CHECK (`status` IN ('NORMAL','RUNNING','PAUSED','DONE','CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工单派工/执行任务';

-- 2.4 入库质检头（新建，不复用 qc_iqc）
CREATE TABLE IF NOT EXISTS `qc_inbound` (
  `inspection_id`        bigint        NOT NULL AUTO_INCREMENT COMMENT '质检单ID',
  `inspection_code`      varchar(32)   NOT NULL              COMMENT '入库质检号 rkzjgd2026110200001',
  `inspection_type`      varchar(16)   NOT NULL DEFAULT 'FINISHED_INBOUND' COMMENT 'FINISHED_INBOUND/PURCHASE_INBOUND',
  `source_doc_type`      varchar(32)   NOT NULL DEFAULT 'WORKORDER' COMMENT '来源类型 WORKORDER/ITEM_RECPT',
  `workorder_id`         bigint        DEFAULT NULL          COMMENT '工单ID',
  `workorder_code`       varchar(32)   DEFAULT NULL          COMMENT '工单号（快照）',
  `batch_id`             bigint        NOT NULL              COMMENT '批次ID',
  `batch_code`           varchar(32)   NOT NULL              COMMENT '批次号（快照）',
  `item_id`              bigint        NOT NULL              COMMENT '物料ID',
  `item_code`            varchar(64)   DEFAULT NULL          COMMENT '物料编码（快照）',
  `item_name`            varchar(255)  DEFAULT NULL          COMMENT '物料名称（快照）',
  `item_spec`            varchar(255)  DEFAULT NULL          COMMENT '规格型号（快照）',
  `attempt_no`           smallint      NOT NULL DEFAULT 1    COMMENT '第几轮检验（返工重检递增）',
  `parent_inspection_id` bigint        DEFAULT NULL          COMMENT '上一轮质检单ID',
  `rework_workorder_id`  bigint        DEFAULT NULL          COMMENT '不合格触发的返工工单ID',
  `quantity_submitted`   decimal(18,4) NOT NULL DEFAULT 0    COMMENT '送检量',
  `quantity_qualified`   decimal(18,4) NOT NULL DEFAULT 0    COMMENT '合格量',
  `quantity_unqualified` decimal(18,4) NOT NULL DEFAULT 0    COMMENT '不合格量',
  `quantity_scrapped`    decimal(18,4) NOT NULL DEFAULT 0    COMMENT '报废量',
  `inspect_result`       varchar(16)   DEFAULT NULL          COMMENT '判定 PASS/FAIL/CONDITIONAL，空=未判定',
  `inspect_date`         datetime      DEFAULT NULL          COMMENT '检验时间',
  `inspector_id`         bigint        DEFAULT NULL          COMMENT '检验员ID',
  `inspector_name`       varchar(128)  DEFAULT NULL          COMMENT '检验员姓名',
  `status`               varchar(16)   NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT/INSPECTING/PASSED/FAILED/CLOSED',
  `remark`               varchar(500)  DEFAULT ''            COMMENT '备注',
  `create_by`            varchar(64)   DEFAULT ''            COMMENT '创建者',
  `create_time`          datetime      DEFAULT NULL          COMMENT '创建时间',
  `update_by`            varchar(64)   DEFAULT ''            COMMENT '更新者',
  `update_time`          datetime      DEFAULT NULL          COMMENT '更新时间',
  PRIMARY KEY (`inspection_id`),
  UNIQUE KEY `uk_inbound_code` (`inspection_code`),
  UNIQUE KEY `uk_inbound_batch_attempt` (`batch_id`, `attempt_no`),
  KEY `idx_inbound_wo` (`workorder_id`),
  KEY `idx_inbound_status` (`status`),
  KEY `idx_inbound_date` (`inspect_date`),
  CONSTRAINT `ck_inbound_status` CHECK (`status` IN ('DRAFT','INSPECTING','PASSED','FAILED','CLOSED')),
  CONSTRAINT `ck_inbound_result` CHECK (`inspect_result` IS NULL OR `inspect_result` IN ('PASS','FAIL','CONDITIONAL')),
  CONSTRAINT `ck_inbound_type` CHECK (`inspection_type` IN ('FINISHED_INBOUND','PURCHASE_INBOUND'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='入库质检单（主链）';

-- 2.5 入库质检项目行
CREATE TABLE IF NOT EXISTS `qc_inbound_line` (
  `line_id`          bigint        NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `inspection_id`    bigint        NOT NULL              COMMENT '质检单ID',
  `seq_no`           int           NOT NULL DEFAULT 1    COMMENT '行号',
  `index_id`         bigint        DEFAULT NULL          COMMENT '检验项目ID',
  `index_code`       varchar(64)   DEFAULT NULL          COMMENT '检验项目编码（快照）',
  `index_name`       varchar(255)  DEFAULT NULL          COMMENT '检验项目名称（快照）',
  `index_type`       varchar(16)   DEFAULT NULL          COMMENT 'NUMERIC/BOOLEAN/TEXT',
  `standard_value`   decimal(18,4) DEFAULT NULL          COMMENT '标准值',
  `threshold_min`    decimal(18,4) DEFAULT NULL          COMMENT '下限',
  `threshold_max`    decimal(18,4) DEFAULT NULL          COMMENT '上限',
  `actual_value`     decimal(18,4) DEFAULT NULL          COMMENT '实测值',
  `actual_text`      varchar(255)  DEFAULT NULL          COMMENT '文本实测值',
  `line_result`      varchar(8)    DEFAULT NULL          COMMENT '行判定 PASS/FAIL',
  `qc_tool`          varchar(64)   DEFAULT NULL          COMMENT '检验工具',
  `check_method`     varchar(255)  DEFAULT NULL          COMMENT '检验方法',
  `remark`           varchar(500)  DEFAULT ''            COMMENT '备注',
  `create_by`        varchar(64)   DEFAULT ''            COMMENT '创建者',
  `create_time`      datetime      DEFAULT NULL          COMMENT '创建时间',
  `update_by`        varchar(64)   DEFAULT ''            COMMENT '更新者',
  `update_time`      datetime      DEFAULT NULL          COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  UNIQUE KEY `uk_inbound_line_seq` (`inspection_id`, `seq_no`),
  KEY `idx_inbound_line_index` (`index_id`),
  CONSTRAINT `ck_inbound_line_result` CHECK (`line_result` IS NULL OR `line_result` IN ('PASS','FAIL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='入库质检项目行';

-- 2.6 入库质检缺陷记录
CREATE TABLE IF NOT EXISTS `qc_inbound_defect` (
  `defect_id`        bigint        NOT NULL AUTO_INCREMENT COMMENT '缺陷记录ID',
  `inspection_id`    bigint        NOT NULL              COMMENT '质检单ID',
  `line_id`          bigint        DEFAULT NULL          COMMENT '质检项目行ID',
  `defect_code`      varchar(64)   DEFAULT NULL          COMMENT '缺陷编码',
  `defect_name`      varchar(255)  DEFAULT NULL          COMMENT '缺陷名称',
  `defect_level`     varchar(8)    NOT NULL DEFAULT 'MIN' COMMENT '缺陷等级 CR/MAJ/MIN',
  `defect_quantity`  decimal(18,4) NOT NULL DEFAULT 0    COMMENT '缺陷数量',
  `disposal`         varchar(16)   DEFAULT NULL          COMMENT '处置 SCRAP/REWORK/CONDITIONAL',
  `remark`           varchar(500)  DEFAULT ''            COMMENT '备注',
  `create_by`        varchar(64)   DEFAULT ''            COMMENT '创建者',
  `create_time`      datetime      DEFAULT NULL          COMMENT '创建时间',
  `update_by`        varchar(64)   DEFAULT ''            COMMENT '更新者',
  `update_time`      datetime      DEFAULT NULL          COMMENT '更新时间',
  PRIMARY KEY (`defect_id`),
  KEY `idx_inbound_defect_insp` (`inspection_id`),
  KEY `idx_inbound_defect_line` (`line_id`),
  CONSTRAINT `ck_inbound_defect_level` CHECK (`defect_level` IN ('CR','MAJ','MIN')),
  CONSTRAINT `ck_inbound_defect_disposal` CHECK (`disposal` IS NULL OR `disposal` IN ('SCRAP','REWORK','CONDITIONAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='入库质检缺陷记录';

SELECT 'STAGE 2 done: 6 new chain tables created' AS stage_result;

-- =============================================================================
-- Stage 3：现有表加列（全部为新增列，不改不删）
-- =============================================================================

-- 3.1 pro_workorder 工单头
CALL ktg_add_column('pro_workorder', 'product_item_id',      'bigint        DEFAULT NULL COMMENT ''产品物料ID（新，归一 md_item）''');
CALL ktg_add_column('pro_workorder', 'source_doc_type',      'varchar(32)   DEFAULT NULL COMMENT ''来源单据类型''');
CALL ktg_add_column('pro_workorder', 'source_doc_id',        'bigint        DEFAULT NULL COMMENT ''来源单据ID''');
CALL ktg_add_column('pro_workorder', 'source_doc_code',      'varchar(64)   DEFAULT NULL COMMENT ''来源单据号（快照）''');
CALL ktg_add_column('pro_workorder', 'root_id',              'bigint        DEFAULT NULL COMMENT ''根工单ID''');
CALL ktg_add_column('pro_workorder', 'rework_of_id',         'bigint        DEFAULT NULL COMMENT ''返工来源工单ID''');
CALL ktg_add_column('pro_workorder', 'batch_strategy',       'varchar(8)    NOT NULL DEFAULT ''SINGLE'' COMMENT ''批次策略 SINGLE/MULTI/NONE''');
CALL ktg_add_column('pro_workorder', 'plan_start_date',      'date          DEFAULT NULL COMMENT ''计划开工日期''');
CALL ktg_add_column('pro_workorder', 'plan_end_date',        'date          DEFAULT NULL COMMENT ''计划完工日期''');
CALL ktg_add_column('pro_workorder', 'quantity_qualified',   'decimal(18,4) NOT NULL DEFAULT 0 COMMENT ''合格入库数量''');
CALL ktg_add_column('pro_workorder', 'quantity_scrapped',    'decimal(18,4) NOT NULL DEFAULT 0 COMMENT ''报废数量''');

-- 3.2 pro_workorder_bom 用料行
CALL ktg_add_column('pro_workorder_bom', 'workorder_line_id', 'bigint        DEFAULT NULL COMMENT ''工单行ID''');
CALL ktg_add_column('pro_workorder_bom', 'issued_quantity',   'decimal(18,4) NOT NULL DEFAULT 0 COMMENT ''已领数量''');
CALL ktg_add_column('pro_workorder_bom', 'consumed_quantity', 'decimal(18,4) NOT NULL DEFAULT 0 COMMENT ''已耗数量''');

-- 3.3 wm_batch 批次
CALL ktg_add_column('wm_batch', 'workorder_line_id', 'bigint        DEFAULT NULL COMMENT ''工单行ID（主链挂载点）''');
CALL ktg_add_column('wm_batch', 'batch_source',      'varchar(16)   NOT NULL DEFAULT ''MANUAL'' COMMENT ''批次来源 WORKORDER/PURCHASE/OUTSOURCE/MANUAL''');
CALL ktg_add_column('wm_batch', 'initial_quantity',  'decimal(18,4) NOT NULL DEFAULT 0 COMMENT ''批次初始数量''');
CALL ktg_add_column('wm_batch', 'batch_status',      'varchar(16)   NOT NULL DEFAULT ''ACTIVE'' COMMENT ''批次状态 ACTIVE/CONSUMED/CLOSED''');

-- 3.4 wm_material_stock 库存余额
CALL ktg_add_column('wm_material_stock', 'stock_status',    'varchar(16)   NOT NULL DEFAULT ''QUALIFIED'' COMMENT ''库存状态 QUALIFIED/UNQUALIFIED/HOLD/FROZEN''');
CALL ktg_add_column('wm_material_stock', 'quantity_frozen', 'decimal(18,4) NOT NULL DEFAULT 0 COMMENT ''冻结数量''');
CALL ktg_add_column('wm_material_stock', 'stock_key',       'char(64) GENERATED ALWAYS AS (CONCAT_WS(''|'', CAST(`item_id` AS CHAR), CAST(`warehouse_id` AS CHAR), IFNULL(`location_id`,0), IFNULL(`area_id`,0), IFNULL(`batch_id`,0), `stock_status`)) STORED COMMENT ''库存维度指纹（生成列）''');

-- 3.5 wm_transaction 库存事务
CALL ktg_add_column('wm_transaction', 'txn_code',      'varchar(32)   DEFAULT NULL COMMENT ''事务号（服务端生成）''');
CALL ktg_add_column('wm_transaction', 'biz_key',       'varchar(191)  DEFAULT NULL COMMENT ''幂等键''');
CALL ktg_add_column('wm_transaction', 'qty_before',    'decimal(18,4) DEFAULT NULL COMMENT ''变动前结存''');
CALL ktg_add_column('wm_transaction', 'qty_after',     'decimal(18,4) DEFAULT NULL COMMENT ''变动后结存''');
CALL ktg_add_column('wm_transaction', 'workorder_id',  'bigint        DEFAULT NULL COMMENT ''工单ID（追溯）''');
CALL ktg_add_column('wm_transaction', 'inspection_id', 'bigint        DEFAULT NULL COMMENT ''入库质检单ID（放行凭证）''');
CALL ktg_add_column('wm_transaction', 'recpt_id',      'bigint        DEFAULT NULL COMMENT ''入库单ID''');

SELECT 'STAGE 3 done: existing tables extended (columns only)' AS stage_result;

-- =============================================================================
-- Stage 4：索引与业务单号唯一约束
-- =============================================================================

-- 4.1 工单头
CALL ktg_add_unique('pro_workorder', 'uk_workorder_code', '`workorder_code`');
CALL ktg_add_index('pro_workorder', 'idx_wo_status',        '`status`');
CALL ktg_add_index('pro_workorder', 'idx_wo_product_item',  '`product_item_id`');
CALL ktg_add_index('pro_workorder', 'idx_wo_parent',        '`parent_id`');
CALL ktg_add_index('pro_workorder', 'idx_wo_request_date',  '`request_date`');
CALL ktg_add_index('pro_workorder', 'idx_wo_client',        '`client_id`');
CALL ktg_add_index('pro_workorder', 'idx_wo_source',        '`source_doc_type`, `source_doc_id`');

-- 4.2 工单用料
CALL ktg_add_index('pro_workorder_bom', 'idx_bom_wo',   '`workorder_id`');
CALL ktg_add_index('pro_workorder_bom', 'idx_bom_item', '`item_id`');
CALL ktg_add_index('pro_workorder_bom', 'idx_bom_line', '`workorder_line_id`');

-- 4.3 批次
CALL ktg_add_unique('wm_batch', 'uk_batch_code', '`batch_code`');
CALL ktg_add_index('wm_batch', 'idx_batch_item',      '`item_id`');
CALL ktg_add_index('wm_batch', 'idx_batch_wo',        '`workorder_id`');
CALL ktg_add_index('wm_batch', 'idx_batch_wo_line',   '`workorder_line_id`');
CALL ktg_add_index('wm_batch', 'idx_batch_quality',   '`quality_status`');

-- 4.4 入库单
CALL ktg_add_unique('wm_item_recpt', 'uk_recpt_code', '`recpt_code`');
CALL ktg_add_index('wm_item_recpt', 'idx_recpt_notice', '`notice_id`');
CALL ktg_add_index('wm_item_recpt_line', 'idx_recpt_line_recpt', '`recpt_id`');
CALL ktg_add_index('wm_item_recpt_line', 'idx_recpt_line_batch',  '`batch_id`');

-- 4.5 库存余额
CALL ktg_add_unique('wm_material_stock', 'uk_stock_key',    '`stock_key`');
CALL ktg_add_index('wm_material_stock',  'idx_stock_batch', '`batch_id`');
CALL ktg_add_index('wm_material_stock',  'idx_stock_item_wh', '`item_id`, `warehouse_id`');
CALL ktg_add_index('wm_material_stock',  'idx_stock_wo',    '`workorder_id`');

-- 4.6 库存事务
CALL ktg_add_unique('wm_transaction', 'uk_txn_code',   '`txn_code`');
CALL ktg_add_unique('wm_transaction', 'uk_txn_biz_key', '`biz_key`');
CALL ktg_add_index('wm_transaction', 'idx_txn_batch_date',    '`batch_id`, `transaction_date`');
CALL ktg_add_index('wm_transaction', 'idx_txn_wo_date',       '`workorder_id`, `transaction_date`');
CALL ktg_add_index('wm_transaction', 'idx_txn_item_wh_date',  '`item_id`, `warehouse_id`, `transaction_date`');
CALL ktg_add_index('wm_transaction', 'idx_txn_source',        '`source_doc_type`, `source_doc_id`');
CALL ktg_add_index('wm_transaction', 'idx_txn_stock',         '`material_stock_id`');
CALL ktg_add_index('wm_transaction', 'idx_txn_inspection',    '`inspection_id`');

-- 4.7 来料质检单号（保持 qc_iqc 原样，仅补唯一约束）
CALL ktg_add_unique('qc_iqc', 'uk_iqc_code', '`iqc_code`');

SELECT 'STAGE 4 done: unique keys and indexes added' AS stage_result;

-- =============================================================================
-- Stage 5：主链外键（主数据 md_item / md_vendor 不加，按 D7）
-- =============================================================================

-- 5.1 工单行 -> 工单头
CALL ktg_add_fk('pro_workorder_line', 'fk_wo_line_wo',
                '`workorder_id`', 'pro_workorder', '`workorder_id`', 'ON DELETE RESTRICT');
-- 5.2 工序 -> 工单行
CALL ktg_add_fk('pro_workorder_operation', 'fk_wo_op_line',
                '`line_id`', 'pro_workorder_line', '`line_id`', 'ON DELETE RESTRICT');
-- 5.3 派工 -> 工序
CALL ktg_add_fk('pro_workorder_task', 'fk_wo_task_op',
                '`operation_id`', 'pro_workorder_operation', '`operation_id`', 'ON DELETE RESTRICT');
-- 5.4 用料 -> 工单行
CALL ktg_add_fk('pro_workorder_bom', 'fk_bom_line',
                '`workorder_line_id`', 'pro_workorder_line', '`line_id`', 'ON DELETE RESTRICT');
-- 5.5 返工工单 -> 原工单（自引用）
CALL ktg_add_fk('pro_workorder', 'fk_wo_rework',
                '`rework_of_id`', 'pro_workorder', '`workorder_id`', 'ON DELETE RESTRICT');

-- 5.6 批次 -> 工单行
CALL ktg_add_fk('wm_batch', 'fk_batch_wo_line',
                '`workorder_line_id`', 'pro_workorder_line', '`line_id`', 'ON DELETE RESTRICT');

-- 5.7 质检 -> 批次 / 工单 / 自引用 / 返工工单
CALL ktg_add_fk('qc_inbound', 'fk_inbound_batch',
                '`batch_id`', 'wm_batch', '`batch_id`', 'ON DELETE RESTRICT');
CALL ktg_add_fk('qc_inbound', 'fk_inbound_wo',
                '`workorder_id`', 'pro_workorder', '`workorder_id`', 'ON DELETE RESTRICT');
CALL ktg_add_fk('qc_inbound', 'fk_inbound_parent',
                '`parent_inspection_id`', 'qc_inbound', '`inspection_id`', 'ON DELETE RESTRICT');
CALL ktg_add_fk('qc_inbound', 'fk_inbound_rework_wo',
                '`rework_workorder_id`', 'pro_workorder', '`workorder_id`', 'ON DELETE RESTRICT');

-- 5.8 质检行/缺陷 -> 质检头
CALL ktg_add_fk('qc_inbound_line', 'fk_inbound_line_insp',
                '`inspection_id`', 'qc_inbound', '`inspection_id`', 'ON DELETE RESTRICT');
CALL ktg_add_fk('qc_inbound_defect', 'fk_inbound_defect_insp',
                '`inspection_id`', 'qc_inbound', '`inspection_id`', 'ON DELETE RESTRICT');
CALL ktg_add_fk('qc_inbound_defect', 'fk_inbound_defect_line',
                '`line_id`', 'qc_inbound_line', '`line_id`', 'ON DELETE RESTRICT');

-- 5.9 入库行 -> 入库单 / 批次
CALL ktg_add_fk('wm_item_recpt_line', 'fk_recpt_line_recpt',
                '`recpt_id`', 'wm_item_recpt', '`recpt_id`', 'ON DELETE RESTRICT');
CALL ktg_add_fk('wm_item_recpt_line', 'fk_recpt_line_batch',
                '`batch_id`', 'wm_batch', '`batch_id`', 'ON DELETE RESTRICT');

-- 5.10 库存余额 -> 批次
CALL ktg_add_fk('wm_material_stock', 'fk_stock_batch',
                '`batch_id`', 'wm_batch', '`batch_id`', 'ON DELETE RESTRICT');

-- 5.11 库存事务 -> 余额 / 批次 / 质检
CALL ktg_add_fk('wm_transaction', 'fk_txn_stock',
                '`material_stock_id`', 'wm_material_stock', '`material_stock_id`', 'ON DELETE RESTRICT');
CALL ktg_add_fk('wm_transaction', 'fk_txn_batch',
                '`batch_id`', 'wm_batch', '`batch_id`', 'ON DELETE RESTRICT');
CALL ktg_add_fk('wm_transaction', 'fk_txn_inspection',
                '`inspection_id`', 'qc_inbound', '`inspection_id`', 'ON DELETE RESTRICT');

-- 5.12 取号器 -> 编码规则
CALL ktg_add_fk('sys_code_sequence', 'fk_code_seq_rule',
                '`rule_code`', 'sys_code_rule', '`rule_code`', 'ON DELETE RESTRICT');

SELECT 'STAGE 5 done: foreign keys added' AS stage_result;

-- =============================================================================
-- Stage 6：状态 CHECK 约束（仅限取值集合明确、且不会破坏现有代码的列）
-- =============================================================================

CALL ktg_add_check('pro_workorder', 'ck_wo_status',
  '`status` IS NULL OR `status` IN (''PREPARE'',''CONFIRMED'',''APPROVING'',''APPROVED'',''IN_PROGRESS'',''FINISHED'',''CANCELED'',''CANCELLED'',''CLOSED'')');
CALL ktg_add_check('pro_workorder', 'ck_wo_batch_strategy',
  '`batch_strategy` IN (''SINGLE'',''MULTI'',''NONE'')');
CALL ktg_add_check('wm_batch', 'ck_batch_source',
  '`batch_source` IN (''WORKORDER'',''PURCHASE'',''OUTSOURCE'',''MANUAL'')');
CALL ktg_add_check('wm_batch', 'ck_batch_status',
  '`batch_status` IN (''ACTIVE'',''CONSUMED'',''CLOSED'')');
CALL ktg_add_check('wm_material_stock', 'ck_stock_status',
  '`stock_status` IN (''QUALIFIED'',''UNQUALIFIED'',''HOLD'',''FROZEN'')');

SELECT 'STAGE 6 done: check constraints added' AS stage_result;

-- =============================================================================
-- Stage 7：清理辅助过程
-- =============================================================================

DROP PROCEDURE IF EXISTS ktg_add_column;
DROP PROCEDURE IF EXISTS ktg_add_index;
DROP PROCEDURE IF EXISTS ktg_add_unique;
DROP PROCEDURE IF EXISTS ktg_add_fk;
DROP PROCEDURE IF EXISTS ktg_add_check;

-- =============================================================================
-- Stage 8：执行后自检输出
-- =============================================================================

SELECT 'SELF-CHECK' AS section;
SELECT (SELECT COUNT(*) FROM information_schema.tables
        WHERE table_schema='j2eedb' AND table_type='BASE TABLE') AS base_tables_total,
       (SELECT COUNT(*) FROM information_schema.tables
        WHERE table_schema='j2eedb' AND table_type='BASE TABLE'
          AND table_name IN ('sys_code_rule','sys_code_sequence','pro_workorder_line',
                             'pro_workorder_operation','pro_workorder_task','qc_inbound',
                             'qc_inbound_line','qc_inbound_defect')) AS new_tables_present,
       (SELECT COUNT(*) FROM information_schema.referential_constraints
        WHERE constraint_schema='j2eedb') AS fk_total,
       (SELECT COUNT(*) FROM information_schema.table_constraints
        WHERE constraint_schema='j2eedb' AND constraint_type='CHECK') AS check_total,
       (SELECT COUNT(*) FROM sys_code_rule) AS code_rule_rows;

SELECT 'STAGE 8 done: script finished' AS stage_result;

-- MES 主线增量迁移。先在数据库副本运行，再部署引用这些列的后端。
-- 保留已有业务单据编号、库存流水和菜单。由 DBA 在业务停止写入后执行。
-- 适用于项目当前 MySQL 8.x 主线库。

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

CREATE TABLE IF NOT EXISTS sys_code_reservation (
    document_code varchar(64) NOT NULL PRIMARY KEY,
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO sys_code_rule (rule_code, rule_name, biz_object, prefix) VALUES
('GD','生产工单','WORKORDER','gd'), ('XSDD','销售订单','SALES_ORDER','dd'),
('RW','生产任务','TASK','rw'), ('BG','生产报工','FEEDBACK','bg'),
('LL','生产领料','ISSUE','ll'), ('ZJ','质量检验','QC','zj'),
('IQC','来料检验','IQC','zj'), ('IPQC','过程检验','IPQC','zj'),
('OQC','出货检验','OQC','zj'), ('RQC','退料检验','RQC','zj'),
('PCGD','成品批次','BATCH','pc'), ('PCCG','来料批次','BATCH','pcg'),
('PCWS','外协批次','BATCH','pcw'), ('CPRK','产品入库','PRODUCT_RECPT','cr'),
('FHTZ','发货通知','SALES_NOTICE','ft'), ('XSCK','销售出库','PRODUCT_SALES','xc'),
('TXN','库存事务','TRANSACTION','tx'), ('RKCG','采购入库','ITEM_RECPT','rc'),
('DHTZ','到货通知','ARRIVAL','dt'), ('TL','生产退料','RT_ISSUE','tl'),
('CTH','供应商退货','RT_VENDOR','ct'), ('WXLL','外协领用','OUTSOURCE_ISSUE','wl'),
('WXRK','外协入库','OUTSOURCE_RECPT','wr'), ('XSTH','销售退货','RT_SALES','xt'),
('YKD','移库','TRANSFER','yk'), ('PD','库存盘点','STOCK_TAKING','pd'),
('PDJH','盘点方案','STOCK_TAKING_PLAN','pj'), ('JYYP','检验样品','QC_RESULT','jy'),
('LZK','流转卡','TRANS_ORDER','lz'), ('SCK','生产卡','CARD','sk'),
('WXD','设备维修','REPAIR','wx'), ('ZCL','杂项出库','MISC_ISSUE','zl'),
('ZCR','杂项入库','MISC_RECPT','zr'), ('BZ','装箱','PACKAGE','bz');

CREATE TABLE IF NOT EXISTS wm_sales_order (
    order_id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_code varchar(64) NOT NULL,
    order_name varchar(250) NOT NULL,
    client_id bigint NOT NULL,
    client_code varchar(64) DEFAULT NULL,
    client_name varchar(255) DEFAULT NULL,
    client_nick varchar(255) DEFAULT NULL,
    order_date date DEFAULT NULL,
    delivery_date date DEFAULT NULL,
    recipient varchar(100) DEFAULT NULL,
    tel varchar(64) DEFAULT NULL,
    address varchar(500) DEFAULT NULL,
    total_quantity decimal(18,4) NOT NULL DEFAULT 0,
    total_amount decimal(18,4) NOT NULL DEFAULT 0,
    status varchar(24) NOT NULL DEFAULT 'PREPARE',
    notice_id bigint DEFAULT NULL,
    notice_code varchar(64) DEFAULT NULL,
    remark varchar(500) DEFAULT NULL,
    attr1 varchar(255) DEFAULT NULL,
    attr2 varchar(255) DEFAULT NULL,
    attr3 bigint DEFAULT NULL,
    attr4 bigint DEFAULT NULL,
    create_by varchar(64) DEFAULT NULL,
    create_time datetime DEFAULT NULL,
    update_by varchar(64) DEFAULT NULL,
    update_time datetime DEFAULT NULL,
    UNIQUE KEY uk_sales_order_code (order_code),
    KEY idx_sales_order_notice (notice_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wm_sales_order_line (
    line_id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id bigint NOT NULL,
    item_id bigint NOT NULL,
    item_code varchar(64) NOT NULL,
    item_name varchar(255) DEFAULT NULL,
    specification varchar(500) DEFAULT NULL,
    unit_of_measure varchar(64) DEFAULT NULL,
    unit_name varchar(64) DEFAULT NULL,
    quantity decimal(18,4) NOT NULL,
    price decimal(18,4) NOT NULL DEFAULT 0,
    amount decimal(18,4) NOT NULL DEFAULT 0,
    quantity_produced decimal(18,4) NOT NULL DEFAULT 0,
    quantity_shipped decimal(18,4) NOT NULL DEFAULT 0,
    workorder_id bigint DEFAULT NULL,
    workorder_code varchar(64) DEFAULT NULL,
    oqc_id bigint DEFAULT NULL,
    oqc_code varchar(64) DEFAULT NULL,
    status varchar(24) NOT NULL DEFAULT 'PREPARE',
    remark varchar(500) DEFAULT NULL,
    attr1 varchar(255) DEFAULT NULL,
    attr2 varchar(255) DEFAULT NULL,
    attr3 bigint DEFAULT NULL,
    attr4 bigint DEFAULT NULL,
    create_by varchar(64) DEFAULT NULL,
    create_time datetime DEFAULT NULL,
    update_by varchar(64) DEFAULT NULL,
    update_time datetime DEFAULT NULL,
    KEY idx_sales_order_line_order (order_id),
    KEY idx_sales_order_line_workorder (workorder_id),
    CONSTRAINT fk_sales_order_line_order FOREIGN KEY (order_id) REFERENCES wm_sales_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DELIMITER $$
DROP PROCEDURE IF EXISTS mes_add_chain_column$$
CREATE PROCEDURE mes_add_chain_column(IN target_table varchar(64), IN target_column varchar(64), IN definition varchar(255))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema=DATABASE() AND table_name=target_table AND column_name=target_column) THEN
        SET @chain_ddl = CONCAT('ALTER TABLE ', target_table, ' ADD COLUMN ', target_column, ' ', definition);
        PREPARE chain_stmt FROM @chain_ddl;
        EXECUTE chain_stmt;
        DEALLOCATE PREPARE chain_stmt;
    END IF;
END$$
DROP PROCEDURE IF EXISTS mes_add_chain_index$$
CREATE PROCEDURE mes_add_chain_index(IN target_table varchar(64), IN target_index varchar(64), IN definition varchar(255))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
        WHERE table_schema=DATABASE() AND table_name=target_table AND index_name=target_index) THEN
        SET @chain_ddl = CONCAT('ALTER TABLE ', target_table, ' ADD ', definition);
        PREPARE chain_stmt FROM @chain_ddl;
        EXECUTE chain_stmt;
        DEALLOCATE PREPARE chain_stmt;
    END IF;
END$$
DELIMITER ;

CALL mes_add_chain_column('pro_workorder','source_doc_type','varchar(32) DEFAULT NULL');
CALL mes_add_chain_column('pro_workorder','source_doc_id','bigint DEFAULT NULL');
CALL mes_add_chain_column('pro_workorder','source_doc_code','varchar(64) DEFAULT NULL');
CALL mes_add_chain_column('pro_workorder_bom','workorder_line_id','bigint DEFAULT NULL');
CALL mes_add_chain_column('wm_batch','workorder_line_id','bigint DEFAULT NULL');
CALL mes_add_chain_column('wm_batch','batch_source','varchar(16) DEFAULT NULL');
CALL mes_add_chain_column('wm_batch','initial_quantity','decimal(18,4) DEFAULT NULL');
CALL mes_add_chain_column('wm_batch','batch_status','varchar(16) DEFAULT NULL');
CALL mes_add_chain_column('wm_transaction','txn_code','varchar(64) DEFAULT NULL');
CALL mes_add_chain_column('wm_transaction','biz_key','varchar(191) DEFAULT NULL');
CALL mes_add_chain_column('wm_transaction','qty_before','decimal(18,4) DEFAULT NULL');
CALL mes_add_chain_column('wm_transaction','qty_after','decimal(18,4) DEFAULT NULL');
CALL mes_add_chain_column('wm_transaction','workorder_id','bigint DEFAULT NULL');
CALL mes_add_chain_column('wm_transaction','inspection_id','bigint DEFAULT NULL');
CALL mes_add_chain_column('wm_transaction','recpt_id','bigint DEFAULT NULL');
CALL mes_add_chain_column('wm_storage_location','max_qty','decimal(18,4) DEFAULT NULL');
CALL mes_add_chain_column('wm_storage_area','max_qty','decimal(18,4) DEFAULT NULL');

CALL mes_add_chain_index('pro_workorder','idx_chain_source_code','INDEX idx_chain_source_code (source_doc_code)');
CALL mes_add_chain_index('wm_transaction','uk_chain_txn_code','UNIQUE INDEX uk_chain_txn_code (txn_code)');
CALL mes_add_chain_index('wm_transaction','uk_chain_txn_biz','UNIQUE INDEX uk_chain_txn_biz (biz_key)');

DROP PROCEDURE mes_add_chain_column;
DROP PROCEDURE mes_add_chain_index;

INSERT INTO pro_workorder_line (workorder_id,line_no,item_id,item_code,item_name,item_spc,unit_of_measure,unit_name,plan_quantity,status,create_by,create_time)
SELECT w.workorder_id,1,w.product_id,w.product_code,w.product_name,w.product_spc,w.unit_of_measure,w.unit_name,COALESCE(w.quantity,0),
       CASE w.status WHEN 'FINISHED' THEN 'FINISHED' WHEN 'CANCELED' THEN 'CANCELLED'
            WHEN 'DELETED' THEN 'CANCELLED' WHEN 'CONFIRMED' THEN 'IN_PROGRESS' ELSE 'PREPARE' END,
       w.create_by,w.create_time
FROM pro_workorder w
WHERE w.product_id > 0 AND NOT EXISTS (
    SELECT 1 FROM pro_workorder_line l WHERE l.workorder_id=w.workorder_id AND l.line_no=1
);

UPDATE pro_workorder_bom b JOIN pro_workorder_line l ON l.workorder_id=b.workorder_id AND l.line_no=1
SET b.workorder_line_id=l.line_id WHERE b.workorder_line_id IS NULL;

UPDATE wm_batch b JOIN pro_workorder_line l ON l.workorder_id=b.workorder_id AND l.item_id=b.item_id AND l.line_no=1
SET b.workorder_line_id=l.line_id, b.batch_source=COALESCE(b.batch_source,'WORKORDER')
WHERE b.workorder_line_id IS NULL;

-- 按已知批次回填，并且只接受唯一工单。无批次或有多个候选的历史行保留原值。
UPDATE wm_product_sales_line l
JOIN (
    SELECT line.line_id, MIN(r.workorder_id) AS workorder_id
    FROM wm_product_sales_line line
    JOIN wm_product_sales h ON h.sales_id=line.sales_id
    JOIN wm_product_recpt_line rl ON rl.batch_id=line.batch_id AND rl.item_id=line.item_id
    JOIN wm_product_recpt r ON r.recpt_id=rl.recpt_id AND r.status='FINISHED'
    JOIN pro_workorder w ON w.workorder_id=r.workorder_id AND w.source_doc_code=h.so_code
    WHERE line.workorder_id IS NULL
    GROUP BY line.line_id
    HAVING COUNT(DISTINCT r.workorder_id)=1
) resolved ON resolved.line_id=l.line_id
JOIN pro_workorder w ON w.workorder_id=resolved.workorder_id
SET l.workorder_id=w.workorder_id, l.workorder_code=w.workorder_code
WHERE l.workorder_id IS NULL;

INSERT INTO sys_config (config_name,config_key,config_value,config_type,create_by,create_time,remark)
SELECT '产品上架混放校验','mes.wm.mixing.enforce','true','N','admin',NOW(),'按库位物料或批次混放设置校验'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='mes.wm.mixing.enforce');

INSERT INTO sys_menu (menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '销售订单',d.menu_id,8,'salesorder','mes/wm/salesorder/index',1,0,'C','0','0','mes:wm:salesorder:list','list','admin',NOW()
FROM sys_menu d WHERE d.path='mes/wm' AND d.parent_id=0 AND d.menu_type='M'
AND NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.parent_id=d.menu_id AND m.path='salesorder');

INSERT INTO sys_menu (menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT a.menu_name,m.menu_id,a.order_num,'#',1,0,'F','0','0',CONCAT('mes:wm:salesorder:',a.action),'#','admin',NOW()
FROM sys_menu m
CROSS JOIN (
    SELECT '订单查询' AS menu_name,1 AS order_num,'query' AS action
    UNION ALL SELECT '订单新增',2,'add'
    UNION ALL SELECT '订单修改',3,'edit'
    UNION ALL SELECT '订单删除',4,'remove'
    UNION ALL SELECT '订单导出',5,'export'
    UNION ALL SELECT '订单确认',6,'confirm'
    UNION ALL SELECT '生成工单',7,'workorder'
    UNION ALL SELECT '生成出货检验',8,'oqc'
    UNION ALL SELECT '生成发货通知',9,'notice'
) a
WHERE m.component='mes/wm/salesorder/index'
AND NOT EXISTS (SELECT 1 FROM sys_menu existing WHERE existing.parent_id=m.menu_id AND existing.perms=CONCAT('mes:wm:salesorder:',a.action));

INSERT INTO sys_role_menu (role_id,menu_id)
SELECT r.role_id,m.menu_id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_key='admin' AND m.perms LIKE 'mes:wm:salesorder:%'
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);

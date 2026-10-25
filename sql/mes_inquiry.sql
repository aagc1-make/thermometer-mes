-- ============================================================
--  小程序端：询价单 + 微信用户绑定
--  只新增三张表，不改动现有 181 张 MES 表
--  执行：mysql -h127.0.0.1 -P3307 -uroot -p j2eedb -e "source E:/JavaCode/sql/mes_inquiry.sql"
-- ============================================================
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `mes_wx_user` (
  `wx_user_id`   bigint      NOT NULL AUTO_INCREMENT COMMENT '微信用户ID',
  `open_id`      varchar(64) NOT NULL COMMENT '微信openid',
  `union_id`     varchar(64)          DEFAULT NULL COMMENT '微信unionid',
  `client_id`    bigint               DEFAULT NULL COMMENT '绑定的客户ID(md_client)',
  `nick_name`    varchar(64)          DEFAULT NULL COMMENT '昵称',
  `avatar`       varchar(500)         DEFAULT NULL COMMENT '头像',
  `phone`        varchar(20)          DEFAULT NULL COMMENT '手机号',
  `company`      varchar(200)         DEFAULT NULL COMMENT '企业名称',
  `contact`      varchar(64)          DEFAULT NULL COMMENT '采购联系人',
  `last_login`   datetime             DEFAULT NULL COMMENT '最近登录',
  `create_time`  datetime             DEFAULT NULL,
  `update_time`  datetime             DEFAULT NULL,
  PRIMARY KEY (`wx_user_id`),
  UNIQUE KEY `uk_open_id` (`open_id`),
  KEY `idx_client` (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序微信用户';

CREATE TABLE IF NOT EXISTS `mes_inquiry_header` (
  `inquiry_id`     bigint       NOT NULL AUTO_INCREMENT COMMENT '询价单ID',
  `inquiry_code`   varchar(64)  NOT NULL COMMENT '询价单号 XJ+日期+流水',
  `client_id`      bigint                DEFAULT NULL COMMENT '客户ID',
  `client_name`    varchar(200)          DEFAULT NULL COMMENT '企业名称',
  `contact`        varchar(64)           DEFAULT NULL COMMENT '联系人',
  `phone`          varchar(20)           DEFAULT NULL COMMENT '联系电话',
  `wx_user_id`     bigint                DEFAULT NULL COMMENT '提交人(微信用户)',
  `status`         varchar(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING待提交/SUBMITTED已提交/QUOTED已报价/CONFIRMED已确认/ORDERED已下单/CANCELED已取消',
  `item_count`     int          NOT NULL DEFAULT 0 COMMENT '品项数',
  `total_qty`      decimal(24,6) NOT NULL DEFAULT 0 COMMENT '总件数',
  `expect_date`    date                  DEFAULT NULL COMMENT '期望交期',
  `address`        varchar(500)          DEFAULT NULL COMMENT '收货地址',
  `invoice_type`   varchar(20)           DEFAULT NULL COMMENT '开票方式',
  `remark`         varchar(500)          DEFAULT NULL COMMENT '备注',
  `quote_amount`   decimal(18,2)         DEFAULT NULL COMMENT '报价总额',
  `quote_time`     datetime              DEFAULT NULL COMMENT '报价时间',
  `sales_name`     varchar(64)           DEFAULT NULL COMMENT '跟进销售',
  `expire_time`    datetime              DEFAULT NULL COMMENT '报价有效期',
  `create_by`      varchar(64)           DEFAULT NULL,
  `create_time`    datetime              DEFAULT NULL,
  `update_time`    datetime              DEFAULT NULL,
  PRIMARY KEY (`inquiry_id`),
  UNIQUE KEY `uk_code` (`inquiry_code`),
  KEY `idx_client` (`client_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序询价单头';

CREATE TABLE IF NOT EXISTS `mes_inquiry_line` (
  `line_id`        bigint       NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `inquiry_id`     bigint       NOT NULL COMMENT '询价单ID',
  `item_id`        bigint                DEFAULT NULL COMMENT '物料/产品ID(md_item)',
  `item_code`      varchar(64)           DEFAULT NULL COMMENT '产品编码',
  `item_name`      varchar(200)          DEFAULT NULL COMMENT '产品名称',
  `specification`  varchar(500)          DEFAULT NULL COMMENT '规格型号',
  `item_type`      varchar(20)           DEFAULT NULL COMMENT '成品/半成品',
  `unit_name`      varchar(20)           DEFAULT NULL COMMENT '单位',
  `quantity`       decimal(24,6)         DEFAULT NULL COMMENT '数量',
  `ref_price`      decimal(18,2)         DEFAULT NULL COMMENT '参考单价',
  `quote_price`    decimal(18,2)         DEFAULT NULL COMMENT '报价单价',
  `delivery_date`  date                  DEFAULT NULL COMMENT '可交期',
  `remark`         varchar(500)          DEFAULT NULL COMMENT '行备注',
  `sort_no`        int                   DEFAULT 0,
  PRIMARY KEY (`line_id`),
  KEY `idx_inquiry` (`inquiry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序询价单行';

-- ---------- 种子：把现有测温枪产品补上「可订购」属性（供小程序目录使用） ----------
-- 用现成的 md_item 数据，只补充展示字段，不新增产品
ALTER TABLE `md_item`
  ADD COLUMN IF NOT EXISTS `sale_flag`      char(1) DEFAULT 'N' COMMENT '是否可订购 Y/N',
  ADD COLUMN IF NOT EXISTS `min_order_qty`  decimal(24,6) DEFAULT 1 COMMENT '起订量',
  ADD COLUMN IF NOT EXISTS `lead_days`      int DEFAULT 15 COMMENT '交付周期(天)',
  ADD COLUMN IF NOT EXISTS `cover_image`    varchar(500) DEFAULT NULL COMMENT '列表封面图';

-- 整机 + 两个半成品开放订购，其余为原材料不展示
UPDATE `md_item` SET sale_flag='Y', min_order_qty=1,  lead_days=15 WHERE item_code = 'TG-FG-001';
UPDATE `md_item` SET sale_flag='Y', min_order_qty=10, lead_days=7  WHERE item_code IN ('TG-SF-001','TG-SF-002');
UPDATE `md_item` SET sale_flag='N' WHERE sale_flag IS NULL;

-- ---------- 种子：两条演示询价单，方便自测「已提交 / 已报价」状态 ----------
DELETE FROM `mes_inquiry_line`   WHERE inquiry_id IN (SELECT inquiry_id FROM `mes_inquiry_header` WHERE inquiry_code LIKE 'XJ-DEMO%');
DELETE FROM `mes_inquiry_header` WHERE inquiry_code LIKE 'XJ-DEMO%';

INSERT INTO `mes_inquiry_header`
  (inquiry_id, inquiry_code, client_id, client_name, contact, phone, status, item_count, total_qty,
   expect_date, address, invoice_type, remark, quote_amount, quote_time, sales_name, expire_time, create_by, create_time, update_time)
VALUES
  (9001, CONCAT('XJ-DEMO-', DATE_FORMAT(NOW(), '%Y%m%d'), '-001'),
   (SELECT client_id FROM md_client ORDER BY client_id LIMIT 1),
   (SELECT client_name FROM md_client ORDER BY client_id LIMIT 1),
   '张采购', '13800000001', 'QUOTED', 2, 30,
   DATE_ADD(CURDATE(), INTERVAL 20 DAY), '上海市浦东新区张江路 88 号 3 号楼', '增值税专票', '希望 20 天内交付',
   128000.00, DATE_SUB(NOW(), INTERVAL 2 HOUR), '李销售', DATE_ADD(NOW(), INTERVAL 7 DAY), 'admin', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
  (9002, CONCAT('XJ-DEMO-', DATE_FORMAT(NOW(), '%Y%m%d'), '-002'),
   (SELECT client_id FROM md_client ORDER BY client_id LIMIT 1),
   (SELECT client_name FROM md_client ORDER BY client_id LIMIT 1),
   '王经理', '13800000002', 'SUBMITTED', 1, 50,
   DATE_ADD(CURDATE(), INTERVAL 30 DAY), '深圳市南山区科技园 12 栋', '增值税普票', '批量采购，价格优惠请电话沟通',
   NULL, NULL, '李销售', NULL, 'admin', NOW(), NOW());

INSERT INTO `mes_inquiry_line`
  (inquiry_id, item_id, item_code, item_name, specification, item_type, unit_name, quantity, ref_price, quote_price, delivery_date, sort_no)
SELECT 9001, item_id, item_code, item_name, specification, '成品', '台', 20, 5800.00, 5600.00, DATE_ADD(CURDATE(), INTERVAL 15 DAY), 1
FROM md_item WHERE item_code = 'TG-FG-001' LIMIT 1;

INSERT INTO `mes_inquiry_line`
  (inquiry_id, item_id, item_code, item_name, specification, item_type, unit_name, quantity, ref_price, quote_price, delivery_date, sort_no)
SELECT 9001, item_id, item_code, item_name, specification, '半成品', '套', 10, 1200.00, 1150.00, DATE_ADD(CURDATE(), INTERVAL 10 DAY), 2
FROM md_item WHERE item_code = 'TG-SF-001' LIMIT 1;

INSERT INTO `mes_inquiry_line`
  (inquiry_id, item_id, item_code, item_name, specification, item_type, unit_name, quantity, ref_price, delivery_date, sort_no)
SELECT 9002, item_id, item_code, item_name, specification, '成品', '台', 50, 5800.00, DATE_ADD(CURDATE(), INTERVAL 25 DAY), 1
FROM md_item WHERE item_code = 'TG-FG-001' LIMIT 1;

-- ---------- 自检 ----------
SELECT '可订购产品' AS 对象, COUNT(*) AS 数量 FROM md_item WHERE sale_flag='Y'
UNION ALL SELECT '询价单头', COUNT(*) FROM mes_inquiry_header
UNION ALL SELECT '询价单行', COUNT(*) FROM mes_inquiry_line
UNION ALL SELECT '微信用户表', COUNT(*) FROM mes_wx_user;

-- ----------------------------
-- 小程序「企业订购」询价单表结构
-- 数据库：j2eedb（与 MES 业务库共用）
-- 字符集：utf8mb4 / utf8mb4_0900_ai_ci（与现有表保持一致）
-- ----------------------------

-- ----------------------------
-- 1. 询价单头表
-- ----------------------------
DROP TABLE IF EXISTS `mes_inquiry_header`;
CREATE TABLE `mes_inquiry_header` (
  `inquiry_id` bigint NOT NULL AUTO_INCREMENT COMMENT '询价单ID',
  `inquiry_no` varchar(32) NOT NULL COMMENT '询价单号',
  `client_id` bigint DEFAULT NULL COMMENT '客户ID',
  `client_name` varchar(255) DEFAULT NULL COMMENT '客户名称',
  `buyer` varchar(64) DEFAULT NULL COMMENT '采购联系人',
  `contact_tel` varchar(64) DEFAULT NULL COMMENT '联系电话',
  `status` varchar(20) NOT NULL DEFAULT 'SUBMITTED' COMMENT '状态：DRAFT草稿 SUBMITTED待报价 QUOTED已报价 CONFIRMED已转订单 CANCELED已取消',
  `expected_date` date DEFAULT NULL COMMENT '期望交期',
  `delivery_address` varchar(500) DEFAULT NULL COMMENT '收货地址',
  `invoice_type` varchar(128) DEFAULT NULL COMMENT '开票方式',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `item_count` int NOT NULL DEFAULT '0' COMMENT '品项数',
  `total_qty` double(12,4) NOT NULL DEFAULT '0.0000' COMMENT '总数量',
  `quote_amount` decimal(14,2) DEFAULT NULL COMMENT '报价总金额',
  `quote_time` datetime DEFAULT NULL COMMENT '报价时间',
  `quote_valid_until` date DEFAULT NULL COMMENT '报价有效期至',
  `sales_name` varchar(64) DEFAULT NULL COMMENT '专属销售',
  `sales_tel` varchar(64) DEFAULT NULL COMMENT '销售电话',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`inquiry_id`),
  UNIQUE KEY `uk_inquiry_no` (`inquiry_no`),
  KEY `idx_client_id` (`client_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='询价单头表';

-- ----------------------------
-- 2. 询价单行表
-- ----------------------------
DROP TABLE IF EXISTS `mes_inquiry_line`;
CREATE TABLE `mes_inquiry_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '询价单行ID',
  `inquiry_id` bigint NOT NULL COMMENT '询价单ID',
  `line_no` int NOT NULL DEFAULT '1' COMMENT '行号',
  `item_id` bigint DEFAULT NULL COMMENT '物料ID（md_item.item_id）',
  `item_code` varchar(64) DEFAULT NULL COMMENT '物料编码（下单时快照）',
  `item_name` varchar(255) DEFAULT NULL COMMENT '物料名称（下单时快照）',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号（下单时快照）',
  `item_or_product` varchar(20) DEFAULT NULL COMMENT '产品物料标识：ITEM物料 PRODUCT成品',
  `qty` double(12,4) NOT NULL DEFAULT '0.0000' COMMENT '询价数量',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位',
  `moq` double(12,4) DEFAULT '0.0000' COMMENT '起订量',
  `lead_days` int DEFAULT NULL COMMENT '参考交期（天）',
  `quoted_price` decimal(14,4) DEFAULT NULL COMMENT '报价单价',
  `delivery_date` date DEFAULT NULL COMMENT '承诺交期',
  `line_amount` decimal(14,2) DEFAULT NULL COMMENT '行金额',
  `remark` varchar(500) DEFAULT NULL COMMENT '行备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_inquiry_id` (`inquiry_id`),
  KEY `idx_item_id` (`item_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='询价单行表';

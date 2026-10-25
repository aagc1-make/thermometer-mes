-- ----------------------------
-- 小程序「企业订购」独立后端数据表
-- 服务：backend/miniapp-api（端口 8081，接口前缀 /api/v1）
-- 数据库：j2eedb（与 MES 共用实例，但表统一 miniapp_ 前缀，MES 后端不读取）
-- 询价单仍复用 mes_inquiry_header / mes_inquiry_line（由小程序后端写入）
-- 字符集：utf8mb4 / utf8mb4_0900_ai_ci
-- ----------------------------

-- ----------------------------
-- 1. 企业客户
-- ----------------------------
DROP TABLE IF EXISTS `miniapp_customer`;
CREATE TABLE `miniapp_customer` (
  `client_id` bigint NOT NULL AUTO_INCREMENT COMMENT '客户ID',
  `company_name` varchar(255) NOT NULL COMMENT '企业名称',
  `short_name` varchar(16) DEFAULT NULL COMMENT '企业简称（头像文字）',
  `credit_code` varchar(64) DEFAULT NULL COMMENT '统一社会信用代码',
  `level_name` varchar(32) DEFAULT NULL COMMENT '客户等级',
  `level_desc` varchar(128) DEFAULT NULL COMMENT '等级说明',
  `buyer` varchar(128) DEFAULT NULL COMMENT '采购联系人',
  `region` varchar(128) DEFAULT NULL COMMENT '所在区域',
  `sales_name` varchar(64) DEFAULT NULL COMMENT '专属销售',
  `sales_phone` varchar(64) DEFAULT NULL COMMENT '销售电话',
  `stat_consulting` int NOT NULL DEFAULT '0' COMMENT '询价中',
  `stat_pending` int NOT NULL DEFAULT '0' COMMENT '待确认',
  `stat_closed` int NOT NULL DEFAULT '0' COMMENT '已成交',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`client_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小程序企业客户';

-- ----------------------------
-- 2. 收货地址
-- ----------------------------
DROP TABLE IF EXISTS `miniapp_address`;
CREATE TABLE `miniapp_address` (
  `address_id` bigint NOT NULL AUTO_INCREMENT COMMENT '地址ID',
  `client_id` bigint NOT NULL COMMENT '客户ID',
  `contact_name` varchar(64) NOT NULL COMMENT '收货人',
  `contact_tel` varchar(64) NOT NULL COMMENT '联系电话',
  `region` varchar(128) NOT NULL COMMENT '省市区',
  `detail` varchar(255) NOT NULL COMMENT '详细地址',
  `tag` varchar(16) DEFAULT NULL COMMENT '标签：默认/收货点/备用',
  `is_default` char(1) NOT NULL DEFAULT 'N' COMMENT '是否默认 Y/N',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`address_id`),
  KEY `idx_client_id` (`client_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小程序收货地址';

-- ----------------------------
-- 3. 订单
-- ----------------------------
DROP TABLE IF EXISTS `miniapp_order`;
CREATE TABLE `miniapp_order` (
  `order_id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` varchar(32) NOT NULL COMMENT '订单号',
  `client_id` bigint NOT NULL COMMENT '客户ID',
  `inquiry_no` varchar(32) DEFAULT NULL COMMENT '来源询价单号',
  `status` varchar(20) NOT NULL DEFAULT 'CONFIRMED' COMMENT '状态：QUOTED待报价 CONFIRMED待确认 PRODUCING生产中 DELIVERED已交付 CLOSED已关闭',
  `status_name` varchar(32) DEFAULT NULL COMMENT '状态名称',
  `item_count` int NOT NULL DEFAULT '0' COMMENT '品项数',
  `total_qty` double(12,4) NOT NULL DEFAULT '0.0000' COMMENT '总数量',
  `total_amount` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '订单金额',
  `delivery_address` varchar(500) DEFAULT NULL COMMENT '收货地址',
  `invoice_type` varchar(128) DEFAULT NULL COMMENT '开票方式',
  `expected_date` date DEFAULT NULL COMMENT '期望交期',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT NULL COMMENT '下单时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_client_id` (`client_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小程序订单';

-- ----------------------------
-- 4. 订单行
-- ----------------------------
DROP TABLE IF EXISTS `miniapp_order_line`;
CREATE TABLE `miniapp_order_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单行ID',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `line_no` int NOT NULL DEFAULT '1' COMMENT '行号',
  `item_code` varchar(64) DEFAULT NULL COMMENT '物料编码',
  `item_name` varchar(255) DEFAULT NULL COMMENT '物料名称',
  `specification` varchar(500) DEFAULT NULL COMMENT '规格型号',
  `qty` double(12,4) NOT NULL DEFAULT '0.0000' COMMENT '数量',
  `unit_name` varchar(32) DEFAULT NULL COMMENT '单位',
  `unit_price` decimal(14,4) DEFAULT NULL COMMENT '单价',
  `amount` decimal(14,2) DEFAULT NULL COMMENT '行金额',
  `art` varchar(32) DEFAULT NULL COMMENT '示意图类型',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小程序订单行';

-- ----------------------------
-- 5. 交付进度
-- ----------------------------
DROP TABLE IF EXISTS `miniapp_delivery`;
CREATE TABLE `miniapp_delivery` (
  `delivery_id` bigint NOT NULL AUTO_INCREMENT COMMENT '交付ID',
  `delivery_no` varchar(32) NOT NULL COMMENT '交付单号',
  `client_id` bigint NOT NULL COMMENT '客户ID',
  `order_no` varchar(32) NOT NULL COMMENT '订单号',
  `status` varchar(20) NOT NULL DEFAULT 'PRODUCING' COMMENT '状态：WAITING待排产 PRODUCING生产中 SHIPPED已发货 DELIVERED已签收',
  `status_name` varchar(32) DEFAULT NULL COMMENT '状态名称',
  `total_qty` double(12,4) NOT NULL DEFAULT '0.0000' COMMENT '订单总数量',
  `shipped_qty` double(12,4) NOT NULL DEFAULT '0.0000' COMMENT '已发货数量',
  `plan_date` date DEFAULT NULL COMMENT '计划交付日期',
  `ship_date` date DEFAULT NULL COMMENT '实际发货日期',
  `carrier` varchar(64) DEFAULT NULL COMMENT '承运商',
  `tracking_no` varchar(64) DEFAULT NULL COMMENT '运单号',
  `address` varchar(500) DEFAULT NULL COMMENT '收货地址',
  `progress` int NOT NULL DEFAULT '0' COMMENT '进度百分比',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`delivery_id`),
  UNIQUE KEY `uk_delivery_no` (`delivery_no`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小程序交付进度';

-- ----------------------------
-- 6. 对账单
-- ----------------------------
DROP TABLE IF EXISTS `miniapp_statement`;
CREATE TABLE `miniapp_statement` (
  `statement_id` bigint NOT NULL AUTO_INCREMENT COMMENT '对账单ID',
  `statement_no` varchar(32) NOT NULL COMMENT '对账单号',
  `client_id` bigint NOT NULL COMMENT '客户ID',
  `period` varchar(32) NOT NULL COMMENT '账期',
  `total_amount` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '应结金额',
  `paid_amount` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '已付金额',
  `unpaid_amount` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '未付金额',
  `status` varchar(20) NOT NULL DEFAULT 'UNPAID' COMMENT '状态：UNPAID待付款 PARTIAL部分付款 PAID已结清',
  `status_name` varchar(32) DEFAULT NULL COMMENT '状态名称',
  `bill_date` date DEFAULT NULL COMMENT '出账日期',
  `due_date` date DEFAULT NULL COMMENT '到期日期',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`statement_id`),
  UNIQUE KEY `uk_statement_no` (`statement_no`),
  KEY `idx_client_id` (`client_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小程序对账单';

-- ----------------------------
-- 7. 发票
-- ----------------------------
DROP TABLE IF EXISTS `miniapp_invoice`;
CREATE TABLE `miniapp_invoice` (
  `invoice_id` bigint NOT NULL AUTO_INCREMENT COMMENT '发票ID',
  `invoice_no` varchar(32) NOT NULL COMMENT '发票单号',
  `client_id` bigint NOT NULL COMMENT '客户ID',
  `invoice_title` varchar(255) NOT NULL COMMENT '发票抬头',
  `tax_no` varchar(64) DEFAULT NULL COMMENT '纳税人识别号',
  `invoice_type` varchar(64) NOT NULL DEFAULT '增值税专用发票 13%' COMMENT '发票类型',
  `amount` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '开票金额',
  `status` varchar(20) NOT NULL DEFAULT 'APPLIED' COMMENT '状态：APPLIED开票中 ISSUED已开票',
  `status_name` varchar(32) DEFAULT NULL COMMENT '状态名称',
  `apply_time` datetime DEFAULT NULL COMMENT '申请时间',
  `issue_time` datetime DEFAULT NULL COMMENT '开票时间',
  `email` varchar(128) DEFAULT NULL COMMENT '接收邮箱',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`invoice_id`),
  UNIQUE KEY `uk_invoice_no` (`invoice_no`),
  KEY `idx_client_id` (`client_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小程序发票';

-- ----------------------------
-- 8. 联系人
-- ----------------------------
DROP TABLE IF EXISTS `miniapp_contact`;
CREATE TABLE `miniapp_contact` (
  `contact_id` bigint NOT NULL AUTO_INCREMENT COMMENT '联系人ID',
  `client_id` bigint NOT NULL COMMENT '客户ID',
  `name` varchar(64) NOT NULL COMMENT '姓名',
  `dept` varchar(64) DEFAULT NULL COMMENT '部门/职务',
  `tel` varchar(64) DEFAULT NULL COMMENT '电话',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
  `is_default` char(1) NOT NULL DEFAULT 'N' COMMENT '是否默认 Y/N',
  PRIMARY KEY (`contact_id`),
  KEY `idx_client_id` (`client_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小程序联系人';

-- ----------------------------
-- 9. 帮助中心 FAQ
-- ----------------------------
DROP TABLE IF EXISTS `miniapp_faq`;
CREATE TABLE `miniapp_faq` (
  `faq_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'FAQ ID',
  `category` varchar(64) NOT NULL COMMENT '分类',
  `question` varchar(255) NOT NULL COMMENT '问题',
  `answer` varchar(1000) NOT NULL COMMENT '回答',
  `sort_no` int NOT NULL DEFAULT '0' COMMENT '排序',
  PRIMARY KEY (`faq_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小程序帮助中心';

-- ============================================================
-- 演示数据（固定 client_id = 1）
-- ============================================================

INSERT INTO `miniapp_customer`
(`client_id`, `company_name`, `short_name`, `credit_code`, `level_name`, `level_desc`, `buyer`, `region`, `sales_name`, `sales_phone`, `stat_consulting`, `stat_pending`, `stat_closed`, `create_time`)
VALUES
(1, '华瑞医疗器械有限公司', '华', '91320594MA1X***7K', '金牌客户', '金牌客户 · 月结 30 天', '采购员 王工 · 苏州工业园区', '苏州工业园区', '张经理', '138****6621', 3, 1, 8, NOW());

INSERT INTO `miniapp_address`
(`client_id`, `contact_name`, `contact_tel`, `region`, `detail`, `tag`, `is_default`, `create_time`, `update_time`)
VALUES
(1, '王工', '0512-8888 6621', '江苏省苏州市', '苏州工业园区星湖街 328 号 B 座 5 楼', '默认', 'Y', NOW(), NOW()),
(1, '李敏', '139****5528', '上海市', '浦东新区张江高科技园区博云路 2 号', '收货点', 'N', NOW(), NOW()),
(1, '陈工', '137****9012', '江苏省无锡市', '新吴区长江南路 18 号 3 号厂房', '备用', 'N', NOW(), NOW());

INSERT INTO `miniapp_order`
(`order_no`, `client_id`, `inquiry_no`, `status`, `status_name`, `item_count`, `total_qty`, `total_amount`, `delivery_address`, `invoice_type`, `expected_date`, `remark`, `create_time`, `update_time`)
VALUES
('SO20261020-002', 1, 'XJ20261020-002', 'QUOTED', '待报价', 1, 60, 0.00, '苏州工业园区星湖街 328 号 B 座 5 楼', '增值税专用发票 13%', '2026-11-03', '等待销售确认单价', '2026-10-20 14:32:00', '2026-10-20 14:32:00'),
('SO20261018-001', 1, 'XJ20261016-001', 'CONFIRMED', '待确认', 2, 260, 156000.00, '苏州工业园区星湖街 328 号 B 座 5 楼', '增值税专用发票 13%', '2026-11-08', '整机与预装件同批次交付', '2026-10-18 10:12:00', '2026-10-19 09:20:00'),
('SO20261015-001', 1, NULL, 'PRODUCING', '生产中', 2, 700, 182000.00, '苏州工业园区星湖街 328 号 B 座 5 楼', '增值税专用发票 13%', '2026-10-30', '已完成 SMT 贴片，进入整机组装', '2026-10-15 09:05:00', '2026-10-23 16:40:00'),
('SO20261012-001', 1, 'XJ20261012-001', 'PRODUCING', '生产中', 1, 500, 128000.00, '上海市浦东新区张江高科技园区博云路 2 号', '增值税专用发票 13%', '2026-10-26', '按客户固件版本定制烧录', '2026-10-12 09:18:00', '2026-10-22 11:10:00'),
('SO20261005-001', 1, NULL, 'DELIVERED', '已交付', 1, 200, 86400.00, '苏州工业园区星湖街 328 号 B 座 5 楼', '增值税专用发票 13%', '2026-10-10', '已签收，回单已上传', '2026-10-05 08:40:00', '2026-10-09 15:30:00'),
('SO20260928-001', 1, NULL, 'DELIVERED', '已交付', 3, 900, 236000.00, '江苏省无锡市新吴区长江南路 18 号 3 号厂房', '增值税专用发票 13%', '2026-10-08', '分两批发运，全部签收', '2026-09-28 15:22:00', '2026-10-08 17:05:00');

INSERT INTO `miniapp_order_line`
(`order_id`, `line_no`, `item_code`, `item_name`, `specification`, `qty`, `unit_name`, `unit_price`, `amount`, `art`, `create_time`)
VALUES
((SELECT order_id FROM miniapp_order WHERE order_no = 'SO20261020-002'), 1, 'IRG-FG-002', '医用高精度测温枪', '量程 32.0-42.9°C · 精度 ±0.1°C', 60, '件', 0.0000, 0.00, 'precision', NOW()),
((SELECT order_id FROM miniapp_order WHERE order_no = 'SO20261018-001'), 1, 'IRG-FG-001', '基础款红外测温枪', '量程 32.0-42.9°C · 精度 ±0.2°C', 200, '件', 432.0000, 86400.00, 'thermometer', NOW()),
((SELECT order_id FROM miniapp_order WHERE order_no = 'SO20261018-001'), 2, 'IRG-FG-002', '医用高精度测温枪', '量程 32.0-42.9°C · 精度 ±0.1°C', 60, '件', 1160.0000, 69600.00, 'precision', NOW()),
((SELECT order_id FROM miniapp_order WHERE order_no = 'SO20261015-001'), 1, 'IRG-SA-CTRL-001', '测温显示电控预装件', '主控 / 传感 / 显示预装', 500, '件', 156.0000, 78000.00, 'pcb', NOW()),
((SELECT order_id FROM miniapp_order WHERE order_no = 'SO20261015-001'), 2, 'IRG-SA-TRIG-001', '扳机侧键手柄预装件', '前壳 / 扳机 / 侧键预装', 200, '件', 520.0000, 104000.00, 'trigger', NOW()),
((SELECT order_id FROM miniapp_order WHERE order_no = 'SO20261012-001'), 1, 'IRG-SA-PCBA-001', '主板贴片组件', '四层板 · 已贴片 · 已烧录', 500, '件', 256.0000, 128000.00, 'mainboard', NOW()),
((SELECT order_id FROM miniapp_order WHERE order_no = 'SO20261005-001'), 1, 'IRG-FG-001', '基础款红外测温枪', '量程 32.0-42.9°C · 精度 ±0.2°C', 200, '件', 432.0000, 86400.00, 'thermometer', NOW()),
((SELECT order_id FROM miniapp_order WHERE order_no = 'SO20260928-001'), 1, 'IRG-FG-003', '壁挂式测温终端', '量程 30.0-45.0°C · 支持壁挂安装', 300, '台', 320.0000, 96000.00, 'wall', NOW()),
((SELECT order_id FROM miniapp_order WHERE order_no = 'SO20260928-001'), 2, 'IRG-SA-SENS-001', '红外传感模组', '数字输出 · 出厂单点校准', 400, '件', 190.0000, 76000.00, 'sensor', NOW()),
((SELECT order_id FROM miniapp_order WHERE order_no = 'SO20260928-001'), 3, 'IRG-ST-FRONT-001', '前壳注塑件', 'ABS 阻燃 · 白色哑光', 200, '件', 320.0000, 64000.00, 'trigger', NOW());

INSERT INTO `miniapp_delivery`
(`delivery_no`, `client_id`, `order_no`, `status`, `status_name`, `total_qty`, `shipped_qty`, `plan_date`, `ship_date`, `carrier`, `tracking_no`, `address`, `progress`, `remark`, `update_time`)
VALUES
('DL20261015-001', 1, 'SO20261015-001', 'PRODUCING', '生产中', 700, 0, '2026-10-30', NULL, NULL, NULL, '苏州工业园区星湖街 328 号 B 座 5 楼', 60, '整机组装中，预计 10 月 28 日完工', '2026-10-23 16:40:00'),
('DL20261012-001', 1, 'SO20261012-001', 'PRODUCING', '生产中', 500, 0, '2026-11-02', NULL, NULL, NULL, '上海市浦东新区张江高科技园区博云路 2 号', 35, '贴片已完成，等待固件烧录', '2026-10-22 11:10:00'),
('DL20261005-001', 1, 'SO20261005-001', 'DELIVERED', '已签收', 200, 200, '2026-10-10', '2026-10-07', '顺丰速运', 'SF1234567890123', '苏州工业园区星湖街 328 号 B 座 5 楼', 100, '客户已签收，回单已归档', '2026-10-09 15:30:00'),
('DL20260928-001', 1, 'SO20260928-001', 'DELIVERED', '已签收', 900, 900, '2026-10-08', '2026-10-06', '德邦物流', 'DP9876543210', '江苏省无锡市新吴区长江南路 18 号 3 号厂房', 100, '分两批发运，全部签收', '2026-10-08 17:05:00');

INSERT INTO `miniapp_statement`
(`statement_no`, `client_id`, `period`, `total_amount`, `paid_amount`, `unpaid_amount`, `status`, `status_name`, `bill_date`, `due_date`, `remark`)
VALUES
('DZ202609-001', 1, '2026 年 9 月', 236000.00, 236000.00, 0.00, 'PAID', '已结清', '2026-10-01', '2026-10-31', '已按合同约定结清'),
('DZ202610-001', 1, '2026 年 10 月', 156000.00, 60000.00, 96000.00, 'PARTIAL', '部分付款', '2026-10-20', '2026-11-19', '预付款 60000 元已到账');

INSERT INTO `miniapp_invoice`
(`invoice_no`, `client_id`, `invoice_title`, `tax_no`, `invoice_type`, `amount`, `status`, `status_name`, `apply_time`, `issue_time`, `email`, `remark`)
VALUES
('FP20261015-001', 1, '华瑞医疗器械有限公司', '91320594MA1X***7K', '增值税专用发票 13%', 128000.00, 'ISSUED', '已开票', '2026-10-12 10:00:00', '2026-10-15 16:20:00', 'finance@huarui-med.com', '纸质发票已寄出'),
('FP20261021-001', 1, '华瑞医疗器械有限公司', '91320594MA1X***7K', '增值税专用发票 13%', 156000.00, 'APPLIED', '开票中', '2026-10-21 09:40:00', NULL, 'finance@huarui-med.com', '财务审核中，预计 3 个工作日开具');

INSERT INTO `miniapp_contact`
(`client_id`, `name`, `dept`, `tel`, `email`, `is_default`)
VALUES
(1, '王工', '采购经理', '0512-8888 6621', 'wang@huarui-med.com', 'Y'),
(1, '李敏', '收货主管', '139****5528', 'li@huarui-med.com', 'N'),
(1, '陈工', '设备工程师', '137****9012', 'chen@huarui-med.com', 'N');

INSERT INTO `miniapp_faq`
(`category`, `question`, `answer`, `sort_no`)
VALUES
('询价与报价', '询价单提交后多久可以收到报价？', '工作时间内提交的询价单，销售会在 2 小时内联系确认；非工作时间顺延至下一个工作日。报价单有效期 7 天，请在有效期内确认。', 1),
('询价与报价', '起订量可以低于产品标称吗？', '成品起订量 50 件、半成品 100 件为标准起订量。小批量打样可与专属销售沟通，按样品政策执行。', 2),
('询价与报价', '半成品可以按我们的图纸定制吗？', '电控组件与结构件均支持定制，需提供图纸、BOM 与固件版本，销售会安排工程评估并给出定制交期。', 3),
('交付与物流', '如何查看订单的交付进度？', '在「我的 - 交付进度」中可查看每个订单的生产进度、计划交付日期与物流单号，也可联系专属销售获取每日进度。', 4),
('交付与物流', '发货后多久可以收到货物？', '江苏省内顺丰次日达，其他地区 2-3 天。大件物流按德邦时效执行，发货后可在交付进度中复制运单号查询。', 5),
('开票与对账', '发票什么时候可以开具？', '订单发货并确认收货后可申请开票，财务在 3 个工作日内开具。增值税专用发票需提供完整开票信息。', 6),
('开票与对账', '对账单在哪里查看？', '每月 1 日出上个月对账单，可在「我的 - 对账单」查看应结、已付与未付金额，账期为月结 30 天。', 7),
('售后与支持', '产品出现测量偏差怎么处理？', '请先确认环境温度与测量距离，仍异常可在「联系销售」中反馈，售后会在 24 小时内响应并提供校准或换货方案。', 8);

-- ============================================================
-- 演示：已报价询价单（供小程序「已报价 → 确认下单」链路使用）
-- 复用 mes_inquiry_header / mes_inquiry_line，仅写入演示数据
-- ============================================================
DELETE l FROM `mes_inquiry_line` l
  JOIN `mes_inquiry_header` h ON h.inquiry_id = l.inquiry_id
 WHERE h.inquiry_no = 'XJ20261019-001';
DELETE FROM `mes_inquiry_header` WHERE inquiry_no = 'XJ20261019-001';

INSERT INTO `mes_inquiry_header`
(`inquiry_no`, `client_id`, `client_name`, `buyer`, `contact_tel`, `status`, `expected_date`, `delivery_address`,
 `invoice_type`, `remark`, `item_count`, `total_qty`, `quote_amount`, `quote_time`, `quote_valid_until`,
 `sales_name`, `sales_tel`, `create_by`, `create_time`)
VALUES
('XJ20261019-001', 1, '华瑞医疗器械有限公司', '王工', '0512-8888 6621', 'QUOTED', '2026-11-15',
 '苏州工业园区星湖街 328 号 B 座 5 楼', '增值税专用发票 13%', '报价含税含运费，有效期 7 天', 2, 300,
 74000.00, '2026-10-21 10:20:00', '2026-10-28', '张经理', '138****6621', 'miniapp', '2026-10-19 09:30:00');

INSERT INTO `mes_inquiry_line`
(`inquiry_id`, `line_no`, `item_id`, `item_code`, `item_name`, `specification`, `item_or_product`, `qty`,
 `unit_name`, `moq`, `lead_days`, `quoted_price`, `delivery_date`, `line_amount`, `create_by`, `create_time`)
VALUES
((SELECT inquiry_id FROM mes_inquiry_header WHERE inquiry_no = 'XJ20261019-001'), 1,
 (SELECT item_id FROM md_item WHERE item_code = 'IRG-FG-001'), 'IRG-FG-001',
 '基础款红外测温枪', '量程 32.0-42.9°C · 精度 ±0.2°C', 'PRODUCT', 100, '件', 50, 7, 428.0000, '2026-11-12', 42800.00, 'miniapp', NOW()),
((SELECT inquiry_id FROM mes_inquiry_header WHERE inquiry_no = 'XJ20261019-001'), 2,
 (SELECT item_id FROM md_item WHERE item_code = 'IRG-SA-CTRL-001'), 'IRG-SA-CTRL-001',
 '测温显示电控预装件', '主控 / 传感 / 显示预装', 'PRODUCT', 200, '件', 100, 10, 156.0000, '2026-11-15', 31200.00, 'miniapp', NOW());

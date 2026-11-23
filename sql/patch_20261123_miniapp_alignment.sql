-- 小程序（miniapp）与 MES 数据/接口对齐
-- 1) 目录：新增 miniapp_product 维护订货参数，商品本身仍取 md_item 真实主数据
-- 2) 客户/账号：按 md_client 的 9751~9753 三家客户建档，账号 c-tg-001~003 / 密码 123456
-- 3) 地址/联系人/FAQ：补齐小程序演示所需基础数据
SET NAMES utf8mb4;

-- ==================== 订货商品参数 ====================
CREATE TABLE IF NOT EXISTS `miniapp_product` (
  `item_code`  varchar(64)   NOT NULL COMMENT '物料编码，对应 md_item.item_code',
  `moq`        decimal(12,4) NOT NULL DEFAULT 1 COMMENT '起订量',
  `lead_days`  int           NOT NULL DEFAULT 7 COMMENT '交付天数',
  `art`        varchar(16)            DEFAULT NULL COMMENT '产品示意图类型',
  `summary`    varchar(255)           DEFAULT NULL COMMENT '小程序详情说明',
  `sort_no`    int           NOT NULL DEFAULT 0 COMMENT '排序',
  `enabled`    char(1)       NOT NULL DEFAULT 'Y' COMMENT '是否上架',
  `update_time` datetime              DEFAULT NULL,
  PRIMARY KEY (`item_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序订货商品参数';

REPLACE INTO `miniapp_product` (`item_code`,`moq`,`lead_days`,`art`,`summary`,`sort_no`,`enabled`,`update_time`) VALUES
('TG-FG-001', 10, 15, 'thermometer', '本期唯一成品，演示生产 10 台', 1, 'Y', NOW()),
('TG-SF-001', 50, 10, 'pcb', '半成品，需总装前预装并过程检验', 2, 'Y', NOW()),
('TG-SF-002', 50, 10, 'trigger', '半成品，需过程检验后供总装', 3, 'Y', NOW()),
('TG-EL-001', 500, 5, 'mainboard', '按整体外购，BOM 不展开内部器件', 4, 'Y', NOW()),
('TG-EL-002', 500, 5, 'sensor', '测温核心器件', 5, 'Y', NOW()),
('TG-EL-003', 500, 5, 'pcb', '显示测温结果与状态', 6, 'Y', NOW()),
('TG-EL-004', 500, 5, 'pcb', '手柄预装件组成', 7, 'Y', NOW()),
('TG-EL-005', 500, 5, 'pcb', '超温与按键提示音', 8, 'Y', NOW()),
('TG-EL-006', 1000, 3, 'pcb', '手柄预装件组成', 9, 'Y', NOW()),
('TG-ST-001', 500, 5, 'trigger', '整机结构件', 10, 'Y', NOW()),
('TG-ST-002', 500, 5, 'trigger', '整机结构件', 11, 'Y', NOW()),
('TG-ST-003', 500, 5, 'trigger', '手柄预装件组成', 12, 'Y', NOW()),
('TG-ST-004', 500, 5, 'sensor', '电控预装件组成', 13, 'Y', NOW()),
('TG-ST-005', 1000, 3, 'pcb', '整机装配紧固件', 14, 'Y', NOW()),
('TG-ST-006', 1000, 3, 'pcb', '电控件缓冲固定', 15, 'Y', NOW()),
('TG-PK-001', 1000, 3, 'pcb', '整机包装附件', 16, 'Y', NOW()),
('TG-PK-002', 1000, 3, 'pcb', '整机包装附件', 17, 'Y', NOW());

-- ==================== 企业客户（与 md_client 一致） ====================
REPLACE INTO `miniapp_customer`
(`client_id`,`company_name`,`short_name`,`credit_code`,`level_name`,`level_desc`,`buyer`,`region`,`sales_name`,`sales_phone`,`stat_consulting`,`stat_pending`,`stat_closed`,`create_time`) VALUES
(9751,'华东医疗器械经销有限公司','华东医疗','91310115MA1K35XXXX','A 级战略客户','A 级战略客户 · 月结 30 天','张采购','上海浦东新区','李销售','13800000001',2,1,6,NOW()),
(9752,'北京康泰健康科技有限公司','北京康泰','91110105MA01K36YYYY','B 级客户','B 级客户 · 月结 45 天','王工','北京朝阳区','陈销售','13800000002',1,0,3,NOW()),
(9753,'广州社区健康服务中心','广州社区','91440104MA5C37ZZZZ','B 级客户','B 级客户 · 款到发货','刘主任','广州越秀区','周销售','13800000003',0,0,2,NOW());

-- ==================== 企业账号（密码统一 123456，SHA-256(salt+明文)） ====================
REPLACE INTO `miniapp_account`
(`client_id`,`username`,`password_hash`,`salt`,`display_name`,`enabled`,`create_time`) VALUES
(9751,'c-tg-001','c787b54f9a7fd95fdbcd7a7d6af2d87bb8bfb384911a0df26d568669b086d41c','ktg-mp-001','华东医疗','Y',NOW()),
(9752,'c-tg-002','d945461976cc589c31264c78ea51ad2961b8c78f891716e5af7b19416daefd17','ktg-mp-002','北京康泰','Y',NOW()),
(9753,'c-tg-003','98f92dfa634705715b142f62f843dbe0d8e86b98f75eb01e0411d24398d0786c','ktg-mp-003','广州社区','Y',NOW());

-- ==================== 收货地址 / 联系人 ====================
DELETE FROM `miniapp_address` WHERE `client_id` IN (9751,9752,9753);
INSERT INTO `miniapp_address` (`client_id`,`contact_name`,`contact_tel`,`region`,`detail`,`tag`,`is_default`,`create_time`,`update_time`) VALUES
(9751,'张采购','13800000001','上海市浦东新区','张江路 88 号 3 号楼','总部','Y',NOW(),NOW()),
(9752,'王工','13800000002','北京市朝阳区','建国路 66 号 A 座 12 层','总部','Y',NOW(),NOW()),
(9753,'刘主任','13800000003','广州市越秀区','中山二路 18 号社区中心','收货点','Y',NOW(),NOW());

DELETE FROM `miniapp_contact` WHERE `client_id` IN (9751,9752,9753);
INSERT INTO `miniapp_contact` (`client_id`,`name`,`dept`,`tel`,`email`,`is_default`) VALUES
(9751,'张采购','采购部','13800000001','zhang@huadong.example','Y'),
(9752,'王工','采购部','13800000002','wang@kangtai.example','Y'),
(9753,'刘主任','后勤部','13800000003','liu@gzsq.example','Y');

-- ==================== 帮助中心 ====================
REPLACE INTO `miniapp_faq` (`faq_id`,`category`,`question`,`answer`,`sort_no`) VALUES
(1,'询价下单','询价单多久可以收到报价？','销售会在 2 小时内响应，报价有效期默认 7 天。',1),
(2,'询价下单','起订量可以调整吗？','起订量为参考值，批量需求可联系专属销售协商。',2),
(3,'交付物流','如何查询交付进度？','在「我的-交付进度」中按订单号查看生产与发货节点。',3);

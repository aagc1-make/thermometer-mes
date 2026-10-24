-- ============================================================================
--  红外测温枪 MES 系统 —— 数据库初始化脚本（结构补丁 + 演示主数据）
--  依据：《红外测温枪MES系统_软件需求规格说明书》 §1.2.2 / §2 / §3.4.5
--  数据库：MySQL 8.x   库名：j2eedb（连接配置见 application-druid.yml）
--
--  设计原则：
--   1. 结构沿用 KTG MES 现有 181 张表，只按需求补充字段（幂等）；
--   2. 本脚本清空全部 MES 业务表后重新灌入「测温枪」主数据，
--      不会删除 sys_* 基础表结构，也不会删除内置 admin 账号；
--   3. 可重复执行：先 DELETE 再 INSERT，ID 固定，多次执行结果一致；
--   4. 单据（到货单/工单/报工/出入库）不在此脚本中伪造，
--      由 scripts/demo-flow.mjs 按业务流程真实生成，保证库存流水与单据状态自洽。
--
--  演示规模：1 个成品 + 2 个自制半成品 + 14 种外购物料 = 17 个物料产品节点，
--           16 行多级 BOM、9 道工序、3 条工艺路线、9 个工作站、
--           4 个仓库（含虚拟线边库）、10 个检测项、4 套检验方案、
--           5 台设备、2 个班组、8 类岗位账号。
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------------------------------------------------------
-- 0. 结构补丁：仓库增加「仓库类型」字段（原料仓/线边库/成品仓）
--    需求 §2.4.1：仓库类型（原料仓 / 线边库 / 成品仓）为仓储档案必填项
-- ----------------------------------------------------------------------------
SET @ddl = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE wm_warehouse ADD COLUMN warehouse_type varchar(32) DEFAULT NULL COMMENT ''仓库类型(RAW/LINE/FG)'' AFTER warehouse_name',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'wm_warehouse' AND column_name = 'warehouse_type'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------------------------------------------------------
-- 1. 清空 MES 业务数据（155 张 md_/pro_/wm_/qc_/dv_/cal_/tm_/iot_/print_/report_ 表）
-- ----------------------------------------------------------------------------
DELETE FROM cal_holiday;
DELETE FROM cal_plan;
DELETE FROM cal_plan_team;
DELETE FROM cal_shift;
DELETE FROM cal_team;
DELETE FROM cal_team_member;
DELETE FROM cal_teamshift;
DELETE FROM dv_check_machinery;
DELETE FROM dv_check_plan;
DELETE FROM dv_check_record;
DELETE FROM dv_check_record_line;
DELETE FROM dv_check_subject;
DELETE FROM dv_machinery;
DELETE FROM dv_machinery_type;
DELETE FROM dv_mainten_record;
DELETE FROM dv_mainten_record_line;
DELETE FROM dv_repair;
DELETE FROM dv_repair_line;
DELETE FROM dv_subject;
DELETE FROM iot_device;
DELETE FROM iot_device_property;
DELETE FROM iot_device_stproperty;
DELETE FROM iot_model;
DELETE FROM iot_model_property;
DELETE FROM iot_model_stproperty;
DELETE FROM md_client;
DELETE FROM md_item;
DELETE FROM md_item_batch_config;
DELETE FROM md_item_type;
DELETE FROM md_product_bom;
DELETE FROM md_product_sip;
DELETE FROM md_product_sop;
DELETE FROM md_produt_sop;
DELETE FROM md_unit_measure;
DELETE FROM md_vendor;
DELETE FROM md_workshop;
DELETE FROM md_workstation;
DELETE FROM md_workstation_machine;
DELETE FROM md_workstation_tool;
DELETE FROM md_workstation_worker;
DELETE FROM print_client;
DELETE FROM print_printer_config;
DELETE FROM print_template;
DELETE FROM pro_andon_config;
DELETE FROM pro_andon_record;
DELETE FROM pro_card;
DELETE FROM pro_card_process;
DELETE FROM pro_feedback;
DELETE FROM pro_process;
DELETE FROM pro_process_content;
DELETE FROM pro_route;
DELETE FROM pro_route_process;
DELETE FROM pro_route_product;
DELETE FROM pro_route_product_bom;
DELETE FROM pro_shutdown_record;
DELETE FROM pro_sn_process;
DELETE FROM pro_task;
DELETE FROM pro_task_issue;
DELETE FROM pro_trans_consume;
DELETE FROM pro_trans_order;
DELETE FROM pro_user_workstation;
DELETE FROM pro_workorder;
DELETE FROM pro_workorder_bom;
DELETE FROM pro_workrecord;
DELETE FROM qc_defect;
DELETE FROM qc_defect_record;
DELETE FROM qc_index;
DELETE FROM qc_ipqc;
DELETE FROM qc_ipqc_line;
DELETE FROM qc_iqc;
DELETE FROM qc_iqc_defect;
DELETE FROM qc_iqc_line;
DELETE FROM qc_oqc;
DELETE FROM qc_oqc_line;
DELETE FROM qc_result;
DELETE FROM qc_result_detail;
DELETE FROM qc_rqc;
DELETE FROM qc_rqc_line;
DELETE FROM qc_template;
DELETE FROM qc_template_index;
DELETE FROM qc_template_product;
DELETE FROM report_chart;
DELETE FROM report_chart_role;
DELETE FROM sys_attachment;
DELETE FROM sys_todo_list;
DELETE FROM tm_tool;
DELETE FROM tm_tool_type;
DELETE FROM wm_arrival_notice;
DELETE FROM wm_arrival_notice_line;
DELETE FROM wm_barcode;
DELETE FROM wm_barcode_config;
DELETE FROM wm_batch;
DELETE FROM wm_issue_detail;
DELETE FROM wm_issue_header;
DELETE FROM wm_issue_line;
DELETE FROM wm_item_consume;
DELETE FROM wm_item_consume_detail;
DELETE FROM wm_item_consume_line;
DELETE FROM wm_item_recpt;
DELETE FROM wm_item_recpt_detail;
DELETE FROM wm_item_recpt_line;
DELETE FROM wm_material_stock;
DELETE FROM wm_materialrequest_notice;
DELETE FROM wm_materialrequest_notice_line;
DELETE FROM wm_misc_issue;
DELETE FROM wm_misc_issue_detail;
DELETE FROM wm_misc_issue_line;
DELETE FROM wm_misc_recpt;
DELETE FROM wm_misc_recpt_detail;
DELETE FROM wm_misc_recpt_line;
DELETE FROM wm_outsource_issue;
DELETE FROM wm_outsource_issue_detail;
DELETE FROM wm_outsource_issue_line;
DELETE FROM wm_outsource_recpt;
DELETE FROM wm_outsource_recpt_detail;
DELETE FROM wm_outsource_recpt_line;
DELETE FROM wm_package;
DELETE FROM wm_package_line;
DELETE FROM wm_pback_header;
DELETE FROM wm_pback_line;
DELETE FROM wm_product_produce;
DELETE FROM wm_product_produce_detail;
DELETE FROM wm_product_produce_line;
DELETE FROM wm_product_recpt;
DELETE FROM wm_product_recpt_detail;
DELETE FROM wm_product_recpt_line;
DELETE FROM wm_product_sales;
DELETE FROM wm_product_sales_detail;
DELETE FROM wm_product_sales_line;
DELETE FROM wm_product_salse;
DELETE FROM wm_product_salse_line;
DELETE FROM wm_rt_issue;
DELETE FROM wm_rt_issue_detail;
DELETE FROM wm_rt_issue_line;
DELETE FROM wm_rt_sales;
DELETE FROM wm_rt_sales_detail;
DELETE FROM wm_rt_sales_line;
DELETE FROM wm_rt_vendor;
DELETE FROM wm_rt_vendor_detail;
DELETE FROM wm_rt_vendor_line;
DELETE FROM wm_sales_notice;
DELETE FROM wm_sales_notice_line;
DELETE FROM wm_sn;
DELETE FROM wm_stock_taking;
DELETE FROM wm_stock_taking_line;
DELETE FROM wm_stock_taking_param;
DELETE FROM wm_stock_taking_plan;
DELETE FROM wm_stock_taking_result;
DELETE FROM wm_storage_area;
DELETE FROM wm_storage_location;
DELETE FROM wm_transaction;
DELETE FROM wm_transfer;
DELETE FROM wm_transfer_detail;
DELETE FROM wm_transfer_line;
DELETE FROM wm_warehouse;

-- ----------------------------------------------------------------------------
-- 2. 字典补充（仓库类型、经销商客户类型、检验待判定结论）
-- ----------------------------------------------------------------------------
DELETE FROM sys_dict_data WHERE dict_type = 'mes_warehouse_type';
DELETE FROM sys_dict_type WHERE dict_type = 'mes_warehouse_type';

INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, remark, create_by, create_time)
VALUES (900, '仓库类型', 'mes_warehouse_type', '0', '仓储基础档案仓库类型', 'admin', NOW());

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
VALUES
 (9001, 1, '原料仓', 'RAW',  'mes_warehouse_type', '', 'primary', 'Y', '0', 'admin', NOW()),
 (9002, 2, '线边库', 'LINE', 'mes_warehouse_type', '', 'warning', 'N', '0', 'admin', NOW()),
 (9003, 3, '成品仓', 'FG',   'mes_warehouse_type', '', 'success', 'N', '0', 'admin', NOW());

-- 客户类型：补充「经销商」（需求 §2.3.7 客商管理）
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT 3, '经销商', 'DEALER', 'mes_client_type', '', 'info', 'N', '0', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_client_type' AND dict_value = 'DEALER');

-- 检验结论：补充「待判定」（需求 §2.6.4 / §2.6.6：无参考基准时只能判待判定）
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT 3, '待判定', 'PENDING', 'mes_qc_result', '', 'warning', 'N', '0', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_qc_result' AND dict_value = 'PENDING');

-- ----------------------------------------------------------------------------
-- 3. 岗位角色与演示账号（需求 §1.3：8 类角色各 1 个演示账号）
--    密码统一为 admin123（BCrypt），超级管理员沿用内置 admin
-- ----------------------------------------------------------------------------
DELETE FROM sys_role_menu WHERE role_id BETWEEN 101 AND 107;
DELETE FROM sys_user_role WHERE user_id BETWEEN 200 AND 206;
DELETE FROM sys_user WHERE user_id BETWEEN 200 AND 206;
DELETE FROM sys_role WHERE role_id BETWEEN 101 AND 107;

INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
VALUES
 (101, '工艺员',     'craftsman',  1, '1', 1, 1, '0', '0', 'admin', NOW(), '维护物料产品、多级BOM、工序与工艺路线'),
 (102, '计划员',     'planner',    2, '1', 1, 1, '0', '0', 'admin', NOW(), '创建生产工单、排产并跟踪进度'),
 (103, '仓库员',     'warehouse',  3, '1', 1, 1, '0', '0', 'admin', NOW(), '采购入库、生产领退料、产品入库、销售出库'),
 (104, '生产操作员', 'operator',   4, '1', 1, 1, '0', '0', 'admin', NOW(), '执行装配任务并登记报工与工序流转'),
 (105, '质检员',     'inspector',  5, '1', 1, 1, '0', '0', 'admin', NOW(), '执行IQC/IPQC/OQC检验与不良处置'),
 (106, '设备员',     'equipment',  6, '1', 1, 1, '0', '0', 'admin', NOW(), '设备台账、点检保养与维修'),
 (107, '班组长',     'teamleader', 7, '1', 1, 1, '0', '0', 'admin', NOW(), '班组人员、班次与排班计划');

INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, remark)
VALUES
 (200, 100, 'craftsman',  '王工艺',   '00', 'craftsman@tgmes.local',  '13800000200', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), '工艺员演示账号'),
 (201, 100, 'planner',    '李计划',   '00', 'planner@tgmes.local',    '13800000201', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), '计划员演示账号'),
 (202, 100, 'warehouse',  '张仓管',   '00', 'warehouse@tgmes.local',  '13800000202', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), '仓库员演示账号'),
 (203, 100, 'operator',   '赵操作',   '00', 'operator@tgmes.local',   '13800000203', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), '生产操作员演示账号'),
 (204, 100, 'inspector',  '孙质检',   '00', 'inspector@tgmes.local',  '13800000204', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), '质检员演示账号'),
 (205, 100, 'equipment',  '周设备',   '00', 'equipment@tgmes.local',  '13800000205', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), '设备员演示账号'),
 (206, 100, 'teamleader', '吴班组长', '00', 'teamleader@tgmes.local', '13800000206', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), '班组长演示账号');

INSERT INTO sys_user_role (user_id, role_id) VALUES (200,101),(201,102),(202,103),(203,104),(204,105),(205,106),(206,107);

-- 清空内置账号的历史头像路径（原路径指向不存在的文件，登录后右上角会显示碎图，
-- 清空后前端统一回退到默认头像 assets/images/profile.jpg）
UPDATE sys_user SET avatar = '' WHERE avatar IS NOT NULL AND avatar <> '';

-- 角色菜单授权：按权限标识前缀授予业务菜单，并补上父级目录（否则侧边栏无法渲染）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 101, menu_id FROM sys_menu
WHERE perms LIKE 'mes:md:%' OR perms LIKE 'mes:pro:process%' OR perms LIKE 'mes:pro:proroute%'
   OR perms LIKE 'mes:qc:qcindex%' OR perms LIKE 'mes:qc:qctemplate%'
   OR menu_id IN (2000, 2072, 2124);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 102, menu_id FROM sys_menu
WHERE perms LIKE 'mes:pro:%' OR perms LIKE 'mes:md:%' OR perms LIKE 'mes:wm:wmstock%'
   OR menu_id IN (2000, 2042, 2072);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 103, menu_id FROM sys_menu
WHERE perms LIKE 'mes:wm:%' OR perms LIKE 'mes:md:mditem:list' OR perms LIKE 'mes:md:client:list' OR perms LIKE 'mes:md:vendor:list'
   OR menu_id IN (2000, 2042);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 104, menu_id FROM sys_menu
WHERE perms LIKE 'mes:pro:feedback%' OR perms LIKE 'mes:pro:protask%' OR perms LIKE 'mes:pro:procard%'
   OR menu_id IN (2072);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 105, menu_id FROM sys_menu
WHERE perms LIKE 'mes:qc:%' OR perms LIKE 'mes:wm:wmstock%'
   OR menu_id IN (2042, 2124);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 106, menu_id FROM sys_menu
WHERE perms LIKE 'mes:dv:%' OR menu_id IN (2059);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 107, menu_id FROM sys_menu
WHERE perms LIKE 'mes:cal:%' OR perms LIKE 'mes:pro:protask%' OR perms LIKE 'mes:pro:feedback%'
   OR menu_id IN (2160, 2072);

-- ----------------------------------------------------------------------------
-- 4. 计量单位（md_unit_measure）
-- ----------------------------------------------------------------------------
INSERT INTO md_unit_measure (measure_id, measure_code, measure_name, primary_flag, change_rate, enable_flag, remark, attr3, attr4, create_by, create_time)
VALUES
 (9001, 'PCS',  '个', 'Y', NULL,   'Y', '计数单位-主单位', 0, 0, 'admin', NOW()),
 (9002, 'SET',  '套', 'N', 1.0000, 'Y', '预装件成套单位',   0, 0, 'admin', NOW()),
 (9003, 'TAI',  '台', 'N', 1.0000, 'Y', '整机单位',         0, 0, 'admin', NOW()),
 (9004, 'COPY', '份', 'N', 1.0000, 'Y', '说明书等单位',     0, 0, 'admin', NOW()),
 (9005, 'BAG',  '包', 'N', 1.0000, 'Y', '螺丝包等单位',     0, 0, 'admin', NOW());

-- ----------------------------------------------------------------------------
-- 5. 物料产品分类（md_item_type）
-- ----------------------------------------------------------------------------
INSERT INTO md_item_type (item_type_id, item_type_code, item_type_name, parent_type_id, ancestors, item_or_product, order_num, enable_flag, remark, attr3, attr4, create_by, create_time)
VALUES
 (9101, 'TG_ROOT', '测温枪物料产品', 0,    '0',           'PRODUCT', 1, 'Y', '红外测温枪产品与物料根分类', 0, 0, 'admin', NOW()),
 (9102, 'TG_FG',   '整机成品',       9101, '0,9101',      'PRODUCT', 1, 'Y', '可直接销售的测温枪整机',       0, 0, 'admin', NOW()),
 (9103, 'TG_SF',   '自制半成品',     9101, '0,9101',      'PRODUCT', 2, 'Y', '需继续参与总装的预装件',       0, 0, 'admin', NOW()),
 (9104, 'TG_ITEM', '外购物料',       9101, '0,9101',      'ITEM',    3, 'Y', '采购到货的外购件',            0, 0, 'admin', NOW()),
 (9105, 'TG_ELEC', '电子件',         9104, '0,9101,9104', 'ITEM',    1, 'Y', 'PCBA、传感器、显示与按键组件', 0, 0, 'admin', NOW()),
 (9106, 'TG_STRU', '结构件',         9104, '0,9101,9104', 'ITEM',    2, 'Y', '外壳、扳机、透镜与紧固件',     0, 0, 'admin', NOW()),
 (9107, 'TG_PACK', '包装辅料',       9104, '0,9101,9104', 'ITEM',    3, 'Y', '说明书与彩盒',                0, 0, 'admin', NOW());

-- ----------------------------------------------------------------------------
-- 6. 物料与产品档案（md_item）：17 个节点
--    成品 1 + 自制半成品 2 + 外购物料 14
-- ----------------------------------------------------------------------------
INSERT INTO md_item (item_id, item_code, item_name, specification, unit_of_measure, item_or_product, item_type_id, item_type_code, item_type_name, enable_flag, safe_stock_flag, high_value, unit_name, batch_flag, remark, create_by, create_time)
VALUES
 -- 整机成品
 (9201, 'TG-FG-001', '红外测温枪整机', 'TG-300 便携式红外测温枪（32~42.9℃）', 'TAI', 'PRODUCT', 9102, 'TG_FG', '整机成品', 'Y', 'N', 'N', '台', 'Y', '本期唯一成品，演示生产 10 台', 'admin', NOW()),
 -- 自制半成品
 (9202, 'TG-SF-001', '测温显示电控预装件', '电控预装件（PCBA+传感器+LCD+蜂鸣器+透镜）', 'SET', 'PRODUCT', 9103, 'TG_SF', '自制半成品', 'Y', 'N', 'N', '套', 'Y', '半成品，需总装前预装并过程检验', 'admin', NOW()),
 (9203, 'TG-SF-002', '扳机与侧键手柄预装件', '手柄预装件（三键按键板+电池+扳机组件）', 'SET', 'PRODUCT', 9103, 'TG_SF', '自制半成品', 'Y', 'N', 'N', '套', 'Y', '半成品，需过程检验后供总装', 'admin', NOW()),
 -- 外购物料：电子件
 (9204, 'TG-EL-001', '主控PCBA', 'TG-MAIN-V1.2 主控板（已贴片）', 'PCS', 'ITEM', 9105, 'TG_ELEC', '电子件', 'Y', 'N', 'Y', '个', 'Y', '按整体外购，BOM 不展开内部器件', 'admin', NOW()),
 (9205, 'TG-EL-002', '红外传感器模块', 'MLX90614 兼容 TO-39 红外测温传感器', 'PCS', 'ITEM', 9105, 'TG_ELEC', '电子件', 'Y', 'N', 'Y', '个', 'Y', '测温核心器件', 'admin', NOW()),
 (9206, 'TG-EL-003', '显示组件', '1.0 寸段码 LCD 显示模组', 'PCS', 'ITEM', 9105, 'TG_ELEC', '电子件', 'Y', 'N', 'N', '个', 'Y', '显示测温结果与状态', 'admin', NOW()),
 (9207, 'TG-EL-004', '三键按键板', '上/下/设置 三键薄膜按键板', 'PCS', 'ITEM', 9105, 'TG_ELEC', '电子件', 'Y', 'N', 'N', '个', 'Y', '手柄预装件组成', 'admin', NOW()),
 (9208, 'TG-EL-005', '蜂鸣器', '5V 有源蜂鸣器', 'PCS', 'ITEM', 9105, 'TG_ELEC', '电子件', 'Y', 'N', 'N', '个', 'Y', '超温与按键提示音', 'admin', NOW()),
 (9209, 'TG-EL-006', '电池', 'AAA 1.5V 碱性电池', 'PCS', 'ITEM', 9105, 'TG_ELEC', '电子件', 'Y', 'N', 'N', '节', 'Y', '手柄预装件组成', 'admin', NOW()),
 -- 外购物料：结构件
 (9210, 'TG-ST-001', '上壳', 'ABS 白色 上壳', 'PCS', 'ITEM', 9106, 'TG_STRU', '结构件', 'Y', 'N', 'N', '个', 'Y', '整机结构件', 'admin', NOW()),
 (9211, 'TG-ST-002', '下壳', 'ABS 白色 下壳', 'PCS', 'ITEM', 9106, 'TG_STRU', '结构件', 'Y', 'N', 'N', '个', 'Y', '整机结构件', 'admin', NOW()),
 (9212, 'TG-ST-003', '扳机组件', '扳机+复位弹簧组件', 'PCS', 'ITEM', 9106, 'TG_STRU', '结构件', 'Y', 'N', 'N', '个', 'Y', '手柄预装件组成', 'admin', NOW()),
 (9213, 'TG-ST-004', '红外透镜', 'HDPE 红外聚焦透镜', 'PCS', 'ITEM', 9106, 'TG_STRU', '结构件', 'Y', 'N', 'N', '个', 'Y', '电控预装件组成', 'admin', NOW()),
 (9214, 'TG-ST-005', '螺丝包', 'M2×6 自攻螺丝 6 颗/包', 'BAG', 'ITEM', 9106, 'TG_STRU', '结构件', 'Y', 'N', 'N', '包', 'Y', '整机装配紧固件', 'admin', NOW()),
 (9215, 'TG-ST-006', '防震泡棉', 'EVA 防震泡棉 30×20mm', 'PCS', 'ITEM', 9106, 'TG_STRU', '结构件', 'Y', 'N', 'N', '片', 'Y', '电控件缓冲固定', 'admin', NOW()),
 -- 外购物料：包装辅料
 (9216, 'TG-PK-001', '说明书', '中文版使用说明书（8 页）', 'COPY', 'ITEM', 9107, 'TG_PACK', '包装辅料', 'Y', 'N', 'N', '份', 'Y', '整机包装附件', 'admin', NOW()),
 (9217, 'TG-PK-002', '彩盒包装', '350×200×80mm 彩盒', 'PCS', 'ITEM', 9107, 'TG_PACK', '包装辅料', 'Y', 'N', 'N', '个', 'Y', '整机包装附件', 'admin', NOW());

-- 批次管理配置：物料 batch_flag='Y' 时必须存在该配置，否则入库生成批次号会报
-- 「物料 XXX 未配置批次管理!」。演示统一使用序号批次，不额外要求生产日期等参数。
INSERT INTO md_item_batch_config (config_id, item_id, produce_date_flag, expire_date_flag, recpt_date_flag, vendor_flag, client_flag, co_code_flag, po_code_flag, workorder_flag, task_flag, workstation_flag, tool_flag, mold_flag, lot_number_flag, quality_status_flag, enable_flag, remark, create_by, create_time)
VALUES
 (9111, 9201, 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', '整机批次管理', 'admin', NOW()),
 (9112, 9202, 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', '电控预装件批次管理', 'admin', NOW()),
 (9113, 9203, 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', '手柄预装件批次管理', 'admin', NOW()),
 (9114, 9204, 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', '主控PCBA批次管理', 'admin', NOW()),
 (9115, 9205, 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', '传感器批次管理', 'admin', NOW()),
 (9116, 9206, 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', '显示组件批次管理', 'admin', NOW()),
 (9117, 9207, 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', '按键板批次管理', 'admin', NOW()),
 (9118, 9208, 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', '蜂鸣器批次管理', 'admin', NOW()),
 (9119, 9209, 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', '电池批次管理', 'admin', NOW()),
 (9120, 9210, 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', '上壳批次管理', 'admin', NOW()),
 (9121, 9211, 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', '下壳批次管理', 'admin', NOW()),
 (9122, 9212, 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', '扳机组件批次管理', 'admin', NOW()),
 (9123, 9213, 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', '红外透镜批次管理', 'admin', NOW()),
 (9124, 9214, 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', '螺丝包批次管理', 'admin', NOW()),
 (9125, 9215, 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', '防震泡棉批次管理', 'admin', NOW()),
 (9126, 9216, 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', '说明书批次管理', 'admin', NOW()),
 (9127, 9217, 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'Y', '彩盒包装批次管理', 'admin', NOW());

-- ----------------------------------------------------------------------------
-- 7. 多级 BOM（md_product_bom）：整机 8 行 + 电控预装件 5 行 + 手柄预装件 3 行
--    需求 §2.3.5：父子项不可为自身，用量必填；PCBA/传感器/显示按整体外购不展开
-- ----------------------------------------------------------------------------
INSERT INTO md_product_bom (bom_id, item_id, bom_item_id, bom_item_code, bom_item_name, bom_item_spec, unit_of_measure, item_or_product, quantity, enable_flag, unit_name, remark, create_by, create_time)
VALUES
 -- 整机 TG-FG-001 的直属组成
 (9301, 9201, 9202, 'TG-SF-001', '测温显示电控预装件', '电控预装件（PCBA+传感器+LCD+蜂鸣器+透镜）', 'SET', 'PRODUCT', 1.0000, 'Y', '套', '第一层：半成品', 'admin', NOW()),
 (9302, 9201, 9203, 'TG-SF-002', '扳机与侧键手柄预装件', '手柄预装件（三键按键板+电池+扳机组件）',   'SET', 'PRODUCT', 1.0000, 'Y', '套', '第一层：半成品', 'admin', NOW()),
 (9303, 9201, 9210, 'TG-ST-001', '上壳', 'ABS 白色 上壳', 'PCS', 'ITEM', 1.0000, 'Y', '个', '整机结构', 'admin', NOW()),
 (9304, 9201, 9211, 'TG-ST-002', '下壳', 'ABS 白色 下壳', 'PCS', 'ITEM', 1.0000, 'Y', '个', '整机结构', 'admin', NOW()),
 (9305, 9201, 9215, 'TG-ST-006', '防震泡棉', 'EVA 防震泡棉 30×20mm', 'PCS', 'ITEM', 1.0000, 'Y', '片', '电控件固定', 'admin', NOW()),
 (9306, 9201, 9214, 'TG-ST-005', '螺丝包', 'M2×6 自攻螺丝 6 颗/包', 'BAG', 'ITEM', 1.0000, 'Y', '包', '装配紧固', 'admin', NOW()),
 (9307, 9201, 9216, 'TG-PK-001', '说明书', '中文版使用说明书（8 页）', 'COPY', 'ITEM', 1.0000, 'Y', '份', '包装附件', 'admin', NOW()),
 (9308, 9201, 9217, 'TG-PK-002', '彩盒包装', '350×200×80mm 彩盒', 'PCS', 'ITEM', 1.0000, 'Y', '个', '包装附件', 'admin', NOW()),
 -- 电控预装件 TG-SF-001 的直属组成
 (9311, 9202, 9204, 'TG-EL-001', '主控PCBA', 'TG-MAIN-V1.2 主控板（已贴片）', 'PCS', 'ITEM', 1.0000, 'Y', '个', '第二层：电子件', 'admin', NOW()),
 (9312, 9202, 9205, 'TG-EL-002', '红外传感器模块', 'MLX90614 兼容 TO-39 红外测温传感器', 'PCS', 'ITEM', 1.0000, 'Y', '个', '第二层：电子件', 'admin', NOW()),
 (9313, 9202, 9206, 'TG-EL-003', '显示组件', '1.0 寸段码 LCD 显示模组', 'PCS', 'ITEM', 1.0000, 'Y', '个', '第二层：电子件', 'admin', NOW()),
 (9314, 9202, 9208, 'TG-EL-005', '蜂鸣器', '5V 有源蜂鸣器', 'PCS', 'ITEM', 1.0000, 'Y', '个', '第二层：电子件', 'admin', NOW()),
 (9315, 9202, 9213, 'TG-ST-004', '红外透镜', 'HDPE 红外聚焦透镜', 'PCS', 'ITEM', 1.0000, 'Y', '个', '第二层：光学件', 'admin', NOW()),
 -- 手柄预装件 TG-SF-002 的直属组成
 (9321, 9203, 9207, 'TG-EL-004', '三键按键板', '上/下/设置 三键薄膜按键板', 'PCS', 'ITEM', 1.0000, 'Y', '个', '第二层：电子件', 'admin', NOW()),
 (9322, 9203, 9209, 'TG-EL-006', '电池', 'AAA 1.5V 碱性电池', 'PCS', 'ITEM', 2.0000, 'Y', '节', '每套手柄配 2 节', 'admin', NOW()),
 (9323, 9203, 9212, 'TG-ST-003', '扳机组件', '扳机+复位弹簧组件', 'PCS', 'ITEM', 1.0000, 'Y', '个', '第二层：结构件', 'admin', NOW());

-- ----------------------------------------------------------------------------
-- 8. 工序（pro_process）
-- ----------------------------------------------------------------------------
INSERT INTO pro_process (process_id, process_code, process_name, attention, enable_flag, remark, create_by, create_time)
VALUES
 (9401, 'TG-OP10', '电控件预装',     '按防静电要求装配 PCBA、传感器、LCD、蜂鸣器与透镜', 'Y', '电控预装件工序', 'admin', NOW()),
 (9402, 'TG-OP20', '电控件通电测试', '通电检验显示、传感器响应与蜂鸣，通电不良不得流入总装', 'Y', '电控预装件过程检验工序', 'admin', NOW()),
 (9403, 'TG-OP30', '手柄件预装',     '装配三键按键板、电池与扳机组件，注意电池极性', 'Y', '手柄预装件工序', 'admin', NOW()),
 (9404, 'TG-OP40', '手柄件功能测试', '检验扳机回弹、按键触感与电池极性', 'Y', '手柄预装件过程检验工序', 'admin', NOW()),
 (9405, 'TG-OP50', '整机总装',       '电控件与手柄件合装，装配上下壳并锁付螺丝', 'Y', '整机装配工序', 'admin', NOW()),
 (9406, 'TG-OP60', '整机功能测试',   '检验三键响应、蜂鸣与自动关机功能', 'Y', '整机功能检验工序', 'admin', NOW()),
 (9407, 'TG-OP70', '温度校准',       '使用黑体炉校准测温显示，记录校准结果', 'Y', '关键工序，需过程检验', 'admin', NOW()),
 (9408, 'TG-OP80', '出货检验',       '按出货检验方案抽检，合格方可包装入库', 'Y', 'OQC 出货检验工序', 'admin', NOW()),
 (9409, 'TG-OP90', '包装入库',       '装入说明书与彩盒，扫码入库成品仓', 'Y', '包装工序', 'admin', NOW());

-- ----------------------------------------------------------------------------
-- 9. 工艺路线（pro_route / pro_route_process / pro_route_product）
-- ----------------------------------------------------------------------------
INSERT INTO pro_route (route_id, route_code, route_name, route_desc, enable_flag, remark, create_by, create_time)
VALUES
 (9451, 'TG-RT-EC', '电控预装工艺路线', '电控预装件：预装 → 通电测试', 'Y', '适用产品：测温显示电控预装件', 'admin', NOW()),
 (9452, 'TG-RT-HD', '手柄预装工艺路线', '手柄预装件：预装 → 功能测试', 'Y', '适用产品：扳机与侧键手柄预装件', 'admin', NOW()),
 (9453, 'TG-RT-FG', '整机装配工艺路线', '整机：总装 → 功能测试 → 校准 → 出货检验 → 包装', 'Y', '适用产品：红外测温枪整机', 'admin', NOW());

INSERT INTO pro_route_process (record_id, route_id, process_id, process_code, process_name, order_num, next_process_id, next_process_code, next_process_name, link_type, default_pre_time, default_suf_time, color_code, key_flag, is_check, create_by, create_time)
VALUES
 (9461, 9451, 9401, 'TG-OP10', '电控件预装',     1, 9402, 'TG-OP20', '电控件通电测试', 'SS', 0, 0, '#00AEF3', 'N', 'N', 'admin', NOW()),
 (9462, 9451, 9402, 'TG-OP20', '电控件通电测试', 2, 0,    NULL,      NULL,             'SS', 0, 0, '#00AEF3', 'Y', 'Y', 'admin', NOW()),
 (9463, 9452, 9403, 'TG-OP30', '手柄件预装',     1, 9404, 'TG-OP40', '手柄件功能测试', 'SS', 0, 0, '#13CE66', 'N', 'N', 'admin', NOW()),
 (9464, 9452, 9404, 'TG-OP40', '手柄件功能测试', 2, 0,    NULL,      NULL,             'SS', 0, 0, '#13CE66', 'Y', 'Y', 'admin', NOW()),
 (9465, 9453, 9405, 'TG-OP50', '整机总装',       1, 9406, 'TG-OP60', '整机功能测试',   'SS', 0, 0, '#FF9900', 'N', 'N', 'admin', NOW()),
 (9466, 9453, 9406, 'TG-OP60', '整机功能测试',   2, 9407, 'TG-OP70', '温度校准',       'SS', 0, 0, '#FF9900', 'N', 'N', 'admin', NOW()),
 (9467, 9453, 9407, 'TG-OP70', '温度校准',       3, 9408, 'TG-OP80', '出货检验',       'SS', 0, 0, '#FF4949', 'Y', 'Y', 'admin', NOW()),
 (9468, 9453, 9408, 'TG-OP80', '出货检验',       4, 9409, 'TG-OP90', '包装入库',       'SS', 0, 0, '#FF9900', 'N', 'N', 'admin', NOW()),
 (9469, 9453, 9409, 'TG-OP90', '包装入库',       5, 0,    NULL,      NULL,             'SS', 0, 0, '#FF9900', 'N', 'N', 'admin', NOW());

INSERT INTO pro_route_product (record_id, route_id, item_id, item_code, item_name, specification, unit_of_measure, unit_name, quantity, production_time, time_unit_type, remark, create_by, create_time)
VALUES
 (9481, 9451, 9202, 'TG-SF-001', '测温显示电控预装件', '电控预装件（PCBA+传感器+LCD+蜂鸣器+透镜）', 'SET', '套', 1.00, 30.00, 'MINUTE', '单套电控预装标准工时 30 分钟', 'admin', NOW()),
 (9482, 9452, 9203, 'TG-SF-002', '扳机与侧键手柄预装件', '手柄预装件（三键按键板+电池+扳机组件）',   'SET', '套', 1.00, 20.00, 'MINUTE', '单套手柄预装标准工时 20 分钟', 'admin', NOW()),
 (9483, 9453, 9201, 'TG-FG-001', '红外测温枪整机', 'TG-300 便携式红外测温枪（32~42.9℃）', 'TAI', '台', 1.00, 60.00, 'MINUTE', '单台整机装配标准工时 60 分钟', 'admin', NOW());

-- 工艺路线-工序-物料：报工时按工序带出应消耗的物料
INSERT INTO pro_route_product_bom (record_id, route_id, process_id, product_id, item_id, item_code, item_name, specification, unit_of_measure, unit_name, quantity, remark, create_by, create_time)
VALUES
 (9491, 9451, 9401, 9202, 9204, 'TG-EL-001', '主控PCBA', 'TG-MAIN-V1.2 主控板（已贴片）', 'PCS', '个', 1.0000, '电控预装投料', 'admin', NOW()),
 (9492, 9451, 9401, 9202, 9205, 'TG-EL-002', '红外传感器模块', 'MLX90614 兼容 TO-39 红外测温传感器', 'PCS', '个', 1.0000, '电控预装投料', 'admin', NOW()),
 (9493, 9451, 9401, 9202, 9206, 'TG-EL-003', '显示组件', '1.0 寸段码 LCD 显示模组', 'PCS', '个', 1.0000, '电控预装投料', 'admin', NOW()),
 (9494, 9451, 9401, 9202, 9208, 'TG-EL-005', '蜂鸣器', '5V 有源蜂鸣器', 'PCS', '个', 1.0000, '电控预装投料', 'admin', NOW()),
 (9495, 9451, 9401, 9202, 9213, 'TG-ST-004', '红外透镜', 'HDPE 红外聚焦透镜', 'PCS', '个', 1.0000, '电控预装投料', 'admin', NOW()),
 (9496, 9452, 9403, 9203, 9207, 'TG-EL-004', '三键按键板', '上/下/设置 三键薄膜按键板', 'PCS', '个', 1.0000, '手柄预装投料', 'admin', NOW()),
 (9497, 9452, 9403, 9203, 9209, 'TG-EL-006', '电池', 'AAA 1.5V 碱性电池', 'PCS', '节', 2.0000, '手柄预装投料', 'admin', NOW()),
 (9498, 9452, 9403, 9203, 9212, 'TG-ST-003', '扳机组件', '扳机+复位弹簧组件', 'PCS', '个', 1.0000, '手柄预装投料', 'admin', NOW()),
 (9499, 9453, 9405, 9201, 9202, 'TG-SF-001', '测温显示电控预装件', '电控预装件（PCBA+传感器+LCD+蜂鸣器+透镜）', 'SET', '套', 1.0000, '总装投入半成品', 'admin', NOW()),
 (9500, 9453, 9405, 9201, 9203, 'TG-SF-002', '扳机与侧键手柄预装件', '手柄预装件（三键按键板+电池+扳机组件）', 'SET', '套', 1.0000, '总装投入半成品', 'admin', NOW()),
 (9501, 9453, 9405, 9201, 9210, 'TG-ST-001', '上壳', 'ABS 白色 上壳', 'PCS', '个', 1.0000, '总装投料', 'admin', NOW()),
 (9502, 9453, 9405, 9201, 9211, 'TG-ST-002', '下壳', 'ABS 白色 下壳', 'PCS', '个', 1.0000, '总装投料', 'admin', NOW()),
 (9503, 9453, 9405, 9201, 9215, 'TG-ST-006', '防震泡棉', 'EVA 防震泡棉 30×20mm', 'PCS', '片', 1.0000, '总装投料', 'admin', NOW()),
 (9504, 9453, 9405, 9201, 9214, 'TG-ST-005', '螺丝包', 'M2×6 自攻螺丝 6 颗/包', 'BAG', '包', 1.0000, '总装投料', 'admin', NOW()),
 (9505, 9453, 9409, 9201, 9216, 'TG-PK-001', '说明书', '中文版使用说明书（8 页）', 'COPY', '份', 1.0000, '包装投料', 'admin', NOW()),
 (9506, 9453, 9409, 9201, 9217, 'TG-PK-002', '彩盒包装', '350×200×80mm 彩盒', 'PCS', '个', 1.0000, '包装投料', 'admin', NOW());

-- ----------------------------------------------------------------------------
-- 10. 车间与工作站（md_workshop / md_workstation）
-- ----------------------------------------------------------------------------
INSERT INTO md_workshop (workshop_id, workshop_code, workshop_name, area, charge, charge_id, enable_flag, remark, create_by, create_time)
VALUES
 (9501, 'TG-WS-01', '测温枪装配车间', 300.00, '王主管', 102, 'Y', '测温枪预装、总装与测试车间', 'admin', NOW());

INSERT INTO md_workstation (workstation_id, workstation_code, workstation_name, workstation_address, workshop_id, workshop_code, workshop_name, process_id, process_code, process_name, warehouse_id, warehouse_code, warehouse_name, location_id, location_code, location_name, area_id, area_code, area_name, enable_flag, remark, create_by, create_time)
VALUES
 (9601, 'TG-ST10', '电控预装站',   '车间 A 区', 9501, 'TG-WS-01', '测温枪装配车间', 9401, 'TG-OP10', '电控件预装',     9701, 'WH-LINE', '线边库', 9711, 'L-EC',  '电控件库区', 9721, 'EC-01', '电控原料库位', 'Y', '电控预装件装配工位', 'admin', NOW()),
 (9602, 'TG-ST20', '电控测试站',   '车间 A 区', 9501, 'TG-WS-01', '测温枪装配车间', 9402, 'TG-OP20', '电控件通电测试', 9701, 'WH-LINE', '线边库', 9711, 'L-EC',  '电控件库区', 9721, 'EC-01', '电控原料库位', 'Y', '通电与显示测试工位', 'admin', NOW()),
 (9603, 'TG-ST30', '手柄预装站',   '车间 B 区', 9501, 'TG-WS-01', '测温枪装配车间', 9403, 'TG-OP30', '手柄件预装',     9701, 'WH-LINE', '线边库', 9712, 'L-HD',  '手柄件库区', 9722, 'HD-01', '手柄原料库位', 'Y', '手柄预装件装配工位', 'admin', NOW()),
 (9604, 'TG-ST40', '手柄测试站',   '车间 B 区', 9501, 'TG-WS-01', '测温枪装配车间', 9404, 'TG-OP40', '手柄件功能测试', 9701, 'WH-LINE', '线边库', 9712, 'L-HD',  '手柄件库区', 9722, 'HD-01', '手柄原料库位', 'Y', '扳机与按键测试工位', 'admin', NOW()),
 (9605, 'TG-ST50', '整机总装站',   '车间 C 区', 9501, 'TG-WS-01', '测温枪装配车间', 9405, 'TG-OP50', '整机总装',       9701, 'WH-LINE', '线边库', 9713, 'L-ST',  '结构件库区', 9723, 'ST-01', '结构件库位', 'Y', '整机总装工位', 'admin', NOW()),
 (9606, 'TG-ST60', '整机测试站',   '车间 C 区', 9501, 'TG-WS-01', '测温枪装配车间', 9406, 'TG-OP60', '整机功能测试',   9701, 'WH-LINE', '线边库', 9713, 'L-ST',  '结构件库区', 9723, 'ST-01', '结构件库位', 'Y', '功能与自动关机测试工位', 'admin', NOW()),
 (9607, 'TG-ST70', '温度校准站',   '车间 C 区', 9501, 'TG-WS-01', '测温枪装配车间', 9407, 'TG-OP70', '温度校准',       9701, 'WH-LINE', '线边库', 9713, 'L-ST',  '结构件库区', 9723, 'ST-01', '结构件库位', 'Y', '黑体炉温度校准工位', 'admin', NOW()),
 (9608, 'TG-ST80', '出货检验站',   '车间 C 区', 9501, 'TG-WS-01', '测温枪装配车间', 9408, 'TG-OP80', '出货检验',       9701, 'WH-LINE', '线边库', 9713, 'L-ST',  '结构件库区', 9723, 'ST-01', '结构件库位', 'Y', 'OQC 抽检工位', 'admin', NOW()),
 (9609, 'TG-ST90', '包装入库站',   '车间 D 区', 9501, 'TG-WS-01', '测温枪装配车间', 9409, 'TG-OP90', '包装入库',       9701, 'WH-LINE', '线边库', 9713, 'L-ST',  '结构件库区', 9723, 'ST-01', '结构件库位', 'Y', '包装与成品入库工位', 'admin', NOW());

-- ----------------------------------------------------------------------------
-- 11. 仓储三级档案（仓库 / 库区 / 库位）
-- ----------------------------------------------------------------------------
INSERT INTO wm_warehouse (warehouse_id, warehouse_code, warehouse_name, warehouse_type, location, area, charge, frozen_flag, user_id, user_name, remark, create_by, create_time)
VALUES
 (9701, 'WH-LINE', '线边库', 'LINE', '车间现场',   120.00, '张仓管', 'N', 202, 'warehouse', '生产现场线边存放区', 'admin', NOW()),
 (9702, 'WH-RAW',  '原料仓', 'RAW',  '厂区 1 号库', 300.00, '张仓管', 'N', 202, 'warehouse', '外购物料与半成品原料存放', 'admin', NOW()),
 (9703, 'WH-FG',   '成品仓', 'FG',   '厂区 2 号库', 200.00, '张仓管', 'N', 202, 'warehouse', '合格成品待发货区', 'admin', NOW()),
 (9704, 'XBK_VIRTUAL', '线边库-虚拟', 'LINE', '系统虚拟', 0.00, '张仓管', 'N', 202, 'warehouse', '系统内部过账使用的虚拟线边库，勿删除', 'admin', NOW());

INSERT INTO wm_storage_location (location_id, location_code, location_name, warehouse_id, area, area_flag, frozen_flag, remark, create_by, create_time)
VALUES
 (9711, 'L-EC',  '电控件库区',  9702, 60.00, 'Y', 'N', 'PCBA、传感器、显示与蜂鸣器等电子件', 'admin', NOW()),
 (9712, 'L-HD',  '手柄件库区',  9702, 40.00, 'Y', 'N', '按键板、电池与扳机组件', 'admin', NOW()),
 (9713, 'L-ST',  '结构件库区',  9702, 80.00, 'Y', 'N', '上下壳、透镜、螺丝与泡棉', 'admin', NOW()),
 (9714, 'L-PK',  '包装材料库区', 9702, 40.00, 'Y', 'N', '说明书与彩盒', 'admin', NOW()),
 (9715, 'L-EC-LINE', '电控线边区', 9701, 30.00, 'Y', 'N', '电控预装件线边存放', 'admin', NOW()),
 (9716, 'L-HD-LINE', '手柄线边区', 9701, 30.00, 'Y', 'N', '手柄预装件线边存放', 'admin', NOW()),
 (9717, 'L-ASM-LINE', '总装线边区', 9701, 60.00, 'Y', 'N', '总装物料与整机线边存放', 'admin', NOW()),
 (9718, 'L-FG',  '成品区',      9703, 200.00, 'Y', 'N', '合格测温枪整机存放', 'admin', NOW()),
 (9719, 'XBKKQ_VIRTUAL', '线边库库区-虚拟', 9704, 0.00, 'Y', 'N', '系统虚拟库区', 'admin', NOW());

-- 原料仓库位允许物资/批次混放，便于演示时多物料共用库位
INSERT INTO wm_storage_area (area_id, area_code, area_name, location_id, area, max_loa, position_x, position_y, position_z, enable_flag, frozen_flag, product_mixing, batch_mixing, remark, create_by, create_time)
VALUES
 (9721, 'EC-01',  '电控原料库位', 9711, 10.00, 200.00, 1, 1, 1, 'Y', 'N', 'Y', 'Y', '电子件原料库位（允许多物料混放）', 'admin', NOW()),
 (9722, 'HD-01',  '手柄原料库位', 9712, 10.00, 200.00, 2, 1, 1, 'Y', 'N', 'Y', 'Y', '手柄件原料库位（允许多物料混放）', 'admin', NOW()),
 (9723, 'ST-01',  '结构件库位',   9713, 10.00, 200.00, 3, 1, 1, 'Y', 'N', 'Y', 'Y', '结构件原料库位（允许多物料混放）', 'admin', NOW()),
 (9724, 'PK-01',  '包装材料库位', 9714, 10.00, 200.00, 4, 1, 1, 'Y', 'N', 'Y', 'Y', '包装辅料库位（允许多物料混放）', 'admin', NOW()),
 (9725, 'EC-L1',  '电控线边库位', 9715, 10.00, 100.00, 1, 1, 1, 'Y', 'N', 'N', 'N', '电控预装件线边库位', 'admin', NOW()),
 (9726, 'HD-L1',  '手柄线边库位', 9716, 10.00, 100.00, 2, 1, 1, 'Y', 'N', 'N', 'N', '手柄预装件线边库位', 'admin', NOW()),
 (9727, 'AS-L1',  '总装线边库位', 9717, 20.00, 100.00, 3, 1, 1, 'Y', 'N', 'N', 'N', '总装线边库位', 'admin', NOW()),
 (9728, 'FG-01',  '成品库位',     9718, 50.00, 500.00, 1, 1, 1, 'Y', 'N', 'N', 'N', '成品存放库位', 'admin', NOW()),
 (9729, 'XBKKW_VIRTUAL', '线边库库位-虚拟', 9719, 0.00, 0.00, 1, 1, 1, 'Y', 'N', 'Y', 'Y', '系统虚拟库位', 'admin', NOW());

-- ----------------------------------------------------------------------------
-- 12. 客户与供应商（md_client / md_vendor）
-- ----------------------------------------------------------------------------
INSERT INTO md_client (client_id, client_code, client_name, client_nick, client_type, tel, contact1, contact1_tel, email, address, enable_flag, remark, create_by, create_time)
VALUES
 (9751, 'C-TG-001', '华东医疗器械经销有限公司', '华东医疗', 'DEALER',     '021-58880001', '陈经理', '13900001001', 'sales@hdmed.example', '上海市浦东新区张江路 88 号', 'Y', '测温枪整机经销商', 'admin', NOW()),
 (9752, 'C-TG-002', '北京康泰健康科技有限公司', '北京康泰', 'ENTERPRISE', '010-68880002', '刘经理', '13900001002', 'buy@ktjk.example',   '北京市海淀区中关村大街 5 号', 'Y', '企业客户', 'admin', NOW()),
 (9753, 'C-TG-003', '广州社区健康服务中心',     '广州社区', 'ENTERPRISE', '020-38880003', '黄主任', '13900001003', 'gzsq@health.example', '广州市天河区体育西路 12 号', 'Y', '终端客户', 'admin', NOW());

INSERT INTO md_vendor (vendor_id, vendor_code, vendor_name, vendor_nick, vendor_level, tel, contact1, contact1_tel, email, address, enable_flag, remark, create_by, create_time)
VALUES
 (9761, 'V-TG-001', '深圳市芯控电子有限公司',   '芯控电子', 'A', '0755-26660001', '林经理', '13700002001', 'sales@xkpcb.example', '深圳市宝安区福永街道 3 号', 'Y', '主控 PCBA 与电子件供应商', 'admin', NOW()),
 (9762, 'V-TG-002', '东莞市精密结构件有限公司', '精密结构', 'B', '0769-82220002', '何经理', '13700002002', 'sales@jmjg.example',  '东莞市长安镇厦岗路 9 号', 'Y', '外壳、扳机与透镜供应商', 'admin', NOW()),
 (9763, 'V-TG-003', '广州市彩印包装有限公司',   '彩印包装', 'B', '020-36660003', '梁经理', '13700002003', 'sales@cybz.example',  '广州市白云区太和镇 7 号', 'Y', '说明书与彩盒供应商', 'admin', NOW());

-- ----------------------------------------------------------------------------
-- 13. 质量基础数据：检测项（qc_index）
-- ----------------------------------------------------------------------------
INSERT INTO qc_index (index_id, index_code, index_name, index_type, qc_tool, qc_result_type, remark, create_by, create_time)
VALUES
 (9801, 'TG-QI-01', '通电测试',   'PERFORMANCE', '测试治具', 'TEXT',  '通电后指示灯与整机工作状态正常', 'admin', NOW()),
 (9802, 'TG-QI-02', '传感器响应', 'PERFORMANCE', '测试治具', 'FLOAT', '传感器对目标温度有稳定响应', 'admin', NOW()),
 (9803, 'TG-QI-03', 'LCD 显示',   'APPEARANCE',  '目视',     'TEXT',  'LCD 字符完整、无缺笔画', 'admin', NOW()),
 (9804, 'TG-QI-04', '三键触点',   'PERFORMANCE', '测试治具', 'TEXT',  '上/下/设置三键触点导通正常', 'admin', NOW()),
 (9805, 'TG-QI-05', '蜂鸣功能',   'PERFORMANCE', '听感',     'TEXT',  '按键与超温提示音正常', 'admin', NOW()),
 (9806, 'TG-QI-06', '扳机回弹',   'PERFORMANCE', '手感',     'TEXT',  '扳机按下与回弹顺畅无卡滞', 'admin', NOW()),
 (9807, 'TG-QI-07', '按键触感',   'APPEARANCE',  '手感',     'TEXT',  '三键触感一致、无虚按', 'admin', NOW()),
 (9808, 'TG-QI-08', '电池极性',   'PERFORMANCE', '万用表',   'TEXT',  '电池仓极性标识与装配方向正确', 'admin', NOW()),
 (9809, 'TG-QI-09', '测温显示',   'PERFORMANCE', '黑体炉',   'FLOAT', '32.0~42.9℃ 区间显示值与基准偏差', 'admin', NOW()),
 (9810, 'TG-QI-10', '自动关机',   'PERFORMANCE', '秒表',     'FLOAT', '无操作 60 秒自动关机', 'admin', NOW());

INSERT INTO qc_template (template_id, template_code, template_name, qc_types, enable_flag, remark, create_by, create_time)
VALUES
 (9821, 'TG-TPL-IQC', '外购物料来料检验方案', 'IQC', 'Y', '适用于 14 种外购物料的来料检验', 'admin', NOW()),
 (9822, 'TG-TPL-EC',  '电控预装件检验方案',   'PQC,IPQC', 'Y', '电控预装件通电与显示检验', 'admin', NOW()),
 (9823, 'TG-TPL-HD',  '手柄预装件检验方案',   'PQC,IPQC', 'Y', '手柄预装件扳机、按键与电池极性检验', 'admin', NOW()),
 (9824, 'TG-TPL-FG',  '测温枪整机出货检验方案', 'OQC,FQC,IPQC', 'Y', '整机出货抽检与过程检验方案', 'admin', NOW());

-- stander_val 为数值型(double)，文字类判定项留空并把判定标准写进 remark
INSERT INTO qc_template_index (record_id, template_id, index_id, index_code, index_name, index_type, qc_tool, check_method, stander_val, unit_of_measure, threshold_max, threshold_min, remark, create_by, create_time)
VALUES
 (9841, 9821, 9803, 'TG-QI-03', 'LCD 显示',   'APPEARANCE',  '目视',     '外观检查', NULL,      NULL,   NULL,    NULL,    '判定标准：无破损、无缺件', 'admin', NOW()),
 (9842, 9821, 9802, 'TG-QI-02', '传感器响应', 'PERFORMANCE', '测试治具', '通电抽检', 50.0000,   'mV',   100.0000, 1.0000, '判定标准：通电后响应正常', 'admin', NOW()),
 (9843, 9821, 9809, 'TG-QI-09', '测温显示',   'PERFORMANCE', '黑体炉',   '抽检比对', 36.5000,   '℃',    37.0000, 36.0000, '来料性能抽检', 'admin', NOW()),
 (9844, 9822, 9801, 'TG-QI-01', '通电测试',   'PERFORMANCE', '测试治具', '全检',     NULL,      NULL,   NULL,    NULL,    '判定标准：通电后指示灯与整机工作状态正常', 'admin', NOW()),
 (9845, 9822, 9802, 'TG-QI-02', '传感器响应', 'PERFORMANCE', '测试治具', '全检',     50.0000,   'mV',   100.0000, 1.0000, '判定标准：传感器对目标温度有稳定响应', 'admin', NOW()),
 (9846, 9822, 9803, 'TG-QI-03', 'LCD 显示',   'APPEARANCE',  '目视',     '全检',     NULL,      NULL,   NULL,    NULL,    '判定标准：显示完整、无缺笔画', 'admin', NOW()),
 (9847, 9822, 9804, 'TG-QI-04', '三键触点',   'PERFORMANCE', '测试治具', '全检',     NULL,      NULL,   NULL,    NULL,    '判定标准：上/下/设置三键触点导通正常', 'admin', NOW()),
 (9848, 9822, 9805, 'TG-QI-05', '蜂鸣功能',   'PERFORMANCE', '听感',     '全检',     NULL,      NULL,   NULL,    NULL,    '判定标准：按键与超温提示音正常', 'admin', NOW()),
 (9849, 9823, 9806, 'TG-QI-06', '扳机回弹',   'PERFORMANCE', '手感',     '全检',     NULL,      NULL,   NULL,    NULL,    '判定标准：扳机按下与回弹顺畅无卡滞', 'admin', NOW()),
 (9850, 9823, 9807, 'TG-QI-07', '按键触感',   'APPEARANCE',  '手感',     '全检',     NULL,      NULL,   NULL,    NULL,    '判定标准：三键触感一致、无虚按', 'admin', NOW()),
 (9851, 9823, 9808, 'TG-QI-08', '电池极性',   'PERFORMANCE', '万用表',   '全检',     NULL,      NULL,   NULL,    NULL,    '判定标准：电池仓极性标识与装配方向正确', 'admin', NOW()),
 (9852, 9824, 9809, 'TG-QI-09', '测温显示',   'PERFORMANCE', '黑体炉',   '抽检 3 台', 36.5000,  '℃',    37.0000, 36.0000, '出货测温精度', 'admin', NOW()),
 (9853, 9824, 9804, 'TG-QI-04', '三键触点',   'PERFORMANCE', '测试治具', '抽检',     NULL,      NULL,   NULL,    NULL,    '判定标准：三键响应正常', 'admin', NOW()),
 (9854, 9824, 9805, 'TG-QI-05', '蜂鸣功能',   'PERFORMANCE', '听感',     '抽检',     NULL,      NULL,   NULL,    NULL,    '判定标准：提示音正常', 'admin', NOW()),
 (9855, 9824, 9810, 'TG-QI-10', '自动关机',   'PERFORMANCE', '秒表',     '抽检',     60.0000,   '秒',   65.0000, 55.0000, '出货自动关机时间', 'admin', NOW());

INSERT INTO qc_template_product (record_id, template_id, item_id, item_code, item_name, specification, unit_of_measure, unit_name, quantity_check, quantity_unqualified, cr_rate, maj_rate, min_rate, remark, create_by, create_time)
VALUES
 (9861, 9821, 9204, 'TG-EL-001', '主控PCBA', 'TG-MAIN-V1.2 主控板（已贴片）', 'PCS', '个', 1, 0, 0.00, 0.00, 100.00, '来料抽检', 'admin', NOW()),
 (9862, 9821, 9205, 'TG-EL-002', '红外传感器模块', 'MLX90614 兼容 TO-39 红外测温传感器', 'PCS', '个', 1, 0, 0.00, 0.00, 100.00, '来料抽检', 'admin', NOW()),
 (9863, 9821, 9206, 'TG-EL-003', '显示组件', '1.0 寸段码 LCD 显示模组', 'PCS', '个', 1, 0, 0.00, 0.00, 100.00, '来料抽检', 'admin', NOW()),
 (9864, 9821, 9207, 'TG-EL-004', '三键按键板', '上/下/设置 三键薄膜按键板', 'PCS', '个', 1, 0, 0.00, 0.00, 100.00, '来料抽检', 'admin', NOW()),
 (9865, 9821, 9208, 'TG-EL-005', '蜂鸣器', '5V 有源蜂鸣器', 'PCS', '个', 1, 0, 0.00, 0.00, 100.00, '来料抽检', 'admin', NOW()),
 (9866, 9821, 9209, 'TG-EL-006', '电池', 'AAA 1.5V 碱性电池', 'PCS', '节', 1, 0, 0.00, 0.00, 100.00, '来料抽检', 'admin', NOW()),
 (9867, 9821, 9210, 'TG-ST-001', '上壳', 'ABS 白色 上壳', 'PCS', '个', 1, 0, 0.00, 0.00, 100.00, '来料抽检', 'admin', NOW()),
 (9868, 9821, 9211, 'TG-ST-002', '下壳', 'ABS 白色 下壳', 'PCS', '个', 1, 0, 0.00, 0.00, 100.00, '来料抽检', 'admin', NOW()),
 (9869, 9821, 9212, 'TG-ST-003', '扳机组件', '扳机+复位弹簧组件', 'PCS', '个', 1, 0, 0.00, 0.00, 100.00, '来料抽检', 'admin', NOW()),
 (9870, 9821, 9213, 'TG-ST-004', '红外透镜', 'HDPE 红外聚焦透镜', 'PCS', '个', 1, 0, 0.00, 0.00, 100.00, '来料抽检', 'admin', NOW()),
 (9871, 9821, 9214, 'TG-ST-005', '螺丝包', 'M2×6 自攻螺丝 6 颗/包', 'BAG', '包', 1, 0, 0.00, 0.00, 100.00, '来料抽检', 'admin', NOW()),
 (9872, 9821, 9215, 'TG-ST-006', '防震泡棉', 'EVA 防震泡棉 30×20mm', 'PCS', '片', 1, 0, 0.00, 0.00, 100.00, '来料抽检', 'admin', NOW()),
 (9873, 9821, 9216, 'TG-PK-001', '说明书', '中文版使用说明书（8 页）', 'COPY', '份', 1, 0, 0.00, 0.00, 100.00, '来料抽检', 'admin', NOW()),
 (9874, 9821, 9217, 'TG-PK-002', '彩盒包装', '350×200×80mm 彩盒', 'PCS', '个', 1, 0, 0.00, 0.00, 100.00, '来料抽检', 'admin', NOW()),
 (9875, 9822, 9202, 'TG-SF-001', '测温显示电控预装件', '电控预装件（PCBA+传感器+LCD+蜂鸣器+透镜）', 'SET', '套', 1, 0, 0.00, 0.00, 100.00, '过程检验', 'admin', NOW()),
 (9876, 9823, 9203, 'TG-SF-002', '扳机与侧键手柄预装件', '手柄预装件（三键按键板+电池+扳机组件）', 'SET', '套', 1, 0, 0.00, 0.00, 100.00, '过程检验', 'admin', NOW()),
 (9877, 9824, 9201, 'TG-FG-001', '红外测温枪整机', 'TG-300 便携式红外测温枪（32~42.9℃）', 'TAI', '台', 3, 0, 0.00, 0.00, 100.00, '出货抽检 3 台', 'admin', NOW());

-- ----------------------------------------------------------------------------
-- 14. 设备管理：类型 / 台账 / 点检保养项目 / 计划
-- ----------------------------------------------------------------------------
INSERT INTO dv_machinery_type (machinery_type_id, machinery_type_code, machinery_type_name, parent_type_id, ancestors, enable_flag, remark, create_by, create_time)
VALUES
 (9901, 'TG-MT-01', '装配设备', 0, '0', 'Y', '电批、压合等装配类设备', 'admin', NOW()),
 (9902, 'TG-MT-02', '测试设备', 0, '0', 'Y', '功能测试与气密性测试设备', 'admin', NOW()),
 (9903, 'TG-MT-03', '校准设备', 0, '0', 'Y', '黑体炉与恒温槽等计量校准设备', 'admin', NOW());

INSERT INTO dv_machinery (machinery_id, machinery_code, machinery_name, machinery_brand, machinery_spec, machinery_type_id, machinery_type_code, machinery_type_name, workshop_id, workshop_code, workshop_name, status, remark, create_by, create_time)
VALUES
 (9911, 'TG-MC-001', '电动螺丝刀',   '博世',   'GSR120-LI',  9901, 'TG-MT-01', '装配设备', 9501, 'TG-WS-01', '测温枪装配车间', 'WORKING', '总装工位锁付设备', 'admin', NOW()),
 (9912, 'TG-MC-002', '定扭矩电批',   '奇力速', 'SK-2000',    9901, 'TG-MT-01', '装配设备', 9501, 'TG-WS-01', '测温枪装配车间', 'WORKING', '扭矩一致性要求 M2 螺丝', 'admin', NOW()),
 (9913, 'TG-MC-003', '功能测试治具', '自制',   'TG-TEST-01', 9902, 'TG-MT-02', '测试设备', 9501, 'TG-WS-01', '测温枪装配车间', 'WORKING', '三键与蜂鸣功能测试', 'admin', NOW()),
 (9914, 'TG-MC-004', '红外黑体炉',   '高精',   'BB-50',      9903, 'TG-MT-03', '校准设备', 9501, 'TG-WS-01', '测温枪装配车间', 'WORKING', '36.5℃ 校准基准源', 'admin', NOW()),
 (9915, 'TG-MC-005', '恒温槽',       '优莱博', 'TC-200',     9903, 'TG-MT-03', '校准设备', 9501, 'TG-WS-01', '测温枪装配车间', 'MAINTEN', '校准设备年度保养中', 'admin', NOW());

INSERT INTO dv_subject (subject_id, subject_code, subject_name, subject_type, subject_content, subject_standard, enable_flag, remark, create_by, create_time)
VALUES
 (9931, 'TG-DS-01', '电批日常点检',   'CHECK',   '检查电源线、批头磨损与启停正常；空载试转 3 次', '无异常噪音、批头无滑牙', 'Y', '每班一次', 'admin', NOW()),
 (9932, 'TG-DS-02', '测试治具点检',   'CHECK',   '检查治具探针接触、指示灯与线缆完好', '探针接触良好，测试通过', 'Y', '每日一次', 'admin', NOW()),
 (9933, 'TG-DS-03', '黑体炉周期保养', 'MAINTEN', '清洁辐射腔、检查控温稳定性并记录温度偏差', '控温偏差 ≤ ±0.2℃', 'Y', '每月一次', 'admin', NOW()),
 (9934, 'TG-DS-04', '恒温槽保养',     'MAINTEN', '更换循环液、清理水垢、校准温度探头', '槽内温度均匀性 ≤ ±0.3℃', 'Y', '每季度一次', 'admin', NOW());

INSERT INTO dv_check_plan (plan_id, plan_code, plan_name, plan_type, start_date, end_date, cycle_type, cycle_count, status, remark, create_by, create_time)
VALUES
 (9941, 'TG-CP-01', '装配设备日常点检计划', 'CHECK',   '2026-10-01 00:00:00', '2026-12-31 00:00:00', 'DAY',   1, 'CONFIRMED', '电批与测试治具日常点检', 'admin', NOW()),
 (9942, 'TG-CP-02', '校准设备月度保养计划', 'MAINTEN', '2026-10-01 00:00:00', '2026-12-31 00:00:00', 'MONTH', 1, 'CONFIRMED', '黑体炉月度保养', 'admin', NOW()),
 (9943, 'TG-CP-03', '恒温槽季度保养计划',   'MAINTEN', '2026-10-01 00:00:00', '2026-12-31 00:00:00', 'MONTH', 3, 'PREPARE',   '恒温槽季度保养', 'admin', NOW());

INSERT INTO dv_check_machinery (record_id, plan_id, machinery_id, machinery_code, machinery_name, machinery_brand, machinery_spec, remark, create_by, create_time)
VALUES
 (9951, 9941, 9911, 'TG-MC-001', '电动螺丝刀',   '博世',   'GSR120-LI',  '日常点检', 'admin', NOW()),
 (9952, 9941, 9912, 'TG-MC-002', '定扭矩电批',   '奇力速', 'SK-2000',    '日常点检', 'admin', NOW()),
 (9953, 9941, 9913, 'TG-MC-003', '功能测试治具', '自制',   'TG-TEST-01', '日常点检', 'admin', NOW()),
 (9954, 9942, 9914, 'TG-MC-004', '红外黑体炉',   '高精',   'BB-50',      '月度保养', 'admin', NOW()),
 (9955, 9943, 9915, 'TG-MC-005', '恒温槽',       '优莱博', 'TC-200',     '季度保养', 'admin', NOW());

INSERT INTO dv_check_subject (record_id, plan_id, subject_id, subject_code, subject_name, subject_type, subject_content, subject_standard, remark, create_by, create_time)
VALUES
 (9961, 9941, 9931, 'TG-DS-01', '电批日常点检',   'CHECK',   '检查电源线、批头磨损与启停正常；空载试转 3 次', '无异常噪音、批头无滑牙', '点检项', 'admin', NOW()),
 (9962, 9941, 9932, 'TG-DS-02', '测试治具点检',   'CHECK',   '检查治具探针接触、指示灯与线缆完好', '探针接触良好，测试通过', '点检项', 'admin', NOW()),
 (9963, 9942, 9933, 'TG-DS-03', '黑体炉周期保养', 'MAINTEN', '清洁辐射腔、检查控温稳定性并记录温度偏差', '控温偏差 ≤ ±0.2℃', '保养项', 'admin', NOW()),
 (9964, 9943, 9934, 'TG-DS-04', '恒温槽保养',     'MAINTEN', '更换循环液、清理水垢、校准温度探头', '槽内温度均匀性 ≤ ±0.3℃', '保养项', 'admin', NOW());

-- ----------------------------------------------------------------------------
-- 15. 班组与排班
-- ----------------------------------------------------------------------------
INSERT INTO cal_team (team_id, team_code, team_name, calendar_type, remark, create_by, create_time)
VALUES
 (9971, 'TG-T01', '测温枪装配一组', 'ZZ', '负责电控与手柄预装', 'admin', NOW()),
 (9972, 'TG-T02', '测温枪总装二组', 'ZZ', '负责整机总装、测试与包装', 'admin', NOW());

INSERT INTO cal_team_member (member_id, team_id, user_id, user_name, nick_name, tel, remark, create_by, create_time)
VALUES
 (9981, 9971, 203, 'operator',   '赵操作',   '13800000203', '生产操作员', 'admin', NOW()),
 (9982, 9971, 204, 'inspector',  '孙质检',   '13800000204', '质检员', 'admin', NOW()),
 (9983, 9972, 203, 'operator',   '赵操作',   '13800000203', '生产操作员', 'admin', NOW()),
 (9984, 9972, 206, 'teamleader', '吴班组长', '13800000206', '班组长', 'admin', NOW()),
 (9985, 9972, 205, 'equipment',  '周设备',   '13800000205', '设备员', 'admin', NOW());

INSERT INTO cal_plan (plan_id, plan_code, plan_name, calendar_type, start_date, end_date, shift_type, shift_method, shift_count, status, remark, create_by, create_time)
VALUES
 (9991, 'TG-CAL-2026', '2026 年 11 月测温枪产线排班计划', 'ZZ', '2026-11-01 00:00:00', '2026-11-30 23:59:59', 'SINGLE', 'DAY', 1, 'CONFIRMED', '单班生产，满足 10 台演示产能', 'admin', NOW());

INSERT INTO cal_shift (shift_id, plan_id, order_num, shift_name, start_time, end_time, remark, create_by, create_time)
VALUES
 (9992, 9991, 1, '白班', '08:00', '17:00', '标准生产班次', 'admin', NOW()),
 (9993, 9991, 2, '中班', '17:00', '01:00', '加班或赶货班次', 'admin', NOW());

INSERT INTO cal_plan_team (record_id, plan_id, team_id, team_code, team_name, remark, create_by, create_time)
VALUES
 (9994, 9991, 9971, 'TG-T01', '测温枪装配一组', '参与排班', 'admin', NOW()),
 (9995, 9991, 9972, 'TG-T02', '测温枪总装二组', '参与排班', 'admin', NOW());

-- ----------------------------------------------------------------------------
-- 16. 生产操作员与工作站绑定（报工时按人带出可用工作站）
-- ----------------------------------------------------------------------------
INSERT INTO pro_user_workstation (record_id, user_id, workstation_id, remark, create_by, create_time)
VALUES
 (9996, 203, 9605, '生产操作员默认总装站', 'admin', NOW()),
 (9997, 203, 9609, '生产操作员默认包装站', 'admin', NOW());

-- ----------------------------------------------------------------------------
-- 16.1 新增「库存流水」菜单（需求 §2.4.9：按单据类型/批次/仓库/起止日期查询库存流水）
--      原系统只有库存现有量页面，缺少库存流水查询入口，这里补齐
-- ----------------------------------------------------------------------------
DELETE FROM sys_role_menu WHERE menu_id BETWEEN 2390 AND 2394;
DELETE FROM sys_menu WHERE menu_id BETWEEN 2390 AND 2394;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES
 (2390, '库存流水', 2042, 20, 'transaction', 'mes/wm/transaction/index', NULL, 1, 0, 'C', '0', '0', 'mes:wm:transaction', 'log', 'admin', NOW(), '库存流水分页查询与导出'),
 (2391, '库存流水查询', 2390, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:transaction:list', '#', 'admin', NOW(), ''),
 (2392, '库存流水详情', 2390, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:transaction:query', '#', 'admin', NOW(), ''),
 (2393, '库存流水导出', 2390, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'mes:wm:transaction:export', '#', 'admin', NOW(), '');

-- 计划员/仓库员/质检员默认可见库存流水
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 102, menu_id FROM sys_menu WHERE menu_id = 2390 OR perms LIKE 'mes:wm:transaction:%';
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 103, menu_id FROM sys_menu WHERE menu_id = 2390 OR perms LIKE 'mes:wm:transaction:%';
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 105, menu_id FROM sys_menu WHERE menu_id = 2390 OR perms LIKE 'mes:wm:transaction:%';

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------------------------------------------------------
-- 17. 执行结果自检
-- ----------------------------------------------------------------------------
SELECT '物料产品' AS 对象, COUNT(*) AS 数量 FROM md_item WHERE item_code LIKE 'TG-%'
UNION ALL SELECT '批次配置', COUNT(*) FROM md_item_batch_config WHERE item_id BETWEEN 9201 AND 9217
UNION ALL SELECT '多级BOM行', COUNT(*) FROM md_product_bom WHERE bom_id BETWEEN 9301 AND 9323
UNION ALL SELECT '工序', COUNT(*) FROM pro_process WHERE process_code LIKE 'TG-OP%'
UNION ALL SELECT '工艺路线', COUNT(*) FROM pro_route WHERE route_code LIKE 'TG-RT-%'
UNION ALL SELECT '路线工序', COUNT(*) FROM pro_route_process WHERE route_id BETWEEN 9451 AND 9453
UNION ALL SELECT '工作站', COUNT(*) FROM md_workstation WHERE workstation_code LIKE 'TG-ST%'
UNION ALL SELECT '仓库', COUNT(*) FROM wm_warehouse WHERE warehouse_id BETWEEN 9701 AND 9704
UNION ALL SELECT '库区', COUNT(*) FROM wm_storage_location WHERE location_id BETWEEN 9711 AND 9719
UNION ALL SELECT '库位', COUNT(*) FROM wm_storage_area WHERE area_id BETWEEN 9721 AND 9729
UNION ALL SELECT '检测项', COUNT(*) FROM qc_index WHERE index_code LIKE 'TG-QI-%'
UNION ALL SELECT '检验方案', COUNT(*) FROM qc_template WHERE template_code LIKE 'TG-TPL-%'
UNION ALL SELECT '设备台账', COUNT(*) FROM dv_machinery WHERE machinery_code LIKE 'TG-MC-%'
UNION ALL SELECT '班组', COUNT(*) FROM cal_team WHERE team_code LIKE 'TG-T%'
UNION ALL SELECT '岗位角色(不含超管)', COUNT(*) FROM sys_role WHERE role_id BETWEEN 101 AND 107
UNION ALL SELECT '演示账号(不含admin)', COUNT(*) FROM sys_user WHERE user_id BETWEEN 200 AND 206;

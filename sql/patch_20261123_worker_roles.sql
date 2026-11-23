-- 工人端小程序：补齐 6 类角色中的车间主任、采购员，并对齐操作工/质检员/仓管的可见权限
-- 账号统一复用 MES；演示密码与 admin 相同（admin123）
SET NAMES utf8mb4;

-- ==================== 新增角色 ====================
INSERT INTO `sys_role` (`role_id`,`role_name`,`role_key`,`role_sort`,`data_scope`,`menu_check_strictly`,`dept_check_strictly`,`status`,`del_flag`,`create_by`,`create_time`,`remark`)
VALUES
(110,'车间主任','workshop_director',4,'1',1,1,'0','0','admin',NOW(),'工人端：工单确认/排产/报工审核/流水线'),
(111,'采购员','purchaser',8,'1',1,1,'0','0','admin',NOW(),'工人端：采购到货与收货')
ON DUPLICATE KEY UPDATE `role_name`=VALUES(`role_name`),`role_key`=VALUES(`role_key`),`status`='0',`del_flag`='0';

-- ==================== 车间主任：工单 + 排产 + 报工审核 + 质检查看 + 领料/入库查看 ====================
INSERT IGNORE INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 110, `menu_id` FROM `sys_menu` WHERE `perms` IN (
 'mes:pro:workorder','mes:pro:workorder:list','mes:pro:workorder:query','mes:pro:workorder:edit',
 'mes:pro:protask','mes:pro:protask:list','mes:pro:protask:query','mes:pro:protask:add','mes:pro:protask:edit',
 'mes:pro:feedback','mes:pro:feedback:list','mes:pro:feedback:query','mes:pro:feedback:approve',
 'mes:qc:ipqc','mes:qc:ipqc:list','mes:qc:ipqc:query','mes:qc:pending:list',
 'mes:wm:issueheader','mes:wm:issueheader:list','mes:wm:issueheader:query',
 'mes:wm:productrecpt','mes:wm:productrecpt:list','mes:wm:productrecpt:query',
 'mes:wm:wmstock','mes:wm:wmstock:list'
);

-- ==================== 采购员：采购入库 + 到货通知 + 采购退货 + 库存查看 ====================
INSERT IGNORE INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 111, `menu_id` FROM `sys_menu` WHERE `perms` IN (
 'mes:wm:itemrecpt','mes:wm:itemrecpt:list','mes:wm:itemrecpt:query','mes:wm:itemrecpt:add','mes:wm:itemrecpt:edit',
 'mes:wm:arrivalnotice','mes:wm:arrivalnotice:list','mes:wm:arrivalnotice:query','mes:wm:arrivalnotice:add','mes:wm:arrivalnotice:edit',
 'mes:wm:rtvendor','mes:wm:rtvendor:list','mes:wm:rtvendor:query',
 'mes:wm:wmstock','mes:wm:wmstock:list'
);

-- ==================== 生产操作工：只保留报工与工单/领料查看，去掉审批评审与排产维护 ====================
DELETE rm FROM `sys_role_menu` rm JOIN `sys_menu` m ON m.`menu_id` = rm.`menu_id`
WHERE rm.`role_id` = 104 AND m.`perms` IN (
 'mes:pro:feedback:approve','mes:pro:feedback:remove',
 'mes:pro:protask:add','mes:pro:protask:edit','mes:pro:protask:remove'
);
INSERT IGNORE INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 104, `menu_id` FROM `sys_menu` WHERE `perms` IN (
 'mes:pro:workorder','mes:pro:workorder:list','mes:pro:workorder:query',
 'mes:wm:issueheader','mes:wm:issueheader:list','mes:wm:issueheader:query'
);

-- ==================== 质检员：补工单与报工查看，便于按批次追溯 ====================
INSERT IGNORE INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 105, `menu_id` FROM `sys_menu` WHERE `perms` IN (
 'mes:pro:workorder','mes:pro:workorder:list','mes:pro:workorder:query',
 'mes:pro:feedback','mes:pro:feedback:list','mes:pro:feedback:query'
);

-- ==================== 仓库管理员：补工单查看与库存查看 ====================
INSERT IGNORE INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 103, `menu_id` FROM `sys_menu` WHERE `perms` IN (
 'mes:pro:workorder','mes:pro:workorder:list','mes:pro:workorder:query',
 'mes:wm:wmstock','mes:wm:wmstock:list'
);

-- ==================== 演示账号：车间主任 / 采购员（密码与 admin 相同） ====================
INSERT INTO `sys_user` (`user_id`,`dept_id`,`user_name`,`nick_name`,`user_type`,`password`,`status`,`del_flag`,`create_by`,`create_time`,`remark`)
SELECT 207,100,'workshop','周主任','00',u.`password`,'0','0','admin',NOW(),'工人端-车间主任'
FROM `sys_user` u WHERE u.`user_id` = 1
ON DUPLICATE KEY UPDATE `nick_name`='周主任',`status`='0',`del_flag`='0';

INSERT INTO `sys_user` (`user_id`,`dept_id`,`user_name`,`nick_name`,`user_type`,`password`,`status`,`del_flag`,`create_by`,`create_time`,`remark`)
SELECT 208,100,'purchaser','钱采购','00',u.`password`,'0','0','admin',NOW(),'工人端-采购员'
FROM `sys_user` u WHERE u.`user_id` = 1
ON DUPLICATE KEY UPDATE `nick_name`='钱采购',`status`='0',`del_flag`='0';

INSERT IGNORE INTO `sys_user_role` (`user_id`,`role_id`) VALUES (207,110),(208,111);

-- ==================== 流水线只读权限 ====================
-- productproduce:list 没有现成菜单，补一个按钮权限位，供各岗位查看工单产出
INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`remark`)
VALUES (2441,'产品产出查询',2042,99,'','','',1,0,'F','0','0','mes:wm:productproduce:list','#','admin',NOW(),'工人端流水线只读权限')
ON DUPLICATE KEY UPDATE `perms`='mes:wm:productproduce:list',`status`='0';

-- 各岗位统一补齐流水线只读数据源（不影响写操作）
INSERT IGNORE INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
JOIN `sys_menu` m ON m.`perms` IN (
 'mes:qc:ipqc:list','mes:wm:productrecpt:list','mes:wm:productproduce:list',
 'mes:wm:wmstock:list','mes:wm:productsales:list'
)
WHERE r.`role_key` IN ('workshop_director','warehouse','operator','inspector','purchaser');

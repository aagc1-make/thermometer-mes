-- =====================================================================
-- 智能助手菜单补齐（幂等升级脚本）
-- 适用：2026-10-24 之前初始化、缺少智能助手菜单的既有环境（无 MYSQL_PWD 等凭据时不要执行）
-- 不适用：全新环境直接导入 01_schema.sql + 02_data.sql 即可（02_data.sql 已含本菜单）
--
-- 口径（与 sql/deploy/02_data.sql 第 3768/3769 行一致）：
--   2410 智能助手  （一级目录，path=mes/ai，order_num=2）
--   2411 智能问答  （子菜单，parent=2410，component=mes/ai/index，perms=mes:ai:query）
--   授权角色：1 超级管理员、101 工艺员、102 计划员、107 班组长
--
-- 安全性：
--   1. 仅当菜单不存在（按 menu_id，或 名称+父级+path 判重）时插入，不更新已存在菜单，
--      不覆盖名称/父级/排序/权限；
--   2. 角色授权按 (role_id, menu_id) 去重，不删除、不重排已有授权；菜单不存在时跳过授权；
--   3. 可重复执行，结果一致。
--   4. ID 或定义冲突时跳过相关写入；执行后检查末尾诊断，冲突需人工处理。
-- =====================================================================

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`,
   `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
   `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2410, '智能助手', 0, 2, 'mes/ai', NULL, NULL,
       1, 0, 'M', '0', '0', NULL, 'message',
       'admin', '2026-10-24 22:06:48', '', NULL, '自然语言查询库存/工单/检验'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_menu`
  WHERE `menu_id` = 2410
     OR (`menu_name` = '智能助手' AND `parent_id` = 0 AND `path` = 'mes/ai')
);

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`,
   `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
   `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2411, '智能问答', 2410, 1, 'chat', 'mes/ai/index', NULL,
       1, 0, 'C', '0', '0', 'mes:ai:query', 'message',
       'admin', '2026-10-24 22:06:48', '', NULL, ''
FROM DUAL
WHERE EXISTS (
  SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2410 AND `menu_name` = '智能助手'
    AND `parent_id` = 0 AND `path` = 'mes/ai' AND `menu_type` = 'M'
)
AND NOT EXISTS (
  SELECT 1 FROM `sys_menu`
  WHERE `menu_id` = 2411
     OR (`menu_name` = '智能问答' AND `parent_id` = 2410 AND `path` = 'chat')
);

-- 角色授权：只补缺失项；角色不存在时自动跳过
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, 2410
FROM `sys_role` r
WHERE ((r.`role_id` = 1 AND r.`role_key` = 'admin') OR (r.`role_id` = 101 AND r.`role_key` = 'craftsman')
    OR (r.`role_id` = 102 AND r.`role_key` = 'planner') OR (r.`role_id` = 107 AND r.`role_key` = 'teamleader'))
  AND EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2410 AND `menu_name` = '智能助手' AND `parent_id` = 0 AND `path` = 'mes/ai' AND `menu_type` = 'M')
  AND EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2411 AND `menu_name` = '智能问答' AND `parent_id` = 2410 AND `path` = 'chat' AND `component` = 'mes/ai/index' AND `perms` = 'mes:ai:query' AND `menu_type` = 'C')
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = 2410);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, 2411
FROM `sys_role` r
WHERE ((r.`role_id` = 1 AND r.`role_key` = 'admin') OR (r.`role_id` = 101 AND r.`role_key` = 'craftsman')
    OR (r.`role_id` = 102 AND r.`role_key` = 'planner') OR (r.`role_id` = 107 AND r.`role_key` = 'teamleader'))
  AND EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2410 AND `menu_name` = '智能助手' AND `parent_id` = 0 AND `path` = 'mes/ai' AND `menu_type` = 'M')
  AND EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2411 AND `menu_name` = '智能问答' AND `parent_id` = 2410 AND `path` = 'chat' AND `component` = 'mes/ai/index' AND `perms` = 'mes:ai:query' AND `menu_type` = 'C')
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = 2411);

SELECT `menu_id`, `menu_name`, `parent_id`, `path`, `component`, `perms`, `menu_type`
FROM `sys_menu` WHERE `menu_id` IN (2410, 2411) OR (`menu_name` IN ('智能助手', '智能问答'));

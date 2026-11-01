-- 为 test 单独添加生产报工审核权限；不改变检验员角色或其他用户。
INSERT INTO sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT '报工审核-test', 'feedback_approver_test', 99, '1', 1, 1, '0', '0', 'system', NOW(), 'test账号生产报工审核'
WHERE EXISTS (SELECT 1 FROM sys_user WHERE user_name = 'test' AND status = '0' AND del_flag = '0')
  AND NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'feedback_approver_test');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role.role_id, menu.menu_id FROM sys_role role JOIN sys_menu menu
  ON menu.menu_id IN (2072, 2237) OR menu.perms IN ('mes:pro:feedback:list', 'mes:pro:feedback:query', 'mes:pro:feedback:edit', 'mes:pro:feedback:approve')
WHERE role.role_key = 'feedback_approver_test' AND role.status = '0' AND role.del_flag = '0' AND menu.status = '0';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT user.user_id, role.role_id FROM sys_user user JOIN sys_role role ON role.role_key = 'feedback_approver_test'
WHERE user.user_name = 'test' AND user.status = '0' AND user.del_flag = '0' AND role.status = '0' AND role.del_flag = '0';

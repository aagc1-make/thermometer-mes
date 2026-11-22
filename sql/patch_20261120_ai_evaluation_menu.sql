-- 评测使用单独的管理员入口。
START TRANSACTION;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '问答评测', directory.menu_id, 4, 'evaluation', 'mes/ai/Evaluation', 1, 1,
       'C', '0', '0', 'mes:ai:query', 'chart', 'admin', now()
FROM sys_menu directory
WHERE directory.parent_id = 0 AND directory.path = 'mes/ai' AND directory.menu_type = 'M'
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu child WHERE child.parent_id = directory.menu_id AND child.path = 'evaluation'
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, evaluation.menu_id
FROM sys_role r
JOIN sys_menu directory ON directory.parent_id = 0 AND directory.path = 'mes/ai' AND directory.menu_type = 'M'
JOIN sys_menu evaluation ON evaluation.parent_id = directory.menu_id AND evaluation.path = 'evaluation'
  AND evaluation.component = 'mes/ai/Evaluation'
WHERE r.role_key = 'admin' AND r.del_flag = '0'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu existing WHERE existing.role_id = r.role_id AND existing.menu_id = evaluation.menu_id
  );

COMMIT;

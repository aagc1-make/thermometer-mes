-- 知识图谱沿用智能问答菜单的角色授权。
START TRANSACTION;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '知识图谱', directory.menu_id, 3, 'graph', 'mes/ai/KnowledgeGraph', 1, 1,
       'C', '0', '0', 'mes:ai:query', 'tree', 'admin', now()
FROM sys_menu directory
WHERE directory.parent_id = 0 AND directory.path = 'mes/ai' AND directory.menu_type = 'M'
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu existing
    WHERE existing.parent_id = directory.menu_id AND existing.path = 'graph'
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT chat_role.role_id, graph.menu_id
FROM sys_menu directory
JOIN sys_menu chat ON chat.parent_id = directory.menu_id AND chat.path = 'chat' AND chat.perms = 'mes:ai:query'
JOIN sys_role_menu chat_role ON chat_role.menu_id = chat.menu_id
JOIN sys_menu graph ON graph.parent_id = directory.menu_id AND graph.path = 'graph'
  AND graph.component = 'mes/ai/KnowledgeGraph'
WHERE directory.parent_id = 0 AND directory.path = 'mes/ai' AND directory.menu_type = 'M'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu existing
    WHERE existing.role_id = chat_role.role_id AND existing.menu_id = graph.menu_id
  );

COMMIT;

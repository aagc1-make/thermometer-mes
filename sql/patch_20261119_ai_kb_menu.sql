-- 在现有智能问答菜单下补充知识库管理入口，沿用智能问答的角色授权。
START TRANSACTION;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '知识库管理', directory.menu_id, 2, 'knowledge', 'mes/ai/KnowledgeManagement', 1, 1,
       'C', '0', '0', 'mes:ai:query', 'documentation', 'admin', now()
FROM sys_menu directory
WHERE directory.parent_id = 0 AND directory.path = 'mes/ai' AND directory.menu_type = 'M'
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu current_menu
    WHERE current_menu.parent_id = directory.menu_id AND current_menu.path = 'knowledge'
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT chat_role.role_id, knowledge.menu_id
FROM sys_menu directory
JOIN sys_menu chat ON chat.parent_id = directory.menu_id AND chat.path = 'chat' AND chat.perms = 'mes:ai:query'
JOIN sys_role_menu chat_role ON chat_role.menu_id = chat.menu_id
JOIN sys_menu knowledge ON knowledge.parent_id = directory.menu_id AND knowledge.path = 'knowledge'
  AND knowledge.component = 'mes/ai/KnowledgeManagement'
WHERE directory.parent_id = 0 AND directory.path = 'mes/ai' AND directory.menu_type = 'M'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu current_role
    WHERE current_role.role_id = chat_role.role_id AND current_role.menu_id = knowledge.menu_id
  );

COMMIT;

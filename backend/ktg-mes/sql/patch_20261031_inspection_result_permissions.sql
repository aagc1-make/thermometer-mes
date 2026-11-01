-- 检测结果是来料、过程、出货和退料检验共用的子功能。
-- 补充菜单权限标识；仅为在四种检验中都拥有对应操作权限的检验员角色授予相应权限。
-- 可重复执行；不授予生产报工或其他模块权限。

INSERT INTO sys_menu
    (menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT p.menu_name, parent.menu_id, p.order_num, '', 1, 0, 'F', '0', '0', p.perms, '#', 'system', NOW()
FROM (
    SELECT '检测结果列表' AS menu_name, 91 AS order_num, 'mes:qc:qcresult:list' AS perms
    UNION ALL SELECT '检测结果查看', 92, 'mes:qc:qcresult:query'
    UNION ALL SELECT '检测结果新增', 93, 'mes:qc:qcresult:add'
    UNION ALL SELECT '检测结果修改', 94, 'mes:qc:qcresult:edit'
    UNION ALL SELECT '检测结果删除', 95, 'mes:qc:qcresult:remove'
    UNION ALL SELECT '检测结果导出', 96, 'mes:qc:qcresult:export'
) p
JOIN (SELECT MIN(parent_id) AS menu_id FROM sys_menu WHERE perms = 'mes:qc:iqc' AND menu_type = 'C') source_parent
JOIN sys_menu parent ON parent.menu_id = source_parent.menu_id AND parent.menu_type = 'M'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu existing WHERE existing.perms = p.perms);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role.role_id, result_menu.menu_id
FROM sys_role role
JOIN sys_menu result_menu ON result_menu.status = '0' AND result_menu.perms IN (
    'mes:qc:qcresult:list', 'mes:qc:qcresult:query', 'mes:qc:qcresult:add',
    'mes:qc:qcresult:edit', 'mes:qc:qcresult:remove', 'mes:qc:qcresult:export'
)
WHERE role.role_key = 'inspector' AND role.status = '0' AND role.del_flag = '0'
  AND (
    SELECT COUNT(DISTINCT source_menu.perms)
    FROM sys_role_menu source_role_menu
    JOIN sys_menu source_menu ON source_menu.menu_id = source_role_menu.menu_id AND source_menu.status = '0'
    WHERE source_role_menu.role_id = role.role_id
      AND source_menu.perms IN (
        CONCAT('mes:qc:iqc:', SUBSTRING_INDEX(result_menu.perms, ':', -1)),
        CONCAT('mes:qc:ipqc:', SUBSTRING_INDEX(result_menu.perms, ':', -1)),
        CONCAT('mes:qc:oqc:', SUBSTRING_INDEX(result_menu.perms, ':', -1)),
        CONCAT('mes:qc:rqc:', SUBSTRING_INDEX(result_menu.perms, ':', -1))
      )
  ) = 4;

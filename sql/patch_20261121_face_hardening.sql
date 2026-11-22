-- 人脸登录加固：授权留痕、补充角度模板、管理员入口
-- 执行顺序：先 sql/face_login.sql（已执行过的可跳过），再执行本脚本。
-- 仅新增列、新增表与菜单，不删除任何数据。

START TRANSACTION;

-- 1. 授权留痕：记录同意时间与授权说明版本，便于举证与撤回（可重复执行）
SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `sys_user_face` ADD COLUMN `consent_at` datetime DEFAULT NULL COMMENT ''授权同意时间'' AFTER `update_time`',
  'DO 0')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'sys_user_face' AND column_name = 'consent_at');
PREPARE alter_consent_at FROM @ddl;
EXECUTE alter_consent_at;
DEALLOCATE PREPARE alter_consent_at;

SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE `sys_user_face` ADD COLUMN `consent_version` varchar(20) DEFAULT NULL COMMENT ''授权说明版本'' AFTER `consent_at`',
  'DO 0')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'sys_user_face' AND column_name = 'consent_version');
PREPARE alter_consent_version FROM @ddl;
EXECUTE alter_consent_version;
DEALLOCATE PREPARE alter_consent_version;

-- 2. 补充角度模板（多模板）：主模板仍在 sys_user_face，这里只放额外角度，最多 3 条
CREATE TABLE IF NOT EXISTS `sys_user_face_alt` (
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `seq` tinyint(4) NOT NULL DEFAULT '1' COMMENT '补充角度序号，从 1 开始',
  `descriptor` text NOT NULL COMMENT '加密特征（gcm:v1:...）',
  `algorithm` varchar(64) NOT NULL COMMENT '特征算法版本',
  `samples` int(11) DEFAULT '0' COMMENT '录入样本帧数',
  `quality` decimal(5,4) DEFAULT NULL COMMENT '录入质量分0-1',
  `create_time` datetime DEFAULT NULL COMMENT '录入时间',
  PRIMARY KEY (`user_id`, `seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人脸补充角度模板（只存加密特征）';

-- 3. 管理员菜单：系统管理 → 人脸管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '人脸管理', parent.menu_id, 6, 'face', 'system/user/face/index', 1, 1,
       'C', '0', '0', 'system:user:face:list', 'peoples', 'admin', now()
FROM sys_menu parent
WHERE parent.parent_id = 0 AND parent.path = 'system' AND parent.menu_type = 'M'
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu existing
    WHERE existing.parent_id = parent.menu_id AND existing.path = 'face'
  );

-- 4. 按钮权限：重置模板、停用/启用人脸登录
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '人脸模板重置', face.menu_id, 1, '', '', 1, 1, 'F', '0', '0', 'system:user:face:remove', '#', 'admin', now()
FROM sys_menu face
WHERE face.path = 'face' AND face.component = 'system/user/face/index'
  AND NOT EXISTS (SELECT 1 FROM sys_menu e WHERE e.perms = 'system:user:face:remove');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '人脸登录启停', face.menu_id, 2, '', '', 1, 1, 'F', '0', '0', 'system:user:face:status', '#', 'admin', now()
FROM sys_menu face
WHERE face.path = 'face' AND face.component = 'system/user/face/index'
  AND NOT EXISTS (SELECT 1 FROM sys_menu e WHERE e.perms = 'system:user:face:status');

-- 5. 授权给管理员角色
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.perms IN ('system:user:face:list', 'system:user:face:remove', 'system:user:face:status')
WHERE r.role_key = 'admin' AND r.del_flag = '0'
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu e WHERE e.role_id = r.role_id AND e.menu_id = m.menu_id);

COMMIT;

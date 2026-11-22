-- 管理员的登录策略随账号保存，不受模板更新或解绑影响。
CREATE TABLE IF NOT EXISTS `sys_user_face_policy` (
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '1启用人脸登录，0停用人脸登录',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户人脸登录策略';

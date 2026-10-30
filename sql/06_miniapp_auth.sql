-- ============================================================
-- 小程序登录账号表（miniapp-api 鉴权层）
-- 说明：可重复执行；演示账号 demo / demo123（salt = ktg2026）
--       密码摘要 = SHA-256(salt + 明文密码) 的十六进制
-- ============================================================
CREATE TABLE IF NOT EXISTS miniapp_account (
  account_id      bigint       NOT NULL AUTO_INCREMENT COMMENT '账号ID',
  client_id       bigint       NOT NULL                COMMENT '所属客户ID',
  username        varchar(64)  NOT NULL                COMMENT '登录账号',
  password_hash   char(64)     NOT NULL                COMMENT 'SHA-256(salt+密码)',
  salt            varchar(32)  NOT NULL                COMMENT '密码盐',
  display_name    varchar(64)      NULL                COMMENT '显示名称',
  enabled         char(1)      NOT NULL DEFAULT 'Y'    COMMENT '是否启用（Y是 N否）',
  last_login_time datetime         NULL                COMMENT '最近登录时间',
  create_time     datetime         NULL                COMMENT '创建时间',
  PRIMARY KEY (account_id),
  UNIQUE KEY uk_miniapp_account_username (username),
  KEY idx_miniapp_account_client (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序登录账号';

INSERT INTO miniapp_account (client_id, username, password_hash, salt, display_name, enabled, create_time)
VALUES (1, 'demo', '2d18619ecad030c351093341e0ed6bc6a01a753105c89d5b7532706734413d00', 'ktg2026', '演示客户', 'Y', NOW())
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), salt = VALUES(salt),
                        display_name = VALUES(display_name), enabled = VALUES(enabled);

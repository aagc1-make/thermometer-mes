-- 关联表单生成后的共享菜单蓝点提醒。
-- 每个目标菜单只有一条状态；任意用户进入菜单后全局清除。

CREATE TABLE IF NOT EXISTS sys_menu_linkage_notice (
    notice_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '提醒ID',
    menu_path VARCHAR(255) NOT NULL COMMENT '目标菜单绝对路径',
    unread_flag CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '是否未读（Y/N）',
    trigger_count BIGINT NOT NULL DEFAULT 1 COMMENT '累计触发次数',
    last_trigger_time DATETIME NOT NULL COMMENT '最近触发时间',
    read_time DATETIME NULL COMMENT '最近已读时间',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (notice_id),
    UNIQUE KEY uk_menu_linkage_notice_path (menu_path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关联表单菜单共享提醒';

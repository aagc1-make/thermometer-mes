-- 智能助手 v1.0 / M1：部门知识库、文档、文档版本、入库任务和文档分块。
-- MySQL 8.0.16+。在已初始化 sys_dept、sys_user 的目标库执行。
-- 可重复执行。兼容已执行过早期四表补丁的数据库，不写入初始数据。

CREATE TABLE IF NOT EXISTS `kb_library` (
  `library_id` bigint NOT NULL AUTO_INCREMENT,
  `dept_id` bigint NOT NULL,
  `name` varchar(120) NOT NULL,
  `type` varchar(20) NOT NULL DEFAULT 'department',
  `owner_id` bigint DEFAULT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'enabled',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`library_id`),
  UNIQUE KEY `uk_kb_library_dept` (`dept_id`),
  UNIQUE KEY `uk_kb_library_scope` (`dept_id`, `library_id`),
  CONSTRAINT `fk_kb_library_dept` FOREIGN KEY (`dept_id`) REFERENCES `sys_dept` (`dept_id`),
  CONSTRAINT `fk_kb_library_owner` FOREIGN KEY (`owner_id`) REFERENCES `sys_user` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门知识库';

CREATE TABLE IF NOT EXISTS `kb_document` (
  `doc_id` bigint NOT NULL AUTO_INCREMENT,
  `library_id` bigint NOT NULL,
  `dept_id` bigint NOT NULL,
  `title` varchar(120) NOT NULL,
  `category` varchar(40) NOT NULL,
  `current_version` varchar(40) DEFAULT NULL COMMENT '已发布的版本号，由发布事务与版本启用状态同步维护',
  `graph_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '是否参与建图',
  `status` varchar(16) NOT NULL DEFAULT 'pending',
  `deleted_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`doc_id`),
  UNIQUE KEY `uk_kb_document_name` (`library_id`, `title`, `category`),
  KEY `idx_kb_document_scope` (`dept_id`, `library_id`),
  CONSTRAINT `fk_kb_document_library_scope` FOREIGN KEY (`dept_id`, `library_id`)
    REFERENCES `kb_library` (`dept_id`, `library_id`),
  CONSTRAINT `ck_kb_document_graph_enabled` CHECK (`graph_enabled` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库文档';

CREATE TABLE IF NOT EXISTS `kb_document_version` (
  `version_id` bigint NOT NULL AUTO_INCREMENT,
  `doc_id` bigint NOT NULL,
  `version` varchar(40) NOT NULL,
  `filename` varchar(255) NOT NULL,
  `size` bigint unsigned NOT NULL COMMENT '原件字节数',
  `chars` int unsigned DEFAULT NULL COMMENT '解析后字符数',
  `storage_path` varchar(512) NOT NULL COMMENT '原件存储路径',
  `checksum_sha256` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '原件校验和',
  `active` tinyint NOT NULL DEFAULT 0,
  `active_doc_id` bigint GENERATED ALWAYS AS (CASE WHEN `active` = 1 THEN `doc_id` END) VIRTUAL,
  `deactivated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`version_id`),
  UNIQUE KEY `uk_kb_version_label` (`doc_id`, `version`),
  UNIQUE KEY `uk_kb_version_active` (`active_doc_id`),
  CONSTRAINT `fk_kb_version_document` FOREIGN KEY (`doc_id`) REFERENCES `kb_document` (`doc_id`),
  CONSTRAINT `ck_kb_version_active` CHECK (`active` IN (0, 1) AND (`deleted_at` IS NULL OR `active` = 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库文档版本';

CREATE TABLE IF NOT EXISTS `kb_ingest_task` (
  `task_id` bigint NOT NULL AUTO_INCREMENT,
  `version_id` bigint NOT NULL,
  `stage` varchar(16) NOT NULL COMMENT 'parse 解析，embed 向量化，graph 图谱抽取',
  `status` varchar(16) NOT NULL DEFAULT 'pending',
  `error` text,
  `started_at` datetime DEFAULT NULL,
  `finished_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`task_id`),
  KEY `idx_kb_task_version_stage` (`version_id`, `stage`, `task_id`),
  CONSTRAINT `fk_kb_task_version` FOREIGN KEY (`version_id`) REFERENCES `kb_document_version` (`version_id`),
  CONSTRAINT `ck_kb_task_stage` CHECK (`stage` IN ('parse', 'embed', 'graph')),
  CONSTRAINT `ck_kb_task_status` CHECK (`status` IN ('pending', 'running', 'completed', 'failed'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库入库任务';

-- 活跃文档占用名称。软删除后释放名称，同时保留原文档及其历史版本。
SET @kb_alter_document = IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'kb_document' AND COLUMN_NAME = 'name_reserved'),
  'DO 0',
  'ALTER TABLE kb_document
     ADD COLUMN name_reserved tinyint GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN 1 END) VIRTUAL,
     DROP INDEX uk_kb_document_name,
     ADD UNIQUE KEY uk_kb_document_name (library_id, title, category, name_reserved),
     ADD UNIQUE KEY uk_kb_document_scope (dept_id, library_id, doc_id)'
);
PREPARE kb_alter_document FROM @kb_alter_document;
EXECUTE kb_alter_document;
DEALLOCATE PREPARE kb_alter_document;

SET @kb_alter_version = IF(
  EXISTS (SELECT 1 FROM information_schema.STATISTICS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'kb_document_version' AND INDEX_NAME = 'uk_kb_version_document'),
  'DO 0',
  'ALTER TABLE kb_document_version ADD UNIQUE KEY uk_kb_version_document (doc_id, version_id)'
);
PREPARE kb_alter_version FROM @kb_alter_version;
EXECUTE kb_alter_version;
DEALLOCATE PREPARE kb_alter_version;

CREATE TABLE IF NOT EXISTS `kb_document_chunk` (
  `chunk_id` bigint NOT NULL AUTO_INCREMENT,
  `version_id` bigint NOT NULL,
  `doc_id` bigint NOT NULL,
  `library_id` bigint NOT NULL,
  `dept_id` bigint NOT NULL,
  `chunk_index` int unsigned NOT NULL COMMENT '从 0 开始的分块序号',
  `page_no` int unsigned DEFAULT NULL COMMENT '文件自带页码时从 1 开始',
  `section_label` varchar(160) DEFAULT NULL,
  `content` mediumtext NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`chunk_id`),
  UNIQUE KEY `uk_kb_chunk_version_index` (`version_id`, `chunk_index`),
  KEY `idx_kb_chunk_scope` (`dept_id`, `library_id`, `doc_id`),
  CONSTRAINT `fk_kb_chunk_document_scope` FOREIGN KEY (`dept_id`, `library_id`, `doc_id`)
    REFERENCES `kb_document` (`dept_id`, `library_id`, `doc_id`),
  CONSTRAINT `fk_kb_chunk_document_version` FOREIGN KEY (`doc_id`, `version_id`)
    REFERENCES `kb_document_version` (`doc_id`, `version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库文档分块';

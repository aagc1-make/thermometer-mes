CREATE TABLE IF NOT EXISTS mes_kb_document (
    document_id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    active_version_id BIGINT DEFAULT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (document_id),
    KEY idx_kb_owner (owner_id, deleted, document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mes_kb_version (
    version_id BIGINT NOT NULL AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    index_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (version_id),
    UNIQUE KEY uk_kb_doc_version (document_id, version_no),
    UNIQUE KEY uk_kb_storage (storage_key),
    CONSTRAINT fk_kb_version_document
        FOREIGN KEY (document_id) REFERENCES mes_kb_document (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

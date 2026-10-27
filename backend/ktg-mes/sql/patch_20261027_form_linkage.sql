-- 表单联动幂等约束。
-- 历史记录保持不变；只有本次改造写入 AUTO: 前缀的 attr1 才参与唯一约束。
-- 执行前请按项目规范备份 j2eedb。

ALTER TABLE qc_iqc
    ADD COLUMN linkage_key VARCHAR(191)
        GENERATED ALWAYS AS (CASE WHEN attr1 LIKE 'AUTO:%' THEN attr1 ELSE NULL END) STORED,
    ADD UNIQUE KEY uk_qc_iqc_linkage_key (linkage_key);

ALTER TABLE qc_ipqc
    ADD COLUMN linkage_key VARCHAR(191)
        GENERATED ALWAYS AS (CASE WHEN attr1 LIKE 'AUTO:%' THEN attr1 ELSE NULL END) STORED,
    ADD UNIQUE KEY uk_qc_ipqc_linkage_key (linkage_key);

ALTER TABLE qc_oqc
    ADD COLUMN linkage_key VARCHAR(191)
        GENERATED ALWAYS AS (CASE WHEN attr1 LIKE 'AUTO:%' THEN attr1 ELSE NULL END) STORED,
    ADD UNIQUE KEY uk_qc_oqc_linkage_key (linkage_key);

ALTER TABLE wm_item_recpt
    ADD COLUMN linkage_key VARCHAR(191)
        GENERATED ALWAYS AS (CASE WHEN attr1 LIKE 'AUTO:%' THEN attr1 ELSE NULL END) STORED,
    ADD UNIQUE KEY uk_wm_item_recpt_linkage_key (linkage_key);

ALTER TABLE wm_product_sales
    ADD COLUMN linkage_key VARCHAR(191)
        GENERATED ALWAYS AS (CASE WHEN attr1 LIKE 'AUTO:%' THEN attr1 ELSE NULL END) STORED,
    ADD UNIQUE KEY uk_wm_product_sales_linkage_key (linkage_key);

ALTER TABLE wm_product_sales_line
    ADD COLUMN linkage_key VARCHAR(191)
        GENERATED ALWAYS AS (CASE WHEN attr1 LIKE 'AUTO:%' THEN attr1 ELSE NULL END) STORED,
    ADD UNIQUE KEY uk_wm_product_sales_line_linkage_key (linkage_key);

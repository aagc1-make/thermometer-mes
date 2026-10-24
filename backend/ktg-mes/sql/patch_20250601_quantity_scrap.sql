-- =====================================================================
-- 补丁：生产报工 / IPQC 新增「工废、料废、其他废」数量字段
--
-- 背景：代码提交 5eb0f37（2025-06-01「生产报工增加工废和料废数量」）
--       给 ProFeedback / QcIpqc 两个实体和对应 Mapper 增加了 3 个字段，
--       但官方 SQL 脚本 ktgmes_20250518 早于该提交，没有这些列，
--       导致查询报 Unknown column 'f.quantity_labor_scrap' in 'field list'。
--
-- 用法：mysql -h127.0.0.1 -P3307 -uroot -p123456789 --default-character-set=utf8mb4 j2eedb < 本文件
-- 说明：纯新增列，可重复执行（已存在会报 1060，可忽略）；历史数据为 NULL。
-- =====================================================================

ALTER TABLE `pro_feedback`
    ADD COLUMN `quantity_labor_scrap`    double(14,2) NULL COMMENT '工废数量'     AFTER `quantity_uncheck`,
    ADD COLUMN `quantity_material_scrap` double(14,2) NULL COMMENT '料废数量'     AFTER `quantity_labor_scrap`,
    ADD COLUMN `quantity_other_scrap`    double(14,2) NULL COMMENT '其他废品数量' AFTER `quantity_material_scrap`;

ALTER TABLE `qc_ipqc`
    ADD COLUMN `quantity_labor_scrap`    double(14,2) NULL COMMENT '工废数量'     AFTER `quantity_qualified`,
    ADD COLUMN `quantity_material_scrap` double(14,2) NULL COMMENT '料废数量'     AFTER `quantity_labor_scrap`,
    ADD COLUMN `quantity_other_scrap`    double(14,2) NULL COMMENT '其他废品数量' AFTER `quantity_material_scrap`;

-- 校验
-- SELECT column_name, column_type FROM information_schema.columns
--  WHERE table_schema = 'j2eedb' AND table_name = 'pro_feedback'
--    AND column_name LIKE 'quantity%';

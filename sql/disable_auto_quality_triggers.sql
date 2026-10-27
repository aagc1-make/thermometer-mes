-- =====================================================================
-- 停用我方两个自动建检验单触发器（方案 A：改由 linkage 应用层建草稿）
--
-- 背景：合并 feat-menu-linkage-notification 后，两套建单逻辑会并存：
--   ① 应用层 linkage（到货提交 → IQC 草稿；报工产出 → IPQC 草稿）
--   ② 数据库触发器（入库完成 → IQC 复检；产出行插入 → IPQC）
-- 同时启用会让同一批货/同一产出行生成两张检验单，故按方案 A 停用触发器。
--
-- 本脚本只 DROP TRIGGER，不改任何业务数据；触发器定义仍保留在
--   sql/auto_ipqc_trigger.sql、sql/auto_iqc_trigger.sql
-- 需要回退时重新执行这两个脚本即可。
-- =====================================================================

SELECT DATABASE() AS 当前库;
-- 隔离库为 j2eedb_it；生产库为 j2eedb。两个库都需要执行一次。

DROP TRIGGER IF EXISTS trg_feedback_auto_ipqc;
DROP TRIGGER IF EXISTS trg_recpt_auto_iqc;

SELECT trigger_name, event_object_table
FROM information_schema.triggers
WHERE trigger_schema = DATABASE();
-- 期望：无输出（本库已无触发器）

-- =============================================================================
-- 报工单号缩短：父工单号-bg三位序号
-- 文件：sql/patch_20261121_feedback_code_short.sql
-- 目标库：j2eedb（MySQL 8.4）
--
-- 背景：
--   报工单号原来内嵌完整取号（如 gd2026112100003-bg2026112100012）。
--   工单号本身已含日期，报工号再带一遍日期冗余；新格式改为
--   「父工单号-bg三位序号」，如 gd2026112100003-bg012。
--   序号按该工单已有报工递增，单工单百级数量足够。
--
-- 内容：
--   1) 重建 fn_feedback_code_valid，允许「工单号-bg数字」后缀（3 位起，兼容历史长格式）
--   2) 重建报工单号校验触发器
--   3) 已存在的长格式报工号迁移成短格式，并同步更新 qc_ipqc.source_doc_code
--
-- 幂等性：
--   * 函数/触发器先 DROP 再建；
--   * 迁移只处理「-bg 后缀长度大于 5」的长格式，重复执行不会再次改写。
-- =============================================================================

SET NAMES utf8mb4;

DROP TRIGGER IF EXISTS trg_pro_feedback_code_bi;
DROP TRIGGER IF EXISTS trg_pro_feedback_code_bu;
DROP FUNCTION IF EXISTS fn_feedback_code_valid;

DELIMITER $$

CREATE FUNCTION fn_feedback_code_valid(p_code VARCHAR(64)) RETURNS tinyint
    DETERMINISTIC
BEGIN
  IF fn_doc_code_valid(p_code) = 1 THEN
    RETURN 1;
  END IF;
  -- 工单号-bg序号：新格式 3 位起，兼容历史 13 位日期流水
  IF p_code COLLATE utf8mb4_bin REGEXP '^[a-z]{2}[0-9]{13}-[a-z]{2}[0-9]{3,}$' THEN
    RETURN 1;
  END IF;
  RETURN 0;
END$$

CREATE TRIGGER trg_pro_feedback_code_bi BEFORE INSERT ON pro_feedback
FOR EACH ROW
BEGIN
  IF fn_feedback_code_valid(NEW.feedback_code) = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '报工单号格式错误：历史格式或“工单号-bg三位序号”';
  END IF;
END$$

CREATE TRIGGER trg_pro_feedback_code_bu BEFORE UPDATE ON pro_feedback
FOR EACH ROW
BEGIN
  IF NOT (OLD.feedback_code <=> NEW.feedback_code) AND fn_feedback_code_valid(NEW.feedback_code) = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '报工单号格式错误：历史格式或“工单号-bg三位序号”';
  END IF;
END$$

DELIMITER ;

-- 迁移：先改引用方 qc_ipqc，再改报工单自身
UPDATE qc_ipqc
SET source_doc_code = CONCAT(workorder_code, '-bg', LPAD(CAST(RIGHT(source_doc_code, 5) AS UNSIGNED), 3, '0'))
WHERE source_doc_type = 'FEEDBACK'
  AND source_doc_code LIKE CONCAT(workorder_code, '-bg%')
  AND CHAR_LENGTH(SUBSTRING_INDEX(source_doc_code, '-', -1)) > 5;

UPDATE pro_feedback
SET feedback_code = CONCAT(workorder_code, '-bg', LPAD(CAST(RIGHT(feedback_code, 5) AS UNSIGNED), 3, '0'))
WHERE feedback_code LIKE CONCAT(workorder_code, '-bg%')
  AND CHAR_LENGTH(SUBSTRING_INDEX(feedback_code, '-', -1)) > 5;

-- 验证：应为 gd...-bg006、gd...-bg007 ... 这类短单号
SELECT record_id, feedback_code FROM pro_feedback WHERE feedback_code LIKE '%-bg%' ORDER BY record_id;
SELECT ipqc_id, ipqc_code, source_doc_code FROM qc_ipqc WHERE source_doc_type = 'FEEDBACK' ORDER BY ipqc_id;

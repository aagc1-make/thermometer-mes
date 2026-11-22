-- =============================================================================
-- 以工单为核心的链号对齐：允许链号子序号 + 工单批次统一 pc 前缀
-- 文件：sql/patch_20261121_chain_code_alignment.sql
-- 目标库：j2eedb（MySQL 8.4）
--
-- 背景：
--   订单 dd2026112100004 确认后，系统自动预建工单、OQC、发货通知等下游单据，
--   单号统一为「2位业务前缀 + 订单的日期+流水」，如 gd2026112100004、zj2026112100004。
--   同一订单下同类单据多张时追加 -01/-02 子序号，因此需要放开单号校验。
--
-- 内容：
--   1) fn_doc_code_valid 增加可选的 "-两位子序号" 后缀
--   2) 工单产出批次规则 PCGD 前缀改为 pc（与业务口径 pc2026112100004 一致）
--
-- 幂等性：
--   * 函数 DROP 后重建；
--   * 前缀更新为定值，重复执行无副作用。
-- =============================================================================

SET NAMES utf8mb4;

DROP FUNCTION IF EXISTS fn_doc_code_valid;

DELIMITER $$

CREATE FUNCTION fn_doc_code_valid(p_code VARCHAR(64)) RETURNS tinyint
    DETERMINISTIC
BEGIN
  IF p_code IS NULL OR p_code = '' THEN
    RETURN 0;
  END IF;
  -- 标准单号：2位业务前缀 + yyyyMMdd + 5位流水；链号多张时允许 -01/-02 子序号
  IF p_code COLLATE utf8mb4_bin REGEXP '^[a-z]{2}[0-9]{13}(-[0-9]{2})?$' THEN
    RETURN 1;
  END IF;
  RETURN 0;
END$$

DELIMITER ;

-- 工单产出批次统一用 pc 前缀（采购来料 cb、外协 wb 保持各自业务前缀）
UPDATE sys_code_rule SET prefix = 'pc' WHERE rule_code = 'PCGD' AND prefix <> 'pc';

-- 验证
SELECT rule_code, prefix FROM sys_code_rule WHERE rule_code IN ('PCGD','PCCG','PCWS');
SELECT fn_doc_code_valid('gd2026112100004') AS base_ok,
       fn_doc_code_valid('zj2026112100004-01') AS sub_ok,
       fn_doc_code_valid('pc2026112100004') AS batch_ok;

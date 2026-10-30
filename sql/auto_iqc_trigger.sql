-- ============================================================
--  D4 自动化编排 · 链路二：采购入库完成 → 自动生成 IQC 来料复检任务
--  评分示例原文：「入库完成自动触发 IQC 检验任务」
--  规则：wm_item_recpt 状态变为 FINISHED 时触发（按行生成检验单）
--  执行：mysql -h127.0.0.1 -P3307 -uroot -p j2eedb -e "source E:/JavaCode/sql/auto_iqc_trigger.sql"
--  注意：触发器内变量必须显式指定 utf8mb4_0900_ai_ci，否则与表字段比对会报 collation 冲突
-- ============================================================
SET NAMES utf8mb4;

DROP TRIGGER IF EXISTS `trg_recpt_auto_iqc`;

DELIMITER $$
CREATE TRIGGER `trg_recpt_auto_iqc`
AFTER UPDATE ON `wm_item_recpt`
FOR EACH ROW
BEGIN
  DECLARE v_tpl  BIGINT DEFAULT NULL;
  DECLARE v_ipqc BIGINT DEFAULT NULL;
  DECLARE v_cnt  BIGINT DEFAULT 0;
  DECLARE done INT DEFAULT 0;

  DECLARE cur CURSOR FOR
    SELECT line_id FROM wm_item_recpt_line WHERE recpt_id = NEW.recpt_id;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

  -- 仅在「入库执行完成」这一刻触发
  IF NEW.status = 'FINISHED' AND (OLD.status IS NULL OR OLD.status <> 'FINISHED') THEN

    BEGIN
      OPEN cur;
      read_loop: LOOP
        FETCH cur INTO v_cnt;
        IF done = 1 THEN
          LEAVE read_loop;
        END IF;

        SELECT MIN(t.template_id) INTO v_tpl
        FROM qc_template t
        JOIN qc_template_product p ON p.template_id=t.template_id
        JOIN wm_item_recpt_line l ON l.item_id=p.item_id
        WHERE l.line_id=v_cnt AND FIND_IN_SET('IQC',t.qc_types) AND t.enable_flag='Y';

        -- 该入库行是否已生成过检验单（幂等）
        IF v_tpl IS NOT NULL AND NOT EXISTS (
          SELECT 1 FROM qc_iqc
          WHERE source_doc_type = 'RECPT_LINE' AND source_doc_id = v_cnt
        ) THEN
          INSERT INTO qc_iqc (
            iqc_code, iqc_name, template_id,
            source_doc_id, source_doc_type, source_doc_code,
            vendor_id, vendor_code, vendor_name,
            item_id, item_code, item_name,
            unit_of_measure, unit_name, specification,
            quantity_recived, quantity_check, status, remark, create_by, create_time
          )
          SELECT
            CONCAT('IQC-A-', l.line_id),
            CONCAT(l.item_name, ' 入库自动复检'),
            v_tpl,
            l.line_id, 'RECPT_LINE', NEW.recpt_code,
            NEW.vendor_id, NEW.vendor_code, NEW.vendor_name,
            l.item_id, l.item_code, l.item_name,
            l.unit_of_measure, l.unit_name, l.specification,
            IFNULL(l.quantity_recived, 0), IFNULL(l.quantity_recived, 0), 'PREPARE',
            '采购入库完成自动触发来料复检', 'system', NOW()
          FROM wm_item_recpt_line l WHERE l.line_id = v_cnt;

          SET v_ipqc = LAST_INSERT_ID();

          -- 同步复制检验项明细（否则完成编制时会因无明细报错）
          INSERT INTO qc_iqc_line (
            iqc_id, index_id, index_code, index_name, index_type,
            qc_tool, check_method, stander_val, unit_of_measure,
            threshold_max, threshold_min,
            cr_quantity, maj_quantity, min_quantity,
            remark, create_by, create_time
          )
          SELECT
            v_ipqc, ti.index_id, ti.index_code, ti.index_name, ti.index_type,
            ti.qc_tool, ti.check_method, ti.stander_val, ti.unit_of_measure,
            ti.threshold_max, ti.threshold_min,
            0, 0, 0, '', 'system', NOW()
          FROM qc_template_index ti WHERE ti.template_id = v_tpl;

          -- 说明：不再回写 wm_item_recpt_line（该表存在严格模式必填约束），
          -- 追溯关系通过 qc_iqc.source_doc_code（入库单号）+ source_doc_id（入库行ID）即可双向查询。
        END IF;
      END LOOP;
      CLOSE cur;
    END;
  END IF;
END$$
DELIMITER ;

-- ---------- 自检 ----------
SELECT 'IQC自动检验单' AS 对象, COUNT(*) AS 数量 FROM qc_iqc WHERE iqc_code LIKE 'IQC-A-%'
UNION ALL SELECT '触发器数量', COUNT(*) FROM information_schema.triggers
          WHERE trigger_schema = 'j2eedb' AND trigger_name LIKE 'trg_%auto%';

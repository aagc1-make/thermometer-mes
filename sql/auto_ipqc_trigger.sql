-- ============================================================
--  D4 自动化编排：报工合格 → 自动生成 IPQC 待检任务
--  规则来源：pro_route_process.is_check = 'Y' 的工序必须检验
--  演示链路：报工（新增 pro_feedback）→ 触发器 → qc_ipqc 出现待检记录
--  执行：mysql -h127.0.0.1 -P3307 -uroot -p j2eedb -e "source E:/JavaCode/sql/auto_ipqc_trigger.sql"
-- ============================================================
SET NAMES utf8mb4;

DROP TRIGGER IF EXISTS `trg_feedback_auto_ipqc`;

DELIMITER $$
CREATE TRIGGER `trg_feedback_auto_ipqc`
AFTER INSERT ON `pro_feedback`
FOR EACH ROW
BEGIN
  DECLARE v_check  CHAR(1) DEFAULT 'N';
  DECLARE v_tpl    BIGINT  DEFAULT NULL;
  DECLARE v_exists INT     DEFAULT 0;

  -- 只处理已完工/已完工报工
  IF NEW.status IN ('FINISHED', 'COMPLETED', 'CONFIRMED') THEN

    -- 该工艺路线上该工序是否需要检验
    SELECT IFNULL(MAX(is_check), 'N') INTO v_check
    FROM pro_route_process
    WHERE route_id = NEW.route_id
      AND process_name = NEW.process_name;

    IF v_check = 'Y' THEN
      -- 去重：同一报工只生成一次
      SELECT COUNT(1) INTO v_exists
      FROM qc_ipqc
      WHERE source_doc_type = 'FEEDBACK' AND source_doc_code = NEW.feedback_code;

      IF v_exists = 0 THEN
        -- 取一条支持 IPQC 的检验方案
        SELECT template_id INTO v_tpl
        FROM qc_template
        WHERE FIND_IN_SET('IPQC', qc_types)
        ORDER BY template_id
        LIMIT 1;

        INSERT INTO qc_ipqc (
          ipqc_code, ipqc_name, ipqc_type, template_id,
          source_doc_type, source_doc_code,
          workorder_id, workorder_code, workorder_name,
          task_id, task_code,
          workstation_id, workstation_code, workstation_name,
          process_id, process_code, process_name,
          item_id, item_code, item_name,
          unit_of_measure, unit_name, specification,
          quantity_check, status, remark,
          create_by, create_time
        ) VALUES (
          CONCAT('IPQC-A-', NEW.record_id), CONCAT(NEW.process_name, ' 自动待检'), 'IPQC', v_tpl,
          'FEEDBACK', NEW.feedback_code,
          NEW.workorder_id, NEW.workorder_code, NEW.workorder_name,
          NEW.task_id, NEW.task_code,
          NEW.workstation_id, NEW.workstation_code, NEW.workstation_name,
          NEW.process_id, NEW.process_code, NEW.process_name,
          NEW.item_id, NEW.item_code, NEW.item_name,
          NEW.unit_of_measure, NEW.unit_name, NEW.specification,
          NEW.quantity_qualified, 'PREPARE', '报工合格自动触发检验',
          'system', NOW()
        );
      END IF;
    END IF;
  END IF;
END$$
DELIMITER ;

-- ---------- 自检：触发前后对比 ----------
SELECT '当前待检(PREPARE)数量' AS 对象, COUNT(*) AS 数量 FROM qc_ipqc WHERE status = 'PREPARE'
UNION ALL SELECT '需要检验的工序数', COUNT(*) FROM pro_route_process WHERE is_check = 'Y'
UNION ALL SELECT '触发器是否存在', COUNT(*) FROM information_schema.triggers
          WHERE trigger_schema = 'j2eedb' AND trigger_name = 'trg_feedback_auto_ipqc';

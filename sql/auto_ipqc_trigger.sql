-- 现有自动 IPQC 修复：产出行存在后再建待检单，保持与检验完成接口的来源约定一致。
-- 部署到验收库前备份原触发器。仅替换触发器，不改写已有业务单据。
SET NAMES utf8mb4;
DROP TRIGGER IF EXISTS `trg_feedback_auto_ipqc`;
DELIMITER $$
CREATE TRIGGER `trg_feedback_auto_ipqc`
AFTER INSERT ON `wm_product_produce_line`
FOR EACH ROW
BEGIN
  DECLARE v_tpl BIGINT DEFAULT NULL;
  DECLARE v_ipqc BIGINT DEFAULT NULL;
  DECLARE v_feedback BIGINT DEFAULT NULL;
  DECLARE v_check CHAR(1) DEFAULT 'N';

  SELECT MAX(feedback_id) INTO v_feedback FROM wm_product_produce WHERE record_id = NEW.record_id;
  SELECT IFNULL(MAX(r.is_check), 'N') INTO v_check
    FROM pro_feedback f JOIN pro_route_process r ON r.route_id=f.route_id AND r.process_id=f.process_id
    WHERE f.record_id=v_feedback;

  IF NEW.quality_status = 'NT' AND v_check = 'Y' THEN
    SELECT MIN(t.template_id) INTO v_tpl
      FROM qc_template t JOIN qc_template_product p ON p.template_id=t.template_id
      WHERE p.item_id=NEW.item_id AND FIND_IN_SET('IPQC', t.qc_types) AND t.enable_flag='Y';
    IF v_tpl IS NULL THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IPQC template missing for product';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM qc_template_index WHERE template_id=v_tpl) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='IPQC template has no inspection items';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM qc_ipqc WHERE source_doc_type='FEEDBACK'
                   AND source_doc_id=NEW.record_id AND source_line_id=NEW.line_id) THEN
      INSERT INTO qc_ipqc (
        ipqc_code, ipqc_name, ipqc_type, template_id,
        source_doc_id, source_doc_type, source_doc_code, source_line_id,
        workorder_id, workorder_code, workorder_name, task_id, task_code,
        workstation_id, workstation_code, workstation_name, process_id, process_code, process_name,
        item_id, item_code, item_name, unit_of_measure, unit_name, specification,
        quantity_check, quantity_qualified, quantity_unqualified, status, remark, create_by, create_time
      )
      SELECT CONCAT('IPQC-A-',NEW.line_id), CONCAT(f.process_name,' 自动待检'), 'IPQC',v_tpl,
        NEW.record_id,'FEEDBACK',f.feedback_code,NEW.line_id,
        f.workorder_id,f.workorder_code,f.workorder_name,f.task_id,f.task_code,
        f.workstation_id,f.workstation_code,f.workstation_name,f.process_id,f.process_code,f.process_name,
        NEW.item_id,NEW.item_code,NEW.item_name,NEW.unit_of_measure,NEW.unit_name,NEW.specification,
        NEW.quantity_produce,0,0,'PREPARE','报工执行后自动生成待检任务','system',NOW()
      FROM pro_feedback f WHERE f.record_id=v_feedback;
      SET v_ipqc=LAST_INSERT_ID();
      INSERT INTO qc_ipqc_line (
        ipqc_id,index_id,index_code,index_name,index_type,qc_tool,check_method,stander_val,unit_of_measure,
        threshold_max,threshold_min,cr_quantity,maj_quantity,min_quantity,remark,attr3,attr4,create_by,create_time
      )
      SELECT v_ipqc,index_id,index_code,index_name,index_type,qc_tool,check_method,stander_val,unit_of_measure,
        threshold_max,threshold_min,0,0,0,'',0,0,'system',NOW()
      FROM qc_template_index WHERE template_id=v_tpl;
    END IF;
  END IF;
END$$
DELIMITER ;

SELECT trigger_name, event_object_table, action_timing, event_manipulation
FROM information_schema.triggers WHERE trigger_schema=DATABASE() AND trigger_name='trg_feedback_auto_ipqc';

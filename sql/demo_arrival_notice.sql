-- ============================================================
--  D4 自动化编排 · 链路二 演示数据（采购端：入库 → 自动 IQC）
--  设计要点：
--    1. 用「原材料」而不是半成品（半成品应内部生产，不该采购）
--    2. iqc_check='N'（免检放行）→ 到货通知可直接提交，符合"入库后自动复检"的业务叙事
--    3. 状态置为 APPROVED（待入库）→ 采购入库可直接从到货通知生成
--    4. 数量取整（10/20），与整机 10 台的 BOM 用量匹配
--  执行：mysql -h127.0.0.1 -P3307 -uroot -p j2eedb -e "source E:/JavaCode/sql/demo_arrival_notice.sql"
-- ============================================================
SET NAMES utf8mb4;

-- ---------- 清理旧演示数据（可重复执行） ----------
DELETE l FROM wm_arrival_notice_line l
JOIN wm_arrival_notice n ON n.notice_id = l.notice_id
WHERE n.notice_code LIKE 'DEMO-AN-%';
DELETE FROM wm_arrival_notice WHERE notice_code LIKE 'DEMO-AN-%';
DELETE l FROM qc_iqc_line l JOIN qc_iqc i ON i.iqc_id = l.iqc_id WHERE i.iqc_code LIKE 'IQC-A-%';
DELETE FROM qc_iqc WHERE iqc_code LIKE 'IQC-A-%';

-- ---------- 到货通知（待入库，可直接生成采购入库） ----------
INSERT INTO wm_arrival_notice
  (notice_code, notice_name, po_code, vendor_id, vendor_code, vendor_name, vendor_nick,
   arrival_date, contact, tel, status, remark, create_by, create_time)
VALUES
  ('DEMO-AN-001', '电子件与结构件到货', 'PO-DEMO-001', 9761, 'V-TG-001', '深圳市芯控电子有限公司', '芯控电子',
   CURDATE(), '张仓管', '13800000202', 'APPROVED', '演示：采购入库自动触发来料复检（免检放行）', 'admin', NOW());

SET @nid := LAST_INSERT_ID();

-- ---------- 到货行：2 种原材料，免检放行 ----------
INSERT INTO wm_arrival_notice_line
  (notice_id, item_id, item_code, item_name, specification, unit_of_measure, unit_name,
   quantity_arrival, quantity_quanlified, iqc_check, remark, create_by, create_time)
SELECT @nid, item_id, item_code, item_name, specification,
       IFNULL(unit_of_measure,'PCS'), IFNULL(unit_name,'个'),
       20, 20, 'N', '演示行', 'admin', NOW()
FROM md_item WHERE item_code = 'TG-EL-001' LIMIT 1;

INSERT INTO wm_arrival_notice_line
  (notice_id, item_id, item_code, item_name, specification, unit_of_measure, unit_name,
   quantity_arrival, quantity_quanlified, iqc_check, remark, create_by, create_time)
SELECT @nid, item_id, item_code, item_name, specification,
       IFNULL(unit_of_measure,'PCS'), IFNULL(unit_name,'个'),
       10, 10, 'N', '演示行', 'admin', NOW()
FROM md_item WHERE item_code = 'TG-ST-001' LIMIT 1;

-- ---------- 自检 ----------
SELECT n.notice_code, n.notice_name, n.status, n.vendor_name,
       l.item_code, l.item_name, l.quantity_arrival, l.iqc_check
FROM wm_arrival_notice n JOIN wm_arrival_notice_line l ON l.notice_id = n.notice_id
WHERE n.notice_code = 'DEMO-AN-001';

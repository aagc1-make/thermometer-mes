-- 销售出库行补齐工单/成品批次绑定，并让发货通知状态跟随出库单完成。
-- 背景：早期出库行未写入 workorder_id/batch_code，导致工单流水线“销售完成”统计不到已发货数量。

-- 1) 按订单的已入库记录回填出库行的工单与批次（同一订单多张工单时按已入库批次锁定）
UPDATE wm_product_sales_line l
JOIN wm_product_sales s ON s.sales_id = l.sales_id
JOIN (
    SELECT r.workorder_id, r.workorder_code, rl.item_id, rl.batch_id, rl.batch_code, o.source_doc_code
    FROM wm_product_recpt_line rl
    JOIN wm_product_recpt r ON r.recpt_id = rl.recpt_id
    JOIN pro_workorder o ON o.workorder_id = r.workorder_id
    WHERE r.status = 'FINISHED'
    ORDER BY rl.line_id
) b ON b.source_doc_code = s.so_code AND b.item_id = l.item_id
SET l.workorder_id = COALESCE(l.workorder_id, b.workorder_id),
    l.workorder_code = COALESCE(l.workorder_code, b.workorder_code),
    l.batch_id = COALESCE(l.batch_id, b.batch_id),
    l.batch_code = COALESCE(l.batch_code, b.batch_code)
WHERE l.workorder_id IS NULL OR l.batch_code IS NULL;

-- 2) 出库单已完成时，回写关联发货通知为已完成，避免“出库完成、通知仍待确认”
UPDATE wm_sales_notice n
JOIN wm_product_sales s ON s.notice_id = n.notice_id
SET n.status = 'FINISHED'
WHERE s.status = 'FINISHED' AND n.status <> 'FINISHED';

-- 3) 校验：允许结果为空，表示已无未绑定工单的已出库行
SELECT l.line_id, l.sales_id, s.sales_code, s.so_code, l.item_code, l.workorder_id, l.batch_code
FROM wm_product_sales_line l
JOIN wm_product_sales s ON s.sales_id = l.sales_id
WHERE s.status = 'FINISHED' AND (l.workorder_id IS NULL OR l.batch_code IS NULL);

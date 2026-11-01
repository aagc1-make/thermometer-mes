-- =============================================================
-- 回填：历史销售出库行按「产品 + 批次」挂到成品来源工单
-- 版本：patch_20261101_workorder_sales_backfill
-- 适用库：j2eedb（主库）、j2eedb_it（隔离验收库）
-- 前提：先执行 patch_20261101_workorder_sales_linkage.sql（新增 workorder_id/workorder_code）
-- 性质：只补写 workorder_id/workorder_code 两列，不改数量、不改状态、不生成单据
-- 说明：只处理"该产品 + 该批次"在已执行产品入库记录里唯一对应一张工单的行；
--       对应多张工单或没有入库记录的行保持为空，由业务在出库单里人工选择，不猜测。
-- =============================================================

-- 1. 预检查：待回填行数 / 可唯一确定行数
SELECT COUNT(*) AS pending_rows
FROM wm_product_sales_line
WHERE workorder_id IS NULL AND batch_code IS NOT NULL AND batch_code <> '';

SELECT COUNT(*) AS backfillable_rows
FROM wm_product_sales_line l
         JOIN (SELECT rl.item_id,
                      rl.batch_code,
                      COUNT(DISTINCT COALESCE(rl.workorder_id, rt.workorder_id)) AS wo_count
               FROM wm_product_recpt_line rl
                        INNER JOIN wm_product_recpt rt ON rt.recpt_id = rl.recpt_id
               WHERE rt.status = 'FINISHED'
                 AND COALESCE(rl.workorder_id, rt.workorder_id) IS NOT NULL
               GROUP BY rl.item_id, rl.batch_code) src
              ON src.item_id = l.item_id AND src.batch_code = l.batch_code AND src.wo_count = 1
WHERE l.workorder_id IS NULL;

-- 2. 回填（可重复执行：只写 workorder_id 为空的行）
UPDATE wm_product_sales_line l
    JOIN (SELECT rl.item_id,
                 rl.batch_code,
                 MIN(COALESCE(rl.workorder_id, rt.workorder_id)) AS workorder_id,
                 COUNT(DISTINCT COALESCE(rl.workorder_id, rt.workorder_id)) AS wo_count
          FROM wm_product_recpt_line rl
                   INNER JOIN wm_product_recpt rt ON rt.recpt_id = rl.recpt_id
          WHERE rt.status = 'FINISHED'
            AND COALESCE(rl.workorder_id, rt.workorder_id) IS NOT NULL
          GROUP BY rl.item_id, rl.batch_code) src
    ON src.item_id = l.item_id AND src.batch_code = l.batch_code AND src.wo_count = 1
SET l.workorder_id = src.workorder_id,
    l.workorder_code = (SELECT w.workorder_code FROM pro_workorder w WHERE w.workorder_id = src.workorder_id)
WHERE l.workorder_id IS NULL;

-- 3. 验收：工单维度「已入库 / 已开单 / 可售」
SELECT r.workorder_id,
       w.workorder_code,
       r.item_code,
       r.received_quantity,
       COALESCE(s.issued_quantity, 0) AS issued_quantity,
       r.received_quantity - COALESCE(s.issued_quantity, 0) AS available_quantity
FROM (SELECT COALESCE(rl.workorder_id, rt.workorder_id) AS workorder_id,
             rl.item_id AS item_id,
             MAX(rl.item_code) AS item_code,
             SUM(rl.quantity_recived) AS received_quantity
      FROM wm_product_recpt_line rl
               INNER JOIN wm_product_recpt rt ON rt.recpt_id = rl.recpt_id
      WHERE rt.status = 'FINISHED'
        AND COALESCE(rl.workorder_id, rt.workorder_id) IS NOT NULL
      GROUP BY COALESCE(rl.workorder_id, rt.workorder_id), rl.item_id) r
         INNER JOIN pro_workorder w ON w.workorder_id = r.workorder_id
         LEFT JOIN (SELECT sl.workorder_id AS workorder_id, sl.item_id AS item_id,
                           SUM(sl.quantity_sales) AS issued_quantity
                    FROM wm_product_sales_line sl
                             INNER JOIN wm_product_sales sa ON sa.sales_id = sl.sales_id
                    WHERE sl.workorder_id IS NOT NULL
                      AND (sa.status IS NULL OR sa.status <> 'CANCELED')
                    GROUP BY sl.workorder_id, sl.item_id) s
                   ON s.workorder_id = r.workorder_id AND s.item_id = r.item_id
ORDER BY w.workorder_code;

-- 4. 剩余无法自动关联的行（人工处理清单）
SELECT l.line_id, s.sales_code, s.status, l.item_code, l.batch_code, l.quantity_sales
FROM wm_product_sales_line l
         LEFT JOIN wm_product_sales s ON s.sales_id = l.sales_id
WHERE l.workorder_id IS NULL
  AND (s.status IS NULL OR s.status <> 'CANCELED');

-- =============================================================================
-- wm_transaction.biz_key 历史可判定子集回填（幂等，可重复执行）
-- 文件：sql/patch_20261114_biz_key_backfill.sql
-- 目标库：j2eedb（MySQL 8.4）
--
-- 依据：
--   doc/数据库设计/数值精度与单据取号_20261114.md 第 3 节
--   配合 patch_20261105_workorder_line_biz_key.sql（建立 uk_txn_biz_key）
--
-- 背景：
--   2026-11-05 引入 biz_key 之前写入的库存事务全部为 NULL，不参与幂等去重。
--   当时未回填的原因是 transaction_quantity 为 double，Java 端 BigDecimal 归一化
--   字符串与旧值无法保证逐字一致。20261114 已将所有 double 转为 DECIMAL，
--   DECIMAL 读出的字符串是确定的，因此可以按 Java 规则精确复现键值。
--
-- 回填规则（必须与 WmTransactionServiceImpl#buildBizKey 完全一致）：
--   <transaction_type>:<source_doc_type>:<source_doc_id>:<source_doc_line_id>:D<item>|<batch>|<wh>|<loc>|<area>|<qty>
--   * 空值在 Java 侧记为 '-'，数量去尾零（stripTrailingZeros().toPlainString()）
--   * 只回填「无明细ID」分支：source_doc_type 不是 IR / PRODUCT_PRODUCT
--     这两类 TxBean（ItemRecptTxBean、ProductProductTxBean）在过账时携带 detail_id，
--     键的最后一节是明细ID；明细ID 未落库、事后无法还原，因此保持 NULL 并输出清单。
--
-- 幂等性：
--   * 只处理 biz_key IS NULL 的行；回填后再次执行不会重复处理。
--   * 与既有非空 biz_key 冲突的行不写；候选内部同键多行的整组不写（疑似重复过账）。
--
-- 执行方式：
--   mysql --host=127.0.0.1 --port=3306 --user=root --password=*** \
--         --database=j2eedb --default-character-set=utf8mb4 \
--         -e "source E:/ktg/program/thermometer-mes-linkage/sql/patch_20261114_biz_key_backfill.sql"
-- =============================================================================

SET NAMES utf8mb4;

-- =============================================================================
-- Stage 0：回填前统计
-- =============================================================================
SELECT 'BEFORE' AS stage,
       COUNT(*)                                            AS total_txn,
       SUM(biz_key IS NULL)                                AS null_biz_key,
       SUM(biz_key IS NOT NULL)                            AS has_biz_key
FROM wm_transaction;

-- =============================================================================
-- Stage 1：生成候选（只含可判定分支）
-- =============================================================================
DROP TEMPORARY TABLE IF EXISTS ktg_biz_pending;

CREATE TEMPORARY TABLE ktg_biz_pending (
  transaction_id BIGINT PRIMARY KEY,
  txn_code       VARCHAR(64),
  biz_key        VARCHAR(255),
  key_conflict   TINYINT DEFAULT 0,   -- 与既有非空 biz_key 冲突
  key_duplicated TINYINT DEFAULT 0,   -- 候选内部同键多行
  KEY idx_key (biz_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO ktg_biz_pending (transaction_id, txn_code, biz_key)
SELECT t.transaction_id,
       t.txn_code,
       CONCAT(
         t.transaction_type, ':',
         COALESCE(CAST(t.source_doc_type AS CHAR), '-'), ':',
         COALESCE(CAST(t.source_doc_id AS CHAR), '-'), ':',
         t.source_doc_line_id, ':D',
         COALESCE(CAST(t.item_id AS CHAR), '-'), '|',
         COALESCE(CAST(t.batch_id AS CHAR), '-'), '|',
         COALESCE(CAST(t.warehouse_id AS CHAR), '-'), '|',
         COALESCE(CAST(t.location_id AS CHAR), '-'), '|',
         COALESCE(CAST(t.area_id AS CHAR), '-'), '|',
         CASE
           WHEN t.transaction_quantity IS NULL THEN '-'
           WHEN t.transaction_quantity = 0 THEN '0'
           ELSE TRIM(TRAILING '.' FROM TRIM(TRAILING '0' FROM CAST(t.transaction_quantity AS CHAR)))
         END
       )
FROM wm_transaction t
WHERE t.biz_key IS NULL
  AND t.transaction_type IS NOT NULL
  AND t.source_doc_line_id IS NOT NULL
  AND t.transaction_type NOT IN ('ITEM_RECPT', 'PRODUCT_PRODUCE');

-- 与既有非空键冲突：不写（说明历史上已有一条同业务事务）
UPDATE ktg_biz_pending p
JOIN wm_transaction e ON e.biz_key = p.biz_key
SET p.key_conflict = 1;

-- 候选内部同键多行：整组不写（疑似重复过账，交人工核对）
-- 临时表不能在同一个查询里被重复打开（ERROR 1137），先物化重复键。
DROP TEMPORARY TABLE IF EXISTS ktg_biz_dup;

CREATE TEMPORARY TABLE ktg_biz_dup (
  biz_key VARCHAR(255) PRIMARY KEY
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO ktg_biz_dup (biz_key)
SELECT biz_key FROM ktg_biz_pending GROUP BY biz_key HAVING COUNT(*) > 1;

UPDATE ktg_biz_pending p
JOIN ktg_biz_dup d ON d.biz_key = p.biz_key
SET p.key_duplicated = 1;

-- =============================================================================
-- Stage 2：回填
-- =============================================================================
UPDATE wm_transaction t
JOIN ktg_biz_pending p ON p.transaction_id = t.transaction_id
SET t.biz_key = p.biz_key
WHERE t.biz_key IS NULL
  AND p.key_conflict = 0
  AND p.key_duplicated = 0;

-- =============================================================================
-- Stage 3：结果与无法判定清单
-- =============================================================================

-- 3.1 本次回填条数
SELECT 'BACKFILLED' AS category,
       COUNT(*) AS rows_backfilled
FROM wm_transaction t
JOIN ktg_biz_pending p ON p.transaction_id = t.transaction_id
WHERE t.biz_key = p.biz_key;

-- 3.2 回填后仍为 NULL 的总量
SELECT 'REMAINING_NULL' AS category, COUNT(*) AS rows_null
FROM wm_transaction
WHERE biz_key IS NULL;

-- 3.3 清单 A：携带明细ID的类型，明细ID未落库，无法还原
SELECT 'A_DETAIL_ID_UNKNOWN' AS reason,
       transaction_id, txn_code, transaction_type, source_doc_type,
       source_doc_id, source_doc_line_id, item_id, transaction_quantity, create_time
FROM wm_transaction
WHERE biz_key IS NULL
  AND transaction_type IN ('ITEM_RECPT', 'PRODUCT_PRODUCE')
ORDER BY transaction_id;

-- 3.4 清单 B：缺少来源单据行，键无法成立（与 Java 侧返回 null 一致）
SELECT 'B_NO_SOURCE_LINE' AS reason,
       transaction_id, txn_code, transaction_type, source_doc_type,
       source_doc_id, source_doc_line_id, item_id, transaction_quantity, create_time
FROM wm_transaction
WHERE biz_key IS NULL
  AND (source_doc_line_id IS NULL OR transaction_type IS NULL)
ORDER BY transaction_id;

-- 3.5 清单 C：键与既有事务冲突（历史重复过账嫌疑）
SELECT 'C_KEY_CONFLICT' AS reason,
       t.transaction_id, t.txn_code, t.transaction_type, t.source_doc_type,
       t.source_doc_id, t.source_doc_line_id, p.biz_key AS computed_key, t.create_time
FROM ktg_biz_pending p
JOIN wm_transaction t ON t.transaction_id = p.transaction_id
WHERE p.key_conflict = 1
ORDER BY t.transaction_id;

-- 3.6 清单 D：候选内部同键多行（疑似重复过账，整组未回填）
SELECT 'D_DUPLICATE_KEY' AS reason,
       p.biz_key AS computed_key,
       t.transaction_id, t.txn_code, t.transaction_type,
       t.source_doc_type, t.source_doc_id, t.source_doc_line_id, t.create_time
FROM ktg_biz_pending p
JOIN wm_transaction t ON t.transaction_id = p.transaction_id
WHERE p.key_duplicated = 1
ORDER BY p.biz_key, t.transaction_id;

SELECT 'AFTER' AS stage,
       COUNT(*)             AS total_txn,
       SUM(biz_key IS NULL) AS null_biz_key,
       SUM(biz_key IS NOT NULL) AS has_biz_key
FROM wm_transaction;

DROP TEMPORARY TABLE IF EXISTS ktg_biz_pending;
DROP TEMPORARY TABLE IF EXISTS ktg_biz_dup;

SELECT 'SUMMARY: deterministic biz_key backfill done (idempotent)' AS summary;

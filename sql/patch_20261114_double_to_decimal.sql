-- =============================================================================
-- double / float 列统一转换为 DECIMAL（精度方案 A：原规模换算）
-- 文件：sql/patch_20261114_double_to_decimal.sql
-- 目标库：j2eedb（MySQL 8.4）
--
-- 依据：
--   doc/数据库设计/数值精度与单据取号_20261114.md
--   用户拍板方案 A：double(12,2) -> DECIMAL(12,2)、double(12,4) -> DECIMAL(12,4)、
--   double(14,2) -> DECIMAL(14,2)，整数位与小数位原样保留，业务语义与前端展示不变。
--
-- 内容：
--   1) 扫描 information_schema，找出所有基础表（不含视图）中的 double/float/real 列
--   2) 按表聚合成每条 ALTER TABLE 语句（同表多列合并，减少表重建次数）
--   3) 逐条执行，列定义保留 UNSIGNED / NOT NULL / DEFAULT / COMMENT
--   4) 校验：输出剩余 double 列数量与每张表的列数
--
-- 幂等性：
--   * 只选取 data_type IN ('double','float','real') 的列，已转换的 DECIMAL 列不会被再次选中，
--     第二次执行时队列为空，不会产生任何 DDL。
--   * 视图 v_stock_alert 不参与转换：它的列类型随基表自动派生。
--
-- 风险与处理：
--   * double(M,D) 的取值本就受 M/D 约束，转换为同规模 DECIMAL 不会截断或溢出；
--     若未来在含异常值的库上执行，严格模式会直接报错而不是静默截断。
--   * 执行前建议做 schema-only 备份（见 sql/README_next.md 第 2 节）。
--
-- 执行方式：
--   mysql --host=127.0.0.1 --port=3306 --user=root --password=*** \
--         --database=j2eedb --default-character-set=utf8mb4 \
--         -e "source E:/ktg/program/thermometer-mes-linkage/sql/patch_20261114_double_to_decimal.sql"
-- =============================================================================

SET NAMES utf8mb4;
SET SESSION group_concat_max_len = 1048576;

-- =============================================================================
-- Stage 0：转换前统计
-- =============================================================================
SELECT 'BEFORE' AS stage,
       COUNT(*) AS double_columns,
       COUNT(DISTINCT c.table_name) AS tables
FROM information_schema.columns c
JOIN information_schema.tables t
  ON t.table_schema = c.table_schema AND t.table_name = c.table_name
WHERE c.table_schema = DATABASE()
  AND t.table_type = 'BASE TABLE'
  AND c.data_type IN ('double','float','real');

-- =============================================================================
-- Stage 1：生成 DDL 队列
--   同表多列合并为一条 ALTER TABLE，列定义保留原属性。
-- =============================================================================
DROP TEMPORARY TABLE IF EXISTS ktg_decimal_ddl;

CREATE TEMPORARY TABLE ktg_decimal_ddl (
  seq        INT AUTO_INCREMENT PRIMARY KEY,
  table_name VARCHAR(64) NOT NULL,
  ddl        TEXT NOT NULL
);

INSERT INTO ktg_decimal_ddl (table_name, ddl)
SELECT x.table_name,
       CONCAT('ALTER TABLE `', x.table_name, '` ',
              GROUP_CONCAT(x.modify_clause ORDER BY x.ordinal_position SEPARATOR ', '))
FROM (
  SELECT c.table_name,
         c.ordinal_position,
         CONCAT('MODIFY COLUMN `', c.column_name, '` DECIMAL(', c.numeric_precision, ',', c.numeric_scale, ')',
                IF(c.column_type LIKE '%unsigned%', ' UNSIGNED', ''),
                IF(c.is_nullable = 'NO', ' NOT NULL', ' NULL'),
                IF(c.column_default IS NULL, '', CONCAT(' DEFAULT ', QUOTE(c.column_default))),
                IF(c.column_comment = '', '', CONCAT(' COMMENT ', QUOTE(c.column_comment)))) AS modify_clause
  FROM information_schema.columns c
  JOIN information_schema.tables t
    ON t.table_schema = c.table_schema AND t.table_name = c.table_name
  WHERE c.table_schema = DATABASE()
    AND t.table_type = 'BASE TABLE'
    AND c.data_type IN ('double','float','real')
) x
GROUP BY x.table_name
ORDER BY x.table_name;

SELECT COUNT(*) AS ddl_batch_count FROM ktg_decimal_ddl;

-- =============================================================================
-- Stage 2：逐条执行
-- =============================================================================
DROP PROCEDURE IF EXISTS ktg2_apply_decimal;

DELIMITER $$

CREATE PROCEDURE ktg2_apply_decimal()
BEGIN
  DECLARE v_done INT DEFAULT 0;
  DECLARE v_ddl  TEXT;
  DECLARE cur CURSOR FOR SELECT ddl FROM ktg_decimal_ddl ORDER BY seq;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO v_ddl;
    IF v_done = 1 THEN
      LEAVE read_loop;
    END IF;
    SET @ktg_ddl = v_ddl;
    PREPARE ktg_stmt FROM @ktg_ddl;
    EXECUTE ktg_stmt;
    DEALLOCATE PREPARE ktg_stmt;
  END LOOP;
  CLOSE cur;
END$$

DELIMITER ;

CALL ktg2_apply_decimal();

DROP PROCEDURE IF EXISTS ktg2_apply_decimal;
DROP TEMPORARY TABLE IF EXISTS ktg_decimal_ddl;

-- =============================================================================
-- Stage 3：校验
-- =============================================================================
SELECT 'AFTER' AS stage,
       COUNT(*) AS remaining_double_columns
FROM information_schema.columns c
JOIN information_schema.tables t
  ON t.table_schema = c.table_schema AND t.table_name = c.table_name
WHERE c.table_schema = DATABASE()
  AND t.table_type = 'BASE TABLE'
  AND c.data_type IN ('double','float','real');

-- 抽样确认精度与列属性
SELECT table_name, column_name, column_type, is_nullable, column_default, column_comment
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND ((table_name = 'wm_transaction'   AND column_name = 'transaction_quantity')
    OR (table_name = 'wm_material_stock' AND column_name = 'quantity_onhand')
    OR (table_name = 'pro_feedback'     AND column_name = 'quantity_qualified')
    OR (table_name = 'md_item'          AND column_name IN ('min_stock','max_stock'))
    OR (table_name = 'wm_package'       AND column_name = 'net_weight')
    OR (table_name = 'sys_attachment'   AND column_name = 'file_size')
    OR (table_name = 'wm_storage_area'  AND column_name = 'area'))
ORDER BY table_name, column_name;

SELECT 'SUMMARY: double/float -> DECIMAL by original precision (idempotent)' AS summary;

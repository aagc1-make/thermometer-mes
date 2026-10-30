-- =============================================================
-- 产品入库联动（工单完工 → 产品入库草稿）增量约束
-- 版本：patch_20261028_product_receipt_linkage
-- 适用库：j2eedb（主库）、j2eedb_it（隔离验收库）
-- 性质：只新增生成列 + 唯一键，不修改/删除任何业务数据
-- 执行前：先跑第 0 节检查；有输出先人工处理
-- 回滚：patch_20261028_product_receipt_linkage_rollback.sql
-- =============================================================

-- 0. 前置检查（应全部返回 0 行）
-- 0.1 自动生成的产品入库单是否已重复（同一工单多张自动草稿）
select attr1, count(*) as duplicate_count
from wm_product_recpt
where attr1 like 'AUTO:PRODUCT_RECPT:%'
group by attr1
having count(*) > 1;

-- 0.2 自动生成的产品入库行是否已重复（同一工单多行）
select attr1, count(*) as duplicate_count
from wm_product_recpt_line
where attr1 like 'AUTO:PRODUCT_RECPT_LINE:%'
group by attr1
having count(*) > 1;

-- 0.3 attr1 超长检查（生成列 191）
select count(*) as too_long from wm_product_recpt where char_length(attr1) > 191
union all
select count(*) from wm_product_recpt_line where char_length(attr1) > 191;

-- 1. 增量变更（可重复执行）
drop procedure if exists apply_patch_20261028_product_receipt;

delimiter $$
create procedure apply_patch_20261028_product_receipt()
begin
    if not exists (select 1 from information_schema.columns
                   where table_schema = database() and table_name = 'wm_product_recpt'
                     and column_name = 'linkage_key') then
        alter table wm_product_recpt
            add column linkage_key varchar(191)
                generated always as (case when attr1 like 'AUTO:%' then attr1 else null end) stored;
    end if;
    if not exists (select 1 from information_schema.statistics
                   where table_schema = database() and table_name = 'wm_product_recpt'
                     and index_name = 'uk_wm_product_recpt_linkage_key') then
        alter table wm_product_recpt
            add unique key uk_wm_product_recpt_linkage_key (linkage_key);
    end if;

    if not exists (select 1 from information_schema.columns
                   where table_schema = database() and table_name = 'wm_product_recpt_line'
                     and column_name = 'linkage_key') then
        alter table wm_product_recpt_line
            add column linkage_key varchar(191)
                generated always as (case when attr1 like 'AUTO:%' then attr1 else null end) stored;
    end if;
    if not exists (select 1 from information_schema.statistics
                   where table_schema = database() and table_name = 'wm_product_recpt_line'
                     and index_name = 'uk_wm_product_recpt_line_linkage_key') then
        alter table wm_product_recpt_line
            add unique key uk_wm_product_recpt_line_linkage_key (linkage_key);
    end if;
end$$
delimiter ;

call apply_patch_20261028_product_receipt();
drop procedure apply_patch_20261028_product_receipt;

-- 2. 验收查询
select table_name, index_name, group_concat(column_name order by seq_in_index) as columns
from information_schema.statistics
where table_schema = database()
  and index_name in ('uk_wm_product_recpt_linkage_key', 'uk_wm_product_recpt_line_linkage_key')
group by table_name, index_name;

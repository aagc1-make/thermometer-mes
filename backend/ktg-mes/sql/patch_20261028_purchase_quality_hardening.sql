-- =============================================================
-- 采购质量链 / 批量工单 增量约束脚本
-- 版本：patch_20261028_purchase_quality_hardening
-- 适用库：j2eedb（主库）、j2eedb_it（隔离验收库）
-- 性质：只新增「生成列 + 唯一约束 + 查询索引」，不修改、不删除任何业务数据，
--       不重跑初始化脚本，不触碰历史单据的业务快照。
-- 执行顺序：先跑第 0 节前置检查 → 再执行第 1 节存储过程 → 用第 2 节验收查询核对。
-- 回滚脚本：patch_20261028_purchase_quality_hardening_rollback.sql
-- =============================================================

-- -------------------------------------------------------------
-- 0. 前置检查（每条都应返回 0 行；有输出时先人工处理，禁止直接加唯一约束）
-- -------------------------------------------------------------

-- 0.1 自动生成的采购入库明细是否已存在重复（同一来源到货行生成多条明细）
select attr1, count(*) as duplicate_count
from wm_item_recpt_line
where attr1 like 'AUTO:ITEM_RECPT_LINE:%'
group by attr1
having count(*) > 1;

-- 0.2 自动生成的领料单是否已存在重复（同一工单多张领料草稿）
select attr1, count(*) as duplicate_count
from wm_issue_header
where attr1 like 'AUTO:ISSUE:%'
group by attr1
having count(*) > 1;

-- 0.3 自动生成的半成品子工单是否已存在重复（同一父工单 + 同一 BOM 行）
select attr1, count(*) as duplicate_count
from pro_workorder
where attr1 like 'AUTO:SUB_WORKORDER:%'
group by attr1
having count(*) > 1;

-- 0.4 历史数据里 attr1 以 AUTO: 开头但列内容可能超过生成列长度的情况
select count(*) as too_long
from wm_item_recpt_line where char_length(attr1) > 191
union all
select count(*) from wm_issue_header where char_length(attr1) > 191
union all
select count(*) from pro_workorder where char_length(attr1) > 191;

-- -------------------------------------------------------------
-- 1. 增量变更（可重复执行：存在则跳过）
-- -------------------------------------------------------------
drop procedure if exists apply_patch_20261028_purchase_quality;

delimiter $$
create procedure apply_patch_20261028_purchase_quality()
begin
    -- 1.1 采购入库明细：按来源到货行防重（仅 AUTO: 前缀的自动明细参与约束）
    if not exists (select 1 from information_schema.columns
                   where table_schema = database() and table_name = 'wm_item_recpt_line'
                     and column_name = 'linkage_key') then
        alter table wm_item_recpt_line
            add column linkage_key varchar(191)
                generated always as (case when attr1 like 'AUTO:%' then attr1 else null end) stored;
    end if;
    if not exists (select 1 from information_schema.statistics
                   where table_schema = database() and table_name = 'wm_item_recpt_line'
                     and index_name = 'uk_wm_item_recpt_line_linkage_key') then
        alter table wm_item_recpt_line
            add unique key uk_wm_item_recpt_line_linkage_key (linkage_key);
    end if;
    if not exists (select 1 from information_schema.statistics
                   where table_schema = database() and table_name = 'wm_item_recpt_line'
                     and index_name = 'idx_wm_item_recpt_line_notice_line') then
        alter table wm_item_recpt_line
            add key idx_wm_item_recpt_line_notice_line (notice_line_id);
    end if;

    -- 1.2 生产领料单：同一工单只生成一张自动领料草稿
    if not exists (select 1 from information_schema.columns
                   where table_schema = database() and table_name = 'wm_issue_header'
                     and column_name = 'linkage_key') then
        alter table wm_issue_header
            add column linkage_key varchar(191)
                generated always as (case when attr1 like 'AUTO:%' then attr1 else null end) stored;
    end if;
    if not exists (select 1 from information_schema.statistics
                   where table_schema = database() and table_name = 'wm_issue_header'
                     and index_name = 'uk_wm_issue_header_linkage_key') then
        alter table wm_issue_header
            add unique key uk_wm_issue_header_linkage_key (linkage_key);
    end if;

    -- 1.3 生产工单：同一父工单 + 同一来源 BOM 行只生成一张半成品子工单
    if not exists (select 1 from information_schema.columns
                   where table_schema = database() and table_name = 'pro_workorder'
                     and column_name = 'linkage_key') then
        alter table pro_workorder
            add column linkage_key varchar(191)
                generated always as (case when attr1 like 'AUTO:%' then attr1 else null end) stored;
    end if;
    if not exists (select 1 from information_schema.statistics
                   where table_schema = database() and table_name = 'pro_workorder'
                     and index_name = 'uk_pro_workorder_linkage_key') then
        alter table pro_workorder
            add unique key uk_pro_workorder_linkage_key (linkage_key);
    end if;

    -- 1.4 来料检验：按来源行累计检测量/合格量的查询索引
    if not exists (select 1 from information_schema.statistics
                   where table_schema = database() and table_name = 'qc_iqc'
                     and index_name = 'idx_qc_iqc_source_line') then
        alter table qc_iqc
            add key idx_qc_iqc_source_line (source_doc_type, source_line_id);
    end if;
end$$
delimiter ;

call apply_patch_20261028_purchase_quality();
drop procedure apply_patch_20261028_purchase_quality;

-- -------------------------------------------------------------
-- 2. 验收查询（结构核对，均应有输出）
-- -------------------------------------------------------------
select table_name, column_name
from information_schema.columns
where table_schema = database() and column_name = 'linkage_key'
  and table_name in ('wm_item_recpt_line', 'wm_issue_header', 'pro_workorder')
order by table_name;

select table_name, index_name, group_concat(column_name order by seq_in_index) as columns
from information_schema.statistics
where table_schema = database()
  and index_name in ('uk_wm_item_recpt_line_linkage_key',
                     'idx_wm_item_recpt_line_notice_line',
                     'uk_wm_issue_header_linkage_key',
                     'uk_pro_workorder_linkage_key',
                     'idx_qc_iqc_source_line')
group by table_name, index_name
order by table_name, index_name;

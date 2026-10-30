-- =============================================================
-- 回滚脚本：patch_20261028_purchase_quality_hardening
-- 只撤销本补丁新增的生成列、唯一约束与查询索引；不触碰任何业务数据。
-- 顺序：先删索引，再删列。
-- =============================================================

drop procedure if exists rollback_patch_20261028_purchase_quality;

delimiter $$
create procedure rollback_patch_20261028_purchase_quality()
begin
    if exists (select 1 from information_schema.statistics
               where table_schema = database() and table_name = 'wm_item_recpt_line'
                 and index_name = 'uk_wm_item_recpt_line_linkage_key') then
        alter table wm_item_recpt_line drop index uk_wm_item_recpt_line_linkage_key;
    end if;
    if exists (select 1 from information_schema.statistics
               where table_schema = database() and table_name = 'wm_item_recpt_line'
                 and index_name = 'idx_wm_item_recpt_line_notice_line') then
        alter table wm_item_recpt_line drop index idx_wm_item_recpt_line_notice_line;
    end if;
    if exists (select 1 from information_schema.columns
               where table_schema = database() and table_name = 'wm_item_recpt_line'
                 and column_name = 'linkage_key') then
        alter table wm_item_recpt_line drop column linkage_key;
    end if;

    if exists (select 1 from information_schema.statistics
               where table_schema = database() and table_name = 'wm_issue_header'
                 and index_name = 'uk_wm_issue_header_linkage_key') then
        alter table wm_issue_header drop index uk_wm_issue_header_linkage_key;
    end if;
    if exists (select 1 from information_schema.columns
               where table_schema = database() and table_name = 'wm_issue_header'
                 and column_name = 'linkage_key') then
        alter table wm_issue_header drop column linkage_key;
    end if;

    if exists (select 1 from information_schema.statistics
               where table_schema = database() and table_name = 'pro_workorder'
                 and index_name = 'uk_pro_workorder_linkage_key') then
        alter table pro_workorder drop index uk_pro_workorder_linkage_key;
    end if;
    if exists (select 1 from information_schema.columns
               where table_schema = database() and table_name = 'pro_workorder'
                 and column_name = 'linkage_key') then
        alter table pro_workorder drop column linkage_key;
    end if;

    if exists (select 1 from information_schema.statistics
               where table_schema = database() and table_name = 'qc_iqc'
                 and index_name = 'idx_qc_iqc_source_line') then
        alter table qc_iqc drop index idx_qc_iqc_source_line;
    end if;
end$$
delimiter ;

call rollback_patch_20261028_purchase_quality();
drop procedure rollback_patch_20261028_purchase_quality;

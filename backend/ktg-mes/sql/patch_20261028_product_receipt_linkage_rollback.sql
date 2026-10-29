-- 回滚：patch_20261028_product_receipt_linkage
-- 只撤销本补丁新增的生成列与唯一键，不触碰业务数据。

drop procedure if exists rollback_patch_20261028_product_receipt;

delimiter $$
create procedure rollback_patch_20261028_product_receipt()
begin
    if exists (select 1 from information_schema.statistics
               where table_schema = database() and table_name = 'wm_product_recpt'
                 and index_name = 'uk_wm_product_recpt_linkage_key') then
        alter table wm_product_recpt drop index uk_wm_product_recpt_linkage_key;
    end if;
    if exists (select 1 from information_schema.columns
               where table_schema = database() and table_name = 'wm_product_recpt'
                 and column_name = 'linkage_key') then
        alter table wm_product_recpt drop column linkage_key;
    end if;

    if exists (select 1 from information_schema.statistics
               where table_schema = database() and table_name = 'wm_product_recpt_line'
                 and index_name = 'uk_wm_product_recpt_line_linkage_key') then
        alter table wm_product_recpt_line drop index uk_wm_product_recpt_line_linkage_key;
    end if;
    if exists (select 1 from information_schema.columns
               where table_schema = database() and table_name = 'wm_product_recpt_line'
                 and column_name = 'linkage_key') then
        alter table wm_product_recpt_line drop column linkage_key;
    end if;
end$$
delimiter ;

call rollback_patch_20261028_product_receipt();
drop procedure rollback_patch_20261028_product_receipt;

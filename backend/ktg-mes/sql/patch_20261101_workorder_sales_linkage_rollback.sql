-- =============================================================
-- 回滚：patch_20261101_workorder_sales_linkage
-- 说明：仅回滚本轮新增的销售出库行工单关联列与索引。
--       若有业务已写入 workorder_id/workorder_code，回滚会一并丢失这些关联，
--       请先备份 select * from wm_product_sales_line where workorder_id is not null;
-- 目标表：wm_product_sales_line（实际在用表）
-- =============================================================

drop procedure if exists rollback_patch_20261101_workorder_sales;

delimiter $$
create procedure rollback_patch_20261101_workorder_sales()
begin
    if exists (select 1 from information_schema.statistics
               where table_schema = database() and table_name = 'wm_product_sales_line'
                 and index_name = 'idx_wm_product_sales_line_workorder') then
        alter table wm_product_sales_line drop index idx_wm_product_sales_line_workorder;
    end if;

    if exists (select 1 from information_schema.columns
               where table_schema = database() and table_name = 'wm_product_sales_line'
                 and column_name = 'workorder_code') then
        alter table wm_product_sales_line drop column workorder_code;
    end if;

    if exists (select 1 from information_schema.columns
               where table_schema = database() and table_name = 'wm_product_sales_line'
                 and column_name = 'workorder_id') then
        alter table wm_product_sales_line drop column workorder_id;
    end if;
end$$
delimiter ;

call rollback_patch_20261101_workorder_sales();
drop procedure rollback_patch_20261101_workorder_sales;

select count(*) as remaining_workorder_columns
from information_schema.columns
where table_schema = database()
  and table_name = 'wm_product_sales_line'
  and column_name in ('workorder_id', 'workorder_code');

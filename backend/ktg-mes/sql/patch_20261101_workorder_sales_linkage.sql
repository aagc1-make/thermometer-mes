-- =============================================================
-- 工单生命周期延伸到销售完成（销售出库行挂工单）
-- 版本：patch_20261101_workorder_sales_linkage
-- 适用库：j2eedb（主库）、j2eedb_it（隔离验收库）
-- 性质：只新增两列 + 一个普通索引，不修改/删除任何业务数据
-- 目标表：wm_product_sales_line（实际在用表；wm_product_salse_* 为历史空表，不处理）
-- 执行前：先跑第 0 节检查；有输出先人工处理
-- 回滚：patch_20261101_workorder_sales_linkage_rollback.sql
-- =============================================================

-- 0. 前置检查（应全部返回 0）
-- 0.1 目标表是否存在
select count(*) as missing_table
from information_schema.tables
where table_schema = database()
  and table_name = 'wm_product_sales_line'
having count(*) = 0;

-- 0.2 若已有人工加过的同名列，先确认类型一致（应返回 2 行 bigint / varchar）
select column_name, data_type, character_maximum_length
from information_schema.columns
where table_schema = database()
  and table_name = 'wm_product_sales_line'
  and column_name in ('workorder_id', 'workorder_code');

-- 1. 增量变更（可重复执行）
drop procedure if exists apply_patch_20261101_workorder_sales;

delimiter $$
create procedure apply_patch_20261101_workorder_sales()
begin
    if not exists (select 1 from information_schema.columns
                   where table_schema = database() and table_name = 'wm_product_sales_line'
                     and column_name = 'workorder_id') then
        alter table wm_product_sales_line
            add column workorder_id bigint null comment '生产工单ID（成品来源工单）';
    end if;

    if not exists (select 1 from information_schema.columns
                   where table_schema = database() and table_name = 'wm_product_sales_line'
                     and column_name = 'workorder_code') then
        alter table wm_product_sales_line
            add column workorder_code varchar(64) null comment '生产工单编号（成品来源工单）';
    end if;

    if not exists (select 1 from information_schema.statistics
                   where table_schema = database() and table_name = 'wm_product_sales_line'
                     and index_name = 'idx_wm_product_sales_line_workorder') then
        alter table wm_product_sales_line
            add index idx_wm_product_sales_line_workorder (workorder_id);
    end if;
end$$
delimiter ;

call apply_patch_20261101_workorder_sales();
drop procedure apply_patch_20261101_workorder_sales;

-- 2. 验收查询（应返回 workorder_id / workorder_code 两列与索引各一行）
select column_name, data_type, character_maximum_length, column_comment
from information_schema.columns
where table_schema = database()
  and table_name = 'wm_product_sales_line'
  and column_name in ('workorder_id', 'workorder_code')
order by column_name;

select index_name, group_concat(column_name order by seq_in_index) as columns
from information_schema.statistics
where table_schema = database()
  and table_name = 'wm_product_sales_line'
  and index_name = 'idx_wm_product_sales_line_workorder'
group by index_name;

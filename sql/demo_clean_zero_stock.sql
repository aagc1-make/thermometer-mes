-- 演示环境复位：虚拟线边仓是技术过账仓，历史多次演示会累积负数行，
-- 报工消耗遍历库存行时遇到负数/零结存行会抛“事务数量必须大于0”。
-- 演示前执行本脚本，让每次演示都从干净线边仓开始（正式环境请勿随意执行）。
DELETE FROM wm_material_stock WHERE warehouse_code = 'XBK_VIRTUAL';
DELETE FROM wm_material_stock WHERE quantity_onhand = 0;

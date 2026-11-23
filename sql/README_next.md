# 下一步数据库脚本执行说明

本目录的增量脚本按顺序执行。所有脚本**只新增**（建表 / 加列 / 加索引 / 加约束），
不删除、不重命名任何已有表、列、索引，可重复执行（幂等）。

## 1. 执行顺序

| 顺序 | 文件 | 作用 | 是否幂等 |
|---|---|---|---|
| 1 | `sql/patch_20261102_workorder_chain_v2.sql` | 工单主链 v2：编码规则表 + 取号器 + 工单行/工序/派工 + 批次改造 + 入库质检三表 + 库存余额/事务改造 + 外键与索引 + 状态 CHECK | 是 |
| 2 | `sql/patch_20261102_code_rules_ext.sql` | 补种 IQC / TXN 两条编码规则 | 是 |
| 3 | `sql/patch_20261105_workorder_line_biz_key.sql` | 工单行回填 + `wm_transaction.biz_key` 唯一索引 + 批次复合外键 | 是 |
| 4 | `sql/patch_20261114_code_sequence_docs.sql` | 20 类单据编码规则 + 单据号唯一索引 | 是 |
| 5 | `sql/patch_20261114_double_to_decimal.sql` | 基础表 154 个 `double/float` 列按原规模转 `DECIMAL` | 是 |
| 6 | `sql/patch_20261114_biz_key_backfill.sql` | `wm_transaction.biz_key` 可判定子集回填 + 无法判定清单 | 是 |
| 7 | `sql/patch_20261121_product_stocking.sql` | 库区/库位容量 `max_qty` + 混放策略 `mes.wm.mixing.enforce` + 产品入库状态字典 | 是 |
| 8 | `sql/patch_20261121_workorder_auto_schedule.sql` | 工单确认自动排产：`pro_task` `(workorder_id, process_id)` 唯一索引 + 任务号/报工号复合格式校验函数与触发器 | 是 |
| 9 | `sql/patch_20261121_feedback_code_short.sql` | 报工号缩短为「工单号-bg三位序号」：重建报工号校验函数与触发器 + 历史长单号及 `qc_ipqc.source_doc_code` 同步迁移 | 是 |
| 10 | `sql/patch_20261121_chain_code_alignment.sql` | 链号对齐：`fn_doc_code_valid` 放开 `-NN` 子序号；工单产出批次规则 `PCGD` 前缀改为 `pc` | 是 |
| 11 | `sql/patch_20261121_batch_workorder_binding.sql` | 成品批次按工单绑定：打开 `md_item_batch_config.workorder_flag`，避免同日多工单共用一个成品批次 | 是 |
| 12 | `sql/patch_20261122_sales_line_workorder_binding.sql` | 销售出库行回填工单/成品批次绑定 + 出库完成后回写发货通知状态，修正流水线销售完成统计 | 是 |
| 13 | `sql/patch_20261123_miniapp_alignment.sql` | 小程序与 MES 对齐：新增 `miniapp_product` 订货参数 + 三家客户/账号/地址/联系人/FAQ 演示数据（账号 c-tg-001~003，密码 123456） | 是 |
| 14 | `sql/patch_20261123_worker_roles.sql` | 工人端角色：补车间主任/采购员角色与账号（workshop/purchaser），对齐操作工/质检/仓管权限，并补流水线只读权限 | 是 |

> 后续阶段（应用代码改造、双写切换、旧表只读）的脚本尚未产出，届时按 `patch_<日期>_<主题>.sql` 命名追加。

## 2. 执行命令

```powershell
$env:KTG_DB_PASSWORD = "<密码>"
& "E:\mysql8.4\bin\mysql.exe" --host=127.0.0.1 --port=3306 --user=root `
  --password=$env:KTG_DB_PASSWORD --database=j2eedb --default-character-set=utf8mb4 `
  -e "source E:/ktg/program/thermometer-mes-linkage/sql/patch_20261102_workorder_chain_v2.sql"
```

执行前请先做 schema-only 备份：

```powershell
& "E:\mysql8.4\bin\mysqldump.exe" --host=127.0.0.1 --port=3306 --user=root `
  --password=$env:KTG_DB_PASSWORD --no-data --routines --triggers --events `
  --databases j2eedb --result-file="E:\ktg\program\thermometer-mes-linkage\backup\schema_only_j2eedb_<时间戳>.sql"
```

## 3. 执行后必须校验

1. 基础表数量：195 → **203**（新增 8 张）。
2. 外键数量：`information_schema.referential_constraints` 应新增 **20** 条（19 条主链 + 1 条取号器→规则）。
3. 业务单号唯一索引：`workorder_code`、`wm_batch.batch_code`、`wm_item_recpt.recpt_code`、`qc_iqc.iqc_code`、`qc_inbound.inspection_code` 均存在 UNIQUE。
4. CHECK 约束：插入非法状态应被拒绝。
5. 原有 195 张表仍在（`SHOW TABLES` 数量不减少）。

## 4. 已知限制

- 脚本**不创建触发器**：单号由应用层原子取号（`sys_code_sequence` 的
  `INSERT ... ON DUPLICATE KEY UPDATE + LAST_INSERT_ID()`），业务表 UNIQUE 兜底。
- 应用代码当前仍可能写入旧格式单号、`batch_code` 维度库存、无 `txn_code` 的事务；
  这些写入在新约束下**不会报错**（新列均可空/有默认值），但主链关联（`batch_id` 外键）
  需要应用改造后才真正生效。
- `wm_material_stock.stock_key` 唯一索引会阻止"同维度重复库存行"。现有
  `loadMaterialStockForUpdate` 的 `limit 1` 逻辑在并发下可能触发
  `DuplicateKeyException`，应用侧需要捕获后重查，属于后续改造项。

## 5. 回滚

本脚本不生成回滚脚本。由于全部为新增对象，回滚方式是：

1. 先 DROP 新增的 8 张表（注意外键依赖顺序）；
2. 再 DROP 新增列/索引/约束（需逐项核对名称）。

建议在确认应用改造完成前**不要回滚**，仅保留备份即可。

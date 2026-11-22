# 实验一 TestLink 安装与实际执行

2026-11-07 已完成部署、教材格式系统测试计划、10 条需求、28 条用例和实际执行。最终 **26 通过、2 失败、0 阻塞、0 未执行**，通过率92.86%；28 条首测加2条原版本复现，TestLink保存30条历史。两条失败合并为工单数量校验缺陷 LAB1-D001，未修复，不满足计划通过标准。

## 查看结果

打开 <http://127.0.0.1:8085>，账号凭据在仓库外 `E:\dev\testlink-stack\credentials.local.json`。选择“温衡智造 MES 实验一”→“LAB1 到货至销售出库 单轮测试”→`main-909b875-R1`。MES-10、MES-25保留首测和复现失败，展开备注可读实际输入、响应及缺陷编号。

`执行记录.md` 包含覆盖、清单、统计和缺陷台账。原始证据在 `evidence/lab1/run-20261107`：断言结果 `results.json`、349次脱敏HTTP请求的 `requests.json`、实际执行脚本、环境及构建日志、SHA-256清单。Word直接更新在 `E:\JavaCode\软件测试与与质量保证个人报告模版.docx`，实验一对应课程上机2“软件测试管理”，十项计划参照所给教材第4.6.2节和附录B。

## 已安装环境与启停

```powershell
& "$env:WINDIR\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File E:\JavaCode\scripts\lab1\start-testlink.ps1
```

停止时将脚本名换为 `stop-testlink.ps1`。已完成安装，不要重新初始化数据目录或运行向导。

- PHP7.4.33 NTS：`E:\dev\testlink-stack\php74`；所需扩展已核验。
- TestLink1.9.20：`E:\dev\testlink-stack\testlink`，上游界面标记`[DEV]`。
- 独立MySQL8.0.44实例3309，数据目录 `E:\dev\testlink-stack\testlink-mysqldata`，管理库`testlink`，应用账户独立，仅监听本机。
- PHP内置Web监听127.0.0.1:8085，免管理员，不安装服务。安装目录已移出Web根目录。
- 页面/XML使用UTF-8；库、表和连接使用utf8mb4，工具common.php固定连接字符集。已补UDFStripHTMLTags及权限，管理工具实例兼容模式为NO_ENGINE_SUBSTITUTION。
- 本地execHistory.php将自定义字段错误的Array显示修正为HTML字符串，原文件已备份，不改变测试状态。失败备注使用纯文本，适配当前显示编辑器。

原3308实例的认证、配置和业务数据未修改。固定旧工具用于本机教学，不据此声称可投产。指定native认证属于兼容选择，不能写成PHP7.4后期版本必然不支持caching_sha2_password。

## 实际受测环境

基线main@909b875，JDK8重新构建并成功。另开后端8082，独立库`mes_lab1_20261107`位于3309，专用数据库用户及Redis DB2。原8081后端不受影响。MES连接初始化为与原3308相同的严格SQL模式，不继承管理工具的宽松模式。

主数据来自3308只读快照，仅清空新库93张业务及日志表；正式执行前库存流水0、物料17项、BOM16行。配置凭据保留在忽略的`.tmp-lab1/application-run.yml`，不写入报告。

正式执行窗口17:17:03—17:17:15，约11秒，包含HTTP请求和数据库核对；准备、适配、设计、报告不包含在内。预执行修正了合格量字段quantityQuanlified、自动报工编号、自动采购入库草稿、明细主键和OQC门禁位置的脚本适配，记录单独保存，不计最终统计。

正常链路完成14批原料、3工单9任务、3领料、9正常报工和3项IPQC。成品批次BAT00000220入库10台、销售6台、本批结存4台；64条库存流水，无负库存。原料合计150是固定样本混合计量单位核对值。

## 缺陷及复测

LAB1-D001（S2严重）：POST /mes/pro/workorder提交quantity=0/-1，合法产品和客户下业务code200，新增工单和8行异常数量BOM。首测工单4/5、复现工单6/7，保存证据后仅从隔离库清理。原版本复现不等于修复后回归；主干业务代码未修改。

建议新增/修改入口共享非空且大于0的服务端数量校验，并保持工单+BOM事务一致。修复后新build回归0、-1、空值、正常1/10及修改场景，保留旧结果。

## 素材和范围

前20条通过XML导入，新增8条通过XML-RPC创建、关联需求、加入原计划并分配tester。`scripts/lab1/generate-assets.mjs`输出已同步至28条，按原文档ID、外部用例ID更新；导入素材只包含设计，重新生成不会覆盖已有实际执行记录。

本轮API使用隔离环境admin，结果归档到TestLink tester。未测MES浏览器兼容、全部岗位越权、并发、完整FIFO拆批、20/30用户性能、10万背景记录、24小时稳定性及月可用率。不做独立R2、设备/排班全覆盖、邮件、外部缺陷平台、报表定制和CSV/HTML导出。

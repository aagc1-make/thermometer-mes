========================================================
红外测温枪 MES · 数据库导入说明
导出时间：2026-10-26
导出环境：MySQL 8.0.44 / 库名 j2eedb / 端口 3307
========================================================

一、文件清单（本目录）
  01_schema.sql   结构：181 张表 + 1 个视图(v_stock_alert) + 2 个触发器  约 0.32 MB
  02_data.sql     数据：5187 条 INSERT（已排除 3 张日志表）              约 1.25 MB
  README_导入说明.txt  本文件

  说明：01 与 02 必须按顺序执行；01 已包含触发器，02 不含触发器（避免重复创建报错）。


二、创建数据库（MySQL 8，字符集必须 utf8mb4）

  CREATE DATABASE j2eedb DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

  注意：本项目导出时使用 utf8mb4_0900_ai_ci。
        如果你的 MySQL 是 5.7，需要先把两个 sql 文件里的
        utf8mb4_0900_ai_ci 全局替换为 utf8mb4_general_ci，
        并把 utf8mb4_0900_as_cs 一并替换。


三、导入方式

  方式 A：Navicat
    1. 连接本地 MySQL（端口按你的实际配置，默认 3306 或 3307）
    2. 右键数据库 j2eedb → 运行 SQL 文件 → 选择 01_schema.sql → 开始
    3. 同样方式导入 02_data.sql

  方式 B：命令行（推荐，快且不依赖 GUI）
    mysql -h127.0.0.1 -P3307 -uroot -p -e "CREATE DATABASE IF NOT EXISTS j2eedb DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
    mysql -h127.0.0.1 -P3307 -uroot -p j2eedb -e "source /path/01_schema.sql"
    mysql -h127.0.0.1 -P3307 -uroot -p j2eedb -e "source /path/02_data.sql"

  Windows 下路径示例：
    mysql -h127.0.0.1 -P3307 -uroot -p j2eedb -e "source E:/mes/deploy/01_schema.sql"


四、导入后校验（数字必须一致）

  SELECT COUNT(*) FROM information_schema.tables
   WHERE table_schema='j2eedb' AND table_type='BASE TABLE';     -- 期望 181
  SELECT COUNT(*) FROM information_schema.views
   WHERE table_schema='j2eedb';                                 -- 期望 1
  SELECT COUNT(*) FROM information_schema.triggers
   WHERE trigger_schema='j2eedb';                               -- 期望 2

  SELECT COUNT(*) FROM md_item;          -- 期望 17（整机1 + 半成品2 + 外购件14）
  SELECT COUNT(*) FROM md_product_bom;   -- 期望 16（多级 BOM 行）
  SELECT COUNT(*) FROM pro_workorder;    -- 期望 30（含演示工单）
  SELECT COUNT(*) FROM wm_transaction;   -- 期望 > 0（库存流水）
  SELECT COUNT(*) FROM sys_menu;         -- 期望 473

  触发器名称应为：
    trg_feedback_auto_ipqc    （产出/报工 → 自动生成 IPQC 待检单）
    trg_recpt_auto_iqc        （采购入库完成 → 自动生成 IQC 复检单）


五、默认账号（密码均为 admin123）

  admin        系统管理员
  craftsman    工艺员
  planner      计划员
  warehouse    仓库员
  operator     生产操作员
  inspector    质检员
  equipment    设备员
  teamleader   班组长

  登录地址：http://localhost
  提示：小程序端需要演示时可再执行 sql/mes_inquiry.sql（建 3 张询价表，可选）


六、后端连接配置（需要自己改成本机）

  文件：backend/ktg-mes/ktg-admin/src/main/resources/application-druid.yml
    url:      jdbc:mysql://127.0.0.1:3307/j2eedb?...   ← 改成你的端口
    username: root
    password: 你的密码

  Redis：application.yml 中 spring.redis.host=127.0.0.1，port=6379。
         若本机 Redis 设置了密码，需在配置里补 password，否则会报
         "ERR Client sent AUTH, but no password is set"。

  MinIO（文件上传用，可选）：application.yml 中 minio.url=http://127.0.0.1:9100，
         bucketName=lx-mes，需自行安装并创建同名桶；不装不影响业务主流程。


七、常见问题

  1) 导入报 "Unknown collation: 'utf8mb4_0900_ai_ci'"
     → 你的 MySQL 是 5.7，按第二节做全局替换后再导入。

  2) 导入报 "Trigger already exists"
     → 说明用旧版脚本重复执行了 01；本版本 02 已排除触发器，重新执行 01 后再执行 02 即可。

  3) 后端启动报 "Table 'j2eedb.xxx' doesn't exist"
     → 01_schema.sql 没导完，或导入时中途报错（看是否有报错被忽略）。

  4) 登录报验证码错误
     → Redis 未启动，或 Redis 有密码但配置没填。

  5) 库存/工单数据看起来"太完整"
     → 数据脚本里含演示单据（工单 30 张、流水若干），用于答辩演示；
        如需从零开始，执行 sql/thermometer_init.sql 会清空业务数据并重建主数据。


八、不要这样做

  × 不要直接拷贝 MySQL 的 data 目录或 .ibd 文件——与版本、路径、页大小强绑定，极易打不开
  × 不要只导入 02_data.sql——没有表结构会全部失败
  × 不要把 application-druid.yml 里的明文密码原样用于公网环境
====== 完 ======

# 项目资料索引

## 开发和部署

- [项目介绍与启动步骤](../README.md)：技术栈、依赖、数据库初始化、开发启动和测试命令。
- [启动说明](启动说明.md)：本机 IDEA、JDK 和中间件启动记录；其中安装路径需按本机环境调整。
- [后端数据库配置示例](../backend/ktg-mes/ktg-admin/src/main/resources/application-druid.example.yml)：复制为同目录的 `application-druid.yml` 后填写连接信息。
- [云端数据库导入说明](../sql/云端数据库导入说明.txt)：SQL 导入及部署说明。
- `sql/01_schema.sql`、`sql/02_data.sql`：新环境建表与初始化数据。
- `backend/ktg-mes/sql/patch_*.sql`、`sql/patch_*.sql`：现有数据库的增量补丁；按目标功能和补丁说明执行，避免重复导入全量数据。
- `sql/face_login.sql`：人脸登录模板表 `sys_user_face`；新环境与演示环境都需执行一次（`CREATE TABLE IF NOT EXISTS`，可重复执行）。

## 业务与设计

- [需求规格说明书](需求规格说明书-红外测温枪MES.doc)：原始需求条目（REQ-001~025）。
- [业务功能说明](红外测温枪MES_第一性原理业务与功能说明.md)
- [业务流程说明](业务流程说明.md)
- [系统业务流程](红外测温枪MES系统业务流程.md)
- [业务流程图（按需求文档三级标题）](业务流程图_按需求文档三级标题.md)
- [侧边栏与数据库表对应关系](侧边栏与数据库表对应关系.md)
- [系统侧边栏菜单结构](系统侧边栏菜单结构.md)
- [系统侧边栏业务流程图](系统侧边栏业务流程图.md)、[优化版](系统侧边栏业务流程图-优化版.md)
- [系统表单业务流程图](系统表单业务流程图.md)
- [工单生命周期延伸到销售完成](工单生命周期延伸到销售完成-20261101.md)
- [系统设计说明书](系统设计说明书-红外测温枪MES.docx)
- [界面定义表](红外测温枪MES_界面定义表.docx)、[内部接口定义表](红外测温枪MES_内部接口定义表.docx)
- [人脸登录说明](face-login.md)：浏览器本地特征比对、活体、限流与隐私边界。
- [本地知识库说明](local-knowledge-base.md)：智能助手文档导入、检索与引用。
- `superpowers/specs/`、`superpowers/plans/`：功能设计与实施记录。

## 测试和演示

- `scripts/run-unit-tests.mjs`：汇总不依赖运行中服务的前端及业务逻辑测试。
- `scripts/tests/`：工单流水线、自动拣货、报工、IPQC、智能助手等回归测试。
- `backend/ktg-mes/ktg-mes/src/test/`：后端单元和业务测试。
- `scripts/demo-flow.mjs`、`scripts/smoke-crud.mjs`、`scripts/check-upload.mjs`：会写入数据的演示和冒烟检查，仅在演示环境使用。
- `scripts/verify-full-flow.test.mjs`：只读检查固定演示数据；需先准备它所要求的数据快照，不作为离线测试执行。

## 本机资料与仓库边界

源代码、静态资源、初始化 SQL、功能补丁、测试及项目文档提交到仓库。
根目录只保留运行入口（启动脚本、Postman 集合）与 `README.md`；业务与设计文档统一放在 `docs/`。
本地数据库连接配置、AI 密钥文件、日志、依赖目录、构建产物、浏览器截图、答辩证据目录及交付备份保留在本机，由 `.gitignore` 排除。
答辩、调研、评审与 AI 提示词等过程性资料不提交到仓库，只在本机归档。

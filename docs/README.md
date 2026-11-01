# 项目资料索引

## 开发和部署

- [项目介绍与启动步骤](../README.md)：技术栈、依赖、数据库初始化、开发启动和测试命令。
- [启动说明](../启动说明.md)：本机 IDEA、JDK 和中间件启动记录；其中安装路径需按本机环境调整。
- [后端数据库配置示例](../backend/ktg-mes/ktg-admin/src/main/resources/application-druid.example.yml)：复制为同目录的 `application-druid.yml` 后填写连接信息。
- [云端数据库导入说明](../sql/云端数据库导入说明.txt)：SQL 导入及部署说明。
- `sql/01_schema.sql`、`sql/02_data.sql`：新环境建表与初始化数据。
- `backend/ktg-mes/sql/patch_*.sql`、`sql/patch_*.sql`：现有数据库的增量补丁；按目标功能和补丁说明执行，避免重复导入全量数据。
- `sql/face_login.sql`：人脸登录模板表 `sys_user_face`；新环境与演示环境都需执行一次（`CREATE TABLE IF NOT EXISTS`，可重复执行）。

## 业务与设计

- [业务功能说明](../红外测温枪MES_第一性原理业务与功能说明.md)
- [系统业务流程](../红外测温枪MES系统业务流程.md)
- [侧边栏与数据库表对应关系](../侧边栏与数据库表对应关系.md)
- [表单联动交付说明](../表单联动改造交付说明-20261028.md)
- [工单生命周期延伸到销售完成](../工单生命周期延伸到销售完成-20261101.md)
- [人脸登录说明](face-login.md)：浏览器本地特征比对、活体、限流与隐私边界。
- [本地知识库说明](local-knowledge-base.md)：智能助手文档导入、检索与引用。
- [系统设计说明书](../系统设计说明书-红外测温枪MES.docx)
- [界面定义表](../红外测温枪MES_界面定义表.docx)
- [内部接口定义表](../红外测温枪MES_内部接口定义表.docx)
- `superpowers/specs/`、`superpowers/plans/`：功能设计与实施记录。

## 测试和演示

- `scripts/run-unit-tests.mjs`：汇总不依赖运行中服务的前端及业务逻辑测试。
- `scripts/tests/`：工单流水线、自动拣货、报工、IPQC、智能助手等回归测试。
- `backend/ktg-mes/ktg-mes/src/test/`：后端单元和业务测试。
- [标准工单闭环手动测试](../手动测试-标准工单闭环.md)
- [演示流程](../演示流程.md)
- `scripts/demo-flow.mjs`、`scripts/smoke-crud.mjs`、`scripts/check-upload.mjs`：会写入数据的演示和冒烟检查，仅在演示环境使用。
- `scripts/verify-full-flow.test.mjs`：只读检查固定演示数据；需先准备它所要求的数据快照，不作为离线测试执行。

## 本机资料与仓库边界

源代码、静态资源、初始化 SQL、功能补丁、测试及项目文档提交到仓库。
本地数据库连接配置、AI 密钥文件、日志、依赖目录、构建产物、浏览器截图、答辩证据目录及交付备份保留在本机，由 `.gitignore` 排除。
原有文档位置保持不变，便于沿用已有引用。

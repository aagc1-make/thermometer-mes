# Dify 工作流编排 · 验收记录（AC-01 ~ AC-08）

| 项目 | 内容 |
| --- | --- |
| 依据 | 《MES 智能助手 · Dify 工作流编排 PRD》V0.1，第 5 节验收用例 |
| 记录日期 | 2026-11-22（真机补测） |
| 被测对象 | 当前工作区 `com.ktg.mes.ai.*` 改动、`docs/dify/mes-ai-workflow.yml`、本机自托管 Dify |
| 实测 Dify 版本 | **1.9.0**（`langgenius/dify-api:1.9.0`，11 个容器，nginx 入口 8082） |
| 执行环境 | Windows 11 26200；WSL 2.9.13.0（内核 6.18.40.1-1）；Docker Desktop 29.9.3；JDK 1.8.0_261；MySQL 3307 / Redis 6379 / Qdrant 6333 / Ollama 11434 本机运行 |

## 一、总览

| 编号 | 覆盖 | 状态 | 说明 |
| --- | --- | --- | --- |
| AC-01 | DF-01 | **通过** | 1.9.0 部署、导入、发布、取应用密钥均完成；状态从 `configured` 转 `online`；四条分支各有真实运行编号 |
| AC-02 | DF-02、03、04、05 | **通过** | 文档、业务、图谱三条分支均真机核对六阶段；图谱运行 `59c954ad-…` 关联 6 实体/5 关系 |
| AC-03 | DF-03、04、05 | **通过** | 一次运行内先文档后业务，同轮返回 1 条引用 + 1 条工单表格 |
| AC-04 | DF-03、04、07 | **部分通过** | 混合单路失败（业务鉴权异常）时文档引用保留、原因写入回答；无命中文档与缺参数未单独造数据 |
| AC-05 | DF-06 | **部分通过** | Dify 输入输出已采样（仅 `ticket`/`route` 与 `receipt`/`route`/`callback_status`）；假凭证 403；部门 A/B 与缺权限账号未回归 |
| AC-06 | DF-05、07 | **通过** | 停 nginx 后轨迹 `Dify 兜底`、回答正常；重启后下一轮恢复 `online` |
| AC-07 | DF-04、07 | **部分通过** | 模拟本地模型离线时引用保留、返回原文片段；取消与老请求隔离未复测 |
| AC-08 | DF-01、06、07 | **部分通过** | 既有 JSON/SSE 接口实测可用（含新增 `business` 事件）；全量业务回归未执行 |

结论：**PRD 的首版主路径（文档、业务、混合、退化、模型离线）已在本机真实 Dify 上跑通，可作为答辩证据；图谱分支与部分权限/回归项仍未验证，不得标记为通过。**

## 二、证据清单

### E1 单元测试（编排协议）

`AiDifyWorkflowTest` 12 项、`MesAiDifyControllerTest` 3 项、既有 `AiPurchaseReceiptsTest` 3 项，合计 18 项全部通过。测试项与证明见部署手册第 0 节；复现命令：

```powershell
cd backend/ktg-mes
$env:JAVA_HOME = 'C:\Program Files\Java\jdk1.8.0_261'
mvn -B -Dmaven.repo.local=C:\Users\<you>\.m2\repository -pl ktg-admin -am test `
  -Dtest='AiDifyWorkflowTest,MesAiDifyControllerTest,AiPurchaseReceiptsTest'
```

```
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0 -- AiDifyWorkflowTest
[INFO] Tests run: 3,  Failures: 0, Errors: 0, Skipped: 0 -- MesAiDifyControllerTest
[INFO] Tests run: 3,  Failures: 0, Errors: 0, Skipped: 0 -- AiPurchaseReceiptsTest
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### E2 工作流结构校验与导入

`docs/dify/mes-ai-workflow.yml`：14 节点 / 17 边；导入结果为 `completed-with-warnings`（0.3.0 → 1.9.0 的 DSL 自动升级提示，非错误），发布 `success`，应用密钥已生成。草稿核验：10 个 HTTP 节点全部使用 `{{#env.mes_callback_url#}}`，且均启用 `retry_config.retry_enabled`。

### E3 编译

`ktg-mes` 894 个源文件全量编译通过；前端 `index.vue`、`AssistantStatus.vue` 通过 `scripts/check-vue.mjs`；前端离线单测 189 通过 / 7 失败（7 项为改动前已存在的沙箱缺桩，HEAD 版本为 9 失败）。

### E4 环境（补测后）

```
Docker: Docker Desktop 29.9.3（引擎 7.9 GB，WSL 2 后端）
WSL:   2.9.13.0 / 内核 6.18.40.1-1
Dify:  11 个容器 Up（api、worker、worker_beat、web、db、redis、weaviate、plugin_daemon、sandbox、ssrf_proxy、nginx）
Neo4j: 5.26.0，HTTP 7474（MES 用 /db/neo4j/query/v2）、Bolt 7687，账号 neo4j / MesNeo4j2026
入口:  http://127.0.0.1:8082（nginx，宿主机 80/443 被 http.sys 占用，改用 8082/8443）
```

安装过程中的三个坑与处理已写入部署手册第 0.2 节：代理不在提权进程里（改用离线 MSI）、Docker Hub 直连不通（registry mirror）、WSL 在 Win11 上是独立包而不是开关。

### E5 真机运行编号与节点时序

以下运行均为本机 Dify 1.9.0 的真实 `workflow_run_id`，MES 轨迹与 Dify 运行可一一对应。

| 场景 | 运行编号 | MES 轨迹要点 |
| --- | --- | --- |
| 业务·安全库存 | `d102ede2-f41a-4c55-ada0-b6c2b4b32f98` | 改写（分支 业务数据）→ 业务工具查询 20 ms → 证据排序“无需重排” → 回答生成 → 引用整理 → 完成回执 → `Dify 编排 · 运行 d102ede2…` |
| 业务·工单进度 | `7fbcc884-f1cd-434a-b75c-fb1003421315` | 工单查询与进度返回 1 条，表格 8 列；同一链路 |
| 业务·恢复后 | `d47ce983-c0f3-4007-87c9-872036bbba0a` | 停 nginx 复测后第一轮即回到 `Dify 编排` |
| 文档·本地模型 | `dea42359-b43c-4140-a41c-23988a669d99` | 文档检索命中 1 片段 → 排序“关键词与向量融合排序” → 生成（qwen2.5:7b 流式）→ 引用 1 条 |
| 文档·仅检索 | `a3517f3e-3bcc-43ef-8bde-a67747a88890` | 本地模型未启动时退化为原文片段，引用仍在 |
| 文档·模拟离线 | `0b162392-8b30-49d6-b03f-320517ce8298` | `skipLocal` 下 `演练本地模型离线 → 仅检索回答`，`done.channel=retrieval` |
| 混合 | `fabdbec0-97b5-4422-b3b0-fc23ba7fc8e1` | 同一运行内 `混合问答·文档检索`（1 片段）+ `混合问答·业务查询`（工单 1 条）；一轮同时返回引用与表格 |
| 图谱 | `59c954ad-f1ec-4d39-9d77-bfe10940d845` | 图谱检索关联 6 实体 / 5 关系（文档、红外传感器模块、蜂鸣器、自动关机、出货检验、红外测温枪整机），命中 3 片段，引用 3 条 |
| 假凭证探测 | `94d3d7cf-3942-4ee6-9892-c59b7582ecf0` | 节点 `403` → 整轮 `failed`，证明回执校验与拒绝语义 |

状态接口实测（`GET /mes/ai/status`）：

```json
{"dify":"online","difyDetail":{"state":"online","configured":true,"timeoutMs":12000,
 "lastRunId":"d47ce983-c0f3-4007-87c9-872036bbba0a","detail":"运行 d47ce983… · 分支 业务数据"},
 "localModel":"online","vectorDb":"online","externalApi":"not_configured"}
```

### E6 补测中发现并修复的问题

1. **混合问答业务分支鉴权失败**：SSE 在独立线程执行，`SecurityContext` 未传递，业务分支报“获取用户信息异常”。已在 `MesKbRagController` 把登录身份带入工作线程；修复后混合问答返回真实工单数据。
2. **Dify HTTP 节点把 403 当成功**：`error_strategy`/`retry` 为空时 4xx 仍返回 `succeeded`，凭证被拒在 Dify 里显示绿色。10 个 HTTP 节点改为 `retry_enabled: true`，现在 403 会让整轮 `failed`。
3. **Maven 增量编译漏编**：只改源码时 `package` 可能不重编 `ktg-mes`，导致运行的仍是旧 jar。交付前需核对 jar 时间戳，必要时先删 `ktg-mes/target/classes`。
4. **图谱地址必须是 HTTP**：`KbNeo4j` 用 Neo4j 的 `/db/{db}/query/v2` HTTP 接口，`mes.ai.graph.url` 填 `bolt://…:7687` 会一直显示 `offline`；正确值是 `http://127.0.0.1:7474`。
5. **建图开关只在新建文档时生效**：`KbDocumentService.upload` 仅在新建文档分支写入 `graphEnabled`，给已有文档追加版本时该参数被忽略（本次补测实测）。演示时请新建文档并勾选“纳入知识图谱”；如需支持“加版本时补建图”，需再改一处 mapper 与入队逻辑。

## 三、逐条记录

### AC-01（DF-01）本机运行

| 检查 | 结果 |
| --- | --- |
| 官方 Compose 部署、记录实测版本 | 通过：Dify 1.9.0，11 容器，入口 8082 |
| 导入并发布 Workflow | 通过：导入 `completed-with-warnings`（DSL 升级），发布 `success` |
| 状态从待验证转在线 | 通过：`configured` → 首轮回执校验后 `online`（`a9d082b7-…`） |
| 未配置 / 待验证 / 在线 / 离线四态 | 通过：`not_configured` / `configured` / `online` / `offline`，`statusDetail` 带运行编号 |
| 无法证明使用 Dify 时标注实际路径 | 通过：停 nginx 后轨迹为 `Dify 兜底 · Dify 不可达或等待超时` |
| 画布截图 | 按需求方要求不附；如需归档，可在 Dify 控制台补一张 |

### AC-02（DF-02、03、04、05）三题分支与阶段

| 检查 | 结果 |
| --- | --- |
| 问题改写与分支标识 | 通过：MES 归一化参数后只传 `route`（`doc`/`graph`/`business`/`mixed`） |
| 节点分支与回答来源一一对应 | 通过（文档、业务）：轨迹与 Dify 运行同一编号 |
| 图谱分支 | 通过：`59c954ad-f1ec-4d39-9d77-bfe10940d845`，轨迹 `图谱检索 · 关联 6 个实体、5 条关系`，排序为“图谱关系与文档证据合并排序”，引用 3 条 |
| 阶段可见（改写/检索/排序/生成/引用/回执） | 通过：每阶段一条轨迹，含耗时与策略说明 |
| 排序记录实际策略 | 通过：文档“关键词与向量融合排序”、业务“无需重排（业务表格保持原始口径）” |

### AC-03（DF-03、04、05）混合问答

| 检查 | 结果 |
| --- | --- |
| 同一运行编号包含文档与业务两个检索节点 | 通过：`fabdbec0-…` 内 `混合问答·文档检索` + `混合问答·业务查询` |
| MES 同屏可查引用和表格 | 通过：一条 SSE 流内先 `citations` 再 `business`（1 项查询、8 列） |
| 数字与现有业务查询一致 | 通过：业务侧复用 `AiReadTools`，追加文字为确定性汇总 |

### AC-04（DF-03、04、07）无证据、缺参数、部分失败

| 检查 | 结果 |
| --- | --- |
| 混合单路失败保留另一路 | 通过（意外验证）：业务分支鉴权异常时仍返回文档引用与 1 条工单缺失说明，`notice` 携带原因 |
| 无命中文档 | 逻辑已实现（`citations` 为空时只输出“没有找到相关资料”），未单独造无命中数据 |
| 工单不明确 | 沿用既有参数校验，未单独造数据 |
| 来源标签与实际结果一致 | 通过：引用来自授权部门库，表格来自只读工具返回行 |

### AC-05（DF-06）权限与数据边界

| 检查 | 结果 |
| --- | --- |
| Dify 输入输出只含执行元数据 | 通过（采样）：输入 `{ticket, route}`，输出 `{receipt, route, callback_status}` |
| 假凭证被拒绝 | 通过：`94d3d7cf-…` 节点 403，整轮 failed |
| 重复/过期/阶段不符凭证 | 单测覆盖重复与阶段不符；过期未做长等待测试 |
| 部门 A/B 隔离、缺库存权限账号 | **未验证**：需两个部门账号 |
| 日志与状态遮盖凭证 | 通过：`status` 与轨迹只有运行编号、分支、校验时间 |

### AC-06（DF-05、07）停机与退化

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| 停止 Dify 后仍正常回答 | 通过 | 停 nginx 后业务题 `mode=rules`，1 条结果，轨迹末条 `Dify 兜底 · Dify 不可达或等待超时` |
| 已完成阶段不重复执行 | 通过（单测） | `outOfOrderStageFallsBackAndReusesFinishedWork` |
| 恢复后下一轮回到在线 | 通过 | 重启 nginx 后 `d47ce983-…` |
| 终止回执失败 | 通过（单测 + 假凭证实测） | `mismatchedCompletionReceiptFallsBack`、`94d3d7cf-…` |

### AC-07（DF-04、07）模型离线

| 检查 | 结果 |
| --- | --- |
| 文档题保留引文 | 通过：`skipLocal` 下引用 1 条，回答为原文片段，`channel=retrieval` |
| 业务题保留规则表格 | 通过：规则分支不依赖模型（`d102ede2-…`） |
| 流式取消与老请求隔离 | **未复测** |

### AC-08（DF-01、06、07）回归

| 检查 | 结果 |
| --- | --- |
| 既有 JSON 入口 | 通过：`POST /mes/ai/business/answer` 返回 `mode/model/results/trace/notice` |
| 既有 SSE 入口 | 通过：`trace/citations/delta/done` 正常，新增 `business` 仅混合轮次出现 |
| 编译与既有单测 | 通过 |
| 工单缺料、调拨草稿等业务回归 | **未执行**，相关代码未改动 |

## 四、后续必做

1. 用部门 A/B 账号与缺库存权限账号补 AC-05 隔离与权限回归。
2. 补 AC-07 的取消/老请求隔离、AC-08 的全量业务回归。
3. 可选：把“追加版本时可补建图”补上（当前只在新建文档时生效），并给图谱补一条“关系型”问题（现有图谱以 MENTIONS 关系为主）。
4. 演示数据已就绪：部门库「MES Demo Library」（研发部门）含《红外传感器模块来料检验指导书》2.1 与已建图的《整机检验规范》1.2。

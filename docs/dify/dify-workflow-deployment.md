# Dify 工作流编排 · 部署与联调手册

> 依据《MES 智能助手 · Dify 工作流编排 PRD》V0.1 编写，对应交付物：`docs/dify/mes-ai-workflow.yml`（可导入工作流）、`docs/dify/acceptance-record.md`（验收记录）。
> 首版演示为单实例 MES + 本机自托管 Dify，回调与原请求由同一实例接收。

## 0. 当前状态（先看这里）

| 项 | 状态 | 说明 |
| --- | --- | --- |
| MES 侧分阶段编排与回调协议 | 已实现 | 15 项单元测试覆盖阶段顺序、凭证重放、阶段不符、回执不符、不可达、恢复在线与回调握手 |
| 可导入工作流 | **已部署通过** | 14 节点、17 条边；导入、发布、应用密钥均成功，四条分支已各跑一次真实运行 |
| 本机 Dify 部署 | **已完成** | Dify 1.9.0，11 个容器，nginx 入口 8082；实测版本见下表 |
| AC-01、AC-02、AC-03、AC-04、AC-06、AC-07 | **通过** | 真机运行编号与节点时序见验收记录 E5 |
| AC-05（Dify 输入输出样本）、AC-08（全量回归） | 部分通过 | 输入输出已采样；全量业务回归未执行 |

### 0.1 实测环境（2026-11-22）

| 组件 | 版本 / 端口 | 备注 |
| --- | --- | --- |
| 宿主 | Windows 11 家庭中文版 10.0.26200 | 无 Hyper-V 角色，走 WSL 2 后端 |
| WSL | 2.9.13.0，内核 6.18.40.1-1 | `wsl --install` 被代理拦成 403，改用 GitHub 发布页的 `wsl.2.9.13.0.x64.msi` 离线安装 |
| Docker | Docker Desktop 29.9.3（引擎同版本，7.9 GB） | 官方安装包 596 MB，`install --quiet --accept-license --backend=wsl-2` |
| Dify | **1.9.0**（`langgenius/dify-api:1.9.0`） | 11 个容器；nginx 暴露 **8082**（宿主 80/443 被 http.sys 占用） |
| 镜像源 | `docker.m.daocloud.io`、`docker.1ms.run`、`docker.xuanyuan.me` | 宿主走本地代理 `127.0.0.1:7897`，容器内不可达，故用 registry mirror 拉取 |
| MES | 8080，`-Dmes.ai.dify.*` 注入配置 | 前端开发服务器 8095 |

### 0.2 与初次交付的差异（实测修正）

1. **Dify 的 HTTP 节点默认把 4xx 当成功**：`error_strategy` 与 `retry` 都为空时，MES 返回 403 也只会得到 `status: succeeded` 和一段错误文本。工作流已把 10 个 HTTP 节点改为 `retry_enabled: true`，401/403 才会让节点与整轮运行明确失败。
2. **导入提示 `completed-with-warnings` 不是错误**：0.3.0 的 DSL 会在 1.9.0 上自动升级，`status` 只反映版本差异。
3. **SSE 线程需要显式传递登录身份**：部门库问答在独立线程执行，混合问答的业务分支要按提问账号校验只读工具权限，`MesKbRagController` 已把 `SecurityContext` 带入工作线程；否则业务分支报“获取用户信息异常”。
4. **Maven 增量编译会漏改**：只改源码时 `mvn package` 可能不重编 `ktg-mes`，交付前需确认 jar 时间戳，必要时先删 `ktg-mes/target/classes`。
5. 回调地址 `http://host.docker.internal:8080/mes/ai/dify/callback` 在 Docker Desktop 上直接可用，未额外加防火墙规则。

## 1. 责任边界

| 角色 | 负责 |
| --- | --- |
| Dify | 选择分支、推进阶段、记录每一步节点执行 |
| MES | 用户与部门权限、检索、排序、确定性计算、模型生成、引用整理、回答渲染 |

进入 Dify 的只有一次性凭证与分支标识；原问题、文档片段、图谱内容、业务数据行、回答正文与登录令牌都留在 MES。回调只返回阶段握手结果，不传业务结果。

## 2. 前置条件

```powershell
# 三条都要有输出，缺一条就先装
wsl --status
docker --version
docker compose version
```

| 检查项 | 期望 | 不满足时 |
| --- | --- | --- |
| WSL 2 | 已安装并运行 `wsl --install` 后的默认发行版 | 管理员 PowerShell 执行 `wsl --install` 后重启 |
| Docker Desktop | 已启动，容器可运行 | 安装后开启“Use the WSL 2 based engine” |
| 端口 80 | 空闲（Dify 默认入口），或改用 `.env` 中的 `EXPOSE_NGINX_PORT` | 换端口并在第 6 节同步改 `MES_AI_DIFY_URL` |
| 端口 8080 | MES 后端监听 | 按 `start-backend-8081.bat` 换端口后同步回调地址 |
| 磁盘 | ≥ 20 GB（镜像约 10 GB） | 先清理再拉取 |

## 3. 部署 Dify

```powershell
git clone https://github.com/langgenius/dify.git E:\dify
cd E:\dify\docker
Copy-Item .env.example .env
# 固定验收版本：把 docker-compose.yaml 中的镜像标签改成实测版本，并把版本号写入验收记录
docker compose up -d
docker compose ps          # 全部 Up / healthy 后再继续
```

- 控制台：<http://localhost>，首次打开创建管理员账号。
- 接口基址：`http://127.0.0.1/v1`（走 compose 的 nginx 入口）。
- 只对本机或演示内网开放；不要给 Dify 配额外的外网访问。

首次拉取镜像与断网演示各验证一次：`docker compose stop` 后 MES 应自动退化（第 8 节），`docker compose start` 后下一轮恢复。

## 4. 导入并发布工作流

1. Dify 控制台 → **工作室 / Studio** → **导入 DSL** → 选择 `docs/dify/mes-ai-workflow.yml`。
2. 打开应用 → **环境变量** → 把 `mes_callback_url` 填成 Dify 容器能访问的 MES 地址：

   | 部署方式 | 取值 |
   | --- | --- |
   | Docker Desktop（Windows/macOS） | `http://host.docker.internal:8080/mes/ai/dify/callback` |
   | Linux 宿主 | 先给 api 容器加 `extra_hosts: ["host.docker.internal:host-gateway"]`，再同上 |
   | 代理阻断 host.docker.internal | 用宿主内网 IP，并在 Windows 防火墙放行 8080 入站 |

3. **发布**（不发布时 `/v1/workflows/run` 返回“工作流未发布”）。
4. **访问 API** → 复制应用密钥 `app-…`，后面填进 `MES_AI_DIFY_KEY`（这是应用密钥，不要填 Dify 内部的 SECRET_KEY）。
5. 回到画布确认节点链完整：

   `领取本轮任务 → 问题改写确认 → 选择检索分支 →（文档 / 图谱 / 业务 / 混合两路）→ 汇集阶段凭证 → 证据排序 → 回答生成 → 引用整理 → 完成回执 → 返回执行回执`

`docs/dify/mes-readonly-workflow.yml` 只用于部署预检（三节点），不进验收：它能证明回调链路通，但阶段链不完整，MES 不会认领本轮编排。

## 5. 连通性预检

```powershell
# ① MES → Dify：密钥与发布状态（200 = 通；本机 Dify 入口是 8082，不是 80）
curl.exe -s -o NUL -w "dify:%{http_code}`n" http://127.0.0.1:8082/v1/parameters -H "Authorization: Bearer app-你的密钥"

# ② Dify → MES：从容器内打回调，403 = 已到达 MES 且被正确拒绝（假凭证）
docker compose exec api sh -lc "curl -s -o /dev/null -w '%{http_code}\n' -X POST http://host.docker.internal:8080/mes/ai/dify/callback -H 'Content-Type: application/json' -d '{\"ticket\":\"0000000000000000000000000000000000000000000\",\"stage\":\"rewrite\"}'"
```

两条都符合预期再继续；`000` / 连接超时说明地址或被防火墙挡住，先修网络。

## 6. MES 配置

`backend/ktg-mes/ktg-admin/src/main/resources/application.yml` 已包含以下配置（值来自环境变量）：

```yaml
mes:
  ai:
    dify:
      api-base-url: ${MES_AI_DIFY_URL:}      # 带 /v1，例如 http://127.0.0.1/v1
      api-key: ${MES_AI_DIFY_KEY:}           # Dify 应用密钥 app-…
      timeout-ms: ${MES_AI_DIFY_TIMEOUT_MS:12000}
```

```powershell
$env:MES_AI_DIFY_URL = 'http://127.0.0.1:8082/v1'
$env:MES_AI_DIFY_KEY = 'app-你的应用密钥'
$env:MES_AI_DIFY_TIMEOUT_MS = '12000'
# 重启后端后检查状态
curl.exe -s http://127.0.0.1:8080/mes/ai/status -H "Authorization: Bearer <登录令牌>"
```

本机直接用 jar 启动时，实测可用的写法（系统属性同样能覆盖配置）：

```powershell
java -Dfile.encoding=UTF-8 `
  -Dmes.ai.dify.api-base-url=http://127.0.0.1:8082/v1 `
  -Dmes.ai.dify.api-key=app-… `
  -Dmes.ai.dify.timeout-ms=12000 `
  -jar ktg-admin/target/ktg-admin.jar
```

`status` 返回：

| 字段 | 含义 |
| --- | --- |
| `dify` | `not_configured` 未配置 / `configured` 已配置待验证 / `online` 在线 / `offline` 离线 |
| `difyDetail.detail` | 最近一次校验结果，成功时为 `运行 <workflow_run_id> · 分支 <分支>` |
| `difyDetail.verifiedAt` | 最近一次回执校验通过时间 |
| `difyDetail.timeoutMs` | 单轮等待上限 |

配置留空或缺少密钥时 `dify` 为 `not_configured`，助手完全走原问答，不产生任何 Dify 请求。

## 7. 演示流程（现场 3–5 分钟）

助手页右上角有 `Dify · 状态` 徽标，悬停可看最近运行编号与校验时间。

| 顺序 | 在 MES 提问（示例，按演示数据替换物料/工单/文件名） | 检查点 |
| --- | --- | --- |
| 1 | 这份来料检验规范要求哪些检查项？ | 气泡展开“执行轨迹”：`Dify · 问题改写 → Dify · 文档检索 → 证据排序 → 回答生成 → 引用整理 → 完成回执 → Dify 编排 · 运行 <id>`；引用可打开原文 |
| 2 | 红外传感器模块与哪些检验项目有关？ | 分支为“图谱”；轨迹出现 `图谱检索 · 关联 6 个实体、5 条关系` 与图谱子图；引用来自关联文档 |
| 3 | 查一下显示屏当前库存。 | 分支为“业务数据”，表格数字与“库存查询”页面同条件一致；轨迹中排序一步为“无需重排” |
| 4 | 检验规范怎样要求抽检？工单 MO202610270001 的过程检验结果如何？ | 同一 `运行 <id>` 内出现 `混合问答·文档检索` 与 `混合问答·业务查询` 两步；同一条气泡内同时有引用与表格 |

> 本机实测：文档、业务、图谱、混合四题均已用「MES Demo Library」跑通，运行编号见验收记录 E5。

建图的三个前提（缺一会回退到文档证据）：

1. Neo4j 在跑，且 `mes.ai.graph.url` 填 **HTTP 地址**（`http://127.0.0.1:7474`），MES 用的是 Neo4j 的 `/db/{db}/query/v2` 接口，不是 Bolt。
2. 文档在**新建时**勾选“纳入知识图谱”。补测发现：给已有文档追加新版本时，上传参数里的 `graphEnabled` 会被忽略（`KbDocumentService.upload` 只在建文档时写入），此时需要新建一份文档或改动该字段后再重建图。
3. 图谱抽取由后台 worker 异步执行，通常是 1–3 分钟；完成标志是文档列表里 `graphStatus=completed`。

打开 Dify：**工作室 → 该应用 → 日志与标注 / 运行**，按时间找到同一轮，核对：

- 输入只有 `ticket`、`route`（凭证在截图与导出材料中遮盖）；
- 节点链与 MES 轨迹分支一致；
- 每个 HTTP 节点：请求体为 `stage` + `ticket`，响应体为下一步凭证，响应头 `X-MES-Stage`（已完成阶段）、`X-MES-Next-Stage`（下一阶段）、`X-MES-Route`（分支）标明阶段状态；
- 输出为 `receipt`（完成回执）、`route`、`callback_status`。

## 8. 超时与退化

| 现象 | MES 行为 |
| --- | --- |
| `MES_AI_DIFY_*` 未配置 | 直接走原问答，轨迹无 Dify 条目 |
| Dify 不可达 / 超过 `timeout-ms` | 轨迹标注 `Dify 兜底 · Dify 不可达或等待超时`；本轮任务由 MES 原地执行，已完成的检索阶段直接复用，不重复查询 |
| 回调阶段不符、凭证过期或重复使用 | Dify 侧该节点 403 失败；MES 轨迹标注 `Dify 兜底 · Dify 阶段回执被拒绝` |
| 工作流状态非 `succeeded`，或完成回执不匹配 | 轨迹标注 `Dify 兜底 · Dify 完成回执未通过校验`；不显示“本轮由 Dify 编排” |
| 混合只有一路成功 | 保留成功那一路：文档侧保留引用，业务侧提示缺权限/无证据；另一路缺失原因写在轨迹与回答中 |
| 恢复后 | 下一轮符合条件的问答重新尝试 Dify；首次回执校验通过即回到 `online` |

阶段等待上限的意义：`/v1/workflows/run` 用 `timeout-ms` 等待；超出后即使 Dify 稍后才回调，回调仍会被接受（`timeout-ms + 8s` 宽限），结果照常复用，但本轮不再标注为 Dify 编排。

## 9. 回滚

1. 清空 `MES_AI_DIFY_URL` / `MES_AI_DIFY_KEY`（或从启动脚本移除），重启 MES。
2. 助手状态变为 `not_configured`，全部问答回到原有入口与路径。
3. 业务数据、部门库、图谱与已有对话不受影响。
4. Dify 侧可停 `docker compose down`；MES 不依赖它启动。

## 10. 排错

| 现象 | 判断 | 处理 |
| --- | --- | --- |
| `dify` 一直是 `configured` | 没有一轮通过回执校验 | 先做第 5 节两条探测；再看 Dify 运行详情里第一个失败节点 |
| Dify 节点 403 | 凭证阶段不符/过期/已用过，或地址指到别的 MES 实例 | 确认 `mes_callback_url` 指向本轮 MES；确认工作流为本手册的版本（阶段名必须一致） |
| Dify 报 `Workflow not published` | 导入后未发布 | 画布右上角“发布” |
| Dify 报变量找不到 | 导入的是旧版 DSL | 重新导入 `mes-ai-workflow.yml`；不要改名或改节点 id |
| MES 轨迹出现 `Dify 兜底 · 工作流正忙` | 并发超过 8 轮 | 等前一轮结束再问 |
| 页面显示在线但回答里没有 Dify 轨迹 | 该问题未命中业务规则 / 未选部门库 | 只有规则命中的业务问答、文档/图谱/混合问答会进 Dify，其余沿用原路径 |

## 11. 已知边界

- 首版并发上限 8 轮（`AiDifyWorkflow` 信号量），单实例内存保存阶段凭证；重启 MES 会丢弃未完成的本轮上下文，助手自动退化。
- 阶段凭证只存在内存，不做多实例共享；扩容前需改为外部存储。
- 业务分支只对命中既有查询规则的问题启用（库存、流水、工单、BOM、检验、安全库存、物料）；未命中的问题沿用原问答。
- Dify 端节点 id、阶段名与 `callback_status` 结构属于契约，改动需同步升级 MES。

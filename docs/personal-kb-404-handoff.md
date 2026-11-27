# 个人知识库 404 修复交接

在 `E:\JavaCode` 继续处理。目标是让当前开发前端连接包含个人知识库接口的后端，恢复登录后的个人库列表和导入入口。改动范围是本地后端启动配置及其确有问题的启动入口。

## 已复现的现象与根因

从 `http://localhost:8095` 登录后，在「智能问答」选择「个人库 RAG」，点「管理个人知识库」。列表弹窗显示「系统接口 404 异常」，导入按钮随列表错误被禁用。

- 用同一个临时登录会话请求 `GET http://127.0.0.1:8095/dev-api/mes/knowledge/documents` 和 `GET http://127.0.0.1:8080/mes/knowledge/documents`，两者都返回 HTTP 404。对照请求 `GET http://127.0.0.1:8095/dev-api/mes/ai/status` 返回 HTTP 200、业务码 200。临时会话已退出。
- 8095 的 Vue 进程来自 `E:\JavaCode\ktg-mes-ui`。`ktg-mes-ui/vue.config.js:13` 默认把开发 API 指向 `http://127.0.0.1:8080`，第 38–42 行去掉 `/dev-api` 前缀。
- 诊断时 8080 的进程 PID 为 `42668`，实际运行的 JAR 是 `E:\Code Files\thermometer-mes-local-preview\backend\ktg-mes\ktg-admin\target\ktg-admin.jar`。解包检查确认其 `BOOT-INF/lib/ktg-mes-*.jar` 有 `MesAiController.class` 和 `MesKbLibraryController.class`，缺 `MesKnowledgeController.class`。`E:\JavaCode\backend\ktg-mes\ktg-admin\target\ktg-admin.jar` 的同一位置包含个人库控制器。
- `PersonalKnowledgeBase.vue:148` 发出 `GET /mes/knowledge/documents`。当前源码 `MesKnowledgeController.java:23` 和第 42–43 行的映射与请求一致。旧 JAR 没有注册这条路由，请求在进列表查询前就返回了 404。

## 修复步骤

1. 重新确认 8080 的监听进程和实际 JAR 路径，核对当前进程使用的数据库、Redis、Dify 和文件存储目录。现有 8080 进程带有 Dify 启动参数。沿用原有本机配置传入这些参数，并在日志、交接及输出中隐去密钥。
2. 校验 `E:\JavaCode` 下的后端包已包含 `com/ktg/mes/ai/controller/MesKnowledgeController.class`。如果需要重打，使用下面已验证过的本机打包命令。`jar.forceCreation` 确保重新组装 admin JAR 时纳入当前 `ktg-mes` 模块。

   ```powershell
   Set-Location 'E:\JavaCode\backend\ktg-mes'
   $env:JAVA_HOME = 'C:\Program Files\Java\jdk1.8.0_261'
   mvn -B '-Dmaven.repo.local=E:/D/.m2/repo' '-Djar.forceCreation=true' '-Dmaven.test.skip=true' -pl ktg-admin -am package
   ```

3. 安排本地切换，确认可以中断旧预览进程后，使用当前工作区的 JAR 启动 8080，并让 8095 继续指向它。沿用现有启动脚本的 `MES_BACKEND_JAR` 配置方式。`start-mes-with-dify.ps1:86–115` 在已有进程时默认复用它，`-RestartBackend` 要求监听进程的 JAR 路径与传入路径一致。遇到旧预览路径时先处理已确认的旧进程，再从当前工作区启动。
4. 验证实际数据源。只读查询已确认本机 `127.0.0.1:3307/j2eedb` 有 `mes_kb_document`、`mes_kb_version` 和部门库表。若启动配置使用另一数据库，先查询其表状态，缺表时按该环境的授权流程使用 `sql/mes_knowledge_base.sql`。Ollama、Qdrant、Neo4j 在诊断时未监听默认端口，导入和问答联调前按 `README.md:134` 检查服务。个人库列表自身只查询 MySQL。

## 验收与回报

- 核对 8080 运行 `E:\JavaCode` 下的 JAR。在登录态验证直连接口和 8095 代理接口都返回 HTTP 200、业务码 200，`data` 为数组。无资料时允许返回 `[]`。
- 按原步骤打开个人库弹窗，确认列表能显示或正确呈现空状态，且「导入文档」可点。用同一会话确认 `/dev-api/mes/ai/status` 和部门知识库仍能打开。具备独立演示账号和已启动的检索服务时，再上传一份合成 TXT 并验证个人库 RAG。
- 若修改了代码，从 `E:\JavaCode` 运行 `node --test scripts/tests/knowledge-chat.test.cjs`。完整 Maven 测试此前在 4 个现有业务联动测试的构造参数不匹配处停在编译阶段。记录实际测试结果，并单独以带登录态的 HTTP 检查验证本问题。
- 回报实际启动入口、新旧 JAR 路径、具体改动、直连与代理的返回结果，以及哪些功能做了真实联调。诊断交接完成时尚未切换进程，也未改业务代码或数据库。

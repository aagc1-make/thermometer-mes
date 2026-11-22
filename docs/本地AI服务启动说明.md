# 本地 AI 服务启动说明（Qdrant / Neo4j / Ollama）

> 这些组件装在仓库内的 `.tools/`（已加入 `.gitignore`，不随代码提交），演示机没有 Docker 与 WSL，因此全部用 Windows 原生二进制运行。
> 2026-11-22 首次落地，用于让「向量入库 + 语义检索 + 图谱建图」在本机跑通。

## 一、组件与端口

| 组件 | 版本 | 端口 | 位置 |
| --- | --- | --- | --- |
| Qdrant | v1.12.4（单 exe） | 6333 REST / 6334 gRPC | `.tools/qdrant/qdrant.exe` |
| Neo4j | Community 5.26.0（zip，需 JDK21） | 7474 HTTP / 7687 Bolt | `.tools/neo4j-community-5.26.0/` |
| Ollama | 最新 windows-amd64 zip | 11434 | `.tools/ollama/ollama.exe`，模型在 `.tools/ollama/models` |

账号：Neo4j `neo4j / MesNeo4j2026`；Qdrant 未开鉴权（仅本机）。

## 二、启动顺序（每次开机）

```powershell
# 1. Qdrant（工作目录必须是 exe 所在目录，存储目录为 .\storage）
cd E:\JavaCode\.tools\qdrant
.\qdrant.exe

# 2. Neo4j（需要 JDK21；PowerShell 里 $home 是只读变量，别用这个名字）
$neo = 'E:\JavaCode\.tools\neo4j-community-5.26.0'
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:NEO4J_HOME = $neo
cd $neo
& "$env:JAVA_HOME\bin\java.exe" -cp ".\lib\*" -Dbasedir="$PWD" org.neo4j.server.startup.Neo4jCommand console

# 3. Ollama（模型目录指到项目内，避免写到用户目录）
$o = 'E:\JavaCode\.tools\ollama'
$env:OLLAMA_MODELS = "$o\models"
$env:OLLAMA_HOST = '127.0.0.1:11434'
.\ollama.exe serve
```

三个都是前台进程，**各开一个窗口**，关窗口即停止。

> 2026-11-22 实测：`neo4j.bat` 会调用 `neo4j.ps1`，在已设置 `JAVA_HOME` / `NEO4J_HOME` 的环境里报“已添加了具有相同键的项”而启动失败，所以上面改成直启 JVM。
> MES 连的是 Neo4j 的 **HTTP 接口**：`mes.ai.graph.url` 应填 `http://127.0.0.1:7474`，不是 `bolt://…:7687`；控制台 <http://127.0.0.1:7474/browser/>，账号 `neo4j / MesNeo4j2026`。

## 三、模型

| 模型 | 用途 | 拉取命令 |
| --- | --- | --- |
| `bge-m3` | 文档/问题向量化（1024 维，与集合 `mes_kb_bge_m3_v1` 匹配） | `ollama pull bge-m3` |
| `qwen2.5:7b-instruct-q4_K_M` | 本地对话模型（默认名，见 `mes.ai.local-model`） | `ollama pull qwen2.5:7b-instruct-q4_K_M` |

拉取时 Ollama 需要能出网，若走代理，在启动 `serve` 的窗口里先设：

```powershell
$env:HTTPS_PROXY = 'http://127.0.0.1:7897'
$env:HTTP_PROXY  = 'http://127.0.0.1:7897'
$env:NO_PROXY    = 'localhost,127.0.0.1'
```

## 四、后端需要的环境变量

后端启动前设置，否则 `/mes/ai/status` 里对应项显示“未配置”：

```powershell
$env:MES_AI_OLLAMA_URL      = 'http://127.0.0.1:11434'
$env:MES_AI_QDRANT_URL      = 'http://127.0.0.1:6333'
$env:MES_AI_NEO4J_URL       = 'http://127.0.0.1:7474'
$env:MES_AI_NEO4J_USER      = 'neo4j'
$env:MES_AI_NEO4J_PASSWORD  = 'MesNeo4j2026'
# 外部通道（可选）：值放在 ai-keys.txt，勿写进代码
$env:MES_AI_KEY             = '<DeepSeek Key>'
$env:MES_AI_FB_KEY          = '<GLM Key>'
```

## 五、验证

```powershell
# Qdrant
curl.exe --noproxy * http://127.0.0.1:6333/collections
# Neo4j（HTTP Query API，返回 202）
curl.exe --noproxy * -u neo4j:MesNeo4j2026 -H "Content-Type: application/json" `
  -d "{\"statement\":\"RETURN 1 AS ok\"}" http://127.0.0.1:7474/db/neo4j/query/v2
# Ollama 向量
curl.exe --noproxy * -H "Content-Type: application/json" `
  -d "{\"model\":\"bge-m3\",\"input\":[\"测试\"]}" http://127.0.0.1:11434/api/embed
```

登录系统后右上角引擎状态点开，应看到「本地 7B / 向量库 / 图谱」三项变绿。

## 六、踩过的坑

1. **Neo4j 的 `neo4j.bat` 依赖 PowerShell 包装脚本**，环境里同时存在 `NO_PROXY` 与 `no_proxy` 时会报 `An item with the same key has already been added`。启动前删掉小写那份：
   `Remove-Item Env:no_proxy,Env:http_proxy,Env:https_proxy`
2. **PowerShell 的 `$home` 是只读自动变量**，别用来存路径。
3. **不要把这三个服务放在仓库外**（如 `E:\dev`）——受限沙箱下写不进去；放 `.tools/` 且已 gitignore。
4. **Qdrant 的存储目录是相对工作目录的 `storage/`**，所以必须 `cd` 到 exe 目录再启动，否则数据会散落在别处。
5. **`node_modules` 不可写时前端起不来**：`vue.config.js` 里加了 `DSH_NO_FS_CACHE=1` 开关（关闭 babel/webpack 磁盘缓存），只影响受限环境，正常 `start-frontend.bat` 不变。

# 温衡智造 · 红外测温枪 MES 系统

基于若依（RuoYi 3.8.2）+ KTG MES 改造的制造执行系统，覆盖红外测温枪从采购到货、生产装配、
质量检验到成品销售出库的全流程业务闭环。

> 需求依据：《红外测温枪MES系统_软件需求规格说明书》（对应 §2.3–2.9，共 REQ-001~025）

当前版本包含工单九环节进度跟踪、整单自动拣货、报工与过程检验联动、工单成品销售出库、智能助手及微信小程序端。
开发、部署、测试和设计资料的入口见 [项目资料索引](docs/README.md)。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 2.5.12 / MyBatis / Druid / Spring Security + JWT / Quartz |
| 前端 | Vue 2.6 / Element-UI 2.15 / ECharts 4.9 / Vue-CLI 4 |
| 数据库 | MySQL 8（库名 `j2eedb`，181 张表） |
| 中间件 | Redis（缓存/验证码/Token）、MinIO（文件上传） |
| 运行时 | JDK 8、Maven 3.6+；Node 20（测试及演示，前端构建需 OpenSSL 兼容参数） |

## 功能模块

```
首页       工作台（生产进度、图表汇总）
主数据     物料产品 / 物料分类 / 计量单位 / 客户 / 供应商 / 车间 / 工作站 / 多级BOM
仓储       仓库库区库位 / 到货通知 / 采购入库 / 生产领料退料 / 产品入库 / 销售出库 / 库存流水
生产       生产驾驶舱 / 生产工单 / 生产排产 / 生产报工 / 工序流转 / 工序 / 工艺流程 / 安灯
质量       检测项 / 质检方案 / 来料检验 / 过程检验 / 出货检验 / 退料检验 / 不良记录 / 批次追溯
设备       设备类型 / 设备台账 / 点检保养项目与方案 / 点检记录 / 保养记录 / 维修单 / 工装夹具
班组排班   班组设置 / 排班计划 / 班次 / 排班日历 / 节假日
系统       用户 / 角色 / 菜单 / 部门 / 岗位 / 字典 / 参数 / 编码规则 / 日志 / 定时任务
```

左侧图标导航切换业务域，业务页右侧显示「母目录 + 子菜单」两级分组。首页只保留图标导航，为工单流水线和图表留出空间。

## 快速开始

### 1. 准备中间件

```powershell
# MySQL 8（端口 3307，按实际修改 application-druid.yml）
# Redis（端口 6379，无密码；若设密码需同步改 application.yml）
# MinIO（端口 9100，桶 lx-mes）
```

### 2. 配置本地数据库连接

在项目根目录执行；已有 `application-druid.yml` 时直接修改现有文件，无须覆盖：

```powershell
$configDir = 'backend/ktg-mes/ktg-admin/src/main/resources'
Copy-Item "$configDir/application-druid.example.yml" "$configDir/application-druid.yml"
$env:MES_DB_USER = 'root'
$env:MES_DB_PASSWORD = '<本机数据库密码>'
```

示例默认连接 `localhost:3307/j2eedb`，也可通过 `MES_DB_URL` 覆盖。数据库配置文件、真实密钥、日志和本地备份不提交到 Git。
Redis、MinIO 等连接参数在 `application.yml` 中配置。可选的大模型密钥通过 `MES_AI_KEY` 和 `MES_AI_FB_KEY` 注入。
当前示例含本机演示用账户；部署到共享环境前需修改默认密码及 Token 签名密钥。

### 3. 初始化数据库

```powershell
mysql -h127.0.0.1 -P3307 -uroot -p -e "CREATE DATABASE IF NOT EXISTS j2eedb DEFAULT CHARACTER SET utf8mb4;"
mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p j2eedb -e "source sql/01_schema.sql"
mysql --default-character-set=utf8mb4 -h127.0.0.1 -P3307 -uroot -p j2eedb -e "source sql/02_data.sql"
```

详细云端部署步骤见 `sql/云端数据库导入说明.txt`。

### 4. 编译并启动后端

设置 `JAVA_HOME` 指向 JDK 8，在项目根目录打开终端执行：

```powershell
cd backend/ktg-mes
mvn -B -pl ktg-admin -am package
java -jar ktg-admin/target/ktg-admin.jar --server.port=8081
# 或 IDEA 运行 com.ktg.RuoYiApplication（JDK 8）
```

前端开发代理指向后端 `8081`；直接使用默认 `8080` 时需同步修改代理配置。
根目录的一键启动脚本保留本机中间件路径，使用前请按安装位置调整。
`start-backend-8081.bat` 是本机演示库专用入口，默认连接 `j2eedb_it`，与上述新建的 `j2eedb` 分开；
使用新初始化的数据库时按上方 `java -jar ... --server.port=8081` 命令启动，或设置 `MES_DB_URL` 显式覆盖脚本中的演示库地址。

### 5. 启动前端

另开终端，在项目根目录执行：

```powershell
cd ktg-mes-ui
npm ci
npm run dev
```

发布构建可在根目录运行 `build-frontend.bat`，脚本会为 Node 17+ 加入 webpack 4 所需的 `--openssl-legacy-provider` 参数。
生产环境需将 `/prod-api` 反向代理到后端。

### 6. 登录

- 地址：`http://localhost`
- 账号：`admin` / `admin123`

| 角色 | 账号 | 密码 |
|---|---|---|
| 工艺员 | craftsman | admin123 |
| 计划员 | planner | admin123 |
| 仓库员 | warehouse | admin123 |
| 生产操作员 | operator | admin123 |
| 质检员 | inspector | admin123 |
| 设备员 | equipment | admin123 |
| 班组长 | teamleader | admin123 |

## 演示与测试脚本

> 需要 Node 20；先在 `ktg-mes-ui` 安装依赖。以下命令在项目根目录执行。

不依赖运行中的后端或数据库的回归测试：

```powershell
node scripts/run-unit-tests.mjs
# 或在 ktg-mes-ui 目录运行 npm run test:unit
```

后端测试：

```powershell
cd backend/ktg-mes
mvn -B -pl ktg-admin -am test
```

以下脚本需要 MySQL、Redis 和后端运行；演示、CRUD 及上传脚本会写入业务数据，请使用演示库：

```powershell
node scripts\demo-flow.mjs      # 10 台整机全流程：到货→IQC→入库→工单→排产→领料→报工→IPQC→成品入库→OQC→销售出库
node scripts\smoke-crud.mjs     # 主数据增删改查 + 12 个核心查询接口冒烟
node scripts\check-api.mjs      # 菜单/上传/库存流水/主数据冒烟
node scripts\check-upload.mjs   # 本地存储 + MinIO 上传通道验证
```

`scripts/verify-full-flow.test.mjs` 是依赖固定演示数据的只读集成检查，不包含在离线测试入口中。
它要求 `demo-flow.mjs` 生成的三张工单、质检、入库及销售数据；已有多轮演示或人工业务数据的数据库不满足该固定快照。
默认检查后端 `8080`，使用 `8081` 时先设置 `$env:MES_BASE='http://127.0.0.1:8081'`。

演示历史数据（可选，用于驾驶舱图表更丰满）：

```powershell
mysql -h127.0.0.1 -P3307 -uroot -p j2eedb -e "source scripts/demo-history.sql"
```

## 目录说明

```
backend/ktg-mes/     后端 Maven 多模块工程（ktg-admin 为启动模块）
ktg-mes-ui/          前端 Vue 工程
ktg-mes-mp/          微信小程序端
sql/                 建库脚本、初始化数据、云端部署说明
scripts/             离线测试入口、业务回归测试、演示与冒烟脚本
docs/                项目资料索引、实施计划及设计说明
改造与验证说明.md     需求条目（REQ-001~025）与实现的逐项对照、接口变更记录
```

## 版本记录

### 2026-11-01

- 工单进度覆盖确认、排产、备料、领料、报工、过程检验、完工、产品入库和销售完成九个环节。
- 增加整单自动拣货、工单成品选择、报工批次范围校验、生产链路操作及排班查询改进。
- 更新温衡智造登录页、图标、字体和首页布局；收窄首页导航右侧留白；生产驾驶舱支持全屏、按钮退出和 Esc 退出。
- 增加本地数据库配置示例、统一离线测试入口及前端环境配置，锁定 npm 依赖；排除本地备份和验证产物。

### 2026-10-24 v2

- 新增「生产驾驶舱」：工单进度、库存水位、设备状态、产量趋势四图，30 秒自动刷新，数据全部取自真实业务接口
- 界面改版：左侧 8 域图标导航 + 右侧母/子分组；登录页、首页工作台、驾驶舱使用测温枪实物图 + 渐变遮罩，卡片毛玻璃效果
- 默认头像切换为 `user.png`，历史头像路径失效时自动回退，不再出现碎图
- 新增云端部署脚本：`sql/01_schema.sql`（181 张表）、`sql/02_data.sql`（初始化数据）、导入说明
- 新增驾驶舱演示历史数据脚本 `scripts/demo-history.sql`（14 天报工、4 种工单状态、多仓库存）
- 修复：排产接口未回填工作站编码导致 500；库存流水查询引用不存在字段；产品入库/销售出库删除校验取反；前端两处接口路径拼写错误
- 工程优化：`CompressionPlugin` 仅生产启用（修复 Node 17+ 的 OpenSSL md4 报错）；gantt/echarts/hiprint 拆异步 chunk

### 2026-10-24 v1

- 按需求书完成测温枪业务数据建模与初始化：17 个物料产品、16 行多级 BOM、9 道工序、3 条工艺路线、9 个工作站、4 仓库/9 库区/9 库位、10 检测项、4 套检验方案、5 台设备、2 班组，8 类岗位账号
- 打通 10 台整机全流程业务闭环，库存流水与结存账实一致
- 新增库存流水查询接口与页面（原系统缺失）

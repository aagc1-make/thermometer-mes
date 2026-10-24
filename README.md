# 红外测温枪 MES 系统

基于若依（RuoYi 3.8.2）+ KTG MES 改造的制造执行系统，覆盖红外测温枪从采购到货、生产装配、
质量检验到成品销售出库的全流程业务闭环。

> 需求依据：《红外测温枪MES系统_软件需求规格说明书》（对应 §2.3–2.9，共 REQ-001~025）

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 2.5.12 / MyBatis / Druid / Spring Security + JWT / Quartz |
| 前端 | Vue 2.6 / Element-UI 2.15 / ECharts 4.9 / Vue-CLI 4 |
| 数据库 | MySQL 8（库名 `j2eedb`，181 张表） |
| 中间件 | Redis（缓存/验证码/Token）、MinIO（文件上传） |
| 运行时 | JDK 8、Node 16（前端 dev/build）、Node 18+（演示脚本） |

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

左侧为 8 个业务域的图标导航，右侧为「母目录 + 子菜单」两级分组，默认展开。

## 快速开始

### 1. 准备中间件

```powershell
# MySQL 8（端口 3307，按实际修改 application-druid.yml）
# Redis（端口 6379，无密码；若设密码需同步改 application.yml）
# MinIO（端口 9100，桶 lx-mes）
```

### 2. 初始化数据库

```powershell
mysql -h127.0.0.1 -P3307 -uroot -p -e "CREATE DATABASE IF NOT EXISTS j2eedb DEFAULT CHARACTER SET utf8mb4;"
mysql -h127.0.0.1 -P3307 -uroot -p j2eedb -e "source E:/JavaCode/sql/01_schema.sql"
mysql -h127.0.0.1 -P3307 -uroot -p j2eedb -e "source E:/JavaCode/sql/02_data.sql"
```

详细云端部署步骤见 `sql/云端数据库导入说明.txt`。

### 3. 启动后端

```powershell
cd backend/ktg-mes/ktg-admin
java -jar target/ktg-admin.jar
# 或 IDEA 运行 com.ktg.RuoYiApplication（JDK 8）
```

### 4. 启动前端

```powershell
cd ktg-mes-ui
nvm use 16
npm install
npm run dev
```

### 5. 登录

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

> 需要 Node 18+（脚本使用全局 fetch）：`nvm use 20`

```powershell
node scripts\demo-flow.mjs      # 10 台整机全流程：到货→IQC→入库→工单→排产→领料→报工→IPQC→成品入库→OQC→销售出库
node scripts\smoke-crud.mjs     # 主数据增删改查 + 12 个核心查询接口冒烟
node scripts\check-api.mjs      # 菜单/上传/库存流水/主数据冒烟
node scripts\check-upload.mjs   # 本地存储 + MinIO 上传通道验证
```

演示历史数据（可选，用于驾驶舱图表更丰满）：

```powershell
mysql -h127.0.0.1 -P3307 -uroot -p j2eedb -e "source E:/JavaCode/scripts/demo-history.sql"
```

## 目录说明

```
backend/ktg-mes/     后端 Maven 多模块工程（ktg-admin 为启动模块）
ktg-mes-ui/          前端 Vue 工程
sql/                 建库脚本、初始化数据、云端部署说明
scripts/             演示与冒烟脚本
改造与验证说明.md     需求条目（REQ-001~025）与实现的逐项对照、接口变更记录
```

## 版本记录

### 2026-10-24 v2（本次）

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

# 商业 MES 前端呈现与技术调研（附来源）

> 调研时点：2026-10。目的：给「苦糖果 MES（Vue2 + Element UI + RuoYi）」的界面升级做对标参考。
> 说明：所有「未核实」项均为公开渠道确实查不到，未做推测填充。

---

## 一、七家厂商逐家结论

| 厂商 | 前端技术线索（可验证证据） | 界面形态 | UI 设计语言 |
|---|---|---|---|
| **Siemens Opcenter** | ① Opcenter Execution V8.0 起改为「responsive framework」，UI 按设备/分辨率/横竖屏自适应，并新增 **HTML5 CAD 可视化控件**（浏览器内 3D 查看）[来源](https://blogs.sw.siemens.com/opcenter/whats-new-in-siemens-opcenter-executiion-electronics-v8/)；② Opcenter X Intosite 是纯浏览器 SaaS，「2D 布局 + 交互式 3D 模型 + 360° 全景」，零安装 [来源](https://blogs.sw.siemens.com/opcenter/introducing-opcenter-x-intosite/)。具体框架（Angular/React）**未核实** | 工位操作台、车间看板、3D 数字孪生/全景导航、移动端适配 | 亮色 + 高密度表格 + 统一组件框架；Intosite 3D 场景偏暗色沉浸（未核实） |
| **SAP Digital Manufacturing** | 云部署在 BTP [产品页](https://www.sap.com/products/scm/digital-manufacturing.html)；核心特征是 **POD Designer** —— 工位操作界面的拖拽式设计器 + POD Plugin 插件机制 [POD Designer](https://www.consilio-gmbh.de/en/solutions/sap-digital-manufacturing/dm-components/pod-designer)、[课程](https://www.classcentral.com/course/sap-learning-exploring-customization-in-sap-digital-manufacturing-497873)。是否 Fiori/UI5：业内通行说法成立，但**没有可引用的官方原文**——未核实 | 工位 POD、KPI 分析、磁贴式工作台 | 亮色、卡片磁贴 + 密集表格、强设计系统（Fiori 风格，未核实） |
| **DELMIA Apriso** | Screen Flow 的 **Client Mode 用 JavaScript 在浏览器端执行、单页应用架构**；HTML Layout Editor 用 `data-flx-bind` 做数据绑定，暴露 `Apr.js` / `SF` JS API；工位控件为 Grid / Tree / Visualization / vQDT / 3DPlay [腾讯云技术文](https://cloud.tencent.com.cn/developer/article/2322008) | 低代码屏幕流设计器 + 可配置工位屏、3D 作业指导、甘特（Visualization） | 亮色、密集表格 + 可配置布局 |
| **摩尔元数（Morewis）** | MC 核心制造平台引擎为「**低代码 + 云开发 + 开源**」，PaaS + SaaS 应用商城 + 工具交易市场 [36氪](https://news.qq.com/rain/a/20200421A04WPD00)。具体前端框架**未核实** | 低代码工业 App、自定义看板 | 未核实 |
| **盘古信息（Pangus IMS）** | 官网功能清单含「生产任务甘特图」「可视化看板」「自定义报表看板平台」[官网](https://www.pangus-ims.com/webProduct/ims_mom_mes.html?languType=2)。框架无证据（页面里的 `tab-react`/`tab-angular` 只是建站模板残留 id，**不能**当技术证据） | 车间建模、甘特排程、可视化看板 | 未核实 |
| **黑湖智造（Blacklake）** | 前端 JD 明确要求「**精通 React, Redux 技术栈**，有复杂前端系统设计经历」[猎聘 JD](https://www.liepin.com/job/1973472749.shtml)；官网为 Next.js（`/_next/image` 图片管线）[官网](https://www.blacklake.cn/) | 经营分析/生产总览驾驶舱、移动端（小工单 App）、AI Agent 任务面板 | 亮色、卡片 + 大数字 KPI，移动优先，营销级视觉 |
| **宝信软件 MES** | **未找到任何公开前端技术证据**（仅有 iPlat Java 平台资料） | 未核实 | 未核实 |

### 三条横向判断

1. **国际三巨头都把「工位界面可配置化」当核心资产**（Opcenter 的 responsive framework、SAP 的 POD Designer、Apriso 的 Screen Flow），而不是给每个客户写死页面。
2. **唯一有硬证据的前端栈是黑湖的 React/Redux**；国产 SaaS 普遍走 React，开源 MES 生态才偏 Vue。
3. **数字孪生已经商业化落地**（Opcenter X Intosite 的 3D + 全景 + 视觉标签），但它是**独立产品模块**，不是主界面的默认形态。

---

## 二、现代 MES 前端通用做法与技术取舍

### 1）大屏可视化选型

| 方案 | 适用场景 | 学习成本 | 主要坑 |
|---|---|---|---|
| [ECharts](https://echarts.apache.org/handbook/zh/) + `transform: scale` 自适应 | 90% 车间看板、产量/OEE/不良趋势 | 低（1–2 人日/屏） | 缩放导致字体模糊、容器 resize 需手动 `resize()`、非 1920 设计稿分辨率要按比例裁切 |
| [DataV](https://github.com/DataV-Team/DataV)（Vue2）/ datav-vue3 | 需要边框装饰、动效包装的「汇报型大屏」 | 低 | Vue2 版基本停更，Vue3 需换 datav-vue3，装饰组件与业务耦合重 |
| [AntV G2](https://g2.antv.antgroup.com/) / [G6](https://g6.antv.antgroup.com/) | G2：复杂交互图表；**G6：工艺路线图、设备拓扑、物料流向图** | 中 | G6 的布局/自定义节点是另一套心智模型；MES 里真正的价值在 **G6 而非 G2** |
| [Three.js](https://threejs.org/) 数字孪生 | 产线/车间 3D、AGV 实时定位 | 高（相机/材质/性能/Draco 压缩） | 模型资产比代码贵；建议「glTF + 现成模型 + 只做状态染色」，不要自建建模管线 |

### 2）低代码 / 元数据驱动

首选 **schema 驱动 + 自研渲染器**：一份 JSON 定义字段、校验、权限、列配置，**Web 表格、PDA 表单、大屏卡片共用同一份元数据**。这正是 Apriso Screen Flow 与 SAP POD 的路线。

要快速出活可用 [form-create](https://github.com/xaboy/form-create)（Vue 生态成熟、JSON 生成 + 动态联动），但复杂布局（工位分栏、扫码枪焦点流）仍需自研渲染器兜底。

判断依据：MES 的界面复杂度集中在「同一实体在不同终端的不同视图」，schema 化的收益远大于组件库选型。

### 3）甘特图排程

| 方案 | 特点 | 适用 |
|---|---|---|
| [dhtmlx-gantt](https://dhtmlx.com/docs/products/dhtmlxGantt/) | 功能最全（依赖连线、资源视图、拖拽改期、基线），**商用收费**、包体大 | 排程逻辑稳定后再上 |
| [frappe-gantt](https://frappe.io/gantt) | 极轻，只适合只读进度展示 | 演示/只读 |
| [vis-timeline](https://visjs.github.io/vis-timeline/) | 时间轴 + 分组 + 大量区间条，可自己叠拖拽交互 | **性价比最高，先用它验证业务** |
| 自研 canvas | 只在多资源约束的有限产能排程才值得 | 谨慎 |

### 4）Vue2 + Element UI → Vue3 迁移要点

组件库三选一：

- [Element Plus](https://element-plus.org/zh-CN/guide/migration.html)：迁移阻力最小，但 API 破坏性变更多（`visible`→`modelValue`、`filter`→插槽、icon 拆独立包、`el-table` 插槽语法变化）
- [Naive UI](https://www.naiveui.com/)：TS 友好、主题变量最强、暗色开箱即用，适合重做设计系统
- [Ant Design Vue](https://antdv.com/)：企业级表格/表单最完备，视觉偏「后台管理」

**真正的成本不在组件库**，而在：`filters` / `EventBus` / `$children` / `$listeners` 全部移除、vue-cli→Vite 重建构建链、Vue2 专属库（DataV 等）要替换、以及大量 `this.$refs` 命令式代码。中型 MES 项目经验估算 **2–4 人月**（经验判断，未核实）。

建议用 `@vue/compat` 做渐进迁移，而非一次性重写。

---

## 三、给 Vue2 开源 MES 的 5 条升级优先级

1. **先定设计 token（色板/间距/字号/表格密度 + 暗亮双主题），再谈换框架**。三巨头的差异在信息架构与密度规范，不在组件库；这一步 ROI 最高、零迁移风险。
2. **大屏先用 ECharts + scale 自适应做完 3 块核心看板**（产量/达成率、设备状态、异常），Three.js 数字孪生列为「有模型资产后」的二期项。
3. **把列表/表单/详情抽象成一份 schema + 渲染器**，让 PC 表格、PDA 触屏表单、看板卡片复用同一份元数据 —— 这是 Apriso/SAP 验证过的路线，也是相对商业产品最该补的短板。
4. **甘特排程先用 vis-timeline 或 frappe-gantt 做只读进度 + 简单拖拽**，别一上来就买 dhtmlx 或自研 canvas。
5. **Vue3 迁移走「新页面新栈、老页面渐进」**：新模块直接用 Vue3 + Element Plus + Vite，老模块用 `@vue/compat` 挂着跑，避免大爆炸式重写。

---

## 四、落到本项目（苦糖果 MES）的具体结论

结合对代码的实测（1150 个 Java 文件 / 305 个 Vue 页面 / 250 处手写 `el-table` / 264 处 `el-dialog`、0 处 `el-drawer` / 无大屏 / ECharts 4.9 + macarons 卡通主题 / 已有 hiprint 打印设计器 + uReport 报表设计器）：

| 优先级 | 动作 | 理由 |
|---|---|---|
| P0 | 设计 token + 暗亮双主题 | 零迁移风险，立刻统一 305 个页面的观感 |
| P0 | 生产/设备/异常 三块大屏（ECharts 5 + scale） | 出效果最快，可当设计基线 |
| P1 | `ProTable` + `DetailDrawer` 封装（schema 驱动） | 收敛 250 处表格 / 264 处弹窗，并为 PDA/触屏复用打基础 |
| P1 | 甘特换 vis-timeline（替换 trial 版 dhtmlx） | 顺带解决商业授权风险 |
| P2 | 工艺路线图用 AntV G6（`pro_route`/`pro_process` 数据现成） | 商业产品高频演示点，G6 是这类图的正解 |
| P3 | Vue3 + Vite 渐进迁移（`@vue/compat`） | 成本 2–4 人月，不阻塞业务 |

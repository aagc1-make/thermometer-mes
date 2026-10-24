# 苦糖果MES 竞争位势调研（开源 × 商业 × 能力差距）

> 调研时间：2026-10；星标/Issue 数为抓取时点快照。未标注来源的判断均已标「未核实」。

## 一、开源 MES / 制造执行系统横向对比

| 项目 | 技术栈 / 许可 / 热度 | 场景 | 核心能力 | 明显短板 |
|---|---|---|---|---|
| [苦糖果 ktg-mes](https://gitee.com/kutangguo/ktg-mes)（+[前端](https://gitee.com/kutangguo/ktg-mes-ui)） | Spring Boot 2.5 + Vue2 + Element UI + MySQL，RuoYi-Vue 前后端分离；MIT；Gitee 6.1K★ / 2.9K fork / 573 commits / 319 open issues；前端仓库**未声明许可证** | 中小离散 | 主数据、排产、报工、质量、排班日历、库存与条码、设备、报表大屏；uniapp 触屏 + PDA + Android | 单厂单租户；排产非有限产能；无 SPC/防错/审计追踪；无设备协议与 OEE 引擎；无 CI/单测 |
| [Odoo MRP](https://www.odoo.com/app/manufacturing) | Python + PostgreSQL；[社区版 LGPLv3 / 企业版专有](https://www.odoo.com/documentation/18.0/legal/licenses.html)；[55.1K★](https://github.com/odoo/odoo) | 离散 + 流程 | 有限产能 + 甘特、Shop Floor 平板、质检点、批次/序列号双向追溯、IoT Box、PLM/维护 | 强绑定 Odoo 全家桶；工序级 APS 浅；企业版功能另收费 |
| [ERPNext Manufacturing](https://frappe.io/erpnext/open-source-manufacturing-erp-software) | Python/Frappe；GPLv3；[40K★](https://github.com/frappe/erpnext) | 离散为主 | BOM/工单/工序卡、产能规划、质检、外协、批次追溯 | 无设备层/SFC 硬件生态；无 SPC；排产靠人工 |
| [frePPLe](https://frepple.com/editions/) | C++/Python；社区版开源 + [云/企业版闭源](https://frepple.com/editions/)；[763★](https://github.com/frePPLe/frepple) | 计划层（不限行业） | 有限物料 + 有限产能、需求预测、what-if 多版本、REST API、Odoo 双向集成 | 交互甘特图与换型矩阵优化**仅付费版**；不是 MES，无报工/质量 |
| [Apache OFBiz](https://ofbiz.apache.org/) | Java；Apache-2.0 | 通用 ERP | manufacturing 组件含 BOM/工艺路线/工单数据模型 | 无现代车间终端与 UI；社区投入低（活跃度未核实） |
| [Carbon](https://github.com/crbnos/carbon) | TypeScript + React Router 7 + Postgres/Supabase；[AGPLv3 CE + 商业版](https://docs.carbon.ms/docs/platform/licensing)；2.7K★ | HMLV/复杂装配 job shop | [ERP+MES+QMS 一套库](https://docs.carbon.ms/docs/building/architecture)、批次序列号追溯、多租户行级隔离、机床监控/OEE 集成 | 北美中小 job shop 取向；无 APS/SPC 深度；AGPL 网络传染 |
| 若依系 fork（[imcoder/smart-mes](https://gitee.com/imcoder/smart-mes)） | 与 ktg-mes 同构；AGPL-3.0；22 commits | 中小离散 | 目录即 ktg-mes / ktg-mes-ui / ktg-mes-pad | 说明生态以 fork 为主，授权已分叉（MIT→AGPL） |

## 二、商业 MES / MOM 分层

| 档位 / 产品 | 定位与客户 | 核心卖点 | 技术能力 | 公开价格 |
|---|---|---|---|---|
| 国际 · [Siemens Opcenter](https://www.siemens.com/en-us/products/opcenter/) | 中大型集团，汽车/电子/流程；MOM 全组合 | 数字孪生闭环（PLM↔自动化）、MES/QMS/APS/EMI/厂内物流一体；Opcenter X 覆盖 SMB 的 SaaS | ISA-95 分层、多工厂、SPC、设备集成、排程 | 未公开（项目制） |
| 国际 · [SAP Digital Manufacturing](https://www.sap.cn/products/scm/digital-manufacturing.html) | 大型集团，已用 S/4HANA | BTP 云原生 MOM，与计划/物流同源，POD 操作员指导、AI KPI | 多工厂/多语言、与 ERP 主数据一致、云多租户 | 未公开 |
| 国际 · [DELMIA Apriso](https://www.3ds.com/products/delmia/apriso) | 全球多工厂推广 | 多工厂模板复制、与 3DEXPERIENCE/PLM 打通 | 低代码流程建模、ISA-95、SPC/追溯 | 未公开 |
| 国内 · [摩尔元数](https://www.morewis.com/) | 集团 + 中小双线（宣称 600+ 集团、30000+ 中小企业） | AI MOM/MES/WMS/[VPS 排程](https://www.morewis.com/maps.html)/QMS + N2Cloud 轻量版；“配置即开发” | 低代码、11+ 行业模板、设备互联、多厂协同 | 项目制 |
| 国内 · [盘古信息](https://www.pangus-ims.com/webProduct/ims_mom_mes.html) | 3C/电子/汽零中大型 | 排程甘特、4M 多级防错、WIP、追溯、AGV/JIT 供料 | 车间建模、工序约束、看板 | 项目制 |
| 国内 · [宝信软件](https://product.baosight.com/products/4177) | 钢铁/流程行业 | 流程行业 MES 龙头，产销一体 | 流程建模、L2/L3 集成 | 项目制 |
| SaaS · [黑湖智造](https://www.blacklake.cn/) | 20~150 人成长型工厂 | 小工单快速上线（1~4 周），SaaS 按年 | 报工/进度/库存/绩效；旗舰版加采购/销售/质量/PDA | **专业版 ¥10,800/年、旗舰版 ¥18,800/年**（含 50 账号，增购 ¥140/账号/年，纯 SaaS 不支持本地部署）[来源](https://blog.blacklake.cn/2026-nian-hei-hu-xiao-gong-dan-jia-ge-jie-xi-zhuan-ye-ban-yu-qi-jian-ban-de-cha-yi-dui-bi/) |

## 三、关键能力差距清单（可落地视角）

| 能力域 | 开源小 MES 现状 | 商业基线 | 可落地动作 |
|---|---|---|---|
| 标准建模 | 自定义“工作站/工序”表，无对象模型 | [ISA-95](https://www.isa.org/standards-and-publications/isa-standards/isa-standards-committees/isa95) 层级（设备/工段/产线/工厂）、ISA-88 配方、B2MML 报文 | 先做 Equipment/WorkUnit/ProductionOrder 三层映射，导出 B2MML 兼容 XML |
| 设备接口 | 无协议栈 | [OPC UA](https://opcfoundation.org/about/opc-technologies/opc-ua/)、[MTConnect](https://www.mtconnect.org/)、Modbus 网关 | 独立边缘采集服务（Java/Node）+ 点位模板，不与业务库耦合 |
| 排产 | 手工排产页 + 日历 | 有限产能 APS、工序级排程、甘特拖拽、换型/模具约束 | 不做自研求解器：接 frePPLe（有限产能 + REST），前端补甘特交互 |
| 质量 | 检验单记录 | SPC 控制图、Cpk/Ppk、Poka-Yoke、GB/T 2828.1 抽样、MRB 处置 | 先做“检验项 + 抽样方案 + 判异规则(8 准则)”三张表 + 控制图 |
| 追溯 | 批次号生成 | 批次/序列号/条码正反向追溯、谱系树 | 统一 `trace_event` 事件表（收/发/转/报工/报废），一次投入长期复用 |
| 设备绩效 | 无 | [OEE](https://www.oee.com/)=A×P×Q、DT/MTBF、Andon | 由报工 + 停机原因码算 OEE，先出日报再上大屏 |
| 架构工程 | 单租户、部门级数据权限 | 多租户、行级隔离、审计追踪（[21 CFR Part 11](https://www.fda.gov/regulatory-information/search-fda-guidance-documents/part-11-electronic-records-electronic-signatures-scope-and-application)/GMP）、i18n、可观测性 | 加 `tenant_id` + 拦截器；操作日志升级为不可篡改审计（哈希链）；容器化 + CI + OpenAPI |
| 商业形态 | Word/PPT 实施文档 | 行业模板包、低代码配置、ERP/PLM/WMS 连接器、实施方法论 | 先固化 2 个行业模板（注塑/机加），再谈平台化 |

## 四、最值得投入的 5 个方向

1. **把“追溯 + 报工 + 防错”做成可验收的硬能力**：事件表驱动的正反向追溯 + 工序级防错，是甲方验收与召回审计的刚需，投入产出比最高。
2. **设备联机优先于 APS**：OPC UA/Modbus 边缘网关 + OEE/停机分析，2~4 周可交付，直接对应“设备利用率”这一买单理由。
3. **APS 用集成而非自研**：frePPLe 社区版已提供有限产能与 REST，补齐甘特交互即可对标商业排程演示效果。
4. **工程化决定能否卖给集成商**：多租户 + 行级权限 + 审计追踪 + 容器化 + CI/单测 + OpenAPI，是把项目制改造成产品的分水岭。
5. **商业化对标黑湖价格带**：1~2 万元/年/50 账号的 SaaS 锚点已存在，开源版应走“私有化 + 行业模板 + 实施服务”，避免与 SaaS 正面拼价格。

> 许可风险：ktg-mes 为 MIT，但 [ktg-mes-ui 未声明许可证](https://gitee.com/kutangguo/ktg-mes-ui)，商用前须与作者确认；若基于 AGPL 的 fork 二次开发，网络分发需开源。

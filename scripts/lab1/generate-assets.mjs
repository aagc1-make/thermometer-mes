import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const out = path.join(root, 'docs/lab1');
fs.mkdirSync(out, { recursive: true });
const spec = 'MES 到货至销售出库需求';
const requirements = [
  ['REQ-002', '物料基础数据管理', '2.3.4', '统一维护物料分类、产品和物料主数据。抽测测温枪成品、半成品和外购物料的查询及编码唯一性。'],
  ['REQ-003', '多级 BOM 管理', '2.3.6', '维护产品组成并支持多级物料需求核算。抽测三层结构和生产 10 台时的物料用量换算。'],
  ['REQ-007', '采购到货与入库管理', '2.4.4', '到货与 IQC 合格放行后才可采购入库，库存及流水一致；重复执行不得重复过账。'],
  ['REQ-008', '生产领料与退料管理', '2.4.6', '本轮只测生产领料，核对原料仓转出与线边库转入、FIFO 自动拣货及库存不足拦截；退料不在范围内。'],
  ['REQ-012', '生产工单与排产管理', '2.5.4', '组织生产任务并按工序、工作站和时间排产，校验计划数量。'],
  ['REQ-013', '生产报工 工序流转与进度查询', '2.5.6', '记录生产执行并回写进度，抽测正常报工与合格、不合格数量一致性；不做全部工序流转卡组合。'],
  ['REQ-015', '来料检验管理', '2.6.4', '控制来料质量和采购入库放行，校验检测数量与合格、不合格数量的一致性。'],
  ['REQ-016', '过程与出货检验管理', '2.6.6', '控制过程质量及出货质量，抽测 IPQC 合格放行和 OQC 不合格拦截。'],
  ['REQ-009', '成品入库与销售出库管理', '2.4.8', '合格成品入库 10 台，OQC 合格后销售出库 6 台，在无其他业务变动时结存 4 台。'],
  ['REQ-010', '库存现有量与批次查询', '2.4.10', '按物料、批次和库位查询库存及流水；不存在的批次返回空结果，不产生写操作。'],
].map(([id, title, section, description]) => ({ id, title, section, description }));

const cases = [];
function add(suite, req, kind, title, pre, actions, expected, source) {
  const id = cases.length + 1;
  cases.push({ id: `MES-${id}`, suite, req, kind, title, pre, actions, expected, source });
}
add('01 主数据', 'REQ-002', '正向', '查询测温枪主数据', '已在隔离测试库执行初始化脚本。',
  ['打开主数据中的物料产品管理，按 TG- 编码前缀查询。', '查看 TG-FG-001 及外购件详情，核对名称、分类、单位和启用状态。'],
  '可识别初始化的 17 个测温枪相关物料产品；成品、半成品和外购件分类及计量单位与初始化数据一致。', 'md/controller/MdItemController.java');
add('01 主数据', 'REQ-002', '异常', '重复物料编码被拒绝', 'TG-FG-001 已存在；仅在隔离测试库操作。',
  ['新建物料，编码填 TG-FG-001，名称填 LAB1-重复编码，其他必填项合法。', '保存，随后重新查询 TG-FG-001 的记录数。'],
  '提示物料编码已存在；原记录不变且不新增重复记录。若异常保存成功，记录新 ID 并在测试库中清理。', 'md/controller/MdItemController.java');
add('01 主数据', 'REQ-003', '正向', '查看整机三级 BOM', 'TG-FG-001、两种自制半成品和 BOM 已初始化。',
  ['打开整机的 BOM，展开电控和手柄半成品。', '与 sql/thermometer_init.sql 中 BOM 定义逐项核对。'],
  '整机、半成品、外购件三层结构可查询；初始化 16 行 BOM 的子件和单位用量一致。', 'md/controller/MdProductBomController.java');
add('01 主数据', 'REQ-003', '正向', '十台整机用料数量换算', '已准备隔离的 LAB1 工单，产品 TG-FG-001，计划 10 台。',
  ['查看工单 BOM 或物料需求，按多级 BOM 核对外购件需求。', '分别核对电池、说明书、彩盒及其他外购件的单位用量乘以计划数量。'],
  '按初始化 BOM 核算为 14 种外购件，合计 150 个业务计量单位；不得漏掉说明书和彩盒。不同单位合计仅用于本固定样本核对。', 'pro/controller/ProWorkorderController.java');
add('02 仓储', 'REQ-007', '正向', 'IQC 合格后采购入库', '有带 LAB1 前缀的到货行，IQC 合格且有对应入库单；记录目标物料入库前库存及流水数。',
  ['核对采购入库行的到货来源、合格数量、批次及库位。', '按页面流程确认并执行采购入库，再查库存和来源单据流水。'],
  '入库单完成；原料仓目标批次数量增加本次实收量，新增流水与单据和批次关联，无额外重复过账。', 'wm/service/impl/ItemRecptWorkflowService.java');
add('02 仓储', 'REQ-007', '异常', '采购入库重复执行不重复过账', 'MES-5 已完成；保存相同单据的库存及流水基线。',
  ['对同一已执行采购入库单重试执行；界面无入口时，可在测试库重放该执行请求。', '再次查询相同单据的库存及流水。'],
  '重复执行被拒绝或作为幂等无操作；单据对应库存和流水均不再增加。按钮隐藏只能证明界面约束，接口未重放应在实际记录中注明。', 'wm/service/impl/ItemRecptWorkflowService.java');
add('02 仓储', 'REQ-008', '正向', '自动拣货与领料双边过账', '工单已确认并排产，存在领料草稿；正常链路可用库存足够，不额外制造多批次数据。',
  ['对领料单执行一键拣货，核对选中批次和需求数量。', '确认并执行领料，查询原料仓、线边库和来源单据流水。'],
  '拣货数量满足需求且不超过批次可用量；原料仓减少量等于线边库增加量，同一领料单保留双边流水。', 'wm/service/impl/IssueAutoPickService.java');
add('02 仓储', 'REQ-008', '异常', '库存不足整单拣货失败', '另建 LAB1-NEG 领料草稿；某物料需求大于可用库存，记录已有拣货明细。',
  ['执行一键拣货。', '重查此单所有拣货明细及库存。'],
  '提示可用库存不足及缺料信息；整单不新增部分拣货，库存不变且不出现负库存。', 'wm/service/impl/IssueAutoPickService.java');
add('03 生产', 'REQ-012', '正向', '十台工单确认并排产', '客户、产品、工艺路线和工作站已初始化；使用本轮唯一 LAB1 前缀。',
  ['新建 TG-FG-001 工单，计划 10 台并确认。', '按既定路线及工作站排产，查看工序任务和数量。'],
  '工单产品和计划数量正确，生成对应工序任务且关联工作站；保存工单及任务编号供后续领料、报工引用。', 'pro/controller/ProWorkorderController.java');
add('03 生产', 'REQ-012', '异常', '非正数工单数量校验', '另建 LAB1-NEG 工单草稿；不更改正常链路工单。',
  ['将计划数量设为 0 并保存或确认。', '记录页面及接口响应，核查工单和排产任务是否被创建。'],
  '非正数计划数量被明确拒绝且不生成有效生产任务；若系统接受，作为待核实缺陷记录实际行为，不预先判定通过。', 'pro/controller/ProWorkorderController.java');
add('03 生产', 'REQ-013', '正向', '正常报工回写生产进度', '生产任务已排产、领料完成；前置工序及必要检验已满足，当前任务尚未完成。',
  ['对当前任务录入合法的本次产出与合格、不合格数量。', '提交报工，查询任务进度、工单累计产出及来源记录。'],
  '只计入本次合法报工数量；任务和工单进度按既定规则更新，需检工序产生可追溯待检记录。', 'pro/controller/ProFeedbackController.java');
add('03 生产', 'REQ-013', '异常', '报工数量不平衡被拒绝', '另备 LAB1-NEG 报工草稿，当前任务允许报工 10 件；记录进度和库存。',
  ['填报工数 10、合格数 8、不合格数 3，保存并尝试执行。', '检查实际响应和提交后的进度、库存及报工单状态。'],
  '执行时明确拒绝数量不一致；不得增加生产进度或生成产出库存流水，草稿若允许保存须记录其仍不可执行。', 'pro/controller/ProFeedbackController.java');
add('04 质量', 'REQ-015', '正向', 'IQC 合格数量正确回写', '正常链路到货已提交待检，生成 IQC 草稿；尚未采购入库。',
  ['填写检测数、合格数、不合格数及检测项结果，使数量平衡。', '完成检验，查询到货行及采购入库草稿。'],
  '检验完成并合格放行，来源行质量状态及可入库合格量一致；记录 IQC 单号供 MES-5 引用。', 'qc/service/impl/QcIqcCompletionService.java');
add('04 质量', 'REQ-015', '异常', 'IQC 数量不平衡被拒绝', '另备 LAB1-NEG IQC 草稿，来源到货量 10，记录原状态。',
  ['设置检测数 10、合格数 8、不合格数 3，尝试完成。', '重新查询检验及到货行质量状态。'],
  '提示数量不一致并拒绝完成；来源行不被放行，库存不变。', 'qc/service/impl/QcIqcCompletionService.java');
add('04 质量', 'REQ-016', '正向', '关键工序 IPQC 合格放行', '正常关键工序完成报工并形成待检任务，存在对应 IPQC。',
  ['核对 IPQC 的工单、工序和来源报工，填写平衡的检测数量及合格结果。', '完成检验，查看来源任务及后续流转状态。'],
  'IPQC 完成且合格，来源记录关联正确；后续工序按实际状态机获得放行资格，数量未被重复累计。', 'qc/service/impl/QcIpqcServiceImpl.java');
add('04 质量', 'REQ-016', '异常', 'OQC 未合格禁止销售出库', '另备独立 LAB1-NEG 销售单，拣货数量正确但 OQC 未合格，保存库存及流水基线。',
  ['尝试将销售单推进到可执行并执行出库；界面阻止时记录提示。', '重查销售单状态、成品库存和流水。'],
  '质量未合格时不能完成出库；成品库存及流水不变。该负例单不用于后续正常销售，避免污染链路。', 'wm/controller/WmProductSalesController.java');
add('05 销售与追溯', 'REQ-009', '正向', '十台合格成品入库', '正常链路整机产出 10 台且必要检验已通过；记录目标成品仓及批次初始量。',
  ['核对成品入库单来源工单、产品、合格量和成品仓库位。', '按页面流程执行入库并查询库存及流水。'],
  '本批成品仓增加 10 台，入库来源、批次及流水一致；销售前该批可用量为 10 台。', 'wm/controller/WmProductRecptController.java');
add('05 销售与追溯', 'REQ-009', '正向', '销售六台后本批结存四台', 'MES-17 已完成，无其他业务变动；客户及发货通知正常，独立正常销售单的 OQC 已合格。',
  ['选择本轮成品批次拣货 6 台，核对出库量与拣货量一致。', '按状态流程执行销售出库，查询本批成品仓库存及销售流水。'],
  '销售单完成，新增本批 -6 的销售出库流水；本批成品仓由 10 台降至 4 台，不能把其他批次合计当成本批结果。', 'wm/controller/WmProductSalesController.java');
add('05 销售与追溯', 'REQ-010', '正向', '按批次查询库存及来源流水', '正常采购、领料、报工、成品入库和销售单号已记录。',
  ['按本轮成品批次查询现有量和库存流水，打开来源单据。', '分别核对关联工单、领料批次及原料采购来源；记录能实际查到的关联路径。'],
  '查询结果仅属于目标物料和批次，数量与单据一致；仅以实际来源字段证明追溯，缺失关联单独记录，不凭批次命名推断完整闭环。', 'wm/controller/WmMaterialStockController.java');
add('05 销售与追溯', 'REQ-010', '异常', '不存在批次返回空结果', '选取确认不存在的批次 LAB1-NOT-EXIST-本轮后缀。',
  ['以该批次精确查询库存现有量及流水。', '检查查询响应、页面提示及数据库是否产生写操作。'],
  '返回空列表或明确无记录，无服务错误，不展示其他批次数据；查询不生成单据或库存流水。', 'wm/controller/WmMaterialStockController.java');

add('01 主数据', 'REQ-003', '异常', '产品不能作为自身 BOM 子件', '保存原 BOM 行数；使用已存在的 TG-FG-001。',
  ['提交 itemId 与 bomItemId 均为 9201 的 BOM 新增请求，其他字段合法。', '核对响应及原 BOM，清理异常接受的测试行。'],
  '明确拒绝自引用 BOM；BOM 行数和原结构不变。', 'md/controller/MdProductBomController.java');
add('01 主数据', 'REQ-002', '异常', '物料名称为空时拒绝保存', '使用独立 LAB1 编码，其他字段与已有合法物料相同。',
  ['提交名称为空字符串的新增物料请求。', '查询测试编码，确认无新增。'],
  '返回名称必填提示；不生成空名称物料。', 'md/controller/MdItemController.java');
add('02 仓储', 'REQ-008', '边界', '自动拣货重复调用不重复分配', '合法领料单已自动拣货，尚未执行。',
  ['记录拣货明细行数、数量与库存，重放相同自动拣货请求。', '重新核对明细和库存。'],
  '不新增重复分配；原拣货数量和库存不变。', 'wm/controller/WmIssueHeaderController.java');
add('03 生产', 'REQ-013', '异常', '已执行报工再次执行不重复过账', '正常报工已执行；记录任务进度、库存和流水。',
  ['重放同一报工的执行请求。', '核对任务已生产量及库存流水。'],
  '拒绝重复执行或幂等返回，进度和库存流水不增加。', 'pro/controller/ProFeedbackController.java');
add('03 生产', 'REQ-012', '边界', '负数量工单拒绝创建', '独立 LAB1-NEG 工单编码，产品及客户合法。',
  ['提交计划数量 -1 的工单新增请求。', '核对工单及 BOM，记录实际接受情况。'],
  '拒绝负数量；不生成工单及负数量 BOM。', 'pro/controller/ProWorkorderController.java');
add('05 销售与追溯', 'REQ-009', '异常', '销售出库重复执行不重复扣库', '正常销售 6 台已完成，本批成品仓余 4 台。',
  ['重放同一销售单执行请求。', '核对本批库存及销售流水。'],
  '拒绝重复执行或幂等返回；本批仍为 4 台且流水不重复。', 'wm/controller/WmProductSalesController.java');
add('05 销售与追溯', 'REQ-010', '权限', '未登录不能查询库存', '不携带 Authorization；需求 §3.3 访问控制抽样。',
  ['无令牌请求库存列表。', '检查业务响应是否拒绝且未泄露库存记录。'],
  '拒绝访问，响应不包含库存业务记录。', 'wm/controller/WmMaterialStockController.java');
add('05 销售与追溯', 'REQ-010', '权限', '退出后旧令牌不能查询库存', '隔离环境取得有效测试会话。',
  ['调用退出接口使会话失效。', '使用同一旧令牌请求库存列表。'],
  '退出成功，旧令牌被拒绝且无库存记录。', 'wm/controller/WmMaterialStockController.java');
if (requirements.length !== 10 || cases.length !== 28) throw Error('用例规模应为 28');
for (const req of requirements) if (cases.filter(c => c.req === req.id).length < 2) throw Error(`${req.id} 覆盖不足`);
for (const c of cases) if (!fs.existsSync(path.join(root, 'backend/ktg-mes/ktg-mes/src/main/java/com/ktg/mes', c.source))) throw Error(`代码路径不存在：${c.source}`);
const esc = s => s.replaceAll('&', '&amp;').replaceAll('"', '&quot;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');
const cd = s => `<![CDATA[${s.replaceAll(']]>', ']]]]><![CDATA[>')}]]>`;
const p = s => `<p>${esc(s)}</p>`;
const write = (name, body) => fs.writeFileSync(path.join(out, name), body, 'utf8');
write('lab1-data.json', JSON.stringify({ baseline: 'main@909b875', spec, requirements, cases }, null, 2) + '\n');
write('testlink-requirements.xml', '<?xml version="1.0" encoding="UTF-8"?>\n<requirement-specification>\n' +
  `<req_spec title="${esc(spec)}" doc_id="LAB1-MES"><revision>1</revision><type>2</type><node_order>1</node_order><total_req>10</total_req><scope>${cd(p('主干 main@909b875；到货至销售出库链路；10 条需求、28 条 API 用例、1 个计划和 1 轮执行。仅覆盖下述需求中的抽样规则。'))}</scope>\n` +
  requirements.map((r, i) => `<requirement><docid>${cd(r.id)}</docid><title>${cd(r.title)}</title><version>1</version><revision>1</revision><node_order>${i + 1}</node_order><description>${cd(p(`来源：需求规格说明书 §${r.section}，原编号 ${r.id}。`) + p(r.description))}</description><status>V</status><type>3</type><expected_coverage>${cases.filter(c => c.req === r.id).length}</expected_coverage></requirement>`).join('\n') + '\n</req_spec>\n</requirement-specification>\n');
const suites = [...new Set(cases.map(c => c.suite))];
write('testlink-cases.xml', '<?xml version="1.0" encoding="UTF-8"?>\n<testsuite name="">\n<details>' + cd(p('5 个套件共 28 条：正向12、异常12、边界2、权限2。导入素材只包含设计，实际结果另见执行记录。执行顺序见 README。')) + '</details>\n' + suites.map((suite, si) =>
  `<testsuite name="${esc(suite)}"><node_order>${si + 1}</node_order><details>${cd(p(suite))}</details>\n` + cases.filter(c => c.suite === suite).map(c => {
    const n = Number(c.id.split('-')[1]); const r = requirements.find(r => r.id === c.req);
    return `<testcase name="${esc(c.title)}"><node_order>${n}</node_order><externalid>${n}</externalid><version>1</version><summary>${cd(p(`${c.id}；${c.kind}；${c.req}；主干 main@909b875。`) + p(`代码落点：com/ktg/mes/${c.source}`))}</summary><preconditions>${cd(p('仅在已核验数据源的隔离 MES 测试库执行。') + p(c.pre))}</preconditions><execution_type>2</execution_type><importance>2</importance><steps>` +
      c.actions.map((a, i) => `<step><step_number>${i + 1}</step_number><actions>${cd(p(a))}</actions><expectedresults>${cd(p(i === c.actions.length - 1 ? c.expected : '完成操作并记录本轮单据、输入及响应。'))}</expectedresults><execution_type>2</execution_type></step>`).join('') +
      `</steps><requirements><requirement><req_spec_title>${cd(spec)}</req_spec_title><doc_id>${cd(c.req)}</doc_id><version>1</version><title>${cd(r.title)}</title></requirement></requirements></testcase>`;
  }).join('\n') + '\n</testsuite>').join('\n') + '\n</testsuite>\n');
let records = '# 实验一执行记录\n\n基线 main@909b875；新建项目导入后 28 条初始未执行；实际执行结果另外归档。执行时填写实际结果、单据和证据路径，负向输入被正确拦截也应记为通过。\n\n## 表一 需求覆盖矩阵\n\n| 需求 | 名称 | 来源章节 | 用例 | 设计覆盖 |\n|---|---|---|---|---|\n';
records += requirements.map(r => `| ${r.id} | ${r.title} | §${r.section} | ${cases.filter(c => c.req === r.id).map(c => c.id).join('、')} | ${cases.filter(c => c.req === r.id).length} 条已设计，未验证 |`).join('\n');
records += '\n\n设计映射覆盖 10/10 条选定需求，只代表每条需求有对应用例；不代表完整需求覆盖或实测通过率。\n\n## 表二 用例执行清单\n\n| 编号 | 需求 | 类型与标题 | 预期 | 结果 | 实际与证据 |\n|---|---|---|---|---|---|\n';
records += cases.map(c => `| ${c.id} | ${c.req} | ${c.kind} ${c.title} | ${c.expected} | 未执行 | 待实测 |`).join('\n');
records += '\n\n## 表三 执行统计\n\n| 总用例 | 通过 | 失败 | 阻塞 | 未执行 | 已执行通过率 |\n|---|---|---|---|---|---|\n| 28 | 0 | 0 | 0 | 28 | 暂不计算 |\n\n仅按最终状态计数，合计应为 28。执行率为（通过+失败+阻塞）/28；已执行通过率按通过/（通过+失败）计算，分母为 0 时不计算。失败项当场复测，保留首次结果和复测说明，不建立 R2。\n\n## 表四 缺陷清单\n\n当前未进行业务测试，尚无已确认业务缺陷；下表供发现真实问题后填写，不预置 3～5 条。\n\n| 缺陷编号 | 用例 | 标题与复现步骤 | 期望与实际 | 严重度 | 证据 | 状态与复测 |\n|---|---|---|---|---|---|---|\n\n缺陷记录应包含基线、环境、输入、操作、实际响应和可重复性。配置阻塞和需求疑问先分别标记，确认后再作为业务缺陷。\n';
// Exporting design assets must not erase evidence from a completed execution.
if (!fs.existsSync(path.join(root, 'evidence/lab1/run-20261107/results.json'))) write('执行记录.md', records);
console.log(`已生成 ${requirements.length} 条需求、${cases.length} 条用例、${suites.length} 个套件；正向 ${cases.filter(c => c.kind === '正向').length}，异常 ${cases.filter(c => c.kind === '异常').length}，边界 ${cases.filter(c => c.kind === '边界').length}，权限 ${cases.filter(c => c.kind === '权限').length}。`);

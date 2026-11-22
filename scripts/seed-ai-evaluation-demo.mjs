import { writeFile, rename } from 'node:fs/promises';
import { resolve } from 'node:path';
import { setTimeout as delay } from 'node:timers/promises';

// Run against the isolated thermometer demo only. A pre-issued administrator token can be used
// when the demo login requires a CAPTCHA.
const base = new URL(process.env.MES_DEMO_URL || 'http://127.0.0.1:18082');
const output = process.argv[2];
const departments = [
  { id: 103, name: '研发演示知识库', prefix: '研发配套核对', intro: '用于研发核对启用的组件清单。' },
  { id: 107, name: '运维演示知识库', prefix: '运维备件核对', intro: '用于演示备件信息的定位和交接。' },
  { id: 113, name: '测试演示知识库', prefix: '测试样机核对', intro: '用于样机验证时核对计划配套组件。' }
];
const category = 'BOM 说明';
const version = 'AI-EVAL-V1';
const productCodes = new Set(['TG-FG-001', 'TG-SF-001', 'TG-SF-002']);
let token = process.env.MES_DEMO_TOKEN;

async function request(path, { method = 'GET', data } = {}) {
  const form = data instanceof FormData;
  const headers = token ? { Authorization: 'Bearer ' + token } : {};
  if (data && !form) headers['Content-Type'] = 'application/json';
  const response = await fetch(new URL(path, base), {
    method, headers, body: form ? data : data && JSON.stringify(data), signal: AbortSignal.timeout(30000)
  });
  if (!response.ok) throw new Error(`${method} ${path} HTTP ${response.status}`);
  return response;
}

async function api(path, options) {
  const result = await (await request(path, options)).json();
  if (result.code !== 200) throw new Error(`${path} ${result.msg}`);
  return result;
}

async function signedIn() {
  if (token) return;
  const username = process.env.MES_DEMO_USER;
  const password = process.env.MES_DEMO_PASSWORD;
  if (!username || !password) throw new Error('请提供演示库管理员凭据或 MES_DEMO_TOKEN。');
  token = (await api('/login', { method: 'POST', data: { username, password } })).token;
}

async function list(path, params = {}) {
  const result = [];
  const pageSize = 100;
  for (let pageNum = 1; pageNum <= 20; pageNum++) {
    const query = new URLSearchParams({ ...params, pageNum, pageSize });
    const page = await api(`${path}?${query}`);
    result.push(...page.rows);
    if (result.length >= page.total || page.rows.length < pageSize) return result;
  }
  throw new Error(`${path} 超出本次演示数据范围。`);
}

async function prepareSamples() {
  const orders = (await list('/mes/pro/workorder/list')).filter(order =>
    productCodes.has(order.productCode) && ['CONFIRMED', 'FINISHED'].includes(order.status)
    && /^MO\d+$/.test(order.workorderCode)
  ).sort((a, b) => a.workorderId - b.workorderId);
  const products = new Map(orders.map(order => [order.productCode, order.productId]));
  const bills = new Map(await Promise.all([...products].map(async ([code, id]) => {
    const rows = await list('/mes/md/bom/list', { itemId: id, enableFlag: 'Y' });
    return [code, rows.filter(item => item.itemOrProduct === 'ITEM').sort((a, b) => a.bomItemCode.localeCompare(b.bomItemCode))];
  })));
  const candidates = new Map();
  for (const order of orders) {
    for (const item of bills.get(order.productCode)) {
      const source = 'Item:' + item.bomItemCode;
      const target = 'Workorder:' + order.workorderCode;
      candidates.set(source + '|' + target, { source, kind: 'USED_IN', target, item, order });
    }
  }
  const relations = [...candidates.values()].slice(0, 100);
  if (relations.length !== 100 || orders.length < 6 || products.size !== 3) {
    throw new Error('演示库缺少足够的已确认测温枪工单和 BOM，未导入资料。');
  }

  const stockCodes = ['TG-EL-001', 'TG-EL-002', 'TG-EL-003', 'TG-EL-006', 'TG-SF-001', 'TG-SF-002', 'TG-FG-001', 'TG-ST-001'];
  const receiptCodes = ['TG-EL-001', 'TG-EL-002', 'TG-EL-003', 'TG-EL-004', 'TG-EL-006', 'TG-ST-001'];
  const numbers = [
    ...stockCodes.map(code => ({ kind: 'stock', code, question: `查询 ${code} 当前库存` })),
    ...orders.slice(0, 6).map(order => ({
      kind: 'produced', code: order.workorderCode, question: `工单 ${order.workorderCode} 已生产多少？`
    })),
    ...receiptCodes.map(code => ({
      kind: 'inbound', code, from: '2026-10-01', to: '2026-10-31',
      question: `查询 ${code} 在 2026-10-01 到 2026-10-31 入库了多少？`
    }))
  ];
  return { relations, numbers };
}

function bundleDocuments(relations) {
  let offset = 0;
  return departments.map((department, deptIndex) => {
    const documents = Array.from({ length: 5 }, (_, index) => {
      const selected = relations.slice(offset, offset + (deptIndex * 5 + index < 10 ? 7 : 6));
      offset += selected.length;
      const rows = selected.map(({ item, order }) => ({
        fragment: `${item.bomItemName}（${item.bomItemCode}）按 BOM 计划用于工单 ${order.workorderCode}。`,
        itemName: item.bomItemName, itemCode: item.bomItemCode, orderCode: order.workorderCode
      }));
      const first = rows[0];
      const last = rows[rows.length - 1];
      const title = `测温枪${department.prefix} ${index + 1}`;
      const cases = [
        { question: `${first.orderCode} 的计划配套清单里有 ${first.itemName}（${first.itemCode}）吗？`, fragment: first.fragment },
        { question: `哪份资料记录了 ${last.itemName}（${last.itemCode}）与工单 ${last.orderCode} 的配套关系？`, fragment: last.fragment }
      ];
      const triples = selected.map(({ source, kind, target }) => ({ source, kind, target }));
      const content = [
        title, department.intro, '以下配套关系来自演示工单的产品与当前启用的直接 BOM 组件。实际领料以领料单为准。',
        '', ...rows.map(row => row.fragment)
      ].join('\n');
      return { title, cases, triples, content };
    });
    return { department, documents };
  });
}

async function getLibrary(department) {
  const path = '/mes/ai/kb/libraries';
  const find = async () => (await api(path + '/admin')).data.find(library => library.deptId === department.id);
  let library = await find();
  if (!library) {
    await api(path, { method: 'POST', data: { deptId: department.id, name: department.name } });
    library = await find();
  }
  if (!library || library.name !== department.name || library.status !== 'enabled') {
    throw new Error(`部门 ${department.id} 没有预期的启用演示库。`);
  }
  return library;
}

async function upload(library, bundle) {
  const path = `/mes/ai/kb/libraries/${library.libraryId}/documents`;
  const { documents } = (await api(path)).data;
  for (const document of bundle) {
    const existing = documents.find(row => row.title === document.title && row.category === category);
    if (existing) {
      if (existing.latestVersion !== version) throw new Error(`${document.title} 已有其他版本，保留原件。`);
      const raw = await (await request(`${path}/versions/${existing.latestVersionId}/original`)).text();
      if (raw !== document.content) throw new Error(`${document.title} 与当前演示工单不符，保留原件。`);
      continue;
    }
    const form = new FormData();
    form.set('title', document.title);
    form.set('category', category);
    form.set('version', version);
    form.set('graphEnabled', 'true');
    form.set('file', new Blob([document.content], { type: 'text/plain' }), document.title + '.txt');
    await api(path, { method: 'POST', data: form });
  }
}

async function waitForGraph(libraries) {
  const deadline = Date.now() + 180000;
  while (Date.now() < deadline) {
    let waiting = 0;
    for (const library of libraries) {
      const titles = new Set(library.documents.map(document => document.title));
      const path = `/mes/ai/kb/libraries/${library.libraryId}/documents`;
      const rows = (await api(path)).data.documents.filter(row => titles.has(row.title) && row.category === category);
      const failed = rows.find(row => row.parseStatus === 'failed' || row.graphStatus === 'failed');
      if (failed) throw new Error(`演示资料处理失败：${failed.title} ${failed.parseError || failed.graphError || ''}`);
      waiting += titles.size - rows.filter(row => row.graphStatus === 'completed' && row.currentVersion === version).length;
    }
    if (waiting === 0) return;
    await delay(2000);
  }
  throw new Error('演示资料的图谱抽取尚未完成，未发布评测清单。');
}

async function run() {
  if (!output) throw new Error('用法：node scripts/seed-ai-evaluation-demo.mjs <评测清单输出路径>');
  if (!['127.0.0.1', 'localhost'].includes(base.hostname) || base.port !== '18082') {
    throw new Error('此脚本只用于本机 18082 端口的隔离演示环境。');
  }
  await signedIn();
  const { relations, numbers } = await prepareSamples();
  const libraries = [];
  for (const { department, documents } of bundleDocuments(relations)) {
    const library = await getLibrary(department);
    await upload(library, documents);
    libraries.push({
      libraryId: library.libraryId, deptId: department.id,
      documents: documents.map(({ content, ...document }) => document)
    });
    console.log(`${department.name}已提交 ${documents.length} 份资料`);
  }
  await waitForGraph(libraries);
  const manifest = { name: '测温枪三部门验收集', libraries, numbers };
  const destination = resolve(output);
  await writeFile(destination + '.tmp', JSON.stringify(manifest, null, 2) + '\n', 'utf8');
  await rename(destination + '.tmp', destination);
  console.log('评测资料已就绪，包含 30 道文档题、20 道数据题和 100 条标注关系。');
}

run().catch(error => { console.error(error.message); process.exitCode = 1; });

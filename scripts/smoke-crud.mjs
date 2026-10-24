/**
 * 基础 CRUD 冒烟测试：计量单位、客户、供应商 的 增/改/查/删
 * 用法：node scripts/smoke-crud.mjs
 */
import net from 'node:net';

const BASE = process.env.MES_BASE || 'http://127.0.0.1:8080';
let token = '';

function redisGet(key) {
  return new Promise((resolve, reject) => {
    const s = net.connect(6379, '127.0.0.1');
    let b = '';
    s.on('data', d => { b += d; if (!b.endsWith('\r\n')) return; s.end();
      const m = b.match(/^\$(-?\d+)\r\n([\s\S]*?)\r\n$/); resolve(m && m[1] !== '-1' ? m[2] : null); });
    s.on('error', reject);
    const k = Buffer.from(key);
    s.write(Buffer.concat([Buffer.from(`*2\r\n$3\r\nGET\r\n$${k.length}\r\n`), k, Buffer.from('\r\n')]));
  });
}

async function api(method, path, body) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = 'Bearer ' + token;
  const r = await fetch(BASE + path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  const t = await r.text();
  try { return t ? JSON.parse(t) : {}; } catch { return { raw: t }; }
}
const ok = (c, m) => console.log(`${c ? '  ✅' : '  ❌'} ${m}`);
let failed = 0;
function assert(cond, msg) { ok(cond, msg); if (!cond) failed++; }

const cap = await (await fetch(BASE + '/captchaImage')).json();
const on = cap.captchaOnOff !== undefined ? cap.captchaOnOff : cap.captchaEnabled;
const code = on ? ((await redisGet('captcha_codes:' + cap.uuid)) || '').replace(/^"|"$/g, '') : '';
const login = await api('POST', '/login', { username: 'admin', password: 'admin123', code, uuid: cap.uuid });
assert(login.code === 200, '登录');
token = login.token;

// ---------- 计量单位 CRUD ----------
console.log('\n[计量单位]');
const unitCode = 'SMOKE_U_' + Date.now().toString().slice(-6);
await api('POST', '/mes/md/unitmeasure', { measureCode: unitCode, measureName: '冒烟测试单位', primaryFlag: 'N', enableFlag: 'Y', changeRate: 1, remark: 'smoke' });
let list = await api('GET', `/mes/md/unitmeasure/list?pageNum=1&pageSize=10&measureCode=${unitCode}`);
const unit = (list.rows || [])[0];
assert(!!unit, '新增并查询到计量单位');
if (unit) {
  const upd = await api('PUT', '/mes/md/unitmeasure', { ...unit, measureName: '冒烟测试单位-已改' });
  assert(upd.code === 200, '修改计量单位');
  const after = (await api('GET', `/mes/md/unitmeasure/${unit.measureId}`)).data;
  assert(after && after.measureName === '冒烟测试单位-已改', '修改后名称已生效');
  const del = await api('DELETE', `/mes/md/unitmeasure/${unit.measureId}`);
  assert(del.code === 200, '删除计量单位');
  const gone = await api('GET', `/mes/md/unitmeasure/${unit.measureId}`);
  assert(gone.code !== 200 || !gone.data, '删除后查询不到');
}

// ---------- 客户 CRUD ----------
console.log('\n[客户]');
const clientCode = 'SMOKE_C_' + Date.now().toString().slice(-6);
await api('POST', '/mes/md/client', { clientCode, clientName: '冒烟测试客户', clientNick: '冒烟客户', clientType: 'ENTERPRISE', enableFlag: 'Y', remark: 'smoke' });
let cl = await api('GET', `/mes/md/client/list?pageNum=1&pageSize=10&clientCode=${clientCode}`);
const client = (cl.rows || [])[0];
assert(!!client, '新增并查询到客户');
if (client) {
  const upd = await api('PUT', '/mes/md/client', { ...client, clientName: '冒烟测试客户-已改' });
  assert(upd.code === 200, '修改客户');
  const after = (await api('GET', `/mes/md/client/${client.clientId}`)).data;
  assert(after && after.clientName === '冒烟测试客户-已改', '修改后名称已生效');
  const del = await api('DELETE', `/mes/md/client/${client.clientId}`);
  assert(del.code === 200, '删除客户');
}

// ---------- 供应商 CRUD ----------
console.log('\n[供应商]');
const vendorCode = 'SMOKE_V_' + Date.now().toString().slice(-6);
await api('POST', '/mes/md/vendor', { vendorCode, vendorName: '冒烟测试供应商', vendorNick: '冒烟供应商', vendorLevel: 'B', enableFlag: 'Y', remark: 'smoke' });
let vl = await api('GET', `/mes/md/vendor/list?pageNum=1&pageSize=10&vendorCode=${vendorCode}`);
const vendor = (vl.rows || [])[0];
assert(!!vendor, '新增并查询到供应商');
if (vendor) {
  const upd = await api('PUT', '/mes/md/vendor', { ...vendor, vendorName: '冒烟测试供应商-已改' });
  assert(upd.code === 200, '修改供应商');
  const del = await api('DELETE', `/mes/md/vendor/${vendor.vendorId}`);
  assert(del.code === 200, '删除供应商');
}

// ---------- 只读关键接口 ----------
console.log('\n[核心查询接口]');
for (const [name, path] of [
  ['物料产品', '/mes/md/mditem/list?pageNum=1&pageSize=5'],
  ['多级BOM', '/mes/md/bom/list?pageNum=1&pageSize=5'],
  ['生产工单', '/mes/pro/workorder/list?pageNum=1&pageSize=5'],
  ['生产任务', '/mes/pro/protask/list?pageNum=1&pageSize=5'],
  ['库存现有量', '/mes/wm/wmstock/list?pageNum=1&pageSize=5'],
  ['库存流水', '/mes/wm/transaction/list?pageNum=1&pageSize=5'],
  ['来料检验', '/mes/qc/iqc/list?pageNum=1&pageSize=5'],
  ['过程检验', '/mes/qc/ipqc/list?pageNum=1&pageSize=5'],
  ['出货检验', '/mes/qc/oqc/list?pageNum=1&pageSize=5'],
  ['设备台账', '/mes/dv/machinery/list?pageNum=1&pageSize=5'],
  ['班组', '/mes/cal/team/list?pageNum=1&pageSize=5'],
  ['排班计划', '/mes/cal/calplan/list?pageNum=1&pageSize=5']
]) {
  const r = await api('GET', path);
  assert(r.code === 200, `${name} 查询（total=${r.total}）`);
}

console.log(`\n=== CRUD 冒烟结果：${failed === 0 ? '全部通过' : failed + ' 项失败'} ===`);
process.exit(failed ? 1 : 0);

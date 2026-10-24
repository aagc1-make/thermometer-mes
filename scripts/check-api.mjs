/**
 * 接口冒烟检查：登录 → 库存流水 / 库存现有量 / 菜单 / 文件上传
 * 用法：node scripts/check-api.mjs
 */
import net from 'node:net';

const BASE = process.env.MES_BASE || 'http://127.0.0.1:8080';
const REDIS_HOST = '127.0.0.1', REDIS_PORT = 6379;

function redisGet(key) {
  return new Promise((resolve, reject) => {
    const sock = net.connect(REDIS_PORT, REDIS_HOST);
    let buf = Buffer.alloc(0);
    const onData = d => {
      buf = Buffer.concat([buf, d]);
      const text = buf.toString('utf8');
      if (!text.endsWith('\r\n')) return;
      sock.removeListener('data', onData); sock.end();
      const m = text.match(/^\$(-?\d+)\r\n([\s\S]*?)\r\n$/);
      resolve(m && m[1] !== '-1' ? m[2] : null);
    };
    sock.on('data', onData);
    sock.on('error', reject);
    sock.setTimeout(5000, () => { sock.destroy(); reject(new Error('redis timeout')); });
    const k = Buffer.from(key, 'utf8');
    sock.write(Buffer.concat([Buffer.from(`*2\r\n$3\r\nGET\r\n$${k.length}\r\n`), k, Buffer.from('\r\n')]));
  });
}

const cap = await (await fetch(BASE + '/captchaImage')).json();
const uuid = cap.uuid;
const on = cap.captchaOnOff !== undefined ? cap.captchaOnOff : cap.captchaEnabled;
const code = on ? (await redisGet(`captcha_codes:${uuid}`) || '').replace(/^"|"$/g, '') : '';
const login = await (await fetch(BASE + '/login', {
  method: 'POST', headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'admin', password: 'admin123', code, uuid })
})).json();
if (login.code !== 200) { console.error('登录失败', login); process.exit(1); }
const token = login.token;
const H = { Authorization: 'Bearer ' + token };

async function g(path) {
  const r = await (await fetch(BASE + path, { headers: H })).json();
  return r;
}

console.log('== 库存流水（新增接口）==');
const tx = await g('/mes/wm/transaction/list?pageNum=1&pageSize=5');
console.log(`  code=${tx.code} total=${tx.total} 首条=${tx.rows?.[0]?.itemCode} ${tx.rows?.[0]?.transactionType} ${tx.rows?.[0]?.transactionQuantity}`);

console.log('== 库存流水按物料过滤 ==');
const tx2 = await g('/mes/wm/transaction/list?pageNum=1&pageSize=5&itemCode=TG-FG-001');
console.log(`  code=${tx2.code} total=${tx2.total}`);

console.log('== 库存现有量 ==');
const st = await g('/mes/wm/wmstock/list?pageNum=1&pageSize=5');
console.log(`  code=${st.code} total=${st.total}`);

console.log('== 菜单（是否含库存流水）==');
const menus = await g('/getRouters');
const s = JSON.stringify(menus.data || []);
console.log(`  含"库存流水": ${s.includes('库存流水')}`);

console.log('== 文件上传 ==');
const fd = new FormData();
fd.append('file', new Blob(['upload smoke test\n'], { type: 'text/plain' }), 'smoke.txt');
const up = await (await fetch(BASE + '/common/upload', { method: 'POST', headers: H, body: fd })).json();
console.log(`  /common/upload code=${up.code} url=${up.data?.url || ''}`);
const fd2 = new FormData();
fd2.append('file', new Blob(['minio smoke test\n'], { type: 'text/plain' }), 'smoke-minio.txt');
const up2 = await (await fetch(BASE + '/common/uploadMinio', { method: 'POST', headers: H, body: fd2 })).json();
console.log(`  /common/uploadMinio code=${up2.code} url=${up2.data?.url || ''}`);

console.log('== 主数据数量 ==');
for (const [name, path] of [
  ['物料产品', '/mes/md/mditem/list?pageNum=1&pageSize=1'],
  ['多级BOM', '/mes/md/bom/list?pageNum=1&pageSize=1'],
  ['工序', '/mes/pro/process/list?pageNum=1&pageSize=1'],
  ['工艺路线', '/mes/pro/proroute/list?pageNum=1&pageSize=1'],
  ['仓库', '/mes/wm/warehouse/list?pageNum=1&pageSize=1'],
  ['检测方案', '/mes/qc/qctemplate/list?pageNum=1&pageSize=1'],
  ['设备台账', '/mes/dv/machinery/list?pageNum=1&pageSize=1'],
  ['班组', '/mes/cal/team/list?pageNum=1&pageSize=1']
]) {
  const r = await g(path);
  console.log(`  ${name}: code=${r.code} total=${r.total}`);
}

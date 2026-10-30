/**
 * 红外测温枪 MES —— 全链路冒烟测试
 *
 * 覆盖：
 *   一、独立小程序后端（8081）：健康检查、产品目录免登录、未登录 401、登录签发令牌、令牌访问受保护接口、/auth/me
 *   二、MES 后端（8080）：admin 登录、库存查询、菜单联动蓝点、AI 智能助手状态
 *
 * 用法（PowerShell）：
 *   $env:REDIS_PASSWORD="123456"; node scripts/smoke-test.mjs
 * 可选环境变量：
 *   MES_BASE（默认 http://127.0.0.1:8080）、MINIAPP_BASE（默认 http://127.0.0.1:8081）
 *   REDIS_HOST / REDIS_PORT / REDIS_PASSWORD
 *   MINIAPP_USER / MINIAPP_PASSWORD（默认 demo / demo123，与 sql/06_miniapp_auth.sql 一致）
 */
import net from 'node:net';

const MES = (process.env.MES_BASE || 'http://127.0.0.1:8080').replace(/\/$/, '');
const MINI = (process.env.MINIAPP_BASE || 'http://127.0.0.1:8081').replace(/\/$/, '');
const REDIS_HOST = process.env.REDIS_HOST || '127.0.0.1';
const REDIS_PORT = Number(process.env.REDIS_PORT || 6379);
const REDIS_PASSWORD = process.env.REDIS_PASSWORD || '';
const MINIAPP_USER = process.env.MINIAPP_USER || 'demo';
const MINIAPP_PASSWORD = process.env.MINIAPP_PASSWORD || 'demo123';

let passed = 0;
let failed = 0;
const failures = [];

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function resp(parts) {
  const out = [Buffer.from(`*${parts.length}\r\n`)];
  for (const part of parts) {
    const buf = Buffer.from(part, 'utf8');
    out.push(Buffer.from(`$${buf.length}\r\n`), buf, Buffer.from('\r\n'));
  }
  return Buffer.concat(out);
}

/** 直接读 Redis 取登录验证码（支持 requirepass） */
function redisGet(key) {
  return new Promise((resolve, reject) => {
    const sock = net.connect(REDIS_PORT, REDIS_HOST);
    let buf = Buffer.alloc(0);
    const onData = (chunk) => {
      buf = Buffer.concat([buf, chunk]);
      let text = buf.toString('utf8');
      if (REDIS_PASSWORD && text.startsWith('+OK\r\n')) text = text.slice(5);
      else if (REDIS_PASSWORD && text.startsWith('-')) {
        sock.removeListener('data', onData);
        sock.end();
        return reject(new Error('Redis AUTH 失败：' + text.trim()));
      }
      if (!text.endsWith('\r\n')) return;
      sock.removeListener('data', onData);
      sock.end();
      const match = text.match(/^\$(-?\d+)\r\n([\s\S]*?)\r\n$/);
      if (!match) return resolve(null);
      resolve(match[1] === '-1' ? null : match[2]);
    };
    sock.on('data', onData);
    sock.on('error', reject);
    sock.setTimeout(5000, () => {
      sock.destroy();
      reject(new Error('Redis 连接超时'));
    });
    const commands = REDIS_PASSWORD
      ? [resp(['AUTH', REDIS_PASSWORD]), resp(['GET', key])]
      : [resp(['GET', key])];
    sock.write(Buffer.concat(commands));
  });
}

async function httpJson(base, method, path, { token, body } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = 'Bearer ' + token;
  const res = await fetch(base + path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  const text = await res.text();
  let json = {};
  try {
    json = text ? JSON.parse(text) : {};
  } catch {
    json = { raw: text.slice(0, 300) };
  }
  return { status: res.status, json };
}

async function check(name, run) {
  try {
    const detail = await run();
    passed++;
    console.log(`  [通过] ${name}${detail ? '：' + detail : ''}`);
  } catch (error) {
    failed++;
    failures.push(name + ' -> ' + error.message);
    console.log(`  [失败] ${name}：${error.message}`);
  }
}

/** MES 后台登录（验证码从 Redis 读取） */
async function mesLogin() {
  const cap = (await httpJson(MES, 'GET', '/captchaImage')).json;
  const captchaOn = cap.captchaOnOff !== undefined ? cap.captchaOnOff : cap.captchaEnabled;
  let code = '';
  const uuid = cap.uuid || '';
  if (captchaOn) {
    const raw = await redisGet(`captcha_codes:${uuid}`);
    code = (raw || '').replace(/^"|"$/g, '');
    if (!code) throw new Error('未能从 Redis 读取验证码，请确认 Redis 可用并已设置 REDIS_PASSWORD');
  }
  const result = await httpJson(MES, 'POST', '/login', {
    body: { username: 'admin', password: 'admin123', code, uuid }
  });
  if (result.json.code !== 200 || !result.json.token) {
    throw new Error('MES 登录失败：' + (result.json.msg || result.status));
  }
  return result.json.token;
}

let miniToken = '';
let mesToken = '';

console.log('== 一、独立小程序后端 ' + MINI + ' ==');

await check('健康检查（公开）', async () => {
  const r = await httpJson(MINI, 'GET', '/api/v1/health');
  assert(r.json.code === 200, `期望 code=200，实际 ${r.json.code}`);
  return r.json.data;
});

await check('产品目录免登录可读', async () => {
  const r = await httpJson(MINI, 'GET', '/api/v1/catalog/products');
  assert(r.json.code === 200, `期望 code=200，实际 ${r.json.code} ${r.json.msg || ''}`);
  assert(Array.isArray(r.json.data) && r.json.data.length > 0, '产品目录为空');
  return `${r.json.data.length} 条产品`;
});

await check('受保护接口未登录返回 401', async () => {
  const r = await httpJson(MINI, 'GET', '/api/v1/orders');
  assert(r.status === 401, `期望 HTTP 401，实际 ${r.status}`);
  assert(r.json.code === 401, `期望业务码 401，实际 ${r.json.code}`);
  return r.json.msg;
});

await check('错误密码被拒绝', async () => {
  const r = await httpJson(MINI, 'POST', '/api/v1/auth/login', {
    body: { username: MINIAPP_USER, password: 'wrong-password' }
  });
  assert(!(r.json.code === 200 && r.json.data && r.json.data.token), '错误密码不应签发令牌');
  return r.json.msg || '已拒绝';
});

await check('演示账号登录签发令牌', async () => {
  const r = await httpJson(MINI, 'POST', '/api/v1/auth/login', {
    body: { username: MINIAPP_USER, password: MINIAPP_PASSWORD }
  });
  assert(r.json.code === 200, `登录失败：${r.json.msg || r.status}`);
  assert(r.json.data && r.json.data.token, '未返回令牌');
  miniToken = r.json.data.token;
  const account = r.json.data.account || {};
  return `账号 ${account.username || MINIAPP_USER}，有效期 ${r.json.data.expiresIn}s`;
});

await check('携带令牌访问受保护接口', async () => {
  const r = await httpJson(MINI, 'GET', '/api/v1/orders', { token: miniToken });
  assert(r.json.code === 200, `期望 code=200，实际 ${r.json.code} ${r.json.msg || ''}`);
  return `${(r.json.data || []).length} 张订单`;
});

await check('携带令牌获取当前账号', async () => {
  const r = await httpJson(MINI, 'GET', '/api/v1/auth/me', { token: miniToken });
  assert(r.json.code === 200, `期望 code=200，实际 ${r.json.code}`);
  return `${r.json.data.username} / ${r.json.data.displayName || ''}`;
});

console.log('== 二、MES 后端 ' + MES + ' ==');

await check('MES 后台登录（admin）', async () => {
  mesToken = await mesLogin();
  return '已获取令牌';
});

await check('库存查询', async () => {
  const r = await httpJson(MES, 'GET', '/mes/wm/wmstock/list?pageNum=1&pageSize=5', { token: mesToken });
  assert(r.json.code === 200, `期望 code=200，实际 ${r.json.code} ${r.json.msg || ''}`);
  const rows = r.json.rows || [];
  assert(rows.length > 0, '库存列表为空');
  return `${r.json.total ?? rows.length} 行库存`;
});

await check('菜单联动蓝点未读', async () => {
  const r = await httpJson(MES, 'GET', '/system/menu-linkage-notice/unread', { token: mesToken });
  assert(r.json.code === 200, `期望 code=200，实际 ${r.json.code} ${r.json.msg || ''}`);
  assert(Array.isArray(r.json.data), '返回结构不是数组');
  assert(r.json.data.length > 0, '当前无未读蓝点（可先运行 node scripts/demo-flow.mjs 造数据）');
  return `${r.json.data.length} 个：${r.json.data.slice(0, 3).join('、')}`;
});

await check('AI 智能助手状态', async () => {
  const r = await httpJson(MES, 'POST', '/mes/ai/status', { token: mesToken, body: {} });
  assert(r.json.code === 200, `期望 code=200，实际 ${r.json.code} ${r.json.msg || ''}`);
  const data = r.json.data || {};
  assert(data.deepseek === true, `DeepSeek 未就绪：${JSON.stringify(data)}`);
  return `deepseek=${data.deepseek} glm=${data.glm} model=${data.model}`;
});

console.log('');
console.log(`== 冒烟结果：通过 ${passed} 项，失败 ${failed} 项 ==`);
if (failed > 0) {
  failures.forEach((item) => console.log('  - ' + item));
  process.exit(1);
}

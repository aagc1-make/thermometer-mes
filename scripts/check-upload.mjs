/**
 * 验证文件上传接口（/common/upload 与 /common/uploadMinio）
 * 用法：node scripts/check-upload.mjs
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
let code = '';
if (cap.captchaOnOff !== undefined ? cap.captchaOnOff : cap.captchaEnabled) {
  code = (await redisGet(`captcha_codes:${uuid}`) || '').replace(/^"|"$/g, '');
}
const login = await (await fetch(BASE + '/login', {
  method: 'POST', headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'admin', password: 'admin123', code, uuid })
})).json();
if (login.code !== 200) { console.error('登录失败', login); process.exit(1); }
const token = login.token;

async function upload(path, field) {
  const fd = new FormData();
  fd.append(field, new Blob(['红外测温枪 MES 上传测试\n'], { type: 'text/plain' }), '测温枪上传测试.txt');
  const res = await fetch(BASE + path, { method: 'POST', headers: { Authorization: 'Bearer ' + token }, body: fd });
  const text = await res.text();
  let json; try { json = JSON.parse(text); } catch { json = { raw: text.slice(0, 200) }; }
  console.log(`${path} -> code=${json.code} msg=${json.msg || ''}`);
  if (json.data) console.log('   data:', JSON.stringify(json.data).slice(0, 220));
  return json;
}

await upload('/common/upload', 'file');
await upload('/common/uploadMinio', 'file');

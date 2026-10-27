import assert from 'node:assert/strict';
import net from 'node:net';
import test from 'node:test';

const BASE = process.env.MES_BASE || 'http://127.0.0.1:8080';
const REDIS_HOST = process.env.REDIS_HOST || '127.0.0.1';
const REDIS_PORT = Number(process.env.REDIS_PORT || 6379);

function redisGet(key) {
  return new Promise((resolve, reject) => {
    const socket = net.connect(REDIS_PORT, REDIS_HOST);
    let buffer = Buffer.alloc(0);

    socket.on('data', chunk => {
      buffer = Buffer.concat([buffer, chunk]);
      const response = buffer.toString('utf8');
      if (!response.endsWith('\r\n')) return;
      socket.end();
      const match = response.match(/^\$(-?\d+)\r\n([\s\S]*?)\r\n$/);
      resolve(match && match[1] !== '-1' ? match[2] : null);
    });
    socket.on('error', reject);
    socket.setTimeout(5000, () => {
      socket.destroy();
      reject(new Error('Redis connection timed out'));
    });

    const encodedKey = Buffer.from(key, 'utf8');
    socket.write(Buffer.concat([
      Buffer.from(`*2\r\n$3\r\nGET\r\n$${encodedKey.length}\r\n`),
      encodedKey,
      Buffer.from('\r\n')
    ]));
  });
}

async function login() {
  const captcha = await (await fetch(`${BASE}/captchaImage`)).json();
  const captchaEnabled = captcha.captchaOnOff ?? captcha.captchaEnabled;
  const code = captchaEnabled
    ? ((await redisGet(`captcha_codes:${captcha.uuid}`)) || '').replace(/^"|"$/g, '')
    : '';
  const response = await fetch(`${BASE}/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123', code, uuid: captcha.uuid })
  });
  const result = await response.json();
  assert.equal(result.code, 200, `login failed: ${result.msg || 'unknown error'}`);
  return result.token;
}

const token = await login();

async function list(path, params = {}) {
  const query = new URLSearchParams({ pageNum: '1', pageSize: '500', ...params });
  const response = await fetch(`${BASE}${path}?${query}`, {
    headers: { Authorization: `Bearer ${token}` }
  });
  const result = await response.json();
  assert.equal(result.code, 200, `${path} failed: ${result.msg || response.status}`);
  return result.rows || [];
}

test('infrared thermometer has a non-trivial multi-level BOM', async () => {
  const root = await list('/mes/md/bom/list', { itemId: '9201' });
  const electrical = await list('/mes/md/bom/list', { itemId: '9202' });
  const handle = await list('/mes/md/bom/list', { itemId: '9203' });

  assert.ok(root.length >= 6, `expected at least 6 direct BOM lines, got ${root.length}`);
  assert.ok(root.length + electrical.length + handle.length >= 10,
    'expected at least 10 total multi-level BOM lines');
});

test('production flow contains three completed workorders and nine tasks and feedback records', async () => {
  const workorders = (await list('/mes/pro/workorder/list'))
    .filter(row => ['TG-SF-001', 'TG-SF-002', 'TG-FG-001'].includes(row.productCode));
  assert.equal(workorders.length, 3, `expected 3 thermometer workorders, got ${workorders.length}`);
  for (const workorder of workorders) {
    assert.equal(Number(workorder.quantity), 10, `${workorder.workorderCode} planned quantity`);
    assert.equal(Number(workorder.quantityProduced), 10, `${workorder.workorderCode} produced quantity`);
  }

  const workorderIds = new Set(workorders.map(row => String(row.workorderId)));
  const tasks = (await list('/mes/pro/protask/list'))
    .filter(row => workorderIds.has(String(row.workorderId)));
  const feedback = (await list('/mes/pro/feedback/list'))
    .filter(row => workorderIds.has(String(row.workorderId)));
  assert.equal(tasks.length, 9, `expected 9 production tasks, got ${tasks.length}`);
  assert.equal(feedback.length, 9, `expected 9 feedback records, got ${feedback.length}`);
});

test('quality flow contains completed IQC, IPQC and OQC records', async () => {
  const iqc = await list('/mes/qc/iqc/list');
  const ipqc = await list('/mes/qc/ipqc/list');
  const oqc = await list('/mes/qc/oqc/list');

  assert.ok(iqc.length >= 14, `expected at least 14 IQC records, got ${iqc.length}`);
  assert.ok(ipqc.length >= 3, `expected at least 3 IPQC records, got ${ipqc.length}`);
  assert.ok(oqc.length >= 1, `expected at least 1 OQC record, got ${oqc.length}`);
  for (const record of [...iqc, ...ipqc, ...oqc]) {
    assert.equal(record.status, 'FINISHED', `${record.iqcCode || record.ipqcCode || record.oqcCode} status`);
    assert.equal(record.checkResult, 'ACCEPT', `${record.iqcCode || record.ipqcCode || record.oqcCode} result`);
  }
});

test('warehouse flow keeps four finished thermometers after selling six', async () => {
  const receipts = await list('/mes/wm/productrecpt/list');
  const sales = await list('/mes/wm/productsales/list');
  const stock = await list('/mes/wm/wmstock/list', {
    itemCode: 'TG-FG-001',
    warehouseCode: 'WH-FG'
  });
  const transactions = await list('/mes/wm/transaction/list', { itemCode: 'TG-FG-001' });

  assert.ok(receipts.length >= 1, 'expected a finished-product receipt');
  assert.ok(sales.length >= 1, 'expected a finished-product sale');
  assert.ok(transactions.length >= 3, `expected finished-product stock movements, got ${transactions.length}`);
  const onHand = stock.reduce((sum, row) => sum + Number(row.quantityOnhand || 0), 0);
  assert.equal(onHand, 4, `expected 4 finished thermometers on hand, got ${onHand}`);
});

test('material issues close inventory without negative line-side stock', async () => {
  const stock = await list('/mes/wm/wmstock/list');
  const thermometerStock = stock.filter(row => String(row.itemCode || '').startsWith('TG-'));
  const negative = thermometerStock.filter(row => Number(row.quantityOnhand || 0) < 0);
  assert.deepEqual(negative.map(row => ({
    itemCode: row.itemCode,
    warehouseCode: row.warehouseCode,
    quantityOnhand: Number(row.quantityOnhand)
  })), [], 'thermometer inventory must never be negative');

  for (const itemCode of ['TG-PK-001', 'TG-PK-002']) {
    const packagingStock = thermometerStock.filter(row => row.itemCode === itemCode);
    const total = packagingStock.reduce((sum, row) => sum + Number(row.quantityOnhand || 0), 0);
    const raw = packagingStock
      .filter(row => row.warehouseCode === 'WH-RAW')
      .reduce((sum, row) => sum + Number(row.quantityOnhand || 0), 0);
    const line = packagingStock
      .filter(row => row.warehouseCode === 'XBK_VIRTUAL')
      .reduce((sum, row) => sum + Number(row.quantityOnhand || 0), 0);
    assert.equal(total, 0, `${itemCode} total stock`);
    assert.equal(raw, 0, `${itemCode} raw-warehouse stock`);
    assert.equal(line, 0, `${itemCode} line-side stock`);
  }
});

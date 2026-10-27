/**
 * 红外测温枪 MES —— 生产 10 台、销售 6 台、结存 4 台全流程演示脚本
 *
 * 依次执行：采购到货 → 来料检验(IQC) → 采购入库 → 生产工单 → 排产 →
 *          生产领料 → 生产报工 → 过程检验(IPQC) → 成品入库 → 出货检验(OQC) → 销售出库
 *
 * 依赖：先执行 sql/thermometer_init.sql（17 个物料产品、多级 BOM、工艺路线、仓库、检验方案）
 * 用法：node scripts/demo-flow.mjs
 * 环境变量：MES_BASE(默认 http://127.0.0.1:8080)、REDIS_HOST、REDIS_PORT
 */
import net from 'node:net';

const BASE = process.env.MES_BASE || 'http://127.0.0.1:8080';
const REDIS_HOST = process.env.REDIS_HOST || '127.0.0.1';
const REDIS_PORT = Number(process.env.REDIS_PORT || 6379);
const TS = Date.now().toString().slice(-6);
let token = '';
let step = 0;

/** 直接读 Redis 取验证码，避免依赖 redis-cli 子进程 */
function redisGet(key) {
  return new Promise((resolve, reject) => {
    const sock = net.connect(REDIS_PORT, REDIS_HOST);
    let buf = Buffer.alloc(0);
    const onData = d => {
      buf = Buffer.concat([buf, d]);
      const text = buf.toString('utf8');
      if (!text.endsWith('\r\n')) return;
      sock.removeListener('data', onData);
      sock.end();
      const m = text.match(/^\$(-?\d+)\r\n([\s\S]*?)\r\n$/);
      if (!m) return resolve(null);
      resolve(m[1] === '-1' ? null : m[2]);
    };
    sock.on('data', onData);
    sock.on('error', reject);
    sock.setTimeout(5000, () => { sock.destroy(); reject(new Error('Redis 连接超时')); });
    const k = Buffer.from(key, 'utf8');
    sock.write(Buffer.concat([
      Buffer.from(`*2\r\n$3\r\nGET\r\n$${k.length}\r\n`), k, Buffer.from('\r\n')
    ]));
  });
}

async function api(method, path, body) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = 'Bearer ' + token;
  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  const text = await res.text();
  let json;
  try { json = text ? JSON.parse(text) : {}; } catch { json = { raw: text }; }
  return json;
}

function fail(label, json) {
  const msg = json && (json.msg || json.message || JSON.stringify(json).slice(0, 300));
  throw new Error(`[失败] ${label}: ${msg}`);
}

async function post(path, body, label) {
  const r = await api('POST', path, body);
  if (r.code !== 200) fail(label || `POST ${path}`, r);
  return r;
}
async function put(path, body, label) {
  const r = await api('PUT', path, body);
  if (r.code !== 200) fail(label || `PUT ${path}`, r);
  return r;
}
async function get(path, label) {
  const r = await api('GET', path, undefined);
  if (r.code !== 200) fail(label || `GET ${path}`, r);
  return r;
}
async function rows(path, params = {}) {
  const qs = new URLSearchParams({ pageNum: 1, pageSize: 200, ...params }).toString();
  const r = await get(path + '?' + qs);
  return r.rows || [];
}
function log(msg) { console.log(`${String(++step).padStart(2, '0')}. ${msg}`); }

// ---------- 主数据常量（与 sql/thermometer_init.sql 对应）----------
const ITEM = {
  fg: 9201, ec: 9202, hd: 9203,
  pcba: 9204, sensor: 9205, lcd: 9206, keypad: 9207, buzzer: 9208, battery: 9209,
  top: 9210, bottom: 9211, trigger: 9212, lens: 9213, screw: 9214, foam: 9215,
  manual: 9216, box: 9217
};
const WH = { raw: 9702, line: 9701, fg: 9703, virtual: 9704 };
const LOC = { ec: 9711, hd: 9712, st: 9713, pk: 9714, fg: 9718, vw: 9719 };
const AREA = { ec: 9721, hd: 9722, st: 9723, pk: 9724, fg: 9728, vw: 9729 };
const STATION = { op10: 9601, op20: 9602, op30: 9603, op40: 9604, op50: 9605, op60: 9606, op70: 9607, op80: 9608, op90: 9609 };
const PROCESS = { op10: 9401, op20: 9402, op30: 9403, op40: 9404, op50: 9405, op60: 9406, op70: 9407, op80: 9408, op90: 9409 };
const ROUTE = { ec: 9451, hd: 9452, fg: 9453 };
const VENDOR = { elec: 9761, struct: 9762, pack: 9763 };
const CLIENT = 9751;

const ITEMS = {
  pcba:  { code: 'TG-EL-001', name: '主控PCBA', spec: 'TG-MAIN-V1.2 主控板（已贴片）', unit: 'PCS', unitName: '个' },
  sensor:{ code: 'TG-EL-002', name: '红外传感器模块', spec: 'MLX90614 兼容 TO-39 红外测温传感器', unit: 'PCS', unitName: '个' },
  lcd:   { code: 'TG-EL-003', name: '显示组件', spec: '1.0 寸段码 LCD 显示模组', unit: 'PCS', unitName: '个' },
  keypad:{ code: 'TG-EL-004', name: '三键按键板', spec: '上/下/设置 三键薄膜按键板', unit: 'PCS', unitName: '个' },
  buzzer:{ code: 'TG-EL-005', name: '蜂鸣器', spec: '5V 有源蜂鸣器', unit: 'PCS', unitName: '个' },
  battery:{ code: 'TG-EL-006', name: '电池', spec: 'AAA 1.5V 碱性电池', unit: 'PCS', unitName: '节' },
  top:   { code: 'TG-ST-001', name: '上壳', spec: 'ABS 白色 上壳', unit: 'PCS', unitName: '个' },
  bottom:{ code: 'TG-ST-002', name: '下壳', spec: 'ABS 白色 下壳', unit: 'PCS', unitName: '个' },
  trigger:{ code: 'TG-ST-003', name: '扳机组件', spec: '扳机+复位弹簧组件', unit: 'PCS', unitName: '个' },
  lens:  { code: 'TG-ST-004', name: '红外透镜', spec: 'HDPE 红外聚焦透镜', unit: 'PCS', unitName: '个' },
  screw: { code: 'TG-ST-005', name: '螺丝包', spec: 'M2×6 自攻螺丝 6 颗/包', unit: 'BAG', unitName: '包' },
  foam:  { code: 'TG-ST-006', name: '防震泡棉', spec: 'EVA 防震泡棉 30×20mm', unit: 'PCS', unitName: '片' },
  manual:{ code: 'TG-PK-001', name: '说明书', spec: '中文版使用说明书（8 页）', unit: 'COPY', unitName: '份' },
  box:   { code: 'TG-PK-002', name: '彩盒包装', spec: '350×200×80mm 彩盒', unit: 'PCS', unitName: '个' }
};
const line = (key, qty) => ({ itemId: ITEM[key], qty, ...ITEMS[key] });

// ---------- 登录 ----------
async function login() {
  const cap = await get('/captchaImage');
  const captchaOn = cap.captchaOnOff !== undefined ? cap.captchaOnOff : cap.captchaEnabled;
  let code = '', uuid = cap.uuid || '';
  if (captchaOn) {
    const raw = await redisGet(`captcha_codes:${uuid}`);
    code = (raw || '').replace(/^"|"$/g, '');
    if (!code) throw new Error('未能从 Redis 读取验证码，请确认 Redis 可访问');
  }
  const r = await api('POST', '/login', { username: 'admin', password: 'admin123', code, uuid });
  if (r.code !== 200) fail('登录', r);
  token = r.token;
  log(`登录成功（admin），验证码 ${captchaOn ? '已从 Redis 读取' : '未启用'}`);
}

function fmt(d) {
  const p = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}

// ---------- 1. 采购到货 ----------
async function createArrival({ code, name, vendorId, vendorCode, vendorName, lines }) {
  await post('/mes/wm/arrivalnotice', {
    noticeCode: code, noticeName: name, vendorId, vendorCode, vendorName,
    arrivalDate: '2026-10-24 09:00:00', poCode: 'PO' + TS,
    contact: '张仓管', tel: '13800000202', remark: '测温枪外购物料到货'
  }, `新增到货单 ${code}`);

  const notice = (await rows('/mes/wm/arrivalnotice/list', { noticeCode: code }))[0];
  if (!notice) throw new Error('到货单创建后未能查询到：' + code);

  for (const ln of lines) {
    await post('/mes/wm/arrivalnoticeline', {
      noticeId: notice.noticeId, itemId: ln.itemId, itemCode: ln.code, itemName: ln.name,
      specification: ln.spec, unitOfMeasure: ln.unit, unitName: ln.unitName,
      quantityArrival: ln.qty, iqcCheck: 'Y', remark: '演示到货'
    }, `新增到货行 ${ln.code}`);
  }

  const detail = (await get(`/mes/wm/arrivalnotice/${notice.noticeId}`)).data || notice;
  detail.status = 'APPROVING';
  await put('/mes/wm/arrivalnotice', detail, `提交到货单 ${code}（待检验）`);
  return notice;
}

// ---------- 2. 来料检验 ----------
async function iqcArrival(notice) {
  const lines = await rows('/mes/wm/arrivalnoticeline/list', { noticeId: notice.noticeId });
  for (const ln of lines) {
    const created = await post('/mes/qc/iqc', {
      iqcCode: 'IQC' + TS + ln.itemId,
      iqcName: ln.itemName + ' 来料检验',
      itemId: ln.itemId, itemCode: ln.itemCode, itemName: ln.itemName,
      specification: ln.specification, unitOfMeasure: ln.unitOfMeasure, unitName: ln.unitName,
      sourceDocId: notice.noticeId, sourceDocType: 'ARRIVAL_NOTICE', sourceDocCode: notice.noticeCode,
      sourceLineId: ln.lineId,
      vendorId: notice.vendorId, vendorCode: notice.vendorCode, vendorName: notice.vendorName,
      quantityRecived: ln.quantityArrival, quantityCheck: ln.quantityArrival,
      quantityQualified: ln.quantityArrival, quantityUnqualified: 0,
      inspectDate: '2026-10-24 10:00:00', status: 'PREPARE', remark: '演示来料检验'
    }, `新增IQC ${ln.itemCode}`);
    const iqcId = created.data;
    const iqc = (await get(`/mes/qc/iqc/${iqcId}`)).data;
    Object.assign(iqc, {
      status: 'FINISHED', checkResult: 'ACCEPT',
      quantityRecived: ln.quantityArrival, quantityCheck: ln.quantityArrival,
      quantityQualified: ln.quantityArrival, quantityUnqualified: 0
    });
    await put('/mes/qc/iqc', iqc, `IQC判定合格 ${ln.itemCode}`);
  }
  log(`来料检验完成：${lines.length} 个批次判定合格`);
}

// ---------- 3. 采购入库 ----------
async function purchaseIn(notice, areaId, locId) {
  const lines = await rows('/mes/wm/arrivalnoticeline/list', { noticeId: notice.noticeId });
  const recptCode = 'RK' + TS + notice.noticeId;

  await post('/mes/wm/itemrecpt', {
    recptCode, recptName: notice.noticeName + ' 采购入库',
    noticeId: notice.noticeId, noticeCode: notice.noticeCode, poCode: notice.poCode,
    vendorId: notice.vendorId, vendorCode: notice.vendorCode, vendorName: notice.vendorName,
    recptDate: '2026-10-24 10:30:00', remark: '演示采购入库'
  }, '新增采购入库单');

  const recpt = (await rows('/mes/wm/itemrecpt/list', { recptCode }))[0];
  if (!recpt) throw new Error('采购入库单创建后未查询到：' + recptCode);

  for (const ln of lines) {
    await post('/mes/wm/itemrecptline', {
      recptId: recpt.recptId, noticeLineId: ln.lineId,
      itemId: ln.itemId, itemCode: ln.itemCode, itemName: ln.itemName,
      specification: ln.specification, unitOfMeasure: ln.unitOfMeasure, unitName: ln.unitName,
      quantityRecived: ln.quantityArrival, iqcCheck: 'Y', iqcId: ln.iqcId, iqcCode: ln.iqcCode
    }, `新增入库行 ${ln.itemCode}`);
  }

  let head = (await get(`/mes/wm/itemrecpt/${recpt.recptId}`)).data;
  head.status = 'APPROVING';
  await put('/mes/wm/itemrecpt', head, '入库单提交待上架');

  const recptLines = await rows('/mes/wm/itemrecptline/list', { recptId: recpt.recptId });
  for (const rl of recptLines) {
    await post('/mes/wm/itemreceptdetail', {
      recptId: recpt.recptId, lineId: rl.lineId,
      itemId: rl.itemId, itemCode: rl.itemCode, itemName: rl.itemName,
      specification: rl.specification, unitOfMeasure: rl.unitOfMeasure, unitName: rl.unitName,
      quantity: rl.quantityRecived,
      batchId: rl.batchId, batchCode: rl.batchCode,
      warehouseId: WH.raw, warehouseCode: 'WH-RAW', warehouseName: '原料仓',
      locationId: locId, areaId, remark: '演示上架'
    }, `上架 ${rl.itemCode}`);
  }

  head = (await get(`/mes/wm/itemrecpt/${recpt.recptId}`)).data;
  head.status = 'APPROVED';
  await put('/mes/wm/itemrecpt', head, '入库单提交待执行');
  await put(`/mes/wm/itemrecpt/${recpt.recptId}`, undefined, '执行采购入库（过账）');
  return recpt;
}

// ---------- 4. 生产工单 ----------
async function createWorkorder({ code, name, productId, productCode, productName, unit, qty }) {
  const r = await post('/mes/pro/workorder', {
    workorderCode: code, workorderName: name, orderSource: 'ORDER', sourceCode: 'SO' + TS,
    productId, productCode, productName, unitOfMeasure: unit,
    quantity: qty, requestDate: '2026-11-05 00:00:00',
    workorderType: 'SELF', clientId: CLIENT, clientCode: 'C-TG-001', clientName: '华东医疗器械经销有限公司',
    remark: '测温枪演示工单'
  }, `新增工单 ${code}`);
  const workorderId = r.data;
  const wb = await rows('/mes/pro/workorderbom/list', { workorderId });
  log(`工单 ${code} 创建成功（自动展开 BOM ${wb.length} 行）`);

  const detail = (await get(`/mes/pro/workorder/${workorderId}`)).data;
  detail.status = 'CONFIRMED';
  await put('/mes/pro/workorder', detail, '确认工单（已下达）');
  return { workorderId, detail };
}

// ---------- 5. 排产 ----------
async function schedule(wo, routeId, tasks) {
  const base = new Date('2026-10-26T08:00:00');
  const created = [];
  const rawReq = wo.detail.requestDate || '2026-11-05 00:00:00';
  const requestDate = String(rawReq).length === 10 ? `${rawReq} 00:00:00` : rawReq;
  for (const t of tasks) {
    const start = new Date(base.getTime() + created.length * 60 * 60 * 1000);
    await post('/mes/pro/protask', {
      workorderId: wo.workorderId,
      workstationId: t.station, routeId, processId: t.process,
      quantity: wo.detail.quantity, duration: t.duration,
      startTime: fmt(start), requestDate,
      colorCode: t.color || '#00AEF3', isCheck: t.isCheck, remark: '演示排产'
    }, `排产工序 ${t.process}`);
    created.push(t);
  }
  const list = await rows('/mes/pro/protask/list', { workorderId: wo.workorderId });
  log(`工单 ${wo.detail.workorderCode} 排产完成：${list.length} 个生产任务`);
  return list;
}

// ---------- 6. 生产领料 ----------
async function issueMaterial(wo, task, lines) {
  const issueCode = 'LL' + TS + wo.workorderId;
  await post('/mes/wm/issueheader', {
    issueCode, issueName: wo.detail.workorderCode + ' 生产领料',
    workorderId: wo.workorderId, workorderCode: wo.detail.workorderCode,
    taskId: task.taskId, taskCode: task.taskCode,
    workstationId: task.workstationId, workstationCode: task.workstationCode, workstationName: task.workstationName,
    issueDate: '2026-10-26 08:30:00', requiredTime: '2026-10-26 09:00:00', remark: '演示生产领料'
  }, '新增领料单');

  const issue = (await rows('/mes/wm/issueheader/list', { issueCode }))[0];
  if (!issue) throw new Error('领料单未查询到：' + issueCode);

  for (const ln of lines) {
    await post('/mes/wm/issueline', {
      issueId: issue.issueId, itemId: ln.itemId, itemCode: ln.code, itemName: ln.name,
      specification: ln.spec, unitOfMeasure: ln.unit, unitName: ln.unitName,
      quantityIssued: ln.qty, remark: '演示领料'
    }, `新增领料行 ${ln.code}`);
  }

  const ilLines = await rows('/mes/wm/issueline/list', { issueId: issue.issueId });
  for (const il of ilLines) {
    const stocks = await rows('/mes/wm/wmstock/list', { itemId: il.itemId, warehouseCode: 'WH-RAW' });
    if (!stocks.length) throw new Error(`原料仓无库存：${il.itemCode}`);
    const st = stocks.find(s => Number(s.quantityOnhand) >= Number(il.quantityIssued));
    if (!st) throw new Error(`原料仓库存不足：${il.itemCode}`);
    await post('/mes/wm/issuedetail', {
      issueId: issue.issueId, lineId: il.lineId,
      materialStockId: st.materialStockId, itemId: il.itemId, itemCode: il.itemCode, itemName: il.itemName,
      specification: il.specification, unitOfMeasure: il.unitOfMeasure, unitName: il.unitName,
      quantity: il.quantityIssued, batchId: st.batchId, batchCode: st.batchCode,
      warehouseId: st.warehouseId, warehouseCode: st.warehouseCode, warehouseName: st.warehouseName,
      locationId: st.locationId, locationCode: st.locationCode, locationName: st.locationName,
      areaId: st.areaId, areaCode: st.areaCode, areaName: st.areaName,
      remark: '演示领料明细'
    }, `领料明细 ${il.itemCode}`);
  }

  let head = (await get(`/mes/wm/issueheader/${issue.issueId}`)).data;
  head.status = 'APPROVING';
  await put('/mes/wm/issueheader', head, '领料单提交');
  const cq = await get(`/mes/wm/issueheader/checkQuantity/${issue.issueId}`);
  if (cq.data !== true) throw new Error('领料数量核对未通过：' + issueCode);
  head = (await get(`/mes/wm/issueheader/${issue.issueId}`)).data;
  head.status = 'APPROVED';
  await put('/mes/wm/issueheader', head, '领料单提交待执行');
  await put(`/mes/wm/issueheader/${issue.issueId}`, undefined, '执行生产领料（原料仓→线边库）');
}

// ---------- 7. 生产报工 ----------
async function feedback(wo, task, qty, isCheck, qualified, unqualified) {
  const item = wo.detail;
  await post('/mes/pro/feedback', {
    feedbackCode: 'BG' + TS + task.taskId,
    feedbackType: 'SELF', feedbackChannel: 'PC',
    workstationId: task.workstationId, workstationCode: task.workstationCode, workstationName: task.workstationName,
    workorderId: wo.workorderId, workorderCode: item.workorderCode, workorderName: item.workorderName,
    routeId: task.routeId, routeCode: task.routeCode,
    processId: task.processId, processCode: task.processCode, processName: task.processName,
    taskId: task.taskId, taskCode: task.taskCode,
    itemId: task.itemId, itemCode: task.itemCode, itemName: task.itemName,
    unitOfMeasure: task.unitOfMeasure, unitName: task.unitName, specification: task.specification,
    quantityFeedback: qty, quantityQualified: qualified,
    quantityUnquanlified: unqualified, quantityUncheck: 0,
    isCheck, feedbackTime: '2026-10-26 10:00:00',
    lotNumber: 'TG-LOT-' + TS, status: 'PREPARE', remark: '演示报工'
  }, `新增报工 ${task.processName}`);

  const fb = (await rows('/mes/pro/feedback/list', { taskId: task.taskId }))[0];
  if (!fb) throw new Error('报工单未查询到，taskId=' + task.taskId);
  await put(`/mes/pro/feedback/${fb.recordId}`, undefined, `执行报工 ${task.processName}`);
  return fb;
}

// ---------- 8. 过程检验 ----------
async function ipqc(wo, fbRecord) {
  const produces = await rows('/mes/wm/productproduce/list', { feedbackId: fbRecord.recordId });
  if (!produces.length) throw new Error('未生成产品产出单，无法过程检验');
  const produce = produces[0];
  const pLines = await rows('/mes/wm/productproduceline/list', { recordId: produce.recordId });
  const pl = pLines[0];
  if (!pl) throw new Error('产品产出单无明细行');
  const qty = Number(pl.quantityProduce);

  const created = await post('/mes/qc/ipqc', {
    ipqcCode: 'IPQC' + TS + produce.recordId,
    ipqcName: produce.workorderCode + ' 过程检验',
    ipqcType: 'IPQC',
    workorderId: wo.workorderId, workorderCode: wo.detail.workorderCode, workorderName: wo.detail.workorderName,
    taskId: fbRecord.taskId, taskCode: fbRecord.taskCode, taskName: fbRecord.processName,
    workstationId: fbRecord.workstationId, workstationCode: fbRecord.workstationCode, workstationName: fbRecord.workstationName,
    processId: fbRecord.processId, processCode: fbRecord.processCode, processName: fbRecord.processName,
    itemId: produce.itemId, itemCode: produce.itemCode, itemName: produce.itemName,
    specification: produce.specification, unitOfMeasure: produce.unitOfMeasure, unitName: produce.unitName,
    sourceDocType: 'FEEDBACK', sourceDocId: produce.recordId, sourceDocCode: produce.workorderCode,
    sourceLineId: pl.lineId,
    quantityCheck: qty, quantityQualified: qty, quantityUnqualified: 0,
    inspectDate: '2026-10-26 11:00:00', status: 'PREPARE', remark: '演示过程检验'
  }, '新增过程检验单');

  const ipqcId = created.data;
  const d = (await get(`/mes/qc/ipqc/${ipqcId}`)).data;
  Object.assign(d, {
    status: 'FINISHED', checkResult: 'ACCEPT',
    quantityCheck: qty, quantityQualified: qty, quantityUnqualified: 0
  });
  await put('/mes/qc/ipqc', d, '过程检验判定合格（产出）');
  log(`过程检验完成：${wo.detail.workorderCode} 合格 ${qty}`);
}

// ---------- 9. 成品入库 ----------
async function productIn(wo, qty) {
  const recptCode = 'CPRK' + TS + wo.workorderId;
  await post('/mes/wm/productrecpt', {
    recptCode, recptName: wo.detail.workorderCode + ' 成品入库',
    workorderId: wo.workorderId, workorderCode: wo.detail.workorderCode, workorderName: wo.detail.workorderName,
    itemId: ITEM.fg, itemCode: 'TG-FG-001', itemName: '红外测温枪整机',
    specification: 'TG-300 便携式红外测温枪', unitOfMeasure: 'TAI', unitName: '台',
    recptDate: '2026-10-26 14:00:00', remark: '演示成品入库'
  }, '新增成品入库单');

  const recpt = (await rows('/mes/wm/productrecpt/list', { recptCode }))[0];
  if (!recpt) throw new Error('成品入库单未查询到：' + recptCode);

  const stocks = await rows('/mes/wm/wmstock/list', { itemId: ITEM.fg, warehouseCode: 'XBK_VIRTUAL' });
  const st = stocks[0];
  if (!st) throw new Error('线边库无整机库存，无法成品入库');

  await post('/mes/wm/productrecptline', {
    recptId: recpt.recptId, itemId: ITEM.fg, itemCode: 'TG-FG-001', itemName: '红外测温枪整机',
    specification: 'TG-300 便携式红外测温枪', unitOfMeasure: 'TAI', unitName: '台',
    quantityRecived: qty, batchId: st.batchId, batchCode: st.batchCode, remark: '演示入库行'
  }, '新增成品入库行');

  let head = (await get(`/mes/wm/productrecpt/${recpt.recptId}`)).data;
  head.status = 'APPROVING';
  await put('/mes/wm/productrecpt', head, '成品入库提交待上架');

  const rLines = await rows('/mes/wm/productrecptline/list', { recptId: recpt.recptId });
  for (const rl of rLines) {
    await post('/mes/wm/productrecptdetail', {
      recptId: recpt.recptId, lineId: rl.lineId,
      itemId: ITEM.fg, itemCode: 'TG-FG-001', itemName: '红外测温枪整机',
      specification: 'TG-300 便携式红外测温枪', unitOfMeasure: 'TAI', unitName: '台',
      quantity: rl.quantityRecived, batchId: rl.batchId, batchCode: rl.batchCode,
      warehouseId: WH.fg, warehouseCode: 'WH-FG', warehouseName: '成品仓',
      locationId: LOC.fg, locationCode: 'L-FG', locationName: '成品区',
      areaId: AREA.fg, areaCode: 'FG-01', areaName: '成品库位', remark: '演示成品上架'
    }, '成品上架');
  }

  const cq = await get(`/mes/wm/productrecpt/checkQuantity/${recpt.recptId}`);
  if (cq.data !== true) throw new Error('成品入库数量核对未通过');
  head = (await get(`/mes/wm/productrecpt/${recpt.recptId}`)).data;
  head.status = 'APPROVED';
  await put('/mes/wm/productrecpt', head, '成品入库提交待执行');
  await put(`/mes/wm/productrecpt/${recpt.recptId}`, undefined, '执行成品入库（线边库→成品仓）');
  log(`成品入库完成：${qty} 台进入成品仓`);
}

// ---------- 10. 销售出库 + OQC ----------
async function sales(qty) {
  const salesCode = 'XS' + TS;
  await post('/mes/wm/productsales', {
    salesCode, salesName: '测温枪整机销售出库',
    clientId: CLIENT, clientCode: 'C-TG-001', clientName: '华东医疗器械经销有限公司',
    clientNick: '华东医疗', salesDate: '2026-10-27 09:00:00', soCode: 'SO' + TS,
    recipient: '陈经理', tel: '13900001001', address: '上海市浦东新区张江路 88 号',
    carrier: '顺丰速运', shippingNumber: 'SF' + TS, status: 'PREPARE', remark: '演示销售出库'
  }, '新增销售出库单');

  const sale = (await rows('/mes/wm/productsales/list', { salesCode }))[0];
  if (!sale) throw new Error('销售出库单未查询到：' + salesCode);

  const stocks = await rows('/mes/wm/wmstock/list', { itemId: ITEM.fg, warehouseCode: 'WH-FG' });
  const st = stocks[0];
  if (!st) throw new Error('成品仓无整机库存');

  await post('/mes/wm/productsalesline', {
    salesId: sale.salesId, itemId: ITEM.fg, itemCode: 'TG-FG-001', itemName: '红外测温枪整机',
    specification: 'TG-300 便携式红外测温枪', unitOfMeasure: 'TAI', unitName: '台',
    quantitySales: qty, oqcCheck: 'Y', qualityStatus: '', batchCode: st.batchCode, remark: '演示销售行'
  }, '新增销售出库行');

  const sl = (await rows('/mes/wm/productsalesline/list', { salesId: sale.salesId }))[0];

  await post('/mes/wm/productsalesdetail', {
    salesId: sale.salesId, lineId: sl.lineId, materialStockId: st.materialStockId,
    itemId: ITEM.fg, itemCode: 'TG-FG-001', itemName: '红外测温枪整机',
    specification: 'TG-300 便携式红外测温枪', unitOfMeasure: 'TAI', unitName: '台',
    quantity: qty, batchId: st.batchId, batchCode: st.batchCode,
    warehouseId: st.warehouseId, warehouseCode: st.warehouseCode, warehouseName: st.warehouseName,
    locationId: st.locationId, locationCode: st.locationCode, locationName: st.locationName,
    areaId: st.areaId, areaCode: st.areaCode, areaName: st.areaName, remark: '演示拣货'
  }, '新增销售拣货明细');

  const oqcCreated = await post('/mes/qc/oqc', {
    oqcCode: 'OQC' + TS, oqcName: '测温枪整机出货检验',
    sourceDocType: 'PRODUCT_SALES', sourceDocId: sale.salesId, sourceLineId: sl.lineId,
    clientId: CLIENT, clientCode: 'C-TG-001', clientName: '华东医疗器械经销有限公司',
    batchCode: st.batchCode,
    itemId: ITEM.fg, itemCode: 'TG-FG-001', itemName: '红外测温枪整机',
    specification: 'TG-300 便携式红外测温枪', unitOfMeasure: 'TAI',
    quantityOut: qty, quantityCheck: qty, quantityQualified: qty, quantityUnqualified: 0,
    inspectDate: '2026-10-27 10:00:00', status: 'PREPARE', remark: '演示出货检验'
  }, '新增出货检验单');
  const oqcId = oqcCreated.data;
  const oqc = (await get(`/mes/qc/oqc/${oqcId}`)).data;
  Object.assign(oqc, { status: 'FINISHED', checkResult: 'ACCEPT', quantityQualified: qty, quantityUnqualified: 0 });
  await put('/mes/qc/oqc', oqc, '出货检验判定合格');
  log('出货检验完成：判定合格');

  for (const status of ['UNSTOCK', 'UNSHIPPING', 'UNEXECUTE']) {
    const head = (await get(`/mes/wm/productsales/${sale.salesId}`)).data;
    head.status = status;
    await put('/mes/wm/productsales', head, `销售单流转 ${status}`);
    const after = (await get(`/mes/wm/productsales/${sale.salesId}`)).data;
    if (after.status !== status) throw new Error(`销售单未能流转到 ${status}，当前 ${after.status}`);
  }
  const cq = await get(`/mes/wm/productsales/checkQuantity/${sale.salesId}`);
  if (cq.data !== true) throw new Error('销售出库数量核对未通过');
  await put(`/mes/wm/productsales/${sale.salesId}`, undefined, '执行销售出库（扣减成品仓）');
  log(`销售出库完成：${qty} 台发货给华东医疗`);
}

// ---------- 主流程 ----------
(async () => {
  console.log('=== 红外测温枪 MES 全流程演示（生产 10 台 / 销售 6 台 / 结存 4 台）===\n');
  await login();

  // A. 采购到货 + IQC + 采购入库
  const arrElec = await createArrival({
    code: 'DH' + TS + '01', name: '电子件到货', vendorId: VENDOR.elec, vendorCode: 'V-TG-001', vendorName: '深圳市芯控电子有限公司',
    lines: [
      line('pcba', 10), line('sensor', 10), line('lcd', 10), line('keypad', 10),
      line('buzzer', 10), line('battery', 20), line('lens', 10)
    ]
  });
  log('电子件到货单创建完成');
  await iqcArrival(arrElec);
  await purchaseIn(arrElec, AREA.ec, LOC.ec);

  const arrStruct = await createArrival({
    code: 'DH' + TS + '02', name: '结构件到货', vendorId: VENDOR.struct, vendorCode: 'V-TG-002', vendorName: '东莞市精密结构件有限公司',
    lines: [line('top', 10), line('bottom', 10), line('trigger', 10), line('screw', 10), line('foam', 10)]
  });
  log('结构件到货单创建完成');
  await iqcArrival(arrStruct);
  await purchaseIn(arrStruct, AREA.st, LOC.st);

  const arrPack = await createArrival({
    code: 'DH' + TS + '03', name: '包装材料到货', vendorId: VENDOR.pack, vendorCode: 'V-TG-003', vendorName: '广州市彩印包装有限公司',
    lines: [line('manual', 10), line('box', 10)]
  });
  log('包装材料到货单创建完成');
  await iqcArrival(arrPack);
  await purchaseIn(arrPack, AREA.pk, LOC.pk);

  // B. 生产工单
  const woEc = await createWorkorder({ code: 'MO' + TS + '01', name: '测温显示电控预装件 10 套', productId: ITEM.ec, productCode: 'TG-SF-001', productName: '测温显示电控预装件', unit: 'SET', qty: 10 });
  const woHd = await createWorkorder({ code: 'MO' + TS + '02', name: '扳机与侧键手柄预装件 10 套', productId: ITEM.hd, productCode: 'TG-SF-002', productName: '扳机与侧键手柄预装件', unit: 'SET', qty: 10 });
  const woFg = await createWorkorder({ code: 'MO' + TS + '03', name: '红外测温枪整机 10 台', productId: ITEM.fg, productCode: 'TG-FG-001', productName: '红外测温枪整机', unit: 'TAI', qty: 10 });

  // C. 排产
  const tEc = await schedule(woEc, ROUTE.ec, [
    { process: PROCESS.op10, station: STATION.op10, duration: 240, isCheck: 'N' },
    { process: PROCESS.op20, station: STATION.op20, duration: 120, isCheck: 'Y', color: '#FF4949' }
  ]);
  const tHd = await schedule(woHd, ROUTE.hd, [
    { process: PROCESS.op30, station: STATION.op30, duration: 180, isCheck: 'N' },
    { process: PROCESS.op40, station: STATION.op40, duration: 90, isCheck: 'Y', color: '#FF4949' }
  ]);
  const tFg = await schedule(woFg, ROUTE.fg, [
    { process: PROCESS.op50, station: STATION.op50, duration: 240, isCheck: 'N' },
    { process: PROCESS.op60, station: STATION.op60, duration: 120, isCheck: 'N' },
    { process: PROCESS.op70, station: STATION.op70, duration: 120, isCheck: 'Y', color: '#FF4949' },
    { process: PROCESS.op80, station: STATION.op80, duration: 60, isCheck: 'N' },
    { process: PROCESS.op90, station: STATION.op90, duration: 60, isCheck: 'N' }
  ]);
  const byProcess = (list, pid) => list.find(t => String(t.processId) === String(pid));

  // D. 领料
  await issueMaterial(woEc, byProcess(tEc, PROCESS.op10), [
    line('pcba', 10), line('sensor', 10), line('lcd', 10), line('buzzer', 10), line('lens', 10)
  ]);
  log('电控预装件领料完成');

  await issueMaterial(woHd, byProcess(tHd, PROCESS.op30), [
    line('keypad', 10), line('battery', 20), line('trigger', 10)
  ]);
  log('手柄预装件领料完成');

  await issueMaterial(woFg, byProcess(tFg, PROCESS.op50), [
    line('top', 10), line('bottom', 10), line('screw', 10), line('foam', 10),
    line('manual', 10), line('box', 10)
  ]);
  log('整机结构件与包装材料领料完成');

  // E. 报工 + 过程检验
  await feedback(woEc, byProcess(tEc, PROCESS.op10), 10, 'N', 10, 0);
  log('电控预装工序报工完成');
  const fbEc = await feedback(woEc, byProcess(tEc, PROCESS.op20), 10, 'Y', 10, 0);
  log('电控测试工序报工完成（待检验）');
  await ipqc(woEc, fbEc);

  await feedback(woHd, byProcess(tHd, PROCESS.op30), 10, 'N', 10, 0);
  log('手柄预装工序报工完成');
  const fbHd = await feedback(woHd, byProcess(tHd, PROCESS.op40), 10, 'Y', 10, 0);
  log('手柄测试工序报工完成（待检验）');
  await ipqc(woHd, fbHd);

  for (const pid of [PROCESS.op50, PROCESS.op60, PROCESS.op80, PROCESS.op90]) {
    await feedback(woFg, byProcess(tFg, pid), 10, 'N', 10, 0);
    log(`整机工序报工完成：${byProcess(tFg, pid).processName}`);
  }
  const fbFg = await feedback(woFg, byProcess(tFg, PROCESS.op70), 10, 'Y', 10, 0);
  log('整机温度校准工序报工完成（待检验）');
  await ipqc(woFg, fbFg);

  // F. 成品入库 + 销售出库
  await productIn(woFg, 10);
  await sales(6);

  // G. 结果核对：单据数量 + 库存结存
  console.log('\n=== 单据核对 ===');
  const woList = await rows('/mes/pro/workorder/list', { productId: ITEM.fg });
  for (const w of woList) console.log(`  工单 ${w.workorderCode} 计划 ${w.quantity} 已生产 ${w.quantityProduced} 状态 ${w.status}`);
  const recptList = await rows('/mes/wm/productrecpt/list', {});
  console.log(`  成品入库单 ${recptList.length} 张`);
  const saleList = await rows('/mes/wm/productsales/list', {});
  console.log(`  销售出库单 ${saleList.length} 张`);

  console.log('\n=== 库存结存（整机入库 10 台、销售 6 台后，成品仓应结存 4 台）===');
  for (const code of ['TG-EL-001', 'TG-EL-002', 'TG-ST-001', 'TG-FG-001']) {
    const ss = await rows('/mes/wm/wmstock/list', { itemCode: code });
    const desc = ss.map(s => `${s.warehouseName}:${Number(s.quantityOnhand)}`).join(' | ');
    console.log(`  ${code}: ${desc || '无结存（已全部消耗/发货）'}`);
  }
  console.log('\n=== 全流程执行完成 ===');
})().catch(e => {
  console.error('\n' + e.message);
  process.exit(1);
});

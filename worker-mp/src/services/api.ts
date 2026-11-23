import { request, requestRaw } from './request';

export interface LoginResult {
  token: string;
}

export interface MesUser {
  userId: number;
  userName: string;
  nickName: string;
  dept?: { deptName?: string };
  deptName?: string;
}

export interface UserInfo {
  user: MesUser;
  roles: string[];
  permissions: string[];
}

export interface Workorder {
  workorderId: number;
  workorderCode: string;
  workorderName?: string;
  productId?: number;
  productCode?: string;
  productName?: string;
  specification?: string;
  unitName?: string;
  quantity?: number;
  quantityProduced?: number;
  status?: string;
  sourceDocCode?: string;
  clientName?: string;
  requestDate?: string;
  [key: string]: unknown;
}

export interface ProTask {
  taskId: number;
  taskCode: string;
  workorderId: number;
  workorderCode: string;
  workstationId?: number;
  workstationName?: string;
  routeId?: number;
  processId?: number;
  processName?: string;
  isCheck?: string;
  itemId?: number;
  itemCode?: string;
  itemName?: string;
  specification?: string;
  unitName?: string;
  quantity?: number;
  quantityProduced?: number;
  status?: string;
  [key: string]: unknown;
}

export interface Feedback {
  recordId: number;
  feedbackCode: string;
  workorderId: number;
  workorderCode: string;
  taskId?: number;
  taskCode?: string;
  itemName?: string;
  quantityFeedback?: number;
  quantityQualified?: number;
  quantityUnquanlified?: number;
  status?: string;
  feedbackTime?: string;
  [key: string]: unknown;
}

export interface PendingInspect {
  sourceDocId?: number;
  sourceDocCode?: string;
  sourceDocType?: string;
  sourceLineId?: number;
  qcDetailType?: string;
  workOrderId?: number;
  workOrderCode?: string;
  workOrderName?: string;
  taskId?: number;
  taskCode?: string;
  workstationId?: number;
  workstationName?: string;
  itemId?: number;
  itemCode?: string;
  itemName?: string;
  specification?: string;
  unitName?: string;
  quantityCheck?: number;
  batchCode?: string;
  qcType?: string;
  [key: string]: unknown;
}

export interface ProductRecpt {
  recptId: number;
  recptCode: string;
  recptName?: string;
  workorderId?: number;
  workorderCode?: string;
  itemCode?: string;
  itemName?: string;
  quantity?: number;
  status?: string;
  recptDate?: string;
  [key: string]: unknown;
}

export interface IssueHeader {
  issueId: number;
  issueCode: string;
  workorderId?: number;
  workorderCode?: string;
  status?: string;
  issueDate?: string;
  [key: string]: unknown;
}

export interface PageQuery {
  pageNum?: number;
  pageSize?: number;
  [key: string]: unknown;
}

export function login(username: string, password: string) {
  return request<{ token: string }>({
    url: '/api/worker/login',
    method: 'POST',
    data: { username, password }
  });
}

export function getInfo() {
  return request<UserInfo>({ url: '/getInfo', method: 'GET' });
}

export function logout() {
  return requestRaw({ url: '/logout', method: 'POST' });
}

export async function listWorkorders(query: PageQuery): Promise<{ rows: Workorder[]; total: number }> {
  const body = await requestRaw<Workorder[]>({
    url: '/mes/pro/workorder/list',
    params: { pageNum: 1, pageSize: 10, ...query }
  });
  return { rows: body.rows || [], total: body.total || 0 };
}

export function getWorkorder(workorderId: number) {
  return request<Workorder>({ url: `/mes/pro/workorder/${workorderId}`, method: 'GET' });
}

export function confirmWorkorder(workorder: Workorder) {
  return requestRaw({
    url: '/mes/pro/workorder',
    method: 'PUT',
    data: { ...workorder, status: 'CONFIRMED' }
  });
}

export function finishWorkorder(workorderId: number) {
  return requestRaw({ url: `/mes/pro/workorder/finish/${workorderId}`, method: 'PUT' });
}

export async function listTasks(workorderId: number): Promise<ProTask[]> {
  const body = await requestRaw<ProTask[]>({
    url: '/mes/pro/protask/list',
    params: { workorderId, pageNum: 1, pageSize: 200 }
  });
  return body.rows || [];
}

export function getTask(taskId: number) {
  return request<ProTask>({ url: `/mes/pro/protask/${taskId}`, method: 'GET' });
}

export function addFeedback(data: Record<string, unknown>) {
  return request<number>({ url: '/mes/pro/feedback', method: 'POST', data });
}

export async function listFeedbacks(workorderId: number): Promise<Feedback[]> {
  const body = await requestRaw<Feedback[]>({
    url: '/mes/pro/feedback/list',
    params: { workorderId, pageNum: 1, pageSize: 200 }
  });
  return body.rows || [];
}

export async function listPendingInspects(query: PageQuery = {}): Promise<PendingInspect[]> {
  const body = await requestRaw<PendingInspect[]>({
    url: '/mes/qc/pending/list',
    params: { pageNum: 1, pageSize: 100, ...query }
  });
  return body.rows || [];
}

export function nextBizCode(ruleCode: string, referenceCode?: string) {
  return request<string>({
    url: `/mes/code/next/${ruleCode}`,
    params: referenceCode ? { referenceCode } : undefined
  });
}

export function addIpqc(data: Record<string, unknown>) {
  return request<number>({ url: '/mes/qc/ipqc', method: 'POST', data });
}

export function updateIpqc(data: Record<string, unknown>) {
  return requestRaw({ url: '/mes/qc/ipqc', method: 'PUT', data });
}

export async function listProductRecpts(query: PageQuery = {}): Promise<ProductRecpt[]> {
  const body = await requestRaw<ProductRecpt[]>({
    url: '/mes/wm/productrecpt/list',
    params: { pageNum: 1, pageSize: 50, ...query }
  });
  return body.rows || [];
}

export function executeProductRecpt(recptId: number) {
  return requestRaw({ url: `/mes/wm/productrecpt/${recptId}`, method: 'PUT' });
}

export function confirmStocking(recptId: number) {
  return requestRaw({ url: `/mes/wm/productrecpt/confirmStocking/${recptId}`, method: 'POST' });
}

export async function listIssues(query: PageQuery = {}): Promise<IssueHeader[]> {
  const body = await requestRaw<IssueHeader[]>({
    url: '/mes/wm/issueheader/list',
    params: { pageNum: 1, pageSize: 50, ...query }
  });
  return body.rows || [];
}

export function autoPickIssue(issueId: number) {
  return requestRaw({ url: `/mes/wm/issueheader/${issueId}/auto-pick`, method: 'POST' });
}

export function executeIssue(issueId: number) {
  return requestRaw({ url: `/mes/wm/issueheader/${issueId}`, method: 'PUT' });
}

/** 首页/待办统计：并发拉取各状态单据数量 */
export async function loadTodoSummary(): Promise<{
  pendingInspect: PendingInspect[];
  receipts: ProductRecpt[];
  issues: IssueHeader[];
  workorders: Workorder[];
}> {
  const [pendingInspect, approvingRecpt, approvedRecpt, approvingIssue, approvedIssue, workorders] =
    await Promise.all([
      listPendingInspects(),
      listProductRecpts({ status: 'APPROVING' }),
      listProductRecpts({ status: 'APPROVED' }),
      listIssues({ status: 'APPROVING' }),
      listIssues({ status: 'APPROVED' }),
      listWorkorders({ status: 'CONFIRMED', pageSize: 5 })
    ]);
  return {
    pendingInspect,
    receipts: [...approvingRecpt, ...approvedRecpt],
    issues: [...approvingIssue, ...approvedIssue],
    workorders: workorders.rows
  };
}

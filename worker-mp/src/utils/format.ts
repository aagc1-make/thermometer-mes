export const WORKORDER_STATUS: Record<string, string> = {
  PREPARE: '草稿',
  CONFIRMED: '已确认',
  APPROVING: '审批中',
  APPROVED: '已审批',
  FINISHED: '已完工',
  CANCELED: '已取消'
};

export const RECPT_STATUS: Record<string, string> = {
  PREPARE: '草稿',
  APPROVING: '待上架',
  APPROVED: '已上架',
  FINISHED: '已入库',
  CANCELED: '已取消'
};

export const ISSUE_STATUS: Record<string, string> = {
  PREPARE: '草稿',
  APPROVING: '待拣货',
  APPROVED: '已拣货',
  FINISHED: '已领出',
  CANCELED: '已取消'
};

export const FEEDBACK_STATUS: Record<string, string> = {
  PREPARE: '草稿',
  APPROVING: '待审核',
  UNCHECK: '待检验',
  FINISHED: '已完成',
  CANCELED: '已取消'
};

export function statusText(map: Record<string, string>, status?: string): string {
  if (!status) {
    return '-';
  }
  return map[status] || status;
}

export function today(): string {
  const now = new Date();
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

export function nowText(): string {
  const now = new Date();
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${today()} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
}

export function num(value: unknown, fallback = 0): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

export function dateOnly(value?: unknown): string {
  if (!value) {
    return '-';
  }
  return String(value).slice(0, 10);
}

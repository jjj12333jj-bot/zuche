/**
 * 金额与时间的统一格式化
 *
 * 后端 LocalDateTime 序列化为 ISO（2026-10-28T23:30:00），
 * 这里统一转成 "2026-10-28 23:30"，避免各页面各写一套 slice。
 */

export const CREDIT_LEVELS = {
  EXCELLENT: { label: '优秀', color: '#67c23a' },
  GOOD: { label: '良好', color: '#409eff' },
  NORMAL: { label: '一般', color: '#e6a23c' },
  WATCH: { label: '待观察', color: '#f56c6c' }
}

export const ORDER_STATUS_TAG = {
  WAIT_PAY: 'warning',
  WAIT_PICKUP: 'primary',
  IN_USE: 'success',
  WAIT_RETURN: 'info',
  RETURNED: 'info',
  COMPLETED: 'success',
  CANCELLED: 'info',
  OVERDUE: 'danger'
}

export function formatMoney(value) {
  const n = Number(value || 0)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function formatDate(value) {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

export function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

/** 系数转折扣文案：0.8 → 8 折，1.2 → 1.2 倍 */
export function coefficientText(coefficient) {
  const c = Number(coefficient || 1)
  if (c === 1) return '原价'
  if (c < 1) return `${(c * 10).toFixed(1).replace(/\.0$/, '')} 折`
  return `${c} 倍`
}

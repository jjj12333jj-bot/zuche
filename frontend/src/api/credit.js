import request from '@/utils/request'

/** 信用中心概览：分数/等级/押金系数 + 行为统计 + 等级规则 */
export function getCreditCenter() {
  return request.get('/credit/center')
}

/** 信用明细分页：{ current, pageSize, changeType } */
export function getCreditLogs(params) {
  return request.get('/credit/logs', { params })
}

/** 四档等级规则（分数区间、押金系数） */
export function getCreditLevels() {
  return request.get('/credit/levels')
}

/** 提交信用申诉：{ creditLogId, reason } */
export function createAppeal(data) {
  return request.post('/credit/appeals', data)
}

/** 我的申诉列表 */
export function getMyAppeals(params) {
  return request.get('/credit/appeals', { params })
}

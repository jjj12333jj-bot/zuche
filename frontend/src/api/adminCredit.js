import request from '@/utils/request'

/* ==================== 规则配置 ==================== */

/** 规则列表：ruleType 为空查全部，可选 LEVEL / ACTION / PARAM */
export function getRules(ruleType) {
  return request.get('/admin/credit/rules', { params: { ruleType } })
}

/** 修改事件规则或风控参数（等级规则走整组保存） */
export function updateRule(id, data) {
  return request.put(`/admin/credit/rules/${id}`, data)
}

/** 整组保存等级规则：四档区间必须从 0 分起、首尾相接 */
export function updateLevelRules(list) {
  return request.put('/admin/credit/rules/levels', list)
}

/** 按当前等级区间重算全部用户等级 */
export function recalcLevels() {
  return request.post('/admin/credit/rules/recalc')
}

/* ==================== 用户信用 ==================== */

export function getAdminUsers(params) {
  return request.get('/admin/credit/users', { params })
}

/** 人工调分：{ scoreChange, reason } */
export function adjustCredit(userId, data) {
  return request.post(`/admin/credit/users/${userId}/adjust`, data)
}

export function getUserLogs(userId, params) {
  return request.get(`/admin/credit/users/${userId}/logs`, { params })
}

/* ==================== 申诉 ==================== */

export function getAppeals(params) {
  return request.get('/admin/credit/appeals', { params })
}

/** 审批：{ approved, restoreScore, remark } */
export function handleAppeal(id, data) {
  return request.post(`/admin/credit/appeals/${id}/handle`, data)
}

/* ==================== 风控名单 ==================== */

export function getBlacklist(params) {
  return request.get('/admin/credit/blacklist', { params })
}

/** 手工加入：{ userId, listType, reason, expireDays } */
export function addBlacklist(data) {
  return request.post('/admin/credit/blacklist', data)
}

export function releaseBlacklist(id) {
  return request.post(`/admin/credit/blacklist/${id}/release`)
}

/* ==================== 逾期任务 ==================== */

/** 立即执行逾期扫描（与每日 01:00 定时任务同一段逻辑） */
export function runOverdueScan() {
  return request.post('/admin/risk/scan')
}

export function getRiskOverview() {
  return request.get('/admin/risk/overview')
}

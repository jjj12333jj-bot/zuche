import request from '@/utils/request'

/* ==================== 订单管理 ==================== */

/** 订单统计：各状态计数 + 今日新增 */
export function getOrderStats() {
  return request.get('/admin/orders/stats')
}

/** 订单分页：{ status, storeId, keyword, startFrom, startTo, page, size } */
export function getOrders(params) {
  return request.get('/admin/orders', { params })
}

/** 订单详情：含明细、支付流水、退款流水 */
export function getOrderDetail(id) {
  return request.get(`/admin/orders/${id}`)
}

/** 关闭订单：{ reason } */
export function closeOrder(id, data) {
  return request.post(`/admin/orders/${id}/close`, data)
}

/* ==================== 评价管理 ==================== */

export function getEvaluationStats() {
  return request.get('/admin/evaluations/stats')
}

/** 评价分页：{ rating, status, unreplied, keyword, page, size } */
export function getEvaluations(params) {
  return request.get('/admin/evaluations', { params })
}

/** 回复评价：{ reply } */
export function replyEvaluation(id, data) {
  return request.post(`/admin/evaluations/${id}/reply`, data)
}

/** 展示/隐藏：{ status } 1 展示 0 隐藏 */
export function updateEvaluationStatus(id, data) {
  return request.put(`/admin/evaluations/${id}/status`, data)
}

/* ==================== 用户管理 ==================== */

export function getUserStats() {
  return request.get('/admin/users/stats')
}

/** 用户分页：{ keyword, status, roleCode, level, page, size } */
export function getUsers(params) {
  return request.get('/admin/users', { params })
}

export function getUserDetail(id) {
  return request.get(`/admin/users/${id}`)
}

/** 冻结/解冻：{ status } 1 正常 0 冻结 */
export function updateUserStatus(id, data) {
  return request.put(`/admin/users/${id}/status`, data)
}

/** 人工调分：{ scoreChange, reason } */
export function adjustUserCredit(id, data) {
  return request.post(`/admin/users/${id}/credit`, data)
}

/* ==================== 网点管理 ==================== */

export function getStoreStats() {
  return request.get('/admin/stores/stats')
}

/** 网点分页：{ keyword, status, page, size } */
export function getStores(params) {
  return request.get('/admin/stores', { params })
}

export function getStoreDetail(id) {
  return request.get(`/admin/stores/${id}`)
}

export function createStore(data) {
  return request.post('/admin/stores', data)
}

export function updateStore(id, data) {
  return request.put(`/admin/stores/${id}`, data)
}

/** 停用/恢复营业：{ status } 1 营业 0 停用 */
export function updateStoreStatus(id, data) {
  return request.put(`/admin/stores/${id}/status`, data)
}

export function deleteStore(id) {
  return request.delete(`/admin/stores/${id}`)
}

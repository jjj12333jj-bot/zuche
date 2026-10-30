import request from '@/utils/request'

/** 网点概览：车辆存量、订单待办、今日与本月经营量 */
export function getStoreOverview() {
  return request.get('/store/overview')
}

/** 本网点订单分页：{ status, keyword, page, size } */
export function getStoreOrders(params) {
  return request.get('/store/orders', { params })
}

/** 办理交付（待取车 → 已取车） */
export function storePickup(id) {
  return request.post(`/store/orders/${id}/pickup`)
}

/** 办理归还并结算（待归还 / 逾期 → 已完成） */
export function storeConfirmReturn(id) {
  return request.post(`/store/orders/${id}/return-confirm`)
}

/* ==================== 车辆与台账 ==================== */

/** 本网点车辆分页：{ status, keyword, page, size } */
export function getStoreVehicles(params) {
  return request.get('/store/vehicles', { params })
}

/** 车辆调价：{ dailyPrice, deposit, remark } */
export function updateVehiclePrice(id, data) {
  return request.put(`/store/vehicles/${id}/price`, data)
}

/** 车辆上下架 / 进维保：{ status, reason } */
export function updateVehicleStatus(id, data) {
  return request.put(`/store/vehicles/${id}/status`, data)
}

/** 台账分页：{ maintType, vehicleId, keyword, page, size } */
export function getMaintenances(params) {
  return request.get('/store/maintenances', { params })
}

/** 登记保养/维修台账 */
export function addMaintenance(id, data) {
  return request.post(`/store/vehicles/${id}/maintenances`, data)
}

/* ==================== 调车 ==================== */

/** 网点调车申请分页：{ status, direction, keyword, page, size } */
export function getDispatches(params) {
  return request.get('/store/dispatches', { params })
}

/** 发起调车申请：{ vehicleId, toStoreId, reason } */
export function createDispatch(data) {
  return request.post('/store/dispatches', data)
}

/** 撤回调车申请 */
export function cancelDispatch(id) {
  return request.post(`/store/dispatches/${id}/cancel`)
}

/** 可调入网点下拉（已排除本网点） */
export function getTargetStores() {
  return request.get('/store/stores/options')
}

/* ==================== 平台审批 ==================== */

/** 平台调车申请分页 */
export function getAdminDispatches(params) {
  return request.get('/admin/dispatch', { params })
}

/** 审批通过 */
export function approveDispatch(id, data) {
  return request.post(`/admin/dispatch/${id}/approve`, data)
}

/** 审批驳回：{ remark } 必填 */
export function rejectDispatch(id, data) {
  return request.post(`/admin/dispatch/${id}/reject`, data)
}

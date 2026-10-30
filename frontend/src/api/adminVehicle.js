import request from '@/utils/request'

/**
 * 平台车辆资产接口
 *
 * 覆盖 5 个子模块：品牌、车型、车辆档案、保养维修、保险记录。
 * 写操作统一返回后端拼好的提示语（如「品牌「大众」已停用，该品牌下 9 台车…」），
 * 前端直接 el-message 展示即可，不要在页面里再拼一遍文案——
 * 同一个操作在列表页和详情页各写一份提示，迟早会不一致。
 */

/* ==================== 品牌 ==================== */

/** 三组全量统计：品牌 / 台账 / 保单，页面顶部数字卡用 */
export function getVehicleAssetStats() {
  return request.get('/admin/vehicle/stats')
}

export function getBrands(params) {
  return request.get('/admin/vehicle/brands', { params })
}

export function getBrandOptions() {
  return request.get('/admin/vehicle/brands/options')
}

export function createBrand(data) {
  return request.post('/admin/vehicle/brands', data)
}

export function updateBrand(id, data) {
  return request.put(`/admin/vehicle/brands/${id}`, data)
}

export function updateBrandStatus(id, status) {
  return request.put(`/admin/vehicle/brands/${id}/status`, { status })
}

export function deleteBrand(id) {
  return request.delete(`/admin/vehicle/brands/${id}`)
}

/* ==================== 车型 ==================== */

export function getModels(params) {
  return request.get('/admin/vehicle/models', { params })
}

export function getModelOptions(brandId) {
  return request.get('/admin/vehicle/models/options', { params: { brandId } })
}

/** 网点下拉：车辆建档用，只返回启用网点 */
export function getAdminStoreOptions() {
  return request.get('/admin/vehicle/stores')
}

export function createModel(data) {
  return request.post('/admin/vehicle/models', data)
}

export function updateModel(id, data) {
  return request.put(`/admin/vehicle/models/${id}`, data)
}

export function updateModelStatus(id, status) {
  return request.put(`/admin/vehicle/models/${id}/status`, { status })
}

export function deleteModel(id) {
  return request.delete(`/admin/vehicle/models/${id}`)
}

/* ==================== 车辆档案 ==================== */

export function getAdminVehicles(params) {
  return request.get('/admin/vehicle/list', { params })
}

export function createAdminVehicle(data) {
  return request.post('/admin/vehicle/list', data)
}

export function updateAdminVehicle(id, data) {
  return request.put(`/admin/vehicle/list/${id}`, data)
}

export function updateAdminVehicleStatus(id, status) {
  return request.put(`/admin/vehicle/list/${id}/status`, { status })
}

export function deleteAdminVehicle(id) {
  return request.delete(`/admin/vehicle/list/${id}`)
}

/* ==================== 保养维修 ==================== */

export function getMaintenances(params) {
  return request.get('/admin/vehicle/maintenance', { params })
}

export function addMaintenance(vehicleId, data) {
  return request.post(`/admin/vehicle/maintenance/vehicles/${vehicleId}`, data)
}

/* ==================== 保险 ==================== */

export function getInsurances(params) {
  return request.get('/admin/vehicle/insurance', { params })
}

export function createInsurance(data) {
  return request.post('/admin/vehicle/insurance', data)
}

export function updateInsurance(id, data) {
  return request.put(`/admin/vehicle/insurance/${id}`, data)
}

export function deleteInsurance(id) {
  return request.delete(`/admin/vehicle/insurance/${id}`)
}

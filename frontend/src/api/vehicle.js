import request from '@/utils/request'

/** 车辆分页列表：keyword/categoryCode/brandId/storeId/minPrice/maxPrice/sortBy/page/size */
export function getVehicles(params) {
  return request.get('/vehicles', { params })
}

/** 车辆详情（含图集、门店、车型参数） */
export function getVehicleDetail(id) {
  return request.get(`/vehicles/${id}`)
}

/** 列表筛选项：分类 / 品牌 / 网点 / 价格区间 */
export function getVehicleOptions() {
  return request.get('/vehicles/options')
}

/** 车辆用户评价：返回平均分、双维度均分、星级分布与评价列表（公开接口） */
export function getVehicleEvaluations(id, params) {
  return request.get(`/vehicles/${id}/evaluations`, { params })
}

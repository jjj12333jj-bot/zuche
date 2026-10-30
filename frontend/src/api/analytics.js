import request from '@/utils/request'

/**
 * 下单时段热力图
 *
 * 窗口默认 90 天：少于 60 天时，某些"星期 × 小时"格子样本太少，
 * 画出来是一片空白，看不出规律。
 */
export function getOrderHeatmap(days = 90) {
  return request.get('/admin/analytics/heatmap', { params: { days } })
}

/**
 * 车型偏好矩阵
 *
 * 与热力图共用窗口，两者放在同一屏，切换时一起刷新才不会口径打架。
 */
export function getModelPreference(days = 90) {
  return request.get('/admin/analytics/model-preference', { params: { days } })
}

/**
 * 营收时序预测
 *
 * @param {number} days 历史窗口，默认 45（够跑 4 周季节因子）
 * @param {number} horizon 预测天数，默认 7
 */
export function getRevenueForecast(days = 45, horizon = 7) {
  return request.get('/admin/analytics/revenue-forecast', { params: { days, horizon } })
}

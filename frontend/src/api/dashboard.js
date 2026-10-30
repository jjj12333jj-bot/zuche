import request from '@/utils/request'

/**
 * 运营看板总览
 *
 * 一次拿全部区块：KPI、趋势、漏斗、车辆状态、网点、车型、信用分布。
 * 拆成多个接口会让首屏出现"图表逐个跳出来"的观感，数据量不大没必要。
 *
 * @param {number} days 统计窗口天数，默认 30，后端上限 180
 */
export function getDashboardOverview(days = 30) {
  return request.get('/admin/dashboard/overview', { params: { days } })
}

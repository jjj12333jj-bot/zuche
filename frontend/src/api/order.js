import request from '@/utils/request'

/** 租赁试算：按当前用户信用等级算租金与押金（不落库） */
export function leaseQuote(data) {
  return request.post('/orders/lease-quote', data)
}

/** 创建订单：返回 orderId，前端跳收银台 */
export function createOrder(data) {
  return request.post('/orders', data)
}

/** 我的订单分页：{ status, page, size } */
export function getMyOrders(params) {
  return request.get('/orders', { params })
}

/** 订单详情：含明细与支付流水 */
export function getOrderDetail(id) {
  return request.get(`/orders/${id}`)
}

/** 支付：生成 RENT + DEPOSIT 两条流水并锁定车辆 */
export function payOrder(id, data) {
  return request.post(`/orders/${id}/pay`, data)
}

/** 取消订单：待支付只释放车辆；待取车（已支付）走退款 + 扣信用分 */
export function cancelOrder(id, data) {
  return request.post(`/orders/${id}/cancel`, data)
}

/** 确认取车：待取车 → 已取车 */
export function pickupOrder(id) {
  return request.post(`/orders/${id}/pickup`)
}

/** 申请归还：已取车/逾期 → 待归还 */
export function applyReturnOrder(id) {
  return request.post(`/orders/${id}/return-apply`)
}

/** 确认归还并结算：退还押金 + 信用结算 */
export function confirmReturnOrder(id) {
  return request.post(`/orders/${id}/return-confirm`)
}

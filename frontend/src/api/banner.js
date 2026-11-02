import request from '@/utils/request'

/**
 * 前台轮播图（无需登录）
 * 只返回后台「显示中」的记录，按排序号升序
 */
export function getBanners(params) {
  return request.get('/banners', { params })
}

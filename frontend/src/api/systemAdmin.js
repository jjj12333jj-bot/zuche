import request from '@/utils/request'

/* ==================== 公告管理 ==================== */

export function getAnnouncementStats() {
  return request.get('/admin/announcements/stats')
}

/** 公告分页：{ keyword, type, status, page, size } */
export function getAnnouncements(params) {
  return request.get('/admin/announcements', { params })
}

export function getAnnouncementDetail(id) {
  return request.get(`/admin/announcements/${id}`)
}

export function createAnnouncement(data) {
  return request.post('/admin/announcements', data)
}

export function updateAnnouncement(id, data) {
  return request.put(`/admin/announcements/${id}`, data)
}

/** 发布/下架：{ status } 1 发布 0 下架 */
export function updateAnnouncementStatus(id, data) {
  return request.put(`/admin/announcements/${id}/status`, data)
}

export function deleteAnnouncement(id) {
  return request.delete(`/admin/announcements/${id}`)
}

/* ==================== 轮播图管理 ==================== */

export function getBannerStats() {
  return request.get('/admin/banners/stats')
}

/** 轮播图分页：{ keyword, position, status, page, size } */
export function getBanners(params) {
  return request.get('/admin/banners', { params })
}

export function createBanner(data) {
  return request.post('/admin/banners', data)
}

export function updateBanner(id, data) {
  return request.put(`/admin/banners/${id}`, data)
}

/** 显示/隐藏：{ status } 1 显示 0 隐藏 */
export function updateBannerStatus(id, data) {
  return request.put(`/admin/banners/${id}/status`, data)
}

export function deleteBanner(id) {
  return request.delete(`/admin/banners/${id}`)
}

/* ==================== 操作日志 ==================== */

export function getLogStats() {
  return request.get('/admin/logs/stats')
}

/** 日志分页：{ keyword, module, status, startDate, endDate, page, size } */
export function getLogs(params) {
  return request.get('/admin/logs', { params })
}

export function getLogModules() {
  return request.get('/admin/logs/modules')
}

export function getLogDetail(id) {
  return request.get(`/admin/logs/${id}`)
}

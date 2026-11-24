import request from '@/utils/request'

/** 公告列表（只含摘要字段），前台首页与公告栏使用 */
export function getAnnouncements() {
  return request.get('/announcements')
}

/** 公告详情（含正文） */
export function getAnnouncement(id) {
  return request.get(`/announcements/${id}`)
}

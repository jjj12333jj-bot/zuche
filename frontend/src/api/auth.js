import request from '@/utils/request'

export function login(data) {
  return request.post('/auth/login', data)
}

export function register(data) {
  return request.post('/auth/register', data)
}

export function getCurrentUser() {
  return request.get('/auth/me')
}

/** 当前角色可见菜单树（M/C 类型），后台侧边栏据此动态渲染 */
export function getMenus() {
  return request.get('/auth/menus')
}

export function changePassword(data) {
  return request.put('/auth/password', data)
}

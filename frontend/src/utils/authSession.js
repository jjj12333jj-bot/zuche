/**
 * 标签页级登录态（sessionStorage）
 *
 * 为什么不用 localStorage：
 *   localStorage 的作用域是整个 origin，同一浏览器里所有标签页读写的是同一份数据。
 *   在 A 标签页用 admin 登录、B 标签页再登录 user1，B 会把 token 覆盖掉；
 *   A 页面的界面仍是 admin（内存里的 pinia 没变），但每次请求读到的却是 user1 的 token，
 *   于是调用管理员接口就 403 —— 表现就是「页面看着没变，功能却说没权限」。
 *
 * 改用 sessionStorage：
 *   作用域是单个标签页，每个标签页各持一份登录态。刷新页面不丢（同一标签页），
 *   新开/复制的标签页互不影响，可以同时开 系统管理员 + 网点管理员 + 多个普通用户。
 *
 * 代价：关掉标签页登录态即失效；新标签页需要重新登录一次。
 *   这是"多账号并行"的必然取舍——否则就又要回到同源共享。
 */
const TOKEN_KEY = 'zuche_token'
const USER_KEY = 'zuche_user'

/** 升级兼容：老版本把登录态放在 localStorage，第一个加载的标签页接管一次后清掉，避免被后来打开的标签页复用 */
function migrateLegacyOnce() {
  try {
    const legacyToken = localStorage.getItem(TOKEN_KEY)
    if (legacyToken && !sessionStorage.getItem(TOKEN_KEY)) {
      sessionStorage.setItem(TOKEN_KEY, legacyToken)
      const legacyUser = localStorage.getItem(USER_KEY)
      if (legacyUser) sessionStorage.setItem(USER_KEY, legacyUser)
    }
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  } catch (e) {
    // 隐私模式下 storage 可能不可写，忽略即可，走未登录流程
  }
}

migrateLegacyOnce()

export function getToken() {
  try {
    return sessionStorage.getItem(TOKEN_KEY) || ''
  } catch (e) {
    return ''
  }
}

export function getUser() {
  try {
    const raw = sessionStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    return null
  }
}

/** 登录/注册成功后写入：token 与用户信息一起落，避免出现有 token 没身份的中间态 */
export function saveAuth(data) {
  try {
    sessionStorage.setItem(TOKEN_KEY, data.token)
    sessionStorage.setItem(USER_KEY, JSON.stringify(data))
  } catch (e) {
    // ignore
  }
}

/** 只更新用户信息（信用分等实时字段），token 不变 */
export function saveUser(userInfo) {
  try {
    sessionStorage.setItem(USER_KEY, JSON.stringify(userInfo))
  } catch (e) {
    // ignore
  }
}

/** 清空当前标签页登录态，其他标签页不受影响 */
export function clearAuth() {
  try {
    sessionStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(USER_KEY)
  } catch (e) {
    // ignore
  }
}

/** 新标签页换账号登录：带 switch=1，登录页会先清掉本标签页继承来的身份；返回是否成功打开 */
export function openLoginInNewTab() {
  const win = window.open('/login?switch=1', '_blank')
  return !!win
}

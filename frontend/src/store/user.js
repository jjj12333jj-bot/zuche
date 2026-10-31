import { defineStore } from 'pinia'
import {
  login as loginApi,
  register as registerApi,
  getCurrentUser,
  getMenus
} from '@/api/auth'
import { getToken, getUser, saveAuth, saveUser, clearAuth } from '@/utils/authSession'

export const useUserStore = defineStore('user', {
  state: () => ({
    // 登录态存在 sessionStorage：每个标签页独立，互不覆盖
    token: getToken(),
    userInfo: getUser(),
    /** 后端按角色返回的菜单树，刷新页面后由路由守卫重新拉取 */
    menus: [],
    menuLoaded: false
  }),

  getters: {
    isLogin: (state) => !!state.token,
    roleCode: (state) => state.userInfo?.roleCode || '',
    isAdmin: (state) => state.userInfo?.roleCode === 'SYS_ADMIN',
    isStoreAdmin: (state) => state.userInfo?.roleCode === 'STORE_ADMIN',
    isUser: (state) => state.userInfo?.roleCode === 'USER',
    roleLabel: (state) => {
      const map = { SYS_ADMIN: '系统管理员', STORE_ADMIN: '网点管理员', USER: '普通用户' }
      return map[state.userInfo?.roleCode] || '未登录'
    },
    displayName: (state) => state.userInfo?.realName || state.userInfo?.username || '未登录'
  },

  actions: {
    async login(form) {
      const res = await loginApi(form)
      this.applyAuth(res.data)
      await this.loadMenus()
      return res.data
    },

    async register(form) {
      const res = await registerApi(form)
      this.applyAuth(res.data)
      await this.loadMenus()
      return res.data
    },

    applyAuth(data) {
      this.token = data.token
      this.userInfo = data
      saveAuth(data)
    },

    /** 刷新页面后同步最新用户信息（信用分是实时值） */
    async refresh() {
      if (!this.token) return null
      const res = await getCurrentUser()
      this.userInfo = { ...this.userInfo, ...res.data }
      saveUser(this.userInfo)
      return this.userInfo
    },

    async loadMenus() {
      if (!this.token) {
        this.menus = []
        this.menuLoaded = true
        return []
      }
      const res = await getMenus()
      this.menus = res.data || []
      this.menuLoaded = true
      return this.menus
    },

    /** 只退出当前标签页；别的标签页各自持有自己的登录态，不受影响 */
    logout() {
      this.token = ''
      this.userInfo = null
      this.menus = []
      this.menuLoaded = false
      clearAuth()
    }
  }
})

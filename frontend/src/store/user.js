import { defineStore } from 'pinia'
import {
  login as loginApi,
  register as registerApi,
  getCurrentUser,
  getMenus
} from '@/api/auth'

const TOKEN_KEY = 'zuche_token'
const USER_KEY = 'zuche_user'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    userInfo: JSON.parse(localStorage.getItem(USER_KEY) || 'null'),
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
      localStorage.setItem(TOKEN_KEY, data.token)
      localStorage.setItem(USER_KEY, JSON.stringify(data))
    },

    /** 刷新页面后同步最新用户信息（信用分是实时值） */
    async refresh() {
      if (!this.token) return null
      const res = await getCurrentUser()
      this.userInfo = { ...this.userInfo, ...res.data }
      localStorage.setItem(USER_KEY, JSON.stringify(this.userInfo))
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

    logout() {
      this.token = ''
      this.userInfo = null
      this.menus = []
      this.menuLoaded = false
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})

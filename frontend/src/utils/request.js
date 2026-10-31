import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { getToken, clearAuth } from '@/utils/authSession'

/**
 * 统一 axios 实例
 * - 自动带 token（取自当前标签页的 sessionStorage，不会被别的标签页覆盖）
 * - 业务码非 200 统一提示并 reject，调用方只处理成功分支
 * - 401 只清当前标签页登录态并跳登录页
 */
const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

/** 并发请求同时 401 时只提示一次、只跳一次，避免弹一串相同报错 */
let authExpiring = false

function handleAuthExpired(message) {
  clearAuth()
  // 内存里的 pinia 也要一起清，否则界面还挂着上一个账号的身份
  import('@/store/user')
    .then(({ useUserStore }) => useUserStore().logout())
    .catch(() => {})

  if (authExpiring) return
  authExpiring = true
  setTimeout(() => {
    authExpiring = false
  }, 1500)

  ElMessage.error(message || '登录已过期，请重新登录')
  const current = router.currentRoute.value
  if (current.path !== '/login') {
    router.push({ path: '/login', query: { redirect: current.fullPath } })
  }
}

service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message

    if (status === 401) {
      handleAuthExpired(message)
    } else if (status === 403) {
      ElMessage.error(message || '没有访问权限')
    } else if (message) {
      ElMessage.error(message)
    } else {
      ElMessage.error('网络异常，请检查后端服务是否已启动')
    }
    return Promise.reject(error)
  }
)

export default service

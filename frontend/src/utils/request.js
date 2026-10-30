import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

/**
 * 统一 axios 实例
 * - 自动带 token
 * - 业务码非 200 统一提示并 reject，调用方只处理成功分支
 * - 401 清理登录态并跳登录页
 */
const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('zuche_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

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
      localStorage.removeItem('zuche_token')
      localStorage.removeItem('zuche_user')
      ElMessage.error(message || '登录已过期，请重新登录')
      if (router.currentRoute.value.path !== '/login') {
        router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
      }
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

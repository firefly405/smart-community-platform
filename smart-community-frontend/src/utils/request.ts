import axios from 'axios'
import type { InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useAuthStore } from '@/store/auth'

interface ResponseData<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

const request = axios.create({
  baseURL: '/',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

function handleUnauthorized(config?: InternalAxiosRequestConfig, message = '未授权') {
  if (/^\/api\/v1\/(auth|setup)\//.test(config?.url || '')) {
    ElMessage.error(message)
    return
  }
  const token = localStorage.getItem('token')
  // An old request must not clear a newer login or redirect a second time.
  if (!token || config?.headers.Authorization !== `Bearer ${token}`) return

  useAuthStore().clearAuth()
  ElMessage.error('登录已过期，请重新登录')
  const current = router.currentRoute.value
  if (current.path !== '/login' && current.path !== '/community/login' && current.path !== '/tenant/login' && current.path !== '/setup') {
    const loginPath = current.path.startsWith('/tenant') ? '/tenant/login' : current.path.startsWith('/community') ? '/community/login' : '/login'
    void router.replace({ path: loginPath, query: { redirect: current.fullPath } })
  }
}

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data as ResponseData<any>
    if (res.code === 200) {
      return res.data
    } else if (res.code === 401) {
      handleUnauthorized(response.config, res.message)
      return Promise.reject(new Error(res.message || '未授权'))
    } else {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  (error) => {
    if (error.response) {
      switch (error.response.status) {
        case 401:
          handleUnauthorized(error.config, error.response.data?.message)
          break
        case 403:
          ElMessage.error(error.response.data?.message || '无权限访问')
          break
        case 404:
          ElMessage.error('请求资源不存在')
          break
        case 500:
          ElMessage.error('服务器错误')
          break
        default:
          ElMessage.error(error.response.data?.message || error.message || '请求失败')
      }
    } else if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
      ElMessage.error(error.message || '请求等待超时，请稍后重试')
    } else {
      ElMessage.error('网络错误，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default request

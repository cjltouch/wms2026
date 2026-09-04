import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { useUserStore } from '@/store/user'
import router from '@/router'

NProgress.configure({ showSpinner: false })

const service: AxiosInstance = axios.create({
  baseURL: '/wms-api',
  timeout: 30000
})

service.interceptors.request.use(
  (config) => {
    NProgress.start()
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    NProgress.done()
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  (response: AxiosResponse) => {
    NProgress.done()
    const res = response.data
    // 文件下载（blob）直接返回，不做业务码判断（错误JSON由调用方识别）
    if (res instanceof Blob) {
      return res
    }
    if (res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      if (res.code === 401) {
        const userStore = useUserStore()
        userStore.resetState()
        router.push('/login')
      }
      return Promise.reject(new Error(res.msg || 'Error'))
    }
    return res
  },
  (error) => {
    NProgress.done()
    const status = error.response?.status
    if (status === 401) {
      const userStore = useUserStore()
      userStore.resetState()
      ElMessageBox.confirm('登录已过期，请重新登录', '提示', {
        confirmButtonText: '重新登录',
        type: 'warning'
      }).then(() => {
        router.push('/login')
      })
    } else if (status === 403) {
      ElMessage.error('没有权限访问')
    } else if (status === 404) {
      ElMessage.error('请求接口不存在')
    } else {
      ElMessage.error(error.response?.data?.msg || error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export function request<T = any>(config: AxiosRequestConfig): Promise<T> {
  return service(config) as unknown as Promise<T>
}

export default service

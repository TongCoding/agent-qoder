import axios from 'axios'
import type {
  AxiosError,
  AxiosInstance,
  AxiosRequestConfig,
  AxiosResponse,
  InternalAxiosRequestConfig,
} from 'axios'
import { ElMessage } from 'element-plus'

import { ApiError, SUCCESS_CODE } from '@/types/api'
import type { ApiResponse, FieldValidationError } from '@/types/api'

/**
 * 扩展 Axios 请求配置：允许单个请求关闭全局错误提示。
 *
 * 使用场景：表单提交失败后希望把错误信息渲染到具体字段上，而不是弹全局 Toast。
 */
declare module 'axios' {
  export interface AxiosRequestConfig {
    /** 为 true 时不弹出全局错误提示，由调用方自行处理 */
    silent?: boolean
  }
}

/** 预留的鉴权 Token 存储键，接入登录后写入即可自动携带 */
const TOKEN_STORAGE_KEY = 'taskhub-token'

/** 请求超时时间（毫秒） */
const DEFAULT_TIMEOUT = 15_000

/**
 * 从后端返回体中提取业务码、错误信息与字段明细。
 *
 * 后端的全局异常处理器保证 4xx / 5xx 响应体依然是统一的 ApiResponse 结构，
 * 因此这里优先使用响应体里的 code，缺失时再按 HTTP 状态码兜底。
 * 参数校验失败时 data 为 FieldValidationError 数组，其余情况 data 为 null。
 */
function extractError(
  payload: unknown,
  httpStatus: number,
): { code: number; message: string; details: FieldValidationError[] } {
  const fallbackCode = httpStatus > 0 ? httpStatus * 100 : -1

  if (payload && typeof payload === 'object' && 'code' in payload) {
    const body = payload as Partial<ApiResponse<unknown>>
    const details = Array.isArray(body.data) ? (body.data as FieldValidationError[]) : []
    return {
      code: body.code ?? fallbackCode,
      message: body.message ?? '请求失败',
      details,
    }
  }
  return { code: fallbackCode, message: '请求失败', details: [] }
}

/**
 * 统一的错误提示。
 *
 * 短时间内的重复提示会被抑制，避免批量请求同时失败时刷屏。
 */
let lastMessageAt = 0
let lastMessageText = ''
function notifyError(message: string): void {
  const now = Date.now()
  if (message === lastMessageText && now - lastMessageAt < 1000) {
    return
  }
  lastMessageAt = now
  lastMessageText = message
  ElMessage.error(message)
}

/**
 * Axios 实例。
 *
 * baseURL 取自环境变量，开发环境为 `/api/v1`，由 Vite 代理转发到 `http://localhost:8080`。
 */
const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: DEFAULT_TIMEOUT,
  headers: { 'Content-Type': 'application/json;charset=utf-8' },
})

// ---------------------------------------------------------------------------
// 请求拦截器
// ---------------------------------------------------------------------------
instance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error: AxiosError) => Promise.reject(error),
)

// ---------------------------------------------------------------------------
// 响应拦截器：校验业务码 + 解包 data
// ---------------------------------------------------------------------------
instance.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<unknown>>) => {
    const body = response.data

    // 兼容非标准响应（例如直接返回文件流）
    if (!body || typeof body !== 'object' || !('code' in body)) {
      return response
    }

    if (body.code !== SUCCESS_CODE) {
      const { details } = extractError(body, response.status)
      const error = new ApiError(body.code, body.message, response.status, details)
      if (!response.config.silent) {
        notifyError(error.message)
      }
      return Promise.reject(error)
    }

    // 解包：把业务数据提升到 response.data，使上层拿到的直接是 T
    ;(response as AxiosResponse<unknown>).data = body.data
    return response
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    let apiError: ApiError

    if (error.response) {
      // 服务端返回了 4xx / 5xx，响应体仍是统一结构
      const { code, message, details } = extractError(error.response.data, error.response.status)
      apiError = new ApiError(code, message, error.response.status, details)
    } else if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
      apiError = new ApiError(-1, '请求超时，请检查网络或后端服务是否已启动')
    } else {
      apiError = new ApiError(-1, '网络异常，无法连接到后端服务')
    }

    if (!error.config?.silent) {
      notifyError(apiError.message)
    }
    return Promise.reject(apiError)
  },
)

/**
 * 类型安全的请求方法集合。
 *
 * 泛型 `T` 表示解包后的业务数据类型，因此调用方无需再手动断言：
 * ```ts
 * const page = await http.get<PageResult<TodoItem>>('/todos', { params })
 * ```
 */
export const http = {
  get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.get<T>(url, config).then((res) => res.data as T)
  },

  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return instance.post<T>(url, data, config).then((res) => res.data as T)
  },

  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return instance.put<T>(url, data, config).then((res) => res.data as T)
  },

  patch<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return instance.patch<T>(url, data, config).then((res) => res.data as T)
  },

  delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.delete<T>(url, config).then((res) => res.data as T)
  },
}

export default instance

/**
 * 与后端 `com.example.taskhub.common` 包对应的通用接口类型。
 */

/** 业务成功码，与后端 ErrorCode.SUCCESS 保持一致 */
export const SUCCESS_CODE = 0

/**
 * 统一响应结构。
 *
 * 后端所有接口（含异常）都返回该结构，Axios 响应拦截器会校验 code 并解包 data。
 */
export interface ApiResponse<T = unknown> {
  /** 业务状态码，0 表示成功 */
  code: number
  /** 提示信息 */
  message: string
  /** 业务数据，失败时为 null */
  data: T | null
  /** 服务端响应时间 */
  timestamp: string
}

/**
 * 分页结果，对应后端 PageResult。
 *
 * 注意 page 为 **1 起始**，与 Element Plus 分页组件语义一致。
 */
export interface PageResult<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
  empty: boolean
}

/** 排序方向 */
export type SortDirection = 'asc' | 'desc'

/** 分页查询基础参数 */
export interface PageQuery {
  page?: number
  size?: number
  sortBy?: string
  sortDir?: SortDirection
}

/** 字段级校验错误明细，对应后端 FieldValidationError */
export interface FieldValidationError {
  field: string
  message: string
  rejectedValue: unknown
}

/**
 * 业务错误对象。
 *
 * 由 Axios 拦截器在非 0 业务码或 HTTP 错误时抛出，调用方可通过 `instanceof ApiError` 判定。
 */
export class ApiError extends Error {
  /** 业务错误码 */
  readonly code: number
  /** HTTP 状态码，网络层错误时为 0 */
  readonly httpStatus: number
  /** 字段级错误明细（仅参数校验失败时存在） */
  readonly details: FieldValidationError[]

  constructor(code: number, message: string, httpStatus = 0, details: FieldValidationError[] = []) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.httpStatus = httpStatus
    this.details = details
  }

  /** 是否为参数校验类错误 */
  get isValidationError(): boolean {
    return this.code === 40001
  }

  /** 按字段名取出第一条错误提示，便于回填到表单项 */
  fieldMessage(field: string): string | undefined {
    return this.details.find((item) => item.field === field)?.message
  }
}

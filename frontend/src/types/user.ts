import type { PageQuery } from './api'

/** 用户角色，与后端 UserRole 枚举一致 */
export type UserRole = 'ADMIN' | 'MANAGER' | 'MEMBER' | 'GUEST'

/** 账号状态，与后端 UserStatus 枚举一致 */
export type UserStatus = 'ACTIVE' | 'DISABLED' | 'LOCKED'

/** 用户信息 */
export interface UserInfo {
  id: number
  username: string
  nickname?: string | null
  email: string
  phone?: string | null
  department?: string | null
  role: UserRole
  status: UserStatus
  remark?: string | null
  /** 名下待办数量，由服务端批量聚合 */
  todoCount: number
  createdAt: string
  updatedAt?: string | null
}

/** 用户下拉选项 */
export interface UserOption {
  id: number
  username: string
  nickname?: string | null
  department?: string | null
}

/** 创建用户的请求体 */
export interface UserCreatePayload {
  username: string
  nickname?: string | null
  email: string
  phone?: string | null
  department?: string | null
  role: UserRole
  status: UserStatus
  remark?: string | null
}

/**
 * 更新用户的请求体。
 *
 * 用户名创建后不可修改，因此这里不包含 username 字段。
 */
export type UserUpdatePayload = Omit<UserCreatePayload, 'username'>

/** 用户列表查询条件 */
export interface UserQuery extends PageQuery {
  keyword?: string
  role?: UserRole | null
  status?: UserStatus | null
  department?: string | null
}

/** 用户统计数据，对应后端 UserStatisticsResponse */
export interface UserStatistics {
  total: number
  statusCounts: Record<UserStatus, number>
  roleCounts: Record<UserRole, number>
}

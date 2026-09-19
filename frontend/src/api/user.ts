import { http } from './request'
import { cleanParams } from '@/utils/params'

import type { PageResult } from '@/types/api'
import type {
  UserCreatePayload,
  UserOption,
  UserQuery,
  UserStatistics,
  UserStatus,
  UserUpdatePayload,
  UserInfo,
} from '@/types/user'

/**
 * 用户管理接口。
 *
 * 对应后端 `UserController`，基础路径 `/api/v1/users`（baseURL 已包含 `/api/v1`）。
 */

/** 分页查询用户列表 */
export function pageUsers(query: UserQuery): Promise<PageResult<UserInfo>> {
  return http.get<PageResult<UserInfo>>('/users', { params: cleanParams(query) })
}

/** 查询用户详情 */
export function getUser(id: number): Promise<UserInfo> {
  return http.get<UserInfo>(`/users/${id}`)
}

/** 查询用户下拉选项（仅 ACTIVE 状态），用于待办的负责人选择器 */
export function listUserOptions(): Promise<UserOption[]> {
  return http.get<UserOption[]>('/users/options')
}

/** 查询用户统计数据（仪表盘） */
export function getUserStatistics(): Promise<UserStatistics> {
  return http.get<UserStatistics>('/users/statistics')
}

/** 创建用户，后端返回 201 */
export function createUser(payload: UserCreatePayload): Promise<UserInfo> {
  // silent：用户名/邮箱重复时由表单把 40901/40902 的错误渲染到对应字段
  return http.post<UserInfo>('/users', payload, { silent: true })
}

/** 全量更新用户，用户名创建后不可修改 */
export function updateUser(id: number, payload: UserUpdatePayload): Promise<UserInfo> {
  return http.put<UserInfo>(`/users/${id}`, payload, { silent: true })
}

/** 变更用户账号状态 */
export function changeUserStatus(id: number, status: UserStatus): Promise<UserInfo> {
  return http.patch<UserInfo>(`/users/${id}/status`, { status })
}

/**
 * 删除用户。
 *
 * 若该用户名下仍存在待办事项，后端返回 code = 40903。
 */
export function deleteUser(id: number): Promise<void> {
  return http.delete<void>(`/users/${id}`)
}

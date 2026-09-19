/**
 * 枚举字典。
 *
 * 后端使用 Java 枚举，前端需要维护一份对应的中文标签与视觉样式映射。
 * 所有下拉选项、el-tag 颜色都从这里取值，避免散落在各个组件中导致文案不一致。
 */

import type { TodoPriority, TodoStatus } from '@/types/todo'
import type { UserRole, UserStatus } from '@/types/user'

/** Element Plus el-tag 支持的类型 */
export type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'

/** 字典项 */
export interface DictOption<T extends string = string> {
  value: T
  label: string
  /** 对应 el-tag 的 type */
  tagType: TagType
  /** 补充说明，用于 tooltip */
  hint?: string
}

/** 待办状态字典 */
export const TODO_STATUS_OPTIONS: DictOption<TodoStatus>[] = [
  { value: 'PENDING', label: '待处理', tagType: 'info', hint: '尚未开始' },
  { value: 'IN_PROGRESS', label: '进行中', tagType: 'primary', hint: '正在处理' },
  { value: 'DONE', label: '已完成', tagType: 'success', hint: '已交付，服务端会写入完成时间' },
  { value: 'CANCELLED', label: '已取消', tagType: 'danger', hint: '不再执行' },
]

/** 待办优先级字典 */
export const TODO_PRIORITY_OPTIONS: DictOption<TodoPriority>[] = [
  { value: 'LOW', label: '低', tagType: 'info' },
  { value: 'MEDIUM', label: '中', tagType: 'primary' },
  { value: 'HIGH', label: '高', tagType: 'warning' },
  { value: 'URGENT', label: '紧急', tagType: 'danger' },
]

/** 用户角色字典 */
export const USER_ROLE_OPTIONS: DictOption<UserRole>[] = [
  { value: 'ADMIN', label: '管理员', tagType: 'danger', hint: '系统最高权限' },
  { value: 'MANAGER', label: '经理', tagType: 'warning', hint: '团队管理权限' },
  { value: 'MEMBER', label: '成员', tagType: 'primary', hint: '普通业务人员' },
  { value: 'GUEST', label: '访客', tagType: 'info', hint: '只读权限' },
]

/** 用户状态字典 */
export const USER_STATUS_OPTIONS: DictOption<UserStatus>[] = [
  { value: 'ACTIVE', label: '启用', tagType: 'success', hint: '账号可正常使用' },
  { value: 'DISABLED', label: '禁用', tagType: 'info', hint: '账号已停用，不可被指派待办' },
  { value: 'LOCKED', label: '锁定', tagType: 'danger', hint: '因安全策略被临时锁定' },
]

/** 每页条数选项，需与后端 `@Max(100)` 约束保持一致 */
export const PAGE_SIZE_OPTIONS = [10, 20, 50, 100]

/** 是否为待办终态（已完成 / 已取消），终态不再参与逾期计算 */
export function isFinishedStatus(status: TodoStatus): boolean {
  return status === 'DONE' || status === 'CANCELLED'
}

/** 按状态值取字典项 */
export function todoStatusDict(status?: TodoStatus | null): DictOption<TodoStatus> | undefined {
  return TODO_STATUS_OPTIONS.find((item) => item.value === status)
}

/** 按优先级值取字典项 */
export function todoPriorityDict(priority?: TodoPriority | null): DictOption<TodoPriority> | undefined {
  return TODO_PRIORITY_OPTIONS.find((item) => item.value === priority)
}

/** 按角色值取字典项 */
export function userRoleDict(role?: UserRole | null): DictOption<UserRole> | undefined {
  return USER_ROLE_OPTIONS.find((item) => item.value === role)
}

/** 按状态值取字典项 */
export function userStatusDict(status?: UserStatus | null): DictOption<UserStatus> | undefined {
  return USER_STATUS_OPTIONS.find((item) => item.value === status)
}

/**
 * 状态流转的下一步选项。
 *
 * 用于表格行内的快捷操作，避免出现「已完成 → 进行中」这类不合理的回退入口。
 */
export function nextStatusOptions(current: TodoStatus): DictOption<TodoStatus>[] {
  const transitions: Record<TodoStatus, TodoStatus[]> = {
    PENDING: ['IN_PROGRESS', 'DONE', 'CANCELLED'],
    IN_PROGRESS: ['DONE', 'PENDING', 'CANCELLED'],
    DONE: ['IN_PROGRESS'],
    CANCELLED: ['PENDING'],
  }
  return (transitions[current] ?? []).map((value) => todoStatusDict(value)!).filter(Boolean)
}

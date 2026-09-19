import type { PageQuery } from './api'

/** 待办状态，与后端 TodoStatus 枚举一致 */
export type TodoStatus = 'PENDING' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED'

/** 待办优先级，与后端 TodoPriority 枚举一致 */
export type TodoPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'

/** 待办负责人精简信息 */
export interface TodoAssignee {
  id: number
  username: string
  nickname?: string | null
}

/**
 * 待办事项。
 *
 * 后端配置了 `default-property-inclusion: non_null`，
 * 值为 null 的字段不会出现在 JSON 中，因此这里统一声明为可选属性。
 */
export interface TodoItem {
  id: number
  title: string
  description?: string | null
  status: TodoStatus
  priority: TodoPriority
  dueDate?: string | null
  completedAt?: string | null
  /** 服务端计算的逾期标记 */
  overdue: boolean
  assignee?: TodoAssignee | null
  createdAt: string
  updatedAt?: string | null
}

/** 创建待办的请求体 */
export interface TodoCreatePayload {
  title: string
  description?: string | null
  status?: TodoStatus | null
  priority?: TodoPriority | null
  dueDate?: string | null
  assigneeId?: number | null
}

/** 更新待办的请求体（全量更新语义） */
export interface TodoUpdatePayload {
  title: string
  description?: string | null
  status: TodoStatus
  priority: TodoPriority
  dueDate?: string | null
  assigneeId?: number | null
}

/** 待办列表查询条件 */
export interface TodoQuery extends PageQuery {
  keyword?: string
  status?: TodoStatus | null
  priority?: TodoPriority | null
  assigneeId?: number | null
  unassigned?: boolean | null
  overdue?: boolean | null
  dueDateFrom?: string | null
  dueDateTo?: string | null
}

/** 待办统计数据，对应后端 TodoStatisticsResponse */
export interface TodoStatistics {
  total: number
  statusCounts: Record<TodoStatus, number>
  priorityCounts: Record<TodoPriority, number>
  overdueCount: number
  dueTodayCount: number
  unassignedCount: number
  /** 完成率，0-100 之间保留两位小数 */
  completionRate: number
  recentTodos: TodoItem[]
}

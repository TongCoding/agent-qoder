import { http } from './request'
import { cleanParams, joinIds } from '@/utils/params'

import type { PageResult } from '@/types/api'
import type {
  TodoCreatePayload,
  TodoItem,
  TodoQuery,
  TodoStatistics,
  TodoStatus,
  TodoUpdatePayload,
} from '@/types/todo'

/**
 * 待办事项接口。
 *
 * 对应后端 `TodoController`，基础路径 `/api/v1/todos`（baseURL 已包含 `/api/v1`）。
 */

/** 分页查询待办列表 */
export function pageTodos(query: TodoQuery): Promise<PageResult<TodoItem>> {
  return http.get<PageResult<TodoItem>>('/todos', { params: cleanParams(query) })
}

/** 查询待办详情 */
export function getTodo(id: number): Promise<TodoItem> {
  return http.get<TodoItem>(`/todos/${id}`)
}

/** 查询待办统计数据（仪表盘） */
export function getTodoStatistics(): Promise<TodoStatistics> {
  return http.get<TodoStatistics>('/todos/statistics')
}

/** 创建待办，后端返回 201 */
export function createTodo(payload: TodoCreatePayload): Promise<TodoItem> {
  // silent：校验失败时由表单组件把错误渲染到对应字段，不弹全局 Toast
  return http.post<TodoItem>('/todos', payload, { silent: true })
}

/** 全量更新待办 */
export function updateTodo(id: number, payload: TodoUpdatePayload): Promise<TodoItem> {
  return http.put<TodoItem>(`/todos/${id}`, payload, { silent: true })
}

/** 仅变更待办状态，流转为 DONE 时服务端会自动写入 completedAt */
export function changeTodoStatus(id: number, status: TodoStatus): Promise<TodoItem> {
  return http.patch<TodoItem>(`/todos/${id}/status`, { status })
}

/** 删除单条待办 */
export function deleteTodo(id: number): Promise<void> {
  return http.delete<void>(`/todos/${id}`)
}

/**
 * 批量删除待办。
 *
 * 后端通过 `?ids=1,2,3` 查询参数接收 ID 列表（DELETE 携带 body 在部分代理下支持不佳）。
 * 若存在无效 ID 则整体失败，不做部分删除。
 */
export function deleteTodos(ids: number[]): Promise<number> {
  return http.delete<number>('/todos', { params: { ids: joinIds(ids) } })
}

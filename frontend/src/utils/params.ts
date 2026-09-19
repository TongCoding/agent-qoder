/**
 * 请求参数清洗工具。
 *
 * Element Plus 的表单控件在清空时会把值置为 `''` 或 `null`，
 * 若直接作为 query string 发送，后端会收到 `status=`、`overdue=null` 之类的脏参数，
 * 进而在枚举 / 布尔绑定时抛出 400。因此统一在发请求前剔除空值。
 */

/**
 * 移除值为 undefined / null / 空字符串 / 空数组的参数。
 *
 * 注意：`false` 与 `0` 是有效业务值（例如 `overdue=false`、`page=1`），必须保留。
 *
 * @param params 原始查询对象
 * @returns 仅保留有效字段的新对象（不修改入参）
 */
export function cleanParams<T extends object>(params: T): T {
  const result: Record<string, unknown> = {}

  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return
    }
    if (Array.isArray(value)) {
      if (value.length > 0) {
        result[key] = value
      }
      return
    }
    result[key] = value
  })

  return result as T
}

/**
 * 序列化数组为逗号分隔字符串。
 *
 * 用于后端 `@RequestParam List<Long> ids` 这类接收 `?ids=1,2,3` 的接口。
 */
export function joinIds(ids: Array<number | string>): string {
  return ids.join(',')
}

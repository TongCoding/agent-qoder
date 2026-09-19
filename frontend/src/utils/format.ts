/**
 * 日期时间格式化工具。
 *
 * 后端 Jackson 配置了 `write-dates-as-timestamps: false` + `time-zone: GMT+8`，
 * 输出的 LocalDateTime 形如 `2026-09-13T10:30:00`（**不带时区后缀**）。
 * 这类字符串交给 `new Date()` 解析时，不同浏览器的时区处理并不一致，
 * 容易出现「显示时间比录入时间差 8 小时」的问题。
 *
 * 因此这里采用**纯字符串解析**：直接截取 ISO-8601 的字面量部分，
 * 所见即所得，不做任何时区换算。
 */

/** 占位符，表示无值 */
const EMPTY_PLACEHOLDER = '-'

/** 补零到两位 */
function pad(value: string | number, length = 2): string {
  return String(value).padStart(length, '0')
}

/**
 * 解析 ISO-8601 日期时间字符串，拆出日期与时间部分。
 *
 * 支持以下输入：
 * - `2026-09-13T10:30:00`
 * - `2026-09-13T10:30:00.123`
 * - `2026-09-13T10:30:00+08:00`（时区后缀会被忽略）
 * - `2026-09-13`
 */
function parseIso(value: string): { date: string; time: string } | null {
  const matched = /^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2})(?::(\d{2}))?)?/.exec(value)
  if (!matched) {
    return null
  }
  const [, year, month, day, hour, minute, second] = matched
  const date = `${year}-${month}-${day}`
  const time = hour ? `${hour}:${minute}:${pad(second ?? 0)}` : ''
  return { date, time }
}

/**
 * 格式化为 `yyyy-MM-dd HH:mm:ss`。
 *
 * @param value ISO-8601 字符串，为空时返回占位符
 * @param showSeconds 是否展示秒，默认展示
 */
export function formatDateTime(value?: string | null, showSeconds = true): string {
  if (!value) {
    return EMPTY_PLACEHOLDER
  }
  const parsed = parseIso(value)
  if (!parsed || !parsed.time) {
    return value
  }
  return showSeconds ? `${parsed.date} ${parsed.time}` : parsed.time.slice(0, 5)
}

/** 格式化为 `yyyy-MM-dd HH:mm` */
export function formatDateTimeShort(value?: string | null): string {
  return formatDateTime(value, false)
}

/** 格式化为 `yyyy-MM-dd`，用于 LocalDate 字段 */
export function formatDate(value?: string | null): string {
  if (!value) {
    return EMPTY_PLACEHOLDER
  }
  const parsed = parseIso(value)
  return parsed ? parsed.date : value
}

/** 取今天的 `yyyy-MM-dd`，用于默认截止日期 */
export function today(): string {
  const now = new Date()
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}

/**
 * 计算两个日期之间相差的天数（自然日）。
 *
 * @returns target 相对 base 的天数差，正数表示未来，负数表示过去
 */
export function diffDays(target: string | Date, base: string | Date = new Date()): number {
  const toMidnight = (input: string | Date): number => {
    const date = typeof input === 'string' ? new Date(`${input.slice(0, 10)}T00:00:00`) : input
    return new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
  }
  return Math.round((toMidnight(target) - toMidnight(base)) / 86_400_000)
}

/**
 * 生成截止日期的可读提示，例如「逾期 3 天」「今天到期」「剩余 5 天」。
 *
 * @param dueDate 截止日期（yyyy-MM-dd）
 * @param finished 待办是否已处于终态，终态不再提示剩余时间
 */
export function describeDueDate(dueDate?: string | null, finished = false): string {
  if (!dueDate) {
    return '无截止日期'
  }
  if (finished) {
    return `${formatDate(dueDate)} 截止`
  }
  const days = diffDays(dueDate)
  if (days < 0) {
    return `逾期 ${Math.abs(days)} 天`
  }
  if (days === 0) {
    return '今天到期'
  }
  if (days === 1) {
    return '明天到期'
  }
  return `剩余 ${days} 天`
}

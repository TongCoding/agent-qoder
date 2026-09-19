import { onBeforeUnmount, onMounted, readonly, ref } from 'vue'
import type { DeepReadonly, Ref } from 'vue'

/**
 * 基于 `window.matchMedia` 的响应式断点判断。
 *
 * 相比监听 `resize` 事件手动比较 `window.innerWidth`，
 * matchMedia 由浏览器负责匹配计算，不会在每次像素变化时都触发回调。
 *
 * @param query CSS 媒体查询，例如 `'(max-width: 768px)'`
 * @returns 只读的布尔响应式引用
 */
export function useMediaQuery(query: string): DeepReadonly<Ref<boolean>> {
  const matched = ref(false)
  let mediaQuery: MediaQueryList | null = null

  const handleChange = (event: MediaQueryListEvent | MediaQueryList): void => {
    matched.value = event.matches
  }

  onMounted(() => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
      return
    }
    mediaQuery = window.matchMedia(query)
    matched.value = mediaQuery.matches
    // Safari 14 以前只支持 addListener，这里做兼容降级
    if (typeof mediaQuery.addEventListener === 'function') {
      mediaQuery.addEventListener('change', handleChange)
    } else {
      mediaQuery.addListener(handleChange)
    }
  })

  onBeforeUnmount(() => {
    if (!mediaQuery) {
      return
    }
    if (typeof mediaQuery.removeEventListener === 'function') {
      mediaQuery.removeEventListener('change', handleChange)
    } else {
      mediaQuery.removeListener(handleChange)
    }
    mediaQuery = null
  })

  return readonly(matched)
}

/** 移动端断点：与 `styles/index.scss` 中的媒体查询保持一致 */
export const MOBILE_QUERY = '(max-width: 768px)'

/** 窄屏断点：统计卡片等栅格布局可据此调整列数 */
export const NARROW_QUERY = '(max-width: 1200px)'

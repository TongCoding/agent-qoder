import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

/** 侧边栏折叠状态的本地存储键 */
const COLLAPSED_STORAGE_KEY = 'taskhub-sidebar-collapsed'

/** 从本地存储读取折叠状态，异常时回退为展开 */
function readCollapsed(): boolean {
  try {
    return localStorage.getItem(COLLAPSED_STORAGE_KEY) === 'true'
  } catch {
    // 隐私模式下 localStorage 可能不可用
    return false
  }
}

/**
 * 全局应用状态。
 *
 * 这里只存放**跨页面共享的 UI 状态**（如侧边栏折叠），
 * 业务数据一律放在各自页面内，避免 store 变成无所不包的上帝对象。
 */
export const useAppStore = defineStore('app', () => {
  /** 侧边栏是否折叠 */
  const sidebarCollapsed = ref<boolean>(readCollapsed())

  /**
   * 侧边栏宽度。
   *
   * 与 DefaultLayout 的样式变量保持一致：展开 220px，折叠 64px。
   */
  const sidebarWidth = computed(() => (sidebarCollapsed.value ? '64px' : '220px'))

  /** 切换折叠状态并持久化 */
  function toggleSidebar(): void {
    sidebarCollapsed.value = !sidebarCollapsed.value
    try {
      localStorage.setItem(COLLAPSED_STORAGE_KEY, String(sidebarCollapsed.value))
    } catch {
      // 写入失败不影响本次会话内的切换效果
    }
  }

  return { sidebarCollapsed, sidebarWidth, toggleSidebar }
})

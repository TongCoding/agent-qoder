import { onMounted, ref } from 'vue'
import type { Ref } from 'vue'

import { listUserOptions } from '@/api/user'
import type { UserOption } from '@/types/user'

/**
 * 负责人下拉选项。
 *
 * 待办列表的筛选栏与编辑弹窗都需要「启用状态用户」列表，
 * 抽成 composable 后组件内只需一行即可获得数据与加载态。
 *
 * 注意：该接口返回的是全量精简数据（不分页），适用于演示规模的团队。
 * 生产环境若用户量较大，应改为 el-select 的 remote 远程搜索模式。
 */
export function useUserOptions(immediate = true) {
  const options: Ref<UserOption[]> = ref([])
  const loading = ref(false)
  /** 请求失败标记，用于在界面上给出重试入口 */
  const failed = ref(false)

  async function loadOptions(): Promise<void> {
    loading.value = true
    failed.value = false
    try {
      options.value = await listUserOptions()
    } catch {
      // 错误提示已由 Axios 拦截器统一处理，这里只记录失败状态
      options.value = []
      failed.value = true
    } finally {
      loading.value = false
    }
  }

  /** 按用户 ID 取展示名，优先昵称 */
  function labelOf(id?: number | null): string {
    if (id == null) {
      return '未分配'
    }
    const matched = options.value.find((item) => item.id === id)
    return matched?.nickname || matched?.username || `用户 #${id}`
  }

  if (immediate) {
    onMounted(() => void loadOptions())
  }

  return { options, loading, failed, loadOptions, labelOf }
}

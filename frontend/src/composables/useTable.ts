import { computed, onMounted, reactive, ref } from 'vue'
import type { ComputedRef, Ref } from 'vue'

import type { PageQuery, PageResult, SortDirection } from '@/types/api'

/** useTable 的配置项 */
export interface UseTableOptions<T, Q extends PageQuery> {
  /** 分页数据获取函数，通常是 `src/api` 下的接口方法 */
  fetcher: (query: Q) => Promise<PageResult<T>>
  /** 查询条件初始值，重置时会回到该状态 */
  defaultQuery: Q
  /** 是否在组件挂载时立即加载，默认 true */
  immediate?: boolean
  /** 加载失败的回调，默认已由 Axios 拦截器弹出提示，此处仅用于额外处理 */
  onError?: (error: unknown) => void
}

/**
 * el-table `sort-change` 事件的负载。
 *
 * order 声明为宽松的 string：Element Plus 内部对该字段的类型标注并不精确，
 * 这里只按值做匹配，避免调用方被迫写类型断言。
 */
export interface SortChangeEvent {
  prop?: string | null
  order?: string | null
}

/** useTable 的返回值 */
export interface UseTableReturn<T, Q extends PageQuery> {
  /** 加载状态，用于 v-loading */
  loading: Ref<boolean>
  /** 当前页数据 */
  list: Ref<T[]>
  /** 总条数 */
  total: Ref<number>
  /** 查询条件（响应式，可直接 v-model 绑定） */
  query: Q
  /** 总页数 */
  totalPages: ComputedRef<number>
  /** 按当前条件重新加载（保持页码） */
  loadData: () => Promise<void>
  /** 条件变更后查询：重置到第 1 页再加载 */
  search: () => Promise<void>
  /** 清空所有筛选条件并重新查询 */
  reset: () => Promise<void>
  /** el-pagination `current-change` 处理 */
  handleCurrentChange: (page: number) => void
  /** el-pagination `size-change` 处理 */
  handleSizeChange: (size: number) => void
  /** el-table `sort-change` 处理 */
  handleSortChange: (event: SortChangeEvent) => void
}

/** el-table 排序方向 → 后端 sortDir */
function toSortDirection(order?: string | null): SortDirection | undefined {
  if (order === 'ascending') return 'asc'
  if (order === 'descending') return 'desc'
  return undefined
}

/**
 * 表格分页通用逻辑。
 *
 * 把「加载态 / 列表 / 总数 / 分页 / 排序 / 查询重置」这套在每个列表页都会重复的
 * 样板代码收敛到一处，业务组件只需提供 fetcher 与 defaultQuery：
 *
 * ```ts
 * const { loading, list, total, query, search, reset } = useTable({
 *   fetcher: pageTodos,
 *   defaultQuery: { page: 1, size: 10, keyword: '', status: null },
 * })
 * ```
 *
 * 已处理的问题：
 * - **请求竞态**：连续触发查询时只采用最后一次响应，避免慢请求覆盖新数据；
 * - **删除最后一页**：由调用方在删除后判断 `list` 是否为空并调用 `search()`；
 * - **排序字段**：直接把 el-table 的列 prop 作为 sortBy 传给后端，
 *   后端有白名单校验，非法字段会安全回退为默认排序。
 */
export function useTable<T, Q extends PageQuery>(
  options: UseTableOptions<T, Q>,
): UseTableReturn<T, Q> {
  const { fetcher, defaultQuery, immediate = true, onError } = options

  const loading = ref(false)
  const list = ref([]) as Ref<T[]>
  const total = ref(0)
  const query = reactive({ ...defaultQuery }) as Q

  /** 请求序号，用于丢弃过期响应 */
  let requestSeq = 0

  const totalPages = computed(() => {
    const size = query.size ?? 10
    return size > 0 ? Math.ceil(total.value / size) : 0
  })

  async function loadData(): Promise<void> {
    const seq = ++requestSeq
    loading.value = true
    try {
      const result = await fetcher(query)
      // 期间又发起了新请求，本次响应已过期，直接丢弃
      if (seq !== requestSeq) {
        return
      }
      list.value = result.content ?? []
      total.value = result.totalElements ?? 0
    } catch (error) {
      if (seq === requestSeq) {
        // 失败时保留原数据会造成「条件已变但列表未变」的错觉，因此清空
        list.value = []
        total.value = 0
        onError?.(error)
      }
    } finally {
      if (seq === requestSeq) {
        loading.value = false
      }
    }
  }

  async function search(): Promise<void> {
    query.page = 1
    await loadData()
  }

  async function reset(): Promise<void> {
    Object.assign(query, { ...defaultQuery })
    await loadData()
  }

  function handleCurrentChange(page: number): void {
    query.page = page
    void loadData()
  }

  function handleSizeChange(size: number): void {
    query.size = size
    // 每页条数变化后总页数会变，回到第 1 页避免出现空白页
    query.page = 1
    void loadData()
  }

  function handleSortChange(event: SortChangeEvent): void {
    const direction = toSortDirection(event.order)
    if (event.prop && direction) {
      query.sortBy = event.prop
      query.sortDir = direction
    } else {
      // 取消排序：清空 sortBy，交由后端使用默认排序字段
      query.sortBy = undefined
      query.sortDir = undefined
    }
    void search()
  }

  if (immediate) {
    onMounted(() => void loadData())
  }

  return {
    loading,
    list,
    total,
    query,
    totalPages,
    loadData,
    search,
    reset,
    handleCurrentChange,
    handleSizeChange,
    handleSortChange,
  }
}

<script setup lang="ts">
/**
 * 待办事项列表页。
 *
 * 覆盖了一个典型后台管理页面的完整能力：
 * 组合筛选、服务端排序、分页、行内状态流转、单条/批量删除、详情抽屉与表单弹窗。
 * 分页与加载态的样板逻辑由 `useTable` 统一收敛。
 */
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import TodoFormDialog from '@/components/todo/TodoFormDialog.vue'
import { changeTodoStatus, deleteTodo, deleteTodos, pageTodos } from '@/api/todo'
import { MOBILE_QUERY, useMediaQuery } from '@/composables/useMediaQuery'
import { useTable } from '@/composables/useTable'
import { useUserOptions } from '@/composables/useUserOptions'
import {
  PAGE_SIZE_OPTIONS,
  TODO_PRIORITY_OPTIONS,
  TODO_STATUS_OPTIONS,
  isFinishedStatus,
  nextStatusOptions,
  todoPriorityDict,
  todoStatusDict,
} from '@/utils/dict'
import { describeDueDate, formatDateTime } from '@/utils/format'
import { ACTION_MENU_POPPER_OPTIONS } from '@/utils/ui'

import type { TodoItem, TodoQuery, TodoStatus } from '@/types/todo'

/** 查询条件的初始值，重置时回到该状态 */
const DEFAULT_QUERY: TodoQuery = {
  page: 1,
  size: 10,
  keyword: '',
  status: null,
  priority: null,
  assigneeId: null,
  unassigned: null,
  overdue: null,
  dueDateFrom: null,
  dueDateTo: null,
}

const {
  loading,
  list,
  total,
  query,
  search,
  reset,
  loadData,
  handleCurrentChange,
  handleSizeChange,
  handleSortChange,
} = useTable<TodoItem, TodoQuery>({
  fetcher: pageTodos,
  defaultQuery: DEFAULT_QUERY,
})

const { options: userOptions } = useUserOptions()

/** 日期区间选择器的绑定值，提交查询时才写入 query */
const dateRange = ref<[string, string] | null>(null)

/** 「只看未分配」开关，与负责人筛选互斥 */
const unassignedOnly = ref(false)

/** 表格多选结果 */
const selection = ref<TodoItem[]>([])

/** 表单弹窗显隐与当前编辑对象 */
const dialogVisible = ref(false)
const editingTodo = ref<TodoItem | null>(null)

/** 详情抽屉显隐与当前查看对象 */
const detailVisible = ref(false)
const detailTodo = ref<TodoItem | null>(null)

/** 行内状态流转的进行中 ID，避免重复点击 */
const pendingStatusId = ref<number | null>(null)

/**
 * 窄屏判定。
 *
 * 操作列在窄屏下切换为紧凑布局：只保留「编辑」「删除」，
 * 状态流转入口移至详情抽屉，把宝贵的横向空间让给数据列。
 */
const isMobile = useMediaQuery(MOBILE_QUERY)

/**
 * 操作列宽度。
 *
 * 桌面端 176px：减去 `.cell` 左右各 12px 内边距后可用 152px，
 * 需容纳「流转」按钮 loading 态额外插入的图标（约 +17px），
 * 否则流转请求进行中会溢出换行。
 * 窄屏 112px：收起流转入口后仅剩编辑与删除，可用 88px 足够。
 */
const actionColumnWidth = computed(() => (isMobile.value ? 112 : 176))

/** 详情抽屉中当前待办可流转的目标状态；窄屏下这里是唯一的流转入口 */
const detailStatusOptions = computed(() =>
  detailTodo.value ? nextStatusOptions(detailTodo.value.status) : [],
)

/**
 * 详情抽屉宽度。
 *
 * 原先硬编码 480px，在 375px 视口下抽屉会左侧溢出约 105px，
 * 导致「状态流转」区块的标题与首个按钮被裁到屏幕外——
 * 而窄屏下这里正是唯一的流转入口，因此必须随断点占满全屏。
 * 与 `UserFormDialog.vue` 的响应式宽度策略保持一致。
 */
const detailDrawerSize = computed(() => (isMobile.value ? '100%' : '480px'))

/**
 * 应用筛选条件并查询。
 *
 * 筛选项先写在各自的控件上，点击「查询」时才同步到 query，
 * 这样用户连续调整多个条件不会触发多次请求。
 */
function handleSearch(): void {
  query.unassigned = unassignedOnly.value ? true : null
  if (unassignedOnly.value) {
    // 未分配与指定负责人互斥，避免构造出必然为空的结果集
    query.assigneeId = null
  }
  query.dueDateFrom = dateRange.value?.[0] ?? null
  query.dueDateTo = dateRange.value?.[1] ?? null
  void search()
}

/** 重置所有筛选条件 */
async function handleReset(): Promise<void> {
  dateRange.value = null
  unassignedOnly.value = false
  await reset()
}

/** 打开新建弹窗 */
function openCreateDialog(): void {
  editingTodo.value = null
  dialogVisible.value = true
}

/** 打开编辑弹窗 */
function openEditDialog(row: TodoItem): void {
  editingTodo.value = row
  dialogVisible.value = true
}

/** 打开详情抽屉 */
function openDetail(row: TodoItem): void {
  detailTodo.value = row
  detailVisible.value = true
}

/**
 * 数据变更后的刷新策略。
 *
 * 如果删空了当前页且不是第一页，则回退一页，避免停留在空白页上。
 */
async function reloadAfterMutation(): Promise<void> {
  if (list.value.length === 0 && (query.page ?? 1) > 1) {
    query.page = (query.page ?? 1) - 1
  }
  await loadData()
}

/** 变更待办状态 */
async function handleChangeStatus(row: TodoItem, status: TodoStatus): Promise<void> {
  pendingStatusId.value = row.id
  try {
    await changeTodoStatus(row.id, status)
    const label = todoStatusDict(status)?.label ?? status
    ElMessage.success(`已流转为「${label}」`)
    await loadData()
  } catch {
    // 错误提示已由 Axios 拦截器统一处理
  } finally {
    pendingStatusId.value = null
  }
}

/**
 * 从详情抽屉变更状态。
 *
 * 抽屉中的 `detailTodo` 是列表项的引用，流转后 `loadData` 会重建列表使其失效，
 * 因此先收起抽屉再执行流转，避免抽屉停留在陈旧数据上（与 `handleDialogSuccess` 同策略）。
 */
async function handleChangeStatusFromDetail(status: TodoStatus): Promise<void> {
  const target = detailTodo.value
  if (!target) {
    return
  }
  detailVisible.value = false
  await handleChangeStatus(target, status)
}

/** 删除单条待办 */
async function handleDelete(row: TodoItem): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除待办「${row.title}」吗？该操作不可恢复。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
    })
  } catch {
    // 用户取消
    return
  }

  try {
    await deleteTodo(row.id)
    ElMessage.success('删除成功')
    await reloadAfterMutation()
  } catch {
    // 错误提示已由 Axios 拦截器统一处理
  }
}

/** 批量删除 */
async function handleBatchDelete(): Promise<void> {
  if (selection.value.length === 0) {
    ElMessage.info('请先勾选要删除的待办')
    return
  }

  const ids = selection.value.map((item) => item.id)
  try {
    await ElMessageBox.confirm(
      `确认删除选中的 ${ids.length} 条待办吗？若存在无效 ID，本次操作会整体失败。`,
      '批量删除确认',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  try {
    const deleted = await deleteTodos(ids)
    ElMessage.success(`已删除 ${deleted} 条待办`)
    selection.value = []
    await reloadAfterMutation()
  } catch {
    // 错误提示已由 Axios 拦截器统一处理
  }
}

/** 保存成功后刷新，并同步详情抽屉中的数据 */
function handleDialogSuccess(): void {
  void loadData()
  if (detailVisible.value && detailTodo.value) {
    // 抽屉里的对象是列表项的引用，重新加载后已失效，直接关闭以避免展示陈旧数据
    detailVisible.value = false
  }
}

/** 从详情抽屉进入编辑：先收起抽屉再弹窗，避免两层浮层叠加 */
function editFromDetail(): void {
  if (!detailTodo.value) {
    return
  }
  const target = detailTodo.value
  detailVisible.value = false
  openEditDialog(target)
}
</script>

<template>
  <div class="todo-page">
    <!-- 筛选区 -->
    <div class="app-card todo-page__filter">
      <el-form :model="query" label-width="72px" @submit.prevent="handleSearch">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="关键字">
              <el-input
                v-model="query.keyword"
                placeholder="标题 / 描述"
                clearable
                @keyup.enter="handleSearch"
                @clear="handleSearch"
              />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="状态">
              <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 100%">
                <el-option
                  v-for="item in TODO_STATUS_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="优先级">
              <el-select
                v-model="query.priority"
                placeholder="全部优先级"
                clearable
                style="width: 100%"
              >
                <el-option
                  v-for="item in TODO_PRIORITY_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="负责人">
              <el-select
                v-model="query.assigneeId"
                placeholder="全部负责人"
                :disabled="unassignedOnly"
                filterable
                clearable
                style="width: 100%"
              >
                <el-option
                  v-for="user in userOptions"
                  :key="user.id"
                  :label="user.nickname || user.username"
                  :value="user.id"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="截止日期">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                unlink-panels
                style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="逾期">
              <el-select v-model="query.overdue" placeholder="不限" clearable style="width: 100%">
                <el-option label="仅看逾期" :value="true" />
                <el-option label="未逾期" :value="false" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label=" ">
              <el-checkbox v-model="unassignedOnly">只看未分配负责人</el-checkbox>
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="24" :md="24" :lg="6">
            <el-form-item label-width="0">
              <el-space :size="8">
                <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
                <el-button icon="RefreshLeft" @click="handleReset">重置</el-button>
              </el-space>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <!-- 列表区 -->
    <div class="app-card todo-page__table">
      <div class="app-toolbar">
        <div>
          <span class="app-section-title todo-page__title">待办列表</span>
          <span class="app-text-secondary">共 {{ total }} 条</span>
        </div>

        <el-space :size="8">
          <el-button
            type="danger"
            plain
            icon="Delete"
            :disabled="selection.length === 0"
            @click="handleBatchDelete"
          >
            批量删除<span v-if="selection.length > 0">（{{ selection.length }}）</span>
          </el-button>
          <el-button icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
          <el-button type="primary" icon="Plus" @click="openCreateDialog">新建待办</el-button>
        </el-space>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        row-key="id"
        border
        stripe
        highlight-current-row
        empty-text="暂无符合条件的待办"
        @selection-change="(rows: TodoItem[]) => (selection = rows)"
        @sort-change="handleSortChange"
      >
        <el-table-column type="selection" width="48" reserve-selection />

        <el-table-column label="标题" prop="title" min-width="220" sortable="custom" show-overflow-tooltip>
          <template #default="{ row }: { row: TodoItem }">
            <el-link type="primary" underline="never" @click="openDetail(row)">
              {{ row.title }}
            </el-link>
            <el-tag v-if="row.overdue" type="danger" size="small" effect="dark" class="todo-page__overdue-tag">
              逾期
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="状态" prop="status" width="110" sortable="custom" align="center">
          <template #default="{ row }: { row: TodoItem }">
            <el-tooltip :content="todoStatusDict(row.status)?.hint" placement="top">
              <el-tag :type="todoStatusDict(row.status)?.tagType ?? 'info'" effect="light">
                {{ todoStatusDict(row.status)?.label ?? row.status }}
              </el-tag>
            </el-tooltip>
          </template>
        </el-table-column>

        <el-table-column label="优先级" prop="priority" width="100" sortable="custom" align="center">
          <template #default="{ row }: { row: TodoItem }">
            <el-tag :type="todoPriorityDict(row.priority)?.tagType ?? 'info'" effect="plain">
              {{ todoPriorityDict(row.priority)?.label ?? row.priority }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="负责人" prop="assignee" width="140" show-overflow-tooltip>
          <template #default="{ row }: { row: TodoItem }">
            <span v-if="row.assignee" class="todo-page__assignee">
              <el-avatar :size="22" class="todo-page__avatar">
                {{ (row.assignee.nickname || row.assignee.username).slice(0, 1) }}
              </el-avatar>
              {{ row.assignee.nickname || row.assignee.username }}
            </span>
            <span v-else class="app-text-secondary">未分配</span>
          </template>
        </el-table-column>

        <el-table-column label="截止日期" prop="dueDate" width="160" sortable="custom">
          <template #default="{ row }: { row: TodoItem }">
            <div>{{ row.dueDate ?? '-' }}</div>
            <div
              class="app-text-secondary"
              :class="{ 'todo-page__due-danger': row.overdue }"
            >
              {{ describeDueDate(row.dueDate, isFinishedStatus(row.status)) }}
            </div>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" prop="createdAt" width="170" sortable="custom">
          <template #default="{ row }: { row: TodoItem }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" :width="actionColumnWidth" fixed="right" align="center">
          <template #default="{ row }: { row: TodoItem }">
            <!--
              用 flex + gap 统一控制间距：
              原先「流转」被 el-dropdown 包裹，`.el-button + .el-button` 的默认 margin
              对其后的「编辑」不生效，导致间距忽有忽无。
            -->
            <div class="app-table-actions">
              <!-- 状态流转：窄屏下整体收起，入口移至详情抽屉 -->
              <el-dropdown
                v-if="!isMobile"
                trigger="click"
                :popper-options="ACTION_MENU_POPPER_OPTIONS"
                @command="(status: TodoStatus) => handleChangeStatus(row, status)"
              >
                <el-button
                  text
                  type="primary"
                  size="small"
                  :loading="pendingStatusId === row.id"
                  title="变更状态"
                  :aria-label="`变更「${row.title}」的状态`"
                >
                  流转<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-for="item in nextStatusOptions(row.status)"
                      :key="item.value"
                      :command="item.value"
                      :title="item.hint"
                      :aria-label="`流转为${item.label}${item.hint ? '，' + item.hint : ''}`"
                    >
                      <el-tag :type="item.tagType" size="small" effect="plain">{{ item.label }}</el-tag>
                      <!-- tag 颜色对视障用户无意义，语义改由 hint 文本承载 -->
                      <span v-if="item.hint" class="app-action-hint">{{ item.hint }}</span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>

              <el-button
                text
                type="primary"
                size="small"
                title="编辑"
                :aria-label="`编辑「${row.title}」`"
                @click="openEditDialog(row)"
              >
                编辑
              </el-button>
              <el-button
                text
                type="danger"
                size="small"
                title="删除"
                :aria-label="`删除「${row.title}」，该操作不可恢复`"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="app-pagination">
        <el-pagination
          :current-page="query.page"
          :page-size="query.size"
          :total="total"
          :page-sizes="PAGE_SIZE_OPTIONS"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="待办详情" :size="detailDrawerSize">
      <el-descriptions v-if="detailTodo" :column="1" border>
        <el-descriptions-item label="标题">{{ detailTodo.title }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="todoStatusDict(detailTodo.status)?.tagType ?? 'info'" effect="light">
            {{ todoStatusDict(detailTodo.status)?.label ?? detailTodo.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="优先级">
          <el-tag :type="todoPriorityDict(detailTodo.priority)?.tagType ?? 'info'" effect="plain">
            {{ todoPriorityDict(detailTodo.priority)?.label ?? detailTodo.priority }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="负责人">
          {{ detailTodo.assignee?.nickname || detailTodo.assignee?.username || '未分配' }}
        </el-descriptions-item>
        <el-descriptions-item label="截止日期">
          {{ detailTodo.dueDate ?? '-' }}
          <span class="app-text-secondary">
            （{{ describeDueDate(detailTodo.dueDate, isFinishedStatus(detailTodo.status)) }}）
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="完成时间">
          {{ formatDateTime(detailTodo.completedAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ formatDateTime(detailTodo.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          {{ formatDateTime(detailTodo.updatedAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="描述">
          <div class="todo-page__description">{{ detailTodo.description || '无' }}</div>
        </el-descriptions-item>
      </el-descriptions>

      <!--
        状态流转区。
        窄屏下操作列已收起流转入口，此处承担全部流转能力；
        桌面端作为行内下拉之外的补充入口，抽屉空间充裕，按钮更大更易点。
      -->
      <div v-if="detailTodo" class="todo-page__drawer-status">
        <div class="app-section-title">状态流转</div>
        <el-space v-if="detailStatusOptions.length > 0" :size="8" wrap>
          <el-button
            v-for="item in detailStatusOptions"
            :key="item.value"
            :type="item.tagType"
            plain
            :loading="pendingStatusId === detailTodo.id"
            :title="item.hint"
            :aria-label="`流转为${item.label}${item.hint ? '，' + item.hint : ''}`"
            @click="handleChangeStatusFromDetail(item.value)"
          >
            转为{{ item.label }}
          </el-button>
        </el-space>
        <span v-else class="app-text-secondary">当前状态没有可流转的后续选项</span>
      </div>

      <template #footer>
        <el-space :size="12">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button type="primary" icon="Edit" @click="editFromDetail">编辑</el-button>
        </el-space>
      </template>
    </el-drawer>

    <!-- 新建 / 编辑弹窗 -->
    <TodoFormDialog v-model="dialogVisible" :todo="editingTodo" @success="handleDialogSuccess" />
  </div>
</template>

<style scoped lang="scss">
.todo-page {
  display: flex;
  flex-direction: column;
  gap: 16px;

  &__filter {
    // 筛选项在小屏下换行时去掉最后一行的下边距，保持视觉紧凑
    :deep(.el-form-item) {
      margin-bottom: 16px;
    }
  }

  &__title {
    margin-bottom: 0;
  }

  &__overdue-tag {
    margin-left: 6px;
  }

  &__assignee {
    display: inline-flex;
    gap: 6px;
    align-items: center;
  }

  &__avatar {
    flex: none;
    font-size: 12px;
    color: #fff;
    background-color: var(--el-color-primary-light-3);
  }

  &__due-danger {
    color: var(--el-color-danger);
  }

  &__description {
    // 保留用户在文本域中输入的换行
    white-space: pre-wrap;
    word-break: break-word;
  }

  // 操作按钮组与菜单说明文字的样式已提取为全局 .app-table-actions / .app-action-hint
  &__drawer-status {
    margin-top: 20px;
  }
}

@media (width <= 768px) {
  .todo-page__filter :deep(.el-form-item) {
    margin-bottom: 12px;
  }
}
</style>

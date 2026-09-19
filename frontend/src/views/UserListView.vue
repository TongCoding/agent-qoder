<script setup lang="ts">
/**
 * 用户管理列表页。
 *
 * 演示了「一对多关联」场景下的典型处理方式：
 * 列表中的 todoCount 由服务端批量聚合返回（不会 N+1），
 * 删除用户时若名下仍有待办，后端返回 40903，前端把该提示直接呈现给用户。
 */
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import UserFormDialog from '@/components/user/UserFormDialog.vue'
import { changeUserStatus, deleteUser, pageUsers } from '@/api/user'
import { MOBILE_QUERY, useMediaQuery } from '@/composables/useMediaQuery'
import { useTable } from '@/composables/useTable'
import {
  PAGE_SIZE_OPTIONS,
  USER_ROLE_OPTIONS,
  USER_STATUS_OPTIONS,
  userRoleDict,
  userStatusDict,
} from '@/utils/dict'
import { formatDateTime } from '@/utils/format'
import { ACTION_MENU_POPPER_OPTIONS } from '@/utils/ui'

import type { UserQuery, UserInfo, UserStatus } from '@/types/user'

/** 查询条件的初始值 */
const DEFAULT_QUERY: UserQuery = {
  page: 1,
  size: 10,
  keyword: '',
  role: null,
  status: null,
  department: '',
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
} = useTable<UserInfo, UserQuery>({
  fetcher: pageUsers,
  defaultQuery: DEFAULT_QUERY,
  // 关闭自动加载，改为在 onMounted 中手动触发，以便首次加载后顺带收集部门候选项
  immediate: false,
})

/** 表单弹窗显隐与当前编辑对象 */
const dialogVisible = ref(false)
const editingUser = ref<UserInfo | null>(null)

/** 状态变更进行中的用户 ID，用于行内 loading */
const pendingStatusId = ref<number | null>(null)

/**
 * 窄屏判定。
 *
 * 与待办列表不同，本页没有详情抽屉可以承接状态流转，
 * 因此窄屏下把「状态 / 编辑 / 删除」全部收进单个「更多」溢出菜单，
 * 操作列由 176px 压到 64px，把宽度让给数据列。
 */
const isMobile = useMediaQuery(MOBILE_QUERY)

/**
 * 操作列宽度。
 *
 * 桌面端 176px：减去 `.cell` 左右各 12px 内边距后可用 152px，
 * 需容纳「状态」按钮 loading 态额外插入的图标（约 +17px），
 * 原值 190px 在计入该图标后内容约 184px，仍会溢出换行。
 * 窄屏 64px：只放一个「更多」图标按钮。
 */
const actionColumnWidth = computed(() => (isMobile.value ? 64 : 176))

/** 部门筛选的候选项，从当前页数据中提取（仅作快捷入口，仍可手动输入） */
const departmentOptions = ref<string[]>([])

/** 收集所有已加载过的部门名，去重后作为筛选候选 */
function collectDepartments(users: UserInfo[]): void {
  const merged = new Set([...departmentOptions.value])
  users.forEach((user) => {
    if (user.department) {
      merged.add(user.department)
    }
  })
  departmentOptions.value = [...merged].sort((a, b) => a.localeCompare(b, 'zh-Hans-CN'))
}

// 首次加载完成后填充部门候选项
onMounted(async () => {
  await loadData()
  collectDepartments(list.value)
})

function openCreateDialog(): void {
  editingUser.value = null
  dialogVisible.value = true
}

function openEditDialog(row: UserInfo): void {
  editingUser.value = row
  dialogVisible.value = true
}

/**
 * 数据变更后的刷新策略。
 *
 * 删空当前页且不是第一页时回退一页，避免停留在空白页。
 */
async function reloadAfterMutation(): Promise<void> {
  if (list.value.length === 0 && (query.page ?? 1) > 1) {
    query.page = (query.page ?? 1) - 1
  }
  await loadData()
}

/** 变更账号状态 */
async function handleChangeStatus(row: UserInfo, status: UserStatus): Promise<void> {
  if (row.status === status) {
    return
  }
  pendingStatusId.value = row.id
  try {
    await changeUserStatus(row.id, status)
    ElMessage.success(`已切换为「${userStatusDict(status)?.label ?? status}」`)
    await loadData()
  } catch {
    // 错误提示已由 Axios 拦截器统一处理
  } finally {
    pendingStatusId.value = null
  }
}

/** 删除用户 */
async function handleDelete(row: UserInfo): Promise<void> {
  try {
    await ElMessageBox.confirm(
      row.todoCount > 0
        ? `「${row.nickname || row.username}」名下还有 ${row.todoCount} 条待办，删除会被拒绝。仍要尝试吗？`
        : `确认删除用户「${row.nickname || row.username}」吗？该操作不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  try {
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    await reloadAfterMutation()
  } catch {
    // 名下仍有待办时后端返回 40903，提示已由拦截器展示
  }
}

function handleDialogSuccess(): void {
  void loadData().then(() => collectDepartments(list.value))
}

/** 可选的状态流转项：排除当前状态 */
function otherStatusOptions(current: UserStatus) {
  return USER_STATUS_OPTIONS.filter((item) => item.value !== current)
}

/**
 * 窄屏「更多」菜单的命令值。
 *
 * 状态项加 `status:` 前缀以与 edit / delete 区分；
 * 用模板字面量类型约束前缀之后只能是合法的 UserStatus，避免命令值写错。
 */
type RowCommand = 'edit' | 'delete' | `status:${UserStatus}`

/** 分发窄屏「更多」菜单的命令 */
function handleRowCommand(row: UserInfo, command: RowCommand): void {
  if (command === 'edit') {
    openEditDialog(row)
    return
  }
  if (command === 'delete') {
    void handleDelete(row)
    return
  }
  // 走到这里 command 已被收窄为 `status:${UserStatus}`，去掉前缀即目标状态
  void handleChangeStatus(row, command.slice('status:'.length) as UserStatus)
}
</script>

<template>
  <div class="user-page">
    <!-- 筛选区 -->
    <div class="app-card user-page__filter">
      <el-form :model="query" label-width="72px" @submit.prevent="search">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="关键字">
              <el-input
                v-model="query.keyword"
                placeholder="用户名 / 昵称 / 邮箱"
                clearable
                @keyup.enter="search"
                @clear="search"
              />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="角色">
              <el-select v-model="query.role" placeholder="全部角色" clearable style="width: 100%">
                <el-option
                  v-for="item in USER_ROLE_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="状态">
              <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 100%">
                <el-option
                  v-for="item in USER_STATUS_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <el-form-item label="部门">
              <el-select
                v-model="query.department"
                placeholder="全部部门"
                clearable
                filterable
                allow-create
                default-first-option
                style="width: 100%"
              >
                <el-option v-for="item in departmentOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label-width="0" class="user-page__filter-actions">
              <el-space :size="8">
                <el-button type="primary" icon="Search" @click="search">查询</el-button>
                <el-button icon="RefreshLeft" @click="reset">重置</el-button>
              </el-space>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <!-- 列表区 -->
    <div class="app-card user-page__table">
      <div class="app-toolbar">
        <div>
          <span class="app-section-title user-page__title">用户列表</span>
          <span class="app-text-secondary">共 {{ total }} 人</span>
        </div>

        <el-space :size="8">
          <el-button icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
          <el-button type="primary" icon="Plus" @click="openCreateDialog">新建用户</el-button>
        </el-space>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        row-key="id"
        border
        stripe
        empty-text="暂无符合条件的用户"
        @sort-change="handleSortChange"
      >
        <el-table-column label="用户" prop="username" min-width="180" sortable="custom">
          <template #default="{ row }: { row: UserInfo }">
            <div class="user-page__identity">
              <el-avatar :size="32" class="user-page__avatar">
                {{ (row.nickname || row.username).slice(0, 1).toUpperCase() }}
              </el-avatar>
              <div class="user-page__identity-text">
                <div>{{ row.nickname || row.username }}</div>
                <div class="app-text-secondary">@{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="邮箱" prop="email" min-width="200" show-overflow-tooltip />

        <el-table-column label="手机号" prop="phone" width="130">
          <template #default="{ row }: { row: UserInfo }">
            {{ row.phone || '-' }}
          </template>
        </el-table-column>

        <el-table-column label="部门" prop="department" width="120" show-overflow-tooltip>
          <template #default="{ row }: { row: UserInfo }">
            {{ row.department || '-' }}
          </template>
        </el-table-column>

        <el-table-column label="角色" prop="role" width="110" sortable="custom" align="center">
          <template #default="{ row }: { row: UserInfo }">
            <el-tooltip :content="userRoleDict(row.role)?.hint" placement="top">
              <el-tag :type="userRoleDict(row.role)?.tagType ?? 'info'" effect="light">
                {{ userRoleDict(row.role)?.label ?? row.role }}
              </el-tag>
            </el-tooltip>
          </template>
        </el-table-column>

        <el-table-column label="状态" prop="status" width="110" sortable="custom" align="center">
          <template #default="{ row }: { row: UserInfo }">
            <el-tag :type="userStatusDict(row.status)?.tagType ?? 'info'" effect="dark">
              {{ userStatusDict(row.status)?.label ?? row.status }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="待办数" prop="todoCount" width="100" align="center">
          <template #default="{ row }: { row: UserInfo }">
            <el-tag v-if="row.todoCount > 0" type="primary" effect="plain" round>
              {{ row.todoCount }}
            </el-tag>
            <span v-else class="app-text-secondary">0</span>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" prop="createdAt" width="170" sortable="custom">
          <template #default="{ row }: { row: UserInfo }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" :width="actionColumnWidth" fixed="right" align="center">
          <template #default="{ row }: { row: UserInfo }">
            <!--
              外层统一包 .app-table-actions：
              原先「状态」被 el-dropdown 包裹，`.el-button + .el-button` 的默认 12px 外边距
              对其后的「编辑」不生效，导致间距忽有忽无；统一交给 gap 控制。
            -->
            <div class="app-table-actions">
              <!--
                窄屏：三个操作全部收进「更多」溢出菜单。
                本页没有详情抽屉可承接状态流转，故不沿用待办列表的方案；
                「长按菜单」在 Web 上会与系统手势冲突、无视觉暗示且键盘不可达，也不采用。
              -->
              <el-dropdown
                v-if="isMobile"
                trigger="click"
                :popper-options="ACTION_MENU_POPPER_OPTIONS"
                @command="(command: RowCommand) => handleRowCommand(row, command)"
              >
                <el-button
                  text
                  type="primary"
                  size="small"
                  :loading="pendingStatusId === row.id"
                  title="更多操作"
                  :aria-label="`对「${row.nickname || row.username}」的更多操作`"
                >
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-for="item in otherStatusOptions(row.status)"
                      :key="item.value"
                      :command="`status:${item.value}`"
                      :title="item.hint"
                      :aria-label="`切换账号状态为${item.label}${item.hint ? '，' + item.hint : ''}`"
                    >
                      <el-tag :type="item.tagType" size="small" effect="plain">{{ item.label }}</el-tag>
                      <!-- tag 颜色对视障用户无意义，语义改由 hint 文本承载 -->
                      <span v-if="item.hint" class="app-action-hint">{{ item.hint }}</span>
                    </el-dropdown-item>

                    <el-dropdown-item
                      divided
                      command="edit"
                      title="编辑"
                      :aria-label="`编辑用户「${row.nickname || row.username}」`"
                    >
                      编辑
                    </el-dropdown-item>
                    <el-dropdown-item
                      command="delete"
                      title="删除"
                      :aria-label="`删除用户「${row.nickname || row.username}」，该操作不可恢复`"
                    >
                      <span class="app-action-danger">删除</span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>

              <!-- 桌面端：状态流转下拉 + 编辑 + 删除 并排 -->
              <template v-else>
                <el-dropdown
                  trigger="click"
                  :popper-options="ACTION_MENU_POPPER_OPTIONS"
                  @command="(status: UserStatus) => handleChangeStatus(row, status)"
                >
                  <el-button
                    text
                    type="primary"
                    size="small"
                    :loading="pendingStatusId === row.id"
                    title="变更账号状态"
                    :aria-label="`变更「${row.nickname || row.username}」的账号状态`"
                  >
                    状态<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item
                        v-for="item in otherStatusOptions(row.status)"
                        :key="item.value"
                        :command="item.value"
                        :title="item.hint"
                        :aria-label="`切换账号状态为${item.label}${item.hint ? '，' + item.hint : ''}`"
                      >
                        <el-tag :type="item.tagType" size="small" effect="plain">
                          {{ item.label }}
                        </el-tag>
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
                  :aria-label="`编辑用户「${row.nickname || row.username}」`"
                  @click="openEditDialog(row)"
                >
                  编辑
                </el-button>
                <el-button
                  text
                  type="danger"
                  size="small"
                  title="删除"
                  :aria-label="`删除用户「${row.nickname || row.username}」，该操作不可恢复`"
                  @click="handleDelete(row)"
                >
                  删除
                </el-button>
              </template>
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

    <!-- 新建 / 编辑弹窗 -->
    <UserFormDialog v-model="dialogVisible" :user="editingUser" @success="handleDialogSuccess" />
  </div>
</template>

<style scoped lang="scss">
.user-page {
  display: flex;
  flex-direction: column;
  gap: 16px;

  &__filter :deep(.el-form-item) {
    margin-bottom: 16px;
  }

  &__filter-actions {
    margin-bottom: 0 !important;
  }

  &__title {
    margin-bottom: 0;
  }

  &__identity {
    display: flex;
    gap: 10px;
    align-items: center;
    min-width: 0;
  }

  &__avatar {
    flex: none;
    font-size: 14px;
    color: #fff;
    background-color: var(--el-color-primary-light-3);
  }

  &__identity-text {
    min-width: 0;
    line-height: 1.4;
  }
}

@media (width <= 768px) {
  .user-page__filter :deep(.el-form-item) {
    margin-bottom: 12px;
  }
}
</style>

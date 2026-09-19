<script setup lang="ts">
/**
 * 仪表盘。
 *
 * 通过两个聚合接口一次性拿到全部指标，避免前端为每个数字单独发一次请求：
 * - `GET /api/v1/todos/statistics`
 * - `GET /api/v1/users/statistics`
 *
 * 两个请求相互独立，因此用 `Promise.allSettled` 并发发起：
 * 即使其中一个失败，另一个的数据依然可以正常展示。
 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import StatCard from '@/components/common/StatCard.vue'
import { getTodoStatistics } from '@/api/todo'
import { getUserStatistics } from '@/api/user'
import {
  TODO_PRIORITY_OPTIONS,
  TODO_STATUS_OPTIONS,
  USER_ROLE_OPTIONS,
  USER_STATUS_OPTIONS,
  isFinishedStatus,
} from '@/utils/dict'
import { describeDueDate, formatDateTime } from '@/utils/format'

import type { TodoStatistics } from '@/types/todo'
import type { UserStatistics } from '@/types/user'

const router = useRouter()

const todoStats = ref<TodoStatistics | null>(null)
const userStats = ref<UserStatistics | null>(null)
const loading = ref(false)
/** 两个接口都失败时展示整体错误态 */
const failed = ref(false)

/** 并行加载统计数据 */
async function loadStatistics(): Promise<void> {
  loading.value = true
  failed.value = false

  const [todoResult, userResult] = await Promise.allSettled([
    getTodoStatistics(),
    getUserStatistics(),
  ])

  todoStats.value = todoResult.status === 'fulfilled' ? todoResult.value : null
  userStats.value = userResult.status === 'fulfilled' ? userResult.value : null
  failed.value = todoResult.status === 'rejected' && userResult.status === 'rejected'
  loading.value = false
}

onMounted(() => void loadStatistics())

/** 计算某一项在总量中的占比，总量为 0 时返回 0 以避免 NaN */
function percentOf(part: number, total: number): number {
  if (total <= 0) {
    return 0
  }
  return Math.round((part / total) * 1000) / 10
}

const todoTotal = computed(() => todoStats.value?.total ?? 0)
const userTotal = computed(() => userStats.value?.total ?? 0)

/** 状态分布：以字典顺序展示，保证 UI 顺序稳定（后端 Map 的遍历顺序不可依赖） */
const statusDistribution = computed(() =>
  TODO_STATUS_OPTIONS.map((item) => {
    const count = todoStats.value?.statusCounts[item.value] ?? 0
    return {
      ...item,
      count,
      percent: percentOf(count, todoTotal.value),
    }
  }),
)

const priorityDistribution = computed(() =>
  TODO_PRIORITY_OPTIONS.map((item) => {
    const count = todoStats.value?.priorityCounts[item.value] ?? 0
    return {
      ...item,
      count,
      percent: percentOf(count, todoTotal.value),
    }
  }),
)

const roleDistribution = computed(() =>
  USER_ROLE_OPTIONS.map((item) => {
    const count = userStats.value?.roleCounts[item.value] ?? 0
    return {
      ...item,
      count,
      percent: percentOf(count, userTotal.value),
    }
  }),
)

const userStatusDistribution = computed(() =>
  USER_STATUS_OPTIONS.map((item) => {
    const count = userStats.value?.statusCounts[item.value] ?? 0
    return {
      ...item,
      count,
      percent: percentOf(count, userTotal.value),
    }
  }),
)

/** el-progress 只接受有限的颜色关键字，这里做一次映射 */
const PROGRESS_COLOR_MAP = {
  primary: '#409eff',
  success: '#67c23a',
  info: '#909399',
  warning: '#e6a23c',
  danger: '#f56c6c',
} as const

function progressColor(tagType: keyof typeof PROGRESS_COLOR_MAP): string {
  return PROGRESS_COLOR_MAP[tagType] ?? PROGRESS_COLOR_MAP.primary
}

/** 跳转到待办列表并带上筛选条件 */
function goTodos(status?: string): void {
  void router.push(status ? { path: '/todos', query: { status } } : { path: '/todos' })
}
</script>

<template>
  <div class="dashboard">
    <!-- 顶部操作栏 -->
    <div class="app-toolbar dashboard__toolbar">
      <div>
        <h2 class="dashboard__heading">数据概览</h2>
        <span class="app-text-secondary">待办与团队成员的实时聚合指标</span>
      </div>
      <el-button icon="Refresh" :loading="loading" @click="loadStatistics">刷新数据</el-button>
    </div>

    <!-- 整体失败态 -->
    <el-card v-if="failed" shadow="never" class="dashboard__error">
      <el-empty description="统计数据加载失败，请确认后端服务已启动">
        <el-button type="primary" icon="Refresh" @click="loadStatistics">重新加载</el-button>
      </el-empty>
    </el-card>

    <template v-else>
      <!-- 核心指标 -->
      <el-row :gutter="16" class="dashboard__row">
        <el-col :xs="24" :sm="12" :lg="6">
          <StatCard
            title="待办总数"
            :value="todoStats?.total ?? 0"
            icon="Tickets"
            color="#409eff"
            :loading="loading"
            :description="`未分配 ${todoStats?.unassignedCount ?? 0} 条`"
            to="/todos"
          />
        </el-col>

        <el-col :xs="24" :sm="12" :lg="6">
          <StatCard
            title="进行中"
            :value="todoStats?.statusCounts.IN_PROGRESS ?? 0"
            icon="Loading"
            color="#e6a23c"
            :loading="loading"
            :description="`待处理 ${todoStats?.statusCounts.PENDING ?? 0} 条`"
            @click="goTodos('IN_PROGRESS')"
          />
        </el-col>

        <el-col :xs="24" :sm="12" :lg="6">
          <StatCard
            title="已逾期"
            :value="todoStats?.overdueCount ?? 0"
            icon="AlarmClock"
            color="#f56c6c"
            :loading="loading"
            :description="`今日到期 ${todoStats?.dueTodayCount ?? 0} 条`"
            @click="goTodos()"
          />
        </el-col>

        <el-col :xs="24" :sm="12" :lg="6">
          <StatCard
            title="完成率"
            :value="todoStats?.completionRate ?? 0"
            suffix="%"
            icon="CircleCheck"
            color="#67c23a"
            :loading="loading"
            :description="`已完成 ${todoStats?.statusCounts.DONE ?? 0} 条`"
          />
        </el-col>
      </el-row>

      <!-- 分布情况 -->
      <el-row :gutter="16" class="dashboard__row">
        <el-col :xs="24" :md="12" :lg="8">
          <div class="app-card dashboard__panel">
            <h3 class="app-section-title">待办状态分布</h3>
            <el-skeleton :loading="loading" animated :rows="4">
              <template #default>
                <div v-for="item in statusDistribution" :key="item.value" class="dashboard__bar">
                  <div class="dashboard__bar-label">
                    <span>{{ item.label }}</span>
                    <span class="app-text-secondary">{{ item.count }} 条 · {{ item.percent }}%</span>
                  </div>
                  <el-progress
                    :percentage="item.percent"
                    :color="progressColor(item.tagType)"
                    :show-text="false"
                    :stroke-width="10"
                  />
                </div>
              </template>
            </el-skeleton>
          </div>
        </el-col>

        <el-col :xs="24" :md="12" :lg="8">
          <div class="app-card dashboard__panel">
            <h3 class="app-section-title">优先级分布</h3>
            <el-skeleton :loading="loading" animated :rows="4">
              <template #default>
                <div v-for="item in priorityDistribution" :key="item.value" class="dashboard__bar">
                  <div class="dashboard__bar-label">
                    <span>{{ item.label }}</span>
                    <span class="app-text-secondary">{{ item.count }} 条 · {{ item.percent }}%</span>
                  </div>
                  <el-progress
                    :percentage="item.percent"
                    :color="progressColor(item.tagType)"
                    :show-text="false"
                    :stroke-width="10"
                  />
                </div>
              </template>
            </el-skeleton>
          </div>
        </el-col>

        <el-col :xs="24" :md="24" :lg="8">
          <div class="app-card dashboard__panel">
            <h3 class="app-section-title">团队概况</h3>
            <el-skeleton :loading="loading" animated :rows="4">
              <template #default>
                <div class="dashboard__team-total">
                  <span class="dashboard__team-number">{{ userTotal }}</span>
                  <span class="app-text-secondary">位成员</span>
                  <el-button text type="primary" size="small" @click="router.push('/users')">
                    管理用户
                  </el-button>
                </div>

                <el-divider content-position="left">角色构成</el-divider>
                <div v-for="item in roleDistribution" :key="item.value" class="dashboard__bar">
                  <div class="dashboard__bar-label">
                    <span>{{ item.label }}</span>
                    <span class="app-text-secondary">{{ item.count }} 人</span>
                  </div>
                  <el-progress
                    :percentage="item.percent"
                    :color="progressColor(item.tagType)"
                    :show-text="false"
                    :stroke-width="8"
                  />
                </div>

                <el-divider content-position="left">账号状态</el-divider>
                <el-space :size="8" wrap>
                  <el-tag
                    v-for="item in userStatusDistribution"
                    :key="item.value"
                    :type="item.tagType"
                    effect="light"
                  >
                    {{ item.label }} {{ item.count }}
                  </el-tag>
                </el-space>
              </template>
            </el-skeleton>
          </div>
        </el-col>
      </el-row>

      <!-- 最近更新的待办 -->
      <div class="app-card dashboard__panel">
        <div class="app-toolbar">
          <h3 class="app-section-title dashboard__panel-title">最近更新的待办</h3>
          <el-button text type="primary" @click="goTodos()">
            查看全部<el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </div>

        <el-table
          v-loading="loading"
          :data="todoStats?.recentTodos ?? []"
          empty-text="暂无待办数据"
          size="default"
        >
          <el-table-column label="标题" prop="title" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              <span>{{ row.title }}</span>
              <el-tag v-if="row.overdue" type="danger" size="small" effect="dark" class="dashboard__overdue">
                逾期
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="状态" prop="status" width="110" align="center">
            <template #default="{ row }">
              <el-tag
                :type="TODO_STATUS_OPTIONS.find((i) => i.value === row.status)?.tagType ?? 'info'"
                effect="light"
              >
                {{ TODO_STATUS_OPTIONS.find((i) => i.value === row.status)?.label ?? row.status }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="优先级" prop="priority" width="100" align="center">
            <template #default="{ row }">
              <el-tag
                :type="TODO_PRIORITY_OPTIONS.find((i) => i.value === row.priority)?.tagType ?? 'info'"
                effect="plain"
              >
                {{ TODO_PRIORITY_OPTIONS.find((i) => i.value === row.priority)?.label ?? row.priority }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="负责人" width="130" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.assignee">{{ row.assignee.nickname || row.assignee.username }}</span>
              <span v-else class="app-text-secondary">未分配</span>
            </template>
          </el-table-column>

          <el-table-column label="截止情况" width="160">
            <template #default="{ row }">
              <span :class="{ 'dashboard__danger': row.overdue }">
                {{ describeDueDate(row.dueDate, isFinishedStatus(row.status)) }}
              </span>
            </template>
          </el-table-column>

          <el-table-column label="更新时间" width="170">
            <template #default="{ row }">
              {{ formatDateTime(row.updatedAt || row.createdAt) }}
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;

  &__toolbar {
    margin-bottom: 0;
  }

  &__heading {
    margin: 0;
    font-size: 20px;
    font-weight: 600;
    color: var(--app-text-primary);
  }

  &__row {
    // el-row 的 gutter 会给子项加左右 padding，这里用负 margin 抵消外层多余间距
    row-gap: 16px;
  }

  &__panel {
    height: 100%;
  }

  &__panel-title {
    margin-bottom: 0;
  }

  &__bar {
    & + & {
      margin-top: 14px;
    }
  }

  &__bar-label {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 6px;
    font-size: 13px;
    color: var(--app-text-regular);
  }

  &__team-total {
    display: flex;
    gap: 8px;
    align-items: baseline;
  }

  &__team-number {
    font-size: 30px;
    font-weight: 700;
    line-height: 1;
    color: var(--el-color-primary);
    font-variant-numeric: tabular-nums;
  }

  &__overdue {
    margin-left: 6px;
  }

  &__danger {
    color: var(--el-color-danger);
  }

  &__error {
    border: none;
    border-radius: var(--app-border-radius);
  }
}

// el-divider 在面板内不需要太大的上下间距
:deep(.el-divider--horizontal) {
  margin: 16px 0 12px;
}
</style>

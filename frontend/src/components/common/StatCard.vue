<script setup lang="ts">
/**
 * 统计卡片。
 *
 * 仪表盘中的指标块，负责把「图标 + 数值 + 说明」统一成一致的视觉样式。
 * 不直接调用接口，数据由父组件传入，保证组件可复用、可测试。
 */
import { useRouter } from 'vue-router'

const props = withDefaults(
  defineProps<{
    /** 指标名称 */
    title: string
    /** 指标数值 */
    value: number | string
    /** Element Plus 图标组件名 */
    icon?: string
    /** 图标底色，默认使用主题色 */
    color?: string
    /** 数值单位或后缀，例如 % */
    suffix?: string
    /** 补充说明，展示在数值下方 */
    description?: string
    /** 加载中，展示骨架屏 */
    loading?: boolean
    /** 点击后跳转的路由地址，为空时卡片不可点击 */
    to?: string
  }>(),
  {
    icon: 'DataLine',
    color: 'var(--el-color-primary)',
    suffix: '',
    description: '',
    loading: false,
    to: '',
  },
)

const emit = defineEmits<{
  /** 卡片被点击，父组件可用于联动筛选 */
  click: []
}>()

const router = useRouter()

function handleClick(): void {
  emit('click')
  if (props.to) {
    void router.push(props.to)
  }
}
</script>

<template>
  <div
    class="stat-card app-card"
    :class="{ 'stat-card--clickable': Boolean(to) }"
    role="group"
    :aria-label="`${title} ${value}${suffix}`"
    @click="handleClick"
  >
    <el-skeleton :loading="loading" animated :rows="1">
      <template #default>
        <div class="stat-card__icon" :style="{ backgroundColor: color }">
          <el-icon :size="22">
            <component :is="icon" />
          </el-icon>
        </div>

        <div class="stat-card__body">
          <span class="stat-card__title app-text-secondary">{{ title }}</span>
          <span class="stat-card__value">
            {{ value }}<small v-if="suffix" class="stat-card__suffix">{{ suffix }}</small>
          </span>
          <span v-if="description" class="stat-card__description app-text-secondary">
            {{ description }}
          </span>
        </div>
      </template>
    </el-skeleton>
  </div>
</template>

<style scoped lang="scss">
.stat-card {
  display: flex;
  gap: 14px;
  align-items: center;
  transition:
    box-shadow var(--app-transition-duration) ease,
    transform var(--app-transition-duration) ease;

  &--clickable {
    cursor: pointer;

    &:hover {
      box-shadow: var(--app-shadow-hover);
      transform: translateY(-2px);
    }
  }

  &__icon {
    display: flex;
    flex: none;
    align-items: center;
    justify-content: center;
    width: 46px;
    height: 46px;
    color: #fff;
    border-radius: 12px;
  }

  &__body {
    display: flex;
    flex-direction: column;
    min-width: 0;
  }

  &__title {
    line-height: 1.4;
  }

  &__value {
    font-size: 26px;
    font-weight: 700;
    line-height: 1.2;
    color: var(--app-text-primary);
    font-variant-numeric: tabular-nums;
  }

  &__suffix {
    margin-left: 2px;
    font-size: 13px;
    font-weight: 500;
    color: var(--app-text-secondary);
  }

  &__description {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

// el-skeleton 内部结构需要撑满，否则占位高度与真实内容不一致
:deep(.el-skeleton) {
  width: 100%;
}

:deep(.el-skeleton__item) {
  height: 46px;
}
</style>

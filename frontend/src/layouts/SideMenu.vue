<script setup lang="ts">
/**
 * 侧边栏菜单。
 *
 * 菜单项直接由路由表（`mainRoutes`）派生，新增页面无需再改这里。
 * 抽成独立组件是为了让桌面端侧边栏与移动端抽屉复用同一份模板。
 */
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import { mainRoutes } from '@/router'

defineProps<{
  /** 是否折叠（仅桌面端生效，折叠后只显示图标） */
  collapsed?: boolean
}>()

const emit = defineEmits<{
  /** 选中菜单项后触发，移动端用于自动关闭抽屉 */
  select: [path: string]
}>()

const route = useRoute()

/** 过滤掉标记为 hidden 的路由，并拼出完整访问路径 */
const menuItems = computed(() =>
  mainRoutes
    .filter((item) => !item.meta?.hidden && item.meta?.title)
    .map((item) => ({
      // mainRoutes 均为 '/' 的直接子路由，path 不含前导斜杠
      path: `/${item.path}`.replace(/\/+/g, '/'),
      title: item.meta?.title ?? '',
      icon: item.meta?.icon,
    })),
)
</script>

<template>
  <el-menu
    :default-active="route.path"
    :collapse="collapsed"
    :collapse-transition="false"
    router
    class="side-menu"
    @select="(path: string) => emit('select', path)"
  >
    <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
      <el-icon v-if="item.icon">
        <component :is="item.icon" />
      </el-icon>
      <template #title>{{ item.title }}</template>
    </el-menu-item>
  </el-menu>
</template>

<style scoped lang="scss">
.side-menu {
  // 去掉 Element Plus 默认的右侧边框，交由容器控制背景
  border-right: none;

  // 折叠态下菜单宽度由 el-aside 决定，这里避免内部出现横向滚动
  &:not(.el-menu--collapse) {
    width: 100%;
  }
}
</style>

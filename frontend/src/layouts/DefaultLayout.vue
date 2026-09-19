<script setup lang="ts">
/**
 * 默认布局。
 *
 * 结构：左侧边栏（导航菜单） + 右侧（顶栏 + 内容区）。
 *
 * 响应式策略：
 * - 桌面端：侧边栏常驻，可折叠为纯图标模式（折叠状态持久化到 localStorage）；
 * - 移动端（≤768px）：侧边栏隐藏，改为 el-drawer 抽屉浮层，选中菜单后自动收起，
 *   避免小屏下内容区被菜单挤压。
 */
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import SideMenu from '@/layouts/SideMenu.vue'
import { MOBILE_QUERY, useMediaQuery } from '@/composables/useMediaQuery'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const appStore = useAppStore()

/** 是否处于移动端断点 */
const isMobile = useMediaQuery(MOBILE_QUERY)

/** 移动端抽屉的显隐 */
const drawerVisible = ref(false)

/** Swagger UI 地址，生产环境未配置时隐藏入口 */
const swaggerUrl = import.meta.env.VITE_SWAGGER_URL

/** 面包屑：首页 + 当前页面 */
const breadcrumbs = computed(() => {
  const current = route.meta.title
  const items = [{ label: '首页', path: '/dashboard' }]
  if (current && route.path !== '/dashboard') {
    items.push({ label: current, path: route.path })
  }
  return items
})

/** 当前页面的说明文字 */
const pageDescription = computed(() => route.meta.description ?? '')

// 切换到桌面端时收起抽屉，避免残留浮层遮挡内容
watch(isMobile, (mobile) => {
  if (!mobile) {
    drawerVisible.value = false
  }
})

/** 移动端选中菜单后关闭抽屉 */
function handleMenuSelect(): void {
  if (isMobile.value) {
    drawerVisible.value = false
  }
}

/** 顶栏左侧按钮：桌面端折叠侧边栏，移动端打开抽屉 */
function handleToggle(): void {
  if (isMobile.value) {
    drawerVisible.value = true
  } else {
    appStore.toggleSidebar()
  }
}
</script>

<template>
  <el-container class="layout">
    <!-- 桌面端侧边栏 -->
    <el-aside v-if="!isMobile" :width="appStore.sidebarWidth" class="layout__aside">
      <div class="layout__logo">
        <el-icon :size="24" color="var(--el-color-primary)">
          <Box />
        </el-icon>
        <span v-show="!appStore.sidebarCollapsed" class="layout__logo-text">TaskHub</span>
      </div>
      <SideMenu :collapsed="appStore.sidebarCollapsed" @select="handleMenuSelect" />
    </el-aside>

    <el-container class="layout__body">
      <!-- 顶栏 -->
      <el-header class="layout__header" height="60px">
        <div class="layout__header-left">
          <el-button text :icon="isMobile ? 'Menu' : 'Fold'" @click="handleToggle">
            <span class="layout__visually-hidden">切换导航</span>
          </el-button>

          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path" :to="item.path">
              {{ item.label }}
            </el-breadcrumb-item>
          </el-breadcrumb>

          <span v-if="pageDescription" class="layout__description app-text-secondary">
            {{ pageDescription }}
          </span>
        </div>

        <div class="layout__header-right">
          <el-tooltip v-if="swaggerUrl" content="在新窗口打开后端接口文档" placement="bottom">
            <el-button text tag="a" :href="swaggerUrl" target="_blank" rel="noopener">
              <el-icon><Document /></el-icon>
              <span class="layout__btn-text">接口文档</span>
            </el-button>
          </el-tooltip>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="layout__main">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>

    <!-- 移动端抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      direction="ltr"
      :size="220"
      :with-header="false"
      class="layout__drawer"
    >
      <div class="layout__logo">
        <el-icon :size="24" color="var(--el-color-primary)">
          <Box />
        </el-icon>
        <span class="layout__logo-text">TaskHub</span>
      </div>
      <SideMenu @select="handleMenuSelect" />
    </el-drawer>
  </el-container>
</template>

<style scoped lang="scss">
.layout {
  height: 100%;

  &__aside {
    display: flex;
    flex-direction: column;
    overflow: hidden;
    background-color: var(--app-bg-container);
    border-right: 1px solid var(--app-border-color);
    // 宽度过渡由 :width 驱动，折叠时更平滑
    transition: width var(--app-transition-duration) ease;
  }

  &__body {
    // 侧边栏展开时内容区需要能被压缩，否则 flex 子项默认 min-width:auto 会撑破容器
    min-width: 0;
    flex-direction: column;
  }

  &__logo {
    display: flex;
    gap: 10px;
    align-items: center;
    height: var(--app-header-height);
    padding: 0 18px;
    overflow: hidden;
    border-bottom: 1px solid var(--app-border-color);
  }

  &__logo-text {
    font-size: 18px;
    font-weight: 700;
    letter-spacing: 0.5px;
    color: var(--app-text-primary);
    white-space: nowrap;
  }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 var(--app-content-padding);
    background-color: var(--app-bg-container);
    border-bottom: 1px solid var(--app-border-color);
  }

  &__header-left,
  &__header-right {
    display: flex;
    gap: 12px;
    align-items: center;
    min-width: 0;
  }

  &__description {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__main {
    padding: var(--app-content-padding);
    overflow-y: auto;
    background-color: var(--app-bg-page);
  }

  &__btn-text {
    margin-left: 4px;
  }

  // 仅供屏幕阅读器识别的文本，图标按钮需要它来说明用途
  &__visually-hidden {
    position: absolute;
    width: 1px;
    height: 1px;
    padding: 0;
    margin: -1px;
    overflow: hidden;
    clip: rect(0, 0, 0, 0);
    white-space: nowrap;
    border: 0;
  }
}

// 小屏隐藏页面说明与按钮文字，只保留图标，防止顶栏溢出
@media (width <= 992px) {
  .layout__description {
    display: none;
  }
}

@media (width <= 768px) {
  .layout__btn-text {
    display: none;
  }
}
</style>

<script setup lang="ts">
/**
 * 应用根组件。
 *
 * 布局与导航都在 `DefaultLayout` 中，根组件只保留一个出口，
 * 这样 404 等不需要布局的页面可以直接作为顶层路由渲染。
 */
</script>

<template>
  <router-view />
</template>

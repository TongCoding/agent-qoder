<script setup lang="ts">
/**
 * 404 页面。
 *
 * 作为顶层路由（不在 DefaultLayout 内），因此自带返回入口，
 * 用户误输入地址时也能一键回到主界面。
 */
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome(): void {
  void router.replace('/dashboard')
}

function goBack(): void {
  // 无历史记录（例如直接粘贴链接打开）时回退到首页
  if (window.history.state?.back) {
    router.back()
  } else {
    goHome()
  }
}
</script>

<template>
  <div class="not-found">
    <el-result icon="warning" title="404" sub-title="抱歉，你访问的页面不存在或已被移除">
      <template #extra>
        <el-space :size="12">
          <el-button type="primary" icon="HomeFilled" @click="goHome">回到首页</el-button>
          <el-button icon="Back" @click="goBack">返回上一页</el-button>
        </el-space>
      </template>
    </el-result>
  </div>
</template>

<style scoped lang="scss">
.not-found {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background-color: var(--app-bg-page);
}
</style>

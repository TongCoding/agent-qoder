import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

/**
 * 路由元信息类型扩展。
 *
 * 声明合并后，`route.meta.title` 在模板与 TS 中都有类型提示。
 */
declare module 'vue-router' {
  interface RouteMeta {
    /** 页面标题，用于浏览器标签、面包屑与侧边栏菜单 */
    title?: string
    /** Element Plus 图标组件名，用于侧边栏菜单 */
    icon?: string
    /** 为 true 时不在侧边栏菜单中展示 */
    hidden?: boolean
    /** 页面副标题说明 */
    description?: string
  }
}

/**
 * 主布局下的子路由。
 *
 * 单独导出是为了让侧边栏菜单直接由路由表派生：
 * 新增页面时只改这一处，菜单会自动出现，避免路由与菜单两份配置不同步。
 */
export const mainRoutes: RouteRecordRaw[] = [
  {
    path: 'dashboard',
    name: 'Dashboard',
    component: () => import('@/views/DashboardView.vue'),
    meta: {
      title: '仪表盘',
      icon: 'Odometer',
      description: '待办与用户的整体概览',
    },
  },
  {
    path: 'todos',
    name: 'TodoList',
    component: () => import('@/views/TodoListView.vue'),
    meta: {
      title: '待办事项',
      icon: 'List',
      description: '创建、筛选、流转与批量管理待办',
    },
  },
  {
    path: 'users',
    name: 'UserList',
    component: () => import('@/views/UserListView.vue'),
    meta: {
      title: '用户管理',
      icon: 'User',
      description: '维护成员档案、角色与账号状态',
    },
  },
]

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    redirect: '/dashboard',
    children: mainRoutes,
  },
  {
    // 兜底路由：使用 path-to-regexp 的命名参数匹配所有未定义路径
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '页面不存在', hidden: true },
  },
]

const router = createRouter({
  // history 模式需要服务端把所有前端路由回退到 index.html，README 中给出了 Nginx 配置示例
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  // 切换路由时回到顶部，避免长列表页的滚动位置被带到下一页
  scrollBehavior: (_to, _from, savedPosition) => savedPosition ?? { top: 0 },
})

/** 站点基础名，取自环境变量 */
const APP_TITLE = import.meta.env.VITE_APP_TITLE || 'TaskHub'

router.afterEach((to) => {
  const title = to.meta.title
  document.title = title ? `${title} · ${APP_TITLE}` : APP_TITLE
})

export default router

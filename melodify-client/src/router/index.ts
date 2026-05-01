import { createRouter, createWebHistory } from 'vue-router'
import type { RouteLocationNormalized } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { safeInternalPath } from '@/utils/redirect'

/**
 * 路由说明：
 * - meta.layout === 'blank'：全屏页（无 AppShell 顶栏），由 App.vue 识别。
 * - meta.requiresAuth：需登录；未登录跳转 /login 并带上 redirect（预留，业务路由按需开启）。
 *
 * 页面组件约定：按路由分段建目录，入口为 index.vue（如 views/home、views/about、views/login）。
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/explore',
      name: 'explore',
      component: () => import('@/views/explore/index.vue'),
    },
    {
      path: '/generate',
      name: 'generate',
      meta: { requiresAuth: true },
      component: () => import('@/views/generate/index.vue'),
    },
    {
      path: '/works',
      name: 'works',
      meta: { requiresAuth: true },
      component: () => import('@/views/works/index.vue'),
    },
    {
      path: '/works/:id',
      name: 'work-detail',
      meta: { requiresAuth: true },
      component: () => import('@/views/works/detail.vue'),
    },
    {
      path: '/recharge',
      name: 'recharge',
      meta: { requiresAuth: true },
      component: () => import('@/views/recharge/index.vue'),
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('@/views/about/index.vue'),
    },
    {
      path: '/login',
      name: 'login',
      meta: { layout: 'blank' },
      component: () => import('@/views/login/index.vue'),
    },
    {
      path: '/register',
      name: 'register',
      meta: { layout: 'blank' },
      component: () => import('@/views/register/index.vue'),
    },
  ],
})

/**
 * 全局前置守卫：恢复会话、拦截已登录访问登录注册页、预留需登录路由。
 */
router.beforeEach(async (to: RouteLocationNormalized) => {
  const auth = useAuthStore()

  // 首访：从 sessionStorage 恢复登录态（供守卫与页面使用）
  if (!auth.initialized) {
    await auth.initialize()
  }

  // 已登录用户不必再看登录/注册页，redirect 仅允许站内路径
  if (auth.isAuthenticated && (to.name === 'login' || to.name === 'register')) {
    return safeInternalPath(to.query.redirect)
  }

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    }
  }
  return true
})

export default router

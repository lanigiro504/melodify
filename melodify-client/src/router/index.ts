import { createRouter, createWebHistory } from 'vue-router'
import type { RouteLocationNormalized } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { safeInternalPath } from '@/utils/redirect'
import { composeClientDocumentTitle } from './routeTitle'

/** 路由说明：
 * - meta.requiresAuth：需登录
 */

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      meta: { title: '首页' },
      component: () => import('@/views/home/index.vue'),
    },
    {
      path: '/explore',
      name: 'explore',
      meta: { title: '作品广场' },
      component: () => import('@/views/explore/index.vue'),
    },
    {
      path: '/generate',
      name: 'generate',
      meta: { requiresAuth: true, title: '创作' },
      component: () => import('@/views/generate/index.vue'),
    },
    {
      path: '/works',
      name: 'works',
      meta: { requiresAuth: true, title: '作品库' },
      component: () => import('@/views/works/index.vue'),
    },
    {
      path: '/works/:id',
      name: 'work-detail',
      meta: { requiresAuth: true, title: '作品详情' },
      component: () => import('@/views/works/detail.vue'),
    },
    {
      path: '/recharge',
      name: 'recharge',
      meta: { requiresAuth: true, title: '积分充值' },
      component: () => import('@/views/recharge/index.vue'),
    },
    {
      path: '/about',
      name: 'about',
      meta: { title: '关于与帮助' },
      component: () => import('@/views/about/index.vue'),
    },
    {
      path: '/login',
      name: 'login',
      meta: { title: '登录' },
      component: () => import('@/views/login/index.vue'),
    },
    {
      path: '/register',
      name: 'register',
      meta: { title: '注册' },
      component: () => import('@/views/register/index.vue'),
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      meta: { title: '页面未找到' },
      component: () => import('@/views/NotFound.vue'),
    },
  ],
})

router.beforeEach(async (to: RouteLocationNormalized) => {
  const auth = useAuthStore()

  if (!auth.initialized) {
    await auth.initialize()
  }

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

router.afterEach((to) => {
  if (typeof window === 'undefined') return
  window.scrollTo({ top: 0, behavior: 'auto' })
  document.title = composeClientDocumentTitle(to)
})

export default router

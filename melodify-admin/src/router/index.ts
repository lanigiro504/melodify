import { createRouter, createWebHistory } from 'vue-router'
import type { RouteLocationNormalized } from 'vue-router'
import { useAdminAuthStore } from '@/stores/adminAuth'

const ADMIN_SITE_TITLE = 'Melodify 控制台'

function composeDocumentTitle(route: RouteLocationNormalized): string {
  const matched = route.matched
  for (let i = matched.length - 1; i >= 0; i--) {
    const t = matched[i]?.meta?.title
    if (typeof t === 'string' && t.trim()) return `${t.trim()} · ${ADMIN_SITE_TITLE}`
  }
  return ADMIN_SITE_TITLE
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'admin-login',
      component: () => import('@/views/login/index.vue'),
      meta: { public: true, title: '登录' },
    },
    {
      path: '/',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          redirect: '/tasks',
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('@/views/users/index.vue'),
          meta: { title: '用户管理' },
        },
        {
          path: 'roles',
          name: 'admin-roles',
          component: () => import('@/views/roles/index.vue'),
          meta: { title: '角色管理' },
        },
        {
          path: 'tasks',
          name: 'admin-tasks',
          component: () => import('@/views/tasks/index.vue'),
          meta: { title: '生成任务' },
        },
        {
          path: 'assets',
          name: 'admin-assets',
          component: () => import('@/views/assets/index.vue'),
          meta: { title: '成品资产' },
        },
        {
          path: 'point-logs',
          name: 'admin-point-logs',
          component: () => import('@/views/point-logs/index.vue'),
          meta: { title: '积分流水' },
        },
        {
          path: 'recharge-orders',
          name: 'admin-recharge-orders',
          component: () => import('@/views/recharge-orders/index.vue'),
          meta: { title: '充值订单' },
        },
        {
          path: 'point-products',
          name: 'admin-point-products',
          component: () => import('@/views/point-products/index.vue'),
          meta: { title: '积分商品' },
        },
      ],
    },
  ],
})

router.beforeEach(async (to: RouteLocationNormalized) => {
  const auth = useAdminAuthStore()
  if (!auth.initialized) {
    await auth.initialize()
  }

  const isPublic = to.meta.public === true
  if (auth.isAuthenticated && to.name === 'admin-login') {
    const r = to.query.redirect
    if (typeof r === 'string' && r.startsWith('/') && !r.startsWith('//')) {
      return r
    }
    return { path: '/' }
  }

  if (!isPublic && to.meta.requiresAuth && !auth.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

router.afterEach((to) => {
  document.title = composeDocumentTitle(to)
  window.scrollTo({ top: 0, behavior: 'auto' })
})

export default router

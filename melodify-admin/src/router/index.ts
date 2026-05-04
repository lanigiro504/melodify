import { createRouter, createWebHistory } from 'vue-router'
import type { RouteLocationNormalized } from 'vue-router'
import { useAdminAuthStore } from '@/stores/adminAuth'
import { safeInternalPath } from '@/utils/safeInternalPath'
import { ADMIN_ROUTE_NAMES, buildAdminShellChildRoutes } from './adminNavConfig'
import { composeAdminDocumentTitle } from './routeTitle'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: ADMIN_ROUTE_NAMES.LOGIN,
      component: () => import('@/views/login/index.vue'),
      meta: { public: true, title: '登录' },
    },
    {
      path: '/',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAuth: true },
      children: buildAdminShellChildRoutes(),
    },
  ],
})

router.beforeEach(async (to: RouteLocationNormalized) => {
  const auth = useAdminAuthStore()
  if (!auth.initialized) {
    await auth.initialize()
  }

  const isPublic = to.meta.public === true
  if (auth.isAuthenticated && to.name === ADMIN_ROUTE_NAMES.LOGIN) {
    const target = safeInternalPath(to.query.redirect)
    return target === '/' ? { path: '/' } : target
  }

  if (!isPublic && to.meta.requiresAuth && !auth.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

router.afterEach((to) => {
  document.title = composeAdminDocumentTitle(to)
  window.scrollTo({ top: 0, behavior: 'auto' })
})

export default router

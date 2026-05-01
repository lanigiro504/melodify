import { createRouter, createWebHistory } from 'vue-router'
import type { RouteLocationNormalized } from 'vue-router'
import { useAdminAuthStore } from '@/stores/adminAuth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'admin-login',
      component: () => import('@/views/login/index.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'admin-dashboard',
          component: () => import('@/views/dashboard/index.vue'),
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('@/views/users/index.vue'),
        },
        {
          path: 'roles',
          name: 'admin-roles',
          component: () => import('@/views/roles/index.vue'),
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

export default router

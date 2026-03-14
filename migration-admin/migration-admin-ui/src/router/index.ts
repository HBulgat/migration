import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/Login/index.vue'),
      meta: { title: '登录', public: true },
    },
    {
      path: '/',
      component: () => import('@/views/Layout/index.vue'),
      redirect: '/introduction',
      meta: { requiresAuth: true },
      children: [
        {
          path: '/introduction',
          name: 'introduction',
          component: () => import('@/views/Introduction/index.vue'),
          meta: { title: '系统简介', requiresAuth: true },
        },
        {
          path: '/diff-dashboard',
          name: 'diff-dashboard',
          component: () => import('@/views/DiffDashboard/index.vue'),
          meta: { title: 'Diff大盘', requiresAuth: true },
        },
        {
          path: '/migration-task',
          name: 'migration-task',
          component: () => import('@/views/MigrationTask/index.vue'),
          meta: { title: '迁移任务', requiresAuth: true },
        },
        {
          path: '/grayscale-rule',
          name: 'grayscale-rule',
          component: () => import('@/views/GrayscaleRule/index.vue'),
          meta: { title: '灰度规则', requiresAuth: true },
        },
        {
          path: '/diff-rule',
          name: 'diff-rule',
          component: () => import('@/views/DiffRule/index.vue'),
          meta: { title: 'Diff规则', requiresAuth: true },
        },
        {
          path: '/alert-rule',
          name: 'alert-rule',
          component: () => import('@/views/AlertRule/index.vue'),
          meta: { title: '告警规则', requiresAuth: true },
        },
        {
          path: '/alert-template',
          name: 'alert-template',
          component: () => import('@/views/alertTemplate/index.vue'),
          meta: { title: '告警模板', requiresAuth: true },
        },
        {
          path: '/diff-record',
          name: 'diff-record',
          component: () => import('@/views/DiffRecord/index.vue'),
          meta: { title: 'Diff记录', requiresAuth: true },
        },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (to.path === '/login') {
    if (!authStore.isAuthenticated) {
      return true
    }

    const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : '/introduction'
    if (redirect && redirect !== '/login') {
      return redirect
    }
    return '/introduction'
  }

  if (!to.matched.some((item) => item.meta?.requiresAuth)) {
    return true
  }

  if (!authStore.isAuthenticated) {
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    }
  }

  if (!authStore.profileChecked) {
    try {
      await authStore.refreshCurrentUser()
    } catch {
      authStore.clearSession()
      return {
        path: '/login',
        query: { redirect: to.fullPath },
      }
    }
  }

  return true
})

export default router

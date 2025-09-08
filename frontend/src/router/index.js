import { createRouter, createWebHistory } from 'vue-router'
import commonRoutes from './common'
import superAdminRoutes from './superadmin'
import managerRoutes from './manager'
import staffRoutes from './staff'

import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    ...commonRoutes,
    ...superAdminRoutes,
    ...managerRoutes,
    ...staffRoutes,

    // 그 외 없는 모든 경로는 홈("/")으로 리다이렉트
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach(async (to, from, next) => {
  const auth = useAuthStore()

  // 아직 로그인 정보가 없으면 refresh 시도
  if (!auth.accessToken) {
    await auth.initialize()
  }

  console.log('🔎 to.meta.role:', to.meta.requiresAuth)
  console.log('🔎 auth.role:', auth.role)
  console.log('🔎 auth.user:', auth.user)


  if (to.meta.requiresAuth) {
    if (!auth.user) {
      return next('/login')
    }
    if (to.meta.role && to.meta.role !== auth.role) {
      return next('/') // 권한 불일치 → 홈으로
    }
  }
  next()
})

export default router
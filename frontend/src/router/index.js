import { createRouter, createWebHistory } from 'vue-router'
import commonRoutes from './common'
import superAdminRoutes from './superadmin'
import managerRoutes from './manager'
import staffRoutes from './staff'

import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'

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

// 라우터 가드 설정
router.beforeEach(async (to, from, next) => {
  const ui = useUiStore()
  const auth = useAuthStore()

  // 로딩화면 표시
  ui.setLoading(true)

  // 1. accessToken 없으면 initialize 시도
  if (!auth.accessToken) {
    const ok = await auth.initialize()
    if (!ok) {
      ui.setLoading(false)
      if (to.path !== '/login') return next('/login')
      return next()
    }
  }

  // 아직 로그인 정보가 없으면 refresh 시도
  console.log('auth.role : ', auth.role)
  console.log('auth.accessToken : ', auth.accessToken)
  console.log('auth.refreshExp : ', auth.refreshExp)
  console.log('auth.user : ', auth.user)

  // 2. 로그인 상태에서 /login 접근 차단
  if (to.path === '/login' && auth.user) {
    ui.setLoading(false)
    return next('/')
  }

  // 3. 권한이 필요한 페이지 접근 제어
  if (to.meta.requiresAuth) {
    // 3-1. 로그인을 안한 경우
    if (!auth.user) {
      ui.setLoading(false)
      return next('/login')
    }
    // 3-2. 로그인을 했으나 다른 권한인 경우
    if (to.meta.role && to.meta.role !== auth.role) {
      ui.setLoading(false)
      return next('/')
    }
  }

  // 모든 조건 통과 후 로딩 종료
  ui.setLoading(false)
  next()
})

export default router
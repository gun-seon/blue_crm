import { createRouter, createWebHistory } from 'vue-router'
import commonRoutes from './common'
// import superAdminRoutes from './superadmin'
// import managerRoutes from './manager'
// import staffRoutes from './staff'

import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import { resetGlobalFilters } from '@/composables/globalFilters.js'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    ...commonRoutes,
    // ...superAdminRoutes,
    // ...managerRoutes,
    // ...staffRoutes,

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

  // console.log('auth.role : ', auth.role)
  // console.log('auth.accessToken : ', auth.accessToken)
  // console.log('auth.refreshExp : ', auth.refreshExp)
  // console.log('auth.name : ', auth.name)
  // console.log('auth.email : ', auth.email)

  // 1. accessToken 없으면 initialize 시도
  if (!auth.accessToken) {
    const ok = await auth.initialize()
    if (!ok) {
      ui.setLoading(false)

      // requiresAuth(권한검사) 없는 페이지 → 그냥 통과
      if (!to.meta.requiresAuth) {
        return next()
      }

      // requiresAuth(권한검사) 있는 페이지만 로그인 페이지로
      return next('/login')
    }
  }

  // 아직 로그인 정보가 없으면 refresh 시도
  // console.log('auth.role : ', auth.role)
  // console.log('auth.accessToken : ', auth.accessToken)
  // console.log('auth.refreshExp : ', auth.refreshExp)
  // console.log('auth.name : ', auth.name)
  // console.log('auth.email : ', auth.email)

  // 2. 로그인 상태에서 /login 접근 차단
  if (to.path === '/login' && auth.role) {
    ui.setLoading(false)
    return next('/')
  }

  // 3. 권한이 필요한 페이지 접근 제어
  if (to.meta.requiresAuth) {
    // 3-1. 로그인을 안한 경우
    if (!auth.role) {
      ui.setLoading(false)
      return next('/login')
    }
    // 3-2. role 제한 있는 경우
    if (to.meta.role) {
      const allowed = Array.isArray(to.meta.role) ? to.meta.role : [to.meta.role]
      if (!allowed.includes(auth.role)) {
        ui.setLoading(false)
        return next('/login')
      }
    }
  }

  // 모든 조건 통과 후 로딩 종료 전에 전역 필터 초기화
  if (!to.meta?.keepFilters) {
    resetGlobalFilters()
  }

  // 로딩 종료
  ui.setLoading(false)
  next()
})

export default router
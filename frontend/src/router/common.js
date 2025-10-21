export default [
    { path: '/login', component: () => import('@/views/auth/LoginView.vue') },
    { path: '/signup', component: () => import('@/views/auth/SignupView.vue') },
    { path: '/find-password', component: () => import('@/views/auth/FindPassword.vue') },
    // 로그인이 필요한 페이지
    // 대시보드
    {
        path: '/',
        component: () => import('@/views/Dashboard.vue'),
        meta: { requiresAuth: true, role: ['SUPERADMIN','MANAGER','STAFF'] }
    },
    // 내정보 수정
    {
        path: '/profile',
        component: () => import('@/views/Pages/MyInfo.vue'),
        meta: { requiresAuth: true, role: ['SUPERADMIN','MANAGER','STAFF'] }
    },
    // DB분배
    {
        path: '/db/allocate',
        component: () => import('@/views/Pages/Customer/Allocate.vue'),
        meta: { requiresAuth: true, role: ['SUPERADMIN','MANAGER'] }
    },
    // 센터DB
    {
        path: '/db/center',
        component: () => import('@/views/Pages/Customer/Center.vue'),
        meta: { requiresAuth: true, role: ['SUPERADMIN'] }
    },
    // 전체DB
    {
        path: '/db/all',
        component: () => import('@/views/Pages/Customer/All.vue'),
        meta: { requiresAuth: true, role: ['SUPERADMIN','MANAGER','STAFF'] }
    },
    // 중복DB
    {
        path: '/db/duplicate',
        component: () => import('@/views/Pages/Customer/Duplicate.vue'),
        meta: { requiresAuth: true, role: ['SUPERADMIN','MANAGER','STAFF'] }
    },
    // DB회수
    {
        path: '/db/revoke',
        component: () => import('@/views/Pages/Customer/Revoke.vue'),
        meta: { requiresAuth: true, role: ['SUPERADMIN','MANAGER'] }
    },
    // 소속정보
    {
        path: '/info',
        component: () => import('@/views/Pages/Info.vue'),
        meta: { requiresAuth: true, role: ['SUPERADMIN','MANAGER','STAFF'] }
    },
    // 회원관리
    {
        path: '/user',
        component: () => import('@/views/Pages/User.vue'),
        meta: { requiresAuth: true, role: ['SUPERADMIN','MANAGER','STAFF'] }
    },
]
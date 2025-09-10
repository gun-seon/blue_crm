export default [
    // { path: '/', component: () => import('@/views/HomeView.vue') },
    {
        path: '/',
        component: () => import('@/views/Ecommerce.vue'),
        meta: { requiresAuth: true, role: ['SUPERADMIN','MANAGER','STAFF'] }   // 로그인 필요
    },
    { path: '/login', component: () => import('@/views/auth/LoginView.vue') },
    { path: '/signup', component: () => import('@/views/auth/SignupView.vue') }
]
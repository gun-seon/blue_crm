export default [
    { path: '/', component: () => import('@/views/HomeView.vue') },
    { path: '/login', component: () => import('@/views/auth/LoginView.vue') },
    { path: '/signup', component: () => import('@/views/auth/SignupView.vue') }
]
export default [
    {
        path: '/manager',
        name: 'ManagerDashboard',
        component: () => import('@/views/manager/DashboardView.vue'),
        meta: { requiresAuth: true, role: 'MANAGER' }
    }
]
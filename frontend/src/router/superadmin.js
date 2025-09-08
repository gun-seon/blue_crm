export default [
    {
        path: '/super',
        name: 'SuperDashboard',
        component: () => import('@/views/superadmin/DashboardView.vue'),
        meta: { requiresAuth: true, role: 'SUPERADMIN' }
    }
]
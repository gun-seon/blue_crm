export default [
    {
        path: '/staff',
        name: 'StaffDashboard',
        component: () => import('@/views/staff/DashboardView.vue'),
        meta: { requiresAuth: true, role: 'STAFF' }
    }
]

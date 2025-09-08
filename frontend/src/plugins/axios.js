import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

const api = axios.create({
    baseURL: '/api',
    withCredentials: true
})

api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const authStore = useAuthStore()

        if (error.response && error.response.status === 401) {
            const refreshed = await authStore.refreshToken()
            if (refreshed) {
                // 원래 요청을 새 Access Token으로 재시도
                error.config.headers['Authorization'] = `Bearer ${authStore.accessToken}`
                return api.request(error.config)
            }
        }

        return Promise.reject(error)
    }
)

export default api
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

        // 서버 응답이 없거나 status 없음 → 그냥 reject
        if (!error.response) {
            return Promise.reject(error)
        }

        // 1. refresh api 시도 자체가 실패한 경우 (리프레시 토큰이 없어서)
        if (
            error.response.status === 401 &&
            error.config.url.includes('/auth/token/refresh')
        ) {
            // 콘솔에 불필요한 에러 안 뜨게 user 클리어 후 reject
            authStore.clearUser()
            return Promise.reject(error)
        }

        // 2. 다른 API 호출 중 401 발생 → refreshToken 시도
        if (error.response.status === 401) {
            const refreshed = await authStore.refreshToken()
            if (refreshed) {
                // 새 토큰으로 Authorization 갱신 후 재시도
                error.config.headers['Authorization'] = `Bearer ${authStore.accessToken}`
                return api.request(error.config)
            }
        }

        return Promise.reject(error)
    }
)

export default api
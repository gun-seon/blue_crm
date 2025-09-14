import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

const api = axios.create({
    baseURL: '',
    withCredentials: true
})

let refreshPromise = null

api.interceptors.response.use(
    r => r,
    async (err) => {
        const authStore = useAuthStore()

        // refresh 요청 자체는 인터셉터에서 건드리지 않음
        if (err.config.url?.includes('/auth/token/refresh')) {
            return Promise.reject(err)
        }

        if (err.response?.status === 401 && !err.config._retry) {
            if (!refreshPromise) {
                refreshPromise = authStore.refreshToken().finally(() => {
                    refreshPromise = null
                })
            }
            const ok = await refreshPromise
            if (ok) {
                err.config._retry = true
                err.config.headers['Authorization'] = `Bearer ${authStore.accessToken}`
                return api.request(err.config)
            }
        }

        return Promise.reject(err)
    }
)

export default api
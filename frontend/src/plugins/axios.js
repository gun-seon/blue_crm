import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

const api = axios.create({
    baseURL: '',
    withCredentials: true
})

// 요청마다 accessToken 붙여주기
api.interceptors.request.use(
    (config) => {
        const authStore = useAuthStore()
        if (authStore.accessToken) {
            config.headers['Authorization'] = `Bearer ${authStore.accessToken}`
        }
        return config
    },
    (err) => Promise.reject(err)
)

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
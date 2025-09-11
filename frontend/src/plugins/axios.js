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
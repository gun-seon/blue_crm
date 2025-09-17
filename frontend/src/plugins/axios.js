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

        // --- 차단/탈퇴 케이스 감지: X-Blocked 헤더 ---
        const h = err.response?.headers;
        const v = typeof h?.get === 'function' ? h.get('x-blocked') : (h?.['x-blocked'] ?? h?.['X-Blocked']);
        if (String(v).toLowerCase() === 'true') {
            alert("계정 탈퇴 됨")
            await authStore.logout();
            return Promise.reject(err);
        }

        // 과거 코드 (엑세스 토큰 갱신이 안되는 문제가 잇었음)
        // // 403 → 강제로그아웃으로 권한이 없을 때
        // if (err.response?.status === 403) {
        //     console.warn("403 Forbidden 응답:", err.response?.data)
        //     // await authStore.logout()
        //     return Promise.reject(err)
        // }

        // 🔑 401 또는 403 → refresh 시도
        if ((err.response?.status === 401 || err.response?.status === 403) && authStore.accessToken) {
            if (!refreshPromise) {
                refreshPromise = authStore.refreshToken().finally(() => {
                    refreshPromise = null;
                });
            }

            try {
                await refreshPromise;
                // 원래 요청 다시 시도
                err.config.headers["Authorization"] = `Bearer ${authStore.accessToken}`;
                return api(err.config);
            } catch (refreshErr) {
                await authStore.logout();
                return Promise.reject(refreshErr);
            }
        }

        // 과거 코드 (엑세스 토큰 갱신이 안되는 문제가 잇었음)
        // if (err.response?.status === 401 && !err.config._retry) {
        //     if (!refreshPromise) {
        //         refreshPromise = authStore.refreshToken().finally(() => {
        //             refreshPromise = null
        //         })
        //     }
        //     const ok = await refreshPromise
        //     if (ok) {
        //         err.config._retry = true
        //         err.config.headers['Authorization'] = `Bearer ${authStore.accessToken}`
        //         return api.request(err.config)
        //     }
        // }

        return Promise.reject(err)
    }
)

export default api
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
            config.headers = config.headers || {} // 헤더가 undefined 인 경우를 대비하여 최소한의 빈 객체 보장
            config.headers['Authorization'] = `Bearer ${authStore.accessToken}`
            config._hadAuth = true // 이 요청은 토큰을 달고 서버로 갔다
        } else {
            config._hadAuth = false // 이 요청은 토큰없이 서버로 갔다 (로그인 같은 공개 API)
        }
        return config
    },
    (err) => Promise.reject(err)
)

let refreshPromise = null
let blockedShown = false

// 응답
api.interceptors.response.use(
    r => r,
    async (err) => {
        const authStore = useAuthStore()

        // refresh 요청 자체는 인터셉터에서 건드리지 않음
        if (err.config.url?.includes('api/auth/token/refresh')) {
            return Promise.reject(err)
        }

        // --- 차단/탈퇴 케이스 감지: X-Blocked 헤더 ---
        const h = err.response?.headers
        const blocked = typeof h?.get === 'function' ? h.get('x-blocked') : (h?.['x-blocked'] ?? h?.['X-Blocked'])
        if (String(blocked).toLowerCase() === 'true') {
            if (!blockedShown) {
                blockedShown = true
                alert("계정 탈퇴 됨")
                await authStore.logout();
            }
            return Promise.reject(err);
        }

        // 과거 코드 (엑세스 토큰 갱신이 안되는 문제가 잇었음)
        // // 403 → 강제로그아웃으로 권한이 없을 때
        // if (err.response?.status === 403) {
        //     console.warn("403 Forbidden 응답:", err.response?.data)
        //     // await authStore.logout()
        //     return Promise.reject(err)
        // }

        if (err.response?.status === 403) {
            // console.warn("403 Forbidden 응답:", err.response?.data);
            return Promise.reject(err);
        }

        // 401인데 "만료"로 판정되거나, 원요청이 토큰을 달고 갔던 경우에만 리프레시 (서버에서 생성한 커스텀 헤더 응용)
        const expiredHeader = typeof h?.get === 'function' ? h.get('x-token-expired') : (h?.['x-token-expired'] ?? h?.['X-Token-Expired'])
        const shouldRefresh = (expiredHeader === '1') || err.config?._hadAuth === true

        // 🔑 401 또는 403 → refresh 시도 (accessToken 존재 여부와 무관)
        if (err.response?.status === 401 && shouldRefresh) {
            if (!refreshPromise) {
                refreshPromise = authStore.refreshToken().finally(() => {
                    refreshPromise = null;
                });
            }

            try {
                await refreshPromise;
                // 원래 요청 재시도 시 새 토큰으로 Authorization 갱신
                err.config.headers = err.config.headers || {}; // 헤더가 undefined 인 경우를 대비하여 최소한의 빈 객체 보장
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

        // 나머지는 그대로 에러
        return Promise.reject(err)
    }
)

export default api
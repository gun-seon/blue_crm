import { defineStore } from 'pinia'
import axios from '@/plugins/axios.js'
import router from "@/router/index.js";

export const useAuthStore = defineStore('auth', {
    state: () => ({
        accessToken: null, // 현재 활성화된 액세스 토큰
        refreshExp: null, // 서버에서 계산해온 refresh 토큰 만료시간
        email: null, // 사용자 email
        name: null, // 사용자 name
        role: null, // 사용자 권한
        isSuper: false, // 특별계정 여부 (DB users.is_super)
    }),
    actions: {
        async pingPong() {
            // 수동 새로고침 (sid 기본값 1)
            const { data } = await axios.post('/api/sheets/refresh?sid=1')
            return data
        },

        // 사용자 정보 설정
        setAuth(data) {
            // data.accessToken 없으면 바로 종료
            // (ex. 로그인 두번 연속 등 사고로 인해 data에 손상이 있을 경우 대물림을 막기 위함)
            if (!data || !data.accessToken) {
                console.warn('refresh 응답에 accessToken 없음:', data)
                return
            }

            this.accessToken = data.accessToken
            this.email = data.email
            this.name = data.name
            this.role = data.role
            this.isSuper = data.isSuper
            this.refreshExp = data.refreshExp
            // axios.defaults.headers.common['Authorization'] = `Bearer ${this.accessToken}`
        },

        // 사용자 정보 clear
        clearUser() {
            this.email = null
            this.name = null
            this.role = null
            this.accessToken = null
            this.refreshExp = null
            this.isSuper = false
            delete axios.defaults.headers.common['Authorization']
        },

        // 이메일 중복 확인
        async checkEmailDuplicate(email) {
            try {
                const res = await axios.get('/api/auth/check-email', {
                    params: { email }
                })

                // 서버에서 true/false 리턴
                return res.data
            } catch (err) {
                console.error('이메일 중복 확인 실패', err)
                throw err
            }
        },

        // 회원가입
        async signup(payload) {
            try {
                const res = await axios.post('/api/auth/signup', payload, { withCredentials: true })

                // 회원가입 성공 메세지 출력
                alert(res.data.message)
                await router.push('/login')
            } catch (err) {
                const msg = err.response?.data || err.message
                alert(msg)
                throw err
            }
        },

        // 로그인
        async login(payload) {
            try {
                // 서버 응답: { accessToken, refreshToken: null, role, email, name, Exp }
                const { data } = await axios.post('/api/auth/login', payload, { withCredentials: true })

                // 사용자 정보 초기화
                this.setAuth(data)
            } catch (err) {
                const msg = err.response?.data || '로그인 중 오류가 발생했습니다.'
                alert(msg)

                await this.logout()
                throw err
            }
        },

        // 로그아웃
        async logout() {
            try {
                await axios.post('/api/auth/token/logout', {}, { withCredentials: true })
            } catch (err) {
                console.warn('로그아웃 요청 실패:', err)
            } finally {
                this.clearUser()
                await router.push('/login')
            }
        },

        // Access Token 갱신
        async refreshToken() {
            try {
                // Refresh Token은 쿠키에서 자동 전송됨
                const { data } = await axios.post('/api/auth/token/refresh', {}, { withCredentials: true })

                // 사용자 정보 초기화
                this.setAuth(data)

                return true
            } catch (err) {
                const msg = err.response?.data || 'Access Token : 세션 연장에 실패했습니다. 다시 로그인 해주세요.'
                console.error(msg, err.response?.data || err)

                await this.logout()
                return false
            }
        },

        // 리프레시 토큰 수동 갱신
        async extendSession() {
            try {
                // 1. 액세스 토큰 없으면 먼저 refresh 시도
                const ok = await this.refreshToken()
                if (!ok) return false

                // 2. refresh 이후 extend 요청
                // Refresh Token은 쿠키에서 자동 전송됨
                const {data} = await axios.post('/api/auth/token/extend', {}, {withCredentials: true})

                // 사용자 정보 초기화
                this.setAuth(data)

                return true
            } catch (err) {
                const msg = err.response?.data || 'Refresh Token : 세션 연장에 실패했습니다. 다시 로그인 해주세요.'
                console.error(msg, err.response?.data || err)

                await this.logout()
                return false
            }
        },

        // 앱 시작 시 호출
        async initialize() {
            // 1. 아직 리프레시 토큰이 살아있지만
            // 2. 엑세스 토큰이 만료되었다면
            if (!this.accessToken) {
                try {
                    // 1. 쿠키 체크
                    try {
                        const res = await axios.post('/api/auth/token/check', {}, { withCredentials: true });
                        if (res.status === 204) return false;      // 쿠키 없음
                    } catch {
                        return false;
                    }

                    // 2. 쿠키가 있으니 refresh 시도
                    // Refresh Token은 쿠키에서 자동 전송됨
                    const { data } = await axios.post('/api/auth/token/refresh', {}, { withCredentials: true })

                    // 사용자 정보 초기화
                    this.setAuth(data)

                    return true
                } catch (err) {
                    await this.logout()
                    return false
                }
            }

            // 그 외 정상 로그인 상태 유지
            return true
        }
    }
})

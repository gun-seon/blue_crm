// src/stores/auth.js
import { defineStore } from 'pinia'
import axios from 'axios'

export const useAuthStore = defineStore('auth', {
    state: () => ({
        accessToken: null, // 현재 활성화된 액세스 토큰
        refreshExp: null, // 서버에서 계산해온 refresh 토큰 만료시간
        user: null, // 사용자 정보 { email, role }
        role: null // 사용자 권한
    }),
    actions: {
        // 사용자 정보 세팅
        setUser(user) {
            // 사용자 정보만 설정 (인증수단인 엑세스 토큰은 설정하지 않음)
            this.user = user
            this.role = user?.role || null
        },

        // 사용자 정보 초기화
        clearUser() {
            this.user = null
            this.role = null
            this.accessToken = null
            delete axios.defaults.headers.common['Authorization']
        },

        // 회원가입
        async signup(payload) {
            await axios.post('/api/auth/signup', payload, { withCredentials: true })
        },

        // 로그인
        async login(payload) {
            // 서버 응답: { accessToken, refreshToken: null, role, email }
            const { data } = await axios.post('/api/auth/login', payload, { withCredentials: true })

            // Access Token만 저장
            this.accessToken = data.accessToken

            // 사용자 정보 구성
            this.user = data.email
            this.role = data.role
            this.refreshExp = data.refreshExp

            // Axios 기본 Authorization 헤더에 Access Token 세팅
            axios.defaults.headers.common['Authorization'] = `Bearer ${this.accessToken}`
        },

        // 로그아웃
        logout() {
            // 서버에서 내려온 에러 메시지 읽기
            const msg = err.response?.data?.error || '로그인 중 오류가 발생했습니다.'
            alert(msg)
            this.clearUser()
        },

        // Access Token 갱신
        async refreshToken() {
            try {
                // Refresh Token은 쿠키에서 자동 전송됨
                const { data } = await axios.post('/api/auth/token/refresh', {}, { withCredentials: true })

                // 새 Access Token 갱신
                this.accessToken = data.accessToken
                this.user = data.email
                this.role = data.role
                this.refreshExp = data.refreshExp

                // Axios Authorization 헤더 갱신
                axios.defaults.headers.common['Authorization'] = `Bearer ${this.accessToken}`

                return true
            } catch (err) {
                this.clearUser()
                return false
            }
        },

        // 리프레시 토큰 수동 갱신
        async extendSession() {
            // Refresh Token은 쿠키에서 자동 전송됨
            const { data } = await axios.post('/api/auth/token/extend', {}, { withCredentials: true })

            // 새 Access Token 갱신
            this.accessToken = data.accessToken
            this.user = data.email
            this.role = data.role
            this.refreshExp = data.refreshExp

            // Axios Authorization 헤더 갱신
            axios.defaults.headers.common['Authorization'] = `Bearer ${this.accessToken}`
        },

        // 앱 시작 시 호출
        async initialize() {
            if (!this.accessToken) {
                try {
                    // Refresh Token은 쿠키에서 자동 전송됨
                    const { data } = await axios.post('/api/auth/token/refresh', {}, { withCredentials: true })

                    // 새 Access Token 갱신
                    this.accessToken = data.accessToken
                    this.user = data.email
                    this.role = data.role
                    this.refreshExp = data.refreshExp

                    // Axios Authorization 헤더 갱신
                    axios.defaults.headers.common['Authorization'] = `Bearer ${this.accessToken}`
                    return true
                } catch (err) {
                    this.clearUser()
                    return false
                }
            }
        }
    }
})

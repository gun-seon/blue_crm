import { defineStore } from 'pinia'
import axios from '@/plugins/axios.js'

export const useMailStore = defineStore('mail', {
    state: () => ({
        email: '', // 인증대상
        ttl: 0, // 인증시간
        codeVerified: false, // 인증여부
    }),
    actions: {
        // 인증번호 최초 발급
        async sendCode(email) {
            try {
                const { data } = await axios.post('/api/mail/send-code',
                    { email },
                    { withCredentials: true })
                this.email = email
                this.ttl = data.ttl
                this.codeVerified = false
                this.isExtended = false
                return data
            } catch (err) {
                const msg = err.response?.data || err.message
                alert(msg)
                return null
            }
        },

        // 인증번호 검증
        async verifyCode(code) {
            if (!this.email) throw new Error('이메일이 설정되지 않았습니다.')
            try {
                await axios.post('/api/mail/verify-code', {
                    email: this.email,
                    code
                }, { withCredentials: true })
                this.codeVerified = true
                return true
            } catch (err) {
                const msg = err.response?.data || err.message
                alert(msg)
                this.codeVerified = false
            }
        },

        // 시간 연장 (1회 한정)
        async extendCode() {
            if (!this.email) throw new Error('이메일이 설정되지 않았습니다.')
            try {
                const { data } = await axios.post('/api/mail/extend-code',
                    { email: this.email },
                    { withCredentials: true })
                this.ttl = data.ttl
                return data
            } catch (err) {
                const msg = err.response?.data || err.message
                alert(msg)
                return null
            }
        },

        // TTL 조회 (새로고침 시 동기화)
        async getCodeTtl() {
            if (!this.email) return 0
            try {
                const { data } = await axios.get('/api/mail/code-ttl', {
                    params: { email: this.email },
                    withCredentials: true
                })
                this.ttl = data.ttl
                return data.ttl
            } catch (err) {
                const msg = err.response?.data || err.message
                alert(msg)
                this.ttl = 0
                return 0
            }
        }
    }
})

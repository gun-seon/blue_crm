<script setup>
import {onUnmounted, ref} from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useMailStore } from '@/stores/mail'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const mail = useMailStore()
const router = useRouter()

// 이메일 인증 관련
const code = ref('') // 인증코드 입력 필드
const codeSent = ref(false) // 인증코드 전송 여부
const verified = ref(false) // 인증 성공 여부
const ttl = ref(0) // 남은 시간
let timer = null

// 그 외 회원가입용 필드
const email = ref('')
const password = ref('')
const role = ref('STAFF') // 디폴트로 STAFF 설정

// 1. 인증코드 전송
const sendCode = async () => {
  const { ttl: newTtl } = await mail.sendCode(email.value)
  if (!newTtl) return

  ttl.value = newTtl
  codeSent.value = true
  verified.value = false
  startCountdown()
  alert('인증코드가 이메일로 전송되었습니다 (3분 이내 입력)')
}

// 2. 인증코드 확인
const verifyCode = async () => {
  mail.email = email.value // mail store에 이메일 싱크
  await mail.verifyCode(code.value)
  if (mail.codeVerified) {
    verified.value = true
    stopCountdown()
    alert('이메일 인증 성공')
  }
}

// 3. 인증 시간 연장 (1회만 가능)
const extendTime = async () => {
  const { ttl: newTtl } = await mail.extendCode()
  if (!newTtl) return

  ttl.value = newTtl
  startCountdown()
  alert('인증 시간이 연장되었습니다.')
}

// 카운트다운 관리
const startCountdown = () => {
  clearInterval(timer)
  timer = setInterval(() => {
    if (ttl.value > 0) {
      ttl.value--
    } else {
      clearInterval(timer)
    }
  }, 1000)
}
const stopCountdown = () => clearInterval(timer)

onUnmounted(() => stopCountdown())

// 4. 최종 회원가입
const doSignup = async () => {
  // 예외처리
  if (!verified.value) {
    alert('이메일 인증을 먼저 완료해주세요')
    return
  }

  try {
    await auth.signup({
      email: email.value,
      password: password.value,
      role: role.value
    })
    alert('회원가입 성공')
    await router.push('/login')
  } catch (err) {
    console.warn('회원가입 실패')
  }
}
</script>

<template>
  <div style="padding:20px">
    <h1>회원가입</h1>
    <input v-model="email" placeholder="Email" />
    <button @click="sendCode">인증코드 전송</button>

    <div v-if="codeSent && !verified">
      <input v-model="code" placeholder="인증코드 입력" />
      <button @click="verifyCode">코드 확인</button>

      <!-- 남은 시간 표시 -->
      <div v-if="ttl > 0">
        <p>남은 인증 시간: {{ ttl }}초</p>
        <button v-if="!mail.isExtended" @click="extendTime">시간 연장 (1회)</button>
      </div>

      <!-- 인증 실패/시간 만료 -->
      <div v-else>
        <p>인증 시간이 만료되었거나 실패했습니다.</p>
        <button @click="sendCode">다시 인증하기</button>
      </div>
    </div>

    <!-- 비밀번호/권한 입력 -->
    <input v-model="password" type="password" placeholder="Password" />
    <select v-model="role">
      <option value="SUPERADMIN">SUPERADMIN</option>
      <option value="MANAGER">MANAGER</option>
      <option value="STAFF">STAFF</option>
    </select>

    <!-- 회원가입 버튼: 인증 성공해야만 활성화 -->
    <button @click="doSignup">회원가입</button>
  </div>
</template>

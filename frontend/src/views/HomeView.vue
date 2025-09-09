<script setup>
  import { ref, onMounted } from 'vue'
  import { useRouter } from 'vue-router'

  const pong = ref('loading...')
  const router = useRouter()

  onMounted(async () => {
    try {
      const res = await fetch('/api/ping') // 프록시 → 백엔드
      const data = await res.json()
      pong.value = JSON.stringify(data)
    } catch (e) {
      pong.value = 'error: ' + e
    }
  })

  // 버튼 클릭 시 라우팅
  // 로그인
  function goLogin() {
    router.push('/login')
  }

  // 회원가입
  function goSignup() {
    router.push('/signup')
  }
</script>

<template>
  <main style="padding: 24px">
    <h1>프론트 ↔ 백 연결 테스트</h1>
    <p>/api/ping → {{ pong }}</p>

    <div style="margin-top: 20px">
      <button @click="goLogin">로그인</button>
      <button @click="goSignup" style="margin-left: 10px">회원가입</button>
    </div>
  </main>
</template>

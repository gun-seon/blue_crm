<script setup>
  import { ref, onMounted } from 'vue'

  const pong = ref('loading...')

  onMounted(async () => {
    try {
      const res = await fetch('/api/ping') // 프록시 → 백엔드
      const data = await res.json()
      pong.value = JSON.stringify(data)
    } catch (e) {
      pong.value = 'error: ' + e
    }
  })

</script>

<template>
  <main style="padding: 24px">
    <h1>프론트 ↔ 백 연결 테스트</h1>
    <p>/api/ping → {{ pong }}</p>
  </main>
</template>

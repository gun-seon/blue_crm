<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import router from "@/router/index.js";

const auth = useAuthStore()
const timeLeft = ref(0)
let timer

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
})

onUnmounted(() => clearInterval(timer))

async function updateTime() {
  if (auth.refreshExp) {
    const diff = Math.floor((auth.refreshExp - Date.now()) / 1000)
    timeLeft.value = diff > 0 ? diff : 0

    if (timeLeft.value === 0) {
      await auth.logout()
      await router.push('/login')
    }
  }
}

async function extendSession() {
  await auth.extendSession()
  await updateTime()
}
</script>

<template>
  <div style="padding:20px">
    <h1>SUPERADMIN Dashboard</h1>
    <p>시스템 전체를 관리할 수 있습니다.</p>
  </div>

  <div v-if="timeLeft > 0" class="session-timer">
    남은 시간: {{ Math.floor(timeLeft / 60) }}분 {{ timeLeft % 60 }}초
    <button @click="extendSession">로그인 연장</button>
  </div>
</template>

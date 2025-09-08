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

// 로그아웃
async function doLogout() {
  await auth.logout()
  await router.push('/login')
}
</script>

<template>
  <div style="padding:20px">
    <h1>MANAGER Dashboard</h1>
    <p>자신이 속한 센터의 데이터만 관리할 수 있습니다.</p>
  </div>

  <div v-if="timeLeft > 0" class="session-timer">
    남은 시간: {{ Math.floor(timeLeft / 60) }}분 {{ timeLeft % 60 }}초
    <button @click="extendSession">로그인 연장</button>
    <button @click="doLogout" style="margin-left:10px">로그아웃</button>
  </div>
</template>

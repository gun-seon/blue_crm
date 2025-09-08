<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const email = ref('')
const password = ref('')
const auth = useAuthStore()
const router = useRouter()

const doLogin = async () => {
  try {
    await auth.login({ email: email.value, password: password.value })
    if (auth.role === 'SUPERADMIN') await router.push('/super')
    else if (auth.role === 'MANAGER') await router.push('/manager')
    else if (auth.role === 'STAFF') await router.push('/staff')
  } catch {
    alert('로그인 실패')
  }
}
</script>

<template>
  <div style="padding:20px">
    <h1>로그인</h1>
    <form @submit.prevent="doLogin">
      <input type="text" placeholder="Email" v-model="email" />
      <input type="password" placeholder="Password" v-model="password" />
      <button type="submit">로그인</button>
    </form>
  </div>
</template>

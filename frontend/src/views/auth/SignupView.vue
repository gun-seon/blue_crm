<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const email = ref('')
const password = ref('')
const role = ref('STAFF')
const auth = useAuthStore()
const router = useRouter()

const doSignup = async () => {
  try {
    await auth.signup({ email: email.value, password: password.value, role: role.value })
    alert('회원가입 성공')
    await router.push('/login')
  } catch {
    alert('회원가입 실패')
  }
}
</script>

<template>
  <div style="padding:20px">
    <h1>회원가입</h1>
    <input v-model="email" placeholder="Email" />
    <input v-model="password" type="password" placeholder="Password" />
    <select v-model="role">
      <option value="SUPERADMIN">SUPERADMIN</option>
      <option value="MANAGER">MANAGER</option>
      <option value="STAFF">STAFF</option>
    </select>
    <button @click="doSignup">회원가입</button>
  </div>
</template>

<script setup>
import { RouterLink, RouterView } from 'vue-router'
import { useUiStore } from '@/stores/ui'

const ui = useUiStore()
</script>

<template>
  <header>
    <div class="wrapper">
      <nav>
        <!-- 네비게이션 메뉴 -->
      </nav>
    </div>
  </header>

  <!-- 메인 화면 -->
  <main>
    <div v-if="ui.loading"> </div>
    <RouterView v-else />
  </main>

</template>

<style scoped>
header {
  line-height: 1.5;
  max-height: 100vh;
}

nav {
  width: 100%;
  font-size: 12px;
  text-align: center;
  margin-top: 2rem;
}
</style>

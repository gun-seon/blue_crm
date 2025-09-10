<script setup>
import { RouterView } from 'vue-router'
import { useUiStore } from '@/stores/ui'
import ThemeProvider from './components/layout/ThemeProvider.vue'
import SidebarProvider from './components/layout/SidebarProvider.vue'

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
  <main class="bg-gray-50 dark:bg-gray-900">
    <ThemeProvider>
      <SidebarProvider>
        <div v-if="ui.loading"> </div>
        <RouterView v-else />
      </SidebarProvider>
    </ThemeProvider>
  </main>

</template>

<style scoped>
</style>

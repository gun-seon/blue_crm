<template>
  <div class="fixed inset-0 z-[100000] flex items-center justify-center bg-black/40 p-4"
       @click.self="$emit('close')"
       @keydown.esc.prevent.stop="$emit('close')">
    <div class="w-full max-w-[700px] rounded-2xl bg-white dark:bg-gray-900 shadow-xl border border-gray-200 dark:border-gray-800">
      <!-- 헤더 -->
      <div class="px-5 py-4 border-b border-gray-200 dark:border-gray-800 flex items-center justify-between">
        <h3 class="text-base font-semibold text-gray-800 dark:text-gray-100">DB 일괄 분배</h3>
        <button class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300" @click="$emit('close')">✕</button>
      </div>

      <!-- 바디 -->
      <div class="px-5 py-4 space-y-4">
        <!-- 선택 개수 -->
        <p class="text-sm text-gray-500 dark:text-gray-400">선택된 고객: <b>{{ selectedCount }}</b>명</p>

        <!-- HQ 전용: 센터 선택 -->
        <div v-if="mode==='HQ'" class="space-y-1">
          <label class="block text-sm text-gray-600 dark:text-gray-300">센터 선택</label>
          <select v-model.number="centerId"
                  class="w-full h-11 border border-gray-200 dark:border-gray-700 rounded-lg px-3 py-2 text-sm dark:bg-gray-800 dark:text-gray-200">
            <option :value="null">센터를 선택하세요</option>
            <option v-for="c in centers" :key="c.centerId" :value="c.centerId">{{ c.centerName }}</option>
          </select>
        </div>

        <!-- 직원 검색 (선택) -->
        <div class="space-y-2">
          <label class="block text-sm text-gray-600 dark:text-gray-300">직원 검색 (선택)</label>
          <div class="flex gap-2">
            <input v-model.trim="query" @input="debouncedSearch" type="text" placeholder="이름으로 검색"
                   class="flex-1 h-11 border border-gray-200 dark:border-gray-700 rounded-lg px-3 py-2 text-sm dark:bg-gray-800 dark:text-gray-200"/>
            <button @click="searchUsers" class="h-11 px-4 rounded-lg border border-gray-200 dark:border-gray-700 text-sm hover:bg-gray-50 dark:hover:bg-gray-800">검색</button>
          </div>
          <div class="max-h-52 overflow-auto border border-gray-100 dark:border-gray-800 rounded-md" v-if="users.length">
            <ul>
              <li v-for="u in users" :key="u.userId" class="flex items-center justify-between px-3 py-2 hover:bg-gray-50 dark:hover:bg-gray-800">
                <div class="text-sm text-gray-700 dark:text-gray-200">
                  <b>{{ u.userName }}</b>
                  <span class="text-gray-400 ml-2">{{ u.centerName }}</span>
                </div>
                <button class="px-2 py-1 text-xs rounded-md border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700"
                        @click="selectUser(u)">선택</button>
              </li>
            </ul>
          </div>
          <p v-if="pickedUser" class="text-xs text-gray-500 dark:text-gray-400">선택됨: <b>{{ pickedUser.userName }}</b></p>
        </div>
      </div>

      <!-- 푸터 -->
      <div class="px-5 py-3 border-t border-gray-200 dark:border-gray-800 flex items-center justify-end gap-2">
        <button class="h-10 px-4 rounded-lg border border-gray-200 dark:border-gray-700 text-sm hover:bg-gray-50 dark:hover:bg-gray-800" @click="$emit('close')">취소</button>
        <button class="h-10 px-4 rounded-lg bg-blue-600 text-white text-sm disabled:opacity-60"
                :disabled="submitting || (mode==='HQ' && !centerId)"
                @click="confirm">
          {{ submitting ? '처리 중...' : '분배하기' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import axios from '@/plugins/axios'

const props = defineProps<{ mode: 'HQ'|'MANAGER', centerId?: number|null, selectedCount: number }>()
const emit = defineEmits<{(e:'close'):void,(e:'confirm', payload:{ centerId?:number|null, userId?:number|null }):void}>()

const centers = ref<{centerId:number, centerName:string}[]>([])
const centerId = ref<number|null>(props.centerId ?? null)
const users = ref<any[]>([])
const query = ref('')
const pickedUser = ref<any|null>(null)
const submitting = ref(false)

async function loadCenters(){
  if (props.mode !== 'HQ') return
  try {
    const { data } = await axios.get('/api/common/centers')
    centers.value = data || []
  } catch (e) { console.error(e) }
}

async function searchUsers(){
  try {
    const params:any = {}
    if (props.mode === 'HQ') params.centerId = centerId.value || undefined
    if (props.mode === 'MANAGER') params.centerId = props.centerId || undefined
    if (query.value) params.q = query.value
    const { data } = await axios.get('/api/work/allocate/users', { params })
    users.value = data || []
  } catch (e) { console.error(e) }
}

let t:any=null
function debouncedSearch(){ clearTimeout(t); t=setTimeout(searchUsers, 250) }

function selectUser(u:any){ pickedUser.value = u }

async function confirm(){
  submitting.value = true
  try {
    emit('confirm', { centerId: centerId.value ?? undefined, userId: pickedUser.value?.userId ?? undefined })
  } finally { submitting.value = false }
}

// 모달 모드 변경 시
watch(
    () => props.mode,
    async (m) => {
      await loadCenters()
      users.value = []
      pickedUser.value = null
      // 센터장 모드는 모달 열리자마자 빈 검색으로 호출
      if (m === 'MANAGER') {
        searchUsers()
      }
    },
    { immediate: true }
)

// HQ에서 센터 선택이 바뀌면, 검색어가 비어 있어도 즉시 호출
watch(centerId, () => {
  users.value = []
  pickedUser.value = null
  searchUsers() // 조건없이 바로 호출
})
</script>
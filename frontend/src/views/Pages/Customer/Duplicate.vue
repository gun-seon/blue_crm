<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="중복DB 확인하기" />
    <div class="grid grid-cols-12 gap-4 min-w-0">
      <div class="col-span-12 space-y-6 min-w-0">
        <ComponentCard
            :showRefresh="true"
            :refreshing="isRefreshing"
            @refresh="onRefresh"
            @changeSize="setSize"
        >
          <PsnsTable
              :columns="columns"
              :data="items"
              :showCheckbox="true"
              :rowSelectable="() => false"
              :page="page"
              :totalPages="totalPages"
              @changePage="changePage"
          />
        </ComponentCard>
      </div>
    </div>
  </AdminLayout>

  <!-- 전역 로딩 오버레이 (메모 모달과 동일하게 body로 텔레포트) -->
  <Teleport to="body">
    <transition name="fade">
      <div
          v-if="showTableSpinner"
          class="fixed inset-0 z-[2147483646]"
          aria-live="polite" aria-busy="true" role="status"
      >
        <!-- 배경 -->
        <div class="absolute inset-0 bg-black/5 dark:bg-black/60"></div>

        <!-- 스피너 -->
        <div class="absolute inset-0 z-[2147483647] flex items-center justify-center">
          <div class="flex flex-col items-center gap-3">
            <svg class="animate-spin h-8 w-8 text-blue-500" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10"
                      stroke="currentColor" stroke-width="4" fill="none"/>
              <path class="opacity-75" fill="currentColor"
                    d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"/>
            </svg>
            <p class="text-sm text-gray-900 dark:text-white/90">불러오는 중…</p>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>

</template>

<script setup>
import {computed, onUnmounted, ref, watch} from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import ComponentCard from '@/components/common/ComponentCard.vue'
import PsnsTable from '@/components/tables/basic-tables/PsnsTable.vue'
import { useTableQuery } from '@/composables/useTableQuery.js'
import { globalFilters } from '@/composables/globalFilters.js'
import axios from '@/plugins/axios.js'

/**
 * 중복DB 전용 컬럼
 * - 전체DB와 동일 구성
 * - 메모는 아이콘/모달 없이 텍스트 그대로 노출
 * - 배지/예약은 편집 불가(컬럼에 editable 안 줌 → PsnsTable에서 클릭 안 됨)
 */
const columns = [
  { key: 'createdAt',  label: 'DB생성일', type: 'text' },
  { key: 'staff',      label: '담당자',   type: 'text' },
  { key: 'division',   label: '구분',     type: 'badge',  options: ['최초','중복','유효'] },
  { key: "",  label: "",   type: "text", ellipsis: { width: 5 } },
  // { key: 'category',   label: '카테고리', type: 'badge',  options: ['주식','코인'] },
  { key: 'name',       label: '이름',     type: 'text' },
  { key: 'phone',      label: '전화번호', type: 'text', ellipsis: { width: 150 } },
  { key: 'source',     label: 'DB출처',   type: 'text' },
  { key: 'content',    label: '내용',     type: 'text' },
  { key: 'memo',       label: '메모',     type: 'text', ellipsis: { width: 150 } },
  { key: 'status',     label: '상태',     type: 'badge',  options: ['없음'] },
]

// 테이블 데이터 훅 (공용 필터/페이지네이션 동일)
const {
  items,
  page,
  size,
  totalPages,
  loading: tableLoading,
  fetchData,
  changePage,
  setSize
} = useTableQuery({
  url: '/api/work/duplicate',        // STAFF도 접근 가능 (SecurityConfig의 /api/work/**)
  externalFilters: globalFilters,     // 날짜/카테고리/키워드 공용
  useExternalKeys: {
    from: 'dateFrom',
    to: 'dateTo',
    category: 'category',
    keyword: 'keyword'
  },
  mapper: (res) => ({
    items: res.data.items,
    totalPages: res.data.totalPages,
    totalCount: res.data.totalCount
  })
})

// 로딩 오버레이 설정
const isRefreshing = ref(false)
const uiLoading = ref(false)
const busy = computed(() => tableLoading.value || isRefreshing.value || uiLoading.value)
const showTableSpinner = ref(false)
let delayTimer = null

async function runBusy(task) {
  if (uiLoading.value) return
  uiLoading.value = true
  try { await task() } finally { uiLoading.value = false }
}

watch(busy, (v) => {
  if (v) {
    // 짧은 로딩은 스피너 숨김
    delayTimer = setTimeout(() => { showTableSpinner.value = true }, 200)
  } else {
    if (delayTimer) { clearTimeout(delayTimer); delayTimer = null }
    showTableSpinner.value = false
  }
})

onUnmounted(() => {
  if (delayTimer) { clearTimeout(delayTimer); delayTimer = null }
})

async function onRefresh() {
  if (isRefreshing.value) return
  isRefreshing.value = true
  try {
    await axios.post('/api/sheets/refresh?sid=1')
    await fetchData()
  } catch (e) {
    console.error(e)
    alert('새로고침 중 오류가 발생했습니다.')
  } finally {
    isRefreshing.value = false
  }
}
</script>

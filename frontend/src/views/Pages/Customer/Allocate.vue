<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="pageTitle" />
    <div class="grid grid-cols-12 gap-4 min-w-0">
      <div class="col-span-12 space-y-6 min-w-0">

        <!-- SUPERADMIN (본사) -->
        <ComponentCard
            v-if="role === 'SUPERADMIN'"
            :selects="[[ '전체','최초','유효' ]]"
            :buttons="['분배하기']"
            :showRefresh="true"
            :refreshing="isRefreshing"
            @refresh="onRefresh"
            @changeSize="setSize"
            @selectChange="onDivisionSelect"
            @buttonClick="onHqButton"
        >
          <PsnsTable
              ref="tableRef"
              :columns="hqColumns"
              :data="items"
              :showCheckbox="true"
              :page="page"
              :totalPages="totalPages"
              @rowSelect="onRowSelect"
              @changePage="changePage"
          />
        </ComponentCard>

        <!-- MANAGER (센터장) -->
        <ComponentCard
            v-else-if="role === 'MANAGER'"
            :buttons="['분배하기']"
            :showRefresh="true"
            :refreshing="isRefreshing"
            @refresh="onRefresh"
            @changeSize="setSize"
            @buttonClick="onMgrButton"
        >
          <PsnsTable
              ref="tableRef"
              :columns="mgrColumns"
              :data="items"
              :showCheckbox="true"
              :page="page"
              :totalPages="totalPages"
              @rowSelect="onRowSelect"
              @changePage="changePage"
          />
        </ComponentCard>

        <!--        &lt;!&ndash; STAFF 접근 X: 보안상 라우팅에서 막히겠지만 방어적으로 안내 &ndash;&gt;-->
        <!--        <div v-else class="p-6 rounded-xl border border-amber-200 bg-amber-50 text-amber-800 dark:border-amber-900/60 dark:bg-amber-950/30 dark:text-amber-200">-->
        <!--          이 페이지는 본사 또는 센터장만 사용할 수 있습니다.-->
        <!--        </div>-->

        <AllocateModal
            v-if="modal.open"
            :mode="modal.mode"
            :selected-count="selectedRows.length"
            @close="closeModal"
            @confirm="onConfirmAllocate"
        />

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

<script setup lang="ts">
import {computed, onUnmounted, ref, watch} from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import ComponentCard from '@/components/common/ComponentCard.vue'
import PsnsTable from '@/components/tables/basic-tables/PsnsTable.vue'
import AllocateModal from '@/components/ui/AllocateModal.vue'
import { useAuthStore } from '@/stores/auth'
import { useTableQuery } from '@/composables/useTableQuery'
import { globalFilters } from '@/composables/globalFilters'
import axios from '@/plugins/axios'

const auth = useAuthStore()
const role = auth.role
const pageTitle = ref('DB 분배하기')

// ===== 테이블 & 페이징 =====
const { items, page, totalPages, fetchData, changePage, setSize, setFilter, loading: tableLoading } = useTableQuery({
  url: '/api/work/allocate/list',
  externalFilters: globalFilters,
  useExternalKeys: { from: 'dateFrom', to: 'dateTo', category: 'category', keyword: 'keyword' },
  mapper: (res:any) => ({ items: res.data.items, totalPages: res.data.totalPages, totalCount: res.data.totalCount })
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

const tableRef = ref<any>(null)
const selectedRows = ref<any[]>([])
const onRowSelect = (rows:any[]) => { selectedRows.value = rows }

// ===== 컬럼 (읽기 전용!) =====
const baseCols = [
  { key: 'createdAt', label: 'DB생성일', type: 'text' },
  // { key: 'category',  label: '카테고리', type: 'badge', options: ['주식','코인'] },
  { key: 'name',      label: '이름',     type: 'text' },
  { key: 'phone',     label: '전화번호', type: 'text', ellipsis: { width: 150 } },
  { key: 'source',    label: 'DB출처',   type: 'text' },
  { key: 'content',   label: '내용',     type: 'text', ellipsis: { width: 150 } },
]

// 본사(HQ)만 담당자 이력 + 구분 노출
const hqColumns = [
  { key: 'createdAt', label: 'DB생성일', type: 'text' },
  { key: 'division',  label: '구분',     type: 'badge', options: ['최초','유효'] },
  // { key: 'category',  label: '카테고리', type: 'badge', options: ['주식','코인'] },
  { key: 'name',      label: '이름',     type: 'text' },
  { key: 'phone',     label: '전화번호', type: 'text', ellipsis: { width: 150 } },
  { key: 'source',    label: 'DB출처',   type: 'text' },
  { key: 'content',   label: '내용',     type: 'text', ellipsis: { width: 150 } },
  { key: "status", label: "상태", type: "badge",
    // 회수와 신규 상태는 수동으로 줄 수 없음
    // 회수 : DB회수하기 메뉴에서
    // 신규 : 한번도 분배가 되지 않은 항목만
    options: ["부재1","부재2","부재3","부재4","부재5","재콜","가망","자연풀","카피","거절"] },
  { key: 'paststaff',     label: '담당자 이력', type: 'text', ellipsis: { width: 200 } }, // HQ 전용
]

// 매니저는 담당자 이력/구분 X (분배만)
const mgrColumns = [ ...baseCols ]

// ===== HQ 전용 구분 필터 =====
function onDivisionSelect({ value }:{ value:string }){
  return runBusy(async () => {
    setFilter('division', value === '전체' ? null : value)
    await fetchData()
  })
}

function needSelection(): number[] {
  const ids = selectedRows.value.map((r:any) => r.id)
  if (!ids.length) alert('분배할 고객을 선택해주세요.')
  return ids
}

// ===== 모달 열기/닫기 =====
const modal = ref<{ open:boolean, mode:'HQ'|'MANAGER' }>({ open:false, mode: 'HQ' })
function onHqButton(btn:string){ if (btn==='분배하기'){ if (!needSelection().length) return; modal.value={open:true, mode:'HQ'} } }
function onMgrButton(btn:string){ if (btn==='분배하기'){ if (!needSelection().length) return; modal.value={open:true, mode:'MANAGER'} } }
function closeModal(){ modal.value.open = false }

// ===== 실제 분배 호출 =====
async function onConfirmAllocate(payload: { centerId?:number|null, userId?:number|null }){
  const ids = needSelection(); if (!ids.length) return
  await runBusy(async () => {
    // 관리자가 분배할 경우: 센터 + 대상자(센터장/담당자) 선택 필수
    if (modal.value.mode === 'HQ' && (!payload.centerId)) {
      alert('센터를 선택하세요.')
      return
    } else if (modal.value.mode === 'HQ' && (!payload.userId)) {
      alert('직원을 선택하세요.')
      return
    }

    if (modal.value.mode === 'HQ') {
      await axios.post('/api/work/allocate/hq', {
        customerIds: ids,
        targetCenterId: payload.centerId,   // 필수 (모달에서 강제)
        targetUserId: payload.userId || null
      })
    } else {
      await axios.post('/api/work/allocate/manager', {
        customerIds: ids,
        targetUserId: payload.userId || null
      })
    }

    // alert(`분배 완료: ${ids.length}건`)
    modal.value.open = false
    selectedRows.value = []
    tableRef.value?.clearSelection?.()

    // 페이지 검사하며 새로고침
    await refetchAndClamp()
  }).catch((e:any) => {
    console.error(e)
    alert(e?.response?.data || '분배 중 오류가 발생했습니다.')
  })
}

// 페이지 검사하며 새로고침
async function refetchAndClamp() {
  await fetchData();

  // 총 페이지 수가 줄어 현재 page가 범위를 넘으면 마지막 페이지로
  if (page.value > totalPages.value) {
    changePage(Math.max(1, totalPages.value)); // changePage가 알아서 fetch 호출
    return;
  }

  // 총 페이지 값은 맞지만 현 페이지 데이터가 0개면 한 페이지 앞으로
  if ((items.value?.length ?? 0) === 0 && page.value > 1) {
    changePage(page.value - 1);
  }
}

// 수동 새로고침
async function onRefresh() {
  if (isRefreshing.value) return
  isRefreshing.value = true
  try {
    await axios.post('/api/sheets/refresh?sid=1')
    await refetchAndClamp()   // 중복 fetch 방지 + 페이지 클램핑 일원화
  } catch (e) {
    console.error(e)
    alert('새로고침 중 오류가 발생했습니다.')
  } finally {
    isRefreshing.value = false
  }
}
</script>
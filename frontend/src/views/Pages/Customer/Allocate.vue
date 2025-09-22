<!--<template>-->
<!--  <AdminLayout>-->
<!--    <PageBreadcrumb :pageTitle="currentPageTitle" />-->

<!--    <div class="grid grid-cols-12 gap-4 min-w-0">-->
<!--      <div class="col-span-12 space-y-6 min-w-0">-->
<!--        &lt;!&ndash; MANAGER / STAFF 전용 테이블 &ndash;&gt;-->
<!--        <ComponentCard-->
<!--            :buttons="['분배하기']"-->
<!--            v-if="role === 'MANAGER' || role === 'STAFF'"-->
<!--        >-->
<!--          <PsnsTable-->
<!--              :columns="commonColumns"-->
<!--              :data="commonCustomers"-->
<!--              :showCheckbox="true"-->
<!--              :page="commonPage"-->
<!--              :totalPages="commonTotalPages"-->
<!--              @rowSelect="onCommonRowSelect"-->
<!--              @badgeUpdate="onCommonBadgeUpdate"-->
<!--              @buttonClick="onCommonButtonClick"-->
<!--              @changePage="onCommonPageChange"-->
<!--          />-->
<!--        </ComponentCard>-->

<!--        &lt;!&ndash; SUPERADMIN 전용 테이블 &ndash;&gt;-->
<!--        <ComponentCard-->
<!--            :selects="[ ['최초', '중복', '유효'] ]"-->
<!--            :buttons="['분배하기']"-->
<!--            v-if="role === 'SUPERADMIN'"-->
<!--        >-->
<!--          <PsnsTable-->
<!--              :columns="adminColumns"-->
<!--              :data="adminCustomers"-->
<!--              :showCheckbox="true"-->
<!--              :page="adminPage"-->
<!--              :totalPages="adminTotalPages"-->
<!--              @rowSelect="onAdminRowSelect"-->
<!--              @badgeUpdate="onAdminBadgeUpdate"-->
<!--              @buttonClick="onAdminButtonClick"-->
<!--              @changePage="onAdminPageChange"-->
<!--          />-->
<!--        </ComponentCard>-->
<!--      </div>-->
<!--    </div>-->

<!--    &lt;!&ndash; 메모 모달 &ndash;&gt;-->
<!--    <Memo-->
<!--        v-if="isMemoOpen"-->
<!--        :row="selectedRow"-->
<!--        fullScreenBackdrop-->
<!--        @close="isMemoOpen = false"-->
<!--    >-->
<!--    </Memo>-->

<!--  </AdminLayout>-->
<!--</template>-->

<!--<script setup>-->
<!--import { ref } from "vue";-->
<!--import PageBreadcrumb from "@/components/common/PageBreadcrumb.vue";-->
<!--import AdminLayout from "@/components/layout/AdminLayout.vue";-->
<!--import ComponentCard from "@/components/common/ComponentCard.vue";-->
<!--import PsnsTable from "@/components/tables/basic-tables/PsnsTable.vue";-->
<!--import Memo from "@/components/ui/MEMO.vue";-->
<!--import { EyeIcon } from '@heroicons/vue/24/outline'-->
<!--import {useAuthStore} from "@/stores/auth.js";-->

<!--const auth = useAuthStore()-->

<!--// 예시: 로그인된 사용자 권한-->
<!--const role = auth.role-->

<!--const currentPageTitle = ref("고객DB 분배하기")-->

<!--/* =============================-->
<!--   MANAGER / STAFF 전용 변수-->
<!--============================= */-->
<!--const commonPage = ref(1)-->
<!--const commonTotalPages = ref(50)-->

<!--const commonColumns = ref([-->
<!--  { key: "createdAt", label: "DB생성일", type: "text" },-->
<!--  { key: "staff", label: "담당자", type: "text" },-->
<!--  { key: "category", label: "카테고리", type: "badge", options: ["주식", "코인"] },-->
<!--  { key: "name", label: "이름", type: "text"},-->
<!--  { key: "phone", label: "전화번호", type: "text" },-->
<!--  { key: "source", label: "고객접속경로", type: "text" },-->
<!--  { key: "content", label: "내용", type: "text", ellipsis: { width: 200 } }-->
<!--])-->

<!--const commonCustomers = ref([-->
<!--  { createdAt: "2025-09-12", staff: "김민수 / 010-1234-5678", category: "주식", name: "박지영", phone: "010-1111-2222", source: "XXX", content: "첫 상담 예약 완료" },-->
<!--  { createdAt: "2025-09-10", staff: "이은지 / 010-9876-5432", category: "코인", name: "최현우", phone: "010-3333-4444", source: "OOO", content: "2개월간 미접속" }-->
<!--])-->

<!--function onCommonRowSelect(row, e) { console.log("Row selected:", row, e.target.checked) }-->
<!--function onCommonBadgeUpdate(row, key, newValue) { console.log("Badge updated:", row, key, newValue) }-->
<!--function onCommonPageChange(newPage) { commonPage.value = newPage }-->

<!--/* =============================-->
<!--   SUPERADMIN 전용 변수-->
<!--============================= */-->
<!--const adminPage = ref(1)-->
<!--const adminTotalPages = ref(50)-->

<!--const adminColumns = ref([-->
<!--  { key: "createdAt", label: "DB생성일", type: "text" },-->
<!--  { key: "staff", label: "담당자", type: "text" },-->
<!--  { key: "division", label: "구분", type: "badge", options: ["최초", "중복", "유효"] }, // 추가된 컬럼-->
<!--  { key: "category", label: "카테고리", type: "badge", options: ["주식", "코인"] },-->
<!--  { key: "name", label: "이름", type: "text" },-->
<!--  { key: "phone", label: "전화번호", type: "text" },-->
<!--  { key: "source", label: "참여경로", type: "text" },-->
<!--  { key: "content", label: "내용", type: "text", ellipsis: { width: 200 } }-->
<!--])-->

<!--const adminCustomers = ref([-->
<!--  { createdAt: "2025-09-12", staff: "홍길동 / 010-1234-5678", category: "주식", division: "최초", name: "김영희", phone: "010-2222-3333", source: "APP", content: "계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료" },-->
<!--  { createdAt: "2025-09-11", staff: "이철수 / 010-9876-5432", category: "코인", division: "중복", name: "이민호", phone: "010-4444-5555", source: "WEB", content: "상담 거절" }-->
<!--])-->

<!--function onAdminRowSelect(row, e) { console.log("Row selected:", row, e.target.checked) }-->
<!--function onAdminBadgeUpdate(row, key, newValue) { console.log("Badge updated:", row, key, newValue) }-->
<!--function onAdminPageChange(newPage) { adminPage.value = newPage }-->

<!--/* =============================-->
<!--   메모 모달 전용 변수-->
<!--============================= */-->

<!--const isMemoOpen = ref(false)-->
<!--const selectedRow = ref(null)-->
<!--const memoText = ref("")-->

<!--function onCommonButtonClick(row, key) {-->
<!--  if (key === "memo") {-->
<!--    selectedRow.value = row-->
<!--    isMemoOpen.value = true-->
<!--  } else {-->
<!--    console.log("공용 버튼:", key, row)-->
<!--  }-->
<!--}-->

<!--function onAdminButtonClick(row, key) {-->
<!--  if (key === "memo") {-->
<!--    selectedRow.value = row-->
<!--    isMemoOpen.value = true-->
<!--  } else {-->
<!--    console.log("관리자 버튼:", key, row)-->
<!--  }-->
<!--}-->
<!--</script>-->
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
            :refreshing="loading"
            @refresh="fetchData"
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
            :refreshing="loading"
            @refresh="fetchData"
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
            :center-id="auth.centerId ?? null"
            :selected-count="selectedRows.length"
            @close="closeModal"
            @confirm="onConfirmAllocate"
        />

      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
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
const { items, page, totalPages, fetchData, changePage, setSize, setFilter, loading } = useTableQuery({
  url: '/api/work/allocate/list',
  pageSize: 10,
  externalFilters: globalFilters,
  useExternalKeys: { from: 'dateFrom', to: 'dateTo', category: 'category', keyword: 'keyword' },
  mapper: (res:any) => ({ items: res.data.items, totalPages: res.data.totalPages, totalCount: res.data.totalCount })
})

const tableRef = ref<any>(null)
const selectedRows = ref<any[]>([])
const onRowSelect = (rows:any[]) => { selectedRows.value = rows }

// ===== 컬럼 (읽기 전용!) =====
const baseCols = [
  { key: 'createdAt', label: 'DB생성일', type: 'text' },
  { key: 'category',  label: '카테고리', type: 'badge', options: ['주식','코인'] },
  { key: 'name',      label: '이름',     type: 'text' },
  { key: 'phone',     label: '전화번호', type: 'text' },
  { key: 'source',    label: 'DB출처',   type: 'text' },
  { key: 'content',   label: '내용',     type: 'text', ellipsis: { width: 150 } },
]

// 본사(HQ)만 담당자 이력 + 구분 노출
const hqColumns = [
  { key: 'createdAt', label: 'DB생성일', type: 'text' },
  { key: 'division',  label: '구분',     type: 'badge', options: ['최초','유효'] },
  { key: 'category',  label: '카테고리', type: 'badge', options: ['주식','코인'] },
  { key: 'name',      label: '이름',     type: 'text' },
  { key: 'phone',     label: '전화번호', type: 'text' },
  { key: 'source',    label: 'DB출처',   type: 'text' },
  { key: 'content',   label: '내용',     type: 'text', ellipsis: { width: 150 } },
  { key: 'paststaff',     label: '담당자 이력', type: 'text', ellipsis: { width: 150 } }, // HQ 전용
]

// 매니저는 담당자 이력/구분 X (분배만)
const mgrColumns = [ ...baseCols ]

// ===== HQ 전용 구분 필터 =====
function onDivisionSelect({ value }:{ value:string }){
  setFilter('division', value === '전체' ? null : value)
  fetchData()
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
  try {
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

    await fetchData()
  } catch (e:any) {
    console.error(e)
    alert(e?.response?.data || '분배 중 오류가 발생했습니다.')
  }
}
</script>
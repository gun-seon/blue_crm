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
  { key: 'paststaff',     label: '담당자 이력', type: 'text', ellipsis: { width: 200 } }, // HQ 전용
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
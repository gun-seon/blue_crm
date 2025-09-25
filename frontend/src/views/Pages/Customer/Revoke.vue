<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="pageTitle" />
    <div class="grid grid-cols-12 gap-4 min-w-0">
      <div class="col-span-12 space-y-6 min-w-0">

        <!-- SUPERADMIN (본사) -->
        <ComponentCard
            v-if="role === 'SUPERADMIN'"
            :buttons="['회수하기']"
            :showRefresh="true"
            :refreshing="loading"
            @refresh="refetchAndClamp"
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

        <!-- MANAGER (센터장) - 추후 기능 확장시 -->
        <ComponentCard
            v-else-if="role === 'MANAGER'"
            :buttons="['회수하기']"
            :showRefresh="true"
            :refreshing="loading"
            @refresh="refetchAndClamp"
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

        <!-- STAFF 접근 시 경고 안내를 노출하려면 주석 해제
        <div v-else class="p-6 rounded-xl border border-amber-200 bg-amber-50 text-amber-800 dark:border-amber-900/60 dark:bg-amber-950/30 dark:text-amber-200">
          이 페이지는 본사 또는 센터장만 사용할 수 있습니다.
        </div>
        -->

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
import { useAuthStore } from '@/stores/auth'
import { useTableQuery } from '@/composables/useTableQuery'
import { globalFilters } from '@/composables/globalFilters'
import axios from '@/plugins/axios'

/** 권한/페이지 타이틀 */
const auth = useAuthStore()
const role = auth.role
const pageTitle = ref('DB 회수하기')

/** 목록 훅 (공용 필터: 날짜/카테고리/키워드) */
const {
  items, page, totalPages, fetchData, changePage, setSize, setFilter, loading,
} = useTableQuery({
  url: '/api/work/revoke/list',
  pageSize: 10,
  externalFilters: globalFilters,
  useExternalKeys: { from: 'dateFrom', to: 'dateTo', category: 'category', keyword: 'keyword' },
  mapper: (res: any) => ({
    items: res.data.items,
    totalPages: res.data.totalPages,
    totalCount: res.data.totalCount,
  }),
})

/** 테이블 선택 */
const tableRef = ref<any>(null)
const selectedRows = ref<any[]>([])
const onRowSelect = (rows: any[]) => { selectedRows.value = rows }
function needSelection(): number[] {
  const ids = selectedRows.value.map((r: any) => r.id)
  if (!ids.length) alert('회수할 고객을 선택해주세요.')
  return ids
}

/** HQ 전용 구분 필터 */
function onDivisionSelect({ value }: { value: string }) {
  setFilter('division', value === '전체' ? null : value)
  fetchData()
}

/** 컬럼 정의 */
const hqColumns = [
  { key: 'createdAt', label: 'DB생성일', type: 'text' },
  { key: 'division',  label: '구분',     type: 'badge', options: ['최초','유효'] },
  // { key: 'category',  label: '카테고리', type: 'badge', options: ['주식','코인'] },
  { key: 'name',      label: '이름',     type: 'text' },
  { key: 'phone',     label: '전화번호', type: 'text', ellipsis: { width: 150 } },
  { key: 'source',    label: 'DB출처',   type: 'text' },
  { key: 'content',   label: '내용',     type: 'text', ellipsis: { width: 150 } },
  { key: "status",    label: "상태",     type: "badge" },
  { key: 'centerName',label: '센터',     type: 'badge' },
  { key: 'staff',     label: '담당자',    type: 'text' },
]

const mgrColumns = [
  { key: 'createdAt', label: 'DB생성일', type: 'text' },
  // { key: 'category',  label: '카테고리', type: 'badge', options: ['주식','코인'] },
  { key: 'name',      label: '이름',     type: 'text' },
  { key: 'phone',     label: '전화번호', type: 'text', ellipsis: { width: 150 } },
  { key: 'source',    label: 'DB출처',   type: 'text' },
  { key: 'content',   label: '내용',     type: 'text', ellipsis: { width: 150 } },
  { key: "status",    label: "상태",     type: "badge" },
  { key: 'centerName',label: '센터',     type: 'badge' },
  { key: 'staff',     label: '담당자',    type: 'text' },
]

/** 버튼 핸들러 */
function onHqButton(btn: string) {
  if (btn === '회수하기') {
    const ids = needSelection()
    if (!ids.length) return

    if (!confirm(ids.length + "개 DB를 회수하시겠습니까?")) return

    onConfirmRevoke(ids)
  }
}
function onMgrButton(btn: string) {
  if (btn === '회수하기') {
    alert('회수는 본사만 가능합니다.')
  }
}

/** 회수 실행 */
async function onConfirmRevoke(ids: number[]) {
  try {
    await axios.post('/api/work/revoke/hq', { customerIds: ids })

    // 선택 초기화
    selectedRows.value = []
    tableRef.value?.clearSelection?.()

    // 페이지 검사 후 새로고침
    await refetchAndClamp()
  } catch (e: any) {
    console.error(e)
    alert(e?.response?.data || '회수 중 오류가 발생했습니다.')
  }
}

// 페이지 검사 후 새로고침
async function refetchAndClamp() {
  await fetchData();

  // 총 페이지 수가 줄어 현재 page가 범위를 넘으면 마지막 페이지로
  if (page.value > totalPages.value) {
    changePage(Math.max(1, totalPages.value));
    await fetchData();        // <- 바로 로드
    return;
  }

  // 총 페이지는 같지만 현 페이지 데이터가 0이면 한 페이지 앞으로
  if ((items.value?.length ?? 0) === 0 && page.value > 1) {
    changePage(page.value - 1);
    await fetchData();        // <- 바로 로드
  }
}
</script>

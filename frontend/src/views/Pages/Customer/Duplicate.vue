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
</template>

<script setup>
import { ref } from 'vue'
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
  { key: 'category',   label: '카테고리', type: 'badge',  options: ['주식','코인'] },
  { key: 'name',       label: '이름',     type: 'text' },
  { key: 'phone',      label: '전화번호', type: 'text' },
  { key: 'source',     label: 'DB출처',   type: 'text',   ellipsis: { width: 120 } },
  { key: 'content',    label: '내용',     type: 'text',   ellipsis: { width: 180 } },
  { key: 'memo',       label: '메모',     type: 'text',   ellipsis: { width: 200 } },
  { key: 'status',     label: '상태',     type: 'badge',  options: ['없음'] },
]

// 테이블 데이터 훅 (공용 필터/페이지네이션 동일)
const {
  items,
  page,
  size,
  totalPages,
  fetchData,
  changePage,
  setSize
} = useTableQuery({
  url: '/api/work/duplicate',        // STAFF도 접근 가능 (SecurityConfig의 /api/work/**)
  pageSize: 10,
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

const isRefreshing = ref(false)
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

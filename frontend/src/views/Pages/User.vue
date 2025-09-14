<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="currentPageTitle" />
    <div class="grid grid-cols-12 gap-4 min-w-0">
      <div class="col-span-12 space-y-6 min-w-0">
        <ComponentCard
            :buttons="['센터관리', '엑셀로 다운로드']">
          <PsnsTable
              :columns="columns"
              :data="users"
              :showCheckbox="true"
              :page="page"
              :totalPages="totalPages"
              @rowSelect="onRowSelect"
              @badgeUpdate="onBadgeUpdate"
              @buttonClick="onButtonClick"
              @changePage="onPageChange"
          />
        </ComponentCard>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref } from "vue";
import PageBreadcrumb from "@/components/common/PageBreadcrumb.vue";
import AdminLayout from "@/components/layout/AdminLayout.vue";
import ComponentCard from "@/components/common/ComponentCard.vue";
import BasicTableOne from "@/components/tables/basic-tables/BasicTableOne.vue";
import PsnsTable from "@/components/tables/basic-tables/PsnsTable.vue";
import { EyeIcon, TrashIcon } from '@heroicons/vue/24/outline'

const currentPageTitle = ref("직원관리")
const page = ref(1)
const totalPages = ref(999)

// 부모에서 컬럼 정의
const columns = ref([
  { key: "type", label: "구분", type: "badge", editable: true, options: ["관리자", "센터장", "담당자"] },
  { key: "name", label: "이름", type: "text", ellipsis: { width: 20 } },
  { key: "phone", label: "전화번호", type: "text", ellipsis: { width: 100 } },
  { key: "email", label: "ID(Email)", type: "text", ellipsis: { width: 150 } },
  { key: "center", label: "소속", type: "badge", editable: true, options: ["본사", "센터A", "센터B", "센터C"] },
  { key: "requestStatus", label: "요청상태", type: "badge", editable: true, options: ["승인", "대기", "탈퇴"] }
])

// 부모에서 데이터 관리 (더미 데이터)
const users = ref([
  { type: "관리자", name: "홍길동", phone: "010-1234-5678", email: "hong@example.com", center: "본사", requestStatus: "승인" },
  { type: "센터장",   name: "김철수", phone: "010-9876-5432", email: "kim@example.com",  center: "센터A", requestStatus: "대기" },
  { type: "담당자",   name: "마리아", phone: "010-4567-8910", email: "maria@example.com",  center: "센터B", requestStatus: "탈퇴" }
])

// 이벤트 핸들러들
function onRowSelect(row, e) {
  console.log("Row selected:", row, e.target.checked)
}
function onBadgeUpdate(row, key, newValue) {
  console.log("Badge updated:", row, key, newValue)
}
function onButtonClick(row, key) {
  console.log("Button clicked:", row, key)
}
function onPageChange(newPage) {
  page.value = newPage
  console.log("Page changed:", newPage)
}
</script>

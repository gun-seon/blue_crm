<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="currentPageTitle" />
    <div class="space-y-5 sm:space-y-6">
      <ComponentCard
        :selects="[
          ['최초', '중복', '유효']
        ]"
        :buttons="['상태별 보기', '분배하기']"
      >
        <BasicTableOne />
        <PsnsTable
            :columns="columns"
            :data="users"
            :showCheckbox="true"
            :page="page"
            :totalPages="5"
            @rowSelect="onRowSelect"
            @badgeUpdate="onBadgeUpdate"
            @buttonClick="onButtonClick"
            @changePage="onPageChange"
        />
      </ComponentCard>
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

const currentPageTitle = ref("DB분배하기")
const page = ref(1)

// 부모에서 컬럼 정의
const columns = ref([
  { key: "name", label: "User", type: "text" },
  { key: "project", label: "Project Name", type: "text", ellipsis: true },
  { key: "status", label: "Status", type: "badge", editable: false, options: ["Active", "Pending", "Cancel"] },
  { key: "category", label: "구분", type: "badge", editable: true, options: ["A", "B", "C"] },
  { key: "budget", label: "Budget", type: "text" },
  { key: "view", label: "", type: "iconButton", icon: EyeIcon },   // Heroicons
  { key: "delete", label: "", type: "iconButton", icon: TrashIcon } // Heroicons
])

// 부모에서 데이터 관리
const users = ref([
  { name: "홍길동", project: "Agency Website", status: "Active", category: "A", budget: "3.9K" },
  { name: "김철수", project: "Technology", status: "Pending", category: "B", budget: "24.9K" }
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

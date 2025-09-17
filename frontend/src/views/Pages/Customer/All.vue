<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="currentPageTitle" />
    <div class="grid grid-cols-12 gap-4 min-w-0">
      <div class="col-span-12 space-y-6 min-w-0">

        <!-- SUPERADMIN (본사) -->
        <ComponentCard
            v-if="role === 'SUPERADMIN'"
            :selects="[ ['최초', '중복', '유효'] ]"
            :buttons="['상태별 보기', '구분별 보기']"
            @changeSize="setSize"
            @selectChange="onDivisionSelect"
            @buttonClick="onAdminButtonClick"
        >
          <PsnsTable
              :columns="adminColumns"
              :data="items"
              :showCheckbox="true"
              :page="page"
              :totalPages="totalPages"
              @rowSelect="onRowSelect"
              @badgeUpdate="onBadgeUpdate"
              @DateUpdate="onDateUpdate"
              @changePage="changePage"
          />
        </ComponentCard>

        <!-- MANAGER / STAFF -->
        <ComponentCard
            v-else
            :buttons="['상태별 보기']"
            @changeSize="setSize"
            @buttonClick="onCommonButtonClick"
        >
          <PsnsTable
              :columns="commonColumns"
              :data="items"
              :showCheckbox="true"
              :page="page"
              :totalPages="totalPages"
              @rowSelect="onRowSelect"
              @badgeUpdate="onBadgeUpdate"
              @DateUpdate="onDateUpdate"
              @changePage="changePage"
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
import PsnsTable from "@/components/tables/basic-tables/PsnsTable.vue";
import { EyeIcon } from "@heroicons/vue/24/outline";
import { useAuthStore } from "@/stores/auth.js";
import { useTableQuery } from "@/composables/useTableQuery.js";
import { globalFilters } from "@/composables/globalFilters.js";
import axios from "@/plugins/axios.js"

const auth = useAuthStore();
const role = auth.role;

const currentPageTitle = ref("전체 고객DB관리");

/* =============================
   공통 useTableQuery
============================= */
const {
  items,
  page,
  size,
  totalPages,
  fetchData,
  changePage,
  setSize,
  setFilter
} = useTableQuery({
  url: "/api/super/db", // 공통 API
  pageSize: 10,
  externalFilters: globalFilters,
  useExternalKeys: { from: "dateFrom", to: "dateTo", category: "category", keyword: "keyword" },
  mapper: (res) => ({
    items: res.data.items,
    totalPages: res.data.totalPages,
    totalCount: res.data.totalCount
  })
});

/* =============================
   SUPERADMIN 전용 컬럼
============================= */
const adminColumns = [
  { key: "createdAt", label: "DB생성일", type: "text" },
  { key: "staff", label: "담당자", type: "text" },
  { key: "division", label: "구분", type: "badge", options: ["최초", "중복", "유효"] },
  { key: "category", label: "카테고리", type: "badge", options: ["주식", "코인"] },
  { key: "name", label: "이름", type: "text" },
  { key: "phone", label: "전화번호", type: "text" },
  { key: "source", label: "출처", type: "text" },
  { key: "content", label: "내용", type: "text", ellipsis: { width: 150 } },
  { key: "memo", label: "메모", type: "iconButton", icon: EyeIcon },
  { key: "status", label: "상태", type: "badge", editable: true, options: ["부재1","부재2","부재3","부재4","부재5","재콜","가망","완료","거절"] },
  { key: "reservation", label: "예약", type: "date" }
];

/* =============================
   MANAGER / STAFF 전용 컬럼
============================= */
const commonColumns = [
  { key: "createdAt", label: "DB생성일", type: "text" },
  { key: "staff", label: "담당자", type: "text" },
  { key: "category", label: "카테고리", type: "badge", options: ["주식", "코인"] },
  { key: "name", label: "이름", type: "text"},
  { key: "phone", label: "전화번호", type: "text" },
  { key: "source", label: "고객접속경로", type: "text" },
  { key: "content", label: "내용", type: "text", ellipsis: { width: 100 } },
  { key: "memo", label: "메모", type: "iconButton", icon: EyeIcon },
  { key: "status", label: "상태", type: "badge", editable: true, options: ["부재1","부재2","부재3","부재4","부재5","재콜","가망","완료","거절"] },
  { key: "reservation", label: "예약", type: "date" }
];

/* =============================
   이벤트 핸들러
============================= */
function onRowSelect(rows) {
  console.log("선택 행:", rows);
}

function onBadgeUpdate(row, key, newValue) {
  console.log("배지 수정:", row, key, newValue);
}

async function onDateUpdate(row, key, newValue) {
  try {
    await axios.patch(`/api/super/db/all/update/${row.id}`, {
      field: key,       // "reservation"
      value: newValue   // 날짜 값
    })
    console.log("예약일 저장 성공:", row.name, newValue)
  } catch (err) {
    console.error("예약일 저장 실패", err)
    alert("예약일 저장 중 오류가 발생했습니다.")
  }
}

function onDivisionSelect({ idx, value }) {
  console.log("구분 필터 선택:", value);
  setFilter("division", value);
  fetchData();
}

function onAdminButtonClick(btn) {
  if (btn === "상태별 보기") {
    setFilter("sort", "status");
  }
  if (btn === "구분별 보기") {
    setFilter("sort", "division");
  }
  fetchData();
}

function onCommonButtonClick(btn) {
  if (btn === "상태별 보기") {
    setFilter("sort", "status");
    fetchData();
  }
}
</script>

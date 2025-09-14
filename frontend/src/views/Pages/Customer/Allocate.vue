<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="currentPageTitle" />

    <div class="grid grid-cols-12 gap-4 min-w-0">
      <div class="col-span-12 space-y-6 min-w-0">
        <!-- MANAGER / STAFF 전용 테이블 -->
        <ComponentCard
            :buttons="['분배하기']"
            v-if="role === 'MANAGER' || role === 'STAFF'"
        >
          <PsnsTable
              :columns="commonColumns"
              :data="commonCustomers"
              :showCheckbox="true"
              :page="commonPage"
              :totalPages="commonTotalPages"
              @rowSelect="onCommonRowSelect"
              @badgeUpdate="onCommonBadgeUpdate"
              @buttonClick="onCommonButtonClick"
              @changePage="onCommonPageChange"
          />
        </ComponentCard>

        <!-- SUPERADMIN 전용 테이블 -->
        <ComponentCard
            :selects="[ ['최초', '중복', '유효'] ]"
            :buttons="['분배하기']"
            v-if="role === 'SUPERADMIN'"
        >
          <PsnsTable
              :columns="adminColumns"
              :data="adminCustomers"
              :showCheckbox="true"
              :page="adminPage"
              :totalPages="adminTotalPages"
              @rowSelect="onAdminRowSelect"
              @badgeUpdate="onAdminBadgeUpdate"
              @buttonClick="onAdminButtonClick"
              @changePage="onAdminPageChange"
          />
        </ComponentCard>
      </div>
    </div>

    <!-- 메모 모달 -->
    <Memo
        v-if="isMemoOpen"
        :row="selectedRow"
        fullScreenBackdrop
        @close="isMemoOpen = false"
    >
    </Memo>

  </AdminLayout>
</template>

<script setup>
import { ref } from "vue";
import PageBreadcrumb from "@/components/common/PageBreadcrumb.vue";
import AdminLayout from "@/components/layout/AdminLayout.vue";
import ComponentCard from "@/components/common/ComponentCard.vue";
import PsnsTable from "@/components/tables/basic-tables/PsnsTable.vue";
import Memo from "@/components/ui/MEMO.vue";
import { EyeIcon } from '@heroicons/vue/24/outline'
import {useAuthStore} from "@/stores/auth.js";

const auth = useAuthStore()

// 예시: 로그인된 사용자 권한
const role = auth.role

const currentPageTitle = ref("고객DB 분배하기")

/* =============================
   MANAGER / STAFF 전용 변수
============================= */
const commonPage = ref(1)
const commonTotalPages = ref(50)

const commonColumns = ref([
  { key: "createdAt", label: "DB생성일", type: "text" },
  { key: "staff", label: "담당자", type: "text" },
  { key: "category", label: "카테고리", type: "badge", options: ["주식", "코인"] },
  { key: "name", label: "이름", type: "text"},
  { key: "phone", label: "전화번호", type: "text" },
  { key: "status", label: "상태", type: "badge", options: ["부재1", "부재2", "부재3", "부재4", "부재5", "재콜", "신규", "가망", "완료", "거절"] },
  { key: "source", label: "고객접속경로", type: "text" },
  { key: "content", label: "내용", type: "text", ellipsis: { width: 200 } },
  { key: "memo", label: "메모", type: "iconButton", icon: EyeIcon }
])

const commonCustomers = ref([
  { createdAt: "2025-09-12", staff: "김민수 / 010-1234-5678", category: "주식", name: "박지영", phone: "010-1111-2222", status: "부재1", source: "XXX", content: "첫 상담 예약 완료" },
  { createdAt: "2025-09-10", staff: "이은지 / 010-9876-5432", category: "코인", name: "최현우", phone: "010-3333-4444", status: "신규", source: "OOO", content: "2개월간 미접속" }
])

function onCommonRowSelect(row, e) { console.log("Row selected:", row, e.target.checked) }
function onCommonBadgeUpdate(row, key, newValue) { console.log("Badge updated:", row, key, newValue) }
function onCommonPageChange(newPage) { commonPage.value = newPage }

/* =============================
   SUPERADMIN 전용 변수
============================= */
const adminPage = ref(1)
const adminTotalPages = ref(50)

const adminColumns = ref([
  { key: "createdAt", label: "DB생성일", type: "text" },
  { key: "staff", label: "담당자", type: "text" },
  { key: "division", label: "구분", type: "badge", options: ["최초", "중복", "유효"] }, // 추가된 컬럼
  { key: "category", label: "카테고리", type: "badge", options: ["주식", "코인"] },
  { key: "name", label: "이름", type: "text" },
  { key: "phone", label: "전화번호", type: "text" },
  { key: "status", label: "상태", type: "badge", options: ["부재1", "부재2", "부재3", "부재4", "부재5", "재콜", "신규", "가망", "완료", "거절"] },
  { key: "source", label: "참여경로", type: "text" },
  { key: "content", label: "내용", type: "text", ellipsis: { width: 200 } },
  { key: "memo", label: "메모", type: "iconButton", icon: EyeIcon }
])

const adminCustomers = ref([
  { createdAt: "2025-09-12", staff: "홍길동 / 010-1234-5678", category: "주식", division: "최초", name: "김영희", phone: "010-2222-3333", status: "완료", source: "APP", content: "계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료계약 완료" },
  { createdAt: "2025-09-11", staff: "이철수 / 010-9876-5432", category: "코인", division: "중복", name: "이민호", phone: "010-4444-5555", status: "거절", source: "WEB", content: "상담 거절" }
])

function onAdminRowSelect(row, e) { console.log("Row selected:", row, e.target.checked) }
function onAdminBadgeUpdate(row, key, newValue) { console.log("Badge updated:", row, key, newValue) }
function onAdminPageChange(newPage) { adminPage.value = newPage }

/* =============================
   메모 모달 전용 변수
============================= */

const isMemoOpen = ref(false)
const selectedRow = ref(null)
const memoText = ref("")

function onCommonButtonClick(row, key) {
  if (key === "memo") {
    selectedRow.value = row
    isMemoOpen.value = true
  } else {
    console.log("공용 버튼:", key, row)
  }
}

function onAdminButtonClick(row, key) {
  if (key === "memo") {
    selectedRow.value = row
    isMemoOpen.value = true
  } else {
    console.log("관리자 버튼:", key, row)
  }
}
</script>

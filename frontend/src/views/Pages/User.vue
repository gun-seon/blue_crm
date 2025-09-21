<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="currentPageTitle" />
    <div class="grid grid-cols-12 gap-4 min-w-0">
      <div class="col-span-12 space-y-6 min-w-0">
        <ComponentCard
            :buttons="['센터관리', '일괄승인']"
            @changeSize="val => setSize(val)"
            @buttonClick="onButtonClick">
          <PsnsTable
              :columns="columns"
              :data="items"
              :showCheckbox="true"
              :page="page"
              :totalPages="totalPages"
              @rowSelect="onRowSelect"
              @badgeUpdate="onBadgeUpdate"
              @changePage="changePage"
          />
        </ComponentCard>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import PageBreadcrumb from "@/components/common/PageBreadcrumb.vue";
import AdminLayout from "@/components/layout/AdminLayout.vue";
import ComponentCard from "@/components/common/ComponentCard.vue";
import PsnsTable from "@/components/tables/basic-tables/PsnsTable.vue";
import axios from "@/plugins/axios.js";
import {useAuthStore} from "@/stores/auth.js";

// 로그인 관리
const auth = useAuthStore()

// 공용 쿼리 컴포저블
import { useTableQuery } from "@/composables/useTableQuery.js";
import { globalFilters } from "@/composables/globalFilters.js";

const currentPageTitle = ref("직원관리")
const selectedRows = ref([])

// DB에서 centers 불러오기
const centerOptions = ref([]);
onMounted(async () => {
  try {
    const res = await axios.get("/api/common/centers");
    // console.log(res)
    centerOptions.value = res.data.map(c => c.centerName);
    // console.log(centerOptions.value)
  } catch (err) {
    console.error("센터 목록 불러오기 실패:", err.response?.data || err.message);
    centerOptions.value = [] // 실패 시 안전하게 초기화
  }
});

// 테이블 데이터 훅 연결
const {
  items,       // 데이터 rows
  page,        // 현재 페이지 (ref)
  size,        // 페이지당 갯수 (ref)
  totalPages,  // 총 페이지 수 (ref)
  changePage,  // 페이지 변경 함수
  setSize,     // 표 사이즈 결정 함수
  fetchData,   // 표 새로고침
  loading,     // 로딩 상태
  error        // 에러 상태
} = useTableQuery({
  url: "/api/super/users", // 실제 API 엔드포인트
  pageSize: 10,                 // 기본 페이지 크기
  externalFilters: globalFilters, // Header.vue에서 keyword 값 반영됨
  useExternalKeys: { keyword: "keyword" }, // 딱 검색어만 API 파라미터로 넘김
  mapper: (res) => {
    // API 응답 매핑 (백엔드 형식 맞게 조정 필요)
    return {
      items: res.data.items.map(u => ({
        userId: u.userId,
        type:
            u.userRole === "SUPERADMIN" ? "관리자" :
            u.userRole === "MANAGER"    ? "센터장" :
            u.userRole === "STAFF"      ? "담당자" : u.userRole,
        name: u.userName,
        phone: u.userPhone,
        email: u.userEmail,
        center: u.centerName || '미할당',
        requestStatus:
            u.userApproved === 'Y' ? '승인' :
            u.userApproved === 'N' ? '대기' :
            u.userApproved === 'X' ? '탈퇴' : u.userApproved
      })),
      totalPages: res.data.totalPages,
      totalCount: res.data.totalCount
    }
  }
});

// 부모에서 컬럼 정의
const columns = computed(() => [
  { key: "type", label: "구분", type: "badge", editable: canEdit, options: ["관리자", "센터장", "담당자"] },
  { key: "name", label: "이름", type: "text", ellipsis: { width: 50 } },
  { key: "phone", label: "전화번호", type: "text", ellipsis: { width: 100 } },
  { key: "email", label: "ID(Email)", type: "text", ellipsis: { width: 150 } },
  { key: "center", label: "소속", type: "badge", editable: (row) => row.center !== "본사", options: centerOptions.value },
  { key: "requestStatus", label: "요청상태", type: "badge", editable: canEdit, options: ["승인", "대기", "탈퇴"] }
])

// 배지 수정 가능 여부 검토
function canEdit(row) {
  const requesterEmail = auth.email
  // 1. 본인 계정이면 불가
  if (row.email === requesterEmail) return false
  // 2. super 계정 제외, 관리자 계정 수정 불가
  return !((row.type === "관리자" || row.center === "본사") && requesterEmail !== "super@naver.com");
}

// 이벤트 핸들러들
// 1. 테이블에서 체크박스 선택 시
function onRowSelect(rows) {
  selectedRows.value = rows
  // console.log("현재 선택된 행들:", rows)
}

// 2. 버튼 클릭 시 (ComponentCard → User.vue)
async function onButtonClick(btn) {
  // console.log("Button clicked:", btn)
  if (btn === "일괄승인") {
    await onBulkApprove()
  }
  if (btn === "센터관리") {
    // 기존 로직
  }
}

// 2-1. 일괄승인 로직
async function onBulkApprove() {
  if (selectedRows.value.length === 0) {
    alert("선택된 사용자가 없습니다.")
    return
  }

  if (!confirm(`${selectedRows.value.length}명의 사용자를 승인하시겠습니까?`)) {
    return
  }

  try {
    const ids = selectedRows.value.map(r => r.userId)
    const res = await axios.patch("/api/super/users/bulk-approve", ids)
    const { approvedIds, skippedIds } = res.data

    // 결과 메시지 구성
    if (approvedIds.length > 0 && skippedIds.length === 0) {
      alert(`일괄 승인 성공`)
    } else if (approvedIds.length > 0 && skippedIds.length > 0) {
      alert(`관리자 권한인 ${skippedIds.length}명 제외, ${approvedIds.length}명 승인 성공`)
    } else if (approvedIds.length === 0 && skippedIds.length > 0) {
      alert(`관리자 권한인 ${skippedIds.length}명 제외, 승인된 사용자가 없습니다`)
    }

    // 테이블 새로고침 (DB 최신 상태 반영)
    await fetchData()

    // 선택 해제
    selectedRows.value = []
  } catch (err) {
    console.error("일괄 승인 실패", err)
    alert("일괄 승인 중 오류가 발생했습니다.")
  }
}

// 3. 배지를 수정할 경우
async function onBadgeUpdate(row, key, newValue) {
  // console.log("Badge updated:", row, key, newValue)
  let fieldLabel = key
  let displayValue = newValue

  // key → 한글
  if (key === "type") fieldLabel = "구분"
  else if (key === "center") fieldLabel = "소속"
  else if (key === "requestStatus") fieldLabel = "요청상태"

  // value → 한글
  if (newValue === "SUPERADMIN") displayValue = "관리자"
  else if (newValue === "MANAGER") displayValue = "센터장"
  else if (newValue === "STAFF") displayValue = "담당자"

  if (!confirm(`${row.name}의 ${fieldLabel}을(를) "${displayValue}"(으)로 변경하시겠습니까?`)) {
    await fetchData()
    return
  }

  try {
    // key: 'type' | 'center' | 'requestStatus' ...
    const payload = { field: key, value: newValue }

    // row 에 userId 가 있어야 함 (없으면 매퍼쪽에서 내려줘야 함)
    await axios.patch(`/api/super/users/update/${row.userId}`, payload)

    // 성공 시 UI 반영
    row[key] = newValue
    await fetchData()

    // console.log(newValue, "업데이트 성공")
  } catch (err) {
    console.error("업데이트 실패", err)
    alert("업데이트 중 오류가 발생했습니다.")
  }
}
// function onPageChange(newPage) {
//   page.value = newPage
//   console.log("Page changed:", newPage)
// }
</script>

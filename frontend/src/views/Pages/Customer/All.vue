<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="currentPageTitle" />
    <div class="grid grid-cols-12 gap-4 min-w-0">
      <div class="col-span-12 space-y-6 min-w-0">

        <!-- SUPERADMIN (본사) -->
        <ComponentCard
            v-if="role === 'SUPERADMIN'"
            :selects="[ ['전체', '최초', '중복', '유효'] ]"
            :buttons="['상태별 보기', '구분별 보기', '중복DB로 이동']"
            @changeSize="setSize"
            @selectChange="onDivisionSelect"
            @buttonClick="onAdminButtonClick"
        >
          <PsnsTable
              :key="tableKey"
              ref="tableRef"
              :columns="adminColumns"
              :data="items"
              :showCheckbox="true"
              :rowSelectable="isRowSelectable"
              :page="page"
              :totalPages="totalPages"
              @rowSelect="onRowSelect"
              @badgeUpdate="onBadgeUpdate"
              @DateUpdate="onDateUpdate"
              @buttonClick="onTableButtonClick"
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
              :key="tableKey"
              ref="tableRef"
              :columns="commonColumns"
              :data="items"
              :showCheckbox="true"
              :rowSelectable="isRowSelectable"
              :page="page"
              :totalPages="totalPages"
              @rowSelect="onRowSelect"
              @badgeUpdate="onBadgeUpdate"
              @DateUpdate="onDateUpdate"
              @buttonClick="onTableButtonClick"
              @changePage="changePage"
          />
        </ComponentCard>

        <Memo
            v-if="memoOpen"
            :row="memoRow"
            @close="closeMemo"
            @saved="onMemoSaved"
        />

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
import Memo from "@/components/ui/MEMO.vue";
import { EyeIcon } from "@heroicons/vue/24/outline";
import { useAuthStore } from "@/stores/auth.js";
import { useTableQuery } from "@/composables/useTableQuery.js";
import { globalFilters } from "@/composables/globalFilters.js";
import axios from "@/plugins/axios.js"

const auth = useAuthStore();
const role = auth.role;

const currentPageTitle = ref("전체 고객DB관리");
const tableKey = ref(0); // 선택 초기화 강제 리렌더용
const tableRef = ref(null); // PsnsTable 메서드 접근
const selectedRows = ref([]); // 선택된 행 캐시
const memoOpen = ref(false); // 메모 모달 상태
const memoRow = ref(null); // 메모 모달에 넘길 행

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
  url: "/api/work/db", // 공통 API
  pageSize: 10,
  externalFilters: globalFilters,
  useExternalKeys: { from: "dateFrom", to: "dateTo", category: "category", keyword: "keyword" },
  mapper: (res) => ({
    items: res.data.items,
    totalPages: res.data.totalPages,
    totalCount: res.data.totalCount
  })
});

/** 체크박스 활성화 조건: '중복'만 선택 가능 (관리자 전용 화면), 유효/최초는 비활성 */
function isRowSelectable(row) {
  return row.origin === 'DUPLICATE';
}

/** 중복이면 모든 편집 비활성 (배지/날짜 등) */
function notDuplicate(row) {
  return row.origin !== 'DUPLICATE';
}

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
  { key: "source", label: "DB출처", type: "text", ellipsis: { width: 100 } },
  { key: "content", label: "내용", type: "text", ellipsis: { width: 150 } },
  { key: "memo", label: "메모", type: "iconButton", icon: EyeIcon, disabled: (row)=> row.origin==='DUPLICATE' },
  { key: "status", label: "상태", type: "badge",
      editable: notDuplicate,
      // 회수와 신규 상태는 수동으로 줄 수 없음
      // 회수 : DB회수하기 메뉴에서
      // 신규 : 한번도 분배가 되지 않은 항목만
      options: ["부재1","부재2","부재3","부재4","부재5","재콜","가망","완료","거절"] },
  { key: "reservation", label: "예약", type: "date", editable: notDuplicate }
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
  { key: "source", label: "DB출처", type: "text", ellipsis: { width: 100 } },
  { key: "content", label: "내용", type: "text", ellipsis: { width: 150 } },
  { key: "memo", label: "메모", type: "iconButton", icon: EyeIcon, disabled: (row)=> row.origin==='DUPLICATE' },
  { key: "status", label: "상태", type: "badge",
      editable: notDuplicate,
      options: ["부재1","부재2","부재3","부재4","부재5","재콜","가망","완료","거절"] },
  { key: "reservation", label: "예약", type: "date", editable: notDuplicate }
];

/* =============================
   이벤트 핸들러
============================= */
function onRowSelect(rows) {
  selectedRows.value = rows;
  console.log("선택 행:", rows);
}

function onBadgeUpdate(row, key, newValue) {
  if (row.origin === 'DUPLICATE') {
    alert('중복 DB는 수정할 수 없습니다.');
    return;
  }

  // 상태 배지만 서버 반영 (필요시 division 등 확장)
  if (key === 'status') {
    axios.patch(`/api/work/db/all/update/${row.id}`, {
      field: key,
      value: newValue
    }).catch(err => {
      console.error("상태 저장 실패", err);
      alert("상태 저장 중 오류가 발생했습니다.");
    });
  }

  console.log("배지 수정:", row, key, newValue);
}

async function onDateUpdate(row, key, newValue) {
  // 중복DB는 클릭 불가
  if (row.origin === 'DUPLICATE') {
    return;
  }

  try {
    await axios.patch(`/api/work/db/all/update/${row.id}`, {
      field: key,       // "reservation"
      value: newValue   // 날짜 값
    })
    console.log("예약일 저장 성공:", row.name, newValue)
  } catch (err) {
    console.error("예약일 저장 실패", err)
    alert("예약일 저장 중 오류가 발생했습니다.")
  }
}

// 메모 아이콘 클릭 -> 모달 오픈
function onTableButtonClick(row, key) {
  if (key !== 'memo') return;
  if (row.origin === 'DUPLICATE') {
    alert('중복 DB는 메모 수정이 불가합니다.');
    return;
  }

  memoRow.value = row;
  memoOpen.value = true;
}

function closeMemo() {
  memoOpen.value = false;
  memoRow.value = null;
}

function onDivisionSelect({ idx, value }) {
  console.log("구분 필터 선택:", value);
  if (value === "전체") {
    setFilter("division", null)
  } else {
    setFilter("division", value);
  }
  fetchData();
}

async function onAdminButtonClick(btn) {
  if (btn === "상태별 보기") {
    setFilter("sort", "status");
  }
  if (btn === "구분별 보기") {
    setFilter("sort", "division");
  }
  if (btn === "중복DB로 이동") {
    const dupIds = selectedRows.value
        .filter(r => r.origin === 'DUPLICATE')
        .map(r => r.id);

    if (dupIds.length === 0) {
      alert("중복 항목만 선택해서 이동할 수 있습니다.");
      return;
    }

    try {
      await axios.post("/api/lead/db/duplicate/hide", {ids: dupIds});
      alert(`중복 ${dupIds.length}건을 중복DB 메뉴로 이동(숨김)했습니다.`);

      // 선택 초기화(내부/외부 모두): 테이블 메서드 + 강제리렌더 + 배열 초기화
      selectedRows.value = [];
      tableRef.value?.clearSelection?.(); // PsnsTable이 메서드 제공 시
      tableKey.value++; // 강제 리렌더로 selection state 초기화
      await fetchData();
    } catch (err) {
      console.error("중복 이동 실패", err);
      alert("중복 이동 중 오류가 발생했습니다.");
    }

    return;
  }

  fetchData();
}

function onCommonButtonClick(btn) {
  if (btn === "상태별 보기") {
    setFilter("sort", "status");
    fetchData();
  }
}

// 모달 갱신
function onMemoSaved(patch) {
  const arr = items.value ?? [];
  const idx = arr.findIndex(r => r.id === patch.id);
  if (idx !== -1) {
    const cur = arr[idx];
    arr.splice(idx, 1, {
      ...cur,
      status: patch.status,
      reservation: patch.reservation, // 바로 리스트에 반영
    });
  } else {
    // 현재 페이지에 행이 없을 때만 안전 재조회 (페이지 유지)
    const curPage = page.value;
    fetchData().then(() => { if (page.value !== curPage) changePage(curPage); });
  }
}
</script>

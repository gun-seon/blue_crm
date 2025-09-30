<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="currentPageTitle" />
    <div class="grid grid-cols-12 gap-4 min-w-0">
      <div class="col-span-12 space-y-6 min-w-0">

        <!-- SUPERADMIN (본사) -->
        <ComponentCard
            v-if="role === 'SUPERADMIN'"
            :selects="[ ['전체', '최초', '중복', '유효'] ]"
            :buttons="['구분별 보기', '상태별 보기', '중복DB로 이동']"
            :active="adminActiveLabels"
            :showRefresh="true"
            :refreshing="isRefreshing"
            @refresh="onRefresh"
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
              :page="pageDisplay"
              :totalPages="lastPageNo"
              :hasNext="hasNext"
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
            :buttons=managerButtons
            :showRefresh="true"
            :refreshing="isRefreshing"
            @refresh="onRefresh"
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
              :page="pageDisplay"
              :totalPages="lastPageNo"
              :hasNext="hasNext"
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
import {ref, computed, onMounted, watch} from "vue"
import PageBreadcrumb from "@/components/common/PageBreadcrumb.vue";
import AdminLayout from "@/components/layout/AdminLayout.vue";
import ComponentCard from "@/components/common/ComponentCard.vue";
import PsnsTable from "@/components/tables/basic-tables/PsnsTable.vue";
import Memo from "@/components/ui/MEMO.vue";
import { EyeIcon } from "@heroicons/vue/24/outline";
import { useAuthStore } from "@/stores/auth.js";
import { useKeysetQuery } from "@/composables/useKeysetQuery.js";
import { globalFilters } from "@/composables/globalFilters.js";
import axios from "@/plugins/axios.js";

// ─────────────────────────────────────────────────────────────
// 권한/뷰 상태
// ─────────────────────────────────────────────────────────────
const auth = useAuthStore();
const role = auth.role;
const isManager = computed(() => role === 'MANAGER');
const mineOnly = ref(false);

const currentPageTitle = ref("전체 고객DB관리");
const tableKey = ref(0);            // 선택 초기화 강제 리렌더
const tableRef = ref(null);         // PsnsTable 메서드 접근
const selectedRows = ref([]);       // 선택된 행
const memoOpen = ref(false);
const memoRow = ref(null);
const isRefreshing = ref(false);

// 공용 필터 값이 바뀌면 서버로 즉시 반영 + 첫 페이지로 리셋
watch(
    () => ({
      dateFrom: globalFilters.dateFrom,
      dateTo:   globalFilters.dateTo,
      category: globalFilters.category,
      keyword:  globalFilters.keyword
    }),
    async (f) => {
      await patchFilters({
        dateFrom: f.dateFrom ?? '',
        dateTo:   f.dateTo   ?? '',
        category: f.category ?? '',
        keyword:  f.keyword  ?? '',
      })
      // 페이지네이션 초기화
      await loadLastPageNo()

      // 선택 초기화(있으면)
      selectedRows.value = []
      tableRef.value?.clearSelection?.()
      tableKey.value++
    },
    { deep: true }
)

// ─────────────────────────────────────────────────────────────
// 키셋 훅: 핵심
//  - +1/–1/–2/200+ 점프
//  - 필터/정렬 텍스트는 그대로 서버로 전달 (서버가 불린/우선순위로 해석)
// ─────────────────────────────────────────────────────────────
const {
  items, hasNext, loading, pageIndex,
  fetchData, prefetchForward, jumpByAnchor,
  goFirst, goPrev, goMinus2, goNext, goPlus2,
  jumpWindow, setPageSize,
  setFilters, patchFilters,
  cursors, currentFilters,
  pageDisplay, lastPageNo, goToPageAbsolute, goLast, loadLastPageNo
} = useKeysetQuery({
  baseUrl: "/api/work/db/keyset",
  pageSize: 10,
  filters: {
    // 글로벌 필터는 그대로 전달 (dateFrom/dateTo/category/keyword 등)
    dateFrom: globalFilters.dateFrom ?? '',
    dateTo:   globalFilters.dateTo   ?? '',
    category: globalFilters.category ?? '',
    keyword:  globalFilters.keyword  ?? '',
    // 초기 정렬은 최신순. 토글 시 아래 버튼 핸들러에서 set/patch 한다.
    divisionSort: 'off',
    statusSort: 'off',
    sort: null,
    mine: null,
    staffUserId: null,
  }
});

// 키셋 모드에서 page/totalPages 래핑 (PsnsTable 인터페이스 하위호환)
const page = computed(() => pageIndex.value + 1);
// totalPages 개념이 없으므로 null/0 처리(컴포넌트가 hasNext 기반으로 버튼 제어해야 함)
const totalPages = computed(() => null);

function setSize(n) {
  setPageSize?.(n);   // 훅에 setPageSize가 없다면, 훅에서 setSize를 export하고 여기선 그걸 그대로 바인딩
  fetchData();
}

// ─────────────────────────────────────────────────────────────
// 새로고침
// ─────────────────────────────────────────────────────────────
async function onRefresh() {
  if (isRefreshing.value) return;
  isRefreshing.value = true;
  try {
    // 예시: 수동 새로고침 트리거
    await axios.post('/api/sheets/refresh?sid=1');
    await refetchAndClamp();
  } catch (err) {
    console.error('수동 새로고침 실패', err);
    alert('새로고침 중 오류가 발생했습니다.');
  } finally {
    isRefreshing.value = false;
  }
}

// ─────────────────────────────────────────────────────────────
// 테이블 유틸
// ─────────────────────────────────────────────────────────────
function isRowSelectable(row) {
  // ‘중복’만 선택 가능(본사 화면 정책)
  return row.origin === 'DUPLICATE';
}
function notDuplicate(row) {
  return row.origin !== 'DUPLICATE';
}

// SUPERADMIN 전용 컬럼
const adminColumns = [
  { key: "createdAt", label: "DB생성일", type: "text" },
  { key: "staff", label: "담당자", type: "text" },
  { key: "division", label: "구분", type: "badge", options: ["최초", "중복", "유효"] },
  { key: "",  label: "",   type: "text", ellipsis: { width: 5 } },
  { key: "name", label: "이름", type: "text" },
  { key: "phone", label: "전화번호", type: "text", ellipsis: { width: 150 } },
  { key: "source", label: "DB출처", type: "text", ellipsis: { width: 100 } },
  { key: "content", label: "내용", type: "text", ellipsis: { width: 150 } },
  { key: "memo", label: "메모", type: "iconButton", icon: EyeIcon, disabled: (row)=> row.origin==='DUPLICATE' },
  { key: "status", label: "상태", type: "badge",
    editable: notDuplicate,
    options: ["부재1","부재2","부재3","부재4","부재5","재콜","가망","완료","거절"] },
  { key: "reservation", label: "예약", type: "date", editable: notDuplicate }
];

// MANAGER / STAFF 전용 컬럼
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

// 선택 핸들러
function onRowSelect(rows) {
  selectedRows.value = rows;
}

// 상태/예약 저장
function onBadgeUpdate(row, key, newValue) {
  if (row.origin === 'DUPLICATE') {
    alert('중복 DB는 수정할 수 없습니다.');
    return;
  }
  if (key === 'status') {
    axios.patch(`/api/work/db/all/update/${row.id}`, { field: key, value: newValue })
        .catch(err => {
          console.error("상태 저장 실패", err);
          alert("상태 저장 중 오류가 발생했습니다.");
        });
  }
}
async function onDateUpdate(row, key, newValue) {
  if (row.origin === 'DUPLICATE') return;
  try {
    await axios.patch(`/api/work/db/all/update/${row.id}`, { field: key, value: newValue });
  } catch (err) {
    console.error("예약일 저장 실패", err);
    alert("예약일 저장 중 오류가 발생했습니다.");
  }
}

// 메모 모달
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
function onMemoSaved(patch) {
  const arr = items.value ?? [];
  const idx = arr.findIndex(r => r.id === patch.id);
  if (idx !== -1) {
    const cur = arr[idx];
    arr.splice(idx, 1, { ...cur, status: patch.status, reservation: patch.reservation });
  } else {
    // 다른 페이지에 있을 경우만 안전 재조회
    const curPage = page.value;
    fetchData().then(() => { if (page.value !== curPage) void 0; });
  }
}

// 구분 필터 셀렉트
function onDivisionSelect({ value }) {
  // "전체" → null
  const division = (value === "전체") ? null : value;
  patchFilters({ division });
}

// ─────────────────────────────────────────────────────────────
// 상단 버튼(본사)
// - “상태별 보기”, “구분별 보기” 토글 → 서버로 텍스트 sort 전달
// - “중복DB로 이동” 처리
// ─────────────────────────────────────────────────────────────
const adminActive = ref({ status: false, division: false });
const adminActiveLabels = computed(() => {
  const arr = [];
  if (adminActive.value.status)   arr.push('상태별 보기');
  if (adminActive.value.division) arr.push('구분별 보기');
  return arr;
});

async function onAdminButtonClick(btn) {
  // 1) 중복DB로 이동
  if (btn === "중복DB로 이동") {
    const dupIds = selectedRows.value.filter(r => r.origin === 'DUPLICATE').map(r => r.id);
    if (dupIds.length === 0) {
      alert("중복 항목만 선택해서 이동할 수 있습니다.");
      return;
    }
    try {
      await axios.post("/api/lead/db/duplicate/hide", { ids: dupIds });
      alert(`중복 ${dupIds.length}건을 중복DB 메뉴로 이동(숨김)했습니다.`);

      // 선택 초기화
      selectedRows.value = [];
      tableRef.value?.clearSelection?.();
      tableKey.value++;

      await refetchAndClamp();
    } catch (err) {
      console.error("중복 이동 실패", err);
      alert("중복 이동 중 오류가 발생했습니다.");
    }
    return;
  }

  // 2) 정렬 토글
  if (btn === "상태별 보기")   adminActive.value.status   = !adminActive.value.status;
  if (btn === "구분별 보기")   adminActive.value.division = !adminActive.value.division;

  // 3) 서버로 텍스트 sort 전달 (우선순위: 구분 → 상태; 둘 다 off면 최신순)
  // 요구사항: “둘 다 on이면 1순위 구분, 2순위 상태”
  const divisionSort = adminActive.value.division ? 'on' : 'off';
  const statusSort   = adminActive.value.status   ? 'on' : 'off';
  // 백엔드에서 이 두 값을 해석해 ORDER BY를 구성(division DESC, status DESC, created_at DESC ... 식)

  await setFilters({
    ...currentFilters,
    divisionSort,
    statusSort,
    sort: (divisionSort === 'off' && statusSort === 'off') ? null : 'custom'
  });
  await loadLastPageNo()

  // 선택 초기화
  selectedRows.value = [];
  tableRef.value?.clearSelection?.();
  tableKey.value++;
}

// ─────────────────────────────────────────────────────────────
// 상단 버튼(매니저/스태프)
// ─────────────────────────────────────────────────────────────
const sortMode = ref('date'); // 'date' | 'status'
const managerButtons = computed(() => {
  const primary = sortMode.value === 'status' ? '최신순 보기' : '상태별 보기';
  const arr = [primary];
  if (isManager.value) arr.push(mineOnly.value ? '전체 보기' : '내 DB만 보기');
  return arr;
});

async function onCommonButtonClick(btn) {
  if (btn === "상태별 보기" || btn === "최신순 보기") {
    sortMode.value = (sortMode.value === 'status') ? 'date' : 'status';
    const statusSort   = (sortMode.value === 'status') ? 'on' : 'off';
    const divisionSort = 'off'; // 매니저/스태프 화면은 구분 정렬 미사용

    await setFilters({
      ...currentFilters,
      divisionSort,
      statusSort,
      sort: (statusSort === 'on') ? 'custom' : null,
      mine: mineOnly.value ? 'Y' : null,
      staffUserId: mineOnly.value ? auth.userId : null
    });

    // 선택 초기화
    selectedRows.value = [];
    tableRef.value?.clearSelection?.();
    tableKey.value++;
    return;
  }

  if (btn === "내 DB만 보기" && isManager.value) {
    mineOnly.value = true;
    await patchFilters({ mine: 'Y', staffUserId: auth.userId, sort: (sortMode.value === 'status') ? 'custom' : null });
    selectedRows.value = [];
    tableRef.value?.clearSelection?.();
    tableKey.value++;
    return;
  }

  if (btn === "전체 보기" && isManager.value) {
    mineOnly.value = false;
    await patchFilters({ mine: null, staffUserId: null, sort: (sortMode.value === 'status') ? 'custom' : null });
    selectedRows.value = [];
    tableRef.value?.clearSelection?.();
    tableKey.value++;
  }
}

// ─────────────────────────────────────────────────────────────
// PsnsTable 페이지 체인지(키셋 제어)
//  - 유연 처리: 숫자(targetPage) 또는 토큰({type})
//  - 필요한 토큰 예시: 'first' | 'prev' | 'next' | 'minus2' | 'plus2' | 'anchor200'
// ─────────────────────────────────────────────────────────────
async function changePage(payload) {
  if (typeof payload === 'number') {
    // 절대 페이지 기준
    const cur = pageDisplay.value
    if (payload === cur) return
    if (payload === 1)   return goFirst()
    const diff = payload - cur
    if (diff === 2)  return goPlus2();
    if (diff === 1)  return goNext();
    if (diff === -1) return goPrev();
    if (diff === -2) return goMinus2();
    // 큰 점프는 절대 페이지 앵커로
    return goToPageAbsolute(payload)
  }
  const type = typeof payload === 'string' ? payload : payload?.type;
  if (!type) return;
  if (type === 'first')      return goFirst();
  if (type === 'prev')       return goPrev();
  if (type === 'next')       return goNext();
  if (type === 'minus2')     return goMinus2();
  if (type === 'plus2')      return goPlus2();
  if (type === 'anchorWindow') return jumpWindow({ windowPages: payload.windowPages ?? 100 });
  if (type === 'last')       return goLast();
}

// 200+ 앵커 점프 유틸
async function handleAnchorJump(window = 200) {
  const a = await fetchAnchor({ window }); // { anchorCreatedAt, anchorId }
  await jumpByAnchor({ cursorCreatedAt: a.anchorCreatedAt, cursorId: a.anchorId });
}

// 페이지 재조회 방어 (키셋 전용)
// - 현재 페이지가 비었으면 한 칸 뒤로(가능 시)
async function refetchAndClamp() {
  await fetchData();
  if ((items.value?.length ?? 0) === 0 && page.value > 1) {
    await goPrev();
  }
}

// ─────────────────────────────────────────────────────────────
// 메모리/초기 로딩
// ─────────────────────────────────────────────────────────────
onMounted(async () => {
  await fetchData()
  await loadLastPageNo()
})
</script>

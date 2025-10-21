<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="currentPageTitle" />
    <div class="grid grid-cols-12 gap-4 min-w-0">
      <div class="col-span-12 space-y-6 min-w-0">

        <!-- SUPERADMIN (본사) -->
        <ComponentCard
            v-if="role === 'SUPERADMIN'"
            :selects="[
                ['구분 전체', '최초', '유효', '중복'],
                ['상태 전체', '부재1', '부재2', '부재3', '부재4', '부재5',
                  '재콜', '신규', '가망', '자연풀', '카피', '거절', '없음', '회수'] ]"
            :buttons="['구분별 보기', '상태별 보기', '중복DB로 이동']"
            :active="adminActiveLabels"
            :showRefresh="true"
            :refreshing="isRefreshing"
            @refresh="onRefresh"
            @changeSize="setSize"
            @selectChange="onAdminSelectChange"
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
            :selects="[['전체', '부재1', '부재2', '부재3', '부재4', '부재5', '재콜', '신규', '가망', '자연풀', '카피', '거절']]"
            :buttons=managerButtons
            :showRefresh="true"
            :refreshing="isRefreshing"
            @refresh="onRefresh"
            @changeSize="setSize"
            @selectChange="onManagerStatusSelect"
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

  <!-- 전역 로딩 오버레이 (메모 모달과 동일하게 body로 텔레포트) -->
  <Teleport to="body">
    <transition name="fade">
      <div
          v-if="showTableSpinner"
          class="fixed inset-0 z-[2147483646]"
          aria-live="polite" aria-busy="true" role="status"
      >
        <!-- 배경 -->
        <div class="absolute inset-0 bg-black/5 dark:bg-black/60"></div>

        <!-- 스피너 -->
        <div class="absolute inset-0 z-[2147483647] flex items-center justify-center">
          <div class="flex flex-col items-center gap-3">
            <svg class="animate-spin h-8 w-8 text-blue-500" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10"
                      stroke="currentColor" stroke-width="4" fill="none"/>
              <path class="opacity-75" fill="currentColor"
                    d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"/>
            </svg>
            <p class="text-sm text-gray-900 dark:text-white/90">불러오는 중…</p>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>

</template>

<script setup>
import {ref, computed, onUnmounted, watch, onMounted} from "vue";
import { useAuthStore } from "@/stores/auth.js";
import PageBreadcrumb from "@/components/common/PageBreadcrumb.vue";
import AdminLayout from "@/components/layout/AdminLayout.vue";
import ComponentCard from "@/components/common/ComponentCard.vue";
import PsnsTable from "@/components/tables/basic-tables/PsnsTable.vue";
import Memo from "@/components/ui/MEMO.vue";
import { EyeIcon } from "@heroicons/vue/24/outline";
import { useTableQuery } from "@/composables/useTableQuery.js";
import { globalFilters } from "@/composables/globalFilters.js";
import axios from "@/plugins/axios.js"

const auth = useAuthStore();
const role = auth.role;
const isManager = computed(() => role === 'MANAGER');
const mineOnly = ref(false);

const currentPageTitle = ref("전체 고객DB관리");
const tableKey = ref(0); // 선택 초기화 강제 리렌더용
const tableRef = ref(null); // PsnsTable 메서드 접근
const selectedRows = ref([]); // 선택된 행 캐시
const memoOpen = ref(false); // 메모 모달 상태
const memoRow = ref(null); // 메모 모달에 넘길 행
const isRefreshing = ref(false); // 새로고침 스피너/비활성

async function onRefresh() {
  if (isRefreshing.value) return;
  isRefreshing.value = true;
  try {
    // 수동 새로고침 (sid 기본값 1)
    const { data } = await axios.post('/api/sheets/refresh?sid=1');
    // 선택 초기화는 유지하고 싶으면 주석 해제
    // selectedRows.value = [];
    // tableRef.value?.clearSelection?.();

    // 테이블 데이터 재조회
    // 페이지 검사하며 새로고침
    await refetchAndClamp();

    // (선택) 서버 reason에 따라 토스트/알림
    // if (data?.reason === 'debounced') alert('조금 뒤에 다시 시도해주세요.');
  } catch (err) {
    console.error('수동 새로고침 실패', err);
    alert('새로고침 중 오류가 발생했습니다.');
  } finally {
    isRefreshing.value = false;
  }
}

/* =============================
   공통 useTableQuery
============================= */
const {
  items,
  page,
  size,
  totalPages,
  loading: tableLoading,
  fetchData,
  changePage,
  setSize,
  setFilter
} = useTableQuery({
  url: "/api/work/db", // 공통 API
  externalFilters: globalFilters,
  useExternalKeys: {
    from: "dateFrom",
    to: "dateTo",
    category: "category",
    keyword: "keyword",
    sort: "sort",
    mine: "mine",
    staffUserId: "staffUserId",
    status: "status",
    division: "division"
  },
  mapper: (res) => ({
    items: res.data.items,
    totalPages: res.data.totalPages,
    totalCount: res.data.totalCount
  })
});

// 로딩 오버레이 설정
const uiLoading = ref(false)
const busy = computed(() => tableLoading.value || isRefreshing.value || uiLoading.value)
const showTableSpinner = ref(false)
let delayTimer = null

async function runBusy(task) {
  if (uiLoading.value) return
  uiLoading.value = true
  try { await task() } finally { uiLoading.value = false }
}

watch(busy, (v) => {
  if (v) {
    // 짧은 로딩은 스피너 숨김
    delayTimer = setTimeout(() => { showTableSpinner.value = true }, 200)
  } else {
    if (delayTimer) { clearTimeout(delayTimer); delayTimer = null }
    showTableSpinner.value = false
  }
})

onUnmounted(() => {
  if (delayTimer) { clearTimeout(delayTimer); delayTimer = null }
})

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
  { key: "",  label: "",   type: "text", ellipsis: { width: 5 } },
  // { key: "category", label: "카테고리", type: "badge", options: ["주식", "코인"] },
  { key: "name", label: "이름", type: "text" },
  { key: "phone", label: "전화번호", type: "text", ellipsis: { width: 150 } },
  { key: "source", label: "DB출처", type: "text", ellipsis: { width: 100 } },
  { key: "content", label: "내용", type: "text", ellipsis: { width: 150 } },
  { key: "memo", label: "메모", type: "iconButton", icon: EyeIcon, disabled: (row)=> row.origin==='DUPLICATE' },
  { key: "status", label: "상태", type: "badge",
      editable: notDuplicate,
      // 회수와 신규 상태는 수동으로 줄 수 없음
      // 회수 : DB회수하기 메뉴에서
      // 신규 : 한번도 분배가 되지 않은 항목만
      options: ["부재1","부재2","부재3","부재4","부재5","재콜","가망","자연풀","카피","거절"] },
  { key: "reservation", label: "예약", type: "date", editable: notDuplicate }
];

/* =============================
   MANAGER / STAFF 전용 컬럼
============================= */
const commonColumns = [
  { key: "createdAt", label: "DB생성일", type: "text" },
  { key: "",  label: "",   type: "text", ellipsis: { width: 5 } },
  { key: "staff", label: "담당자", type: "text" },
  // { key: "category", label: "카테고리", type: "badge", options: ["주식", "코인"] },
  { key: "name", label: "이름", type: "text"},
  { key: "phone", label: "전화번호", type: "text", ellipsis: { width: 150 } },
  { key: "source", label: "DB출처", type: "text", ellipsis: { width: 100 } },
  { key: "content", label: "내용", type: "text", ellipsis: { width: 150 } },
  { key: "memo", label: "메모", type: "iconButton", icon: EyeIcon, disabled: (row)=> row.origin==='DUPLICATE' },
  { key: "status", label: "상태", type: "badge",
      editable: notDuplicate,
      options: ["부재1","부재2","부재3","부재4","부재5","재콜","가망","자연풀","카피","거절"] },
  { key: "reservation", label: "예약", type: "date", editable: notDuplicate }
];

/* =============================
   이벤트 핸들러
============================= */
function onRowSelect(rows) {
  selectedRows.value = rows;
  // console.log("선택 행:", rows);
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

  // console.log("배지 수정:", row, key, newValue);
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
    // console.log("예약일 저장 성공:", row.name, newValue)
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

function onAdminSelectChange({ idx, value }) {
  return runBusy(async () => {
    // idx: 0=구분, 1=상태
    if (idx === 0) {
      // '구분 전체'면 해제
      setFilter("division", value === "구분 전체" ? null : value);
    } else if (idx === 1) {
      // '상태 전체'면 해제
      setFilter("status", value === "상태 전체" ? null : value);
    }
    changePage(1);
  })
}

function onManagerStatusSelect({ idx, value }) {
  return runBusy(async () => {
    // 매니저/스태프는 상태 셀렉트만 있음. '전체'면 해제
    setFilter("status", value === "전체" ? null : value);
    changePage(1);
  })
}

// 버튼 토클
const adminActive = ref({ status: false, division: false })

const adminActiveLabels = computed(() => {
  const arr = []
  if (adminActive.value.status) arr.push('상태별 보기')
  if (adminActive.value.division) arr.push('구분별 보기')
  return arr
})

async function onAdminButtonClick(btn) {
  // busy 가드
  if (uiLoading.value) return;
  uiLoading.value = true;

  try {
    // 1) 먼저 중복 이동 처리
    if (btn === "중복DB로 이동") {
      const dupIds = selectedRows.value
          .filter(r => r.origin === 'DUPLICATE')
          .map(r => r.id);

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

        // 페이지 검사하며 새로고침
        await refetchAndClamp();
      } catch (err) {
        console.error("중복 이동 실패", err);
        alert("중복 이동 중 오류가 발생했습니다.");
      }
      return; // 조기 종료
    }

    // 2) 상태/구분 토글
    if (btn === "상태별 보기")   adminActive.value.status   = !adminActive.value.status;
    if (btn === "구분별 보기")   adminActive.value.division = !adminActive.value.division;

    // 3) sort 조합
    const sortParts = [];
    if (adminActive.value.status)   sortParts.push('status');
    if (adminActive.value.division) sortParts.push('division');
    setFilter("sort", sortParts.length ? sortParts.join(",") : null);

    // 선택 초기화
    selectedRows.value = [];
    tableRef.value?.clearSelection?.();
    tableKey.value++;

  } finally {
    uiLoading.value = false;
  }
}

// 정렬 모드
const sortMode = ref('date')

const managerButtons = computed(() => {
  const primary = sortMode.value === 'status' ? '최신순 보기' : '상태별 보기'
  const arr = [primary]

  // MANAGER면 "내 DB만 보기" 토글 버튼 추가, STAFF면 기존 그대로
  if (isManager.value) arr.push(mineOnly.value ? '전체 보기' : '내 DB만 보기')
  return arr
})

function onCommonButtonClick(btn) {
  if (btn === "상태별 보기" || btn === "최신순 보기") {
    sortMode.value = (sortMode.value === 'status') ? 'date' : 'status'

    setFilter("sort", sortMode.value === 'status' ? "status" : null)
    setFilter("mine", mineOnly.value ? "Y" : null)
    setFilter("staffUserId", mineOnly.value ? auth.userId : null)

    // 선택 초기화(내부/외부 모두): 테이블 메서드 + 강제리렌더 + 배열 초기화
    selectedRows.value = [];
    tableRef.value?.clearSelection?.(); // PsnsTable이 메서드 제공 시
    tableKey.value++; // 강제 리렌더로 selection state 초기화
    fetchData();
    return
  }

 // MANAGER 전용: 내 DB만 보기 / 전체 보기 토글
  if (btn === "내 DB만 보기" && isManager.value) {
    mineOnly.value = true;
    setFilter("mine", "Y");
    setFilter("staffUserId", auth.userId);
    // 현재 정렬도 유지해서 함께 적용
    setFilter("sort", sortMode.value === 'status' ? "status" : null)

    // 선택 초기화(내부/외부 모두): 테이블 메서드 + 강제리렌더 + 배열 초기화
    selectedRows.value = [];
    tableRef.value?.clearSelection?.(); // PsnsTable이 메서드 제공 시
    tableKey.value++; // 강제 리렌더로 selection state 초기화
    fetchData();
    return;
  }

  if (btn === "전체 보기" && isManager.value) {
    mineOnly.value = false;
    setFilter("mine", null);
    setFilter("staffUserId", null);
    // 정렬은 유지
    setFilter("sort", sortMode.value === 'status' ? "status" : null)

    // 선택 초기화(내부/외부 모두): 테이블 메서드 + 강제리렌더 + 배열 초기화
    selectedRows.value = [];
    tableRef.value?.clearSelection?.(); // PsnsTable이 메서드 제공 시
    tableKey.value++; // 강제 리렌더로 selection state 초기화
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

// 페이지 방어
async function refetchAndClamp() {
  await fetchData();

  // 총 페이지가 줄어들어 현재 페이지가 범위를 넘은 경우 → 마지막 페이지로 이동
  if (page.value > totalPages.value) {
    changePage(Math.max(1, totalPages.value)); // watch가 트리거되어 fetch 자동 호출
    return;
  }

  // 방어: 총 페이지 값은 맞지만, 현재 페이지에 레코드가 0개면 이전 페이지로 한 칸
  if ((items.value?.length ?? 0) === 0 && page.value > 1) {
    changePage(page.value - 1);
  }
}

onMounted(() => {
  if (isManager.value) {
    mineOnly.value = true;
    setFilter('mine', 'Y');
    setFilter('staffUserId', auth.userId);
    if (page.value !== 1) changePage(1);
  }
});

onUnmounted(() => {
  globalFilters.mine = null;
  globalFilters.staffUserId = null;
});
</script>

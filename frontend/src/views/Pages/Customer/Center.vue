<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="센터별 고객DB 조회" />
    <div class="grid grid-cols-12 gap-4 min-w-0">
      <div class="col-span-12 space-y-6 min-w-0">

        <ComponentCard
            v-if="role === 'SUPERADMIN'"
            :selects="[centerOptions]"
            :buttons="buttons"
            :showRefresh="true"
            :refreshing="loading"
            @refresh="fetchData"
            @changeSize="setSize"
            @selectChange="onSelectChange"
            @buttonClick="onButtonClick"
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

<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import AdminLayout from "@/components/layout/AdminLayout.vue";
import PageBreadcrumb from "@/components/common/PageBreadcrumb.vue";
import ComponentCard from "@/components/common/ComponentCard.vue";
import PsnsTable from "@/components/tables/basic-tables/PsnsTable.vue";
import api from "@/plugins/axios";
import { useAuthStore } from "@/stores/auth.js";

// 공용 필터/훅 (전체DB와 동일)
import { globalFilters, setGlobalFilters } from "@/composables/globalFilters.js";
import { useTableQuery } from "@/composables/useTableQuery.js";

const auth = useAuthStore();
const role = computed(() => auth.role);

// --- 센터 드롭다운 데이터 (본사 제외) ---
type C = { centerId: number; centerName: string };
const centers   = ref<C[]>([]);
const centerMap = ref<Record<string, number>>({}); // name -> id
const centerId  = ref<number|null>(null);          // 페이지 전용 필터

const todayOnly = ref(false)
const centerOptions = computed(() => ['전체', ...centers.value.map(c => c.centerName)]);

// 테이블 데이터: 공용 훅 사용
const {
  items, page, size, totalPages, loading,
  fetchData, changePage, setSize, setFilter
} = useTableQuery({
  url: "/api/work/center/db",
  pageSize: 10,
  externalFilters: globalFilters,     // 날짜/카테고리/검색은 전역필터
  useExternalKeys: {
    from: "dateFrom",
    to: "dateTo",
    // category: "category",
    keyword: "keyword",
  },
  mapper: (res) => ({
    items: res.data.items,
    totalPages: res.data.totalPages,
    totalCount: res.data.totalCount,
  }),
});

// 컬럼 고정
const columns = [
  { key: "",  label: "",   type: "text", ellipsis: { width: 10 } },
  { key: "createdAt",  label: "생성일",   type: "text", ellipsis: { width: 150 } },
  { key: "name",       label: "이름",     type: "text", ellipsis: { width: 100 } },
  { key: "phone",      label: "전화번호", type: "text", ellipsis: { width: 150 } },
  {
    key: "centerName",
    label: "센터",
    type: "badge",
    width: 100,
    render: (val: string) => `<span>${val ?? '없음'}</span>`,
  },
  { key: "",  label: "",   type: "text", ellipsis: { width: 20 } },
];

onMounted(async () => {
  await loadCenters();
  // 초기: 전체(=centerId null)
  setFilter("centerId", null);
  await fetchData();
});

async function loadCenters() {
  const { data } = await api.get("/api/work/center/centers"); // HQ 제외 반환
  centers.value = data || [];
  centerMap.value = Object.fromEntries(centers.value.map(c => [c.centerName, c.centerId]));
}

// ComponentCard select emit 핸들러
function onSelectChange({ idx, value }: { idx:number; value:string }) {
  if (idx !== 0) return; // 드롭다운 1개만 사용
  if (value === '전체') {
    centerId.value = null;
  } else {
    centerId.value = centerMap.value[value] ?? null;
  }
  setFilter("centerId", centerId.value); // 페이지 전용 필터 적용
  fetchData();
}

// 버튼 라벨 토글용
const buttons = computed(() => [
  todayOnly.value ? "전체 보기" : "오늘만 보기",
  "엑셀 다운로드",
])

// ComponentCard button emit 핸들러
async function onButtonClick(btn: string) {
  if (btn === "오늘만 보기") {
    const s = todayStr()
    setGlobalFilters({ dateFrom: s, dateTo: s }) // 오늘만
    todayOnly.value = true
    await fetchData()
    return
  }
  if (btn === "전체 보기") {
    setGlobalFilters({ dateFrom: null, dateTo: null }) // 날짜 필터 초기화
    todayOnly.value = false
    await fetchData()
    return
  }
  if (btn === '엑셀 다운로드') {
    await downloadExcel();
    return;
  }
}

// 선택된 센터 이름 계산 (없으면 빈 문자열)
const currentCenterName = computed(() => {
  const id = centerId.value
  if (id == null) return "마크CRM" // '전체'일 때 빈 문자열로
  const found = centers.value.find(c => c.centerId === id)
  return found?.centerName ?? ""
})

// yyyy-mm-dd
function todayStr() {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${dd}`;
}

// 파일명에서 불가 문자 치환 (윈도우 대비)
function sanitize(name: string) {
  return name.replace(/[\\/:*?"<>|]/g, "_");
}

// 엑셀 다운로드 (전역 + 페이지 필터 그대로 전달)
async function downloadExcel() {
  const params: any = {
    dateFrom:  globalFilters.dateFrom || undefined,
    dateTo:    globalFilters.dateTo   || undefined,
    // category:  Array.isArray(globalFilters.category)
    //     ? globalFilters.category.join(",")
    //     : (globalFilters.category || undefined),
    keyword:   (globalFilters.keyword || "").trim() || undefined,
    centerId:  centerId.value ?? undefined,
  };
  const res  = await api.get("/api/work/center/db/export", { params, responseType: "blob" });
  const blob = new Blob([res.data], { type: res.headers["content-type"] });
  const a    = document.createElement("a");
  a.href     = URL.createObjectURL(blob);

  const namePart = currentCenterName.value
  a.download = sanitize(
      namePart ? `${namePart} (${todayStr()}).xlsx` : `${todayStr()}.xlsx`
  )

  a.click();
  URL.revokeObjectURL(a.href);
}
</script>

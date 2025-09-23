<!--<template>-->
<!--  <admin-layout>-->
<!--    <div class="grid grid-cols-12 gap-4 md:gap-6">-->
<!--      <div class="col-span-12 space-y-6 xl:col-span-7">-->
<!--        &lt;!&ndash; 총 회원수 등 간략화된 지표 &ndash;&gt;-->
<!--        <ecommerce-metrics />-->
<!--      </div>-->

<!--&lt;!&ndash;      <div class="col-span-12 xl:col-span-5">&ndash;&gt;-->
<!--&lt;!&ndash;        <customer-demographic />&ndash;&gt;-->
<!--&lt;!&ndash;      </div>&ndash;&gt;-->

<!--&lt;!&ndash;      <div class="col-span-12 xl:col-span-7">&ndash;&gt;-->
<!--&lt;!&ndash;        <recent-orders />&ndash;&gt;-->
<!--&lt;!&ndash;      </div>&ndash;&gt;-->
<!--    </div>-->
<!--  </admin-layout>-->
<!--</template>-->

<!--<script>-->
<!--import AdminLayout from '../components/layout/AdminLayout.vue'-->
<!--import EcommerceMetrics from '../components/ecommerce/EcommerceMetrics.vue'-->
<!--import CustomerDemographic from '../components/ecommerce/CustomerDemographic.vue'-->
<!--import RecentOrders from '../components/ecommerce/RecentOrders.vue'-->
<!--export default {-->
<!--  components: {-->
<!--    AdminLayout,-->
<!--    EcommerceMetrics,-->
<!--    CustomerDemographic,-->
<!--    RecentOrders,-->
<!--  },-->
<!--  name: 'Ecommerce',-->
<!--}-->
<!--</script>-->

<!-- src/views/Dashboard.vue -->
<template>
  <AdminLayout>
    <!-- ===== KPI (짧은 제목) ===== -->
    <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 md:gap-6">
      <KpiCardCompact title="직원수" :value="kpi.users" delta="11.0%" trend="up">
        <template #icon>
          <svg class="fill-gray-800 dark:fill-white/90" width="24" height="24" viewBox="0 0 24 24">
            <path fill-rule="evenodd" clip-rule="evenodd"
                  d="M8.804 5.602c-1.213 0-2.197.984-2.197 2.197s.984 2.197 2.197 2.197 2.197-.984 2.197-2.197-.984-2.197-2.197-2.197Zm-3.697 2.197c0-2.042 1.655-3.697 3.697-3.697s3.697 1.655 3.697 3.697-1.655 3.697-3.697 3.697-3.697-1.655-3.697-3.697Zm-.245 7.522c-.775.767-1.159 1.74-1.346 2.54-.033.142.004.256.09.35.095.103.26.188.469.188h9.349c.209 0 .374-.085.468-.188.086-.094.123-.208.09-.35-.187-.8-.571-1.773-1.346-2.54-.756-.749-1.948-1.366-3.888-1.366s-3.132.617-3.888 1.366Z"/>
          </svg>
        </template>
      </KpiCardCompact>

      <KpiCardCompact title="센터수" :value="kpi.centers" delta="9.1%" trend="down">
        <template #icon>
          <svg class="fill-gray-800 dark:fill-white/90" width="24" height="24" viewBox="0 0 24 24">
            <path fill-rule="evenodd" clip-rule="evenodd"
                  d="M11.665 3.756c.211-.105.46-.105.671 0l6.445 3.222-6.445 3.223a.75.75 0 0 1-.671 0L5.22 6.978 11.665 3.756ZM4.293 8.192v7.903c0 .284.16.544.414.671L11.25 20.037V11.65a2.23 2.23 0 0 1-.256-.108L4.293 8.192Zm8.457 11.845 6.543-3.271a.75.75 0 0 0 .415-.671V8.192l-6.701 3.351c-.083.042-.169.078-.257.109v8.385Z"/>
          </svg>
        </template>
      </KpiCardCompact>
    </div>

    <!-- ===== 통계: 센터별 / 담당자별 ===== -->
    <div class="mt-6 grid grid-cols-12 gap-4 md:gap-6">

      <!-- ===== 센터별 ===== -->
      <div class="col-span-12 xl:col-span-6 rounded-2xl border border-gray-200 bg-white p-5 shadow-sm dark:border-gray-800 dark:bg-white/[0.03]">
        <div class="mb-3 text-lg font-semibold">센터별</div>

        <!-- 트리거/요약 -->
        <div class="mb-4 flex flex-wrap items-center gap-2">
          <button class="h-10 rounded-lg border px-3" @click="openCenterPicker = true">
            센터 선택 <span class="ml-1 text-gray-500">({{ pickedCenters.size }}개)</span>
          </button>
          <button class="h-10 rounded-lg border px-3" @click="clearCenters" :disabled="pickedCenters.size===0">초기화</button>
          <div class="flex flex-wrap gap-1">
            <span v-for="c in pickedPreview" :key="c.id"
                  class="rounded-md border px-2 py-1 text-xs text-gray-700 dark:text-gray-300">
              {{ c.name }}
            </span>
            <span v-if="pickedCenters.size > 3" class="text-xs text-gray-500">+{{ pickedCenters.size - 3 }}</span>
          </div>
        </div>

        <!-- 값 리스트 -->
        <ul class="divide-y rounded-xl border">
          <li class="flex items-center justify-between px-4 py-3">
            <span class="text-sm text-gray-600">담당자</span>
            <b class="text-base">{{ centerAgg.totalOwners.toLocaleString() }}</b>
          </li>
          <li class="flex items-center justify-between px-4 py-3">
            <span class="text-sm text-gray-600">DB(기간+중복)</span>
            <b class="text-base">{{ centerAgg.dbRangeWithDup.toLocaleString() }}</b>
          </li>
          <li class="flex items-center justify-between px-4 py-3">
            <span class="text-sm text-gray-600">DB(기간)</span>
            <b class="text-base">{{ centerAgg.dbRangeOnly.toLocaleString() }}</b>
          </li>
          <li class="flex items-center justify-between px-4 py-3">
            <span class="text-sm text-gray-600">DB(전체+중복)</span>
            <b class="text-base">{{ centerAgg.dbAllWithDup.toLocaleString() }}</b>
          </li>
          <li class="flex items-center justify-between px-4 py-3">
            <span class="text-sm text-gray-600">DB(전체)</span>
            <b class="text-base">{{ centerAgg.dbAllOnly.toLocaleString() }}</b>
          </li>
        </ul>
      </div>

      <!-- ===== 담당자별 (단일 선택) ===== -->
      <div class="col-span-12 xl:col-span-6 rounded-2xl border border-gray-200 bg-white p-5 shadow-sm dark:border-gray-800 dark:bg-white/[0.03]">
        <div class="mb-3 text-lg font-semibold">담당자별</div>

        <div class="mb-4 flex items-center gap-2">
          <input v-model="userQuery" placeholder="담당자 검색" class="h-10 w-full rounded-lg border px-3" />
          <button class="h-10 rounded-lg border px-3" @click="pickFirstUser">선택</button>
          <span v-if="pickedUser" class="rounded-md border px-2 py-1 text-xs text-gray-700 dark:text-gray-300">
            {{ pickedUser?.name }}
          </span>
        </div>

        <ul class="divide-y rounded-xl border">
          <li class="flex items-center justify-between px-4 py-3">
            <span class="text-sm text-gray-600">담당자</span>
            <b class="text-base">{{ userAgg.totalOwners.toLocaleString() }}</b>
          </li>
          <li class="flex items-center justify-between px-4 py-3">
            <span class="text-sm text-gray-600">DB(기간+중복)</span>
            <b class="text-base">{{ userAgg.dbRangeWithDup.toLocaleString() }}</b>
          </li>
          <li class="flex items-center justify-between px-4 py-3">
            <span class="text-sm text-gray-600">DB(기간)</span>
            <b class="text-base">{{ userAgg.dbRangeOnly.toLocaleString() }}</b>
          </li>
          <li class="flex items-center justify-between px-4 py-3">
            <span class="text-sm text-gray-600">DB(전체+중복)</span>
            <b class="text-base">{{ userAgg.dbAllWithDup.toLocaleString() }}</b>
          </li>
          <li class="flex items-center justify-between px-4 py-3">
            <span class="text-sm text-gray-600">DB(전체)</span>
            <b class="text-base">{{ userAgg.dbAllOnly.toLocaleString() }}</b>
          </li>
        </ul>
      </div>
    </div>

    <!-- ===== 센터 선택 모달 ===== -->
    <div v-if="openCenterPicker" class="fixed inset-0 z-[9999] flex items-center justify-center">
      <div class="absolute inset-0 bg-black/30" @click="openCenterPicker=false"></div>
      <div class="relative w-[680px] max-w-[92vw] rounded-2xl border border-gray-200 bg-white p-4 shadow-xl dark:border-gray-800 dark:bg-gray-900">
        <div class="mb-3 flex items-center justify-between">
          <div class="text-lg font-semibold">센터 선택</div>
          <button class="rounded-lg px-2 py-1 text-sm text-gray-500 hover:bg-gray-100 dark:hover:bg-white/10"
                  @click="openCenterPicker=false">닫기</button>
        </div>

        <div class="mb-3 flex items-center gap-2">
          <input v-model="centerQuery" placeholder="센터 검색"
                 class="h-10 w-full rounded-lg border px-3 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100" />
          <button class="h-10 rounded-lg border px-3" @click="selectAllFiltered">전체</button>
          <button class="h-10 rounded-lg border px-3" @click="clearCenters">해제</button>
        </div>

        <div class="max-h-[52vh] overflow-auto rounded-lg border dark:border-gray-800">
          <ul>
            <li v-for="c in filteredCenters" :key="c.id"
                class="flex items-center justify-between px-3 py-2 hover:bg-gray-50 dark:hover:bg-white/5">
              <div class="truncate">{{ c.name }}</div>
              <button
                  class="rounded-md border px-2 py-1 text-xs"
                  :class="pickedCenters.has(c.id) ? 'bg-gray-900 text-white border-gray-900 dark:bg-white dark:text-gray-900' : 'text-gray-700 dark:text-gray-300'"
                  @click="toggleCenter(c.id)">
                {{ pickedCenters.has(c.id) ? '선택됨' : '선택' }}
              </button>
            </li>
          </ul>
        </div>

        <div class="mt-3 flex items-center justify-between text-sm">
          <div class="text-gray-500 dark:text-gray-400">
            {{ filteredCenters.length.toLocaleString() }}개 결과 · 선택 {{ pickedCenters.size.toLocaleString() }}개
          </div>
          <div class="flex gap-2">
            <button class="h-10 rounded-lg border px-3" @click="openCenterPicker=false">확인</button>
          </div>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, computed } from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import KpiCardCompact from '@/components/ui/KpiCardCompact.vue'
import { globalFilters as gf } from '@/composables/globalFilters'   // 헤더(AppHeader)에서 날짜 갱신

/* ===== 더미 데이터 (API로 교체 예정) ===== */
const centers = ref([
  { id: 1, name: '수지' }, { id: 2, name: '분당' }, { id: 3, name: '강남' },
])
const users = ref([
  { id: 11, name: '홍길동', centerId: 1 },
  { id: 12, name: '김철수', centerId: 1 },
  { id: 21, name: '이영희', centerId: 2 },
  { id: 31, name: '박민수', centerId: 3 },
])
// type: '유효' | '최신' | '중복'
const dbs = ref([
  { id: 1, ownerId: 11, centerId: 1, type: '유효',  created: '2025-09-10' },
  { id: 2, ownerId: 12, centerId: 1, type: '최신',  created: '2025-09-22' },
  { id: 3, ownerId: 12, centerId: 1, type: '중복',  created: '2025-09-18' },
  { id: 4, ownerId: 21, centerId: 2, type: '유효',  created: '2025-09-01' },
  { id: 5, ownerId: 21, centerId: 2, type: '최신',  created: '2025-09-20' },
  { id: 6, ownerId: 31, centerId: 3, type: '유효',  created: '2025-08-31' },
  { id: 7, ownerId: 31, centerId: 3, type: '중복',  created: '2025-09-05' },
])

/* ===== KPI ===== */
const kpi = computed(() => ({
  users: users.value.length,
  centers: centers.value.length,
}))

/* ===== 공통: 헤더 날짜만 적용 ===== */
const inRange = (ymd) => (!gf.dateFrom || ymd >= gf.dateFrom) && (!gf.dateTo || ymd <= gf.dateTo)

/* ===== 센터 멀티 선택(대량 대응: Set) ===== */
const pickedCenters = ref(new Set([1, 2]))
const openCenterPicker = ref(false)
const centerQuery = ref('')

const filteredCenters = computed(() => {
  const q = centerQuery.value.trim().toLowerCase()
  return q ? centers.value.filter(c => c.name.toLowerCase().includes(q)) : centers.value
})
function toggleCenter(id) {
  const s = new Set(pickedCenters.value)
  s.has(id) ? s.delete(id) : s.add(id)
  pickedCenters.value = s
}
function selectAllFiltered() {
  const s = new Set(pickedCenters.value)
  for (const c of filteredCenters.value) s.add(c.id)
  pickedCenters.value = s
}
function clearCenters() { pickedCenters.value = new Set() }
const pickedPreview = computed(() => centers.value.filter(c => pickedCenters.value.has(c.id)).slice(0, 3))

/* 집계: 센터별 */
const centerAgg = computed(() => {
  const cs = pickedCenters.value
  if (cs.size === 0) return { totalOwners: 0, dbRangeWithDup: 0, dbRangeOnly: 0, dbAllWithDup: 0, dbAllOnly: 0 }
  const owners = users.value.filter(u => cs.has(u.centerId))
  const ownerIds = new Set(owners.map(o => o.id))
  const r = dbs.value.filter(x => cs.has(x.centerId) && inRange(x.created))
  const a = dbs.value.filter(x => cs.has(x.centerId))
  return {
    totalOwners: ownerIds.size,
    dbRangeWithDup: r.length,
    dbRangeOnly: r.filter(x => x.type !== '중복').length,
    dbAllWithDup: a.length,
    dbAllOnly: a.filter(x => x.type !== '중복').length,
  }
})

/* ===== 담당자 단일 선택 ===== */
const userQuery = ref('')
const pickedUserId = ref(null)
const pickedUser = computed(() => users.value.find(u => u.id === pickedUserId.value) || null)
function pickFirstUser() {
  const s = userQuery.value.trim()
  const hit = users.value.find(u => u.name.includes(s))
  pickedUserId.value = hit ? hit.id : null
}

/* 집계: 담당자별(단일) */
const userAgg = computed(() => {
  const id = pickedUserId.value
  if (!id) return { totalOwners: 0, dbRangeWithDup: 0, dbRangeOnly: 0, dbAllWithDup: 0, dbAllOnly: 0 }
  const r = dbs.value.filter(x => x.ownerId === id && inRange(x.created))
  const a = dbs.value.filter(x => x.ownerId === id)
  return {
    totalOwners: 1,
    dbRangeWithDup: r.length,
    dbRangeOnly: r.filter(x => x.type !== '중복').length,
    dbAllWithDup: a.length,
    dbAllOnly: a.filter(x => x.type !== '중복').length,
  }
})
</script>

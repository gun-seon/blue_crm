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
    <!-- KPI -->
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

    <!-- 통계: 센터별 / 담당자별 -->
    <div class="mt-6 grid grid-cols-12 gap-4 md:gap-6">

      <!-- 센터별 -->
      <div class="col-span-12 xl:col-span-6 rounded-2xl border border-gray-200 bg-white p-5 shadow-sm
                  dark:border-white/10 dark:bg-[#0f172a]">
        <div class="mb-3 text-lg font-semibold text-gray-900 dark:text-gray-100">센터별</div>

        <!-- 트리거 -->
        <div class="mb-4 flex flex-wrap items-center gap-2">
          <button
              class="h-11 rounded-lg border border-gray-300 bg-white px-4 text-sm text-gray-800 hover:bg-gray-50
                   dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100 dark:hover:bg-gray-700/60"
              @click="openCenterPicker = true">
            센터 선택 <span class="ml-1 text-gray-500 dark:text-gray-400">({{ pickedCenters.size }}개)</span>
          </button>
          <button
              class="h-11 rounded-lg border border-gray-300 bg-white px-4 text-sm text-gray-800 hover:bg-gray-50
                   disabled:opacity-50
                   dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100 dark:hover:bg-gray-700/60"
              @click="clearCenters" :disabled="pickedCenters.size===0">
            초기화
          </button>
        </div>

        <!-- 값 리스트: 구분선 다크모드 색상 지정 -->
        <ul class="divide-y divide-gray-200 rounded-xl border border-gray-200 overflow-hidden
                   dark:divide-white/10 dark:border-white/10">
          <li class="flex items-center justify-between px-4 py-3">
            <span class="text-sm text-gray-600 dark:text-gray-300">담당자</span>
            <b class="text-base text-gray-900 dark:text-gray-100">{{ centerAgg.totalOwners.toLocaleString() }}</b>
          </li>
          <li class="flex items-center justify-between px-4 py-3">
            <span class="text-sm text-gray-600 dark:text-gray-300">DB(기간+중복)</span>
            <b class="text-base text-gray-900 dark:text-gray-100">{{ centerAgg.dbRangeWithDup.toLocaleString() }}</b>
          </li>
          <li class="flex items-center justify-between px-4 py-3">
            <span class="text-sm text-gray-600 dark:text-gray-300">DB(기간)</span>
            <b class="text-base text-gray-900 dark:text-gray-100">{{ centerAgg.dbRangeOnly.toLocaleString() }}</b>
          </li>
          <li class="flex items-center justify-between px-4 py-3">
            <span class="text-sm text-gray-600 dark:text-gray-300">DB(전체+중복)</span>
            <b class="text-base text-gray-900 dark:text-gray-100">{{ centerAgg.dbAllWithDup.toLocaleString() }}</b>
          </li>
          <li class="flex items-center justify-between px-4 py-3">
            <span class="text-sm text-gray-600 dark:text-gray-300">DB(전체)</span>
            <b class="text-base text-gray-900 dark:text-gray-100">{{ centerAgg.dbAllOnly.toLocaleString() }}</b>
          </li>
        </ul>
      </div>

      <!-- 담당자별 (단일 선택) -->
      <div class="col-span-12 xl:col-span-6 rounded-2xl border border-gray-200 bg-white p-5 shadow-sm
                  dark:border-white/10 dark:bg-[#0f172a]">
        <div class="mb-3 text-lg font-semibold text-gray-900 dark:text-gray-100">담당자별</div>

        <div class="mb-4 flex items-center gap-2">
          <input
              v-model="userQuery"
              placeholder="담당자 검색"
              class="h-11 w-full rounded-lg border border-gray-300 bg-white px-3 text-sm text-gray-900 placeholder:text-gray-400
                   focus:border-gray-400 focus:outline-hidden focus:ring-3 focus:ring-gray-500/10
                   dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100 dark:placeholder:text-gray-400 dark:focus:border-gray-600" />
          <button
              class="h-11 shrink-0 rounded-lg border border-gray-300 bg-white px-4 text-sm text-gray-800 hover:bg-gray-50
                   dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100 dark:hover:bg-gray-700/60"
              @click="pickFirstUser">
            선택
          </button>
        </div>

        <!-- 여기도 구분선 색상 지정 -->
        <ul class="divide-y divide-gray-200 rounded-xl border border-gray-200 overflow-hidden
                   dark:divide-white/10 dark:border-white/10">
          <li class="flex items-center justify-between px-4 py-3">
            <span class="text-sm text-gray-600 dark:text-gray-300">담당자</span>
            <b class="text-base text-gray-900 dark:text-gray-100">{{ userAgg.totalOwners.toLocaleString() }}</b>
          </li>
          <li class="flex items-center justify-between px-4 py-3">
            <span class="text-sm text-gray-600 dark:text-gray-300">DB(기간+중복)</span>
            <b class="text-base text-gray-900 dark:text-gray-100">{{ userAgg.dbRangeWithDup.toLocaleString() }}</b>
          </li>
          <li class="flex items-center justify-between px-4 py-3">
            <span class="text-sm text-gray-600 dark:text-gray-300">DB(기간)</span>
            <b class="text-base text-gray-900 dark:text-gray-100">{{ userAgg.dbRangeOnly.toLocaleString() }}</b>
          </li>
          <li class="flex items-center justify-between px-4 py-3">
            <span class="text-sm text-gray-600 dark:text-gray-300">DB(전체+중복)</span>
            <b class="text-base text-gray-900 dark:text-gray-100">{{ userAgg.dbAllWithDup.toLocaleString() }}</b>
          </li>
          <li class="flex items-center justify-between px-4 py-3">
            <span class="text-sm text-gray-600 dark:text-gray-300">DB(전체)</span>
            <b class="text-base text-gray-900 dark:text-gray-100">{{ userAgg.dbAllOnly.toLocaleString() }}</b>
          </li>
        </ul>
      </div>
    </div>

    <!-- 센터 선택 모달 -->
    <Teleport to="body">
      <div v-if="openCenterPicker"
           class="fixed inset-0 z-[100000] flex items-center justify-center bg-black/40 p-4"
           role="dialog" aria-modal="true"
           @click.self="openCenterPicker=false">
        <div
            class="w-full max-w-[700px] rounded-2xl bg-white dark:bg-gray-900 shadow-xl
               border border-gray-200 dark:border-gray-800"
        >
          <!-- 헤더 -->
          <div class="px-5 py-4 border-b border-gray-200 dark:border-gray-800 flex items-center justify-between">
            <h3 class="text-base font-semibold text-gray-800 dark:text-gray-100">센터 선택</h3>
            <button
                class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
                @click="openCenterPicker=false"
            >✕</button>
          </div>

          <!-- 바디 -->
          <div class="px-5 py-4 space-y-4">
            <!-- 검색 -->
            <div class="flex gap-2">
              <input
                  v-model="centerQuery"
                  placeholder="센터 검색"
                  class="h-11 w-full rounded-lg border px-3
                   bg-white text-gray-800 placeholder:text-gray-400
                   focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                   dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100 dark:placeholder:text-gray-400" />
              <div class="flex gap-2">
                <button
                    class="inline-flex h-11 items-center justify-center rounded-lg border border-gray-200 px-4 text-sm
                     hover:bg-gray-50 disabled:opacity-60
                     dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100 dark:hover:bg-gray-700/60"
                    @click="selectAllFiltered">
                  <span class="whitespace-nowrap">전체</span>
                </button>
                <button
                    class="inline-flex h-11 items-center justify-center rounded-lg border border-gray-200 px-4 text-sm
                     hover:bg-gray-50 disabled:opacity-60
                     dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100 dark:hover:bg-gray-700/60"
                    @click="clearCenters">
                  <span class="whitespace-nowrap">해제</span>
                </button>
              </div>
            </div>

            <!-- 리스트 -->
            <div class="max-h-[56vh] overflow-auto border border-gray-200 dark:border-gray-800 rounded-md">
              <ul>
                <li
                    v-for="c in filteredCenters" :key="c.id"
                    @click="toggleCenter(c.id)"
                    class="flex items-center justify-between px-3 py-2 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-800"
                    :class="pickedCenters.has(c.id) ? 'bg-gray-100 dark:bg-gray-800/70' : ''"
                >
                  <div class="truncate text-sm text-gray-900 dark:text-gray-100">{{ c.name }}</div>

                  <!-- 버튼: 선택됨/선택 -->
                  <button
                      class="px-2 py-1 text-xs rounded-md border transition"
                      @click.stop="toggleCenter(c.id)"
                      :class="pickedCenters.has(c.id)
                  ? 'bg-gray-900 text-white border-gray-900 hover:bg-gray-800 dark:bg-white dark:text-gray-900 dark:border-white dark:hover:bg-gray-200'
                  : 'text-gray-700 border-gray-300 hover:bg-gray-50 dark:text-gray-300 dark:border-gray-700 dark:hover:bg-gray-800/70'">
                    {{ pickedCenters.has(c.id) ? '선택됨' : '선택' }}
                  </button>
                </li>
              </ul>
            </div>

            <!-- 선택 현황 -->
            <div class="text-sm text-gray-500 dark:text-gray-400">
              총 {{ filteredCenters.length.toLocaleString() }}개 중 선택 {{ pickedCenters.size.toLocaleString() }}개
            </div>
          </div>

          <!-- 푸터 -->
          <div class="px-5 py-3 border-t border-gray-200 dark:border-gray-800 flex items-center justify-end gap-2">
            <button
                class="inline-flex h-10 items-center justify-center rounded-lg border border-gray-200 px-4 text-sm
                 hover:bg-gray-50
                 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100 dark:hover:bg-gray-700/60"
                @click="openCenterPicker=false">
              취소
            </button>
            <button
                class="inline-flex h-10 items-center justify-center rounded-lg bg-blue-600 text-white px-4 text-sm hover:bg-blue-700 disabled:opacity-60"
                @click="openCenterPicker=false">
              확인
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </AdminLayout>
</template>

<script setup>
import { ref, computed } from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import KpiCardCompact from '@/components/ui/KpiCardCompact.vue'
import { globalFilters as gf } from '@/composables/globalFilters'

/* 더미 데이터 (API 교체 예정) */
const centers = ref([{ id:1, name:'수지' }, { id:2, name:'분당' }, { id:3, name:'강남' }])
const users = ref([
  { id:11, name:'홍길동', centerId:1 },
  { id:12, name:'김철수', centerId:1 },
  { id:21, name:'이영희', centerId:2 },
  { id:31, name:'박민수', centerId:3 },
])
// type: '유효' | '최신' | '중복'
const dbs = ref([
  { id:1, ownerId:11, centerId:1, type:'유효',  created:'2025-09-10' },
  { id:2, ownerId:12, centerId:1, type:'최신',  created:'2025-09-22' },
  { id:3, ownerId:12, centerId:1, type:'중복',  created:'2025-09-18' },
  { id:4, ownerId:21, centerId:2, type:'유효',  created:'2025-09-01' },
  { id:5, ownerId:21, centerId:2, type:'최신',  created:'2025-09-20' },
  { id:6, ownerId:31, centerId:3, type:'유효',  created:'2025-08-31' },
  { id:7, ownerId:31, centerId:3, type:'중복',  created:'2025-09-05' },
])

/* KPI */
const kpi = computed(() => ({ users: users.value.length, centers: centers.value.length }))

/* 날짜 범위 (헤더 전역 필터만 적용) */
const inRange = (ymd) => (!gf.dateFrom || ymd >= gf.dateFrom) && (!gf.dateTo || ymd <= gf.dateTo)

/* 센터 멀티 선택 */
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

/* 담당자 단일 선택 */
const userQuery = ref('')
const pickedUserId = ref(null)
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

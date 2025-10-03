<template>
  <AdminLayout>

    <!-- 본사 전용 -->
    <div v-if="isSuperAdmin">
      <!-- KPI -->
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 md:gap-6">
        <KpiCardCompact title="마크CRM 총 직원 수" :value="kpi.users" delta="11.0%" trend="up">
          <template #icon>
            <UserGroupIcon class="w-6 h-6 fill-gray-800 dark:fill-white/90" />
          </template>
        </KpiCardCompact>

        <KpiCardCompact title="마크CRM 총 센터 수" :value="kpi.centers" delta="9.1%" trend="down">
          <template #icon>
            <HomeIcon class="w-6 h-6 fill-gray-800 dark:fill-white/90" />
          </template>
        </KpiCardCompact>
      </div>

      <!-- 통계: 센터별 / 담당자별 -->
      <div class="mt-6 grid grid-cols-12 gap-4 md:gap-6">

        <!-- 센터별 -->
        <div class="col-span-12 xl:col-span-6 rounded-2xl border border-gray-200 bg-white p-5
            dark:border-gray-800 dark:bg-white/[0.03]">
          <!-- 헤더 + 버튼을 같은 행 -->
          <div class="mb-4 flex items-center justify-between">
            <div class="text-lg font-semibold text-gray-900 dark:text-gray-100">센터별</div>
            <div class="flex items-center gap-2">
              <button
                  class="h-11 shrink-0 rounded-lg border border-gray-300 bg-white px-4 text-sm text-gray-800 hover:bg-gray-50
             dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100 dark:hover:bg-gray-700/60"
                  @click="openCenterPicker = true">
                센터 선택 <span class="ml-1 text-gray-500 dark:text-gray-400">({{ pickedCenters.size }}개)</span>
              </button>
              <button
                  class="h-11 shrink-0 rounded-lg border border-gray-300 bg-white px-4 text-sm text-gray-800 hover:bg-gray-50
             disabled:opacity-50
             dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100 dark:hover:bg-gray-700/60"
                  @click="clearPickedCenters" :disabled="pickedCenters.size===0">
                초기화
              </button>
            </div>
          </div>

          <!-- 값 리스트 -->
          <ul class="divide-y divide-gray-200 rounded-xl border border-gray-200 overflow-hidden
                   dark:divide-white/10 dark:border-white/10">
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">총 담당자 수</span>
              <b class="text-base text-gray-900 dark:text-gray-100">{{ centerAgg.totalUsers.toLocaleString() }}</b>
            </li>
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">기간내 DB 갯수 (중복 포함)</span>
              <b class="text-base text-gray-900 dark:text-gray-100">{{ centerAgg.dbRangeWithDup.toLocaleString() }}</b>
            </li>
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">기간내 DB 갯수 (유효DB만)</span>
              <b class="text-base text-gray-900 dark:text-gray-100">{{ centerAgg.dbRangeOnly.toLocaleString() }}</b>
            </li>
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">전체 DB 갯수 (중복 포함)</span>
              <b class="text-base text-gray-900 dark:text-gray-100">{{ centerAgg.dbAllWithDup.toLocaleString() }}</b>
            </li>
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">전체 DB 갯수 (유효DB만)</span>
              <b class="text-base text-gray-900 dark:text-gray-100">{{ centerAgg.dbAllOnly.toLocaleString() }}</b>
            </li>
          </ul>
        </div>

        <!-- 담당자별 (단일 선택) -->
        <div class="col-span-12 xl:col-span-6 rounded-2xl border border-gray-200 bg-white p-5
            dark:border-gray-800 dark:bg-white/[0.03]">
          <!-- 제목 + 검색창 + 버튼 같은 행 -->
          <div class="mb-4 flex items-center gap-2">
            <div class="text-lg font-semibold text-gray-900 dark:text-gray-100 shrink-0 mr-2">
              담당자별
            </div>

            <!-- 검색창 + 버튼 묶음 -->
            <div class="flex items-center gap-2 flex-1">
              <input
                  v-model="userQuery"
                  placeholder="담당자 검색"
                  @keydown.enter.prevent="pickFirstUser"
                  class="h-11 w-full rounded-lg border px-3
                   bg-white text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                   dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100" />
              <button
                  type="button"
                  class="h-11 shrink-0 rounded-lg border border-gray-300 bg-white px-4 text-sm text-gray-800 hover:bg-gray-50
                    dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100 dark:hover:bg-gray-700/60"
                  @click="pickFirstUser"
                  :disabled="!canSearchUser">
                선택
              </button>
            </div>
          </div>

          <!-- 값 리스트 -->
          <ul class="divide-y divide-gray-200 rounded-xl border border-gray-200 overflow-hidden
                   dark:divide-white/10 dark:border-white/10">
            <!-- 담당자 정보 -->
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">선택된 담당자 정보</span>
              <b class="text-base text-gray-900 dark:text-gray-100">
                <template v-if="pickedUserInfo">
                  {{ pickedUserInfo.name }} ({{ pickedUserInfo.center }}, {{ roleLabel(pickedUserInfo.role) }})
                </template>
                <template v-else>
                  조회 결과 없음
                </template>
              </b>
            </li>
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">기간내 DB 갯수 (중복 포함)</span>
              <b v-if="userAggLoading" class="text-base text-gray-900 dark:text-gray-100">로딩 중 ...</b>
              <b v-else class="text-base text-gray-900 dark:text-gray-100">{{ userAgg.dbRangeWithDup.toLocaleString() }}</b>
            </li>
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">기간내 DB 갯수 (유효DB만)</span>
              <b v-if="userAggLoading" class="text-base text-gray-900 dark:text-gray-100">로딩 중 ...</b>
              <b v-else class="text-base text-gray-900 dark:text-gray-100">{{ userAgg.dbRangeOnly.toLocaleString() }}</b>
            </li>
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">전체 DB 갯수 (중복 포함)</span>
              <b v-if="userAggLoading" class="text-base text-gray-900 dark:text-gray-100">로딩 중 ...</b>
              <b v-else class="text-base text-gray-900 dark:text-gray-100">{{ userAgg.dbAllWithDup.toLocaleString() }}</b>
            </li>
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">전체 DB 갯수 (유효DB만)</span>
              <b v-if="userAggLoading" class="text-base text-gray-900 dark:text-gray-100">로딩 중 ...</b>
              <b v-else class="text-base text-gray-900 dark:text-gray-100">{{ userAgg.dbAllOnly.toLocaleString() }}</b>
            </li>
          </ul>
        </div>
      </div>
    </div>

    <!-- 센터장/담당자용 -->
    <div v-else class="mt-2 grid grid-cols-12 gap-4 md:gap-6 items-stretch">
      <!-- KPI -->
      <div class="col-span-12 xl:col-span-4 space-y-4 xl:space-y-0 xl:grid xl:grid-rows-[1fr_1fr] xl:gap-4 xl:h-full xl:min-h-0" >
        <KpiCardCompact title="마크CRM 총 직원 수" :value="kpi.users" delta="11.0%" trend="up">
          <template #icon>
            <UserGroupIcon class="w-6 h-6 fill-gray-800 dark:fill-white/90" />
          </template>
        </KpiCardCompact>

        <KpiCardCompact title="마크CRM 총 센터 수" :value="kpi.centers" delta="9.1%" trend="down">
          <template #icon>
            <HomeIcon class="w-6 h-6 fill-gray-800 dark:fill-white/90" />
          </template>
        </KpiCardCompact>
      </div>

      <!-- 통계: 담당자별 -->
      <div class="col-span-12 xl:col-span-8 xl:h-full rounded-2xl border bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03]">

        <!-- 담당자별 (단일 선택) -->
        <div class="col-span-12 xl:col-span-6 rounded-2xl border border-gray-200 bg-white p-5
            dark:border-gray-800 dark:bg-white/[0.03]">
          <!-- 제목 + 검색창 + 버튼 같은 행 -->
          <div class="mb-4 flex items-center gap-2">
            <div class="text-lg font-semibold text-gray-900 dark:text-gray-100 shrink-0 mr-2">
              담당자별
            </div>

            <!-- 검색창 + 버튼 묶음 -->
            <div class="flex items-center gap-2 flex-1">
              <input
                  v-model="userQuery"
                  placeholder="담당자 검색"
                  @keydown.enter.prevent="pickFirstUser"
                  class="h-11 w-full rounded-lg border px-3
                   bg-white text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                   dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100" />
              <button
                  type="button"
                  class="h-11 shrink-0 rounded-lg border border-gray-300 bg-white px-4 text-sm text-gray-800 hover:bg-gray-50
                    dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100 dark:hover:bg-gray-700/60"
                  @click="pickFirstUser"
                  :disabled="!canSearchUser">
                선택
              </button>
            </div>
          </div>

          <!-- 값 리스트 -->
          <ul class="divide-y divide-gray-200 rounded-xl border border-gray-200 overflow-hidden
                   dark:divide-white/10 dark:border-white/10">
            <!-- 담당자 정보 -->
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">선택된 담당자 정보</span>
              <b class="text-base text-gray-900 dark:text-gray-100">
                <template v-if="pickedUserInfo">
                  {{ pickedUserInfo.name }} ({{ pickedUserInfo.center }}, {{ roleLabel(pickedUserInfo.role) }})
                </template>
                <template v-else>
                  조회 결과 없음
                </template>
              </b>
            </li>
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">기간내 DB 갯수 (중복 포함)</span>
              <b v-if="userAggLoading" class="text-base text-gray-900 dark:text-gray-100">로딩 중 ...</b>
              <b v-else class="text-base text-gray-900 dark:text-gray-100">{{ userAgg.dbRangeWithDup.toLocaleString() }}</b>
            </li>
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">기간내 DB 갯수 (유효DB만)</span>
              <b v-if="userAggLoading" class="text-base text-gray-900 dark:text-gray-100">로딩 중 ...</b>
              <b v-else class="text-base text-gray-900 dark:text-gray-100">{{ userAgg.dbRangeOnly.toLocaleString() }}</b>
            </li>
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">전체 DB 갯수 (중복 포함)</span>
              <b v-if="userAggLoading" class="text-base text-gray-900 dark:text-gray-100">로딩 중 ...</b>
              <b v-else class="text-base text-gray-900 dark:text-gray-100">{{ userAgg.dbAllWithDup.toLocaleString() }}</b>
            </li>
            <li class="flex items-center justify-between px-4 py-3">
              <span class="text-sm text-gray-600 dark:text-gray-300">전체 DB 갯수 (유효DB만)</span>
              <b v-if="userAggLoading" class="text-base text-gray-900 dark:text-gray-100">로딩 중 ...</b>
              <b v-else class="text-base text-gray-900 dark:text-gray-100">{{ userAgg.dbAllOnly.toLocaleString() }}</b>
            </li>
          </ul>
        </div>
      </div>
    </div>

    <!-- 센터 선택 모달 -->
    <Teleport v-if="isSuperAdmin" to="body">
      <div v-if="openCenterPicker"
           class="fixed inset-0 z-[100000] flex items-center justify-center bg-black/40 p-4"
           role="dialog" aria-modal="true"
           @click.self="cancelCenters()">
        <div
            class="w-full max-w-[700px] rounded-2xl bg-white dark:bg-gray-900 shadow-xl
               border border-gray-200 dark:border-gray-800"
        >
          <!-- 헤더 -->
          <div class="px-5 py-4 border-b border-gray-200 dark:border-gray-800 flex items-center justify-between">
            <h3 class="text-base font-semibold text-gray-800 dark:text-gray-100">센터 선택</h3>
            <button
                class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
                @click="cancelCenters()"
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
                    @click="modalCenters = new Set()">
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
                    :class="modalCenters.has(c.id) ? 'bg-gray-100 dark:bg-gray-800/70' : ''"
                >
                  <div class="truncate text-sm text-gray-900 dark:text-gray-100">{{ c.name }}</div>

                  <!-- 버튼: 선택됨/선택 -->
                  <button
                      class="px-2 py-1 text-xs rounded-md border transition"
                      @click.stop="toggleCenter(c.id)"
                      :class="modalCenters.has(c.id)
                  ? 'bg-gray-900 text-white border-gray-900 hover:bg-gray-800 dark:bg-white dark:text-gray-900 dark:border-white dark:hover:bg-gray-200'
                  : 'text-gray-700 border-gray-300 hover:bg-gray-50 dark:text-gray-300 dark:border-gray-700 dark:hover:bg-gray-800/70'">
                    {{ modalCenters.has(c.id) ? '선택됨' : '선택' }}
                  </button>
                </li>
              </ul>
            </div>

            <!-- 선택 현황 -->
            <div class="text-sm text-gray-500 dark:text-gray-400">
              총 {{ filteredCenters.length.toLocaleString() }}개 중 선택 {{ modalCenters.size.toLocaleString() }}개
            </div>
          </div>

          <!-- 푸터 -->
          <div class="px-5 py-3 border-t border-gray-200 dark:border-gray-800 flex items-center justify-end gap-2">
            <button
                class="inline-flex h-10 items-center justify-center rounded-lg border border-gray-200 px-4 text-sm
                 hover:bg-gray-50
                 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100 dark:hover:bg-gray-700/60"
                @click="cancelCenters">
              취소
            </button>
            <button
                class="inline-flex h-10 items-center justify-center rounded-lg bg-blue-600 text-white px-4 text-sm hover:bg-blue-700 disabled:opacity-60"
                @click="confirmCenters">
              확인
            </button>
          </div>
        </div>
      </div>
    </Teleport>
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
import {ref, computed, onMounted, watch, onUnmounted} from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import KpiCardCompact from '@/components/ui/KpiCardCompact.vue'
import { globalFilters as gf } from '@/composables/globalFilters'
import axios from '@/plugins/axios.js'
import { useAuthStore } from '@/stores/auth'
import { UserGroupIcon, HomeIcon } from '@/icons'

const auth = useAuthStore()
const roleLabel = (r) => {
  if (r === 'SUPERADMIN') return '관리자'
  if (r === 'MANAGER')    return '센터장'
  return '담당자'
}

const isSuperAdmin = computed(() => auth.role === 'SUPERADMIN')
const myCenterId = ref(null) // 내 센터ID를 한 번만 채워서 재사용
const isScopedToCenter = computed(() => !isSuperAdmin.value && myCenterId.value != null)

const centers = ref([]) // 센터 목록 (모달 열릴 때만 채움)
const centersLoaded = ref(false)
const usersFound = ref([]) // 최근 검색 결과(상위 n명)
const dbsUser = ref([]) // 현재 선택된 "담당자"의 DB
const dbsCenters = ref([]) // 현재 선택된 "센터들"의 DB 묶음
const usersForCenters = ref([]) // 선택된 센터들의 직원 목록(본사 제외, STAFF만)

// 로딩 상태
const userAggLoading   = ref(false)
const uiLoading = ref(false)
const busy = computed(() => uiLoading.value)
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

/* KPI */
const kpiUsers = ref(0)
const kpiCenters = ref(0)
const kpi = computed(() => ({ users: kpiUsers.value, centers: kpiCenters.value }))

/* 날짜 범위 (헤더 전역 필터만 적용) */
const inRange = (ymd) => (!gf.dateFrom || ymd >= gf.dateFrom) && (!gf.dateTo || ymd <= gf.dateTo)

/* 센터 멀티 선택 */
const pickedCenters = ref(new Set())
const openCenterPicker = ref(false)
const centerQuery = ref('')
const modalCenters = ref(new Set())   // 모달 내부 임시 선택 -> 확인 버튼 클릭시 확정

// 선택된 담당자 정보 (이름/소속/권한)
const pickedUserInfo = computed(() => {
  if (!pickedUserId.value) return null
  return usersFound.value.find(u => u.id === pickedUserId.value) || null
})

const filteredCenters = computed(() => {
  const q = centerQuery.value.trim().toLowerCase()

  const list = centers.value ?? []
  return q ? list.filter(c => (c.name ?? '').toLowerCase().includes(q)) : list
})
function toggleCenter(id) {
  const s = new Set(modalCenters.value)
  s.has(id) ? s.delete(id) : s.add(id)
  modalCenters.value = s
}
function selectAllFiltered() {
  const s = new Set(modalCenters.value)
  for (const c of filteredCenters.value) s.add(c.id)
  modalCenters.value = s
}
function clearPickedCenters() {
  pickedCenters.value = new Set()
  dbsCenters.value = []
  usersForCenters.value = []
  modalCenters.value = new Set()
}

// 확인 버튼
async function confirmCenters() {
  if(!isSuperAdmin.value) return

  pickedCenters.value = new Set(modalCenters.value)

  const ids = Array.from(pickedCenters.value)

  // 확인을 눌러야 실제로 조회
  await runBusy(async () => {
    try {
      await Promise.all([
        loadCenterDbs(ids),
        loadCenterUsers(ids),
      ])
    } finally {
      openCenterPicker.value = false
    }
  })
}

/* 집계: 센터별 */
const centerAgg = computed(() => {
  const cs = pickedCenters.value
  if (cs.size === 0) return { totalUsers: 0, dbRangeWithDup: 0, dbRangeOnly: 0, dbAllWithDup: 0, dbAllOnly: 0 }

  // 선택된 센터의 직원만(본사 제외, STAFF만) — 네트워크에서 이미 필터된 데이터
  const staffInSelectedCenters = usersForCenters.value
      .filter(u => cs.has(u.centerId) && u.role === 'STAFF')
  const staffIds = new Set(staffInSelectedCenters.map(u => u.id))

  const r = dbsCenters.value.filter(x => cs.has(x.centerId) && inRange(x.created))
  const a = dbsCenters.value.filter(x => cs.has(x.centerId))
  return {
    totalUsers: staffIds.size,
    dbRangeWithDup: r.length,
    dbRangeOnly: r.filter(x => x.type !== '중복').length,
    dbAllWithDup: a.length,
    dbAllOnly: a.filter(x => x.type !== '중복').length,
  }
})

/* 담당자 단일 선택 */
const userQuery = ref('')
const pickedUserId = ref(null)
const canSearchUser = computed(() => userQuery.value.trim().length > 0)

async function pickFirstUser() {
  const s = userQuery.value.trim()
  if (!s) return

  await searchUsersExactByName(s) // 담당자명 정확히 일치해야함
  // await searchUsers(s) // 부분 일치

  const hit = usersFound.value[0]

  if (isScopedToCenter.value && hit && hit.centerId !== myCenterId.value) {
    // 다른 센터면 선택 불가
    pickedUserId.value = null
    dbsUser.value = []
    return
  }

  pickedUserId.value = hit ? hit.id : null

  // 해당 담당자 DB 로드
  userAggLoading.value = true
  try {
    if (pickedUserId.value) {
      await loadUserDbs(pickedUserId.value)
    } else {
      dbsUser.value = []
    }
  } finally {
    userAggLoading.value = false
  }
}

/* 집계: 담당자별(단일) */
const userAgg = computed(() => {
  const id = pickedUserId.value
  if (!id) return { totalUsers: 0, dbRangeWithDup: 0, dbRangeOnly: 0, dbAllWithDup: 0, dbAllOnly: 0 }
  const r = dbsUser.value.filter(x => x.userId === id && inRange(x.created))
  const a = dbsUser.value.filter(x => x.userId === id)
  return {
    totalUsers: 1,
    dbRangeWithDup: r.length,
    dbRangeOnly: r.filter(x => x.type !== '중복').length,
    dbAllWithDup: a.length,
    dbAllOnly: a.filter(x => x.type !== '중복').length,
  }
})

// 모달 닫기 버튼
function cancelCenters() {
  // 임시 선택/검색 상태 롤백
  modalCenters.value = new Set(pickedCenters.value)
  centerQuery.value = ''        // (선택) 검색어도 초기화
  openCenterPicker.value = false
}

// ---------- API helpers ----------
const normCenters = (raw = []) =>
    raw.map(r => ({ id: r.id ?? r.centerId, name: r.name ?? r.centerName }))

const normUsers = (raw = []) =>
    raw.map(r => ({
      id: r.id ?? r.userId,
      name: r.name ?? r.userName,
      email: r.email ?? r.userEmail,
      centerId: r.centerId,
      center: r.center,
      role: r.role,
    }))

const normDbs = (raw = []) =>
    raw.map(r => ({
      userId:   r.userId   ?? r.user_id,                    // 담당자 ID
      centerId: r.centerId ?? r.center_id,                  // 센터 ID (JOIN으로 내려줌)
      created:  (r.created ?? r.customer_created_at ?? r.duplicate_created_at ?? '').slice(0,10),
      // type: '중복' | '유효/최초' 등 — 서버에서 isDuplicate 또는 division으로 내려줌
      type:     r.type ?? (r.isDuplicate ? '중복' : (r.customer_division ?? '유효')),
    }))

async function ensureCentersOnce() {
  if (centersLoaded.value) return
  const res = await axios.get('/api/common/centers') // 모달 열릴 때만 호출
  centers.value = normCenters(res.data)
  centersLoaded.value = true
}

function scopeUsers(list = []) {
  return list
      .filter(u => u.centerId !== 1) // HQ 제외
      .filter(u => !isScopedToCenter.value || u.centerId === myCenterId.value) // 내 센터만
}
async function searchUsersExactByName(name) {
  const params = { name, excludeHq: true }
  // 권한 스코프(비본사면 내 센터만)
  if (isScopedToCenter.value) params.centerId = myCenterId.value

  const res = await axios.get('/api/common/dashboard/users/find', { params })
  // 안전장치로 프론트에서도 한 번 더 거르기
  usersFound.value = scopeUsers(normUsers(res.data || []))
}
async function searchUsersExactByEmail(email) {
  const params = { email, excludeHq: true }
  if (isScopedToCenter.value) params.centerId = myCenterId.value

  const res = await axios.get('/api/common/dashboard/users/find', { params })
  usersFound.value = scopeUsers(normUsers(res.data || []))
}

async function loadUserDbs(userId) {
  const params = { userId } // 전체 기간으로 한 번에

  // 권한 스코프
  if (auth.role !== 'SUPERADMIN' && auth.centerId) params.centerId = auth.centerId

  const res = await axios.get('/api/common/dashboard/customers', { params })
  dbsUser.value = normDbs(res.data)
}

async function loadCenterDbs(centerIds = []) {
  if (!Array.isArray(centerIds) || centerIds.length === 0) {
    dbsCenters.value = []
    return
  }

  const res = await axios.get('/api/common/dashboard/customers', {
    params: { centerIds: centerIds.join(',') }
  })
  dbsCenters.value = normDbs(res.data)
}
async function loadCenterUsers(centerIds = []) {
  if (!Array.isArray(centerIds) || centerIds.length === 0) {
    usersForCenters.value = []
    return
  }
  const params = {
    centerIds: centerIds.join(','), // 선택한 센터만
    excludeHq: true, // 본사 제외
    role: 'STAFF', // 담당자만(센터장 제외)
  }
  // 필요시 권한 스코프
  if (auth.role !== 'SUPERADMIN' && auth.centerId) {
    params.centerId = auth.centerId
  }
  const res = await axios.get('/api/common/dashboard/users', { params })
  usersForCenters.value = normUsers(res.data || [])
}

// ---------- lazy flows ----------
watch(openCenterPicker, async v => {
  if (v) {
    await ensureCentersOnce()
    modalCenters.value = new Set(pickedCenters.value)  // 기존 선택값으로 초기화
  }
})

onMounted(async () => {
  // KPI 로딩 (전체 직원수, 전체 센터수)
  try {
    const { data } = await axios.get('/api/common/dashboard/summary')
    kpiUsers.value = data.users ?? 0
    kpiCenters.value = data.centers ?? 0
  } catch(e) { console.warn('KPI 로드 실패', e) }

  if (auth.role === 'MANAGER' || auth.role === 'STAFF') {
    if (auth.name) {
      await searchUsersExactByName(auth.name)
    } else if (auth.email) {
      await searchUsersExactByEmail(auth.email)
    }

    const me = usersFound.value[0]

    if (me) {
      myCenterId.value = me.centerId
      pickedUserId.value = me.id
      userQuery.value = me.name || ''

      userAggLoading.value = true
      try { await loadUserDbs(me.id) }
      finally { userAggLoading.value = false }
    }

  }
})
</script>

<template>
  <div class="fixed inset-0 flex items-center justify-center z-99999">
    <!-- 배경 -->
    <div
        class="fixed inset-0 bg-black/40 dark:bg-black/60"
        @click="$emit('close')"
    ></div>

    <!-- 모달 본체 -->
    <div
        ref="modalRoot"
        class="relative bg-white dark:bg-gray-900 rounded-xl shadow-xl w-[750px] max-w-full p-6 z-10 border border-gray-200 dark:border-gray-700"
    >
      <!-- 헤더 -->
      <header class="flex justify-between items-center mb-4">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-gray-100">
          {{ row.name }} 님 메모
        </h3>
        <button
            @click="$emit('close')"
            class="text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
        >
          ✕
        </button>
      </header>

      <!-- 조회 전용 -->
      <div class="space-y-2 mb-6 text-sm text-gray-700 dark:text-gray-300">
        <div class="flex">
          <span class="w-24 font-semibold">DB생성일</span>
          <span>{{ row.createdAt }}</span>
        </div>
        <div class="flex">
          <span class="w-24 font-semibold">고객이름</span>
          <span>{{ row.name }}</span>
        </div>
        <div class="flex">
          <span class="w-24 font-semibold">전화번호</span>
          <span>{{ row.phone }}</span>
        </div>
        <div class="flex">
          <span class="w-24 font-semibold">카테고리</span>
          <span>{{ row.category }}</span>
        </div>

        <!-- SUPERADMIN 전용 -->
        <template v-if="isSuperAdmin">
          <div class="flex">
            <span class="w-24 font-semibold">구분</span>
            <span>{{ row.division || '' }}</span>
          </div>
          <div class="flex">
            <span class="w-24 font-semibold">현재 담당자</span>
            <span>{{ row.staff || '' }}</span>
          </div>
          <div class="flex">
            <span class="w-24 font-semibold mb-1">담당자 이력</span>
            <ul class="list-disc ml-5" v-if="row.staffHistory">
              <li v-for="(s, idx) in row.staffHistory || []" :key="idx">
                {{ s }}
              </li>
            </ul>
            <span v-else>없음</span>
          </div>
        </template>
        <hr class="border-gray-200 dark:border-gray-700" />
      </div>

      <!-- 수정 가능 -->
      <div class="space-y-3">
        <!-- 닉네임 -->
        <div class="mt-[-5px]">
          <label class="block mb-1 text-sm font-medium text-gray-800 dark:text-gray-200">닉네임</label>
          <input
              v-model="nickname"
              class="w-full border rounded-lg p-2 bg-white dark:bg-gray-800 border-gray-300 dark:border-gray-600 text-gray-800 dark:text-gray-100"
              placeholder="닉네임을 입력하세요"
          />
        </div>

        <!-- 메모 -->
        <div>
          <label class="block mb-1 text-sm font-medium text-gray-800 dark:text-gray-200">메모</label>
          <textarea
              v-model="memo"
              rows="4"
              class="w-full border rounded-lg p-3 bg-white dark:bg-gray-800 border-gray-300 dark:border-gray-600 text-gray-800 dark:text-gray-100"
          />
        </div>

        <!-- 상태 -->
        <div class="mt-[-5px]">
          <label class="block mb-1 text-sm font-medium text-gray-800 dark:text-gray-200">상태</label>
          <select
              v-model="status"
              class="w-full border rounded-lg p-2 bg-white dark:bg-gray-800 border-gray-300 dark:border-gray-600 text-gray-800 dark:text-gray-100"
          >
            <option value="없음">없음</option>
            <option value="신규">신규</option>
            <option value="부재1">부재1</option>
            <option value="부재2">부재2</option>
            <option value="부재3">부재3</option>
            <option value="부재4">부재4</option>
            <option value="부재5">부재5</option>
            <option value="재콜">재콜</option>
            <option value="가망">가망</option>
            <option value="완료">완료</option>
            <option value="거절">거절</option>
          </select>
        </div>

        <!-- 약속시간 -->
        <div v-if="status === '재콜'">
          <label class="block mb-1 text-sm font-medium text-gray-800 dark:text-gray-200">
            약속시간
          </label>
          <input
              ref="timepicker"
              type="text"
              class="w-full border rounded-lg p-2
         bg-white dark:bg-gray-800
         border-gray-300 dark:border-gray-600
         text-gray-800 dark:text-gray-100"
          />
        </div>

        <!-- 다중 체크박스 -->
        <div class="mt-4">
          <label class="block mb-3 text-sm font-medium text-gray-800 dark:text-gray-200">가입/이용 정보</label>
          <div class="grid grid-cols-3 gap-y-2 gap-x-4 text-gray-700 dark:text-gray-300">
            <label><input type="checkbox" value="무료방" v-model="options" /> 무료방</label>
            <label><input type="checkbox" value="시그널방" v-model="options" /> 시그널방</label>
            <label><input type="checkbox" value="거래소 가입유무" v-model="options" /> 거래소 가입유무</label>
            <label><input type="checkbox" value="트레이딩뷰 가입유무" v-model="options" /> 트레이딩뷰 가입유무</label>
            <label><input type="checkbox" value="지표 유무" v-model="options" /> 지표 유무</label>
          </div>
        </div>
      </div>

      <!-- 버튼 -->
      <footer class="flex justify-end gap-3 mt-6">
        <button
            class="px-4 py-2 bg-gray-200 dark:bg-gray-700 text-gray-800 dark:text-gray-100 rounded-md"
            @click="$emit('close')"
        >
          닫기
        </button>
        <button
            class="px-4 py-2 bg-blue-600 dark:bg-blue-500 text-white rounded-md hover:bg-blue-700 dark:hover:bg-blue-400"
            @click="save"
        >
          저장
        </button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onBeforeUnmount } from "vue"
import { useAuthStore } from "@/stores/auth.js"
import { onBeforeRouteLeave } from "vue-router"

import flatpickr from "flatpickr"
import { Korean } from "flatpickr/dist/l10n/ko.js"
import "flatpickr/dist/flatpickr.css"

const props = defineProps({ row: Object })
const emit  = defineEmits(["close"])

const auth = useAuthStore()
const isSuperAdmin = auth.role === "SUPERADMIN"

// form states
const nickname   = ref("")
const memo       = ref("")
const status     = ref("없음")
const options    = ref([])
const promiseTime = ref("")

// input ref (timepicker)
const timepicker = ref(null)

/** flatpickr instance (JS만) */
/** @type {import('flatpickr').Instance | null} */
let fpInstance = null;

function pad(n){ return String(n ?? '').padStart(2,'0'); }
function clamp(n,min,max){
  const v = parseInt(n ?? '', 10);
  if (Number.isNaN(v)) return min;
  return Math.min(max, Math.max(min, v));
}

function commitFromInputs(ins){
  const base = ins.selectedDates[0] || ins.latestSelectedDateObj || new Date();
  const ymd  = ins.formatDate(base, 'Y-m-d');
  const hh   = pad(clamp(ins.hourElement?.value,   0, 23));
  const mm   = pad(clamp(ins.minuteElement?.value, 0, 59));
  // 뭔가 입력했거나 날짜가 선택돼 있으면 커밋
  if ((ins.hourElement?.value ?? '') !== '' ||
      (ins.minuteElement?.value ?? '') !== '' ||
      ins.selectedDates.length > 0) {
    const final = `${ymd} ${hh}:${mm}`;
    promiseTime.value = final;
    ins.setDate(final, true, 'Y-m-d H:i'); // input에도 즉시 반영
  }
}

function attachCommit(ins){
  if (ins._commitAttached) return; // 중복 방지
  const h = () => commitFromInputs(ins);
  ins._commitHandler = h;
  ins.hourElement?.addEventListener('input', h);
  ins.minuteElement?.addEventListener('input', h);
  ins.hourElement?.addEventListener('blur', h);
  ins.minuteElement?.addEventListener('blur', h);
  ins._commitAttached = true;
}

function detachCommit(ins){
  const h = ins._commitHandler;
  if (!h) return;
  ins.hourElement?.removeEventListener('input', h);
  ins.minuteElement?.removeEventListener('input', h);
  ins.hourElement?.removeEventListener('blur', h);
  ins.minuteElement?.removeEventListener('blur', h);
  ins._commitHandler = null;
  ins._commitAttached = false;
}

function initTimepicker(){
  if (!timepicker.value) return;

  if (fpInstance) {
    try { detachCommit(fpInstance); fpInstance.destroy(); } catch(e){}
    fpInstance = null;
  }

  fpInstance = flatpickr(timepicker.value, {
    enableTime: true,
    noCalendar: false,
    dateFormat: 'Y-m-d H:i',
    time_24hr: true,
    allowInput: true,
    locale: Korean,

    // 처음 열릴 때 입력칸이 비어 있으면: 오늘 날짜 '선택 표시'(input은 그대로 비워둠)
    onReady: (_, __, ins) => {
      if (!promiseTime.value) ins.setDate(new Date(), false);
    },

    // 매번 열릴 때 리스너 재부착 + 오늘로 점프(입력칸 비어 있을 때만)
    onOpen: (_, __, ins) => {
      attachCommit(ins);
      if (!promiseTime.value) ins.jumpToDate(new Date());
    },

    // 팝업 닫힐 때도 강제 커밋(커서가 깜빡이는 중이어도 반영)
    onClose: (_, __, ins) => {
      commitFromInputs(ins);
    },
  });

  // 넘버패드 숫자가 방향키로 처리되지 않게 캡처 단계에서 차단
  timepicker.value.addEventListener(
      'keydown',
      (e) => { if (/[0-9:\- ]/.test(e.key)) e.stopPropagation(); },
      { capture: true }
  );
}

// 닫을 때 팝업 잔상 남지 않도록 정리
function cleanupPicker () {
  if (fpInstance) {
    try { fpInstance.close() } catch {}
    try { fpInstance.destroy() } catch {}
    fpInstance = null
  }
}

// 모달 닫기 버튼/백드롭에서 이걸 호출하도록 바꾸면 더 안전
function handleClose () {
  cleanupPicker()
  emit("close")
}

const modalRoot = ref(null)
// 부모에서 v-if로 모달 제거될 때도 안전하게 정리
onBeforeUnmount(() => { cleanupPicker() })
onBeforeRouteLeave(() => cleanupPicker())

/* ---------- 데이터 바인딩 ---------- */

// row 들어오면 값 세팅 (입력칸은 DB값이 있을 때만 세팅)
watch(
    () => props.row,
    async (v) => {
      if (!v) return
      nickname.value   = ""
      memo.value       = v.memo || ""
      status.value     = v.status || "없음"
      promiseTime.value = v.promiseTime || ""
      options.value    = v.options || []
    },
    { immediate: true }
)

// status 또는 input DOM 등장 후에만 flatpickr 초기화 (렌더 이후 실행)
watch(
    () => [status.value, timepicker.value],
    async ([st, el]) => {
      if (st !== "재콜" || !el) { cleanupPicker(); return }
      await nextTick()
      initTimepicker()
    },
    { immediate: true, flush: "post" }
)
</script>

<style>
/* 모달 안에 뜨는 달력 강제 최소 크기 */
.flatpickr-calendar.hasTime {
  width: 350px !important;
}

</style>

<style>
/* 다크모드: flatpickr 시간 선택 */
.dark .flatpickr-time {
  background: #1f2937 !important;   /* gray-800 */
  border-top: 1px solid #374151 !important; /* gray-700 */
}

.dark .flatpickr-time input.flatpickr-hour,
.dark .flatpickr-time input.flatpickr-minute {
  background: #111827 !important;   /* gray-900 */
  color: #f9fafb !important;        /* gray-50 */
  border: 1px solid #4b5563 !important; /* gray-600 */
  border-radius: 6px;
}

.dark .flatpickr-time .numInputWrapper {
  background: transparent !important;
  color: #f9fafb !important;
}

.dark .flatpickr-time .numInputWrapper span.arrowUp:after {
  border-bottom-color: #f9fafb !important;
}

.dark .flatpickr-time .numInputWrapper span.arrowDown:after {
  border-top-color: #f9fafb !important;
}

/* 다크모드: 시간 구분자 (:) */
.dark .flatpickr-time-separator {
  color: #f9fafb !important;
  font-weight: 500;
}
</style>
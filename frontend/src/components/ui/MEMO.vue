<template>
  <div class="fixed inset-0 flex items-center justify-center z-[2147483645]">
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
            <span class="w-24 font-semibold">현재 담당자</span>
            <span>{{ effectiveStaff }}</span>
          </div>
          <div class="flex items-start">
            <span class="w-24 font-semibold mb-1 shrink-0">담당자 이력</span>
            <div
                class="flex-1 max-h-20 overflow-y-auto pr-2 break-words whitespace-pre-wrap history-scroll"
            >
              <span>
                {{ effectiveHistory.length ? effectiveHistory.join(', ') : '없음' }}
              </span>
            </div>
          </div>
        </template>
        <hr class="border-gray-200 dark:border-gray-700" />
      </div>

      <!-- 수정 가능 -->
      <div class="space-y-3">
        <!-- 닉네임 2개 (가로 배치) -->
        <div class="flex gap-4">
          <div class="flex-1">
            <label class="block mb-1 text-sm font-medium text-gray-800 dark:text-gray-200">
              트레이딩뷰 ID
            </label>
            <input
                v-model="nickname1"
                class="w-full border rounded-lg p-2 bg-white dark:bg-gray-800
                focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
             border-gray-300 dark:border-gray-600 text-gray-800 dark:text-gray-100"
                placeholder="트레이딩뷰 ID 입력"
            />
          </div>

          <div class="flex-1">
            <label class="block mb-1 text-sm font-medium text-gray-800 dark:text-gray-200">
              텔레그램 닉네임
            </label>
            <input
                v-model="nickname2"
                class="w-full border rounded-lg p-2 bg-white dark:bg-gray-800
                focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
             border-gray-300 dark:border-gray-600 text-gray-800 dark:text-gray-100"
                placeholder="텔레그램 닉네임 입력"
            />
          </div>
        </div>

        <!-- 메모 -->
        <div>
          <label class="block mb-1 text-sm font-medium text-gray-800 dark:text-gray-200">메모</label>
          <textarea
              v-model="memo"
              rows="4"
              class="w-full border rounded-lg p-3 bg-white
                  focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
                  dark:bg-gray-800 border-gray-300 dark:border-gray-600 text-gray-800 dark:text-gray-100
                  resize-none overflow-y-auto h-32"
              placeholder="메모를 입력하세요"
          />
        </div>

        <!-- 상태 -->
        <div class="mt-[-5px]">
          <label class="block mb-1 text-sm font-medium text-gray-800 dark:text-gray-200">상태</label>
          <select
              v-model="status"
              class="w-full border rounded-lg p-2 bg-white dark:bg-gray-800 border-gray-300 dark:border-gray-600 text-gray-800 dark:text-gray-100
                focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10 "
          >
            <option value="부재1">부재1</option>
            <option value="부재2">부재2</option>
            <option value="부재3">부재3</option>
            <option value="부재4">부재4</option>
            <option value="부재5">부재5</option>
            <option value="재콜">재콜</option>
            <option value="가망">가망</option>
            <option value="자연풀">자연풀</option>
            <option value="카피">카피</option>
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
         focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10
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
import { ref, watch, nextTick, onBeforeUnmount, computed } from "vue"
import { useAuthStore } from "@/stores/auth.js"
import { onBeforeRouteLeave } from "vue-router"
import axios from "@/plugins/axios.js"

import flatpickr from "flatpickr"
import { Korean } from "flatpickr/dist/l10n/ko.js"
import "flatpickr/dist/flatpickr.css"

const props = defineProps({ row: Object })
const emit  = defineEmits(["close", "saved"])

const auth = useAuthStore()
const isSuperAdmin = auth.role === "SUPERADMIN"

// form states
const nickname1   = ref("")
const nickname2   = ref("")
const memo        = ref("")
const status     = ref("없음")
const options    = ref([])
const promiseTime = ref("")

const detailStaff   = ref(null)   // 현재 담당자
const staffHistory  = ref([])     // 담당자 이력

// input ref (timepicker)
const timepicker = ref(null)

/** flatpickr instance */
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
      ins.calendarContainer.style.position = "fixed";
      ins.calendarContainer.style.zIndex = "2147483648" // 모달보다 1단계 높게

      // X 버튼 추가
      if (!ins._clearButton) {
        const btn = document.createElement("button");
        btn.type = "button";
        btn.textContent = "✕";
        btn.className =
            "absolute right-3 top-[65%] -translate-y-1/2 text-gray-400 hover:text-red-500";
        btn.addEventListener("click", () => {
          promiseTime.value = "";
          ins.clear();   // flatpickr 값 지우기
        });

        ins.input.parentNode.style.position = "relative";
        ins.input.parentNode.appendChild(btn);
        ins._clearButton = btn;
      }

      // if (!promiseTime.value) ins.setDate(new Date(), false);
      // DB 값이 있으면 그걸로 세팅, 없으면 캘린더만 오늘로 포커스(인풋은 그대로 비움)
      if (promiseTime.value) {
        ins.setDate(promiseTime.value, false, 'Y-m-d H:i');
      } else {
        ins.jumpToDate(new Date());
      }
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

/* ---------- api 연결 ---------- */

// 상세 로드(모달 오픈 시)
async function fetchDetail(id) {
  if (!id) return
  try {
    const { data } = await axios.get(`/api/work/db/memo/${id}`)
    // console.log(data)
    nickname1.value   = data.tradingviewId || ""
    nickname2.value   = data.telegramNickname || ""
    memo.value        = data.memo || ""
    status.value      = data.status || "없음"
    promiseTime.value = data.promiseTime ? String(data.promiseTime).replace("T"," ").slice(0,16) : "" // 'YYYY-MM-DD HH:mm'
    detailStaff.value  = data.staff ?? null
    staffHistory.value = Array.isArray(data.staffHistory) ? data.staffHistory : []
    const opts = []
    if ((data.freeRoom|0) === 1)           opts.push("무료방")
    if ((data.signalRoom|0) === 1)         opts.push("시그널방")
    if ((data.exchangeJoined|0) === 1)     opts.push("거래소 가입유무")
    if ((data.tradingviewJoined|0) === 1)  opts.push("트레이딩뷰 가입유무")
    if ((data.indicatorFlag|0) === 1)      opts.push("지표 유무")
    options.value = opts
  } catch (e) {
    console.error(e)
    alert("메모 상세 조회 중 오류가 발생했습니다.")
  }
}

// 화면에 표시할 값(상세값 우선, 없으면 부모 row 폴백)
const effectiveStaff = computed(() => {
  return (detailStaff.value ?? props.row?.staff ?? "").trim() || "없음"
})
const effectiveHistory = computed(() => {
  const arr = (staffHistory.value && staffHistory.value.length
          ? staffHistory.value
          : (props.row?.staffHistory || []))
  return Array.isArray(arr) ? arr.filter(Boolean) : []
})
function toKRReservationString(s) {
  if (!s) return null;
  const [datePart, timePart] = String(s).split(' ');
  if (!datePart || !timePart) return null;
  const [Y, M, D] = datePart.split('-').map(Number);
  const [h, m]    = timePart.split(':').map(Number);
  const hh = String(h).padStart(2, '0');
  const mm = String(m).padStart(2, '0');
  return `${M}월 ${D}일 ${hh}:${mm}`;
}

// 저장
async function save() {
  if (!props.row?.id) return
  try {
    const has = (label) => options.value.includes(label)
    const normalizedPromise =
        status.value === '재콜'
            ? ((promiseTime.value || "").trim() || null)
            : null

    const body = {
      memo:              memo.value || null,
      status:            status.value || "없음",
      promiseTime:       normalizedPromise,
      tradingviewId:     nickname1.value || null,
      telegramNickname:  nickname2.value || null,
      freeRoom:          has("무료방") ? 1 : 0,
      signalRoom:        has("시그널방") ? 1 : 0,
      exchangeJoined:    has("거래소 가입유무") ? 1 : 0,
      tradingviewJoined: has("트레이딩뷰 가입유무") ? 1 : 0,
      indicatorFlag:     has("지표 유무") ? 1 : 0
    }

    await axios.patch(`/api/work/db/memo/${props.row.id}`, body)

    const reservationText =
        body.status === '재콜' && body.promiseTime
            ? toKRReservationString(body.promiseTime) // 예: '9월 8일 20:30'
            : null;

    // 부모 테이블이 국지 갱신할 수 있도록 변경분을 함께 보냄
    emit("saved", {
      id: props.row.id,
      status: body.status,
      reservation: reservationText
    })
    handleClose()
  } catch (e) {
    console.error(e)
    alert("메모 저장 중 오류가 발생했습니다.")
  }
}

/* ---------- 데이터 바인딩 ---------- */

// row.id 생기면 백엔드에서 상세 조회
watch(
    () => props.row?.id,
    (id) => { if (id) fetchDetail(id) },
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

/* 담당자 이력 스크롤 디자인(필요시) */
.history-scroll::-webkit-scrollbar {
  width: 6px;
}
.history-scroll::-webkit-scrollbar-thumb {
  background-color: rgba(107,114,128,.5); /* gray-600 정도 */
  border-radius: 9999px;
}
.dark .history-scroll::-webkit-scrollbar-thumb {
  background-color: rgba(156,163,175,.5); /* gray-400 정도 */
}
</style>

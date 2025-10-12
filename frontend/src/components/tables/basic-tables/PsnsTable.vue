<template>
  <div class="overflow-hidden rounded-xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
    <div class="max-w-full overflow-x-auto custom-scrollbar">
      <table class="w-full table-auto" ref="tableScrollHost">
        <thead>
        <tr class="border-b border-gray-200 dark:border-gray-700">
          <th v-if="showCheckbox" class="px-5 py-3 w-10">
            <input type="checkbox" :checked="allSelected" @change="toggleAll" />
          </th>

          <th v-for="(col, idx) in columns" :key="idx" class="px-3 py-3 text-left">
            <p class="font-medium text-gray-500 text-theme-xs dark:text-gray-400">{{ col.label }}</p>
          </th>
        </tr>
        </thead>

        <tbody class="divide-y divide-gray-200 dark:divide-gray-700">
        <!-- 데이터 있을 때 -->
        <tr v-for="(row, rowIndex) in data"
            key="getRowKey(row, rowIndex)"
            class="border-t border-gray-100 dark:border-gray-800">
          <!-- 체크박스 -->
          <td v-if="showCheckbox" class="px-5 py-4 sm:px-6">
            <input type="checkbox"
                   :checked="selectedRows.includes(rowIndex)" @change="toggleRow(rowIndex)"
                   :disabled="rowSelectable ? !rowSelectable(row) : false"/>
          </td>

          <!-- 동적 컬럼 -->
          <td v-for="(col, colIndex) in columns" :key="colIndex" class="px-2 py-4 min-w-0">
            <!-- 생성일 컬럼 -->
            <span
                v-if="col.key === 'createdAt'"
                class="text-gray-500 text-theme-sm dark:text-gray-400 block text-left leading-tight whitespace-nowrap"
            >
              {{ row[col.key].split(' ')[0] }} <br />
              {{ row[col.key].split(' ')[1] }}
            </span>

            <!-- 텍스트 -->
            <template v-else-if="col.type === 'text'">
              <!-- 셀 전체 클릭/호버 영역(배경 X) -->
              <button
                  class="block w-full -mx-2 -my-1 px-2 py-1 rounded focus:outline-none"
                  @click="toggleExpand(row, rowIndex, col.key)"
              >
                <!-- 회색 박스 = 텍스트 영역 -->
                <span
                    :class="isExpanded(row, rowIndex, col.key)
                    ? 'block rounded text-gray-500 text-theme-sm dark:text-gray-400 text-left leading-tight whitespace-pre-wrap break-words overflow-auto'
                    : 'block rounded text-gray-500 text-theme-sm dark:text-gray-400 text-left leading-tight truncate whitespace-nowrap overflow-hidden'"
                                :style="(() => {
                    const pref = ellipsisPx(col)
                    // 'maxWidth'가 아니라 'width'를 줘서 배경 폭=텍스트 폭으로 고정
                    const base = { width: `${pref}px` }
                    return isExpanded(row, rowIndex, col.key)
                      ? { ...base, maxHeight: '6.75rem', lineHeight: '1.25rem', paddingRight: '0.25rem' }
                      : base
                  })()"
                >
                  {{ col.key === 'staff'
                                ? (row[col.key]?.toString().trim() || '담당자 없음')
                                : (row[col.key] ?? '') }}
                </span>
              </button>
            </template>

            <!-- 배지 -->
            <div v-else-if="col.type === 'badge'" class="relative inline-block w-[60px] h-[28px]">
                <span
                    v-show="!(editState.row === rowIndex && editState.col === col.key)"
                    class="absolute inset-0 flex items-center justify-center rounded-full px-2 py-0.5 text-theme-xs font-medium cursor-pointer transition"
                    :class="[badgeClass(row[col.key]), col.editable ? 'hover:opacity-80' : '']"
                    @click.stop="
                      typeof col.editable === 'function'
                        ? (col.editable(row) && startEdit(rowIndex, col.key, row[col.key]))
                        : (col.editable ? startEdit(rowIndex, col.key, row[col.key]) : null)
                    "
                >
                  <!-- render 함수가 있으면 그것을 사용 -->
                  <span v-if="typeof col.render === 'function'" v-html="col.render(row[col.key], row)"></span>

                                <!-- 없으면 빈문자열/공백 → '미배정' 폴백 -->
                  <template v-else>
                    {{
                      (typeof row[col.key] === 'string'
                          ? row[col.key].trim()
                          : row[col.key]) || (col.emptyText ?? '미배정')
                    }}
                  </template>
                </span>

              <select
                  v-show="editState.row === rowIndex && editState.col === col.key"
                  v-model="editValue"
                  class="absolute inset-0 w-full h-full px-1 py-1 text-xs rounded-md border border-gray-300 dark:border-gray-600 focus:border-blue-500 focus:ring-blue-500 dark:bg-gray-800 dark:text-white"
                  @change="updateBadge(row, col.key)"
                  @blur="cancelEdit"
              >
                <option v-for="opt in col.options" :key="opt" :value="opt">{{ opt }}</option>
              </select>
            </div>

            <!-- 아이콘 버튼 -->
            <button
                v-else-if="col.type === 'iconButton'"
                class="p-2 transition rounded-md
                     text-gray-500 hover:text-blue-600
                     disabled:text-gray-400 disabled:hover:text-gray-300
                     disabled:opacity-50 disabled:cursor-not-allowed
                     disabled:pointer-events-none"
                :disabled="col.disabled && col.disabled(row)"
                @click="$emit('buttonClick', row, col.key)"
            >
              <component :is="col.icon" class="w-5 h-5" />
            </button>

            <!-- 날짜/시간 -->
            <div v-else-if="col.type === 'date'" class="relative h-9 min-w-[5.5rem] w-auto">
              <!-- 표시 모드 -->
              <span
                  v-if="props.data[rowIndex]?.status === '재콜'"
                  v-show="!(editState.row === rowIndex && editState.col === col.key)"
                  class="absolute inset-0 flex items-center px-2 text-xs leading-5
                       text-gray-500 dark:text-gray-400
                       rounded-md border border-transparent
                       overflow-hidden whitespace-nowrap text-ellipsis cursor-pointer bg-transparent"
                  @click.stop="startDateEdit(rowIndex, col.key, row[col.key])"
              >
                {{ row[col.key] || '없음' }}
              </span>

              <!-- 수정 모드(Flatpickr 대상 input) -->
              <input
                  v-show="editState.row === rowIndex && editState.col === col.key"
                  :ref="el => bindDateInput(rowIndex, el)"
                  type="text"
                  class="absolute inset-0 w-full h-full
                       border rounded-md px-2 text-xs leading-5
                       dark:bg-gray-800 dark:text-white
                       focus:ring-1 focus:ring-blue-500 focus:border-blue-500"
              />
            </div>
          </td>
        </tr>

        <!-- 데이터 없을 때 -->
        <tr v-if="!data || data.length === 0">
          <td :colspan="(columns.length + (showCheckbox ? 1 : 0))"
              class="px-4 py-6 text-center text-gray-500 dark:text-gray-400">
            조회된 데이터가 없습니다.
          </td>
        </tr>
        </tbody>
      </table>
    </div>

    <!-- 페이지네이션 -->
    <div class="grid grid-cols-3 items-center px-4 py-3 gap-x-35">
      <div class="flex justify-end">
        <button
            class="w-8 h-7 flex items-center justify-center text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md disabled:opacity-40"
            :disabled="page === 1"
            @click="page > 1 && $emit('changePage', page - 1)"
        >
          <ChevronLeftIcon class="w-4 h-4" />
        </button>
      </div>

      <div class="flex justify-center space-x-1">
        <button
            v-for="n in visiblePages"
            :key="n"
            :disabled="n === '...'"
            @click="n !== '...' && $emit('changePage', n)"
            :class="[
            'px-3 py-1 text-sm transition',
            n === '...' ? 'text-gray-400 cursor-default'
            : page === n ? 'bg-blue-500 text-white rounded-md'
            : 'text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md'
          ]"
        >
          {{ n }}
        </button>
      </div>

      <div class="flex justify-start">
        <button
            class="w-8 h-7 flex items-center justify-center text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md disabled:opacity-40"
            :disabled="page === totalPages"
            @click="page < totalPages && $emit('changePage', page + 1)"
        >
          <ChevronRightIcon class="w-4 h-4" />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted, onBeforeUnmount, nextTick } from "vue"
import { ChevronLeftIcon, ChevronRightIcon } from '@heroicons/vue/24/solid'

// 내용 - 툴팁 확장용
const expandedCell = ref(null)
function ellipsisPx(col) {
  const raw = (col && typeof col.ellipsis === 'object' && col.ellipsis !== null)
      ? col.ellipsis.width
      : col?.ellipsis;
  const n = (typeof raw === 'number') ? raw : Number.parseInt(raw, 10);
  return Number.isFinite(n) ? n : 100;
}

const props = defineProps({
  columns: { type: Array, required: true },
  data: { type: Array, required: true },
  showCheckbox: { type: Boolean, default: false },
  page: { type: Number, default: 1 },
  totalPages: { type: Number, default: 1 },
  rowSelectable: { type: Function, default: null },
  rowKey: { type: [String, Function], default: 'id' }
})
const emit = defineEmits(["rowSelect", "badgeUpdate", "buttonClick", "changePage", "DateUpdate"])

// 툴팁 확장 관련 함수
function getRowKey(row, idx) {
  if (typeof props.rowKey === 'function') return props.rowKey(row, idx)
  if (row && row[props.rowKey] != null) return row[props.rowKey]
  // id가 없으면 페이지-인덱스로라도 충돌 방지
  return `${props.page}-${idx}`
}
function cellKey(row, idx, colKey) {
  return `${getRowKey(row, idx)}::${colKey}`
}
function toggleExpand(row, idx, colKey) {
  const k = cellKey(row, idx, colKey)
  expandedCell.value = (expandedCell.value === k) ? null : k
}
function isExpanded(row, idx, colKey) {
  return expandedCell.value === cellKey(row, idx, colKey)
}

// 페이지 변경시 선택 초기화
watch(() => props.page, () => {
  selectedRows.value = []
  emitSelected()
  expandedCell.value = null
  cancelEdit()
})

/* 체크박스 */
const selectedRows = ref([])
const allSelected = computed(() => {
  const selectableCount = props.data
      .filter(row => (props.rowSelectable ? props.rowSelectable(row) : true)).length
  return selectableCount > 0 && selectedRows.value.length === selectableCount
})
function toggleAll(e) {
  if (e.target.checked) {
    selectedRows.value = props.data
        .map((row, idx) => ({ row, idx }))
        .filter(({ row }) => (props.rowSelectable ? props.rowSelectable(row) : true))
        .map(({ idx }) => idx)
  } else {
    selectedRows.value = []
  }
  emitSelected()
}

function toggleRow(idx) {
  if (selectedRows.value.includes(idx)) {
    selectedRows.value = selectedRows.value.filter(i => i !== idx)
  } else {
    selectedRows.value.push(idx)
  }
  emitSelected()
}
watch(() => props.page, () => (selectedRows.value = []))
function emitSelected() {
  const rows = selectedRows.value.map(i => props.data[i])
  emit("rowSelect", rows)
}
watch(() => props.page, () => { selectedRows.value = []; emitSelected() })

/* 배지 수정 */
const editState = ref({ row: null, col: null })
const editValue = ref(null)
function startEdit(rowIndex, colKey, currentValue) {
  // 다음 틱에서 편집 모드로
  setTimeout(() => {
    editState.value = { row: rowIndex, col: colKey }
    editValue.value = currentValue
  }, 0)
}
function cancelEdit() { editState.value = { row: null, col: null } }
function updateBadge(row, key) {
  row[key] = editValue.value
  emit("badgeUpdate", row, key, editValue.value)
  cancelEdit()
}

/* --- 날짜/시간: flatpickr --- */
import flatpickr from "flatpickr"
import { Korean } from "flatpickr/dist/l10n/ko.js"
import "flatpickr/dist/flatpickr.css"

const dateInputs = ref({})   // rowIndex -> element
const fpInstances = ref({})  // rowIndex -> flatpickr

function startDateEdit(rowIndex, colKey, currentValue) {
  const row = props.data[rowIndex]
  if (row?.status !== "재콜") return

  // 상태가 재콜일 경우에만 수정할 수 있도록
  setTimeout(() => {
    editState.value = { row: rowIndex, col: colKey }
    editValue.value = currentValue
  }, 0)
}

// 안전한 ref 바인더 (인라인 함수 대신 사용)
function bindDateInput(idx, el) {
  if (!dateInputs.value) dateInputs.value = {}
  if (el) dateInputs.value[idx] = el
  else if (dateInputs.value[idx]) delete dateInputs.value[idx]
}

function fmtLocal(dt) {
  const y = dt.getFullYear()
  const m = String(dt.getMonth() + 1).padStart(2, '0')
  const d = String(dt.getDate()).padStart(2, '0')
  const hh = String(dt.getHours()).padStart(2, '0')
  const mm = String(dt.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${hh}:${mm}`
}

watch(
    () => editState.value,
    async (state) => {
      if (!state) return
      const { row, col } = state
      if ((props.columns.find(c => c.key === col)?.type) !== "date" || row == null) return;

      await nextTick()
      const el = dateInputs.value && dateInputs.value[row]
      if (!el) return

      // 기존 인스턴스 정리
      if (fpInstances.value[row]) {
        try { fpInstances.value[row].destroy() } catch {}
        delete fpInstances.value[row]
      }

      fpInstances.value[row] = flatpickr(el, {
        enableTime: true,
        time_24hr: true,
        dateFormat: "n월 j일 H:i",
        locale: Korean,
        allowInput: true,
        defaultDate: props.data[row][col] || null,

        // 실시간 반영
        onReady: (_d, _str, ins) => {
          ins.open()

          // ====== 1) 시간 인풋 실시간 커밋 유지 ======
          if (!ins._commitAttached) {
            const commit = () => {
              const hh = String(parseInt(ins.hourElement?.value || 0, 10)).padStart(2, "0")
              const mm = String(parseInt(ins.minuteElement?.value || 0, 10)).padStart(2, "0")

              let base = ins.selectedDates[0]
              // 만약 clear 후라서 날짜가 없다면 → 오늘 날짜 기준으로
              if (!base) {
                base = new Date()
                // 날짜만 오늘로 잡고, 선택 배열에도 넣어줌
                ins.selectedDates = [base]
              }

              // const base = ins.selectedDates[0] || new Date()
              const ymd  = ins.formatDate(base, "n월 j일")
              const val  = `${ymd} ${hh}:${mm}`
              ins.setDate(val, false, "n월 j일 H:i")   // 값만 업데이트 → emit은 onValueUpdate가 담당
            }
            ins.hourElement?.addEventListener("input", commit)
            ins.minuteElement?.addEventListener("input", commit)
            ins._commitAttached = true
          }

          // ====== 2) 달력 팝업 내부 X ======
          if (!ins._popupClearBtn) {
            const btn = document.createElement("button")
            btn.type = "button"
            btn.textContent = "✕"
            btn.title = "입력값 지우기"
            btn.className =
                "fp-clear-btn absolute top-1 right-1 w-6 h-6 flex items-center justify-center " +
                "text-xs text-gray-500 hover:text-gray-700 rounded z-50";

            btn.addEventListener("mousedown", (e) => {
              // 팝업 닫히는 버블링 방지
              e.preventDefault()
              e.stopPropagation()

              // flatpickr 비우기
              ins.clear()

              // 중복 방지
              ins._wasCleared = true

              // 곧바로 닫기
              ins.close()
            })

            // 팝업 컨테이너는 position이 이미 잡혀있으므로 별도 변경 불필요
            ins.calendarContainer.appendChild(btn)
            ins._popupClearBtn = btn
          }

          // ====== 3) 입력창 안의 X ======
          if (!ins._clearButton) {
            const btn = document.createElement("button")
            btn.type = "button"
            btn.textContent = "✕"
            btn.title = "입력값 지우기"
            btn.className =
                "absolute left-[75%] top-1/2 -translate-y-1/2 text-gray-400 hover:text-red-500"

            btn.addEventListener("mousedown", (e) => {
              e.preventDefault();
              e.stopPropagation();

              // 1) flatpickr 비우기
              ins.clear();

              // 3) onClose에서 중복 반영 방지 플래그
              ins._wasCleared = true;

              // 4) 곧바로 닫기
              ins.close();
            });

            // input wrapper에 버튼 넣기
            el.parentNode.style.position = "relative"
            el.parentNode.appendChild(btn)
            ins._clearButton = btn
          }
        },
        onClose: (dates, _str, ins) => {
          let displayStr = ""
          let apiStr = ""

          if (ins._wasCleared) {
            // X 눌러서 지운 경우
            displayStr = ""
            apiStr = ""
          } else if (dates?.length) {
            // 정상적으로 날짜가 선택된 경우
            displayStr = (ins.input.value || "").trim()
            apiStr = ins.formatDate(dates[0], "Y-m-d H:i")
          } else {
            // clear 이후, 날짜는 없지만 시/분 필드에 값이 있는 경우
            const hh = parseInt(ins.hourElement?.value || "0", 10)
            const mm = parseInt(ins.minuteElement?.value || "0", 10)
            if (!isNaN(hh) || !isNaN(mm)) {
              const base = new Date()
              base.setHours(hh)
              base.setMinutes(mm)
              displayStr = ins.formatDate(base, "n월 j일 H:i")
              apiStr = ins.formatDate(base, "Y-m-d H:i")
            }
          }
          ins._wasCleared = false;

          // 셀 반영 + 부모에 emit
          props.data[row][col] = displayStr;
          emit("DateUpdate", props.data[row], col, apiStr);

          cancelEdit()

          if (ins._clearButton) {
            ins._clearButton.remove()
            ins._clearButton = null
          }
        }
      })
    },
    { deep: true }
)

onBeforeUnmount(() => {
  Object.values(fpInstances.value || {}).forEach(ins => { try { ins.destroy() } catch {} })
})

/* 바깥 클릭 시 편집 종료 (달력/입력/select/배지는 예외) */
function onDocMouseDown(e) {
  if (editState.value.row === null) return
  const t = e.target
  if (
      t?.closest?.(".flatpickr-calendar") ||
      t?.closest?.("input.flatpickr-input") ||
      t?.closest?.("select") ||
      t?.closest?.(".editable-badge") ||
      t?.closest?.(".fp-clear-btn")
  ) return
  cancelEdit()
}
onMounted(() => { document.addEventListener("mousedown", onDocMouseDown) })
onBeforeUnmount(() => { document.removeEventListener("mousedown", onDocMouseDown) })

/* 배지 색상 */
const badgeClass = (value) => {
  switch (value) {
    case "본사":
    case "관리자":
    case "탈퇴":
    case "거절":
    case "회수":
    case "차단":
      return "bg-[#F8D7DA] text-[#B23A48] dark:bg-[#4A1C1C]/80 dark:text-[#F28B82]/85"
    case "담당자":
    case "신규":
    case "승인":
    case "최초":
      return "bg-[#D6E4FF] text-[#1E40AF] dark:bg-[#1A2B4C] dark:text-[#8AB4F8]"
    case "센터장":
    case "유효":
    case "재콜":
      return "bg-[#D1F2D6] text-[#2F855A] dark:bg-[#1B3A2E] dark:text-[#5FA97A]"
    case "부재1":
    case "부재2":
    case "부재3":
    case "부재4":
    case "부재5":
    case "대기":
    case "미할당":
      return "bg-[#FFF3CD] text-[#B7791F] dark:bg-[#4A3A12]/60 dark:text-[#C9A94B]/80"
    case "자연풀":
      return "bg-[#D0F0F0] text-[#00796B] dark:bg-[#1B3C3C] dark:text-[#66C2A5]"
    case "카피":
      return "bg-[#E0D7F8] text-[#5B4B9A] dark:bg-[#312C5C] dark:text-[#B6A9E3]"
    case "가망":
      return "bg-[#EADBC8] text-[#8F6842] dark:bg-[#3E2C1E] dark:text-[#D4A373]/80"
    default:
      return "bg-[#E5E7EB] text-[#4B5563] dark:bg-[#374151] dark:text-[#D1D5DB]"
  }
}

/* 페이지네이션 */
const visiblePages = computed(() => {
  const total = props.totalPages
  const current = props.page
  const delta = 2
  const pages = []
  if (total <= 7) { for (let i=1;i<=total;i++) pages.push(i); return pages }
  if (current <= 3) { pages.push(1,2,3,4,5,'...',total) }
  else if (current >= total - 2) { pages.push(1,'...',total-4,total-3,total-2,total-1,total) }
  else { pages.push(1,'...',current-delta,current-1,current,current+1,current+delta,'...',total) }
  return pages
})

// 선택 해제 메서드
const selectedRowIds = ref(new Set());
function clearSelection() {
  selectedRows.value = [];
  selectedRowIds.value = new Set();
  emitSelected();
}
defineExpose({ clearSelection });

// 스크롤/휠 시 달력 모두 닫기
const tableScrollHost = ref(null)

// --- 스크롤/휠 시 테이블 내 모든 달력 강제 닫기 ---
const closeAllPickersOnScroll = (e) => {
  // 달력 위에서의 스크롤은 무시하고 싶다면 주석 해제
  // const t = e.target
  // if (t && t.closest && t.closest('.flatpickr-calendar')) return

  const map = fpInstances.value || {}
  for (const k in map) {
    const ins = map[k]
    if (ins?.isOpen) {
      try { ins.close() } catch {}
    }
  }
}

onMounted(() => {
  // 윈도우 스크롤/휠/터치
  window.addEventListener('wheel',     closeAllPickersOnScroll, { passive: true, capture: true })
  window.addEventListener('scroll',    closeAllPickersOnScroll, { passive: true, capture: true })
  window.addEventListener('touchmove', closeAllPickersOnScroll, { passive: true, capture: true })

  // 테이블 자체 스크롤(overflow 컨테이너)도 별도로 잡아줘야 함 (scroll은 버블 안 됨)
  const host = tableScrollHost.value
  if (host) {
    host.addEventListener('wheel',     closeAllPickersOnScroll, { passive: true, capture: true })
    host.addEventListener('scroll',    closeAllPickersOnScroll, { passive: true, capture: true })
    host.addEventListener('touchmove', closeAllPickersOnScroll, { passive: true, capture: true })
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('wheel',     closeAllPickersOnScroll, true)
  window.removeEventListener('scroll',    closeAllPickersOnScroll, true)
  window.removeEventListener('touchmove', closeAllPickersOnScroll, true)

  const host = tableScrollHost.value
  if (host) {
    host.removeEventListener('wheel',     closeAllPickersOnScroll, true)
    host.removeEventListener('scroll',    closeAllPickersOnScroll, true)
    host.removeEventListener('touchmove', closeAllPickersOnScroll, true)
  }
})

</script>

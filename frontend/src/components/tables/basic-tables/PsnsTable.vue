<template>
  <div
      class="overflow-hidden rounded-xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]"
  >
    <div class="max-w-full overflow-x-auto custom-scrollbar">
      <table class="min-w-full">
        <thead>
        <tr class="border-b border-gray-200 dark:border-gray-700">
          <!-- 선택 체크박스 -->
          <th v-if="showCheckbox" class="px-5 py-3 w-10">
            <input type="checkbox" :checked="allSelected" @change="toggleAll" />
          </th>

          <!-- 동적 헤더 -->
          <th
              v-for="(col, idx) in columns"
              :key="idx"
              class="px-5 py-3 text-left sm:px-6"
          >
            <p class="font-medium text-gray-500 text-theme-xs dark:text-gray-400">
              {{ col.label }}
            </p>
          </th>
        </tr>
        </thead>

        <tbody class="divide-y divide-gray-200 dark:divide-gray-700">
        <tr
            v-for="(row, rowIndex) in data"
            :key="rowIndex"
            class="border-t border-gray-100 dark:border-gray-800"
        >
          <!-- 체크박스 -->
          <td v-if="showCheckbox" class="px-5 py-4 sm:px-6">
            <input
                type="checkbox"
                :checked="selectedRows.includes(rowIndex)"
                @change="toggleRow(rowIndex)"
            />
          </td>

          <!-- 동적 컬럼 -->
          <td
              v-for="(col, colIndex) in columns"
              :key="colIndex"
              class="px-5 py-4 sm:px-6"
          >
            <!-- 텍스트 -->
            <span
                v-if="col.type === 'text'"
                :class="[
                  'text-gray-500 text-theme-sm dark:text-gray-400 block',
                  col.ellipsis
                    ? 'truncate whitespace-nowrap overflow-hidden'
                    : 'whitespace-normal break-words'
                ]"
                              :style="col.ellipsis ? { maxWidth: (typeof col.ellipsis === 'object' ? col.ellipsis.width : 150) + 'px' } : {}"
                          >
                {{ row[col.key] }}
            </span>

            <!-- 배지 (수정 가능) -->
            <div v-else-if="col.type === 'badge'" class="relative inline-block w-[65px] h-[28px]">
              <!-- 배지 표시 모드 -->
              <span
                  v-show="!(editState.row === rowIndex && editState.col === col.key)"
                  class="absolute inset-0 flex items-center justify-center rounded-full px-2 py-0.5 text-theme-xs font-medium cursor-pointer transition"
                  :class="[badgeClass(row[col.key]), col.editable ? 'hover:opacity-80' : '']"
                  @click.stop="col.editable ? startEdit(rowIndex, col.key, row[col.key]) : null"
              >
                {{ row[col.key] }}
              </span>

              <!-- 수정 모드 (select) -->
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
                class="p-2 text-gray-500 hover:text-blue-600 transition"
                @click="$emit('buttonClick', row, col.key)"
            >
              <component :is="col.icon" class="w-5 h-5" />
            </button>
          </td>
        </tr>
        </tbody>
      </table>
    </div>

    <!-- 페이지네이션 -->
    <div class="grid grid-cols-3 items-center px-4 py-3 gap-x-35">
      <!-- 이전 -->
      <div class="flex justify-end">
        <button
            class="w-8 h-7 flex items-center justify-center text-gray-600 dark:text-gray-300
             hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md disabled:opacity-40"
            :disabled="page === 1"
            @click="page > 1 && $emit('changePage', page - 1)"
        >
          <ChevronLeftIcon class="w-4 h-4" />
        </button>
      </div>

      <!-- 숫자 그룹 (중앙 정렬) -->
      <div class="flex justify-center space-x-1">

        <button
            v-for="n in visiblePages"
            :key="n"
            :disabled="n === '...'"
            @click="n !== '...' && $emit('changePage', n)"
            :class="[
        'px-3 py-1 text-sm transition',
        n === '...'
          ? 'text-gray-400 cursor-default'
          : page === n
            ? 'bg-blue-500 text-white rounded-md'
            : 'text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md'
      ]"
        >
          {{ n }}
        </button>
      </div>

      <!-- 다음 -->
      <div class="flex justify-start">
        <button
            class="w-8 h-7 flex items-center justify-center text-gray-600 dark:text-gray-300
             hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md disabled:opacity-40"
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
import { ref, watch, computed, onMounted, onBeforeUnmount } from "vue"
import { ChevronLeftIcon, ChevronRightIcon } from '@heroicons/vue/24/solid'

const props = defineProps({
  columns: { type: Array, required: true },
  data: { type: Array, required: true },
  showCheckbox: { type: Boolean, default: false },
  page: { type: Number, default: 1 },
  totalPages: { type: Number, default: 1 }
})

const emit = defineEmits(["rowSelect", "badgeUpdate", "buttonClick", "changePage"])

// 체크박스 관리
const selectedRows = ref([])
const allSelected = computed(
    () => selectedRows.value.length === props.data.length && props.data.length > 0
)
function toggleAll(e) {
  selectedRows.value = e.target.checked ? props.data.map((_, idx) => idx) : []
  emitSelected()
}
function toggleRow(idx) {
  if (selectedRows.value.includes(idx)) {
    selectedRows.value = selectedRows.value.filter((i) => i !== idx)
  } else {
    selectedRows.value.push(idx)
  }
  emitSelected()
}
watch(() => props.page, () => (selectedRows.value = []))

// 전체 선택된 행 데이터를 부모로 emit
function emitSelected() {
  const rows = selectedRows.value.map(i => props.data[i])
  emit("rowSelect", rows)
}

// 페이지 바뀔 때 선택 해제
watch(() => props.page, () => {
  selectedRows.value = []
  emitSelected()
})

// 배지 수정 관리
const editState = ref({ row: null, col: null })
const editValue = ref(null)

function startEdit(rowIndex, colKey, currentValue) {
  setTimeout(() => {
    editState.value = { row: rowIndex, col: colKey }
    editValue.value = currentValue
  }, 0)
}
function cancelEdit() {
  editState.value = { row: null, col: null }
}
function updateBadge(row, key) {
  row[key] = editValue.value
  emit("badgeUpdate", row, key, editValue.value)
  cancelEdit()
}

// 외부 클릭 시 드롭다운 취소
function handleClickOutside(e) {
  if (editState.value.row !== null) {
    const target = e.target
    if (!target.closest("select") && !target.closest(".editable-badge")) {
      cancelEdit()
    }
  }
}
onMounted(() => {
  document.addEventListener("mousedown", handleClickOutside)
})
onBeforeUnmount(() => {
  document.removeEventListener("mousedown", handleClickOutside)
})

// 배지 색상
const badgeClass = (value) => {
  switch (value) {
      // 🔴 빨강 (본사, 관리자, 탈퇴, 거절)
    case "본사":
    case "관리자":
    case "탈퇴":
    case "거절":
      return "bg-[#F8D7DA] text-[#B23A48] dark:bg-[#4A1C1C]/80 dark:text-[#F28B82]/85"

      // 🔵 파랑 (담당자, 신규, 승인, 최초)
    case "담당자":
    case "신규":
    case "승인":
    case "최초":
      return "bg-[#D6E4FF] text-[#1E40AF] dark:bg-[#1A2B4C] dark:text-[#8AB4F8]"

      // 🟢 초록 (센터장, 재콜)
    case "센터장":
    case "재콜":
      return "bg-[#D1F2D6] text-[#2F855A] dark:bg-[#1B3A2E] dark:text-[#5FA97A]"

      // 🟡 노랑 (대기, 부재1~5)
    case "부재1":
    case "부재2":
    case "부재3":
    case "부재4":
    case "부재5":
    case "대기":
    case "미할당":
      return "bg-[#FFF3CD] text-[#B7791F] dark:bg-[#4A3A12]/60 dark:text-[#C9A94B]/80"

      // 🟣 보라 (미래 확장용)
    case "now-none":
      return "bg-[#E9D8FD] text-[#6B46C1] dark:bg-[#3B2165]/70 dark:text-[#C6A5F3]/80"

      // 🟤 가망 (브론즈/코퍼)
    case "가망":
      return "bg-[#EADBC8] text-[#B5895A] dark:bg-[#3E2C1E] dark:text-[#D4A373]/80"

      // 🩶 기본값 = 회색
    default:
      return "bg-[#E5E7EB] text-[#4B5563] dark:bg-[#374151] dark:text-[#D1D5DB]"
  }

}

// 페이지네이션 계산 (1 ... 3 4 5 6 7 ... 20 형태)
const visiblePages = computed(() => {
  const total = props.totalPages
  const current = props.page
  const delta = 2
  const pages = []

  if (total <= 7) {
    // 페이지 수가 작으면 그냥 전부 출력
    for (let i = 1; i <= total; i++) pages.push(i)
    return pages
  }

  if (current <= 3) {
    // 앞부분
    pages.push(1, 2, 3, 4, 5, '...', total)
  } else if (current >= total - 2) {
    // 뒷부분
    pages.push(1, '...', total - 4, total - 3, total - 2, total - 1, total)
  } else {
    // 중간
    pages.push(
        1,
        '...',
        current - delta,
        current - 1,
        current,
        current + 1,
        current + delta,
        '...',
        total
    )
  }

  return pages
})
</script>

<style>

</style>
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
                :class="[ 'text-gray-500 text-theme-sm dark:text-gray-400',
                col.ellipsis ? `truncate whitespace-nowrap overflow-hidden block max-w-[${typeof col.ellipsis === 'object' ? col.ellipsis.width : 150}px]` : '' ]"
            >
                {{ row[col.key] }}
              </span>

            <!-- 배지 (수정 가능) -->
            <div v-else-if="col.type === 'badge'" class="relative inline-block w-[100px] h-[28px]">
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
                  class="absolute inset-0 w-full h-full px-2 py-1 text-xs rounded-md border border-gray-300 dark:border-gray-600 focus:border-blue-500 focus:ring-blue-500 dark:bg-gray-800 dark:text-white"
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
}
function toggleRow(idx) {
  if (selectedRows.value.includes(idx)) {
    selectedRows.value = selectedRows.value.filter((i) => i !== idx)
  } else {
    selectedRows.value.push(idx)
  }
}
watch(() => props.page, () => (selectedRows.value = []))

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
      // 🔴 빨강
    case "본사":
    case "관리자":
      return "bg-red-100 text-red-700 dark:bg-red-500/20 dark:text-red-400"

      // 🔵 파랑
    case "담당자":
      return "bg-blue-100 text-blue-700 dark:bg-blue-500/20 dark:text-blue-400"

      // 🟢 초록
    case "센터장":
      return "bg-green-100 text-green-700 dark:bg-green-500/20 dark:text-green-400"

      // 🟡 노랑
    case "대기":
      return "bg-amber-100 text-amber-700 dark:bg-amber-500/20 dark:text-amber-400"

      // 🟣 보라
    case "탈퇴":
      return "bg-purple-100 text-purple-700 dark:bg-purple-500/20 dark:text-purple-400"

      // 🟠 주황
    case "D":
      return "bg-orange-100 text-orange-700 dark:bg-orange-500/20 dark:text-orange-400"

      // 🩵 하늘색
    case "":
      return "bg-cyan-100 text-cyan-700 dark:bg-cyan-500/20 dark:text-cyan-400"

      // 🟤 갈색
    case "2":
      return "bg-yellow-900/20 text-yellow-900 dark:bg-yellow-900/40 dark:text-yellow-300"

      // 🌿 청록
    case "승인":
      return "bg-teal-100 text-teal-700 dark:bg-teal-500/20 dark:text-teal-400"

      // 🩶 기본값 = 회색
    default:
      return "bg-gray-100 text-gray-600 dark:bg-gray-600/30 dark:text-gray-300"
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

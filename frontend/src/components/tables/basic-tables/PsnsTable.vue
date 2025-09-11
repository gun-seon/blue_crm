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
                  'text-gray-500 text-theme-sm dark:text-gray-400',
                  col.ellipsis ? 'truncate block max-w-[150px]' : ''
                ]"
            >
                {{ row[col.key] }}
              </span>

            <!-- 배지 (수정 가능) -->
            <div v-else-if="col.type === 'badge'" class="inline-block">
              <!-- 수정 모드 -->
              <select
                  v-if="editState.row === rowIndex && editState.col === col.key"
                  v-model="editValue"
                  class="px-2 py-1 text-xs rounded-md border border-gray-300 focus:border-blue-500 focus:ring-blue-500 dark:bg-gray-800 dark:text-white"
                  @change="updateBadge(row, col.key)"
                  @blur="cancelEdit"
              >
                <option v-for="opt in col.options" :key="opt" :value="opt">
                  {{ opt }}
                </option>
              </select>

              <!-- 배지 표시 모드 -->
              <span
                  v-else
                  v-for="(badge, i) in [].concat(row[col.key])"
                  :key="i"
                  class="editable-badge"
                  @click.stop="col.editable ? startEdit(rowIndex, col.key, badge) : null"
                  :class="[
                    'rounded-full px-2 py-0.5 text-theme-xs font-medium cursor-pointer transition',
                    badgeClass(badge),
                    col.editable ? 'hover:opacity-80' : ''
                  ]"
              >
                  {{ badge }}
                </span>
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

    <!-- 페이지네이션 (숫자 + ... 형식, 구분선 제거) -->
    <div class="flex items-center justify-end px-4 py-3">
      <button
          class="px-3 py-1 text-sm border rounded disabled:opacity-50"
          :disabled="page === 1"
          @click="$emit('changePage', page - 1)"
      >
        이전
      </button>

      <div class="flex items-center mx-2 space-x-1">
        <button
            v-for="n in visiblePages"
            :key="n"
            :disabled="n === '...'"
            @click="n !== '...' && $emit('changePage', n)"
            :class="[
            'px-3 py-1 text-sm rounded',
            page === n
              ? 'bg-blue-500 text-white'
              : 'border text-gray-600 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-700',
            n === '...' ? 'cursor-default border-none' : ''
          ]"
        >
          {{ n }}
        </button>
      </div>

      <button
          class="px-3 py-1 text-sm border rounded disabled:opacity-50"
          :disabled="page === totalPages"
          @click="$emit('changePage', page + 1)"
      >
        다음
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted, onBeforeUnmount } from "vue"

const props = defineProps({
  columns: { type: Array, required: true },
  data: { type: Array, required: true },
  showCheckbox: { type: Boolean, default: false },
  page: { type: Number, default: 1 },
  totalPages: { type: Number, default: 1 }
})

const emit = defineEmits(["rowSelect", "badgeUpdate", "buttonClick", "changePage"])

// ✅ 체크박스 관리
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

// ✅ 배지 수정 관리
const editState = ref({ row: null, col: null })
const editValue = ref(null)

function startEdit(rowIndex, colKey, currentValue) {
  editState.value = { row: rowIndex, col: colKey }
  editValue.value = currentValue
}
function cancelEdit() {
  editState.value = { row: null, col: null }
}
function updateBadge(row, key) {
  row[key] = editValue.value
  emit("badgeUpdate", row, key, editValue.value)
  cancelEdit()
}

// ✅ 외부 클릭 시 드롭다운 취소
function handleClickOutside(e) {
  if (editState.value.row !== null) {
    const target = e.target
    if (!target.closest("select") && !target.closest(".editable-badge")) {
      cancelEdit()
    }
  }
}
onMounted(() => {
  document.addEventListener("click", handleClickOutside)
})
onBeforeUnmount(() => {
  document.removeEventListener("click", handleClickOutside)
})

// ✅ 배지 색상
const badgeClass = (status) => {
  switch (status) {
    case "Active":
      return "bg-green-100 text-green-700 dark:bg-green-500/20 dark:text-green-400"
    case "Pending":
      return "bg-yellow-100 text-yellow-700 dark:bg-yellow-500/20 dark:text-yellow-400"
    case "Cancel":
      return "bg-red-100 text-red-700 dark:bg-red-500/20 dark:text-red-400"
    default:
      return "bg-gray-100 text-gray-600 dark:bg-gray-600/30 dark:text-gray-300"
  }
}

// ✅ 페이지네이션 계산 (1 ... 5 6 7 ... 20)
const visiblePages = computed(() => {
  const total = props.totalPages
  const current = props.page
  const delta = 2
  const range = []
  const rangeWithDots = []
  let l

  for (let i = 1; i <= total; i++) {
    if (i === 1 || i === total || (i >= current - delta && i <= current + delta)) {
      range.push(i)
    }
  }
  for (let i of range) {
    if (l) {
      if (i - l === 2) {
        rangeWithDots.push(l + 1)
      } else if (i - l > 2) {
        rangeWithDots.push("...")
      }
    }
    rangeWithDots.push(i)
    l = i
  }
  return rangeWithDots
})
</script>

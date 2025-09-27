<template>
  <li class="space-y-2">
    <!-- 제목 바: 본사 / 본사 직원 / 센터명 -->
    <div
        v-if="isTitle(node)"
        :class="[
            'flex items-center gap-2 rounded-2xl px-4 py-2',
            node.userRole === 'HQ'
              ? 'bg-gray-300/70 text-gray-900 dark:bg-gray-700/40 dark:text-gray-200 dark:ring-gray-600'
              : 'bg-gray-200/70 text-gray-900 dark:bg-gray-800/80 dark:text-gray-300 dark:ring-gray-700'
          ]"
    >
      <button
          @click="toggle()"
          class="mr-1 w-6 h-6 inline-flex items-center justify-center rounded
               hover:bg-gray-300/60 dark:hover:bg-gray-700/60"
      >
        <span v-if="expanded">−</span>
        <span v-else>+</span>
      </button>
      <span class="font-semibold">{{ node.userName }}</span>
      <!-- 제목에는 권한/수정 버튼 없음 -->
    </div>

    <!-- 제목 노드 펼침 -->
    <template v-if="isTitle(node) && expanded">
      <!-- 섹션(제목) 자식이 있으면 재귀 -->
      <ul v-if="hasTitleChild" class="ml-6 space-y-2">
        <UserNode
            v-for="child in node.children"
            :key="keyOf(child)"
            :node="child"
            :current-user="currentUser"
            :expand-keys="expandKeys"
            @toggle="$emit('toggle', $event)"
            @edit="$emit('edit', $event)"
        />
      </ul>

      <!-- 리프(직원) 리스트 -->
      <div v-else-if="hasLeaf" class="ml-6">
        <div class="rounded-2xl bg-white/60 p-2 ring-1 ring-gray-100 dark:bg-white/[0.01] dark:ring-gray-800">
          <div
              v-for="child in node.children"
              :key="child.userId"
              :id="'user-' + child.userId"
              class="grid grid-cols-[2fr_2fr_1.3fr_auto] items-center gap-3 px-2 py-2
                   border-b last:border-b-0 border-gray-100 dark:border-gray-800"
          >
            <!-- 이름(+권한) -->
            <div class="truncate">
              <span class="font-medium text-gray-800 dark:text-gray-100">{{ child.userName }}</span>
              <span class="ml-1 text-gray-500 dark:text-gray-400">({{ roleLabel(child.userRole) }})</span>
            </div>
            <!-- 이메일 -->
            <div class="truncate text-gray-700 dark:text-gray-300">{{ child.userEmail }}</div>
            <!-- 전화 -->
            <div class="truncate text-gray-700 dark:text-gray-300">{{ child.userPhone || '-' }}</div>
            <!-- 작업 -->
            <div class="text-right">
              <button
                  :disabled="!canEdit(currentUser, child)"
                  @click="$emit('edit', child)"
                  class="px-2 py-1 text-xs rounded
                       disabled:bg-gray-300 disabled:text-gray-500
                       bg-brand-500 text-white hover:bg-brand-600"
              >
                수정
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 비어있을 때 -->
      <div v-else class="ml-6">
        <div
            class="rounded-xl border border-dashed border-gray-200 p-4 text-sm
           text-gray-500 dark:border-gray-700 dark:text-gray-400"
        >
          <template v-if="isUnassignedStaff">
            아직 센터에 배정되지 않았습니다.
          </template>
          <template v-else>
            구성원이 없습니다.
          </template>
        </div>
      </div>
    </template>

    <!-- 안전망(단독 사용자 노드가 내려올 때) -->
    <div v-else-if="!isTitle(node)" class="ml-10 flex items-center gap-3">
      <span class="font-medium">{{ node.userName }}</span>
      <span class="text-sm text-gray-500">({{ roleLabel(node.userRole) }})</span>
      <span class="text-sm text-gray-500">{{ node.userEmail }}</span>
      <span class="text-sm text-gray-500">{{ node.userPhone || '-' }}</span>
      <button
          :disabled="!canEdit(currentUser, node)"
          @click="$emit('edit', node)"
          class="ml-auto px-2 py-1 text-xs rounded
               disabled:bg-gray-300 disabled:text-gray-500 disabled:cursor-not-allowed
               bg-brand-500 text-white hover:bg-brand-600"
      >수정</button>
    </div>
  </li>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  node:         { type: Object, required: true },
  currentUser:  { type: Object, required: true }, // { userId, userEmail, userRole, centerId, isSuper }
  expandKeys:   { type: Object, required: true }  // Set<string>
})
const emit = defineEmits(['toggle','edit'])

const isTitle = (n) => ['HQ','GROUP','CENTER'].includes(n.userRole)
const keyOf   = (n) => (n.userId ? `U:${n.userId}` : `${n.userRole}:${n.userName}`)

const isUnassignedStaff = computed(() =>
    props.currentUser?.userRole === "STAFF" && !props.currentUser?.centerId
)
// const hasChildren = props.node.children && props.node.children.length > 0
// const hasTitleChild = computed(() => hasChildren && props.node.children.some(c => isTitle(c)))

const hasChildren    = computed(() => Array.isArray(props.node.children) && props.node.children.length > 0)
const hasTitleChild  = computed(() => hasChildren.value && props.node.children.some(c => isTitle(c)))
const hasLeaf        = computed(() => hasChildren.value && props.node.children.some(c => c.userId))
const isEmpty        = computed(() => isTitle(props.node) && !hasTitleChild.value && !hasLeaf.value)

/** 부모의 expandKeys(Set)만을 단일 소스로 사용 */
const expanded = computed(() => props.expandKeys.has(keyOf(props.node)))
function toggle() {
  const k = keyOf(props.node)
  emit('toggle', { key: k, open: !expanded.value })
}

function roleLabel(r){ return r==='SUPERADMIN'?'관리자':r==='MANAGER'?'센터장':r==='STAFF'?'담당자':r }

/** 프론트 UX 1차 차단 (서버가 최종 검증) */
function canEdit(viewer, target) {
  if (!viewer || !target) return false
  if (isTitle(target)) return false
  if (viewer.isSuper) return true                   // super는 모두 가능(본인 포함)
  if (viewer.userId === target.userId) return false // 본인은 금지

  const rank = { SUPERADMIN:3, MANAGER:2, STAFF:1 }
  const vr = rank[viewer.userRole] || 0
  const tr = rank[target.userRole] || 0

  // HQ/SUPERADMIN: 전체 열람, 같은 권한은 수정 불가
  if (viewer.centerId === 1 || viewer.userRole === 'SUPERADMIN') return vr > tr

  // MANAGER: 같은 센터 + 자신보다 낮은 권한(STAFF)만
  if (viewer.userRole === 'MANAGER') return viewer.centerId === target.centerId && vr > tr

  // STAFF: 불가
  return false
}
</script>

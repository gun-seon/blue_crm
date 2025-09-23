<template>
  <div class="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] md:p-6">
    <div class="flex items-center justify-center w-12 h-12 bg-gray-100 rounded-xl dark:bg-gray-800">
      <!-- 아이콘 슬롯 -->
      <slot name="icon">
        <!-- 기본 아이콘 (없어도 됨) -->
        <svg class="fill-gray-800 dark:fill-white/90" width="24" height="24" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/></svg>
      </slot>
    </div>

    <div class="mt-5 flex items-end justify-between">
      <div>
        <span class="text-sm text-gray-500 dark:text-gray-400">{{ title }}</span>
        <h4 class="mt-2 font-bold text-gray-800 text-title-sm dark:text-white/90">{{ fmt(value) }}</h4>
      </div>

      <span
          :class="['flex items-center gap-1 rounded-full py-0.5 pl-2 pr-2.5 text-sm font-medium',
                 trend === 'up' ? 'bg-success-50 text-success-600 dark:bg-success-500/15 dark:text-success-500'
                                : 'bg-error-50 text-error-600 dark:bg-error-500/15 dark:text-error-500']">
        <svg class="fill-current" width="12" height="12" viewBox="0 0 12 12">
          <path v-if="trend==='up'" fill-rule="evenodd" clip-rule="evenodd"
                d="M5.565 1.624a.75.75 0 0 1 .559-.25h.001c.192 0 .384.073.531.22l3 2.998a.75.75 0 1 1-1.06 1.061L6.873 3.932V10.125a.75.75 0 0 1-1.5 0V3.936L3.655 5.653a.75.75 0 0 1-1.06-1.061l2.97-2.968Z"/>
          <path v-else fill-rule="evenodd" clip-rule="evenodd"
                d="M6.435 10.376a.75.75 0 0 1-.56.25h-.001a.75.75 0 0 1-.53-.22l-3-2.998a.75.75 0 1 1 1.06-1.061l1.72 1.72V1.875a.75.75 0 0 1 1.5 0v5.997l1.718-1.717a.75.75 0 1 1 1.06 1.061l-2.97 2.968Z"/>
        </svg>
        {{ delta }}
      </span>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  title: { type: String, default: '' },   // 예: '직원수'
  value: { type: [Number, String], default: 0 }, // 예: 3782
  delta: { type: String, default: '' },   // 예: '11.0%'
  trend: { type: String, default: 'up' }  // 'up' | 'down'
})
const fmt = (v) => {
  const n = Number(v)
  return Number.isFinite(n) ? n.toLocaleString() : v
}
</script>
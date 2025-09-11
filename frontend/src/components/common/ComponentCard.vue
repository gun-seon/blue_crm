<template>
  <div
    :class="[
      'rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]',
      className,
    ]"
  >
    <!-- Card Header -->
    <div class="px-6 py-5">
      <h3 class="text-base font-medium text-gray-800 dark:text-white/90">
        {{ title }}
      </h3>
      <p v-if="desc" class="mt-1 text-sm text-gray-500 dark:text-gray-400">
        {{ desc }}
      </p>

      <!-- 옵션들 -->
      <div class="flex items-center gap-1 px-1 py-1">
        <!-- 고정 select (10, 20, 30) -->
        <select
            class="w-30 h-11 border border-gray-300 dark:border-gray-700 rounded-lg px-2 py-1 text-sm dark:bg-gray-800 dark:text-gray-200"
        >
          <option v-for="n in [10,20,30]" :key="n" :value="n">{{ n }}개</option>
        </select>

        <!-- 부모에서 넘겨준 select 리스트 -->
        <template v-for="(selectOptions, idx) in selects" :key="'sel-' + idx">
          <select
              class="w-30 h-11 border border-gray-300 dark:border-gray-700 rounded-lg px-2 py-1 text-sm dark:bg-gray-800 dark:text-gray-200"
          >
            <option v-for="opt in selectOptions" :key="opt" :value="opt">{{ opt }}</option>
          </select>
        </template>

        <!-- 부모에서 넘겨준 버튼 리스트 -->
        <template v-for="(btn, idx) in buttons" :key="'btn-' + idx">
          <button
              class="w-30 h-11 px-3 py-1 rounded-lg border border-gray-300 text-sm font-medium
                   text-gray-700 hover:bg-gray-100
                   dark:border-gray-700 dark:text-gray-300 dark:hover:bg-gray-700"
          >
            {{ btn }}
          </button>
        </template>
      </div>
    </div>

    <!-- Card Body -->
    <div class="px-6 pt-0 pb-5 border-gray-100 dark:border-gray-800">
      <div class="space-y-5">
        <slot></slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineProps } from 'vue'

interface Props {
  title?: string
  className?: string
  desc?: string
  selects?: string[][] // select option 문자열 배열들의 배열
  buttons?: string[] // 버튼 문자열 배열
}

defineProps<Props>()
</script>

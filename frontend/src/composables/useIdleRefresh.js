// 공용 무입력-자동새로고침 훅 (Vue 3, <script setup> 호환)
import { onMounted, onUnmounted, watch, ref } from 'vue'

export function useIdleRefresh(opts = {}) {
    const IDLE_REFRESH_MS = opts.timeoutMs ?? 3 * 60 * 1000
    const events = opts.events ?? [
        'mousemove','mousedown','keydown','scroll','wheel','touchstart','input','visibilitychange'
    ]

    let timer = null
    let detach = null
    const isRunning = ref(false)

    const refreshNow = () => window.location.reload()

    const reset = () => {
        if (timer) clearTimeout(timer)
        timer = window.setTimeout(refreshNow, IDLE_REFRESH_MS)
    }

    const handler = () => {
        // 요구사항: 백그라운드 전환도 타이머 리셋 유지
        // (원하면: if (e.type==='visibilitychange' && document.visibilityState==='hidden') return)
        reset()
    }

    const start = () => {
        if (isRunning.value) return
        events.forEach(ev => window.addEventListener(ev, handler, { passive: true }))
        reset()
        isRunning.value = true
        detach = () => events.forEach(ev => window.removeEventListener(ev, handler))
    }

    const stop = () => {
        if (timer) { clearTimeout(timer); timer = null }
        if (detach) { detach(); detach = null }
        isRunning.value = false
    }

    const maybeStart = () => {
        const en = typeof opts.enabled === 'function' ? !!opts.enabled() : (opts.enabled ?? true)
        en ? start() : stop()
    }

    onMounted(maybeStart)
    onUnmounted(stop)

    if (typeof opts.enabled === 'function') {
        watch(opts.enabled, () => maybeStart())
    }

    return { start, stop, reset, isRunning }
}
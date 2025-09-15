import { reactive } from 'vue'

/**
 * 헤더 공통 필터 (필요할 때만 값이 들어감)
 * - dateFrom/dateTo: 'YYYY-MM-DD' 문자열(또는 null)
 * - category: string | string | null
 * - keyword: string (빈 문자열이면 미사용)
 */
export const globalFilters = reactive({
    dateFrom: null,
    dateTo: null,
    category: null,   // '주식' | '코인' | null
    keyword: ''
})

/** 부분 업데이트 (넘어온 키만 반영) */
export function setGlobalFilters(p = {}) {
    if ('dateFrom' in p) globalFilters.dateFrom = p.dateFrom ?? null
    if ('dateTo'   in p) globalFilters.dateTo   = p.dateTo   ?? null
    if ('category' in p) globalFilters.category = p.category ?? null
    if ('keyword'  in p) globalFilters.keyword  = p.keyword  ?? ''
}

/** 전체 초기화(필요 시 호출) */
export function resetGlobalFilters() {
    globalFilters.dateFrom = null
    globalFilters.dateTo   = null
    globalFilters.category = null
    globalFilters.keyword  = ''
}

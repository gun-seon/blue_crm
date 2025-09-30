// src/composables/useKeysetQuery.js
// - Keyset 페이지네이션 공통 훅
// - 버튼 세트: «(처음), -2, -1, [현재], +1, +2, 200+
// - silent jump(+2 등): 중간 단계는 커서만 미리 채우고 마지막에 한 번만 렌더
// - cursorOnly=true 지원: 서버가 items 없이 nextCreatedAt/nextId만 반환
// - 필터/정렬 텍스트 → 그대로 백엔드 전달 (백엔드 서비스에서 불린/우선순위 해석)

import { ref, reactive, toRaw, computed } from 'vue'
import axios from '@/plugins/axios.js'

/**
 * @typedef {Object} UseKeysetOptions
 * @property {string} [baseUrl='/api/work/db/keyset'] - 기본 목록 API (GET)
 * @property {number} [pageSize=50]
 * @property {Object} [filters] - 전달할 필터(문자열/불린/숫자). 예) { dateFrom, dateTo, division, status, sortMode, search, mine, ... }
 *   - 정렬 정책(텍스트):
 *     * divisionSort='on' | 'off' (1순위)
 *     * statusSort='on'   | 'off' (2순위)
 *     * 둘 다 off면 최신순(created_at DESC, id DESC)
 *   - 가시권한/전화 검색 관련 파라미터도 그대로 전달(백엔드가 권한/마스킹/불가 처리)
 */
export function useKeysetQuery({
                                   baseUrl = '/api/work/db/keyset',
                                   pageSize = 50,
                                   filters = {}
                               } = /** @type {UseKeysetOptions} */({})) {

    // 렌더 데이터/상태
    const items = ref([])
    const hasNext = ref(true)
    const loading = ref(false)
    const pageIndex = ref(0) // 0-based

    // 각 페이지의 "시작 커서"를 보관한다. 첫 페이지(0)는 {at:null, id:null}
    const cursors = ref([{ at: null, id: null }])

    // 현재 필터(반응형). 외부에서 setFilters()로 변경 가능.
    const currentFilters = reactive({ ...(filters || {}) })

    const pageOffset = ref(0)
    const pageDisplay = computed(() => pageOffset.value + pageIndex.value + 1)
    const lastPageNo = ref(null)

    // 내부: 공통 fetch
    let reqSeq = 0; // 전역
    async function _fetch({ cursorOnly = false, applyItems = true, overrideCursor } = {}) {
        const mySeq = ++reqSeq;

        const params = {
            size: pageSize,
            ...toRaw(currentFilters),
        };

        // 커서 우선순위: overrideCursor(임시 커서) → 현재 페이지 커서
        const baseCur = overrideCursor ?? cursors.value[pageIndex.value] ?? { at: null, id: null };
        if (baseCur.at) params.cursorCreatedAt = baseCur.at;
        if (baseCur.id) params.cursorId = baseCur.id;
        if (cursorOnly) params.cursorOnly = true;

        const { data } = await axios.get(baseUrl, { params });
        // 뒤늦게 도착한 응답은 무시
        if (mySeq !== reqSeq) return { ignored: true };

        // 커서 갱신
        if (data?.nextCreatedAt && data?.nextId) {
            cursors.value[pageIndex.value + 1] = { at: data.nextCreatedAt, id: data.nextId };
            hasNext.value = true;
        } else {
            hasNext.value = false;
        }

        if (!cursorOnly && applyItems) {
            items.value = data.items ?? [];
        }
        return data;
    }

    /** 최초/일반 로드 */
    async function fetchData(opts) {
        return _fetch({ cursorOnly: false, applyItems: true, ...(opts || {}) })
    }

    /**
     * 앞으로 n페이지 선프리패치(silent jump).
     * - n-1회는 cursorOnly로 커서만 미리 채움(화면 그대로)
     * - 마지막 1회만 실제 items 렌더
     */
    async function prefetchForward(n = 1) {
        if (n <= 0) return;
        // 1) 시작점 보존
        const startIdx = pageIndex.value;
        let tempCursor = cursors.value[startIdx] ?? { at: null, id: null };

        // 2) 임시 커서로만 n회 진행 (항상 cursorOnly + applyItems=false, pageIndex 변경 금지)
        for (let i = 0; i < n; i++) {
            const data = await _fetch({ cursorOnly: true, applyItems: false, overrideCursor: tempCursor });
            if (!data || data.ignored) return;
            if (!data.nextCreatedAt || !data.nextId) {
                // 더 이상 다음이 없으면 중단
                break;
            }
            tempCursor = { at: data.nextCreatedAt, id: data.nextId };
        }

        // 3) 최종 커서를 주입하고 마지막에 딱 한 번만 페이지 이동 + 렌더
        const targetIdx = startIdx + n;
        cursors.value[targetIdx] = tempCursor;
        pageIndex.value = targetIdx;             // ← 여기서 한 번만 UI 페이지 변경
        await _fetch({ cursorOnly: false, applyItems: true, overrideCursor: cursors.value[targetIdx] });
    }

    /** 100+ 같은 윈도우 점프: 백엔드 /anchor(windowIndex) 규격에 맞춰 호출 */
    async function fetchAnchorWindow({ windowPages = 100, size = pageSize, anchorUrl = '/api/work/db/keyset/anchor' } = {}) {
        const currentPage = pageIndex.value + 1; // 1-based
        const curWindowIndex = Math.floor((currentPage - 1) / windowPages);
        const nextWindowIndex = curWindowIndex + 1; // 다음 블록 시작점

        const params = { windowIndex: nextWindowIndex, size, ...toRaw(currentFilters) };
        const { data } = await axios.get(anchorUrl, { params });
        return { anchorCreatedAt: data.cursorCreatedAt, anchorId: data.cursorId };
    }

    async function jumpWindow({ windowPages = 100 } = {}) {
        const a = await fetchAnchorWindow({ windowPages });
        if (!a.anchorCreatedAt || !a.anchorId) return; // 범위 밖
        // jumpByAnchor는 내부적으로 pageIndex/cursors를 적절히 세팅하고 fetch 한다고 가정
        const currentPage = pageIndex.value + 1
        const curWindowIndex = Math.floor((currentPage - 1) / windowPages)
        const nextWindowIndex = curWindowIndex + 1
        await jumpByAnchor({
            cursorCreatedAt: a.anchorCreatedAt,
            cursorId: a.anchorId,
            windowIndex: nextWindowIndex,
            windowPages
        })
    }

    async function jumpByAnchor({ cursorCreatedAt, cursorId, pageAbsolute, windowIndex, windowPages = 100 }) {
        cursors.value = [{ at: cursorCreatedAt ?? null, id: cursorId ?? null }]
        pageIndex.value = 0
        if (typeof pageAbsolute === 'number') {
            pageOffset.value = Math.max(0, pageAbsolute - 1)
        } else if (typeof windowIndex === 'number') {
            pageOffset.value = windowIndex * (Number(windowPages) || 100)
        } else {
            pageOffset.value = 0
        }
        await _fetch({ cursorOnly: false, applyItems: true })
    }

    // ===== 이동 API =====

    /** « 완전 처음으로 */
    async function goFirst() {
        cursors.value = [{ at: null, id: null }]
        pageIndex.value = 0
        pageOffset.value = 0
        await _fetch({ cursorOnly: false, applyItems: true, overrideCursor: { at: null, id: null } });
    }

    /** -1 */
    async function goPrev() {
        if (pageIndex.value > 0) {
            pageIndex.value -= 1
            await _fetch({ cursorOnly: false, applyItems: true })
            return
        }
        // 현재 로컬 윈도우의 첫 페이지(=pageIndex 0)이면 절대 페이지로 이동
        const target = pageDisplay.value - 1
        if (target < 1) return
        await goToPageAbsolute(target)
    }

    /** -2 */
    async function goMinus2() {
        if (pageIndex.value >= 2) {
            pageIndex.value -= 2
            await _fetch({ cursorOnly: false, applyItems: true })
            return
        }
        // 0이나 1일 때는 절대 페이지로 처리
        const target = pageDisplay.value - 2
        if (target < 1) return
        await goToPageAbsolute(target)
    }

    /** +1 */
    const goNext  = () => prefetchForward(1);

    /** +2 (silent jump) */
    const goPlus2 = () => prefetchForward(2);

    // ===== 앵커 API 호출 유틸 =====

    /**
     * 서버 즉석 앵커 API 호출 (예: window=200 → size*200 만큼 건너뛴 커서)
     * @param {{ window?: number, size?: number, anchorParams?: Object, anchorUrl?: string }} arg
     * @returns {Promise<{anchorCreatedAt: string, anchorId: number}>}
     */
    async function fetchAnchor({ window = 200, size = pageSize, anchorParams = {}, anchorUrl = '/api/work/db/keyset/anchor' } = {}) {
        const params = {
            window, size,
            ...toRaw(currentFilters),   // 현재와 동일한 WHERE/ORDER 조건 유지
            ...anchorParams,
        }
        const { data } = await axios.get(anchorUrl, { params })
        // 응답: { anchorCreatedAt, anchorId }
        return data
    }

    // ===== 필터/정렬 변경 =====

    /**
     * 필터 교체(텍스트 → 백엔드에서 불린/우선순위로 해석)
     * - 예: setFilters({ divisionSort:'on', statusSort:'off', division:'중복', status:'활성' })
     * - 호출 시 완전 처음으로 리셋하여 재조회
     */
    async function setFilters(next = {}) {
        // 값 덮어쓰기
        for (const k of Object.keys(currentFilters)) delete currentFilters[k]
        Object.assign(currentFilters, next || {})
        // 완전 처음으로
        cursors.value = [{ at: null, id: null }]
        pageIndex.value = 0
        pageOffset.value = 0
        await fetchData()
    }

    /**
     * 부분 업데이트(특정 키만 바꿈)
     * - 예: patchFilters({ statusSort:'on' })
     */
    async function patchFilters(patch = {}) {
        Object.assign(currentFilters, patch || {})
        cursors.value = [{ at: null, id: null }]
        pageIndex.value = 0
        pageOffset.value = 0
        await fetchData()
    }

    // 마지막 페이지 메타 가져오기(점프하지 않고 lastPageNo만 갱신)
    async function loadLastPageNo({ size = pageSize, anchorUrl = '/api/work/db/keyset/anchor-last' } = {}) {
        const params = { size, ...toRaw(currentFilters) }
        const { data } = await axios.get(anchorUrl, { params })
        lastPageNo.value = data?.lastPageNo ?? null
        return lastPageNo.value
    }

// 마지막 페이지로 이동
    async function goLast({ size = pageSize, anchorUrl = '/api/work/db/keyset/anchor-last' } = {}) {
        const params = { size, ...toRaw(currentFilters) }
        const { data } = await axios.get(anchorUrl, { params })
        lastPageNo.value = data?.lastPageNo ?? null
        await jumpByAnchor({
            cursorCreatedAt: data.cursorCreatedAt,
            cursorId: data.cursorId,
            pageAbsolute: data.lastPageNo
        })
    }

// 절대 페이지로 이동
    async function goToPageAbsolute(pageNo, { size = pageSize, anchorUrl = '/api/work/db/keyset/anchor-page' } = {}) {
        if (!pageNo || pageNo < 1) return
        const params = { pageNo, size, ...toRaw(currentFilters) }
        const { data } = await axios.get(anchorUrl, { params })
        await jumpByAnchor({
            cursorCreatedAt: data.cursorCreatedAt,
            cursorId: data.cursorId,
            pageAbsolute: pageNo
        })
    }

    return {
        // state
        items, hasNext, loading, pageIndex, cursors, currentFilters, pageDisplay, lastPageNo,
        // load
        fetchData, prefetchForward, jumpByAnchor, loadLastPageNo,
        // moves
        goFirst, goPrev, goMinus2, goNext, goPlus2, goLast, goToPageAbsolute,
        // anchors
        fetchAnchor, fetchAnchorWindow, jumpWindow,
        // filters
        setFilters, patchFilters,
    }
}

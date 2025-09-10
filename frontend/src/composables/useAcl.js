import { useAuthStore } from '@/stores/auth'

// 권한별 허용된 메뉴/기능 목록 정의
// menu 접두사 : 사이드바
const policy = {
    // 본사
    SUPERADMIN: [
        // 사이드바
        'menu.dashboard',    // 대시보드
        'menu.db.all',       // 전체 DB
        'menu.db.center',    // 센터별 DB
        'menu.db.allocate',  // DB분할하기
        'menu.db.revoke',    // DB회수하기
        'menu.db.duplicate', // 중복 DB
        'menu.info',         // 정보보기
        'menu.user',         // 회원관리
    ],
    // 센터
    MANAGER: [
        // 사이드바
        'menu.dashboard',    // 대시보드
        'menu.db.all',       // 전체 DB
        'menu.db.allocate',  // DB분할하기
        'menu.db.revoke',    // DB회수하기
        'menu.db.duplicate', // 중복 DB
        'menu.info',         // 정보보기
    ],
    // 담당자
    STAFF: [
        // 사이드바
        'menu.dashboard',    // 대시보드
        'menu.db.all',       // 전체 DB
        'menu.db.duplicate', // 중복 DB
        'menu.info',         // 정보보기
    ],
}

// 권한 체크용 훅 (컴포저블)
export function useCan() {
    // 현재 로그인된 유저의 role 가져오기
    const auth = useAuthStore()

    // 특정 권한 문자열(perm)을 체크하는 함수
    const can = (perm) => {
        // perm이 없으면(=null, undefined) 기본적으로 허용
        if (!perm) return true

        // 현재 유저의 role에 맞는 권한 리스트 가져오기
        const list = policy[auth.role] || []

        // 리스트에 해당 권한이 포함돼 있으면 true 반환
        return list.includes(perm)
    }

    // 다른 곳에서 can() 함수를 사용할 수 있게 반환
    return { can }
}

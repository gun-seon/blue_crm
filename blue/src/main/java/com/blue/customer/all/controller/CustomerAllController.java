package com.blue.customer.all.controller;

import com.blue.customer.all.dto.*;
import com.blue.customer.all.service.CustomerAllService;
import com.blue.customer.common.memo.dto.MemoUpdateDto;
import com.blue.customer.common.memo.service.MemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api") // 클래스 레벨은 /api 만
public class CustomerAllController {
  
  private final CustomerAllService service;
  
  /** 키셋 페이지네이션 (프론트 훅이 사용) */
  @GetMapping("/work/db/keyset")
  public KeysetResponse<AllDbRowDto> getAllKeyset(
      Authentication auth,
      @RequestParam(defaultValue = "10") int size,        // 프론트 기본과 맞춤
      @RequestParam(required = false) String cursorCreatedAt, // "yyyy-MM-dd HH:mm:ss"
      @RequestParam(required = false) Long cursorId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String dateFrom,
      @RequestParam(required = false) String dateTo,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String division,
      @RequestParam(required = false) String sort,  // 예: "division,status"
      @RequestParam(required = false) String mine,   // MANAGER: "Y"면 내 DB만
      @RequestParam(required=false) String divisionSort, // "on" | "off" | null
      @RequestParam(required=false) String statusSort    // "on" | "off" | null
  ) {
    return service.getAllKeyset(
        auth.getName(), size, cursorCreatedAt, cursorId,
        keyword, dateFrom, dateTo, category, division, sort, mine,
        divisionSort, statusSort
    );
  }
  
  /**
   * 대규모 점프(예: 2000+ 버튼)용 앵커 커서 계산.
   * windowIndex=0  → 1~2000 페이지 윈도우 (초기)
   * windowIndex=1  → 2001~4000 페이지 윈도우 시작 커서 반환
   */
  @GetMapping("/work/db/keyset/anchor")
  public CursorAnchor getKeysetAnchor(
      Authentication auth,
      @RequestParam int windowIndex,                     // 0,1,2,...
      @RequestParam(defaultValue = "10") int size,       // 페이지 크기(프론트와 동일)
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String dateFrom,
      @RequestParam(required = false) String dateTo,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String division,
      @RequestParam(required = false) String sort,       // "division,status"
      @RequestParam(required = false) String mine,   // MANAGER: "Y"면 내 DB만
      @RequestParam(required=false) String divisionSort, // "on" | "off" | null
      @RequestParam(required=false) String statusSort    // "on" | "off" | null
  ) {
    return service.getKeysetAnchor(
        auth.getName(), windowIndex, size,
        keyword, dateFrom, dateTo, category, division, sort, mine,
        divisionSort, statusSort
    );
  }
  
  @GetMapping("/work/db/keyset/anchor-page")
  public CursorAnchor getKeysetAnchorByPage(
      Authentication auth,
      @RequestParam int pageNo,                       // 1-based 타겟 페이지
      @RequestParam(defaultValue = "10") int size,    // 페이지 크기
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String dateFrom,
      @RequestParam(required = false) String dateTo,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String division,
      @RequestParam(required = false) String sort,    // "division,status" 플래그 문자열
      @RequestParam(required = false) String mine,   // MANAGER: "Y"면 내 DB만
      @RequestParam(required=false) String divisionSort, // "on" | "off" | null
      @RequestParam(required=false) String statusSort    // "on" | "off" | null
  ) {
    return service.getKeysetAnchorByPage(
        auth.getName(), pageNo, size,
        keyword, dateFrom, dateTo, category, division, sort, mine,
        divisionSort, statusSort
    );
  }
  
  @GetMapping("/work/db/keyset/anchor-last")
  public CursorAnchorLast getKeysetAnchorLast(
      Authentication auth,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String dateFrom,
      @RequestParam(required = false) String dateTo,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String division,
      @RequestParam(required = false) String sort,    // "division,status" 등
      @RequestParam(required = false) String mine,   // MANAGER: "Y"면 내 DB만
      @RequestParam(required=false) String divisionSort, // "on" | "off" | null
      @RequestParam(required=false) String statusSort    // "on" | "off" | null
  ) {
    return service.getKeysetAnchorLast(
        auth.getName(), size,
        keyword, dateFrom, dateTo, category, division, sort, mine,
        divisionSort, statusSort
    );
  }
  
  // 인라인 업데이트(배지 status / 예약 reservation) — customers만 수정 가능
  @PatchMapping("/work/db/all/update/{customerId}")
  public ResponseEntity<Void> updateField(
      Authentication auth,
      @PathVariable Long customerId,
      @RequestBody UpdateFieldDto dto
  ) {
    service.updateField(auth.getName(), customerId, dto);
    return ResponseEntity.ok().build();
  }
  
  // (본사 전용) 중복 DB 숨김 duplicate_display=0
  @PostMapping("/lead/db/duplicate/hide")
  public ResponseEntity<Void> hideDuplicates(
      Authentication auth,
      @RequestBody IdsDto req
  ) {
    service.hideDuplicates(auth.getName(), req.getIds()); // 서비스에서 SUPERADMIN 재검증
    return ResponseEntity.ok().build();
  }
}

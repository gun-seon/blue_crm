package com.blue.customer.all.controller;

import com.blue.customer.all.dto.AllDbRowDto;
import com.blue.customer.all.dto.PagedResponse;
import com.blue.customer.all.dto.UpdateFieldDto;
import com.blue.customer.all.dto.IdsDto;
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
  private final MemoService memoService;
  
  // 전체 DB 조회 (역할별 범위는 서비스에서 적용)
  @GetMapping("/work/db")
  public PagedResponse<AllDbRowDto> getAll(
      Authentication auth,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String dateFrom,
      @RequestParam(required = false) String dateTo,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String division, // SUPERADMIN만 의미: 최초/유효/중복
      @RequestParam(required = false) String sort,     // status | division | null
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String mine,     // "Y"면 내 DB만 (MANAGER 토글용)
      @RequestParam(required = false) Long staffUserId // (보안상 서비스에서 무시/강제)
  ) {
    return service.getAll(auth.getName(), page, size, keyword, dateFrom, dateTo, category, division, sort, status, mine, staffUserId);
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

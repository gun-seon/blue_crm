package com.blue.customer.duplicate.controller;

import com.blue.customer.all.dto.AllDbRowDto;
import com.blue.customer.all.dto.PagedResponse;
import com.blue.customer.duplicate.service.CustomerDuplService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api") // 전체DB와 동일
public class CustomerDuplController {
  
  private final CustomerDuplService service;
  
  /**
   * 중복DB 조회(조회 전용)
   * - 소스: customers_duplicate
   * - 노출: duplicate_display = 0 인 행만
   * - 권한 범위:
   *   SUPERADMIN: 전체 / MANAGER: 자기 센터 / STAFF: 본인 담당
   * - 정렬: createdAt DESC 고정 (상태/구분 정렬 옵션 없음)
   * - 마스킹: SUPERADMIN + visible=N 이면 전화 마스킹, 마스킹된 값은 검색 불가
   *
   * - DTO는 전체DB의 것을 공유하여 사용
   */
  @GetMapping("/work/duplicate")
  public PagedResponse<AllDbRowDto> getDuplicates(
      Authentication auth,
      @RequestParam(defaultValue = "1")  int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String dateFrom,
      @RequestParam(required = false) String dateTo,
      @RequestParam(required = false) String category
  ) {
    return service.getAll(auth.getName(), page, size, keyword, dateFrom, dateTo, category);
  }
}

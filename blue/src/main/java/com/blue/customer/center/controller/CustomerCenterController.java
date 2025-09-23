package com.blue.customer.center.controller;

import com.blue.customer.all.dto.PagedResponse;
import com.blue.customer.center.dto.CenterDbRowDto;
import com.blue.customer.center.service.CustomerCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CustomerCenterController {
  
  private final CustomerCenterService service;
  
  /** 센터 리스트 드롭다운용 */
  @GetMapping("/work/center/centers")
  public ResponseEntity<?> getCenters() {
    return ResponseEntity.ok(service.getCenters());
  }
  
  /** 센터DB 목록 */
  @GetMapping("/work/center/db")
  public PagedResponse<CenterDbRowDto> getCenterDb(
      Authentication auth,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String dateFrom,
      @RequestParam(required = false) String dateTo,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String division,   // '최초','유효','중복'
      @RequestParam(required = false) Long centerId,     // null=전체
      @RequestParam(required = false, defaultValue = "N") String today // "Y"면 오늘만
  ) {
    // today 버튼 처리(서울/아시아 기준)
    if ("Y".equalsIgnoreCase(today)) {
      LocalDate d = LocalDate.now(); // 서버 TZ가 +09:00이면 그대로 사용
      dateFrom = d.toString();
      dateTo   = d.toString();
    }
    return service.getCenterDb(auth.getName(), page, size, keyword, dateFrom, dateTo, category, division, centerId);
  }
  
  /** 엑셀 다운로드 (현재 필터와 동일한 조건) */
  @GetMapping("/work/center/db/export")
  public ResponseEntity<byte[]> exportCenterDb(
      Authentication auth,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String dateFrom,
      @RequestParam(required = false) String dateTo,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String division,
      @RequestParam(required = false) Long centerId,
      @RequestParam(required = false, defaultValue = "N") String today
  ) {
    if ("Y".equalsIgnoreCase(today)) {
      LocalDate d = LocalDate.now();
      dateFrom = d.toString();
      dateTo   = d.toString();
    }
    return service.exportExcel(auth.getName(), keyword, dateFrom, dateTo, category, division, centerId);
  }
}

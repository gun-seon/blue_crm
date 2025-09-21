package com.blue.customer.common.sheets.controller;

import com.blue.customer.common.sheets.dto.GsheetSourceDto;
import com.blue.customer.common.sheets.mapper.SyncMapper;
import com.blue.customer.common.sheets.service.SheetsSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sheets")
public class SheetsSyncController {
  
  private final SheetsSyncService service;
  private final SyncMapper mapper;
  
  // 수동 새로고침 (full=true 이면 드레인)
  @PostMapping("/refresh")
  public ResponseEntity<?> refresh(@RequestParam(name = "sid", defaultValue = "1") long sourceId,
                                   @RequestParam(name = "full", defaultValue = "false") boolean full) {
    var r = full ? service.nightlyResume(sourceId, 0) : service.manualRefresh(sourceId);
    return ResponseEntity.ok(new Resp(r.executed(), r.rows(), r.reason()));
  }
  
  @GetMapping("/cursor")
  public ResponseEntity<?> cursor(@RequestParam(name = "sid", defaultValue = "1") long sourceId) {
    GsheetSourceDto s = mapper.findSourceById(sourceId);
    return ResponseEntity.ok(s);
  }
  
  record Resp(boolean executed, int rows, String reason) {}
}

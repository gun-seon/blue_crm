package com.blue.customer.revoke.controller;

import com.blue.customer.all.dto.PagedResponse;
import com.blue.customer.revoke.dto.RevokeListRowDto;
import com.blue.customer.revoke.dto.RevokeReq;
import com.blue.customer.revoke.dto.RevokeResult;
import com.blue.customer.revoke.service.CustomerRevokeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CustomerRevokeController {
  
  private final CustomerRevokeService service;
  
  // 목록 (본사/센터장)
  @GetMapping("/work/revoke/list")
  public PagedResponse<RevokeListRowDto> list(Authentication auth,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String dateFrom,
                                              @RequestParam(required = false) String dateTo,
                                              @RequestParam(required = false) String category,
                                              @RequestParam(required = false) String division, // HQ only
                                              @RequestParam(required = false) String sort) {
    return service.list(auth.getName(), page, size, keyword, dateFrom, dateTo, category, division, sort);
  }
  
  // 본사 회수 실행
  @PostMapping("/work/revoke/hq")
  public RevokeResult revokeByHq(Authentication auth, @RequestBody RevokeReq req) {
    return service.revokeByHq(auth.getName(), req);
  }
  
  // 센터장 회수 실행
  @PostMapping("/work/revoke/manager")
  public RevokeResult revokeByManager(Authentication auth, @RequestBody RevokeReq req) {
    return service.revokeByManager(auth.getName(), req);
  }
}

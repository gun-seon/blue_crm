package com.blue.customer.allocate.controller;

import com.blue.customer.all.dto.PagedResponse;
import com.blue.customer.allocate.dto.*;
import com.blue.customer.allocate.service.CustomerAllocateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CustomerAllocateController {
  
  private final CustomerAllocateService service;
  
  // 목록 (본사/센터장)
  @GetMapping("/work/allocate/list")
  public PagedResponse<AllocateListRowDto> list(Authentication auth,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String dateFrom,
                                                @RequestParam(required = false) String dateTo,
                                                @RequestParam(required = false) String category,
                                                @RequestParam(required = false) String division, // HQ only: '최초'|'유효'
                                                @RequestParam(required = false) String sort) {
    return service.list(auth.getName(), page, size, keyword, dateFrom, dateTo, category, division, sort);
  }
  
  // 본사 분배
  @PostMapping("/work/allocate/hq")
  public AllocateResult allocateByHq(Authentication auth, @RequestBody AllocateHqReq req) {
    return service.allocateByHq(auth.getName(), req);
  }
  
  // 센터장 재분배
  @PostMapping("/work/allocate/manager")
  public AllocateResult allocateByManager(Authentication auth, @RequestBody AllocateMgrReq req) {
    return service.allocateByManager(auth.getName(), req);
  }
  
  // 직원 검색
  @GetMapping("/work/allocate/users")
  public List<UserPickDto> searchUsers(
      Authentication auth,
      @RequestParam(required = false) Long centerId,
      @RequestParam(required = false) String q
  ) {
    return service.searchUsersForAllocate(auth.getName(), centerId, q);
  }
  
  // 센터 조회
  @GetMapping("/work/allocate/centers")
  public List<CenterPickDto> centers(Authentication auth) {
    return service.centersForAllocate(auth.getName());
  }
}

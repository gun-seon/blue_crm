package com.blue.dashboard.controller;

import com.blue.dashboard.dto.CenterDto;
import com.blue.dashboard.dto.SummaryDto;
import com.blue.dashboard.dto.UserDto;
import com.blue.dashboard.dto.CustomerDto;
import com.blue.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/common/dashboard")
@RequiredArgsConstructor
public class DashboardController {
  
  private final DashboardService dashboardService;
  
  // 승인(Y) 사용자 리스트(간단 조회)
  @GetMapping("/users")
  public List<UserDto> getUsers() {
    return dashboardService.findUsers();
  }
  
  // 정확 일치 검색 (이름 또는 이메일) — 프론트에서 사용
  @GetMapping("/users/find")
  public List<UserDto> findUsersExact(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String email
  ) {
    return dashboardService.findUsersExact(name, email);
  }
  
  // 본체+중복 통합 조회 (필터: userId / centerIds / from / to)
  @GetMapping("/customers")
  public List<CustomerDto> getCustomers(
      @RequestParam(required = false) Long userId,
      @RequestParam(required = false) List<Long> centerIds,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to
  ) {
    return dashboardService.findCustomers(userId, centerIds, from, to);
  }
  
  // KPI (프론트 호환)
  @GetMapping("/kpi")
  public SummaryDto getKpi() {
    return dashboardService.getSummary();
  }
  
  // KPI - 동일 기능 별칭 (/summary)
  @GetMapping("/summary")
  public SummaryDto getSummary() {
    return dashboardService.getSummary();
  }
}
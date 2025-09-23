package com.blue.dashboard.service;

import com.blue.dashboard.dto.CenterDto;
import com.blue.dashboard.dto.SummaryDto;
import com.blue.dashboard.dto.UserDto;
import com.blue.dashboard.dto.CustomerDto;
import com.blue.dashboard.mapper.DashboardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {
  
  private final DashboardMapper dashboardMapper;
  
  public List<CenterDto> findCenters() {
    return dashboardMapper.findCenters(); // (리스트는 '본사' 제외)
  }
  
  public List<UserDto> findUsers() {
    return dashboardMapper.findUsers(); // 승인 Y
  }
  
  public List<UserDto> findUsersExact(String name, String email) {
    if (name != null && !name.isBlank()) return dashboardMapper.findUsersExactByName(name);
    if (email != null && !email.isBlank()) return dashboardMapper.findUsersExactByEmail(email);
    return List.of();
  }
  
  public List<CustomerDto> findCustomers(Long userId, List<Long> centerIds, String from, String to) {
    return dashboardMapper.findCustomers(userId, centerIds, from, to);
  }
  
  public SummaryDto getSummary() {
    int users = dashboardMapper.countApprovedUsers(); // 승인 Y 전체
    int centers = dashboardMapper.countCentersExcludingHQ(); // '본사' 제외
    return new SummaryDto(users, centers);
  }
}
package com.blue.dashboard.mapper;

import com.blue.dashboard.dto.CenterDto;
import com.blue.dashboard.dto.UserDto;
import com.blue.dashboard.dto.CustomerDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DashboardMapper {
  
  List<CenterDto> findCenters(); // 본사 제외 목록 (모달용)
  List<UserDto> findUsers(); // 승인 Y
  
  List<UserDto> findUsersExactByName(@Param("name") String name);
  List<UserDto> findUsersExactByEmail(@Param("email") String email);
  
  List<CustomerDto> findCustomers(
      @Param("userId") Long userId,
      @Param("centerIds") List<Long> centerIds,
      @Param("from") String from,
      @Param("to") String to
  );
  
  int countApprovedUsers();
  int countCentersExcludingHQ();
}
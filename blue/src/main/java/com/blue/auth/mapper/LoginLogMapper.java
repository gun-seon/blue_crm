package com.blue.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface LoginLogMapper {
  
  /** 로그인 성공 시 기록 */
  void insertLoginLog(
      @Param("userId") Long userId,
      @Param("name") String snapshotName,
      @Param("phone") String snapshotPhone,
      @Param("role") String snapshotRole
  );
  
  /** 로그아웃 성공 시 최신 기록 갱신 */
  void updateLogoutLog(@Param("email") String email);
  
}
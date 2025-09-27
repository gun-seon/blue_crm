package com.blue.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface LoginLogMapper {
  
//  /** 로그인 성공 시 기록 */
//  void insertLoginLog(
//      @Param("userId") Long userId,
//      @Param("name") String snapshotName,
//      @Param("phone") String snapshotPhone,
//      @Param("role") String snapshotRole
//  );

//  /** 로그아웃 성공 시 최신 기록 갱신 */
//  void updateLogoutLog(@Param("email") String email);
  
  // 세션키(jti) 기반 로그인 기록 (중복 세션이면 login_at만 갱신되도록 XML에 ON DUPLICATE)
  int insertLoginLogWithSession(
      @Param("userId") Long userId,
      @Param("name") String name,
      @Param("phone") String phone,
      @Param("role") String role,
      @Param("sessionKey") String sessionKey
  );
  
  // 세션키(jti)로 정확히 해당 세션 로그아웃 처리
  int updateLogoutLogBySession(@Param("sessionKey") String sessionKey);
  
  // 세션 키 회전
  int rotateSessionKey(@Param("oldJti") String oldJti,
                       @Param("newJti") String newJti);
  
}
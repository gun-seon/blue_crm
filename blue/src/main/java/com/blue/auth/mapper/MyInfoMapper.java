package com.blue.auth.mapper;

import com.blue.auth.dto.CenterDto;
import com.blue.auth.dto.MyInfoResponse;
import com.blue.auth.dto.SheetSettingsDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MyInfoMapper {
  MyInfoResponse findByEmail(@Param("userEmail") String userEmail);
  
  void updatePhone(@Param("userEmail") String userEmail,
                   @Param("userPhone") String userPhone);
  
  void updatePassword(@Param("userEmail") String userEmail,
                      @Param("userPassword") String userPassword);
  
  // 구글 스프레드 시트 관련
  SheetSettingsDto findOneBySourceId(@Param("sourceId") long sourceId);
  int updateOne(@Param("sourceId") long sourceId,
                @Param("sheetId") String sheetId,
                @Param("startRow") int startRow,
                @Param("sheetName") String sheetName);
  
  // 센터 관리
  List<CenterDto> findAll();
  boolean existsByName(@Param("centerName") String centerName);
  int insert(@Param("centerName") String centerName);
  int countUsersInCenter(@Param("centerId") long centerId);
  int delete(@Param("centerId") long centerId);
  
  // 기간 내 원본 행 조회 (SELECT * 그대로, login_at 기준)
  List<java.util.Map<String, Object>> findLoginLogsRaw(
      @Param("fromYmd") String fromYmd,
      @Param("toYmd")   String toYmd,
      @Param("limit")   int limit
  );
  
  // 슈퍼 단일성: 모두 해제 (is_super = 'N')
  int demoteAllSuperToManager();
  
  // 특정 사용자 권한 변경 (role 세팅 + SUPER면 is_super='Y' 세팅)
  int updateRoleByUserId(@Param("userId") long userId, @Param("role") String role);
  
  // 위임 대상 요약 조회 (프론트 카드에 필요한 필드만)
  MyInfoResponse findUserBriefById(@Param("userId") long userId);
  MyInfoResponse findUserBriefByEmail(@Param("email") String email);
  
}

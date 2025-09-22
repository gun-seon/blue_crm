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
                @Param("startRow") int startRow);
  
  // 센터 관리
  List<CenterDto> findAll();
  boolean existsByName(@Param("centerName") String centerName);
  int insert(@Param("centerName") String centerName);
  int countUsersInCenter(@Param("centerId") long centerId);
  int delete(@Param("centerId") long centerId);
  
}

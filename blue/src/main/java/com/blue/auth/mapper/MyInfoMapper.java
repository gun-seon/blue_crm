package com.blue.auth.mapper;

import com.blue.auth.dto.MyInfoResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MyInfoMapper {
  MyInfoResponse findByEmail(@Param("userEmail") String userEmail);
  
  void updatePhone(@Param("userEmail") String userEmail,
                   @Param("userPhone") String userPhone);
  
  void updatePassword(@Param("userEmail") String userEmail,
                      @Param("userPassword") String userPassword);
}

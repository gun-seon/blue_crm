package com.blue.info.mapper;

import com.blue.info.dto.UserRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InfoMapper {
  List<UserRow> findAllUsers();
  UserRow findByEmail(@Param("email") String email);
  UserRow findById(@Param("userId") Long userId);
  
  int updateUserRole(@Param("userId") Long userId, @Param("role") String role);
  int updateUserCenter(@Param("userId") Long userId, @Param("centerId") Long centerId);
  int updateUserApproved(@Param("userId") Long userId, @Param("approved") String approved);
  
  // 추가: 이름/전화
  int updateUserName(@Param("userId") Long userId, @Param("name") String name);
  int updateUserPhone(@Param("userId") Long userId, @Param("phone") String phone);
}

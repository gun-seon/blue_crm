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
  
  // 추가: 이름/전화
  int updateUserName(@Param("userId") Long userId, @Param("name") String name);
  int updateUserPhone(@Param("userId") Long userId, @Param("phone") String phone);
}

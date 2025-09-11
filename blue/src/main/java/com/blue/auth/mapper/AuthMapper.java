package com.blue.auth.mapper;

import com.blue.auth.dto.UserDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthMapper {
  // email로 회원 찾기
  UserDto findByEmail(@Param("email") String email);
  // phone으로 회원 찾기
  UserDto findByPhone(@Param("phone") String phone);
  
  // 회원가입 (미승인 회원 추가)
  void insertUser(UserDto user);
}
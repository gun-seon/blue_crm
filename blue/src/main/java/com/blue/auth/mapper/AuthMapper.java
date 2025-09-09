package com.blue.auth.mapper;

import com.blue.auth.dto.UserDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthMapper {
  // email로 회원 찾기
  UserDto findByEmail(@Param("email") String email);
  
  // 회원가입 (회원 추가)
  void insertUser(UserDto user);
}
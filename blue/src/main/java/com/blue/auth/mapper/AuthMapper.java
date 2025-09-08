package com.blue.auth.mapper;

import com.blue.auth.dto.UserDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthMapper {
  UserDto findByEmail(@Param("email") String email);
}
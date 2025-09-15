package com.blue.user.mapper;

import com.blue.user.dto.UserSelectDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
  // 페이지 로딩시 최초 조회 - 데이터 목록 조회
  List<UserSelectDto> findUsers(@Param("offset") int offset,
                                @Param("size") int size,
                                @Param("keyword") String keyword);
  
  // 페이지 로딩시 최초 조회 - 데이터 최대 갯수 조회
  int countUsers(@Param("keyword") String keyword);
  
  // 직원관리 페이지에서 배지 수정이 발생한 경우- 단일 필드 수정
  int updateUserField(@Param("userId") Long userId,
                      @Param("field") String field,
                      @Param("value") String value);
}

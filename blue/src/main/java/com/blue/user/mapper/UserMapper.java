package com.blue.user.mapper;

import com.blue.user.dto.UserSelectDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
  // 슈퍼계정인지 판단
  Boolean isSuperByEmail(String email);
  
  // 페이지 로딩시 최초 조회 - 데이터 목록 조회
  List<UserSelectDto> findUsers(@Param("offset") int offset,
                                @Param("size") int size,
                                @Param("keyword") String keyword);
  
  // 페이지 로딩시 최초 조회 - 데이터 최대 갯수 조회
  int countUsers(@Param("keyword") String keyword);
  
  // 아이디로 다수 유저 검색
  List<UserSelectDto> findUsersByIds(@Param("ids") List<Long> ids);
  // 아이디로 단일 유저 검색
  UserSelectDto findUserById(@Param("userId") Long userId);
  
  // 직원관리 페이지에서 배지 수정이 발생한 경우- 단일 필드 수정
  int updateUserField(@Param("userId") Long userId,
                      @Param("field") String field,
                      @Param("value") String value);
  
  // 직원관리 페이지에서 일괄 승인하는 경우
  int approveUsers(@Param("ids") List<Long> ids);
  
  // 해당 center에 '다른' MANAGER가 있는지 (본사 제외, 본인 제외 가능)
  int countManagersInCenter(@Param("centerName") String centerName,
                            @Param("excludeUserId") Long excludeUserId);
  
  // 센터장(팀장)에서 다른 역할로 변경 직후: 해당 센터장(팀장)이 들고 있던 디비중 상태='없음'만 회수
  int autoRecallStatusNoneByOwner(Long userId);
  
}

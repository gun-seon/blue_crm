package com.blue.user.service;

import com.blue.user.dto.PageResponse;
import com.blue.user.dto.UserSelectDto;
import com.blue.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
  
  private final UserMapper userMapper;
  
  // 페이지 로딩시 최초 조회
  public PageResponse<UserSelectDto> getUsers(int page, int size, String keyword) {
    int offset = (page - 1) * size;
    List<UserSelectDto> items = userMapper.findUsers(offset, size, keyword);
    int totalCount = userMapper.countUsers(keyword);
    int totalPages = (int) Math.ceil((double) totalCount / size);
    
    return new PageResponse<>(items, totalPages, totalCount);
  }
  
  // 직원관리 페이지에서 배지 수정이 발생한 경우
  public void updateUserField(Long userId, String field, String value) {
    userMapper.updateUserField(userId, field, value);
  }
}

package com.blue.user.service;

import com.blue.user.dto.BulkApproveResponse;
import com.blue.user.dto.PageResponse;
import com.blue.user.dto.UserSelectDto;
import com.blue.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
  
  private final UserMapper userMapper;
  
  @Value("${app.security.super-account}")
  private String superAccount;
  
  // 페이지 로딩시 최초 조회
  public PageResponse<UserSelectDto> getUsers(int page, int size, String keyword) {
    int offset = (page - 1) * size;
    List<UserSelectDto> items = userMapper.findUsers(offset, size, keyword);
    int totalCount = userMapper.countUsers(keyword);
    int totalPages = (int) Math.ceil((double) totalCount / size);
    
    return new PageResponse<>(items, totalPages, totalCount);
  }
  
  // 직원관리 페이지에서 배지 수정이 발생한 경우
  @Transactional
  public void updateUserField(Long userId, String field, String value, String requesterEmail) {
    // 수정 대상 조회
    UserSelectDto target = userMapper.findUserById(userId);
    if (target == null) {
      throw new IllegalArgumentException("해당 직원을 찾을 수 없습니다.");
    }
    
    // 1. 본인 계정 수정 금지
    if (target.getUserEmail().equals(requesterEmail)) {
      throw new SecurityException("본인 계정은 수정할 수 없습니다.");
    }
    
    // 2. 특별 계정이 아닌 경우 → 본사/관리자 계정 수정 불가
    boolean isRestricted = "SUPERADMIN".equals(target.getUserRole()) || "본사".equals(target.getCenterName());
    if (isRestricted && !superAccount.equals(requesterEmail)) {
      throw new SecurityException("관리자 계정의 정보는 특별 계정만 수정 가능합니다.");
    }
    
    // 조건 통과한 경우만 업데이트 실행
    userMapper.updateUserField(userId, field, value);
  }
  
  // 직원관리 페이지에서 직원에 대한 일괄수정이 발생한 경우
  @Transactional
  public BulkApproveResponse bulkApprove(List<Long> userIds, String requesterEmail) {
    List<UserSelectDto> users = userMapper.findUsersByIds(userIds);
    
    List<Long> approved = new ArrayList<>(); // 승인 성공한 사람들
    List<Long> skipped = new ArrayList<>(); // 제외된 사람들
    
    for (UserSelectDto u : users) {
      // 승인하고 싶은 계정이 관리자권한 혹은 본사소속인지 확인
      boolean isRestricted = "SUPERADMIN".equals(u.getUserRole()) || "본사".equals(u.getCenterName());
      
      // 만약 승인대상에 관리자/본사가 포함되어있으면서
      // 동시에 요청한 사람의 계정이 특별한 계정이 아니라면
      if (isRestricted && !superAccount.equals(requesterEmail)) {
        // 승인불가 리스트에 추가
        skipped.add(u.getUserId());
      } else {
        approved.add(u.getUserId());
      }
    }
    
    // 승인 가능한 사용자만 일괄 승인
    if (!approved.isEmpty()) {
      userMapper.approveUsers(approved);
    }
    
    return new BulkApproveResponse(approved, skipped);
  }
}

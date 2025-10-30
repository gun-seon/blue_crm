package com.blue.user.service;

import com.blue.user.dto.BulkApproveResponse;
import com.blue.user.dto.PageResponse;
import com.blue.user.dto.UserSelectDto;
import com.blue.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
  
  private final UserMapper userMapper;
  
  // 슈퍼계정인지 판단
  private boolean isRequesterSuper(String email) {
    Boolean b = userMapper.isSuperByEmail(email);
    return Boolean.TRUE.equals(b);
  }
  
  // 페이지 로딩시 최초 조회
  public PageResponse<UserSelectDto> getUsers(int page, int size, String keyword) {
    int offset = (page - 1) * size;
    List<UserSelectDto> items = userMapper.findUsers(offset, size, keyword);
    int totalCount = userMapper.countUsers(keyword);
    int totalPages = (int) Math.ceil((double) totalCount / size);
    
    return new PageResponse<>(items, totalPages, totalCount);
  }
  
  // 프론트 사전 확인용 (본사면 0 반환)
  public int countManagersInCenter(String centerName, Long excludeUserId) {
    if ("본사".equals(centerName)) return 0;
    return userMapper.countManagersInCenter(centerName, excludeUserId);
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
    if (isRestricted && !isRequesterSuper(requesterEmail)) {
      throw new SecurityException("권한이 없습니다.");
    }
    
    // 3) 가시권한(visible)은 super만 수정 가능
    if ("visible".equals(field) && !isRequesterSuper(requesterEmail)) {
      throw new SecurityException("권한이 없습니다.");
    }
    
    // 4) 센터장 1명 제한(서버 가드)
    // 4-1) 구분을 '센터장'으로 바꾸는 경우 → 현재 소속에 다른 센터장이 있으면 불가 (본사 제외)
    if ("type".equals(field) && "센터장".equals(value)) {
      String centerName = target.getCenterName();
      if (centerName != null && !"본사".equals(centerName)) {
        int cnt = countManagersInCenter(centerName, userId);
        if (cnt > 0) {
          throw new IllegalStateException("'" + centerName + "'에는 이미 센터장이 있습니다.");
        }
      }
    }
    
    // 4-2) 소속을 바꾸는 경우 → 대상이 현재 MANAGER라면, 이동할 소속에 다른 센터장이 있으면 불가 (본사 제외)
    if ("center".equals(field)) {
      boolean targetIsManager = "MANAGER".equals(target.getUserRole());
      if (targetIsManager && value != null && !"본사".equals(value)) {
        int cnt = countManagersInCenter(value, userId);
        if (cnt > 0) {
          throw new IllegalStateException("'" + value + "'에는 이미 센터장이 있습니다.");
        }
      }
    }
    
    // 어느 직급에서 어느 직급으로 변경되었는지 확인하기 위해,
    // 업데이트 이전 직급과 업데이트 희망 직급을 둘 다 저장
    String oldRole = target.getUserRole();
    String newRole = oldRole;
    if ("type".equals(field)) {
      if ("관리자".equals(value)) newRole = "SUPERADMIN";
      else if ("센터장".equals(value)) newRole = "MANAGER";
      else if ("담당자".equals(value)) newRole = "STAFF";
    }
    
    // 조건 통과한 경우만 실제 업데이트 실행
    userMapper.updateUserField(userId, field, value);
    
    // 업데이트가 성공적일 경우
    // 다음 각 경우에 대해 상태가 '없음'인 디비를 회수
    if ("type".equals(field)) {
      // 1. 센터장이였다가 -> 담당자로 강등된 경우
      boolean demoteManagerToStaff = "MANAGER".equals(oldRole) && "STAFF".equals(newRole);
      // 2. 센터장이였다가 -> 본사로 승급된 경우
      boolean managerToHq = "MANAGER".equals(oldRole) && "SUPERADMIN".equals(newRole);
      
      // 위 두 경우중 하나라도 해당 된다면 회수 처리
      if (demoteManagerToStaff || managerToHq) {
        userMapper.autoRecallStatusNoneByOwner(userId);
      }
    }
  }
  
  // 직원관리 페이지에서 직원에 대한 일괄수정이 발생한 경우
  @Transactional
  public BulkApproveResponse bulkApprove(List<Long> userIds, String requesterEmail) {
    List<UserSelectDto> users = userMapper.findUsersByIds(userIds);
    
    List<Long> approved = new ArrayList<>(); // 승인 성공한 사람들
    List<Long> skipped = new ArrayList<>(); // 제외된 사람들
    
    boolean requesterIsSuper = isRequesterSuper(requesterEmail);
    for (UserSelectDto u : users) {
      // 승인하고 싶은 계정이 관리자권한 혹은 본사소속인지 확인
      boolean isRestricted = "SUPERADMIN".equals(u.getUserRole()) || "본사".equals(u.getCenterName());
      
      // 만약 승인대상에 관리자/본사가 포함되어있으면서
      // 동시에 요청한 사람의 계정이 특별한 계정이 아니라면
      if (isRestricted && !requesterIsSuper) {
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

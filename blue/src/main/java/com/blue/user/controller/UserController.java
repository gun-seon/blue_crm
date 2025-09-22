package com.blue.user.controller;

import com.blue.user.dto.BulkApproveResponse;
import com.blue.user.dto.PageResponse;
import com.blue.user.dto.UpdateUserRequest;
import com.blue.user.dto.UserSelectDto;
import com.blue.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/super/users")
@RequiredArgsConstructor
public class UserController {
  
  private final UserService userService;
  
  @GetMapping
  // 페이지 로딩시 최초 조회
  public PageResponse<UserSelectDto> getUsers(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String keyword
  ) {
    
//    System.out.println(keyword);
//    System.out.println(userService.getUsers(page, size, keyword));
    return userService.getUsers(page, size, keyword);
  }
  
  // 프론트 사전 확인: 해당 센터에 다른 MANAGER 있는지
  @GetMapping("/has-manager")
  public ResponseEntity<?> hasManager(@RequestParam String centerName,
                                      @RequestParam(required = false) Long excludeUserId) {
    int cnt = userService.countManagersInCenter(centerName, excludeUserId);
    return ResponseEntity.ok(Map.of("exists", cnt > 0));
  }
  
  // 직원관리 페이지에서 배지 수정이 발생한 경우
  @PatchMapping("/update/{userId}")
  public void updateUserField(
      @PathVariable Long userId,
      @RequestBody UpdateUserRequest req,   // 예: {'권한' : '관리자'}
      Authentication auth
      
  ) {
//    System.out.println(userId);
    String requesterEmail = auth.getName(); // 현재 로그인 사용자
    userService.updateUserField(userId, req.getField(), req.getValue(), requesterEmail);
  }
  
  // 직원관리 페이지에서 직원에 대한 일괄수정이 발생한 경우
  @PatchMapping("/bulk-approve")
  public ResponseEntity<?> bulkApprove(@RequestBody List<Long> userIds, Authentication auth) {
//    System.out.println(userIds);
    String requesterEmail = auth.getName(); // 현재 로그인 사용자
    BulkApproveResponse result = userService.bulkApprove(userIds, requesterEmail);
    return ResponseEntity.ok(result);
  }
}

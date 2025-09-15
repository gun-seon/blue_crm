package com.blue.user.controller;

import com.blue.user.dto.PageResponse;
import com.blue.user.dto.UpdateUserRequest;
import com.blue.user.dto.UserSelectDto;
import com.blue.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
  
  // 직원관리 페이지에서 배지 수정이 발생한 경우
  @PatchMapping("/update/{userId}")
  public void updateUserField(
      @PathVariable Long userId,
      @RequestBody UpdateUserRequest req   // 예: {'권한' : '관리자'}
  ) {
//    System.out.println(userId);
    userService.updateUserField(userId, req.getField(), req.getValue());
  }
}

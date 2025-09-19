package com.blue.info.controller;

import com.blue.info.service.InfoService;
import com.blue.user.dto.UpdateUserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/info")
public class InfoController {
  
  private final InfoService infoService;
  
  /** 조직도 트리 + 현재 사용자 (헤더 검색은 프론트에서 펼침만 제어) */
  @GetMapping("/tree")
  public Map<String, Object> tree(Authentication auth) {
    return infoService.getOrgTree(auth);
  }
  
  /** 프론트 옵션: 센터 이름 목록 (필요 시 HQ 제외/포함 조정 가능) */
  @GetMapping("/centers")
  public List<String> centers() {
    return infoService.centerNames();
  }
  
  /** 조직도에서 (이름/전화) 수정 */
  @PatchMapping("/users/update/{userId}")
  public void updateFromOrg(@PathVariable Long userId,
                            @RequestBody UpdateUserRequest req,
                            Authentication auth) {
    infoService.updateUserFromOrg(userId, req, auth);
  }
}

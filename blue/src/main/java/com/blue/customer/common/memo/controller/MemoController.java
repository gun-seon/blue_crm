package com.blue.customer.common.memo.controller;

import com.blue.customer.common.memo.dto.MemoDetailDto;
import com.blue.customer.common.memo.dto.MemoUpdateDto;
import com.blue.customer.common.memo.service.MemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/work/db/memo")
public class MemoController {
  
  private final MemoService memoService;
  
  @GetMapping("/{customerId}")
  public ResponseEntity<MemoDetailDto> getDetail(@PathVariable Long customerId,
                                                 Authentication auth) {
    String email = auth.getName(); // 토큰/세션에서 들어온 사용자 식별자(보통 username)
    return ResponseEntity.ok(memoService.getMemo(customerId, email));
  }
  
  @PatchMapping("/{customerId}")
  public ResponseEntity<Void> update(@PathVariable Long customerId,
                                     @RequestBody MemoUpdateDto body,
                                     Authentication auth) {
    String email = auth.getName();
    memoService.updateMemo(email, customerId, body);
    return ResponseEntity.noContent().build();
  }
}

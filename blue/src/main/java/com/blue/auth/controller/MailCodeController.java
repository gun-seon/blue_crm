package com.blue.auth.controller;

import com.blue.auth.service.MailCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
public class MailCodeController {
  private final MailCodeService mailCodeService;
  
  // 인증번호 최초 발급
  @PostMapping("/send-code")
  public ResponseEntity<?> sendCode(@RequestBody Map<String, String> body) {
    String email = body.get("email");
    long ttl = mailCodeService.sendCode(email);
    return ResponseEntity.ok(Map.of("ttl", ttl));
  }
  
  // 인증번호 검증
  @PostMapping("/verify-code")
  public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> body) {
    String email = body.get("email");
    String code = body.get("code");
    
    boolean success = mailCodeService.verifyCode(email, code);
    if (success) {
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "인증 실패 또는 코드 만료"));
    }
  }
  
  // 시간 연장 (1회 한정)
  @PostMapping("/extend-code")
  public ResponseEntity<?> extendCode(@RequestBody Map<String, String> body) {
    String email = body.get("email");
    long TTL = mailCodeService.extendTime(email);
    
    if (TTL > 0) {
      return ResponseEntity.ok(Map.of("ttl", TTL)); // 다시 3분으로 초기화
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", "이미 연장했거나 코드가 없음"));
    }
  }
  
  // TTL 조회
  @GetMapping("/code-ttl")
  public ResponseEntity<?> getCodeTtl(@RequestParam String email) {
    long ttl = mailCodeService.getRemainingTime(email);
    if (ttl <= 0) {
      return ResponseEntity.status(HttpStatus.GONE)
          .body(Map.of("error", "코드가 만료되었거나 존재하지 않음"));
    }
    return ResponseEntity.ok(Map.of("ttl", ttl));
  }
}
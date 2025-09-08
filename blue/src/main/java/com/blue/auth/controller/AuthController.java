package com.blue.auth.controller;

import com.blue.auth.dto.AuthResponse;
import com.blue.auth.dto.LoginRequest;
import com.blue.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;
  
  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
    try {
      AuthResponse authResponse = authService.login(request, response);
      return ResponseEntity.ok(authResponse);
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "아이디 또는 비밀번호가 올바르지 않습니다."));
    }
  }
  
  // 공통 메소드 : /refresh, /extend
  // 쿠키에서 refreshToken 추출
  private String extractRefreshToken(HttpServletRequest request) {
    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if ("refreshToken".equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }
  
  // 엑세스 토큰의 재발급
  @PostMapping("/token/refresh")
  public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
    String refreshToken = extractRefreshToken(request);
    if (refreshToken == null) {
      return ResponseEntity.status(401).build();
    }
    return ResponseEntity.ok(authService.refresh(refreshToken));
  }
  
  // 로그인 수동 연장 (리프레시 토큰의 재발급)
  @PostMapping("/token/extend")
  public ResponseEntity<AuthResponse> extend(HttpServletRequest request, HttpServletResponse response) {
    String refreshToken = extractRefreshToken(request);
    if (refreshToken == null) {
      return ResponseEntity.status(401).build();
    }
    return ResponseEntity.ok(authService.extend(refreshToken, response));
  }
}
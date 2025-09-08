package com.blue.auth.service;

import com.blue.auth.dto.AuthResponse;
import com.blue.auth.dto.LoginRequest;
import com.blue.auth.dto.UserDto;
import com.blue.auth.mapper.AuthMapper;
import com.blue.global.exception.AuthException;
import com.blue.global.security.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
  private final AuthMapper authMapper;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  
  // 공통 메소드 : login, extend
  // 리프레시 쿠키 설정 메서드
  private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
    Cookie cookie = new Cookie("refreshToken", refreshToken);
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setPath("/api/auth/token");
    // 브라우저 창 닫아도 쿠키 유지 : 30분
    // cookie.setMaxAge(60 * 30);
    response.addCookie(cookie);
  }
  
  // 로그인
  public AuthResponse login(LoginRequest request, HttpServletResponse response) {
    // DB 조회
    UserDto user = authMapper.findByEmail(request.getEmail());
    
    if (user == null || !passwordEncoder.matches(request.getPassword(), user.getUserPassword())) {
      throw new RuntimeException("Invalid credentials");
    }
    
    String accessToken = jwtUtil.generateAccessToken(user);
    String refreshToken = jwtUtil.generateRefreshToken(user);
    
    // 리프레시 토큰을 쿠키에 저장
    addRefreshTokenCookie(response, refreshToken);
    
    // Refresh Token exp 추출
    var claims = jwtUtil.validateToken(refreshToken);
    long refreshExp = claims.getExpiration().getTime();
    
    return new AuthResponse(accessToken, null, user.getUserRole(), user.getUserEmail(), refreshExp);
  }
  
  // 엑세스 토큰의 재발급
  public AuthResponse refresh(String refreshToken) {
    try {
      var claims = jwtUtil.validateToken(refreshToken);
      
      UserDto user = new UserDto();
      user.setUserEmail(claims.getSubject());
      user.setUserRole(claims.get("role", String.class));
      
      // 기존 refreshToken 그대로 쓰므로 exp도 동일
      long refreshExp = claims.getExpiration().getTime();
      
      String newAccessToken = jwtUtil.generateAccessToken(user);
      return new AuthResponse(newAccessToken, null, user.getUserRole(), user.getUserEmail(), refreshExp);
    } catch (ExpiredJwtException e) {
      // Refresh Token 자체가 만료된 경우
      // 정상종료이므로 사용자의 재로그인이 필요함
      log.info("Refresh token expired at {}", e.getClaims().getExpiration());
      throw new AuthException("Refresh token expired", HttpStatus.UNAUTHORIZED);
    } catch (JwtException e) {
      // 비정상 종료
      log.warn("Invalid refresh token: {}", e.getMessage());
      throw new AuthException("Invalid refresh token", HttpStatus.UNAUTHORIZED);
    }
  }
  
  // 로그인 수동 연장 (리프레시 토큰의 재발급)
  public AuthResponse extend(String refreshToken, HttpServletResponse response) {
    try {
      var claims = jwtUtil.validateToken(refreshToken);
      
      UserDto user = new UserDto();
      user.setUserEmail(claims.getSubject());
      user.setUserRole(claims.get("role", String.class));
      
      // 새 Access Token + 새 Refresh Token 발급
      String newAccessToken = jwtUtil.generateAccessToken(user);
      String newRefreshToken = jwtUtil.generateRefreshToken(user);
      
      // 새 Refresh Token을 쿠키에 덮어쓰기
      addRefreshTokenCookie(response, newRefreshToken);
      
      // 새 Refresh Token의 만료시간
      var newClaims = jwtUtil.validateToken(newRefreshToken);
      long refreshExp = newClaims.getExpiration().getTime();
      
      return new AuthResponse(newAccessToken, null, user.getUserRole(), user.getUserEmail(), refreshExp);
    } catch (ExpiredJwtException e) {
      // Refresh Token 자체가 만료된 경우
      // 정상종료이므로 사용자의 재로그인이 필요함
      log.info("Refresh token expired at {}", e.getClaims().getExpiration());
      throw new AuthException("Refresh token expired", HttpStatus.UNAUTHORIZED);
    } catch (JwtException e) {
      // 비정상 종료
      log.warn("Invalid refresh token: {}", e.getMessage());
      throw new AuthException("Invalid refresh token", HttpStatus.UNAUTHORIZED);
    }
  }
}
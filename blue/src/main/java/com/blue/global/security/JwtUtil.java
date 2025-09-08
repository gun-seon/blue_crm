package com.blue.global.security;

import com.blue.auth.dto.UserDto;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
// 토큰 생성/검증 유틸
public class JwtUtil {
  private final String SECRET = "psnsSecretKeymySecretBluemySecretWhale20251005";
  private final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
  
  // 엑세스 토큰 발급
  public String generateAccessToken(UserDto user) {
    // 엑세스 토큰 활성화 시간
    long ACCESS_TOKEN_EXPIRATION = 1000 * 30 * 1; // 5분
    
    return Jwts.builder()
        // 토큰에 추가하고 싶은 정보
        .setSubject(user.getUserEmail())
        .claim("role", user.getUserRole())
        // 토큰 기본 정보
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
        .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
        .compact();
  }
  
  // 리프레시 토큰 발급
  public String generateRefreshToken(UserDto user) {
    // 리프레시 토큰 활성화 시간
    long REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 1; // 10분
    
    return Jwts.builder()
        // 토큰에 추가하고 싶은 정보
        .setSubject(user.getUserEmail())
        .claim("role", user.getUserRole())
        // 토큰 기본 정보
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
        .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
        .compact();
  }
  
  // 토큰 검증
  public Claims validateToken(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(SECRET_KEY)
        // 시간 오차 허용 (30초)
        .setAllowedClockSkewSeconds(30)
        .build()
        .parseClaimsJws(token)
        .getBody();
  }
}

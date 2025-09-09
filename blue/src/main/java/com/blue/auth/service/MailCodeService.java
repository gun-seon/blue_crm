package com.blue.auth.service;

import com.blue.global.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
// 이메일 인증코드 발송/검증을 담당하는 서비스
public class MailCodeService {
  
  private final StringRedisTemplate redisTemplate;
  private final AsyncMailService asyncMailService;
  
  // 인증코드 유효기간
  private static final long CODE_TTL = 60 * 1; // 3분
  
  // 인증코드 생성 및 이메일 발송
  public long sendCode(String email) {
    if (email == null || email.isBlank()) {
      throw new AuthException("이메일이 필요합니다.", HttpStatus.BAD_REQUEST);
    }
    
    try {
      // 6자리 난수 생성
      String code = String.format("%06d", new Random().nextInt(999999));
      
      // Redis에 "auth:code:이메일" / "code:연장여부" / TTL 형태로 저장 (TTL 후 자동 삭제)
      redisTemplate.opsForValue().set("auth:code:" + email, code + ":0", CODE_TTL, TimeUnit.SECONDS);
      
      // 단순 텍스트 메일 발송
      SimpleMailMessage message = new SimpleMailMessage();
      message.setTo(email);
      message.setSubject("[마크CRM] 이메일 인증코드");
      message.setText("인증코드: " + code + "\n3분 안에 입력해주세요.");
      // 메일 발송은 비동기 처리
      asyncMailService.sendMail(message);
      
      return CODE_TTL; // 초 단위 TTL 반환
    } catch (Exception e) {
      throw new AuthException("이메일 발송 실패", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
  
  // 인증코드 검증
  public boolean verifyCode(String email, String code) {
    String key = "auth:code:" + email;
    String saved = redisTemplate.opsForValue().get(key);
    
    if (saved == null) {
      throw new AuthException("인증코드가 존재하지 않거나 만료되었습니다.", HttpStatus.GONE);
    }
    
    String savedCode = saved.split(":")[0];
    if (!savedCode.equals(code)) {
      throw new AuthException("인증코드가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED);
    }
    
    // 인증 성공시 코드 삭제
    redisTemplate.delete(key);
    return true;
  }
  
  // 시간 연장 (1회 한정)
  public long extendTime(String email) {
    String key = "auth:code:" + email;
    String saved = redisTemplate.opsForValue().get(key);
    
    if (saved == null) {
      throw new AuthException("인증코드가 존재하지 않거나 만료되었습니다.", HttpStatus.GONE);
    }
    
    String[] parts = saved.split(":");
    String code = parts[0];
    String extendedFlag = parts[1];
    
    // 이미 연장한 경우
    if ("1".equals(extendedFlag)) {
      throw new AuthException("이미 연장하였습니다.", HttpStatus.BAD_REQUEST);
    }
    
    // 연장 처리 (남은 시간과 무관하게 3분으로 리셋)
    redisTemplate.opsForValue().set(key, code + ":1", CODE_TTL, TimeUnit.SECONDS);
    return CODE_TTL;
  }
  
  // 남은 인증시간 확인
  public Long getRemainingTime(String email) {
    String key = "auth:code:" + email;
    
    // 남은 TTL(초 단위) 조회, 없으면 -2 (키 없음) 또는 -1 (만료시간 없음)
    long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
    
    if (ttl <= 0) {
      throw new AuthException("인증코드가 존재하지 않거나 만료되었습니다.", HttpStatus.GONE);
    }
    return ttl;
  }
}
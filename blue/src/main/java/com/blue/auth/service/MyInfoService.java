package com.blue.auth.service;

import com.blue.auth.dto.MyInfoResponse;
import com.blue.auth.mapper.MyInfoMapper;
import com.blue.global.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MyInfoService {
  
  private final MyInfoMapper myInfoMapper;
  private final PasswordEncoder passwordEncoder;
  
  @Value("${app.security.super-account}")
  private String superAccount;
  
  public MyInfoResponse getMeByEmail(String email) {
    MyInfoResponse dto = myInfoMapper.findByEmail(email);
    dto.setUserPassword("");
    dto.setSuper(superAccount.equalsIgnoreCase(email));
    return dto;
  }
  
  public void updatePhone(String email, String phone) {
    String normalizePhone = phone.trim().replaceAll("^\"|\"$", "");
    
    if (!StringUtils.hasText(normalizePhone)) {
      throw new AuthException("전화번호를 입력하세요.", HttpStatus.BAD_REQUEST);
    }
    if (!Pattern.compile("^010-\\d{4}-\\d{4}$").matcher(normalizePhone).matches()) {
      throw new AuthException("전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)", HttpStatus.BAD_REQUEST);
    }
    
    myInfoMapper.updatePhone(email, normalizePhone);
  }
  
  public void changePassword(String email, String currentPassword, String newPassword) {
    MyInfoResponse user = myInfoMapper.findByEmail(email);
    if (user == null) throw new AuthException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
    
    if (!passwordEncoder.matches(currentPassword, user.getUserPassword())) {
      throw new AuthException("현재 비밀번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST);
    }
    
    String encPw = passwordEncoder.encode(newPassword);
    myInfoMapper.updatePassword(email, encPw);
  }
}

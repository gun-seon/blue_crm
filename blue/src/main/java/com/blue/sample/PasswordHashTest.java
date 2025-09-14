package com.blue.sample;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordHashTest {
  public static void main(String[] args) {
    PasswordEncoder encoder = new BCryptPasswordEncoder();
    
    // 회원가입 없이 비밀번호 생성할 때 사용
    System.out.println(encoder.encode("test"));
  }
}
package com.blue.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {
  private String email;
  private String password;
  private String role; // SUPERADMIN / MANAGER / STAFF
  private String name;
  private String phone;
  // 센터ID는 가입승인시 설정
  // 메니저 role 중 phone 볼 수 있는 사람 설정도 가입승인시 설정
  // 가입승인 여부는 무조건 N -> 가입승인시 Y로 변경
}
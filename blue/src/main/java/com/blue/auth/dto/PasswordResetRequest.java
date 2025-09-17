package com.blue.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetRequest {
  private String email;
  private String newPassword; // 새 비밀번호
}
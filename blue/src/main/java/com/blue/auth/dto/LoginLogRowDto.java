package com.blue.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoginLogRowDto {
  private LocalDateTime createdAt;
  private Long userId;
  private String userName;
  private String userEmail;
  private String userRole;
  private String centerName;
  private String ip;
  private String userAgent;
}
package com.blue.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDto {
  private Long userId;
  private String userEmail;
  private String userPassword;
  private String userRole;
  private Long centerId;
  private String createdAt;
}
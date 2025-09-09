package com.blue.auth.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class UserDto {
  private Long userId;
  private String userEmail;
  private String userPassword;
  private String userRole;
  private Long centerId;
  private LocalDateTime userCreatedAt;
}
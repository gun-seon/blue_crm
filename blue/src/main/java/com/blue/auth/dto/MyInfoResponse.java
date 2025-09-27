package com.blue.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MyInfoResponse {
  private Long userId;
  private String userName;
  private String userEmail;
  private String userPhone;
  private String userRole;
  private String userPassword;
  private String centerName; // centerId 대신 센터 이름
  private boolean isSuper; // super계정 여부
}
package com.blue.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {
  private String accessToken;
  private String refreshToken;
  private String role;
  private String email;
  private String name;
  private Long refreshExp;
  
  @JsonProperty("isSuper")
  private boolean isSuper;
}
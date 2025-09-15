package com.blue.user.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
  private String field;
  private String value;
}
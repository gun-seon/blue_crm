package com.blue.customer.allocate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class UserContextDto {
  private Long userId;
  private String role;     // "SUPERADMIN" | "MANAGER" | "STAFF"
  private Long centerId;
  private String email;
  private String visible;  // 전화 가시권한 (여기서는 미사용)
}

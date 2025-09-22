package com.blue.customer.allocate.dto;

import lombok.Data;

/** 분배 모달 직원 검색 결과 DTO */
@Data
public class UserPickDto {
  private Long userId;
  private String userName;
  private Long centerId;
  private String centerName;
  private String role;       // MANAGER | STAFF
  private String phoneTail;  // 전화번호 뒷 4자리 (예: RIGHT(user_phone, 4))
}
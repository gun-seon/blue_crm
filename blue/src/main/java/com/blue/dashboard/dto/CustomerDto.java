package com.blue.dashboard.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerDto {
  private Long id;         // customers.customer_id or customers_duplicate.duplicate_id
  private Long userId;     // customers.user_id (통일)
  private Long centerId;   // users.center_id (JOIN)
  private String type;     // '최초' | '유효' | '중복'
  private LocalDateTime created; // 본체: customer_created_at, 중복: duplicate_created_at
}
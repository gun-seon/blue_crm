package com.blue.customer.allocate.dto;

import lombok.Data;

@Data
public class CenterPickDto {
  private Long centerId;
  private String centerName;
  private boolean hasManager; // 해당 센터에 MANAGER 존재?
  private int userCount;      // 센터 소속 인원 수
}
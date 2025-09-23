package com.blue.customer.center.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 드롭다운용 센터 목록 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CenterSimpleDto {
  private Long centerId;
  private String centerName;
}
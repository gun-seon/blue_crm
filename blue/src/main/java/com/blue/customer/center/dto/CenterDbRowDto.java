package com.blue.customer.center.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/** 센터DB 화면 한 행(본사 전용). 컬럼: 생성일/이름/전화/센터 */
@Data
public class CenterDbRowDto {
  private Long id;                 // 식별자 (customer_id 또는 duplicate_id)
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
  private LocalDateTime createdAt; // 생성일
  private String name;             // 이름
  private String phone;            // 전화 (가시권한 N이면 마스킹 적용)
  private Long centerId;           // 소속 센터 ID
  private String centerName;       // 소속 센터명 (배지 표기)
}
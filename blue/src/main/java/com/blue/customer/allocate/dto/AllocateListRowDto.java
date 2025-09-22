package com.blue.customer.allocate.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AllocateListRowDto {
  private Long id;            // customers.customer_id
  private String name;        // 고객명
  private String phone;       // 전화
  private String paststaff;       // 현재 담당자명 (nullable)
  private String division;    // '최초' | '유효'
  private String category;    // 카테고리
  private String status;      // '없음','회수',... 등
  private String centerName;  // 현재 담당자의 센터명 (nullable)
  private String source;      // DB출처
  private String content;     // 내용
  
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
  private LocalDateTime createdAt; // 생성시각
}

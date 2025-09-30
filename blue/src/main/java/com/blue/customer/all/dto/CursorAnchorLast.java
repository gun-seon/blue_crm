package com.blue.customer.all.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CursorAnchorLast {
  private String cursorCreatedAt;  // null이면 첫 페이지 시작
  private Long   cursorId;
  private int    lastPageNo;       // 절대 페이지 번호(1-based)
}
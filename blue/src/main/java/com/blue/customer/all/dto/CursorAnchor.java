package com.blue.customer.all.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CursorAnchor {
  private String cursorCreatedAt; // "yyyy-MM-dd HH:mm:ss"
  private Long   cursorId;
  private int    windowIndex;
}
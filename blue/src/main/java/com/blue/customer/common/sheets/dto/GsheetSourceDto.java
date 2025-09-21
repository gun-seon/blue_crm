package com.blue.customer.common.sheets.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GsheetSourceDto {
  private Long sourceId;
  private String spreadsheetId;
  private String sheetName;
  private Integer cursorRow; // 헤더=1, 다음 읽기는 cursorRow+1
  private LocalDateTime updatedAt;
}

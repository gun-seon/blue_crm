package com.blue.auth.dto;

import lombok.Data;

@Data
public class SheetSettingsDto {
  private String sheetId;   // spreadsheet_id
  private Integer startRow; // cursor_row
}
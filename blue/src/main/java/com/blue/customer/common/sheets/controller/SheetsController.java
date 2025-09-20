package com.blue.customer.common.sheets.controller;

import com.blue.customer.common.sheets.service.SheetsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SheetsController {
  private final SheetsService service;
  
  public SheetsController(SheetsService service) {
    this.service = service;
  }
  
  @GetMapping("/api/sheets/test")
  public String testSheets() {
    // 테스트용: 시트 ID와 범위 넣기
    String spreadsheetId = "1tD9AwbcTa9ik29JPMNgEHmsZNs2xoYIc1HNafpnb0gA";
    String range = "Sheet1!A1:D10";
    service.printSheet(spreadsheetId, range);
    return "시트 데이터 콘솔 출력 완료";
  }
}
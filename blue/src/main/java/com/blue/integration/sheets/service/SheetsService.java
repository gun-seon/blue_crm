package com.blue.integration.sheets.service;

import com.blue.integration.sheets.client.GoogleSheetsClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SheetsService {
  private final GoogleSheetsClient client;
  
  public SheetsService(GoogleSheetsClient client) {
    this.client = client;
  }
  
  public void printSheet(String spreadsheetId, String range) {
    try {
      List<List<Object>> rows = client.readRange(spreadsheetId, range);
      if (rows == null || rows.isEmpty()) {
        System.out.println("데이터가 없습니다.");
      } else {
        rows.forEach(System.out::println);
      }
    } catch (Exception e) {
      throw new RuntimeException("Google Sheets 읽기 실패", e);
    }
  }
}
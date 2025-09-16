package com.blue.sample;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.util.List;

@RestController
public class ExcelController {
  
  @GetMapping("/api/excel")
  public ResponseEntity<byte[]> downloadExcel() throws Exception {
    // 더미 데이터
    List<String[]> data = List.of(
        new String[]{"이름", "전화번호", "상태"},
        new String[]{"홍길동", "010-1234-5678", "정상"},
        new String[]{"김영희", "010-2222-3333", "대기"},
        new String[]{"이철수", "010-9999-0000", "보류"}
    );
    
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("데이터");
    
    // 데이터 채우기
    for (int i = 0; i < data.size(); i++) {
      Row row = sheet.createRow(i);
      for (int j = 0; j < data.get(i).length; j++) {
        row.createCell(j).setCellValue(data.get(i)[j]);
      }
    }
    
    // 바이트 배열 변환
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    workbook.write(out);
    workbook.close();
    
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=data.xlsx")
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(out.toByteArray());
  }
}
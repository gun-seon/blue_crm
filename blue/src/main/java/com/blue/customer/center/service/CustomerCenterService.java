package com.blue.customer.center.service;

import com.blue.customer.all.dto.PagedResponse;
import com.blue.customer.all.dto.UserContextDto;
import com.blue.customer.center.dto.CenterDbRowDto;
import com.blue.customer.center.dto.CenterSimpleDto;
import com.blue.customer.center.mapper.CustomerCenterMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerCenterService {
  
  private final CustomerCenterMapper mapper;
  
  /** 본사 제외 센터 목록 */
  public List<CenterSimpleDto> getCenters() {
    return mapper.findCentersExcludingHQ();
  }
  
  /** 센터DB 목록 (본사 전용) */
  public PagedResponse<CenterDbRowDto> getCenterDb(
      String callerEmail, int page, int size,
      String keyword, String dateFrom, String dateTo,
      String category, String division, Long centerId
  ) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null) throw new IllegalArgumentException("인증 사용자 정보를 찾을 수 없습니다.");
    if (!"SUPERADMIN".equals(me.getRole())) throw new IllegalArgumentException("본사 전용 메뉴입니다.");
    
    // 카테고리 필터 무시 (향후 확장성 고려해서 필드는 남겨둠)
    category = null;
    
    int offset = (page - 1) * size;
    List<CenterDbRowDto> items = mapper.findForAdmin(
        offset, size, keyword, dateFrom, dateTo, category, division, centerId, me.getVisible()
    );
    int total = mapper.countForAdmin(
        keyword, dateFrom, dateTo, category, division, centerId, me.getVisible()
    );
    
    // SUPERADMIN 가시권한 N: 전화 마스킹
    if ("N".equalsIgnoreCase(me.getVisible())) {
      items.forEach(r -> r.setPhone(maskPhone(r.getPhone())));
    }
    
    int totalPages = (int) Math.ceil((double) total / size);
    if (totalPages == 0) totalPages = 1;
    return new PagedResponse<>(items, totalPages, total);
  }
  
  public ResponseEntity<byte[]> exportExcel(
      String callerEmail,
      String keyword, String dateFrom, String dateTo,
      String category, String division, Long centerId
  ) {
    // 현재 필터 조건으로 최대 5만 건까지 출력 (필요 시 스트리밍 전환)
    final int MAX = 50_000;
    PagedResponse<CenterDbRowDto> page = getCenterDb(
        callerEmail, 1, MAX, keyword, dateFrom, dateTo, category, division, centerId
    );
    List<CenterDbRowDto> data = page.getItems();
    
    try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sh = wb.createSheet("센터DB");
      // 헤더
      Row h = sh.createRow(0);
      h.createCell(0).setCellValue("생성일");
      h.createCell(1).setCellValue("이름");
      h.createCell(2).setCellValue("전화번호");
      h.createCell(3).setCellValue("센터");
      
      int r = 1;
      for (CenterDbRowDto row : data) {
        Row rr = sh.createRow(r++);
        rr.createCell(0).setCellValue(row.getCreatedAt() == null ? "" : row.getCreatedAt().toString().replace('T',' '));
        rr.createCell(1).setCellValue(nz(row.getName()));
        rr.createCell(2).setCellValue(nz(row.getPhone()));     // 가시권한 N이면 이미 마스킹된 값
        rr.createCell(3).setCellValue(nz(row.getCenterName()));
      }
      wb.write(out);
      
      String filename = URLEncoder.encode("center-db.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
          .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
          .body(out.toByteArray());
    } catch (Exception e) {
      throw new RuntimeException("엑셀 생성 실패", e);
    }
  }
  
  private String nz(String s) { return s == null ? "" : s; }
  
  private String maskPhone(String phone) {
    if (phone == null) return null;
    String digits = phone.replaceAll("\\D", "");
    if (digits.length() < 7) return phone;
    return digits.substring(0,3) + "-****-" + digits.substring(digits.length()-4);
  }
}

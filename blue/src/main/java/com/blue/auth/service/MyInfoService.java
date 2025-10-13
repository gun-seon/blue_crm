package com.blue.auth.service;

import com.blue.auth.dto.*;
import com.blue.auth.mapper.MyInfoMapper;
import com.blue.global.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MyInfoService {
  
  private final MyInfoMapper myInfoMapper;
  private final PasswordEncoder passwordEncoder;
  
  public MyInfoResponse getMeByEmail(String email) {
    MyInfoResponse dto = myInfoMapper.findByEmail(email);
    dto.setUserPassword(""); // 보안상 비번 제거
    return dto;
  }
  
  public void updatePhone(String email, String phone) {
    String normalizePhone = phone.trim().replaceAll("^\"|\"$", "");
    
    if (!StringUtils.hasText(normalizePhone)) {
      throw new AuthException("전화번호를 입력하세요.", HttpStatus.BAD_REQUEST);
    }
    if (!Pattern.compile("^010-\\d{4}-\\d{4}$").matcher(normalizePhone).matches()) {
      throw new AuthException("전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)", HttpStatus.BAD_REQUEST);
    }
    
    myInfoMapper.updatePhone(email, normalizePhone);
  }
  
  public void changePassword(String email, String currentPassword, String newPassword) {
    MyInfoResponse user = myInfoMapper.findByEmail(email);
    if (user == null) throw new AuthException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
    
    if (!passwordEncoder.matches(currentPassword, user.getUserPassword())) {
      throw new AuthException("현재 비밀번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST);
    }
    
    String encPw = passwordEncoder.encode(newPassword);
    myInfoMapper.updatePassword(email, encPw);
  }
  
  // 구글 스프레드 시트 관련
  @Transactional(readOnly = true)
  public SheetSettingsDto getSheetSettings(String email) {
    ensureSuper(email);
    SheetSettingsDto dto = myInfoMapper.findOneBySourceId(1L);
    return (dto == null) ? new SheetSettingsDto() : dto;
  }
  
  @Transactional
  public void saveSheetSettings(String email, SheetSettingsDto dto) {
    ensureSuper(email);
    if (dto == null || !StringUtils.hasText(dto.getSheetId())) {
      throw new AuthException("sheetId는 필수입니다.", HttpStatus.BAD_REQUEST);
    }
    String sheetId = dto.getSheetId().trim();
    if (sheetId.length() > 80) {
      throw new AuthException("sheetId가 너무 깁니다. (최대 80자)", HttpStatus.BAD_REQUEST);
    }
    
    int startRow = (dto.getStartRow() == null || dto.getStartRow() < 1) ? 1 : dto.getStartRow();
    
    String sheetName = (dto.getSheetName() == null) ? "" : dto.getSheetName().trim();
    if (sheetName.length() > 100) {
      throw new AuthException("sheetName이 너무 깁니다. (최대 100자)", HttpStatus.BAD_REQUEST);
    }
    
    myInfoMapper.updateOne(1L, sheetId, startRow, sheetName);
  }
  
  // 센터관리
  @Transactional(readOnly = true)
  public List<CenterDto> listCenters(String email) {
    ensureSuper(email);
    return myInfoMapper.findAll();
  }
  
  @Transactional
  public void addCenter(String email, String centerName) {
    ensureSuper(email);
    if (!StringUtils.hasText(centerName)) {
      throw new AuthException("센터 이름을 입력하세요.", HttpStatus.BAD_REQUEST);
    }
    String name = centerName.trim();
    if (name.length() > 100) {
      throw new AuthException("센터 이름이 너무 깁니다. (최대 100자)", HttpStatus.BAD_REQUEST);
    }
    if (myInfoMapper.existsByName(name)) {
      throw new AuthException("이미 존재하는 센터명입니다.", HttpStatus.CONFLICT);
    }
    myInfoMapper.insert(name);
  }
  
  @Transactional
  public void removeCenter(String email, long centerId) {
    ensureSuper(email);
    // 직원 존재 여부 체크
    int userCount = myInfoMapper.countUsersInCenter(centerId);
    if (userCount > 0) {
      throw new AuthException("센터에 소속된 직원이 있어 삭제할 수 없습니다.", HttpStatus.CONFLICT);
    }
    int affected = myInfoMapper.delete(centerId);
    if (affected == 0) {
      throw new AuthException("존재하지 않는 센터입니다.", HttpStatus.NOT_FOUND);
    }
  }
  
  /** 접속 로그 엑셀 (login_logs 구조 기반, login_at 기준, 헤더/키 하드코딩) */
  @Transactional(readOnly = true)
  public ResponseEntity<byte[]> exportLoginLogsExcel(String callerEmail, String fromYmd, String toYmd) {
    ensureSuper(callerEmail);
    
    LocalDate from = LocalDate.parse(fromYmd);
    LocalDate to   = LocalDate.parse(toYmd);
    if (from.isAfter(to)) throw new AuthException("시작일이 종료일보다 늦을 수 없습니다.", HttpStatus.BAD_REQUEST);
    
    final int MAX = 50_000;
    
    // 1) 원본 행 그대로 (SELECT * FROM login_logs WHERE login_at BETWEEN ...)
    List<Map<String,Object>> rows = myInfoMapper.findLoginLogsRaw(fromYmd, toYmd, MAX);
    
    // 2) 엑셀 헤더(표시용) & DB 컬럼 키(매핑용) 하드코딩
    final String[] headers = {
        "로그ID", "사용자ID", "이름", "전화번호", "역할", "로그인 시간", "로그아웃 시간"
    };
    final String[] keys = {
        "log_id", "user_id", "snapshot_name", "snapshot_phone", "snapshot_role", "login_at", "logout_at"
    };
    
    try (var wb = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
      Sheet sh = wb.createSheet("login_log");
      
      // 헤더
      Row h = sh.createRow(0);
      for (int i = 0; i < headers.length; i++) {
        h.createCell(i).setCellValue(headers[i]);
      }
      
      // 데이터
      int r = 1;
      for (var row : rows) {
        Row rr = sh.createRow(r++);
        for (int c = 0; c < keys.length; c++) {
          Object v = row.get(keys[c]);
          rr.createCell(c).setCellValue(v == null ? "" : String.valueOf(v));
        }
      }
      
      wb.write(out);
      String filename = URLEncoder.encode("login_log_" + fromYmd + "_" + toYmd + ".xlsx", StandardCharsets.UTF_8)
          .replace("+", "%20");
      return ResponseEntity.ok()
          .header("Content-Disposition", "attachment; filename*=UTF-8''" + filename)
          .contentType(org.springframework.http.MediaType.parseMediaType(
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
          .body(out.toByteArray());
    } catch (Exception e) {
      throw new RuntimeException("엑셀 생성 실패", e);
    }
  }
  
  /** 슈퍼 권한 위임 대상 조회 */
  @Transactional(readOnly = true)
  public MyInfoResponse lookupDelegateTargetByEmail(String callerEmail, String targetEmail) {
    ensureSuper(callerEmail);
    
    var dto = myInfoMapper.findUserBriefByEmail(targetEmail);
    if (dto == null) throw new AuthException("대상 사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
    dto.setUserPassword(""); // 안전
    return dto;
  }
  
  /** 슈퍼 권한 위임 (슈퍼계정의 단일성 보장) */
  @Transactional
  public void delegateSuper(String callerEmail, long targetUserId) {
    ensureSuper(callerEmail);
    
    MyInfoResponse me = myInfoMapper.findByEmail(callerEmail);
    if (me == null) throw new AuthException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
    if (me.getUserId() != null && me.getUserId().equals(targetUserId)) {
      throw new AuthException("자기 자신에게 위임할 수 없습니다.", HttpStatus.BAD_REQUEST);
    }
    
    // 1) 기존 SUPERADMIN 전부 MANAGER로 (정책: 단일 SUPERADMIN)
    myInfoMapper.demoteAllSuperToManager();
    
    // 2) 대상 승격
    int updated = myInfoMapper.updateRoleByUserId(targetUserId, "SUPERADMIN");
    if (updated == 0) throw new AuthException("대상 사용자가 존재하지 않습니다.", HttpStatus.NOT_FOUND);
  }
  
  // 공통 함수
  private void ensureSuper(String email) {
    MyInfoResponse user = myInfoMapper.findByEmail(email);
    if (user == null || !user.isSuper()) {
      throw new AuthException("접근 권한이 없습니다.", HttpStatus.FORBIDDEN);
    }
  }
  
}

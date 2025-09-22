package com.blue.auth.service;

import com.blue.auth.dto.CenterDto;
import com.blue.auth.dto.MyInfoResponse;
import com.blue.auth.dto.SheetSettingsDto;
import com.blue.auth.mapper.MyInfoMapper;
import com.blue.global.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MyInfoService {
  
  private final MyInfoMapper myInfoMapper;
  private final PasswordEncoder passwordEncoder;
  
  @Value("${app.security.super-account}")
  private String superAccount;
  
  public MyInfoResponse getMeByEmail(String email) {
    MyInfoResponse dto = myInfoMapper.findByEmail(email);
    dto.setUserPassword("");
    dto.setSuper(superAccount.equalsIgnoreCase(email));
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
    myInfoMapper.updateOne(1L, sheetId, startRow);
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
  
  // 공통 함수
  private void ensureSuper(String email) {
    if (email == null || !superAccount.equalsIgnoreCase(email.trim())) {
      throw new AuthException("접근 권한이 없습니다.", HttpStatus.FORBIDDEN);
    }
  }
  
}

package com.blue.auth.controller;


import com.blue.auth.dto.CenterDto;
import com.blue.auth.dto.ChangePasswordRequest;
import com.blue.auth.dto.CreateCenterRequest;
import com.blue.auth.dto.MyInfoResponse;
import com.blue.auth.dto.SheetSettingsDto;
import com.blue.auth.service.MyInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MyInfoController {
  
  private final MyInfoService myInfoService;
  
  // 내 정보 조회
  @GetMapping
  public ResponseEntity<MyInfoResponse> getMe(Authentication auth) {
    String email = auth.getName();
//    System.out.println(email);
    MyInfoResponse me = myInfoService.getMeByEmail(email);
    return ResponseEntity.ok(me);
  }
  
  // 전화번호 변경
  @PutMapping("/phone")
  public ResponseEntity<Void> updatePhone(@RequestBody String phone, Authentication auth) {
    String email = auth.getName();
//    System.out.println(phone);
    myInfoService.updatePhone(email, phone);
    return ResponseEntity.ok().build();
  }
  
  // 비밀번호 변경
  @PutMapping("/password")
  public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest req, Authentication auth) {
    String email = auth.getName();
//    System.out.println(email);
    myInfoService.changePassword(email, req.getCurrentPassword(), req.getNewPassword());
    return ResponseEntity.ok().build();
  }
  
  // 스프레스 시트 조회
  @GetMapping("/sheet-settings")
  public ResponseEntity<SheetSettingsDto> getSheetSettings(Authentication auth) {
    String email = auth.getName();
    return ResponseEntity.ok(myInfoService.getSheetSettings(email));
  }
  
  // 스프레드 시트 수정
  @PutMapping("/sheet-settings")
  public ResponseEntity<Void> putSheetSettings(@RequestBody SheetSettingsDto dto, Authentication auth) {
    String email = auth.getName();
    myInfoService.saveSheetSettings(email, dto);
    return ResponseEntity.ok().build();
  }
  
  // 센터 목록 (특별계정 전용)
  @GetMapping("/centers")
  public ResponseEntity<List<CenterDto>> list(Authentication auth) {
    String email = auth.getName();
    return ResponseEntity.ok(myInfoService.listCenters(email));
  }
  
  // 센터 추가 (특별계정 전용)
  @PostMapping("/centers")
  public ResponseEntity<Void> add(@RequestBody CreateCenterRequest req, Authentication auth) {
    String email = auth.getName();
    myInfoService.addCenter(email, req.getCenterName());
    return ResponseEntity.ok().build();
  }
  
  // 센터 삭제 (특별계정 전용, 직원 존재 시 409)
  @DeleteMapping("/centers/{id}")
  public ResponseEntity<Void> delete(@PathVariable("id") long id, Authentication auth) {
    String email = auth.getName();
    myInfoService.removeCenter(email, id);
    return ResponseEntity.ok().build();
  }
}
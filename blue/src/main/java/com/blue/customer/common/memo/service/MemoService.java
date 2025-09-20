package com.blue.customer.common.memo.service;

import com.blue.customer.all.dto.UserContextDto;
import com.blue.customer.common.memo.dto.MemoDetailDto;
import com.blue.customer.common.memo.dto.MemoUpdateDto;
import com.blue.customer.common.memo.mapper.MemoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemoService {
  
  private final MemoMapper mapper;
  
  @Transactional(readOnly = true)
  public MemoDetailDto getMemo(Long customerId, String callerEmail) {
    // 권한 체크는 "조회"는 보통 완화하지만, 필요하면 동일 정책으로 제한
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null) throw new IllegalArgumentException("인증 사용자 정보를 찾을 수 없습니다.");
    
    Integer exists = mapper.existsCustomerById(customerId);
    if (exists == null || exists == 0) throw new IllegalArgumentException("고객을 찾을 수 없습니다.");
    
    // MANAGER/STAFF는 소유권 검사
    switch (me.getRole()) {
      case "SUPERADMIN" -> {}
      case "MANAGER" -> {
        Integer ownsCenter = mapper.customerOwnedByCenter(customerId, me.getCenterId());
        if (ownsCenter == null || ownsCenter == 0) throw new IllegalArgumentException("권한이 없습니다.");
      }
      case "STAFF" -> {
        Integer ownsSelf = mapper.customerOwnedByUser(customerId, me.getUserId());
        if (ownsSelf == null || ownsSelf == 0) throw new IllegalArgumentException("권한이 없습니다.");
      }
      default -> throw new IllegalStateException("Unknown role: " + me.getRole());
    }
    
    // 1) 기본 메모 블록
    MemoDetailDto dto = mapper.selectCustomerMemoBlock(customerId);
    if (dto == null) throw new IllegalArgumentException("고객을 찾을 수 없습니다.");
    
    // 2) 전화번호로 과거 담당자 이력 조회 후 세팅 (resultMap 없이)
    if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
      dto.setStaffHistory(mapper.selectPastStaffByPhone(dto.getPhone()));
    }
    
    return dto;
  }
  
  @Transactional
  public void updateMemo(String callerEmail, Long customerId, MemoUpdateDto dto) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null) throw new IllegalArgumentException("인증 사용자 정보를 찾을 수 없습니다.");
    
    Integer exists = mapper.existsCustomerById(customerId);
    if (exists == null || exists == 0) throw new IllegalArgumentException("중복 DB는 수정할 수 없습니다.");
    
    switch (me.getRole()) {
      case "SUPERADMIN" -> {}
      case "MANAGER" -> {
        Integer ownsCenter = mapper.customerOwnedByCenter(customerId, me.getCenterId());
        if (ownsCenter == null || ownsCenter == 0) throw new IllegalArgumentException("권한이 없습니다.");
      }
      case "STAFF" -> {
        Integer ownsSelf = mapper.customerOwnedByUser(customerId, me.getUserId());
        if (ownsSelf == null || ownsSelf == 0) throw new IllegalArgumentException("권한이 없습니다.");
      }
      default -> throw new IllegalStateException("Unknown role: " + me.getRole());
    }
    
    // 빈문자열 → null 정규화
    String promise = (dto.getPromiseTime() == null || dto.getPromiseTime().isBlank())
        ? null : dto.getPromiseTime();
    
    mapper.updateCustomerMemoBlock(
        customerId,
        normalize(dto.getMemo()),
        normalize(dto.getStatus()),
        promise,
        normalize(dto.getTradingviewId()),
        normalize(dto.getTelegramNickname()),
        nz(dto.getFreeRoom()),
        nz(dto.getSignalRoom()),
        nz(dto.getExchangeJoined()),
        nz(dto.getTradingviewJoined()),
        nz(dto.getIndicatorFlag())
    );
  }
  
  private String normalize(String s) { return (s == null || s.isBlank()) ? null : s; }
  private Integer nz(Integer v) { return v == null ? 0 : v; }
}

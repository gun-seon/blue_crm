package com.blue.customer.common.memo.service;

import com.blue.customer.all.dto.UserContextDto;
import com.blue.customer.common.memo.dto.MemoUpdateDto;
import com.blue.customer.common.memo.mapper.MemoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemoService {
  
  private final MemoMapper mapper;
  
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
    
    mapper.updateCustomerMemoBlock(
        customerId,
        dto.getMemo(),
        dto.getStatus(),
        dto.getPromiseTime(),
        dto.getTradingviewId(),
        dto.getTelegramNickname(),
        dto.getFreeRoom(),
        dto.getSignalRoom(),
        dto.getExchangeJoined(),
        dto.getTradingviewJoined(),
        dto.getIndicatorFlag()
    );
  }
}

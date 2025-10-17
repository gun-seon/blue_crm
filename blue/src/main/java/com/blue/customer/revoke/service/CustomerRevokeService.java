package com.blue.customer.revoke.service;

import com.blue.customer.all.dto.PagedResponse;
import com.blue.customer.revoke.dto.*;
import com.blue.customer.revoke.mapper.CustomerRevokeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerRevokeService {
  
  private final CustomerRevokeMapper mapper;
  
  public PagedResponse<RevokeListRowDto> list(String callerEmail,
                                              int page, int size,
                                              String keyword, String dateFrom, String dateTo,
                                              String category, String division, String sort) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null) throw new IllegalArgumentException("인증 사용자 정보를 찾을 수 없습니다.");
    
    int offset = (page - 1) * size;
    List<RevokeListRowDto> items;
    int total;
    
    switch (me.getRole()) {
      case "SUPERADMIN" -> {
        items = mapper.findListForHq(offset, size, keyword, dateFrom, dateTo, category, division, sort, me.getVisible());
        total = mapper.countListForHq(keyword, dateFrom, dateTo, category, division, me.getVisible());
      }
      case "MANAGER" -> {
        items = mapper.findListForManager(offset, size, keyword, dateFrom, dateTo, category, sort, me.getCenterId(), me.getVisible(), me.getUserId());
        total = mapper.countListForManager(keyword, dateFrom, dateTo, category, me.getCenterId(), me.getVisible(), me.getUserId());
      }
      default -> throw new IllegalArgumentException("이 메뉴는 본사/센터장만 사용할 수 있습니다.");
    }
    
    int totalPages = (int) Math.ceil((double) total / size);
    return new PagedResponse<>(items, totalPages, total);
  }
  
  @Transactional
  public RevokeResult revokeByHq(String callerEmail, RevokeReq req) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null || !"SUPERADMIN".equals(me.getRole())) {
      throw new IllegalArgumentException("본사만 수행할 수 있습니다.");
    }
    if (req.getCustomerIds() == null || req.getCustomerIds().isEmpty()) {
      return new RevokeResult(0, 0);
    }
    
    // 대상 잠금 (상태 NOT IN 없음/회수)
    List<Long> lockIds = mapper.lockCustomersForRevokeByHq(req.getCustomerIds());
    if (lockIds.isEmpty()) return new RevokeResult(0, req.getCustomerIds().size());
    
    // 회수 처리: 상태=회수, 담당자=NULL, 예약시간=NULL
    mapper.updateToRevoked(lockIds);
    
    return new RevokeResult(lockIds.size(), req.getCustomerIds().size() - lockIds.size());
  }
  
  @Transactional
  public RevokeResult revokeByManager(String callerEmail, RevokeReq req) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null || !"MANAGER".equals(me.getRole())) {
      throw new IllegalArgumentException("센터장만 수행할 수 있습니다.");
    }
    if (req.getCustomerIds() == null || req.getCustomerIds().isEmpty()) {
      return new RevokeResult(0, 0);
    }
    
    // 같은 센터 소속 담당자의 건만 락
    List<Long> lockIds = mapper.lockCustomersForRevokeByManager(req.getCustomerIds(), me.getCenterId(), me.getUserId());
    if (lockIds.isEmpty()) return new RevokeResult(0, req.getCustomerIds().size());
    
    mapper.updateToRevoked(lockIds);
    return new RevokeResult(lockIds.size(), req.getCustomerIds().size() - lockIds.size());
  }
}

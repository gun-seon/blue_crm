package com.blue.customer.allocate.service;

import com.blue.customer.all.dto.PagedResponse;
import com.blue.customer.allocate.dto.*;
import com.blue.customer.allocate.mapper.CustomerAllocateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerAllocateService {
  
  private final CustomerAllocateMapper mapper;
  
  public PagedResponse<AllocateListRowDto> list(String callerEmail,
                                                int page, int size,
                                                String keyword, String dateFrom, String dateTo,
                                                String category, String division, String sort) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null) throw new IllegalArgumentException("인증 사용자 정보를 찾을 수 없습니다.");
    
    int offset = (page - 1) * size;
    List<AllocateListRowDto> items;
    int total;
    
    switch (me.getRole()) {
      case "SUPERADMIN" -> {
        items = mapper.findListForHq(offset, size, keyword, dateFrom, dateTo, category, division, sort, me.getVisible());
        total = mapper.countListForHq(keyword, dateFrom, dateTo, category, division, me.getVisible());
      }
      case "MANAGER" -> {
        items = mapper.findListForManager(offset, size, keyword, dateFrom, dateTo, category, sort, me.getUserId(), me.getVisible());
        total = mapper.countListForManager(keyword, dateFrom, dateTo, category, me.getUserId(), me.getVisible());
      }
      default -> throw new IllegalArgumentException("이 메뉴는 본사/센터장만 사용할 수 있습니다.");
    }
    
    int totalPages = (int) Math.ceil((double) total / size);
    return new PagedResponse<>(items, totalPages, total);
  }
  
  @Transactional
  public AllocateResult allocateByHq(String callerEmail, AllocateHqReq req) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null || !"SUPERADMIN".equals(me.getRole())) {
      throw new IllegalArgumentException("본사만 수행할 수 있습니다.");
    }
    if (req.getCustomerIds() == null || req.getCustomerIds().isEmpty()) {
      return new AllocateResult(0, 0);
    }
    if (req.getTargetCenterId() == null) {
      throw new IllegalArgumentException("센터를 선택하세요.");
    }
    
    Long targetUserId = req.getTargetUserId();
    if (targetUserId != null) {
      Integer ok = mapper.userBelongsToCenter(targetUserId, req.getTargetCenterId());
      if (ok == null || ok == 0) throw new IllegalArgumentException("선택한 직원이 해당 센터 소속이 아닙니다.");
    } else {
      targetUserId = mapper.findCenterHeadUserId(req.getTargetCenterId());
      if (targetUserId == null) throw new IllegalArgumentException("해당 센터의 센터장이 없습니다.");
    }
    
    // 조건에 맞는 대상만 잠금
    List<Long> lockIds = mapper.lockCustomersForHq(req.getCustomerIds());
    if (lockIds.isEmpty()) return new AllocateResult(0, req.getCustomerIds().size());
    
    // 새로 배정받는 사람(센터장 or 선택한 직원)을 과거이력에 추가
    mapper.insertPastForNewOwner(lockIds, targetUserId);
    
    // 소유자 변경
    mapper.updateOwner(lockIds, targetUserId);
    
    // 본사 분배 시 상태 = '신규'
    mapper.updateStatusToNew(lockIds);
    
    return new AllocateResult(lockIds.size(), req.getCustomerIds().size() - lockIds.size());
  }
  
  @Transactional
  public AllocateResult allocateByManager(String callerEmail, AllocateMgrReq req) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null || !"MANAGER".equals(me.getRole())) {
      throw new IllegalArgumentException("센터장만 수행할 수 있습니다.");
    }
    if (req.getCustomerIds() == null || req.getCustomerIds().isEmpty()) {
      return new AllocateResult(0, 0);
    }
    if (req.getTargetUserId() == null) {
      throw new IllegalArgumentException("직원을 선택하세요.");
    }
    
    Integer ok = mapper.staffInSameCenter(me.getUserId(), req.getTargetUserId());
    if (ok == null || ok == 0) throw new IllegalArgumentException("같은 센터의 직원만 분배할 수 있습니다.");
    
    // 조건에 맞는 대상만 잠금(현재담당=센터장 본인, 상태!=완료)
    List<Long> lockIds = mapper.lockCustomersForManager(req.getCustomerIds(), me.getUserId());
    if (lockIds.isEmpty()) return new AllocateResult(0, req.getCustomerIds().size());
    
    // 상태가 '신규'인 건만 센터장 이력 제거 (임시배정 후 즉시 재분배 케이스)
    mapper.deleteManagerFromPast(lockIds, me.getUserId());
    
    // 소유자 변경
    mapper.updateOwner(lockIds, req.getTargetUserId());
    
    // 새 담당자(=직원) 과거 이력 적재
    mapper.insertPastForNewOwner(lockIds, req.getTargetUserId());
    
    return new AllocateResult(lockIds.size(), req.getCustomerIds().size() - lockIds.size());
  }
  
  // 직원 검색
  public List<UserPickDto> searchUsersForAllocate(String email, Long centerId, String q) {
    UserContextDto me = mapper.findUserContextByEmail(email);
    if (me == null) throw new AccessDeniedException("No user");
    
    Long targetCenterId = null;
    switch (me.getRole()) {
      case "SUPERADMIN" -> {
        // HQ는 선택 센터가 있으면 그 센터, 없으면 전체
        targetCenterId = centerId; // null이면 전체
      }
      case "MANAGER" -> {
        // 매니저는 항상 본인 센터 강제 (전달 centerId는 무시)
        if (me.getCenterId() == null) throw new AccessDeniedException("Manager center not set");
        targetCenterId = me.getCenterId();
      }
      default -> throw new AccessDeniedException("Forbidden");
    }
    
    // 같은 센터의 'STAFF'만 후보로 (요구사항에 맞춤)
    return mapper.searchStaffForAllocate(targetCenterId, q);
  }
  
  // 센터 조회
  public List<CenterPickDto> centersForAllocate(String callerEmail) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null || !"SUPERADMIN".equals(me.getRole())) {
      throw new IllegalArgumentException("본사만 조회할 수 있습니다.");
    }
    return mapper.findCentersForAllocate();
  }
}

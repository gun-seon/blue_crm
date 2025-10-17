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
        // HQ 리스트는 "담당자 없음 AND 상태 ∈ {없음, 회수}"가 XML에서 강제됨
        items = mapper.findListForHq(offset, size, keyword, dateFrom, dateTo, category, division, sort, me.getVisible());
        total = mapper.countListForHq(keyword, dateFrom, dateTo, category, division, me.getVisible());
      }
      case "MANAGER" -> {
        // MANAGER 리스트는 "담당자=나 AND 상태=없음"이 XML에서 강제됨
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
    if (req.getTargetUserId() == null) {
      throw new IllegalArgumentException("센터장 또는 직원을 반드시 선택해야 합니다.");
    }
    
    Long targetUserId = req.getTargetUserId();
    Integer ok = mapper.userBelongsToCenter(targetUserId, req.getTargetCenterId());
    if (ok == null || ok == 0) {
      throw new IllegalArgumentException("선택한 직원이 해당 센터 소속이 아닙니다.");
    }
    String targetRole = mapper.findUserRole(targetUserId);
    
    // 조건에 맞는 대상만 잠금
    List<Long> lockIds = mapper.lockCustomersForHq(req.getCustomerIds());
    if (lockIds.isEmpty()) return new AllocateResult(0, req.getCustomerIds().size());
    
    // 새로 배정받는 사람(센터장 or 선택한 직원)을 과거이력에 추가
    mapper.insertPastForNewOwner(lockIds, targetUserId);
    
    // 소유자 변경
    mapper.updateOwner(lockIds, targetUserId);
    
    // 본사 기준 상태 변경 규칙
    // - 센터장에게 분배: 상태를 '없음'으로 통일(회수도 없음으로 전환)
    // - 담당자에게 분배: '신규'
    if ("MANAGER".equals(targetRole)) {
      mapper.updateStatusToNone(lockIds);
    } else {
      mapper.updateStatusToNew(lockIds);
    }
    
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
    
    // 조건에 맞는 대상만 잠금(현재담당=센터장 본인 AND 상태=없음)
    List<Long> lockIds = mapper.lockCustomersForManager(req.getCustomerIds(), me.getUserId());
    if (lockIds.isEmpty()) return new AllocateResult(0, req.getCustomerIds().size());
    
    // 팀장이 '팀원에게' 분배하는 경우, 센터장 과거 이력 삭제 후 팀원 본인 이력 추가
    boolean selfReallocate = req.getTargetUserId().equals(me.getUserId());
    if (!selfReallocate) {
      mapper.deleteManagerFromPast(lockIds, me.getUserId());
      mapper.insertPastForNewOwner(lockIds, req.getTargetUserId());
    }
    
    // 소유자 변경
    mapper.updateOwner(lockIds, req.getTargetUserId());
    
    // 팀장→본인/팀원 모두 '신규'
    mapper.updateStatusToNew(lockIds);
    
    return new AllocateResult(lockIds.size(), req.getCustomerIds().size() - lockIds.size());
  }
  
  // 직원 검색
  public List<UserPickDto> searchUsersForAllocate(String email, Long centerId, String q) {
    UserContextDto me = mapper.findUserContextByEmail(email);
    if (me == null) throw new AccessDeniedException("No user");
    
    Long targetCenterId = null;
    switch (me.getRole()) {
      case "SUPERADMIN" -> {
        targetCenterId = centerId; // null이면 전체
      }
      case "MANAGER" -> {
        // 매니저는 항상 본인 센터 강제 (전달 centerId는 무시)
        if (me.getCenterId() == null) throw new AccessDeniedException("센터장의 센터가 미배정 상태입니다.");
        targetCenterId = me.getCenterId();
      }
      default -> throw new AccessDeniedException("Forbidden");
    }
    
    // 같은 센터의 MANAGER/STAFF 모두 반환
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

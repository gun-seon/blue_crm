package com.blue.customer.all.service;

import com.blue.customer.all.dto.AllDbRowDto;
import com.blue.customer.all.dto.PagedResponse;
import com.blue.customer.all.dto.UpdateFieldDto;
import com.blue.customer.all.dto.UserContextDto;
import com.blue.customer.all.mapper.CustomerAllMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CustomerAllService {
  
  private final CustomerAllMapper mapper;
  
  public PagedResponse<AllDbRowDto> getAll(
      String callerEmail, int page, int size,
      String keyword, String dateFrom, String dateTo,
      String category, String division, String sort,
      String status, String mine, Long staffUserId
  ) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null) throw new IllegalArgumentException("인증 사용자 정보를 찾을 수 없습니다.");
    
    int offset = (page - 1) * size;
    List<AllDbRowDto> items;
    int total;
    
    switch (me.getRole()) {
      case "SUPERADMIN" -> {
//        System.out.println("datefrom: " + dateFrom);
//        System.out.println("dateto: " + dateTo);
        items = mapper.findAllForAdmin(offset, size, keyword, dateFrom, dateTo, category, division, sort, status, me.getVisible());
        total = mapper.countAllForAdmin(keyword, dateFrom, dateTo, category, division, status, me.getVisible());
      }
      case "MANAGER" -> {
        // mine=Y이면 '내 DB만' → 클라 staffUserId 무시, 토큰의 본인 ID 강제
        boolean mineOnly = "Y".equalsIgnoreCase(mine);
        if (mineOnly) {
          Long myUserId = me.getUserId();
          items = mapper.findAllForStaff(
              offset, size, keyword, dateFrom, dateTo, category, division, sort, status, myUserId
          );
          total = mapper.countAllForStaff(
              keyword, dateFrom, dateTo, category, division, status, myUserId
          );
        } else {
          // 센터 범위
          items = mapper.findAllForManager(
              offset, size, keyword, dateFrom, dateTo, category, division, sort, status, me.getCenterId()
          );
          total = mapper.countAllForManager(
              keyword, dateFrom, dateTo, category, division, status, me.getCenterId()
          );
        }
      }
      case "STAFF" -> {
        // STAFF는 원래 '내 DB'만
        items = mapper.findAllForStaff(offset, size, keyword, dateFrom, dateTo, category, division, sort, status, me.getUserId());
        total = mapper.countAllForStaff(keyword, dateFrom, dateTo, category, division, status, me.getUserId());
      }
      default -> throw new IllegalStateException("Unknown role: " + me.getRole());
    }
    
    // SUPERADMIN이지만 가시권한이 N이면 전화번호 마스킹
    if ("SUPERADMIN".equals(me.getRole()) && "N".equalsIgnoreCase(me.getVisible())) {
      items.forEach(r -> r.setPhone(maskPhone(r.getPhone())));
    }
    
    int totalPages = (int) Math.ceil((double) total / size);
    return new PagedResponse<>(items, totalPages, total);
  }
  
  private String maskPhone(String phone) {
    if (phone == null) return null;
    
    // 숫자만 뽑고 재조립
    String digits = phone.replaceAll("\\D", "");
    if (digits.length() < 7) return phone; // 너무 짧으면 원본
    
    String first = digits.substring(0, 3);
    String last  = digits.substring(digits.length() - 4);
    return first + "-****-" + last;
  }
  
  // 배지/예약 수정 — 현재 담당자, 그 센터장, 본사만
  @Transactional
  public void updateField(String callerEmail, Long customerId, UpdateFieldDto dto) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null) throw new IllegalArgumentException("인증 사용자 정보를 찾을 수 없습니다.");
    
    // customers만 수정 가능(중복 차단)
    Integer exists = mapper.existsCustomerById(customerId);
    if (exists == null || exists == 0) throw new IllegalArgumentException("중복 DB는 수정할 수 없습니다.");
    
    // 권한 재검증
    switch (me.getRole()) {
      case "SUPERADMIN" -> { /* ok */ }
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
    
    String field = dto.getField() == null ? "" : dto.getField().toLowerCase();
    switch (field) {
      case "reservation" -> {
        LocalDateTime when;
        
        try {
          if (dto.getValue() == null || dto.getValue().isBlank()) {
            when = null;
          } else {
            DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            when = LocalDateTime.parse(dto.getValue(), FMT);
          }
        } catch (DateTimeParseException e) {
          throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다. 예) 2025-09-21 14:00");
        }
        mapper.updateCustomerReservation(customerId, when);
      }
      case "status" -> {
        mapper.updateCustomerStatus(customerId, dto.getValue());
      }
      default -> throw new IllegalArgumentException("지원하지 않는 필드: " + dto.getField());
    }
  }
  
  // 중복 숨김 — 본사만
  @Transactional
  public void hideDuplicates(String callerEmail, List<Long> duplicateIds) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null) throw new IllegalArgumentException("인증 사용자 정보를 찾을 수 없습니다.");
    if (!"SUPERADMIN".equals(me.getRole())) throw new IllegalArgumentException("본사만 수행할 수 있습니다.");
    if (duplicateIds == null || duplicateIds.isEmpty()) return;
    
    mapper.hideDuplicates(duplicateIds); // duplicate_display = 0
  }
}

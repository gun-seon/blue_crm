package com.blue.customer.duplicate.service;

import com.blue.customer.all.dto.AllDbRowDto;
import com.blue.customer.all.dto.PagedResponse;
import com.blue.customer.all.dto.UserContextDto;
import com.blue.customer.duplicate.mapper.CustomerDuplMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerDuplService {
  
  private final CustomerDuplMapper mapper;
  
  public PagedResponse<AllDbRowDto> getAll(
      String callerEmail, int page, int size,
      String keyword, String dateFrom, String dateTo, String category
  ) {
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null) throw new IllegalArgumentException("인증 사용자 정보를 찾을 수 없습니다.");
    
    int offset = (page - 1) * size;
    List<AllDbRowDto> items;
    int total;
    
    switch (me.getRole()) {
      case "SUPERADMIN" -> {
        items = mapper.findAllForAdmin(offset, size, keyword, dateFrom, dateTo, category, me.getVisible());
        total = mapper.countAllForAdmin(keyword, dateFrom, dateTo, category, me.getVisible());
      }
      case "MANAGER" -> {
        items = mapper.findAllForManager(offset, size, keyword, dateFrom, dateTo, category, me.getCenterId());
        total = mapper.countAllForManager(keyword, dateFrom, dateTo, category, me.getCenterId());
      }
      case "STAFF" -> {
        items = mapper.findAllForStaff(offset, size, keyword, dateFrom, dateTo, category, me.getUserId());
        total = mapper.countAllForStaff(keyword, dateFrom, dateTo, category, me.getUserId());
      }
      default -> throw new IllegalStateException("Unknown role: " + me.getRole());
    }
    
    // SUPERADMIN 가시권한 N: 전화번호 마스킹
    if ("SUPERADMIN".equals(me.getRole()) && "N".equalsIgnoreCase(me.getVisible())) {
      items.forEach(r -> r.setPhone(maskPhone(r.getPhone())));
    }
    
    int totalPages = (int) Math.ceil((double) total / size);
    return new PagedResponse<>(items, totalPages, total);
  }
  
  private String maskPhone(String phone) {
    if (phone == null) return null;
    String digits = phone.replaceAll("\\D", "");
    if (digits.length() < 7) return phone;
    String first = digits.substring(0, 3);
    String last  = digits.substring(digits.length() - 4);
    return first + "-****-" + last;
  }
}

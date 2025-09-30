package com.blue.customer.all.service;

import com.blue.customer.all.dto.*;
import com.blue.customer.all.mapper.CustomerAllMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerAllService {
  
  private final CustomerAllMapper mapper;
  
  private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final int WINDOW_PAGES = 20;
  
  /* ========= 헬퍼 ========= */
  
  private static class SortFlags {
    final boolean division;
    final boolean status;
    SortFlags(boolean d, boolean s) { this.division = d; this.status = s; }
  }
  
  private SortFlags parseSortFlags(String sort, String divisionSort, String statusSort) {
    boolean div = "on".equalsIgnoreCase(divisionSort);
    boolean st  = "on".equalsIgnoreCase(statusSort);
    if (!div || !st) {
      final String s = (sort == null ? "" : sort).toLowerCase();
      div = div || s.contains("division");
      st  = st  || s.contains("status");
    }
    return new SortFlags(div, st);
  }
  
  private LocalDateTime parseCursor(String cursorCreatedAt) {
    if (cursorCreatedAt == null || cursorCreatedAt.isBlank()) return null;
    return LocalDateTime.parse(cursorCreatedAt, TS);
  }
  
  private String extractDigits(String s) {
    if (s == null || s.isBlank()) return null;
    String d = s.replaceAll("\\D", "");
    return d.isEmpty() ? null : d;
  }
  
  /** 전화 마스킹 (SUPERADMIN & visible=N) */
  private String maskPhone(String phone) {
    if (phone == null) return null;
    String digits = phone.replaceAll("\\D", "");
    if (digits.length() < 7) return phone;
    String first = digits.substring(0, Math.min(3, digits.length()));
    String last  = digits.substring(digits.length() - 4);
    return first + "-****-" + last;
  }
  
  private int calcPrelimit(int size) {
    // size * 200, 최대 1,000
    int k = Math.max(1, size);
    long pre = (long) k * 200L;
    return (int) Math.min(pre, 1000L);
  }
  
  /* ========= 핵심: 키셋 조회 ========= */
  
  public KeysetResponse<AllDbRowDto> getAllKeyset(
      String callerEmail, int size,
      String cursorCreatedAt, Long cursorId,
      String keyword, String dateFrom, String dateTo,
      String category, String division, String status, String sort, String mine,
      String divisionSort, String statusSort
      ) {
    if (size <= 0) size = 10;
    // hasNext 판정용 한 개 더
    final int fetch = Math.min(size + 1, 100);
    
    // 로그인 사용자 컨텍스트
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null) throw new IllegalArgumentException("인증 사용자 정보를 찾을 수 없습니다.");
    
    // 정렬 플래그(문자열 → 불린)
    SortFlags flags = parseSortFlags(sort, divisionSort, statusSort);
    
    // 커서/키워드
    LocalDateTime cursorAt = parseCursor(cursorCreatedAt);
    String keywordDigits = extractDigits(keyword);
    
    // UNION prelimit 크게
    int prelimit = calcPrelimit(size);
    
    // 역할별 분기
    List<AllDbRowDto> rows;
    switch (me.getRole()) {
      case "SUPERADMIN" -> {
        rows = mapper.findAllKeysetForAdmin(
            fetch, prelimit, cursorAt, cursorId,
            keyword, keywordDigits,
            dateFrom, dateTo, category, division, status,
            /* 정렬 불린 */ flags.division, flags.status,
            /* 가시권한 */ me.getVisible()
        );
      }
      case "MANAGER" -> {
        boolean mineOnly = "Y".equalsIgnoreCase(mine);
        if (mineOnly) {
          rows = mapper.findAllKeysetForStaff(
              fetch, prelimit, cursorAt, cursorId,
              keyword, keywordDigits,
              dateFrom, dateTo, category, division, status,
              flags.division, flags.status,
              /* 본인 */ me.getUserId()
          );
        } else {
          rows = mapper.findAllKeysetForManager(
              fetch, prelimit, cursorAt, cursorId,
              keyword, keywordDigits,
              dateFrom, dateTo, category, division, status,
              flags.division, flags.status,
              /* 센터 */ me.getCenterId()
          );
        }
      }
      case "STAFF" -> {
        rows = mapper.findAllKeysetForStaff(
            fetch, prelimit, cursorAt, cursorId,
            keyword, keywordDigits,
            dateFrom, dateTo, category, division, status,
            flags.division, flags.status,
            /* 본인 */ me.getUserId()
        );
      }
      default -> throw new IllegalStateException("Unknown role: " + me.getRole());
    }
    
    // hasNext/슬라이싱
    boolean hasNext = rows.size() > size;
    if (hasNext) rows = rows.subList(0, size);
    
    // 다음 커서(정렬: createdAt DESC, id DESC 가정)
    String nextCreatedAt = null;
    Long   nextId        = null;
    if (hasNext && !rows.isEmpty()) {
      AllDbRowDto tail = rows.get(rows.size() - 1);
      nextCreatedAt = tail.getCreatedAt().format(TS);
      nextId        = tail.getId();
    }
    
    // SUPERADMIN & visible=N → 전화 마스킹
    if ("SUPERADMIN".equals(me.getRole()) && "N".equalsIgnoreCase(me.getVisible())) {
      for (AllDbRowDto r : rows) r.setPhone(maskPhone(r.getPhone()));
    }
    
    // 프론트 기대 키 이름: nextCreatedAt / nextId
    return new KeysetResponse<>(
        rows,
        hasNext,
        nextCreatedAt,
        nextId
    );
  }
  
  /** 앵커 점프: windowIndex(0부터) 기준 윈도우 시작점의 '직전' 레코드를 커서로 반환 */
  public CursorAnchor getKeysetAnchor(
      String callerEmail,
      int windowIndex,
      int size,
      String keyword, String dateFrom, String dateTo,
      String category, String division, String status, String sort, String mine,
      String divisionSort, String statusSort
  ) {
    if (windowIndex < 0) throw new IllegalArgumentException("windowIndex는 0 이상이어야 합니다.");
    if (size <= 0) size = 10;
    
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null) throw new IllegalArgumentException("인증 사용자 정보를 찾을 수 없습니다.");
    
    SortFlags flags = parseSortFlags(sort, divisionSort, statusSort);
    String keywordDigits = extractDigits(keyword);
    
    // 윈도우 시작 인덱스(0-based): windowIndex * WINDOW_PAGES * size
    long startIndex = (long) windowIndex * WINDOW_PAGES * (long) size;
    
    // 첫 윈도우면 처음부터 → 커서 없음(null 반환)
    if (startIndex == 0) {
      return new CursorAnchor(null, null, windowIndex);
    }
    
    // "직전 레코드" 인덱스
    long prevIndex = startIndex - 1;
    
    // 역할별 offset-anchor 1건 조회
    AllDbRowDto anchorRow;
    switch (me.getRole()) {
      case "SUPERADMIN" -> anchorRow = mapper.findAnchorForAdmin(
          prevIndex, // LIMIT 1 OFFSET prevIndex
          keyword, keywordDigits, dateFrom, dateTo, category, division, status,
          flags.division, flags.status, me.getVisible()
      );
      case "MANAGER" -> {
        boolean mineOnly = "Y".equalsIgnoreCase(mine);
        if (mineOnly) {
          anchorRow = mapper.findAnchorForStaff(
              prevIndex,
              keyword, keywordDigits, dateFrom, dateTo, category, division, status,
              flags.division, flags.status, me.getUserId()
          );
        } else {
          anchorRow = mapper.findAnchorForManager(
              prevIndex,
              keyword, keywordDigits, dateFrom, dateTo, category, division, status,
              flags.division, flags.status, me.getCenterId()
          );
        }
      }
      case "STAFF" -> anchorRow = mapper.findAnchorForStaff(
          prevIndex,
          keyword, keywordDigits, dateFrom, dateTo, category, division, status,
          flags.division, flags.status, me.getUserId()
      );
      default -> throw new IllegalStateException("Unknown role: " + me.getRole());
    }
    
    if (anchorRow == null) {
      // 데이터 범위를 초과한 점프일 때: 커서 없다고 알려줌(프론트에서 버튼 비활성)
      return new CursorAnchor(null, null, windowIndex);
    }
    
    return new CursorAnchor(
        anchorRow.getCreatedAt().format(TS),
        anchorRow.getId(),
        windowIndex
    );
  }
  
  public CursorAnchor getKeysetAnchorByPage(
      String callerEmail,
      int pageNo,
      int size,
      String keyword, String dateFrom, String dateTo,
      String category, String division, String status, String sort, String mine,
      String divisionSort, String statusSort
  ) {
    if (pageNo < 1) throw new IllegalArgumentException("pageNo는 1 이상이어야 합니다.");
    if (size <= 0) size = 10;
    
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null) throw new IllegalArgumentException("인증 사용자 정보를 찾을 수 없습니다.");
    
    // pageNo=1 ⇒ 커서 없이 첫 페이지
    long startIndex = (long) (pageNo - 1) * (long) size;
    if (startIndex == 0) {
      return new CursorAnchor(null, null, /*windowIndex*/ -1);
    }
    
    // “해당 페이지의 시작 레코드 직전”을 가져오기 위해 prevIndex 사용
    long prevIndex = startIndex - 1;
    
    SortFlags flags = parseSortFlags(sort, divisionSort, statusSort);
    String keywordDigits = extractDigits(keyword);
    
    AllDbRowDto anchorRow;
    switch (me.getRole()) {
      case "SUPERADMIN" -> anchorRow = mapper.findAnchorForAdmin(
          prevIndex,
          keyword, keywordDigits, dateFrom, dateTo, category, division, status,
          flags.division, flags.status, me.getVisible()
      );
      case "MANAGER" -> {
        boolean mineOnly = "Y".equalsIgnoreCase(mine);
        if (mineOnly) {
          anchorRow = mapper.findAnchorForStaff(
              prevIndex,
              keyword, keywordDigits, dateFrom, dateTo, category, division, status,
              flags.division, flags.status, me.getUserId()
          );
        } else {
          anchorRow = mapper.findAnchorForManager(
              prevIndex,
              keyword, keywordDigits, dateFrom, dateTo, category, division, status,
              flags.division, flags.status, me.getCenterId()
          );
        }
      }
      case "STAFF" -> anchorRow = mapper.findAnchorForStaff(
          prevIndex,
          keyword, keywordDigits, dateFrom, dateTo, category, division, status,
          flags.division, flags.status, me.getUserId()
      );
      default -> throw new IllegalStateException("Unknown role: " + me.getRole());
    }
    
    if (anchorRow == null) {
      // 범위를 벗어나면 커서 없음(프론트에서 무시/비활성 처리)
      return new CursorAnchor(null, null, -1);
    }
    
    return new CursorAnchor(
        anchorRow.getCreatedAt().format(TS),
        anchorRow.getId(),
        -1
    );
  }
  
  public CursorAnchorLast getKeysetAnchorLast(
      String callerEmail, int size,
      String keyword, String dateFrom, String dateTo,
      String category, String division, String status, String sort, String mine,
      String divisionSort, String statusSort
  ) {
    if (size <= 0) size = 10;
    
    UserContextDto me = mapper.findUserContextByEmail(callerEmail);
    if (me == null) throw new IllegalArgumentException("인증 사용자 정보를 찾을 수 없습니다.");
    
    SortFlags flags = parseSortFlags(sort, divisionSort, statusSort);
    String keywordDigits = extractDigits(keyword);
    
    // 1) 필터 동일 적용으로 총 개수 구함
    int total;
    switch (me.getRole()) {
      case "SUPERADMIN" -> {
        total = mapper.countAllForAdmin(
            keyword, dateFrom, dateTo, category, division, status,
            me.getVisible(), keywordDigits
        );
      }
      case "MANAGER" -> {
        boolean mineOnly = "Y".equalsIgnoreCase(mine);
        if (mineOnly) {
          total = mapper.countAllForStaff(
              keyword, dateFrom, dateTo, category, division, status,
              me.getUserId(), keywordDigits
          );
        } else {
          total = mapper.countAllForManager(
              keyword, dateFrom, dateTo, category, division, status,
              me.getCenterId(), keywordDigits
          );
        }
      }
      case "STAFF" -> {
        total = mapper.countAllForStaff(
            keyword, dateFrom, dateTo, category, division, status,
            me.getUserId(), keywordDigits
        );
      }
      default -> throw new IllegalStateException("Unknown role: " + me.getRole());
    }
    
    // 데이터 없음 → 첫 페이지로
    if (total <= 0) return new CursorAnchorLast(null, null, 1);
    
    // 2) 마지막 페이지 계산
    int lastPageNo = Math.max(1, (int)Math.ceil((double) total / size));
    long startIndex = (long)(lastPageNo - 1) * size;    // 마지막 페이지의 첫 레코드 전역 인덱스(0-based)
    
    // 3) 마지막 페이지가 첫 페이지와 같다면 앵커 불필요
    if (startIndex == 0) {
      return new CursorAnchorLast(null, null, lastPageNo);
    }
    
    // 4) ‘직전 레코드’(startIndex-1)로 앵커 레코드 획득
    long prevIndex = startIndex - 1;
    
    AllDbRowDto anchorRow;
    switch (me.getRole()) {
      case "SUPERADMIN" -> anchorRow = mapper.findAnchorForAdmin(
          prevIndex,
          keyword, keywordDigits, dateFrom, dateTo, category, division, status,
          flags.division, flags.status, me.getVisible()
      );
      case "MANAGER" -> {
        boolean mineOnly = "Y".equalsIgnoreCase(mine);
        if (mineOnly) {
          anchorRow = mapper.findAnchorForStaff(
              prevIndex,
              keyword, keywordDigits, dateFrom, dateTo, category, division, status,
              flags.division, flags.status, me.getUserId()
          );
        } else {
          anchorRow = mapper.findAnchorForManager(
              prevIndex,
              keyword, keywordDigits, dateFrom, dateTo, category, division, status,
              flags.division, flags.status, me.getCenterId()
          );
        }
      }
      case "STAFF" -> anchorRow = mapper.findAnchorForStaff(
          prevIndex,
          keyword, keywordDigits, dateFrom, dateTo, category, division, status,
          flags.division, flags.status, me.getUserId()
      );
      default -> throw new IllegalStateException("Unknown role: " + me.getRole());
    }
    
    if (anchorRow == null) {
      // 극단적 경합 등으로 못 찾았으면 안전하게 첫 페이지 취급
      return new CursorAnchorLast(null, null, lastPageNo);
    }
    
    return new CursorAnchorLast(
        anchorRow.getCreatedAt().format(TS),
        anchorRow.getId(),
        lastPageNo
    );
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

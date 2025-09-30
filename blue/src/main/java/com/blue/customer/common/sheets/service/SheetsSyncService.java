package com.blue.customer.common.sheets.service;

import com.blue.customer.common.sheets.client.GoogleSheetsClient;
import com.blue.customer.common.sheets.dto.CustomerUpsertDto;
import com.blue.customer.common.sheets.dto.DuplicateUpsertDto;
import com.blue.customer.common.sheets.dto.GsheetSourceDto;
import com.blue.customer.common.sheets.mapper.SyncMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class SheetsSyncService {
  
  private final GoogleSheetsClient sheetsClient;
  private final SyncMapper mapper;
  private final StringRedisTemplate redis;
  
  // ===== 운영 파라미터 =====
  private static final int BATCH_SIZE = 200;
  private static final String END_COL = "I";                 // A~I
  private static final int MANUAL_DEBOUNCE_SEC = 1;          // 수동 1초
  private static final int MANUAL_MAX_PER_MIN = 59;          // 수동 분당 상한
  private static final ZoneId Z_SEOUL = ZoneId.of("Asia/Seoul");
  private static final LocalTime MAINT_START = LocalTime.of(0, 0); // [00:00
  private static final LocalTime MAINT_END   = LocalTime.of(1, 0); //  ,01:00)
  
  public record SyncResult(boolean executed, int rows, String reason) {}
  
  // ====== 락 키 ======
  private static String minuteBucketKey(long sid) {
    var bucket = ZonedDateTime.now(Z_SEOUL)
        .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
    return "gsync:auto:lock:" + sid + ":" + bucket;
  }
  private static String jobLockKey(long sid) { return "gsync:job:" + sid; }
  
  // ====== 유지보수창 ======
  private boolean inMaintenanceWindow() {
    LocalTime now = LocalTime.now(Z_SEOUL);
    return !now.isBefore(MAINT_START) && now.isBefore(MAINT_END);
  }
  
  // ====== 수동 ======
  @Transactional
  public SyncResult manualRefresh(long sourceId) {
    if (inMaintenanceWindow()) return new SyncResult(false, 0, "maintenance_window");
    
    String rateKey = "gsync:rate:" + sourceId;
    Long c = redis.opsForValue().increment(rateKey);
    if (c != null && c == 1L) redis.expire(rateKey, Duration.ofSeconds(60));
    if (c != null && c > MANUAL_MAX_PER_MIN) return new SyncResult(false, 0, "rate_guard");
    
    String debounceKey = "gsync:lock:" + sourceId;
    boolean passed = Boolean.TRUE.equals(
        redis.opsForValue().setIfAbsent(debounceKey, "1", Duration.ofSeconds(MANUAL_DEBOUNCE_SEC)));
    if (!passed) return new SyncResult(false, 0, "debounced");
    
    String jobKey = jobLockKey(sourceId);
    boolean job = Boolean.TRUE.equals(
        redis.opsForValue().setIfAbsent(jobKey, "manual", Duration.ofSeconds(30)));
    if (!job) return new SyncResult(false, 0, "busy");
    
    try {
      return doIncrementalSync(sourceId, "manual");
    } finally {
      redis.delete(jobKey);
    }
  }
  
  // ====== 자동 ======
  @Transactional
  public SyncResult autoRefresh(long sourceId) {
    if (inMaintenanceWindow()) return new SyncResult(false, 0, "maintenance_window");
    
    String minuteKey = minuteBucketKey(sourceId);
    boolean firstThisMinute = Boolean.TRUE.equals(
        redis.opsForValue().setIfAbsent(minuteKey, "1", Duration.ofSeconds(120)));
    if (!firstThisMinute) return new SyncResult(false, 0, "auto_locked");
    
    String jobKey = jobLockKey(sourceId);
    boolean job = Boolean.TRUE.equals(
        redis.opsForValue().setIfAbsent(jobKey, "auto", Duration.ofSeconds(30)));
    if (!job) return new SyncResult(false, 0, "busy");
    
    try {
      return doIncrementalSync(sourceId, "auto");
    } finally {
      redis.delete(jobKey);
    }
  }
  
  // ====== 01:00 재개 ======
  @Transactional
  public SyncResult nightlyResume(long sourceId, int lookback) {
    if (inMaintenanceWindow()) return new SyncResult(false, 0, "maintenance_window");
    
    GsheetSourceDto src = mapper.findSourceById(sourceId);
    if (src == null) return new SyncResult(false, 0, "no_source");
    int cursor = Optional.ofNullable(src.getCursorRow()).orElse(1);
    
    String jobKey = jobLockKey(sourceId);
    boolean job = Boolean.TRUE.equals(
        redis.opsForValue().setIfAbsent(jobKey, "nightly", Duration.ofSeconds(300)));
    if (!job) return new SyncResult(false, 0, "busy");
    
    try {
      int total = drainFromCursor(sourceId);   // 커서가 1이든 아니든 새벽 1시에는 항상 드레인
      return new SyncResult(true, total, "nightly_drain_ok");
    } finally {
      redis.delete(jobKey);
    }
  }

// 룩백 비활성화
//  // 1) 룩백 경로만 lookback 전용 persist 호출
//  protected int recheckLookback(long sourceId, int lookback) {
//    GsheetSourceDto src = mapper.findSourceById(sourceId);
//    if (src == null) return 0;
//
//    int cursor = Optional.ofNullable(src.getCursorRow()).orElse(1);
//    int start = Math.max(2, cursor - lookback + 1);
//    if (start > cursor) return 0;
//
//    String range = src.getSheetName() + "!A" + start + ":" + END_COL + cursor;
//    List<List<Object>> raw = fetchWithRetry(src.getSpreadsheetId(), range);
//    if (raw == null || raw.isEmpty()) return 0;
//
//    List<CustomerUpsertDto> parsed = mapCompleteRows(raw);
//
//    int n = persistRows30dLookback(parsed);
//    log.info("[NightlyRecheck] sid={} start={} end={} parsed={} persisted={}",
//        sourceId, start, cursor, parsed.size(), n);
//    return n;
//  }
//
//  // 2) 룩백 전용 persist: 존재하면 건너뛴다
//  protected int persistRows30dLookback(List<CustomerUpsertDto> rows) {
//    int n = 0;
//    for (CustomerUpsertDto row : rows) {
//      LocalDateTime createdAt = Optional.ofNullable(row.getCustomerCreatedAt()).orElse(LocalDateTime.now());
//      LocalDateTime threshold = createdAt.minusDays(30);
//
//      String phoneDigits = digitsOnly(row.getCustomerPhone());
//
//      // 1) 본체 이벤트(같은 번호/시각/출처)가 이미 있으면 무조건 스킵 (동일 이벤트 재처리 방지)
//      if (mapper.existsCustomerEvent(phoneDigits, createdAt, row.getCustomerSource())) {
//        log.debug("[LookbackSkip] base event already exists phone={} at={} src={}", phoneDigits, createdAt, row.getCustomerSource());
//        continue;
//      }
//
//      // 2) 30일 이내 본체 존재 여부 확인
//      Long recentBaseId = mapper.findBaseCustomerIdWithinDays(phoneDigits, threshold, createdAt);
//
//      if (recentBaseId != null) {
//        // 2-1) 이미 동일 duplicate 이벤트가 있으면 스킵 (중복 중복 방지)
//        if (mapper.existsDuplicateEvent(recentBaseId, createdAt, row.getCustomerSource())) {
//          log.debug("[LookbackSkip] duplicate exists cid={} at={} src={}", recentBaseId, createdAt, row.getCustomerSource());
//          continue;
//        }
//        // 2-2) 새로운 duplicate 삽입
//        DuplicateUpsertDto dup = new DuplicateUpsertDto();
//        dup.setCustomerId(recentBaseId);
//        dup.setDuplicateCreatedAt(createdAt);
//        dup.setDuplicateName(row.getCustomerName());
//        dup.setDuplicateMemo(row.getCustomerMemo());
//        dup.setDuplicateContent(row.getCustomerContent());
//        dup.setDuplicateSource(row.getCustomerSource());
//        mapper.insertDuplicateMinimal(dup);
//        n++;
//      } else {
//        // 3) 본체 신규 삽입
//        boolean existed = mapper.existsAnyBaseCustomer(phoneDigits);
//        row.setCustomerDivision(existed ? "유효" : "최초");
//        row.setCustomerStatus("없음");
//        row.setCustomerCategory("주식");
//        mapper.insertCustomerMinimal(row);
//        n++;
//      }
//    }
//    return n;
//  }
  
  protected int drainFromCursor(long sourceId) {
    int total = 0, calls = 0;
    while (true) {
      SyncResult r = doIncrementalSync(sourceId, "drain");
      if (!r.executed()) break;
      total += r.rows();
      calls++;
      if (r.rows() == 0) break;
      if (calls >= 59) break;   // 사용자당 60/min 보호 (자동 1회 고려)
      try { Thread.sleep(1100); } catch (InterruptedException ignored) {}
    }
    log.info("[Drain] sid={} calls={} totalRows={}", sourceId, calls, total);
    return total;
  }
  
  // ====== 증분 동기화 ======
  private SyncResult doIncrementalSync(long sourceId, String mode) {
    GsheetSourceDto src = mapper.findSourceById(sourceId);
    if (src == null) return new SyncResult(false, 0, "no_source");
    
    int start = (src.getCursorRow() == null ? 1 : src.getCursorRow()) + 1;
    String range = src.getSheetName() + "!A" + start + ":" + END_COL + (start + BATCH_SIZE - 1);
    
    List<List<Object>> raw = fetchWithRetry(src.getSpreadsheetId(), range);
    if (raw == null || raw.isEmpty()) return new SyncResult(true, 0, "no_new");
    
    // 앞에서부터 '완전' 행만 연속 처리
    List<CustomerUpsertDto> toPersist = new ArrayList<>();
    int leadingComplete = 0;
    for (List<Object> r : raw) {
      if (!isCompleteRow(r)) break;
      toPersist.add(mapRow(r));
      leadingComplete++;
    }
    if (leadingComplete == 0) return new SyncResult(true, 0, "incomplete_head");
    
    int processed = persistRows30d(toPersist);
    
    // 커서는 '완전 연속 구간'만큼 전진
    int newCursor = start + leadingComplete - 1;
    mapper.advanceSourceCursor(sourceId, newCursor);
    log.info("[SheetsSync] {} processed={} advanced={} cursor {}->{}",
        mode, processed, leadingComplete, start - 1, newCursor);
    
    return new SyncResult(true, processed, "ok");
  }
  
  // ====== 시트 읽기 재시도 ======
  private List<List<Object>> fetchWithRetry(String spreadsheetId, String range) {
    final int MAX_TRIES = 3;
    int attempt = 0;
    while (true) {
      try {
        return sheetsClient.readRange(spreadsheetId, range);
      } catch (Exception e) {
        attempt++;
        if (attempt >= MAX_TRIES) {
          log.warn("[Sheets] readRange failed (last). range={} err={}", range, e.toString());
          return Collections.emptyList();
        }
        long backoff = (long) (Math.pow(2, attempt - 1) * 300);
        long jitter  = ThreadLocalRandom.current().nextLong(200, 600);
        try { Thread.sleep(backoff + jitter); } catch (InterruptedException ignored) {}
        log.debug("[Sheets] retry attempt={} range={}", attempt + 1, range);
      }
    }
  }
  
  // ====== 행 검증/매핑 ======
  private static final Locale KO = Locale.KOREAN;
  private static final DateTimeFormatter KOREAN_TS =
      new DateTimeFormatterBuilder()
          .parseCaseInsensitive()
          .appendPattern("yyyy. M. d ")
          .appendPattern("a ")       // 오전/오후
          .appendPattern("h:mm")
          .optionalStart().appendPattern(":ss").optionalEnd()
          .toFormatter(KO);
  
  private boolean isCompleteRow(List<Object> r) {
    // A(생성일)
    LocalDateTime at = parseCreatedAtKST(asStr(get(r, 0)));
    if (at == null) return false;
    
    // C(이름)
    String name = asStr(get(r, 2));
    if (name == null || name.isEmpty()) return false;
    
    // E(전화): 한국 포맷 하이픈 표준화 가능해야 함
    String phoneFmt = formatPhoneKR(asStr(get(r, 4)));
    if (phoneFmt == null) return false;
    
    // I(출처)
    String src = asStr(get(r, 8));
    if (src == null || src.isEmpty()) return false;
    
    return true;
  }
  
  private List<CustomerUpsertDto> mapCompleteRows(List<List<Object>> rows) {
    List<CustomerUpsertDto> out = new ArrayList<>();
    for (List<Object> r : rows) {
      if (!isCompleteRow(r)) continue;
      out.add(mapRow(r));
    }
    return out;
  }
  
  private CustomerUpsertDto mapRow(List<Object> r) {
    CustomerUpsertDto dto = new CustomerUpsertDto();
    LocalDateTime at = parseCreatedAtKST(asStr(get(r, 0)));
    if (at == null) at = LocalDateTime.now(Z_SEOUL);
    dto.setCustomerCreatedAt(at);                 // A
    dto.setCustomerName(asStr(get(r, 2)));        // C
    dto.setCustomerPhone(formatPhoneKR(asStr(get(r, 4)))); // E
    dto.setCustomerMemo(asStr(get(r, 5)));        // F
    
    String content = asStr(get(r, 6));        // G
    if (content != null && !content.isEmpty()) {
      dto.setCustomerContent("시청한 종목명 : " + content);
    } else {
      dto.setCustomerContent(null);
    }
    
    dto.setCustomerSource(asStr(get(r, 8)));      // I
    return dto;
  }
  
  private static LocalDateTime parseCreatedAtKST(String s) {
    if (s == null || s.trim().isEmpty()) return null;
    String t = s.trim();
    try { return LocalDateTime.parse(t, KOREAN_TS); } catch (Exception ignored) {}
    String[] patterns = {
        "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd", "yyyy.M.d. HH:mm:ss", "yyyy.M.d."
    };
    for (String p : patterns) {
      try { return LocalDateTime.parse(t, DateTimeFormatter.ofPattern(p, KO)); }
      catch (Exception ignored) {}
    }
    return null;
  }
  
  // 숫자만
  private static String digitsOnly(String s) {
    if (s == null) return "";
    return s.replaceAll("\\D", "");
  }
  
  /**
   * 한국 전화 포맷팅:
   * - 02: 9자리 => 02-XXX-XXXX, 10자리 => 02-XXXX-XXXX
   * - 0시작(010/011/070/050X 등): 10자리 => 3-3-4, 11자리 => 3-4-4
   * - 8자리 => 휴대전화로 간주, 010 붙여서 3-4-4
   * - 그 외는 null(불완전)
   */
  private static String formatPhoneKR(String raw) {
    if (raw == null) return null;
    String d = digitsOnly(raw);
    int len = d.length();
    if (len < 8) return null;
    
    // 서울번호
    if (d.startsWith("02")) {
      if (len == 9)  return "02-" + d.substring(2,5) + "-" + d.substring(5);
      if (len == 10) return "02-" + d.substring(2,6) + "-" + d.substring(6);
      return null;
    }
    
    // 일반 휴대폰/지역번호 (0으로 시작)
    if (d.startsWith("0")) {
      if (len == 11) return d.substring(0,3) + "-" + d.substring(3,7) + "-" + d.substring(7);
      if (len == 10) return d.substring(0,3) + "-" + d.substring(3,6) + "-" + d.substring(6);
      return null;
    }
    
    // 8자리 → 휴대전화로 간주하여 010 붙여서 처리
    if (len == 8) {
      return "010-" + d.substring(0,4) + "-" + d.substring(4);
    }
    
    return null;
  }
  
  // ====== DB 지속 ======
  protected int persistRows30d(List<CustomerUpsertDto> rows) {
    int n = 0;
    for (CustomerUpsertDto row : rows) {
      LocalDateTime createdAt = Optional.ofNullable(row.getCustomerCreatedAt()).orElse(LocalDateTime.now());
      LocalDateTime threshold = createdAt.minusDays(30);
      
      // 숫자만 키로 중복판정
      String phoneDigits = digitsOnly(row.getCustomerPhone());
      
      // 1) 완전 동일 "본체" 이벤트 스킵
      if (mapper.existsCustomerEvent(
          phoneDigits, createdAt, row.getCustomerSource(),
          row.getCustomerName(), row.getCustomerMemo(), row.getCustomerContent())) {
        log.debug("[Skip] base same-all-fields phone={} at={} src={}", phoneDigits, createdAt, row.getCustomerSource());
        continue;
      }
      
      Long recentBaseId = mapper.findBaseCustomerIdWithinDays(phoneDigits, threshold, createdAt);
      if (recentBaseId != null) {
        // 2) 같은 본체에 동일 duplicate 이벤트가 이미 있으면 스킵
        if (mapper.existsDuplicateEvent(
            recentBaseId, createdAt, row.getCustomerSource(),
            row.getCustomerName(), row.getCustomerMemo(), row.getCustomerContent())) {
          log.debug("[Skip] dup same-all-fields cid={} at={} src={}", recentBaseId, createdAt, row.getCustomerSource());
          continue;
        }
        
        DuplicateUpsertDto dup = new DuplicateUpsertDto();
        dup.setCustomerId(recentBaseId);
        dup.setDuplicateCreatedAt(createdAt);
        dup.setDuplicateName(row.getCustomerName());
        dup.setDuplicateMemo(row.getCustomerMemo());
        dup.setDuplicateContent(row.getCustomerContent());
        dup.setDuplicateSource(row.getCustomerSource());
        mapper.insertDuplicateMinimal(dup);
        n++;
      } else {
        boolean existed = mapper.existsAnyBaseCustomer(phoneDigits);
        row.setCustomerDivision(existed ? "유효" : "최초");
        row.setCustomerStatus("없음");
        row.setCustomerCategory("주식");
        mapper.insertCustomerMinimal(row); // customer_id 자동 생성됨
        
        // === phone_lookup 동시 반영 ===
        Long customerId = row.getCustomerId(); // useGeneratedKeys 로 채워진 값
        String mid   = phoneDigits.length() >= 7 ? phoneDigits.substring(3, 7) : "";
        String last4 = phoneDigits.length() >= 4 ? phoneDigits.substring(phoneDigits.length() - 4) : "";
        
        Map<String, Object> pl = new HashMap<>();
        pl.put("customerId", customerId);
        pl.put("phoneDigits", phoneDigits);
        pl.put("phoneMid", mid);
        pl.put("phoneLast4", last4);
        pl.put("createdAt", createdAt);
        
        mapper.insertPhoneLookup(pl);
        
        n++;
      }
    }
    return n;
  }
  
  // ====== helpers ======
  private static Object get(List<Object> r, int i){ return i<r.size()? r.get(i): null; }
  private static String asStr(Object o){ return o==null? null: String.valueOf(o).trim(); }
}

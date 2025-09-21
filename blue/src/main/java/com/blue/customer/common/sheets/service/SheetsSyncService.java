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
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SheetsSyncService {
  
  private final GoogleSheetsClient sheetsClient;
  private final SyncMapper mapper;
  private final StringRedisTemplate redis;
  
  private static final int BATCH_SIZE = 200;        // 한번의 외부 api 호출시 읽어들이는 행의 갯수
  private static final String END_COL = "I";        // A~I
  private static final int MANUAL_DEBOUNCE_SEC = 1; // 수동 1초
  private static final int AUTO_LOCK_SEC = 58;      // 자동 1분 락
  private static final int MANUAL_MAX_PER_MIN = 59; // 수동 분당 상한
  
  public record SyncResult(boolean executed, int rows, String reason) {}
  
  @Transactional
  public SyncResult manualRefresh(long sourceId) {
    String rateKey = "gsync:rate:" + sourceId;
    Long c = redis.opsForValue().increment(rateKey);
    if (c != null && c == 1L) redis.expire(rateKey, Duration.ofSeconds(60));
    if (c != null && c > MANUAL_MAX_PER_MIN) return new SyncResult(false, 0, "rate_guard");
    
    String lockKey = "gsync:lock:" + sourceId;
    boolean got = Boolean.TRUE.equals(
        redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(MANUAL_DEBOUNCE_SEC)));
    if (!got) return new SyncResult(false, 0, "debounced");
    
    return doIncrementalSync(sourceId, "manual");
  }
  
  @Transactional
  public SyncResult autoRefresh(long sourceId) {
    String autoKey = "gsync:auto:lock:" + sourceId;
    boolean got = Boolean.TRUE.equals(
        redis.opsForValue().setIfAbsent(autoKey, "1", Duration.ofSeconds(AUTO_LOCK_SEC)));
    if (!got) return new SyncResult(false, 0, "auto_locked");
    return doIncrementalSync(sourceId, "auto");
  }
  
  private SyncResult doIncrementalSync(long sourceId, String mode) {
    GsheetSourceDto src = mapper.findSourceById(sourceId);
    if (src == null) return new SyncResult(false, 0, "no_source");
    
    int start = (src.getCursorRow() == null ? 1 : src.getCursorRow()) + 1;
    String range = src.getSheetName() + "!A" + start + ":" + END_COL + (start + BATCH_SIZE - 1);
    
    List<List<Object>> raw = fetchWithRetry(src.getSpreadsheetId(), range);
    if (raw == null || raw.isEmpty()) return new SyncResult(true, 0, "no_new");
    
    List<CustomerUpsertDto> parsed = parseRows(raw);
    int processed = persistRows30d(parsed);
    
    // 커서는 '읽은 개수(raw.size())' 기준으로 전진해야 재조회가 안 생김
    int advanced = raw.size();
    if (advanced > 0) {
      int newCursor = start + advanced - 1;
      mapper.advanceSourceCursor(sourceId, newCursor);
      log.info("[SheetsSync] {} processed={} scanned={} cursor {}->{}",
          mode, processed, advanced, start - 1, newCursor);
    }
    return new SyncResult(true, processed, "ok");
  }
  
  private List<List<Object>> fetchWithRetry(String spreadsheetId, String range) {
    final int MAX_TRIES = 3;
    int attempt = 0;
    while (true) {
      try {
        return sheetsClient.readRange(spreadsheetId, range);
      } catch (Exception e) {
        attempt++;
        if (attempt >= MAX_TRIES) {
          log.warn("[Sheets] readRange failed (last attempt). range={} error={}", range, e.toString());
          return java.util.Collections.emptyList();
        }
        long backoff = (long) (Math.pow(2, attempt - 1) * 300); // 300ms, 600ms, ...
        long jitter  = java.util.concurrent.ThreadLocalRandom.current().nextLong(200, 600);
        try { Thread.sleep(backoff + jitter); } catch (InterruptedException ignored) {}
        log.debug("[Sheets] retrying attempt={} range={}", attempt + 1, range);
      }
    }
  }
  
  protected List<CustomerUpsertDto> parseRows(List<List<Object>> rows) {
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd['T'HH:mm[:ss]][ HH:mm[:ss]]");
    List<CustomerUpsertDto> out = new ArrayList<>();
    for (List<Object> r : rows) {
      if (r.isEmpty()) continue;
      
      CustomerUpsertDto dto = new CustomerUpsertDto();
      dto.setCustomerCreatedAt(parseDate(get(r,0), dtf, LocalDateTime.now())); // A
      dto.setCustomerName(asStr(get(r,2)));                                    // C
      dto.setCustomerPhone(normalizePhone(asStr(get(r,3))));                    // D
      dto.setCustomerMemo(asStr(get(r,5)));                                     // F
      dto.setCustomerContent(asStr(get(r,6)));                                  // G
      dto.setCustomerSource(asStr(get(r,8)));                                   // I
      
      if (blank(dto.getCustomerName()) || blank(dto.getCustomerPhone())) continue;
      out.add(dto);
    }
    
    System.out.println(out);
    return out;
  }
  
  protected int persistRows30d(List<CustomerUpsertDto> rows) {
    int n = 0;
    for (CustomerUpsertDto row : rows) {
      LocalDateTime createdAt = Optional.ofNullable(row.getCustomerCreatedAt()).orElse(LocalDateTime.now());
      LocalDateTime threshold = createdAt.minusDays(30);
      
      Long recentBaseId = mapper.findBaseCustomerIdWithinDays(row.getCustomerPhone(), threshold);
      if (recentBaseId != null) {
        // 중복 → customers_duplicate (쓰기만)
        DuplicateUpsertDto dup = new DuplicateUpsertDto();
        dup.setCustomerId(recentBaseId);
        dup.setDuplicateCreatedAt(createdAt);
        dup.setDuplicateName(row.getCustomerName());
        dup.setDuplicateMemo(row.getCustomerMemo());
        dup.setDuplicateContent(row.getCustomerContent());
        dup.setDuplicateSource(row.getCustomerSource());
        dup.setDuplicateCategory("주식");
        dup.setDuplicateDisplay(1);
        mapper.insertDuplicateMinimal(dup);
        n++;
      } else {
        // 본체 → customers
        boolean existed = mapper.existsAnyBaseCustomer(row.getCustomerPhone());
        row.setCustomerDivision(existed ? "유효" : "최초");
        row.setCustomerStatus("없음");
        row.setCustomerCategory("주식");
        mapper.insertCustomerMinimal(row);
        n++;
      }
    }
    return n;
  }
  
  // helpers
  private static Object get(List<Object> r, int i){ return i<r.size()? r.get(i): null; }
  private static String asStr(Object o){ return o==null? null: String.valueOf(o).trim(); }
  private static boolean blank(String s){ return s==null || s.isEmpty(); }
  private static LocalDateTime parseDate(Object o, DateTimeFormatter f, LocalDateTime dft){
    if (o==null) return dft;
    String s = String.valueOf(o).trim();
    if (s.isEmpty()) return dft;
    try { return LocalDateTime.parse(s, f); } catch(Exception e){ return dft; }
  }
  private static String normalizePhone(String s){
    if (s==null) return null;
    String t = s.trim();
    if (t.isEmpty()) return t;
    t = t.replaceAll("[^0-9-]", "");
    t = t.replaceAll("^-+|-+$", "").replaceAll("--+", "-");
    return t;
  }
}

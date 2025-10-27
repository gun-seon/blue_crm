package com.blue.customer.common.sheets.scheduler;

import com.blue.customer.common.sheets.mapper.SyncMapper;
import com.blue.customer.common.sheets.service.SheetsSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


// 임시
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class SheetsSyncScheduler {
  
  private final SheetsSyncService service;
  private final SyncMapper mapper;
  
//  // 임시
//  private static final AtomicBoolean BACKFILL_DONE = new AtomicBoolean(false);
//  // 유지보수 시간(00~05) 중 01:05에 '딱 한 번' 백필 실행
//  @Scheduled(cron = "0 5 1 * * *", zone = "Asia/Seoul")
//  public void backfillBOnce() {
//    if (BACKFILL_DONE.getAndSet(true)) {
//      log.info("[B-BackfillOnce] already done in this JVM. skip");
//      return;
//    }
//    for (Long sid : mapper.findAllSourceIds()) {
//      var r = service.backfillBIntoMemoOnce(sid);
//      log.info("[B-BackfillOnce] sid={} scanned={} withB={} updated={} unmatched={} skippedInvalid={}",
//          sid, r.scanned(), r.withB(), r.updated(), r.unmatched(), r.skippedInvalid());
//    }
//  }
  
  private static final int LOOKBACK = 50;
  
//  // 05:00 재개 (유지보수시간 임시 확장)
//  @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul")
  // 01:00 재개
  @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
  public void nightlyResume() {
    for (Long sid : mapper.findAllSourceIds()) {
      var r = service.nightlyResume(sid, LOOKBACK);
      log.info("[NightlyResume] sid={} rows={} reason={}", sid, r.rows(), r.reason());
    }
  }
  
//  // 05:00~23:59 매분 자동 (유지보수시간 임시 확장)
//  @Scheduled(cron = "0 * 5-23 * * *", zone = "Asia/Seoul")
  // 01:00~23:59 매분 자동
  @Scheduled(cron = "0 * 1-23 * * *", zone = "Asia/Seoul")
  public void runEveryMinute() {
    for (Long sid : mapper.findAllSourceIds()) {
      var r = service.autoRefresh(sid);
      if (r.executed()) {
        log.info("[AutoSync] sid={} rows={} reason={}", sid, r.rows(), r.reason());
      } else {
        log.debug("[AutoSync] sid={} skipped reason={}", sid, r.reason());
      }
    }
  }
}

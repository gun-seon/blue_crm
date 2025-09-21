package com.blue.customer.common.sheets.scheduler;

import com.blue.customer.common.sheets.mapper.SyncMapper;
import com.blue.customer.common.sheets.service.SheetsSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SheetsSyncScheduler {
  
  private final SheetsSyncService service;
  private final SyncMapper mapper;
  
  private static final int LOOKBACK = 50;
  
  // 01:00 재개
  @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
  public void nightlyResume() {
    for (Long sid : mapper.findAllSourceIds()) {
      var r = service.nightlyResume(sid, LOOKBACK);
      log.info("[NightlyResume] sid={} rows={} reason={}", sid, r.rows(), r.reason());
    }
  }
  
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

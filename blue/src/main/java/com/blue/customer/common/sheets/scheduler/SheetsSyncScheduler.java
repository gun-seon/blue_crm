package com.blue.customer.common.sheets.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.blue.customer.common.sheets.mapper.SyncMapper;
import com.blue.customer.common.sheets.service.SheetsSyncService;

@Slf4j
@Component
@RequiredArgsConstructor
public class SheetsSyncScheduler {
  
  private final SheetsSyncService service;
  private final SyncMapper mapper;
  
  // 매 분 0초마다 실행 (서울 시간)
  @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
  public void runEveryMinute() {
    for (Long sid : mapper.findAllSourceIds()) {
      var r = service.autoRefresh(sid); // 서비스에 Redis 락(58초) 내장되어 있음
      if (r.executed()) {
        log.info("[AutoSync] sid={} rows={} reason={}", sid, r.rows(), r.reason());
      } else {
        log.debug("[AutoSync] sid={} skipped reason={}", sid, r.reason());
      }
    }
  }
}

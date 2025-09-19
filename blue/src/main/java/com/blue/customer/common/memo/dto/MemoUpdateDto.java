package com.blue.customer.common.memo.dto;

import lombok.Data;

/**
 * 메모 모달 저장(PATCH) 요청 바디 DTO.
 *
 * <p>사용 맥락</p>
 * <ul>
 *   <li>PATCH /api/super/db/memo/{customerId}</li>
 *   <li>customers(유효/최초)만 수정 가능. 중복(DUPLICATE)은 서비스 레이어에서 즉시 차단.</li>
 *   <li>비어 있거나 null인 값은 업데이트에서 제외(Mapper set 절의 동적 SQL로 처리).</li>
 * </ul>
 *
 * <p>권한 규칙</p>
 * <ul>
 *   <li>SUPERADMIN: 모두 허용</li>
 *   <li>MANAGER: 본인 센터 소속 직원(자기 포함)이 담당 중인 고객만</li>
 *   <li>STAFF: 본인이 담당 중인 고객만</li>
 * </ul>
 */
@Data
public class MemoUpdateDto {
  /** customer_memo */
  private String memo;
  
  /** customer_status (중복은 항상 "없음" 고정이므로 적용 대상 아님) */
  private String status;
  
  /** customer_promise_time (예: "2025-09-21 14:00") */
  private String promiseTime;
  
  /** customer_tradingview_id */
  private String tradingviewId;
  
  /** customer_telegram_nickname */
  private String telegramNickname;
  
  /** customer_free_room (0/1) */
  private Boolean freeRoom;
  
  /** customer_signal_room (0/1) */
  private Boolean signalRoom;
  
  /** customer_exchange_joined (0/1) */
  private Boolean exchangeJoined;
  
  /** customer_tradingview_joined (0/1) */
  private Boolean tradingviewJoined;
  
  /** customer_indicator_flag (0/1) */
  private Boolean indicatorFlag;
}

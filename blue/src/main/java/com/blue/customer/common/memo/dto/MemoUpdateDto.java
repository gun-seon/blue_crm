// com.blue.customer.common.memo.dto.MemoUpdateDto
package com.blue.customer.common.memo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MemoUpdateDto {
  private String memo;               // null 허용
  private String status;             // "없음"/"부재1~5"/"재콜"/"가망"/"완료"/"거절"
  private String promiseTime;        // "yyyy-MM-dd HH:mm" 또는 null/빈문자열
  private String tradingviewId;      // 닉네임1
  private String telegramNickname;   // 닉네임2
  private Integer freeRoom;          // 0/1
  private Integer signalRoom;        // 0/1
  private Integer exchangeJoined;    // 0/1
  private Integer tradingviewJoined; // 0/1
  private Integer indicatorFlag;     // 0/1
}

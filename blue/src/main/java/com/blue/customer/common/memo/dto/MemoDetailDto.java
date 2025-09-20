package com.blue.customer.common.memo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
public class MemoDetailDto {
  private Long customerId;
  private LocalDateTime createdAt;
  private String name;
  private String phone;
  private String category;
  
  private String staff; // 현재 담당자명(옵션)
  private List<String> staffHistory; // 이력(옵션)
  
  private String memo;
  private String status;
  private LocalDateTime promiseTime;
  
  private String tradingviewId;
  private String telegramNickname;
  
  private Integer freeRoom;
  private Integer signalRoom;
  private Integer exchangeJoined;
  private Integer tradingviewJoined;
  private Integer indicatorFlag;
}

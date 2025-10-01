package com.blue.customer.all.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CursorMeta {
  private Integer divisionRank;
  private Integer statusRank;
  private LocalDateTime promiseAt; // 재콜 정렬용, 중복은 null
}
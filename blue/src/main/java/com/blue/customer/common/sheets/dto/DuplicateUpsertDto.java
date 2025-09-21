package com.blue.customer.common.sheets.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DuplicateUpsertDto {
  private Long duplicateId;                 // useGeneratedKeys용
  private Long customerId;                  // 본체 PK (필수)
  private LocalDateTime duplicateCreatedAt; // A열
  private String duplicateName;             // C열
  private String duplicateMemo;             // F열
  private String duplicateContent;          // G열
  private String duplicateSource;           // I열
  private String duplicateCategory = "주식"; // 기본값
  private Integer duplicateDisplay = 1;     // 기본 1
}
package com.blue.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BulkApproveResponse {
  private List<Long> approvedIds;    // 승인 성공한 사람들
  private List<Long> skippedIds;     // 제외된 사람들
}
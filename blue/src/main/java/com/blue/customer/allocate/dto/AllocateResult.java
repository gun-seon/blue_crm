package com.blue.customer.allocate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AllocateResult {
  private int successCount;
  private int skippedCount;
}

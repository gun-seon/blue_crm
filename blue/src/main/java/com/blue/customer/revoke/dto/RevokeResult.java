package com.blue.customer.revoke.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RevokeResult {
  private int successCount;
  private int skippedCount;
}

package com.blue.customer.revoke.dto;

import lombok.Data;
import java.util.List;

@Data
public class RevokeReq {
  private List<Long> customerIds;
}

package com.blue.customer.allocate.dto;

import lombok.Data;
import java.util.List;

@Data
public class AllocateMgrReq {
  private List<Long> customerIds; // 분배할 customers.customer_id 목록(현재 담당=센터장 본인)
  private Long targetUserId;      // 필수: 같은 센터의 직원
}

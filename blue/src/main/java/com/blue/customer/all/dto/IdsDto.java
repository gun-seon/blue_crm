package com.blue.customer.all.dto;

import lombok.Data;

import java.util.List;

/**
 * ID 리스트를 전달하기 위한 단순 DTO.
 *
 * <p>사용 맥락</p>
 * <ul>
 *   <li>POST /api/super/db/duplicate/hide — 선택된 중복 레코드(duplicate_id)의 표시를 일괄 숨김(duplicate_display=0)</li>
 *   <li>향후 일괄 작업(예: 배치 승인/회수 등)에도 재사용 가능</li>
 * </ul>
 */
@Data
public class IdsDto {
  /** 예: [101, 102, 103] (customers_duplicate.duplicate_id 목록) */
  private List<Long> ids;
}

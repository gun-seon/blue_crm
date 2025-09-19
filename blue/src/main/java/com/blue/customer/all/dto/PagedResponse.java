package com.blue.customer.all.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 공통 페이지네이션 응답 래퍼.
 *
 * <p>사용 맥락</p>
 * <ul>
 *   <li>GET /api/super/db (all.vue 목록 조회)에서 사용.</li>
 *   <li>items에는 화면에 표시할 DTO 리스트(여기서는 AllDbRowDto)를 담고,
 *       totalPages/totalCount는 페이지네이션 UI를 위해 프론트로 전달한다.</li>
 * </ul>
 *
 * @param <T> 리스트 항목 타입 (예: AllDbRowDto)
 */
@Data
@AllArgsConstructor
public class PagedResponse<T> {
  /** 현재 페이지의 아이템 목록 */
  private List<T> items;
  
  /** 전체 페이지 수 (totalCount / size 올림) */
  private int totalPages;
  
  /** 전체 아이템 수 */
  private int totalCount;
}

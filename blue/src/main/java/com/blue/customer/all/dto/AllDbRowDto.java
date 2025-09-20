package com.blue.customer.all.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * all.vue(전체 DB 화면) 테이블의 한 행(row)을 표현하는 DTO.
 *
 * <p>특징</p>
 * <ul>
 *   <li>SUPERADMIN의 전체 조회는 customers(최초/유효) + customers_duplicate(중복)를 UNION ALL로 합쳐 내려주므로,
 *       한 타입으로 매핑할 공통 DTO가 필요하다.</li>
 *   <li>MANAGER/STAFF는 customers(본인/센터 소속 범위)만 내려주지만, 프론트는 동일 컬럼으로 렌더링하므로 공통 DTO를 그대로 사용한다.</li>
 *   <li>origin이 "CUSTOMER"면 고객 레코드, "DUPLICATE"면 중복 레코드.</li>
 * </ul>
 *
 * <p>렌더링/비즈니스 규칙</p>
 * <ul>
 *   <li>division: "최초", "유효", "중복"</li>
 *   <li>중복(DUPLICATE) 행은 status가 항상 "없음"으로 노출되고 수정 불가(메모/예약 포함)</li>
 *   <li>중복(DUPLICATE) 행의 reservation은 항상 null</li>
 *   <li>체크박스는 중복(DUPLICATE) 행만 활성화</li>
 * </ul>
 */
@Data
public class AllDbRowDto {
  /** 고객이면 customers.customer_id, 중복이면 customers_duplicate.duplicate_id */
  private Long id;
  
  /** "CUSTOMER" | "DUPLICATE" (프론트에서 중복 행 제약/스타일링 분기에 사용) */
  private String origin;
  
  /** 행의 생성 시각: 고객은 customer_created_at, 중복은 duplicate_created_at */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
  private LocalDateTime createdAt;
  
  /** 현재 담당자명 (없을 수 있음). 중복 행은 원본 customers의 현재 담당자를 참조 */
  private String staff;
  
  /** "최초" | "유효" | "중복" */
  private String division;
  
  /** 카테고리. 고객/중복 각각의 category 컬럼 매핑 */
  private String category;
  
  /** 이름. 고객: customer_name / 중복: duplicate_name */
  private String name;
  
  /** 전화번호. 고객: customer_phone / 중복은 원본 customers의 phone을 조인해 채움 */
  private String phone;
  
  /** 유입/출처. 고객: customer_source / 중복: duplicate_source */
  private String source;
  
  /** 상담/비고 내용. 고객: customer_content / 중복: duplicate_content */
  private String content;
  
  /** 메모. 고객: customer_memo / 중복: duplicate_memo (단, 중복은 수정 불가) */
  private String memo;
  
  /** 상태. 고객은 customer_status / 중복은 항상 "없음"으로 강제 표기 */
  private String status;
  
  /** 약속시간(예약). 고객: customer_promise_time / 중복: 항상 null */
  private String reservation;
}

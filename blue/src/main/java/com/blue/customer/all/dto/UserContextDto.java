package com.blue.customer.all.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 로그인 사용자 컨텍스트 정보 DTO.
 *
 * <p>용도</p>
 * <ul>
 *   <li>서비스 레이어에서 권한 분기: "SUPERADMIN" | "MANAGER" | "STAFF"</li>
 *   <li>MANAGER: centerId 기준으로 센터 소속 전체 스코프 제한</li>
 *   <li>STAFF: userId 기준으로 본인 소유 스코프 제한</li>
 *   <li>공통: 로깅/추적용 email</li>
 * </ul>
 *
 * <p>규칙</p>
 * <ul>
 *   <li>SUPERADMIN의 centerId는 항상 <b>1</b> (본사)</li>
 *   <li>centerId가 <code>null</code>일 수 있는 경우는
 *       승인되지 않았거나 아직 센터 미배정된 계정뿐</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserContextDto {
  /** users.user_id — STAFF 소유권 검증, 감사 로그용 */
  private Long userId;
  
  /** users.user_role — "SUPERADMIN" | "MANAGER" | "STAFF" */
  private String role;
  
  /** users.center_id — MANAGER 스코프 판정에 사용 (SUPERADMIN은 1 고정) */
  private Long centerId;
  
  /** users.user_email — 호출자 식별/로깅용 */
  private String email;
  
  /** users.manager_phone_access — 전화번호 가시권한 */
  private String visible;
}

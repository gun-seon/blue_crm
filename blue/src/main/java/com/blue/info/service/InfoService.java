package com.blue.info.service;

import com.blue.customer.common.center.dto.CenterDto;
import com.blue.customer.common.center.service.CenterService;
import com.blue.info.dto.UserRow;
import com.blue.info.mapper.InfoMapper;
import com.blue.user.dto.UpdateUserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InfoService {
  
  private final InfoMapper infoMapper;
  private final CenterService centerService;
  
  // 회사명 상수
  private static final String COMPANY_NAME = "마크CRM";
  
  /**
   * 조직도 트리 + 현재 사용자
   * - centerId == null 사용자는 제외
   * - 보기 범위: STAFF=본인만, MANAGER=본인 센터 전체, HQ/SUPERADMIN=전체
   * - 정렬: 권한(관리자>센터장>담당자) → 입사일(오름차순)
   */
  public Map<String, Object> getOrgTree(Authentication auth) {
    UserRow me = (auth != null) ? infoMapper.findByEmail(auth.getName()) : null;
    if (me == null) {
      return Map.of("nodes", List.of(), "currentUser", null);
    }
    
    // 전체 사용자(전화/입사일 포함) 가져오기
    List<UserRow> users = infoMapper.findAllUsers();
    
    // centerId가 null인 사용자 완전 제외
    users = users.stream()
        .filter(u -> u.getCenterId() != null)
        .toList();
    
    // 정렬: 권한 우선(내림차순) → 입사순(오름차순)
    Comparator<UserRow> comp = Comparator
        .comparingInt((UserRow u) -> rank(u.getUserRole())).reversed()
        .thenComparing(UserRow::getUserCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
    users = users.stream().sorted(comp).toList();
    
    // 보기 범위 필터
    List<UserRow> visible = filterVisible(me, users);
    
    // 본사/센터로 그룹핑
    List<UserRow> hqUsers = visible.stream()
        .filter(u -> Objects.equals(u.getCenterId(), 1L))
        .sorted(
            Comparator
                .comparing((UserRow u) -> u.isSuper() ? 0 : 1) //  superAccount 먼저
                .thenComparingInt(u -> -rank(u.getUserRole())) // 권한: 관리자>센터장>담당자
                .thenComparing(UserRow::getUserCreatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder())) // 입사순
        )
        .toList();
    
    Map<Long, List<UserRow>> byCenter = visible.stream()
        .filter(u -> !Objects.equals(u.getCenterId(), 1L))
        .collect(Collectors.groupingBy(UserRow::getCenterId));
    
    // 루트(HQ)
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("userId", null);
    root.put("userName", COMPANY_NAME);
    root.put("userRole", "HQ");
    
    List<Map<String, Object>> children = new ArrayList<>();
    
    // 본사 직원 그룹 (HQ 열람 가능할 때만)
    if (canSeeAllCenters(me)) {
      children.add(group("본사", "GROUP", toUserNodeList(hqUsers)));
    }
    
    // 센터 노드들
    for (CenterDto c : centerService.getCenters()) {
      if (Objects.equals(c.getCenterId(), 1L)) continue;
      
      // MANAGER/STAFF는 자신의 센터만
      if (!canSeeAllCenters(me) && !Objects.equals(me.getCenterId(), c.getCenterId())) {
        continue;
      }
      
      List<UserRow> rows = byCenter.getOrDefault(c.getCenterId(), List.of());
      
      // STAFF는 본인만
      if (isStaff(me)) {
        rows = rows.stream()
            .filter(u -> Objects.equals(u.getUserId(), me.getUserId()))
            .toList();
      }
      
      // (원한다면 빈 센터도 보이게 유지 가능. 지금은 내 범위 안에서만 보임)
      children.add(group(c.getCenterName(), "CENTER", toUserNodeList(rows)));
    }
    
    root.put("children", children);
    
    Map<String, Object> currentUser = new LinkedHashMap<>();
    currentUser.put("userId", me.getUserId());
    currentUser.put("userEmail", me.getUserEmail());
    currentUser.put("userRole", me.getUserRole());
    currentUser.put("centerId", me.getCenterId());
    currentUser.put("isSuper", isSuper(me)); // 프론트 비활성화 표시용
    
    return Map.of("nodes", List.of(root), "currentUser", currentUser);
  }
  
  /** 이름/전화 수정 전용 (조직도에서) */
  @Transactional
  public void updateUserFromOrg(Long targetUserId, UpdateUserRequest req, Authentication auth) {
    UserRow viewer = infoMapper.findByEmail(auth.getName());
    UserRow target = infoMapper.findById(targetUserId);
    
    if (viewer == null || target == null) {
      throw new IllegalArgumentException("사용자 없음");
    }
    if (!canEdit(viewer, target)) {
      throw new IllegalStateException("권한이 없습니다.");
    }
    
    String field = Optional.ofNullable(req.getField()).orElse("").trim();
    String value = Optional.ofNullable(req.getValue()).orElse("").trim();
    
    switch (field) {
      case "name" -> {
        if (value.isBlank()) throw new IllegalArgumentException("이름은 비어 있을 수 없습니다.");
        infoMapper.updateUserName(targetUserId, value); // ※ XML에서 updated_at 사용하지 마세요
      }
      case "phone" -> {
        String phone = normalizePhone(value);
        infoMapper.updateUserPhone(targetUserId, phone);
      }
      default -> throw new IllegalArgumentException("지원하지 않는 필드: " + field);
    }
  }
  
  /** 프론트 옵션: 센터명 목록 반환 */
  public List<String> centerNames() {
    return centerService.getCenters().stream()
        .map(CenterDto::getCenterName)
        .toList();
  }
  
  /* ======================= 내부 유틸 ======================= */
  // 추가: 편의 메서드
  private boolean isAdmin(UserRow u)   { return u != null && "SUPERADMIN".equals(u.getUserRole()); }
  private boolean isManager(UserRow u) { return u != null && "MANAGER".equals(u.getUserRole()); }
  private boolean isStaff(UserRow u)   { return u != null && "STAFF".equals(u.getUserRole()); }
  private boolean isSuper(UserRow u)   { return u != null && u.isSuper(); }
  private int rank(String r){ return switch (r){ case "SUPERADMIN"->3; case "MANAGER"->2; case "STAFF"->1; default->0; }; }
  
  /** 전역 열람 판단: HQ(센터ID=1) 또는 super 계정만 true */
  private boolean canSeeAllCenters(UserRow u) {
    if (u == null) return false;
    if (isSuper(u)) return true; // 슈퍼어드민(특별한계정) 전권
    return Objects.equals(u.getCenterId(), 1L);  // 본사 소속만 전역 열람
  }
  
  /** 보기 범위:
   *  - super/HQ: 전체
   *  - SUPERADMIN(비HQ) & MANAGER: 본인 센터
   *  - STAFF: 본인만
   */
  private List<UserRow> filterVisible(UserRow me, List<UserRow> all) {
    if (me == null) return List.of();
    if (canSeeAllCenters(me)) return all; // super 또는 HQ
    
    if (isAdmin(me) || isManager(me)) {
      // 관리자(SUPERADMIN)인데 HQ가 아니면 ‘자기 센터만’
      return all.stream().filter(u -> Objects.equals(u.getCenterId(), me.getCenterId())).toList();
    }
    // 직원은 본인만
    return all.stream().filter(u -> Objects.equals(u.getUserId(), me.getUserId())).toList();
  }
  
  /** 수정 권한:
   *  - super: 모두 가능(본인 포함)
   *  - HQ: 전 센터 가능, 단 ‘동일 권한’은 불가
   *  - SUPERADMIN(비HQ) / MANAGER: 같은 센터 + 본인보다 낮은 권한만(STAFF)
   *  - STAFF: 불가
   */
  private boolean canEdit(UserRow viewer, UserRow target) {
    if (viewer == null || target == null) return false;
    
    if (isSuper(viewer)) return true; // super 전권
    if (Objects.equals(viewer.getUserId(), target.getUserId())) return false; // 본인 금지
    
    if (canSeeAllCenters(viewer)) {
      // HQ
      return rank(viewer.getUserRole()) > rank(target.getUserRole());
    }
    
    // SUPERADMIN(비HQ) 또는 MANAGER: 같은 센터 + 낮은 권한만
    if (isAdmin(viewer) || isManager(viewer)) {
      return Objects.equals(viewer.getCenterId(), target.getCenterId())
          && rank(viewer.getUserRole()) > rank(target.getUserRole());
    }
    
    return false; // STAFF
  }
  
  private Map<String, Object> group(String name, String role, List<Map<String, Object>> children) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("userId", null);
    m.put("userName", name);
    m.put("userRole", role);
    m.put("children", children);
    return m;
  }
  
  private List<Map<String, Object>> toUserNodeList(List<UserRow> list) {
    return list.stream().map(u -> {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("userId", u.getUserId());
      m.put("userName", u.getUserName());
      m.put("userEmail", u.getUserEmail());
      m.put("userRole", u.getUserRole());
      m.put("userPhone", u.getUserPhone()); // 전화 포함
      m.put("userCreatedAt", u.getUserCreatedAt()); // 정렬용 (프론트가 쓰면 좋고, 안 쓰면 무시)
      m.put("centerId", u.getCenterId()); // 프론트 권한판정용
      m.put("children", List.of());
      return m;
    }).toList();
  }
  
  /** 010-xxxx-xxxx 포맷 보정 (빈값 허용 X) */
  private String normalizePhone(String v) {
    String d = (v == null ? "" : v).replaceAll("\\D", "");
    if (!d.startsWith("010") || (d.length() != 10 && d.length() != 11)) {
      throw new IllegalArgumentException("전화번호 형식 오류(예: 010-1234-5678)");
    }
    return (d.length() == 10)
        ? String.format("010-%s-%s", d.substring(3, 6), d.substring(6))
        : String.format("010-%s-%s", d.substring(3, 7), d.substring(7));
  }
}

package com.blue.customer.all.mapper;

import com.blue.customer.all.dto.AllDbRowDto;
import com.blue.customer.all.dto.UserContextDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CustomerAllMapper {
  
  // 로그인 사용자 컨텍스트
  UserContextDto findUserContextByEmail(@Param("email") String email);
  
  // SUPERADMIN
  List<AllDbRowDto> findAllKeysetForAdmin(
      @Param("limit") int limit,
      @Param("prelimit") int prelimit,
      @Param("cursorAt") LocalDateTime cursorAt,
      @Param("cursorId") Long cursorId,
      @Param("keyword") String keyword,
      @Param("keywordDigits") String keywordDigits,
      @Param("dateFrom") String dateFrom,
      @Param("dateTo") String dateTo,
      @Param("category") String category,
      @Param("division") String division,
      @Param("sortDivision") boolean sortDivision,
      @Param("sortStatus") boolean sortStatus,
      @Param("visible") String visible
  );
  
  // MANAGER (센터 범위)
  List<AllDbRowDto> findAllKeysetForManager(
      @Param("limit") int limit,
      @Param("prelimit") int prelimit,
      @Param("cursorAt") LocalDateTime cursorAt,
      @Param("cursorId") Long cursorId,
      @Param("keyword") String keyword,
      @Param("keywordDigits") String keywordDigits,
      @Param("dateFrom") String dateFrom,
      @Param("dateTo") String dateTo,
      @Param("category") String category,
      @Param("division") String division,
      @Param("sortDivision") boolean sortDivision,
      @Param("sortStatus") boolean sortStatus,
      @Param("centerId") Long centerId
  );
  
  // STAFF (본인 범위)
  List<AllDbRowDto> findAllKeysetForStaff(
      @Param("limit") int limit,
      @Param("prelimit") int prelimit,
      @Param("cursorAt") LocalDateTime cursorAt,
      @Param("cursorId") Long cursorId,
      @Param("keyword") String keyword,
      @Param("keywordDigits") String keywordDigits,
      @Param("dateFrom") String dateFrom,
      @Param("dateTo") String dateTo,
      @Param("category") String category,
      @Param("division") String division,
      @Param("sortDivision") boolean sortDivision,
      @Param("sortStatus") boolean sortStatus,
      @Param("userId") Long userId
  );
  
  // SUPERADMIN: 합본(UNION)에서 ORDER 적용 후 LIMIT 1 OFFSET #{offset}
  AllDbRowDto findAnchorForAdmin(
      @Param("offset") long offset,
      @Param("keyword") String keyword,
      @Param("keywordDigits") String keywordDigits,
      @Param("dateFrom") String dateFrom,
      @Param("dateTo") String dateTo,
      @Param("category") String category,
      @Param("division") String division,
      @Param("sortDivision") boolean sortDivision,
      @Param("sortStatus") boolean sortStatus,
      @Param("visible") String visible
  );
  
  // MANAGER: 센터 범위
  AllDbRowDto findAnchorForManager(
      @Param("offset") long offset,
      @Param("keyword") String keyword,
      @Param("keywordDigits") String keywordDigits,
      @Param("dateFrom") String dateFrom,
      @Param("dateTo") String dateTo,
      @Param("category") String category,
      @Param("division") String division,
      @Param("sortDivision") boolean sortDivision,
      @Param("sortStatus") boolean sortStatus,
      @Param("centerId") Long centerId
  );
  
  // STAFF: 본인 범위
  AllDbRowDto findAnchorForStaff(
      @Param("offset") long offset,
      @Param("keyword") String keyword,
      @Param("keywordDigits") String keywordDigits,
      @Param("dateFrom") String dateFrom,
      @Param("dateTo") String dateTo,
      @Param("category") String category,
      @Param("division") String division,
      @Param("sortDivision") boolean sortDivision,
      @Param("sortStatus") boolean sortStatus,
      @Param("userId") Long userId
  );
  
  // SUPERADMIN — customers + customers_duplicate(display=1)
  int countAllForAdmin(@Param("keyword") String keyword,
                       @Param("dateFrom") String dateFrom,
                       @Param("dateTo") String dateTo,
                       @Param("category") String category,
                       @Param("division") String division,
                       @Param("visible") String visible,
                       @Param("keywordDigits") String keywordDigits);
  
  // MANAGER — 자기 센터 소속 직원 담당 고객
  int countAllForManager(@Param("keyword") String keyword,
                         @Param("dateFrom") String dateFrom,
                         @Param("dateTo") String dateTo,
                         @Param("category") String category,
                         @Param("division") String division,
                         @Param("centerId") Long centerId,
                         @Param("keywordDigits") String keywordDigits);
  
  // STAFF — 본인 담당 고객
  int countAllForStaff(@Param("keyword") String keyword,
                       @Param("dateFrom") String dateFrom,
                       @Param("dateTo") String dateTo,
                       @Param("category") String category,
                       @Param("division") String division,
                       @Param("userId") Long userId,
                       @Param("keywordDigits") String keywordDigits);
  
  // 검사/권한
  Integer existsCustomerById(@Param("customerId") Long customerId);
  Integer customerOwnedByCenter(@Param("customerId") Long customerId, @Param("centerId") Long centerId);
  Integer customerOwnedByUser(@Param("customerId") Long customerId, @Param("userId") Long userId);
  
  // 수정
  int updateCustomerReservation(@Param("customerId") Long customerId, @Param("when") LocalDateTime when);
  int updateCustomerStatus(@Param("customerId") Long customerId, @Param("status") String status);
  
  // 중복 숨김
  int hideDuplicates(@Param("ids") List<Long> duplicateIds);
}

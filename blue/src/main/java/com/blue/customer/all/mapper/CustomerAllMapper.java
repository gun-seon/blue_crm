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
  
  // SUPERADMIN — customers + customers_duplicate(display=1)
  List<AllDbRowDto> findAllForAdmin(@Param("offset") int offset,
                                    @Param("size") int size,
                                    @Param("keyword") String keyword,
                                    @Param("dateFrom") String dateFrom,
                                    @Param("dateTo") String dateTo,
                                    @Param("category") String category,
                                    @Param("division") String division,
                                    @Param("sort") String sort,
                                    @Param("status") String status,
                                    @Param("visible") String visible);
  int countAllForAdmin(@Param("keyword") String keyword,
                       @Param("dateFrom") String dateFrom,
                       @Param("dateTo") String dateTo,
                       @Param("category") String category,
                       @Param("division") String division,
                       @Param("status") String status,
                       @Param("visible") String visible);
  
  // MANAGER — 자기 센터 소속 직원 담당 고객
  List<AllDbRowDto> findAllForManager(@Param("offset") int offset,
                                      @Param("size") int size,
                                      @Param("keyword") String keyword,
                                      @Param("dateFrom") String dateFrom,
                                      @Param("dateTo") String dateTo,
                                      @Param("category") String category,
                                      @Param("division") String division,
                                      @Param("sort") String sort,
                                      @Param("pStatus") String status,
                                      @Param("centerId") Long centerId);
  int countAllForManager(@Param("keyword") String keyword,
                         @Param("dateFrom") String dateFrom,
                         @Param("dateTo") String dateTo,
                         @Param("category") String category,
                         @Param("division") String division,
                         @Param("pStatus") String status,
                         @Param("centerId") Long centerId);
  
  // STAFF — 본인 담당 고객
  List<AllDbRowDto> findAllForStaff(@Param("offset") int offset,
                                    @Param("size") int size,
                                    @Param("keyword") String keyword,
                                    @Param("dateFrom") String dateFrom,
                                    @Param("dateTo") String dateTo,
                                    @Param("category") String category,
                                    @Param("division") String division,
                                    @Param("sort") String sort,
                                    @Param("pStatus") String status,
                                    @Param("userId") Long userId);
  int countAllForStaff(@Param("keyword") String keyword,
                       @Param("dateFrom") String dateFrom,
                       @Param("dateTo") String dateTo,
                       @Param("category") String category,
                       @Param("division") String division,
                       @Param("pStatus") String status,
                       @Param("userId") Long userId);
  
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

package com.blue.customer.duplicate.mapper;

import com.blue.customer.all.dto.AllDbRowDto;
import com.blue.customer.all.dto.UserContextDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CustomerDuplMapper {
  
  // 로그인 사용자 컨텍스트
  UserContextDto findUserContextByEmail(@Param("email") String email);
  
  // SUPERADMIN
  List<AllDbRowDto> findAllForAdmin(@Param("offset") int offset,
                                    @Param("size") int size,
                                    @Param("keyword") String keyword,
                                    @Param("dateFrom") String dateFrom,
                                    @Param("dateTo") String dateTo,
                                    @Param("category") String category,
                                    @Param("visible") String visible);
  int countAllForAdmin(@Param("keyword") String keyword,
                       @Param("dateFrom") String dateFrom,
                       @Param("dateTo") String dateTo,
                       @Param("category") String category,
                       @Param("visible") String visible);
  
  // MANAGER (자기 센터)
  List<AllDbRowDto> findAllForManager(@Param("offset") int offset,
                                      @Param("size") int size,
                                      @Param("keyword") String keyword,
                                      @Param("dateFrom") String dateFrom,
                                      @Param("dateTo") String dateTo,
                                      @Param("category") String category,
                                      @Param("centerId") Long centerId);
  int countAllForManager(@Param("keyword") String keyword,
                         @Param("dateFrom") String dateFrom,
                         @Param("dateTo") String dateTo,
                         @Param("category") String category,
                         @Param("centerId") Long centerId);
  
  // STAFF (본인 담당)
  List<AllDbRowDto> findAllForStaff(@Param("offset") int offset,
                                    @Param("size") int size,
                                    @Param("keyword") String keyword,
                                    @Param("dateFrom") String dateFrom,
                                    @Param("dateTo") String dateTo,
                                    @Param("category") String category,
                                    @Param("userId") Long userId);
  int countAllForStaff(@Param("keyword") String keyword,
                       @Param("dateFrom") String dateFrom,
                       @Param("dateTo") String dateTo,
                       @Param("category") String category,
                       @Param("userId") Long userId);
}

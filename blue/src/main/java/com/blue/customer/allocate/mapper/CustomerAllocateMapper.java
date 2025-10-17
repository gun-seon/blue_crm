package com.blue.customer.allocate.mapper;

import com.blue.customer.allocate.dto.AllocateListRowDto;
import com.blue.customer.allocate.dto.CenterPickDto;
import com.blue.customer.allocate.dto.UserContextDto;
import com.blue.customer.allocate.dto.UserPickDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CustomerAllocateMapper {
  
  // 로그인 사용자 컨텍스트
  UserContextDto findUserContextByEmail(@Param("email") String email);
  
  // 목록 (HQ)
  List<AllocateListRowDto> findListForHq(@Param("offset") int offset,
                                         @Param("size") int size,
                                         @Param("keyword") String keyword,
                                         @Param("dateFrom") String dateFrom,
                                         @Param("dateTo") String dateTo,
                                         @Param("category") String category,
                                         @Param("division") String division,
                                         @Param("sort") String sort,
                                         @Param("visible") String visible);
  int countListForHq(@Param("keyword") String keyword,
                     @Param("dateFrom") String dateFrom,
                     @Param("dateTo") String dateTo,
                     @Param("category") String category,
                     @Param("division") String division,
                     @Param("visible") String visible);
  
  // 목록 (MANAGER)
  List<AllocateListRowDto> findListForManager(@Param("offset") int offset,
                                              @Param("size") int size,
                                              @Param("keyword") String keyword,
                                              @Param("dateFrom") String dateFrom,
                                              @Param("dateTo") String dateTo,
                                              @Param("category") String category,
                                              @Param("sort") String sort,
                                              @Param("managerUserId") Long managerUserId,
                                              @Param("visible") String visible);
  int countListForManager(@Param("keyword") String keyword,
                          @Param("dateFrom") String dateFrom,
                          @Param("dateTo") String dateTo,
                          @Param("category") String category,
                          @Param("managerUserId") Long managerUserId,
                          @Param("visible") String visible);
  
  // 검증/조회
  Integer userBelongsToCenter(@Param("userId") Long userId, @Param("centerId") Long centerId);
  String  findUserRole(@Param("userId") Long userId);
  
  // 유저가 센터에 소속되어있는지 검사
  Integer staffInSameCenter(@Param("managerUserId") Long managerUserId,
                            @Param("staffUserId") Long staffUserId);
  
  // 잠금(선정)
  List<Long> lockCustomersForHq(@Param("ids") List<Long> ids);
  List<Long> lockCustomersForManager(@Param("ids") List<Long> ids,
                                     @Param("managerUserId") Long managerUserId);
  
  // 이력 (phone 기반)
  int insertPastForNewOwner(@Param("ids") List<Long> ids, @Param("userId") Long userId);
  int deleteManagerFromPast(@Param("ids") List<Long> ids, @Param("managerUserId") Long managerUserId);
  
  // 소유자/상태
  int updateOwner(@Param("ids") List<Long> ids, @Param("userId") Long userId);
  void updateStatusToNew(@Param("ids") List<Long> ids);
  void updateStatusToNone(@Param("ids") List<Long> ids);
  
  // 직원 검색 (센터 제한 + 키워드)
  List<UserPickDto> searchStaffForAllocate(
      @Param("centerId") Long centerId,
      @Param("q") String q
  );
  
  // 센터 조회
  List<CenterPickDto> findCentersForAllocate();
  
}

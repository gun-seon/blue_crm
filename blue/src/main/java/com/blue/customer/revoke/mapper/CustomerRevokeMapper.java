package com.blue.customer.revoke.mapper;

import com.blue.customer.revoke.dto.RevokeListRowDto;
import com.blue.customer.revoke.dto.UserContextDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CustomerRevokeMapper {
  
  UserContextDto findUserContextByEmail(@Param("email") String email);
  
  List<RevokeListRowDto> findListForHq(@Param("offset") int offset,
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
  
  List<RevokeListRowDto> findListForManager(@Param("offset") int offset,
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
  
  List<Long> lockCustomersForRevoke(@Param("ids") List<Long> ids);
  
  int updateToRevoked(@Param("ids") List<Long> ids);
}

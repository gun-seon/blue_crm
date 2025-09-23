package com.blue.customer.center.mapper;

import com.blue.customer.all.dto.UserContextDto;
import com.blue.customer.center.dto.CenterDbRowDto;
import com.blue.customer.center.dto.CenterSimpleDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CustomerCenterMapper {
  
  UserContextDto findUserContextByEmail(@Param("email") String email);
  
  /** 본사(center_id=1) 제외 */
  List<CenterSimpleDto> findCentersExcludingHQ();
  
  /** SUPERADMIN 전용 목록/카운트 */
  List<CenterDbRowDto> findForAdmin(@Param("offset") int offset,
                                    @Param("size") int size,
                                    @Param("keyword") String keyword,
                                    @Param("dateFrom") String dateFrom,
                                    @Param("dateTo") String dateTo,
                                    @Param("category") String category,
                                    @Param("division") String division,
                                    @Param("centerId") Long centerId,
                                    @Param("visible") String visible);
  
  int countForAdmin(@Param("keyword") String keyword,
                    @Param("dateFrom") String dateFrom,
                    @Param("dateTo") String dateTo,
                    @Param("category") String category,
                    @Param("division") String division,
                    @Param("centerId") Long centerId,
                    @Param("visible") String visible);
}

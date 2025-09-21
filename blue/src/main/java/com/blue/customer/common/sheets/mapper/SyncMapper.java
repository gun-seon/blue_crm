package com.blue.customer.common.sheets.mapper;

import com.blue.customer.common.sheets.dto.CustomerUpsertDto;
import com.blue.customer.common.sheets.dto.DuplicateUpsertDto;
import com.blue.customer.common.sheets.dto.GsheetSourceDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SyncMapper {
  // gsheet_sources (커서/설정)
  GsheetSourceDto findSourceById(@Param("sourceId") Long sourceId);
  List<Long> findAllSourceIds();
  int advanceSourceCursor(@Param("sourceId") Long sourceId, @Param("newCursor") int newCursor);
  
  // customers (중복 판정용 조회)
  Long findBaseCustomerIdWithinDays(@Param("phone") String phone,
                                    @Param("threshold") LocalDateTime threshold);
  boolean existsAnyBaseCustomer(@Param("phone") String phone);
  
  // 쓰기
  int insertCustomerMinimal(CustomerUpsertDto dto);
  int insertDuplicateMinimal(DuplicateUpsertDto dto);
}
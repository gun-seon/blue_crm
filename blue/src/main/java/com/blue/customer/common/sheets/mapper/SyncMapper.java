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
  // gsheet_sources
  GsheetSourceDto findSourceById(@Param("sourceId") Long sourceId);
  List<Long> findAllSourceIds();
  int advanceSourceCursor(@Param("sourceId") Long sourceId, @Param("newCursor") int newCursor);
  
  // customers 조회(숫자만 비교)
  Long findBaseCustomerIdWithinDays(@Param("phoneDigits") String phoneDigits,
                                    @Param("threshold") java.time.LocalDateTime threshold,
                                    @Param("createdAt") java.time.LocalDateTime createdAt);
  boolean existsAnyBaseCustomer(@Param("phoneDigits") String phoneDigits);
  
  // insert
  int insertCustomerMinimal(CustomerUpsertDto dto);
  int insertDuplicateMinimal(DuplicateUpsertDto dto);
  
  // 멱등 방지
  boolean existsCustomerEvent(@Param("phoneDigits") String phoneDigits,
                              @Param("createdAt") LocalDateTime createdAt,
                              @Param("source") String source,
                              @Param("name") String name,
                              @Param("memo") String memo,
                              @Param("content") String content);
  
  boolean existsDuplicateEvent(@Param("customerId") Long customerId,
                               @Param("createdAt") LocalDateTime createdAt,
                               @Param("source") String source,
                               @Param("name") String name,
                               @Param("memo") String memo,
                               @Param("content") String content);
  
//  // B열 업데이트 (임시)
//  int appendMemoByPhoneAt(@Param("phone") String phoneFormatted,
//                          @Param("createdAt") LocalDateTime createdAt,
//                          @Param("line") String line);
//  int appendDuplicateMemoByPhoneAt(@Param("phone") String phoneFormatted,
//                                   @Param("createdAt") LocalDateTime createdAt,
//                                   @Param("line") String line);
}

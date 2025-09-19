package com.blue.customer.common.memo.mapper;

import com.blue.customer.all.dto.UserContextDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MemoMapper {
  
  UserContextDto findUserContextByEmail(@Param("email") String email);
  
  Integer existsCustomerById(@Param("customerId") Long customerId);
  Integer customerOwnedByCenter(@Param("customerId") Long customerId, @Param("centerId") Long centerId);
  Integer customerOwnedByUser(@Param("customerId") Long customerId, @Param("userId") Long userId);
  
  int updateCustomerMemoBlock(@Param("customerId") Long customerId,
                              @Param("memo") String memo,
                              @Param("status") String status,
                              @Param("promiseTime") String promiseTime,
                              @Param("tradingviewId") String tradingviewId,
                              @Param("telegramNickname") String telegramNickname,
                              @Param("freeRoom") Boolean freeRoom,
                              @Param("signalRoom") Boolean signalRoom,
                              @Param("exchangeJoined") Boolean exchangeJoined,
                              @Param("tradingviewJoined") Boolean tradingviewJoined,
                              @Param("indicatorFlag") Boolean indicatorFlag);
}

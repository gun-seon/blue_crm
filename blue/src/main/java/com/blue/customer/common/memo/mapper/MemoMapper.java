package com.blue.customer.common.memo.mapper;

import com.blue.customer.all.dto.UserContextDto;
import com.blue.customer.common.memo.dto.MemoDetailDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemoMapper {
  
  UserContextDto findUserContextByEmail(@Param("email") String email);
  
  // nested select로 사용할 쿼리: phone -> past staff 이름 리스트
  List<String> selectPastStaffByPhone(String phone);
  
  Integer existsCustomerById(@Param("customerId") Long customerId); // customers 테이블에만
  
  Integer customerOwnedByCenter(@Param("customerId") Long customerId,
                                @Param("centerId") Long centerId);
  
  Integer customerOwnedByUser(@Param("customerId") Long customerId,
                              @Param("userId") Long userId);
  
  MemoDetailDto selectCustomerMemoBlock(@Param("customerId") Long customerId);
  
  void updateCustomerMemoBlock(@Param("customerId") Long customerId,
                               @Param("memo") String memo,
                               @Param("status") String status,
                               @Param("promiseTime") String promiseTime, // "yyyy-MM-dd HH:mm" or null
                               @Param("tradingviewId") String tradingviewId,
                               @Param("telegramNickname") String telegramNickname,
                               @Param("freeRoom") Integer freeRoom,
                               @Param("signalRoom") Integer signalRoom,
                               @Param("exchangeJoined") Integer exchangeJoined,
                               @Param("tradingviewJoined") Integer tradingviewJoined,
                               @Param("indicatorFlag") Integer indicatorFlag);
}

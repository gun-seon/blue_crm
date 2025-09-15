package com.blue.customer.common.center.mapper;

import com.blue.customer.common.center.dto.CenterDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CenterMapper {
  List<CenterDto> findAll();
}

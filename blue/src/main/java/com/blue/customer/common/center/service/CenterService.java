package com.blue.customer.common.center.service;

import com.blue.customer.common.center.dto.CenterDto;
import com.blue.customer.common.center.mapper.CenterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CenterService {
  
  private final CenterMapper centerMapper;
  
  public List<CenterDto> getCenters() {
    return centerMapper.findAll();
  }
}
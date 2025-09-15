package com.blue.customer.common.center.controller;

import com.blue.customer.common.center.dto.CenterDto;
import com.blue.customer.common.center.service.CenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/common/centers")
@RequiredArgsConstructor
public class CenterController {
  
  private final CenterService centerService;
  
  @GetMapping
  public List<CenterDto> getCenters() {
    return centerService.getCenters();
  }
}

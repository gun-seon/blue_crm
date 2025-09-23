package com.blue.dashboard.dto;

import lombok.Data;

@Data
public class UserDto {
  Long id;
  String name;
  Long centerId;
  String center; // 조인된 센터명
  String role;
}
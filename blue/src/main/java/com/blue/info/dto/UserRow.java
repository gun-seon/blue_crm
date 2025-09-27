package com.blue.info.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserRow {
  private Long userId;
  private String userEmail;
  private String userRole;        // SUPERADMIN / MANAGER / STAFF
  private String userApproved;    // Y/N/X
  private String userName;
  private String userPhone;
  private String managerPhoneAccess;
  private Long centerId;          // nullable
  private String centerName;      // nullable
  private LocalDateTime userCreatedAt;
  private boolean isSuper;
}
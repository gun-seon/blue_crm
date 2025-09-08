package com.blue.auth.controller;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordHashTest {
  public static void main(String[] args) {
    PasswordEncoder encoder = new BCryptPasswordEncoder();
    
    System.out.println(encoder.encode("qkrwm207@"));
    System.out.println(encoder.encode("wm52566"));
    System.out.println(encoder.encode("test"));
  }
}
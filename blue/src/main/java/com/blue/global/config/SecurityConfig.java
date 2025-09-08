package com.blue.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
// 스프링 시큐리티 설정
public class SecurityConfig {
  @Bean
  // 시큐리티 필터
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable()) // JWT 사용할 예정: CSRF 비활성(운영도 JWT면 보통 disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            // 모든 권한
            .requestMatchers(
                "/api/ping",
                "/actuator/health",
                "/api/auth/**").permitAll()
            
            // 본사 (최고 관리자)
            .requestMatchers("/api/super/**").hasRole("SUPERADMIN")
            
            // 관리자 (센터장)
            .requestMatchers("/api/admin/**").hasRole("MANAGER")
            
            // 직원 (일반 사용자)
            .requestMatchers("/api/staff/**").hasRole("STAFF")
            .anyRequest().authenticated()
        );
    return http.build();
  }
  
  @Bean
  // 비밀번호 암호화
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}

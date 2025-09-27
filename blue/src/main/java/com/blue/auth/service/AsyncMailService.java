package com.blue.auth.service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class AsyncMailService {
  private final JavaMailSender mailSender;
  
  @Async
  public void sendHtmlWithLogo(String to, String subject, String html, String plainTextFallback) {
    try {
      MimeMessage mime = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(
          mime,
          MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
          java.nio.charset.StandardCharsets.UTF_8.name()
      );
      
      helper.setFrom(new InternetAddress("no-reply@markcrm.co.kr", "마크CRM", java.nio.charset.StandardCharsets.UTF_8.name()));
      helper.setTo(to);
      helper.setValidateAddresses(true);
      helper.setSubject(subject);
      helper.setReplyTo("support@markcrm.co.kr", "마크CRM 고객센터");
      
      String plain = (plainTextFallback == null || plainTextFallback.isBlank())
          ? "인증 메일 안내: HTML이 보이지 않으면 앱/웹에서 인증 번호를 입력해 주세요."
          : plainTextFallback;
      helper.setText(plain, html);
      
      // 인라인 로고
      org.springframework.core.io.Resource logo = new org.springframework.core.io.ClassPathResource("static/logo.png");
      if (logo.exists()) {
        helper.addInline("logo", logo, "image/png"); // jpg면 "image/jpeg"
      } else {
        log.warn("메일 로고 리소스 없음: classpath:mail/logo.png");
      }
      
      mailSender.send(mime);
      log.info("메일 발송 완료: to={}, subject={}", to, subject);
      
    } catch (Exception e) {
      log.error("HTML 메일 발송 실패: to={}, subject={}, cause={}", to, subject, e.getMessage(), e);
      throw new RuntimeException("HTML 메일 발송 실패", e);
    }
  }
}
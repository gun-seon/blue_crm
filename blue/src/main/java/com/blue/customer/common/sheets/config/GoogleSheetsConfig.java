package com.blue.customer.common.sheets.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class GoogleSheetsConfig {
  
  @Value("${google.creds.location}")
  private Resource keyFile;
  
  @Bean
  public Sheets sheets() throws Exception {
    GoogleCredentials creds = GoogleCredentials.fromStream(keyFile.getInputStream())
        .createScoped(List.of(SheetsScopes.SPREADSHEETS_READONLY)); // 읽기전용
    
    return new Sheets.Builder(
        GoogleNetHttpTransport.newTrustedTransport(),
        GsonFactory.getDefaultInstance(),
        new HttpCredentialsAdapter(creds)
    ).setApplicationName("BlueCRM").build();
  }
}
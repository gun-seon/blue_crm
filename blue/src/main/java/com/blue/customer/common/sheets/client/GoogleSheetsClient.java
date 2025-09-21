package com.blue.customer.common.sheets.client;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class GoogleSheetsClient {
  private final Sheets sheets;
  
  public GoogleSheetsClient(Sheets sheets) {
    this.sheets = sheets;
  }
  
  public List<List<Object>> readRange(String spreadsheetId, String range) throws IOException {
    ValueRange response = sheets.spreadsheets().values().get(spreadsheetId, range).execute();
    return response.getValues();
  }
}
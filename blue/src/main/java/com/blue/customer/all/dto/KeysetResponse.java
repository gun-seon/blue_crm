package com.blue.customer.all.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeysetResponse<T> {
  private List<T> items;
  private boolean hasNext;
  
  @JsonProperty("nextCreatedAt")
  private String nextCreatedAt;
  
  @JsonProperty("nextId")
  private Long nextId;
}
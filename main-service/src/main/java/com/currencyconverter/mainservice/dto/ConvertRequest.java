package com.currencyconverter.mainservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class ConvertRequest {
  @NotBlank(message = "`from` currency is required")
  private String from;

  @NotBlank(message = "`to` currency is required")
  private String to;

  @Positive(message = "`amount` must be greater than zero")
  private double amount;

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public double getAmount() {
    return amount;
  }

  public void setAmount(double amount) {
    this.amount = amount;
  }
}

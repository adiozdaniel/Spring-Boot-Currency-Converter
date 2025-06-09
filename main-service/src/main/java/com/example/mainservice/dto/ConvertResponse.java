package com.example.mainservice.dto;

public class ConvertResponse {
  private String from;
  private String to;
  private double rate;
  private double amount;
  private double converted;

  public ConvertResponse(String from, String to, double rate, double amount, double converted) {
    this.from = from;
    this.to = to;
    this.rate = rate;
    this.amount = amount;
    this.converted = converted;
  }

  // Getters only
  public String getFrom() {
    return from;
  }

  public String getTo() {
    return to;
  }

  public double getRate() {
    return rate;
  }

  public double getAmount() {
    return amount;
  }

  public double getConverted() {
    return converted;
  }
}

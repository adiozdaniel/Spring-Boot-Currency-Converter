package com.example.mainservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Table("conversion_schema.conversions")
public class Conversion {

  @Id
  @Column("id")
  private UUID id;

  @Column("from_currency")
  private String fromCurrency;

  @Column("to_currency")
  private String toCurrency;

  @Column("amount")
  private BigDecimal amount;

  @Column("rate")
  private BigDecimal rate;

  @Column("converted_amount")
  private BigDecimal convertedAmount;

  @Column("created_at")
  private LocalDateTime timestamp;

  public Conversion() {
  }

  public Conversion(String fromCurrency, String toCurrency, BigDecimal amount, BigDecimal rate,
      BigDecimal convertedAmount, LocalDateTime timestamp) {
    this.fromCurrency = fromCurrency;
    this.toCurrency = toCurrency;
    this.amount = amount;
    this.rate = rate;
    this.convertedAmount = convertedAmount;
    this.timestamp = timestamp;
  }

  // Getters and setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getFromCurrency() {
    return fromCurrency;
  }

  public void setFromCurrency(String fromCurrency) {
    this.fromCurrency = fromCurrency;
  }

  public String getToCurrency() {
    return toCurrency;
  }

  public void setToCurrency(String toCurrency) {
    this.toCurrency = toCurrency;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public BigDecimal getRate() {
    return rate;
  }

  public void setRate(BigDecimal rate) {
    this.rate = rate;
  }

  public BigDecimal getConvertedAmount() {
    return convertedAmount;
  }

  public void setConvertedAmount(BigDecimal convertedAmount) {
    this.convertedAmount = convertedAmount;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }
}

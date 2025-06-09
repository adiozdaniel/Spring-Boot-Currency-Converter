package com.example.mainservice.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversions")
public class Conversion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "from_currency", nullable = false)
  private String fromCurrency;

  @Column(name = "to_currency", nullable = false)
  private String toCurrency;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(nullable = false)
  private BigDecimal rate;

  @Column(name = "converted_amount", nullable = false)
  private BigDecimal convertedAmount;

  @Column(name = "created_at", nullable = false)
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
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
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

package com.deutschebank.model;

import com.google.gson.annotations.SerializedName;
import java.time.LocalDateTime;

public class Settlement {
    @SerializedName("settlementId")
    private String settlementId;

    @SerializedName("accountId")
    private String accountId;

    @SerializedName("security")
    private String security;

    @SerializedName("quantity")
    private Double quantity;

    @SerializedName("amount")
    private Double amount;

    @SerializedName("currency")
    private String currency;

    @SerializedName("settlementDate")
    private LocalDateTime settlementDate;

    @SerializedName("status")
    private String status; // NEW, MATCHED, RECONCILED, FAILED

    // Constructors
    public Settlement() {}

    public Settlement(String settlementId, String accountId, String security,
                     Double quantity, Double amount, String currency) {
        this.settlementId = settlementId;
        this.accountId = accountId;
        this.security = security;
        this.quantity = quantity;
        this.amount = amount;
        this.currency = currency;
        this.settlementDate = LocalDateTime.now();
        this.status = "NEW";
    }

    // Getters & Setters
    public String getSettlementId() {
        return settlementId;
    }

    public void setSettlementId(String settlementId) {
        this.settlementId = settlementId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getSecurity() {
        return security;
    }

    public void setSecurity(String security) {
        this.security = security;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDateTime settlementDate) {
        this.settlementDate = settlementDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Settlement{" +
                "settlementId='" + settlementId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", security='" + security + '\'' +
                ", quantity=" + quantity +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}

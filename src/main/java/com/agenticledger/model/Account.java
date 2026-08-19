package com.agenticledger.model;

import com.google.gson.annotations.SerializedName;
import java.time.LocalDateTime;

public class Account {
    @SerializedName("accountId")
    private String accountId;

    @SerializedName("accountName")
    private String accountName;

    @SerializedName("balance")
    private Double balance;

    @SerializedName("currency")
    private String currency;

    @SerializedName("status")
    private String status; // ACTIVE, SUSPENDED, CLOSED

    @SerializedName("lastUpdated")
    private LocalDateTime lastUpdated;

    // Constructors
    public Account() {}

    public Account(String accountId, String accountName, Double balance, String currency) {
        this.accountId = accountId;
        this.accountName = accountName;
        this.balance = balance;
        this.currency = currency;
        this.status = "ACTIVE";
        this.lastUpdated = LocalDateTime.now();
    }

    // Getters & Setters
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountId='" + accountId + '\'' +
                ", accountName='" + accountName + '\'' +
                ", balance=" + balance +
                ", currency='" + currency + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}

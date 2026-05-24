package com.example.quanlychitieu.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "recurring_transactions")
public class RecurringTransaction {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String userId;
    private double amount;
    private int categoryId;
    private int accountId;
    private String type; // INCOME or EXPENSE
    private String note;
    private String frequency; // DAILY, WEEKLY, MONTHLY
    private String startDate;
    private String nextExecutionDate;
    private boolean isActive;
    private long lastUpdated;

    public RecurringTransaction() {
        this.isActive = true;
        this.lastUpdated = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getNextExecutionDate() { return nextExecutionDate; }
    public void setNextExecutionDate(String nextExecutionDate) { this.nextExecutionDate = nextExecutionDate; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
}

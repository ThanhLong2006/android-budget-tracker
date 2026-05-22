package com.example.quanlychitieu.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "budgets")
public class Budget {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String syncId; // Firestore ID
    private String userId; // Firebase User ID
    private int categoryId;
    private double amountLimit;
    private String period;
    private String month;
    private long lastUpdated;

    public Budget() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getSyncId() { return syncId; }
    public void setSyncId(String syncId) { this.syncId = syncId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public double getAmountLimit() { return amountLimit; }
    public void setAmountLimit(double amountLimit) { this.amountLimit = amountLimit; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
}

package com.example.quanlychitieu.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transaction")
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String syncId; // Firestore ID
    private String userId; // Firebase User ID
    private double amount;
    private String date; // YYYY-MM-DD
    private String type; // INCOME / EXPENSE
    private int categoryID;
    private int accountId;
    private String note;
    private String imageUrl; // URL ảnh hóa đơn trên Firebase Storage
    private long lastUpdated;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getSyncId() { return syncId; }
    public void setSyncId(String syncId) { this.syncId = syncId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getCategoryID() { return categoryID; }
    public void setCategoryID(int categoryID) { this.categoryID = categoryID; }
    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
}

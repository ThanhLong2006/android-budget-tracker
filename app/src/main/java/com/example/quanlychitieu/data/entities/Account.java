package com.example.quanlychitieu.data.entities;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "accounts")
public class Account {
    
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String syncId;
    private String userId;
    private String name;
    private double balance;
    private String icon;
    private long lastUpdated;

    public Account() {}

    @Ignore
    public Account(String name, double balance, String icon) {
        this.name = name;
        this.balance = balance;
        this.icon = icon;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getSyncId() { return syncId; }
    public void setSyncId(String syncId) { this.syncId = syncId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
}

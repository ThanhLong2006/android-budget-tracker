package com.example.quanlychitieu.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.quanlychitieu.data.entities.RecurringTransaction;

import java.util.List;

@Dao
public interface RecurringTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(RecurringTransaction recurringTransaction);

    @Update
    void update(RecurringTransaction recurringTransaction);

    @Delete
    void delete(RecurringTransaction recurringTransaction);

    @Query("SELECT * FROM recurring_transactions WHERE userId = :userId ORDER BY lastUpdated DESC")
    LiveData<List<RecurringTransaction>> getAllRecurringTransactions(String userId);

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND nextExecutionDate <= :currentDate")
    List<RecurringTransaction> getDueRecurringTransactions(String currentDate);
}

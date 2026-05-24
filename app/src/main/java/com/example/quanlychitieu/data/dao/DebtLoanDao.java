package com.example.quanlychitieu.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.quanlychitieu.data.entities.DebtLoan;

import java.util.List;

@Dao
public interface DebtLoanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DebtLoan debtLoan);

    @Update
    void update(DebtLoan debtLoan);

    @Delete
    void delete(DebtLoan debtLoan);

    @Query("SELECT * FROM debt_loans WHERE userId = :userId ORDER BY lastUpdated DESC")
    LiveData<List<DebtLoan>> getAllDebtLoans(String userId);

    @Query("SELECT * FROM debt_loans WHERE userId = :userId AND type = :type ORDER BY lastUpdated DESC")
    LiveData<List<DebtLoan>> getDebtLoansByType(String userId, String type);

    @Query("SELECT SUM(amount) FROM debt_loans WHERE userId = :userId AND type = 'LOAN' AND isPaid = 0")
    LiveData<Double> getTotalLoanAmount(String userId);

    @Query("SELECT SUM(amount) FROM debt_loans WHERE userId = :userId AND type = 'DEBT' AND isPaid = 0")
    LiveData<Double> getTotalDebtAmount(String userId);
}

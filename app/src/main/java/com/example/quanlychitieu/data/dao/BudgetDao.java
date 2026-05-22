package com.example.quanlychitieu.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RewriteQueriesToDropUnusedColumns;
import androidx.room.Update;

import com.example.quanlychitieu.data.entities.Budget;

import java.util.List;

@Dao
public interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Budget budget);

    @Update
    void update(Budget budget);

    @Delete
    void delete(Budget budget);

    @Query("SELECT * FROM budgets WHERE userId = :userId")
    LiveData<List<Budget>> getAllBudgets(String userId);

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT b.*, c.name as categoryName, c.icon as categoryIcon, c.color as categoryColor, " +
           "(SELECT SUM(amount) FROM `transaction` t WHERE t.userId = b.userId AND t.categoryID = b.categoryId AND strftime('%m-%Y', t.date) = b.month) as currentSpent " +
           "FROM budgets b JOIN categories c ON b.categoryId = c.id " +
           "WHERE b.userId = :userId AND b.month = :month")
    LiveData<List<BudgetWithCategory>> getBudgetsByMonthWithCategory(String userId, String month);

    @Query("SELECT * FROM budgets WHERE userId = :userId AND categoryId = :categoryId AND month = :month LIMIT 1")
    Budget getBudgetSync(String userId, int categoryId, String month);

    @Query("SELECT * FROM budgets WHERE userId = :userId AND month = :month")
    LiveData<List<Budget>> getBudgetsByMonth(String userId, String month);

    @Query("SELECT * FROM budgets WHERE userId = :userId AND categoryId = :categoryId AND month = :month LIMIT 1")
    LiveData<Budget> getBudgetByCategoryAndMonth(String userId, int categoryId, String month);

    @Query("SELECT * FROM budgets WHERE userId = :userId")
    List<Budget> getAllBudgetsSync(String userId);

    @Query("SELECT * FROM budgets WHERE syncId = :syncId LIMIT 1")
    Budget getBudgetBySyncId(String syncId);

    public static class BudgetWithCategory {
        public int id;
        public String userId;
        public int categoryId;
        public double amountLimit;
        public String month;
        public String categoryName;
        public String categoryIcon;
        public String categoryColor;
        public double currentSpent;
    }
}

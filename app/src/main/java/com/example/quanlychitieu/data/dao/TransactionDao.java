package com.example.quanlychitieu.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RewriteQueriesToDropUnusedColumns;
import androidx.room.Update;

import com.example.quanlychitieu.data.entities.Transaction;

import java.util.List;

@Dao
public interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Transaction transaction);

    @Update
    void update(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    @Query("DELETE FROM `transaction` WHERE userId = :userId")
    void deleteAllTransactions(String userId);

    @Query("SELECT * FROM `transaction` WHERE id = :id")
    LiveData<Transaction> getTransactionById(int id);

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT t.*, c.name as categoryName, c.icon as categoryIcon, c.color as categoryColor " +
           "FROM `transaction` t LEFT JOIN categories c ON t.categoryID = c.id " +
           "WHERE t.userId = :userId ORDER BY t.date DESC")
    LiveData<List<TransactionWithCategory>> getAllTransactionsWithCategory(String userId);

    @Query("SELECT SUM(CASE WHEN type = 'INCOME' THEN amount ELSE -amount END) FROM `transaction` WHERE userId = :userId")
    LiveData<Double> getTotalBalance(String userId);

    @Query("SELECT SUM(CASE WHEN type = 'INCOME' THEN amount ELSE -amount END) FROM `transaction` WHERE userId = :userId")
    Double getTotalBalanceSync(String userId);

    @Query("SELECT SUM(amount) FROM `transaction` WHERE userId = :userId AND type = :type AND strftime('%Y-%m', date) = strftime('%Y-%m', 'now')")
    LiveData<Double> getMonthlyTotalByType(String userId, String type);

    @Query("SELECT SUM(amount) FROM `transaction` WHERE userId = :userId AND type = :type AND strftime('%Y-%m', date) = :month")
    LiveData<Double> getSpecificMonthlyTotal(String userId, String type, String month);

    @Query("SELECT SUM(amount) FROM `transaction` WHERE userId = :userId AND categoryID = :categoryId AND strftime('%m-%Y', date) = :month AND type = 'EXPENSE'")
    LiveData<Double> getCategoryTotalByMonth(String userId, int categoryId, String month);

    @Query("SELECT SUM(amount) FROM `transaction` WHERE userId = :userId AND categoryID = :categoryId AND strftime('%m-%Y', date) = :month AND id != :excludeId AND type = 'EXPENSE'")
    Double getCategoryTotalByMonthExcludeSync(String userId, int categoryId, String month, int excludeId);

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT t.*, c.name as categoryName, c.icon as categoryIcon, c.color as categoryColor " +
           "FROM `transaction` t LEFT JOIN categories c ON t.categoryID = c.id " +
           "WHERE t.userId = :userId ORDER BY t.date DESC LIMIT 5")
    LiveData<List<TransactionWithCategory>> getRecentTransactionsWithCategory(String userId);

    @Query("SELECT c.name as categoryName, SUM(t.amount) as totalAmount, c.color as categoryColor " +
           "FROM `transaction` t JOIN categories c ON t.categoryID = c.id " +
           "WHERE t.userId = :userId AND t.type = :type AND strftime('%Y-%m', t.date) = strftime('%Y-%m', 'now') " +
           "GROUP BY c.name ORDER BY totalAmount DESC")
    LiveData<List<CategoryStats>> getCategoryStats(String userId, String type);

    @Query("SELECT date as label, SUM(amount) as value FROM `transaction` " +
           "WHERE userId = :userId AND type = :type " +
           "GROUP BY date ORDER BY date ASC LIMIT 7")
    LiveData<List<ChartData>> getDailyStats(String userId, String type);

    @Query("SELECT date as label, " +
           "SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END) as incomeValue, " +
           "SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) as expenseValue " +
           "FROM `transaction` WHERE userId = :userId " +
           "GROUP BY date ORDER BY date DESC LIMIT 7")
    LiveData<List<TrendData>> getTrendStats(String userId);

    @Query("SELECT * FROM `transaction` WHERE userId = :userId")
    List<Transaction> getAllTransactionsSync(String userId);

    @Query("SELECT * FROM `transaction` WHERE syncId = :syncId LIMIT 1")
    Transaction getTransactionBySyncId(String syncId);

    public static class TransactionWithCategory {
        public int id;
        public double amount;
        public String date;
        public String type;
        public String note;
        public String imageUrl;
        public String categoryName;
        public String categoryIcon;
        public String categoryColor;
    }

    public static class CategoryStats {
        public String categoryName;
        public double totalAmount;
        public String categoryColor;
    }

    public static class ChartData {
        public String label;
        public double value;
    }

    public static class TrendData {
        public String label;
        public double incomeValue;
        public double expenseValue;
    }
}

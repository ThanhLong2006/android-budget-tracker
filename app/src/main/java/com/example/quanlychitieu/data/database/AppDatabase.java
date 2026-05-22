package com.example.quanlychitieu.data.database;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.quanlychitieu.data.dao.AccountDao;
import com.example.quanlychitieu.data.dao.BudgetDao;
import com.example.quanlychitieu.data.dao.CategoryDao;
import com.example.quanlychitieu.data.dao.TransactionDao;
import com.example.quanlychitieu.data.entities.Account;
import com.example.quanlychitieu.data.entities.Budget;
import com.example.quanlychitieu.data.entities.Category;
import com.example.quanlychitieu.data.entities.Transaction;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Transaction.class, Category.class, Account.class, Budget.class},
        version = 8, 
        exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract TransactionDao transactionDao();
    public abstract CategoryDao categoryDao();
    public abstract AccountDao accountDao();
    public abstract BudgetDao budgetDao();
    
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    private static volatile AppDatabase INSTANCE;
    public static AppDatabase getDatabase(final Context context)
    {
        if(INSTANCE == null)
        {
            synchronized (AppDatabase.class)
            {
                if(INSTANCE == null)
                {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class,"quanlychitieu_db")
                            .fallbackToDestructiveMigration()
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            databaseWriteExecutor.execute(() -> {
                try {
                    CategoryDao categoryDao = INSTANCE.categoryDao();
                    seedSystemCategory(categoryDao, "Ăn uống", "EXPENSE", "baseline_coffee_24", "#FF9800");
                    seedSystemCategory(categoryDao, "Di chuyển", "EXPENSE", "outline_directions_car_24", "#2196F3");
                    seedSystemCategory(categoryDao, "Hóa đơn", "EXPENSE", "outline_account_balance_24", "#9C27B0");
                    seedSystemCategory(categoryDao, "Mua sắm", "EXPENSE", "outline_add_shopping_cart_24", "#E91E63");
                    seedSystemCategory(categoryDao, "Giải trí", "EXPENSE", "outline_bolt_24", "#FFEB3B");
                    seedSystemCategory(categoryDao, "Sức khỏe", "EXPENSE", "outline_article_person_24", "#4CAF50");
                    seedSystemCategory(categoryDao, "Giáo dục", "EXPENSE", "outline_article_person_24", "#3F51B5");

                    seedSystemCategory(categoryDao, "Lương", "INCOME", "outline_account_balance_wallet_24", "#4CAF50");
                    seedSystemCategory(categoryDao, "Kinh doanh", "INCOME", "outline_add_chart_24", "#009688");
                    seedSystemCategory(categoryDao, "Thưởng", "INCOME", "outline_bolt_24", "#FFC107");
                    seedSystemCategory(categoryDao, "Khác", "INCOME", "outline_more_horiz_24", "#9E9E9E");

                    AccountDao accountDao = INSTANCE.accountDao();
                    if (accountDao.getAllAccountsSync().isEmpty()) {
                        accountDao.insert(new Account("Tiền mặt", 0.0, "cash"));
                        accountDao.insert(new Account("Thẻ ngân hàng", 0.0, "card"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        private void seedSystemCategory(CategoryDao dao, String name, String type, String icon, String color) {
            List<Category> existing = dao.getAllCategoriesSync();
            Category found = null;
            for (Category c : existing) {
                if (c.getUserId() == null && name.equals(c.getName())) {
                    found = c;
                    break;
                }
            }
            if (found == null) {
                Category newCat = new Category();
                newCat.setName(name);
                newCat.setType(type);
                newCat.setIcon(icon);
                newCat.setColor(color);
                newCat.setUserId(null);
                dao.insert(newCat);
            } else if (found.getIcon() == null || found.getColor() == null) {
                found.setIcon(icon);
                found.setColor(color);
                dao.update(found);
            }
        }
    };
}

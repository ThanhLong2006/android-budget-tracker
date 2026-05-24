package com.example.quanlychitieu.worker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.quanlychitieu.data.database.AppDatabase;
import com.example.quanlychitieu.data.entities.RecurringTransaction;
import com.example.quanlychitieu.data.entities.Transaction;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecurringWorker extends Worker {

    public RecurringWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // 1. Lấy danh sách giao dịch định kỳ đến hạn
        List<RecurringTransaction> dueItems = db.recurringTransactionDao().getDueRecurringTransactions(today);

        for (RecurringTransaction rt : dueItems) {
            // 2. Tạo một giao dịch thực tế từ mẫu định kỳ
            Transaction t = new Transaction();
            t.setUserId(rt.getUserId());
            t.setAmount(rt.getAmount());
            t.setCategoryID(rt.getCategoryId());
            t.setAccountId(rt.getAccountId());
            t.setType(rt.getType());
            t.setNote("[Định kỳ] " + rt.getNote());
            t.setDate(today);
            
            db.transactionDao().insert(t);

            // 3. Tính toán ngày thực hiện tiếp theo
            rt.setNextExecutionDate(calculateNextDate(rt.getNextExecutionDate(), rt.getFrequency()));
            rt.setLastUpdated(System.currentTimeMillis());
            db.recurringTransactionDao().update(rt);
        }

        return Result.success();
    }

    private String calculateNextDate(String currentStr, String frequency) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(currentStr));

            switch (frequency) {
                case "DAILY": cal.add(Calendar.DAY_OF_YEAR, 1); break;
                case "WEEKLY": cal.add(Calendar.WEEK_OF_YEAR, 1); break;
                case "MONTHLY": cal.add(Calendar.MONTH, 1); break;
            }
            return sdf.format(cal.getTime());
        } catch (Exception e) {
            return currentStr;
        }
    }
}

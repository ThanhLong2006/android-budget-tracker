package com.example.quanlychitieu;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.quanlychitieu.data.database.AppDatabase;
import com.google.firebase.auth.FirebaseAuth;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * BalanceWidget: Cung cấp thông tin số dư nhanh ngoài màn hình chính và phím tắt thêm giao dịch.
 */
public class BalanceWidget extends AppWidgetProvider {

    public static final String ACTION_UPDATE_WIDGET = "com.example.quanlychitieu.UPDATE_WIDGET";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    /**
     * updateWidget: Phương thức tĩnh giúp các phần khác của app yêu cầu Widget làm mới dữ liệu.
     */
    public static void updateWidget(Context context) {
        Intent intent = new Intent(context, BalanceWidget.class);
        intent.setAction(ACTION_UPDATE_WIDGET);
        context.sendBroadcast(intent);
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.balance_widget);
        
        AppDatabase.databaseWriteExecutor.execute(() -> {
            String userId = FirebaseAuth.getInstance().getUid();
            double balance = 0;
            
            if (userId != null) {
                Double b = AppDatabase.getDatabase(context).transactionDao().getTotalBalanceSync(userId);
                if (b != null) balance = b;
            }

            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            String balanceStr = formatter.format(balance);
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

            // Cập nhật text số dư và thời gian
            views.setTextViewText(R.id.tv_widget_balance, balanceStr);
            views.setTextViewText(R.id.tv_widget_update_time, "Cập nhật: " + time);

            // Intent mở màn hình chính khi nhấn vào số dư
            Intent intentMain = new Intent(context, MainActivity.class);
            PendingIntent piMain = PendingIntent.getActivity(context, 0, intentMain, PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.tv_widget_balance, piMain);

            // Intent mở thẳng màn hình Thêm giao dịch khi nhấn nút [+]
            Intent intentAdd = new Intent(context, MainActivity.class);
            intentAdd.putExtra("OPEN_ADD_TRANSACTION", true);
            intentAdd.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent piAdd = PendingIntent.getActivity(context, appWidgetId, intentAdd, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.btn_widget_add, piAdd);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        });
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_UPDATE_WIDGET.equals(intent.getAction()) ||
            AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, BalanceWidget.class));
            for (int id : ids) {
                updateAppWidget(context, appWidgetManager, id);
            }
        }
    }
}

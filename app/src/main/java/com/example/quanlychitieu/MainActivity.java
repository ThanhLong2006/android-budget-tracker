package com.example.quanlychitieu;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.quanlychitieu.worker.RecurringWorker;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("wealthflow_prefs", Context.MODE_PRIVATE);
        String lang = prefs.getString("language", "Tiếng Việt");
        String langCode = lang.equals("English") ? "en" : "vi";
        
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        Context context = newBase.createConfigurationContext(config);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyDarkMode();
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Lập lịch chạy Giao dịch định kỳ (Chạy 1 lần mỗi ngày)
        scheduleRecurringTasks();

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
            FloatingActionButton fab = findViewById(R.id.fab_utility);
            
            NavigationUI.setupWithNavController(bottomNav, navController);

            fab.setOnClickListener(v -> {
                navController.navigate(R.id.addTransactionFragment);
            });

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int id = destination.getId();
                if (id == R.id.nav_start || id == R.id.loginFragment || id == R.id.registerFragment) {
                    bottomNav.setVisibility(View.GONE);
                    fab.setVisibility(View.GONE);
                } else if (id == R.id.nav_budget || id == R.id.nav_debt_loan || id == R.id.nav_recurring) {
                    bottomNav.setVisibility(View.VISIBLE);
                    fab.setVisibility(View.GONE);
                } else {
                    bottomNav.setVisibility(View.VISIBLE);
                    fab.setVisibility(View.VISIBLE);
                }
            });
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
    }

    private void scheduleRecurringTasks() {
        PeriodicWorkRequest recurringWork =
                new PeriodicWorkRequest.Builder(RecurringWorker.class, 1, TimeUnit.DAYS)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "RecurringTransactionWork",
                ExistingPeriodicWorkPolicy.KEEP,
                recurringWork
        );
    }

    private void applyDarkMode() {
        SharedPreferences sharedPreferences = getSharedPreferences("wealthflow_prefs", Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}

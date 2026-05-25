package com.example.quanlychitieu;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
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
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private boolean isAuthenticated = false;

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

        // Kiểm tra bảo mật vân tay nếu được bật
        checkBiometricSecurity();

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

            if (getIntent().getBooleanExtra("OPEN_ADD_TRANSACTION", false)) {
                navController.navigate(R.id.addTransactionFragment);
            }

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

    private void checkBiometricSecurity() {
        SharedPreferences prefs = getSharedPreferences("wealthflow_prefs", Context.MODE_PRIVATE);
        boolean isBiometricEnabled = prefs.getBoolean("biometric_enabled", false);

        if (isBiometricEnabled && !isAuthenticated) {
            // Ẩn nội dung app cho đến khi xác thực xong
            findViewById(R.id.main).setVisibility(View.INVISIBLE);
            showBiometricPrompt();
        }
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(MainActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                isAuthenticated = true;
                findViewById(R.id.main).setVisibility(View.VISIBLE);
                Toast.makeText(getApplicationContext(), "Xác thực thành công", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(), "Xác thực lỗi: " + errString, Toast.LENGTH_SHORT).show();
                finish(); // Đóng app nếu xác thực lỗi/hủy
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Xác thực thất bại", Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Khóa ứng dụng")
                .setSubtitle("Xác thực để tiếp tục sử dụng WealthFlow")
                .setNegativeButtonText("Thoát")
                .build();

        biometricPrompt.authenticate(promptInfo);
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

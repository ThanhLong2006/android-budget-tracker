package com.example.quanlychitieu;

// Import các thư viện cần thiết cho Activity, giao diện và điều hướng
import android.content.Context; // Cung cấp thông tin về môi trường ứng dụng
import android.content.SharedPreferences; // Để truy cập dữ liệu lưu trữ nhỏ (như cài đặt ngôn ngữ, chế độ tối)
import android.content.res.Configuration; // Để cấu hình lại tài nguyên hệ thống (ví dụ: ngôn ngữ)
import android.content.res.Resources; // Cung cấp quyền truy cập vào tài nguyên của ứng dụng
import android.os.Bundle; // Để truyền dữ liệu giữa các thành phần
import android.view.View; // Lớp cơ sở cho tất cả các thành phần giao diện

import androidx.activity.EdgeToEdge; // Hỗ trợ hiển thị ứng dụng tràn viền
import androidx.appcompat.app.AppCompatActivity; // Lớp cơ sở cho các Activity hỗ trợ thư viện AppCompat
import androidx.appcompat.app.AppCompatDelegate; // Quản lý các chế độ giao diện (như chế độ tối)
import androidx.core.graphics.Insets; // Đại diện cho các khoảng cách lề (padding/margin)
import androidx.core.view.ViewCompat; // Cung cấp khả năng tương thích ngược cho các tính năng của View
import androidx.navigation.NavController; // Điều khiển luồng điều hướng trong ứng dụng
import androidx.navigation.fragment.NavHostFragment; // Fragment làm vật chứa cho các màn hình điều hướng
import androidx.navigation.ui.NavigationUI; // Kết nối NavController với các thành phần UI như BottomNavigationView
import androidx.core.view.WindowInsetsCompat; // Quản lý các phần lề của cửa sổ (như thanh trạng thái, thanh điều hướng)

import com.google.android.material.bottomnavigation.BottomNavigationView; // Thanh điều hướng phía dưới
import com.google.android.material.floatingactionbutton.FloatingActionButton; // Nút bấm nổi (Floating Action Button)

import java.util.Locale; // Định nghĩa ngôn ngữ và khu vực

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
                // Ẩn nút FAB tiện ích ở màn hình khởi đầu và màn hình Ngân sách để tránh đè nút
                if (destination.getId() == R.id.nav_start) {
                    bottomNav.setVisibility(View.GONE);
                    fab.setVisibility(View.GONE);
                } else if (destination.getId() == R.id.nav_budget) {
                    bottomNav.setVisibility(View.VISIBLE);
                    fab.setVisibility(View.GONE); // Ẩn để hiện nút riêng của trang Ngân sách
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

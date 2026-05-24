package com.example.quanlychitieu;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.quanlychitieu.data.SyncManager;
import com.example.quanlychitieu.data.database.AppDatabase;
import com.example.quanlychitieu.worker.ReminderWorker;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.TimeUnit;

public class SettingsFragment extends Fragment {

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "wealthflow_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_BIOMETRIC = "biometric_enabled";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";

    private TextView tvUserNameSub, tvLanguageSub, tvEmailSub;
    private FirebaseAuth mAuth;
    private SyncManager syncManager;

    public SettingsFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mAuth = FirebaseAuth.getInstance();
        syncManager = new SyncManager(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvUserNameSub = view.findViewById(R.id.tv_user_name_sub);
        tvLanguageSub = view.findViewById(R.id.tv_language_sub);
        tvEmailSub = view.findViewById(R.id.tv_email_sub);

        updateUI();

        view.findViewById(R.id.layout_profile).setOnClickListener(v -> showEditProfileDialog());
        view.findViewById(R.id.layout_security).setOnClickListener(v -> showSecurityOptions());
        view.findViewById(R.id.layout_language).setOnClickListener(v -> showLanguageDialog());
        view.findViewById(R.id.layout_sync).setOnClickListener(v -> {
            syncManager.syncAll();
            showToast("Đang đồng bộ...");
        });

        // Chế độ tối
        MaterialSwitch switchDarkMode = view.findViewById(R.id.switch_dark_mode);
        switchDarkMode.setChecked(sharedPreferences.getBoolean(KEY_DARK_MODE, false));
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        // Bảo mật Biometric
        MaterialSwitch switchBiometric = view.findViewById(R.id.switch_biometric);
        switchBiometric.setChecked(sharedPreferences.getBoolean(KEY_BIOMETRIC, false));
        switchBiometric.setOnCheckedChangeListener((btn, isChecked) -> {
            sharedPreferences.edit().putBoolean(KEY_BIOMETRIC, isChecked).apply();
            showToast(isChecked ? "Đã bật khóa vân tay" : "Đã tắt khóa vân tay");
        });

        // Thông báo nhắc nhở
        MaterialSwitch switchNotif = view.findViewById(R.id.switch_notifications);
        switchNotif.setChecked(sharedPreferences.getBoolean(KEY_NOTIFICATIONS, true));
        switchNotif.setOnCheckedChangeListener((btn, isChecked) -> {
            sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS, isChecked).apply();
            if (isChecked) scheduleDailyReminder();
            else WorkManager.getInstance(requireContext()).cancelUniqueWork("DailyReminder");
        });

        view.findViewById(R.id.layout_logout).setOnClickListener(v -> showLogoutConfirm());
    }

    private void updateUI() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            if (name == null || name.isEmpty()) name = sharedPreferences.getString(KEY_USER_NAME, "Người dùng");
            tvUserNameSub.setText(name);
            tvEmailSub.setText(user.getEmail());
        }
        tvLanguageSub.setText(sharedPreferences.getString(KEY_LANGUAGE, "Tiếng Việt"));
    }

    private void showSecurityOptions() {
        String[] options = {"Quản lý Vay & Nợ", "Định kỳ (Recurring)", "Đổi mật khẩu", "Xuất báo cáo (CSV)", "Xóa sạch dữ liệu"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Tiện ích & Bảo mật")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: Navigation.findNavController(getView()).navigate(R.id.nav_debt_loan); break;
                        case 1: Navigation.findNavController(getView()).navigate(R.id.nav_recurring); break;
                        case 2: Navigation.findNavController(getView()).navigate(R.id.resetPasswordFragment); break;
                        case 3: exportToCSV(); break;
                        case 4: showClearDataConfirm(); break;
                    }
                }).show();
    }

    private void exportToCSV() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                String userId = mAuth.getUid();
                var transactions = AppDatabase.getDatabase(requireContext()).transactionDao().getAllTransactionsSync(userId);
                File file = new File(requireContext().getExternalFilesDir(null), "transactions.csv");
                FileWriter writer = new FileWriter(file);
                writer.append("ID,Amount,Type,Date,Note\n");
                for (var t : transactions) {
                    writer.append(t.getId() + "," + t.getAmount() + "," + t.getType() + "," + t.getDate() + "," + t.getNote() + "\n");
                }
                writer.flush();
                writer.close();
                getActivity().runOnUiThread(() -> showToast("Đã xuất file tại: " + file.getAbsolutePath()));
            } catch (Exception e) {
                getActivity().runOnUiThread(() -> showToast("Lỗi khi xuất file"));
            }
        });
    }

    private void scheduleDailyReminder() {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(ReminderWorker.class, 1, TimeUnit.DAYS)
                .setInitialDelay(12, TimeUnit.HOURS) // Ví dụ: Nhắc vào buổi tối
                .build();
        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork("DailyReminder", ExistingPeriodicWorkPolicy.KEEP, request);
    }

    private void showLogoutConfirm() {
        new AlertDialog.Builder(requireContext()).setTitle("Đăng xuất").setMessage("Bạn muốn thoát?")
                .setPositiveButton("Thoát", (d, w) -> { mAuth.signOut(); Navigation.findNavController(getView()).navigate(R.id.action_settings_to_login); })
                .setNegativeButton("Hủy", null).show();
    }

    private void showEditProfileDialog() {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
        EditText et = v.findViewById(R.id.et_profile_name);
        if (mAuth.getCurrentUser() != null) et.setText(mAuth.getCurrentUser().getDisplayName());
        new AlertDialog.Builder(requireContext()).setTitle("Sửa hồ sơ").setView(v)
                .setPositiveButton("Lưu", (d, w) -> {
                    String n = et.getText().toString().trim();
                    if (!n.isEmpty()) {
                        sharedPreferences.edit().putString(KEY_USER_NAME, n).apply();
                        updateUI();
                    }
                }).show();
    }

    private void showClearDataConfirm() {
        new AlertDialog.Builder(requireContext()).setTitle("Xóa dữ liệu").setMessage("Xóa sạch lịch sử giao dịch?")
                .setPositiveButton("Xóa", (d, w) -> AppDatabase.databaseWriteExecutor.execute(() -> {
                    AppDatabase.getDatabase(requireContext()).transactionDao().deleteAllTransactions(mAuth.getUid());
                    getActivity().runOnUiThread(() -> showToast("Đã xóa sạch"));
                })).setNegativeButton("Hủy", null).show();
    }

    private void showLanguageDialog() {
        String[] langs = {"Tiếng Việt", "English"};
        new AlertDialog.Builder(requireContext()).setTitle("Ngôn ngữ").setItems(langs, (d, w) -> {
            sharedPreferences.edit().putString(KEY_LANGUAGE, langs[w]).apply();
            requireActivity().recreate();
        }).show();
    }

    private void showToast(String m) { Toast.makeText(getContext(), m, Toast.LENGTH_SHORT).show(); }
}

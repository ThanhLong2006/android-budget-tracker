package com.example.quanlychitieu;

// Import các thư viện cần thiết cho giao diện, lưu trữ và xác thực
import android.app.AlertDialog; // Dùng để tạo các hộp thoại thông báo/xác nhận
import android.content.Context; // Cung cấp ngữ cảnh ứng dụng
import android.content.SharedPreferences; // Lưu trữ các cấu hình nhỏ như Chế độ tối
import android.os.Bundle; // Truyền dữ liệu giữa các Fragment
import android.view.LayoutInflater; // Nạp layout từ XML
import android.view.View; // Lớp cơ sở cho UI
import android.view.ViewGroup; // Chứa các View khác
import android.widget.EditText; // Ô nhập liệu trong Dialog
import android.widget.TextView; // Hiển thị văn bản
import android.widget.Toast; // Thông báo nhanh

import androidx.annotation.NonNull; // Tham số không null
import androidx.annotation.Nullable; // Tham số có thể null
import androidx.appcompat.app.AppCompatDelegate; // Điều khiển giao diện Sáng/Tối
import androidx.fragment.app.Fragment; // Mảnh giao diện
import androidx.navigation.Navigation; // Điều hướng màn hình

import com.example.quanlychitieu.data.SyncManager; // Quản lý đồng bộ Cloud
import com.example.quanlychitieu.data.database.AppDatabase; // Database Room
import com.google.android.material.materialswitch.MaterialSwitch; // Nút gạt Dark Mode
import com.google.firebase.auth.FirebaseAuth; // Xác thực người dùng Firebase
import com.google.firebase.auth.FirebaseUser; // Người dùng hiện tại

/**
 * SettingsFragment: Quản lý các thiết lập ứng dụng và tài khoản.
 */
public class SettingsFragment extends Fragment {

    // Định nghĩa các hằng số làm khóa lưu trữ thông tin cấu hình
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "wealthflow_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_LANGUAGE = "language";

    // Khai báo các thành phần UI
    private TextView tvUserNameSub, tvLanguageSub, tvEmailSub;
    private FirebaseAuth mAuth; // Đối tượng Firebase xử lý Account
    private SyncManager syncManager; // Xử lý đồng bộ dữ liệu

    public SettingsFragment() {} // Constructor mặc định

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Khởi tạo các đối tượng xử lý dữ liệu và xác thực ngay khi Fragment được tạo
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mAuth = FirebaseAuth.getInstance();
        syncManager = new SyncManager(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Nạp layout fragment_settings để hiển thị lên màn hình
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ các View từ XML sang mã Java
        tvUserNameSub = view.findViewById(R.id.tv_user_name_sub);
        tvLanguageSub = view.findViewById(R.id.tv_language_sub);
        tvEmailSub = view.findViewById(R.id.tv_email_sub);

        updateUI(); // Hiển thị thông tin người dùng hiện tại lên màn hình

        // Thiết lập sự kiện click cho các mục trong cài đặt
        view.findViewById(R.id.layout_profile).setOnClickListener(v -> showEditProfileDialog());
        view.findViewById(R.id.layout_security).setOnClickListener(v -> showSecurityOptions());
        
        // Mục đồng bộ dữ liệu thủ công
        View syncLayout = view.findViewById(R.id.layout_sync);
        if (syncLayout != null) {
            syncLayout.setOnClickListener(v -> {
                syncManager.syncAll();
                showToast("Đang bắt đầu đồng bộ dữ liệu...");
            });
        }

        view.findViewById(R.id.layout_language).setOnClickListener(v -> showLanguageDialog());

        // Xử lý nút gạt Chế độ tối
        MaterialSwitch switchDarkMode = view.findViewById(R.id.switch_dark_mode);
        switchDarkMode.setChecked(sharedPreferences.getBoolean(KEY_DARK_MODE, false));
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        // Thiết lập sự kiện nhấn nút Đăng xuất
        view.findViewById(R.id.layout_logout).setOnClickListener(v -> showLogoutConfirm());
    }

    /**
     * updateUI: Lấy và hiển thị thông tin mới nhất của người dùng.
     */
    private void updateUI() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            if (name == null || name.isEmpty()) {
                name = sharedPreferences.getString(KEY_USER_NAME, "Người dùng");
            }
            if (tvUserNameSub != null) tvUserNameSub.setText(name);
            if (tvEmailSub != null) tvEmailSub.setText(user.getEmail());
        }
        
        String lang = sharedPreferences.getString(KEY_LANGUAGE, "Tiếng Việt");
        if (tvLanguageSub != null) tvLanguageSub.setText(lang);
    }

    /**
     * showLogoutConfirm: Hiển thị hộp thoại xác nhận đăng xuất và thực hiện thoát tài khoản.
     * Giải thích: Sử dụng mAuth.signOut() để xóa session và Navigation để quay về màn hình khởi đầu.
     */
    private void showLogoutConfirm() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    // Bước 1: Đăng xuất khỏi Firebase
                    mAuth.signOut();
                    
                    // Bước 2: Điều hướng về màn hình Login (action_settings_to_login đã cấu hình clear backstack)
                    if (getView() != null) {
                        Navigation.findNavController(getView()).navigate(R.id.action_settings_to_login);
                        showToast("Đã đăng xuất");
                    }
                })
                .setNegativeButton("Hủy", null)
                .show(); // Đã thêm .show() để hiển thị hộp thoại
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Chỉnh sửa hồ sơ");
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
        builder.setView(dialogView);
        EditText etName = dialogView.findViewById(R.id.et_profile_name);
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) etName.setText(user.getDisplayName());

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newName = etName.getText().toString().trim();
            if (!newName.isEmpty() && user != null) {
                com.google.firebase.auth.UserProfileChangeRequest profileUpdates = new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(newName)
                        .build();
                user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        sharedPreferences.edit().putString(KEY_USER_NAME, newName).apply();
                        updateUI();
                        showToast("Đã cập nhật tên thành công");
                    }
                });
            }
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void showSecurityOptions() {
        String[] options = {"Đổi mật khẩu", "Xóa toàn bộ dữ liệu", "Xuất báo cáo (CSV)"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Bảo mật & Dữ liệu")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Navigation.findNavController(getView()).navigate(R.id.resetPasswordFragment);
                    } else if (which == 1) {
                        showClearDataConfirm();
                    } else if (which == 2) {
                        showToast("Tính năng đang phát triển...");
                    }
                })
                .show();
    }

    private void showClearDataConfirm() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa dữ liệu")
                .setMessage("Bạn có chắc chắn muốn xóa toàn bộ giao dịch? Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa tất cả", (dialog, which) -> {
                    String userId = mAuth.getUid();
                    if (userId != null) {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            AppDatabase.getDatabase(requireContext()).transactionDao().deleteAllTransactions(userId);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> showToast("Đã xóa sạch dữ liệu"));
                            }
                        });
                    }
                })
                .setNegativeButton("Hủy", null)
                .show(); // Sửa lỗi gọi biến builder không tồn tại
    }

    private void showLanguageDialog() {
        String[] languages = {"Tiếng Việt", "English"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Chọn ngôn ngữ")
                .setItems(languages, (dialog, which) -> {
                    String selectedLang = languages[which];
                    String currentLang = sharedPreferences.getString(KEY_LANGUAGE, "Tiếng Việt");
                    if (!selectedLang.equals(currentLang)) {
                        sharedPreferences.edit().putString(KEY_LANGUAGE, selectedLang).apply();
                        requireActivity().recreate();
                    }
                })
                .show();
    }

    private void showToast(String message) {
        if (isAdded()) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}

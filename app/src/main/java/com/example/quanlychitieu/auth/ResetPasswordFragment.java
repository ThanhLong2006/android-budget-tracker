package com.example.quanlychitieu.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.quanlychitieu.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * ResetPasswordFragment: Xử lý đổi mật khẩu an toàn.
 * Đã sửa lỗi: Thêm .trim() để tránh khoảng trắng thừa gây lệch mật khẩu giữa các màn hình.
 */
public class ResetPasswordFragment extends Fragment {

    private TextInputEditText etOldPassword, etNewPassword, etConfirmNewPassword;
    private Button btnReset;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reset_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etOldPassword = view.findViewById(R.id.et_old_password);
        etNewPassword = view.findViewById(R.id.et_new_password);
        etConfirmNewPassword = view.findViewById(R.id.et_confirm_new_password);
        btnReset = view.findViewById(R.id.btn_reset);
        progressBar = view.findViewById(R.id.progress_bar);

        btnReset.setOnClickListener(v -> validateAndReset());
    }

    private void validateAndReset() {
        // Sử dụng .trim() để đảm bảo tính đồng nhất giữa các màn hình Đăng ký/Đăng nhập/Đổi mật khẩu
        String oldPassword = etOldPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmNewPassword.getText().toString().trim();

        if (TextUtils.isEmpty(oldPassword)) {
            etOldPassword.setError("Vui lòng nhập mật khẩu cũ");
            return;
        }
        if (newPassword.length() < 6) {
            etNewPassword.setError("Mật khẩu mới phải ít nhất 6 ký tự");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            etConfirmNewPassword.setError("Xác nhận mật khẩu không khớp");
            return;
        }
        if (newPassword.equals(oldPassword)) {
            etNewPassword.setError("Mật khẩu mới phải khác mật khẩu cũ");
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            progressBar.setVisibility(View.VISIBLE);
            btnReset.setEnabled(false);

            // Bước 1: Xác thực lại để đảm bảo quyền đổi mật khẩu
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPassword);

            user.reauthenticate(credential).addOnCompleteListener(reAuthTask -> {
                if (reAuthTask.isSuccessful()) {
                    // Bước 2: Cập nhật mật khẩu mới lên server Firebase
                    user.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
                        progressBar.setVisibility(View.GONE);
                        btnReset.setEnabled(true);

                        if (updateTask.isSuccessful()) {
                            // Bước 3: Đăng xuất ngay sau khi đổi thành công để xóa session cũ
                            mAuth.signOut();
                            Toast.makeText(getContext(), "Đổi mật khẩu thành công! Vui lòng đăng nhập lại bằng mật khẩu mới.", Toast.LENGTH_LONG).show();
                            
                            // Quay về màn hình đăng nhập
                            if (getView() != null) {
                                Navigation.findNavController(getView()).navigate(R.id.action_resetPassword_to_login);
                            }
                        } else {
                            Toast.makeText(getContext(), "Lỗi: " + updateTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    progressBar.setVisibility(View.GONE);
                    btnReset.setEnabled(true);
                    etOldPassword.setError("Mật khẩu cũ không chính xác");
                    Toast.makeText(getContext(), "Xác thực thất bại, mật khẩu cũ không đúng.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}

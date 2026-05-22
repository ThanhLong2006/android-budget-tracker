package com.example.quanlychitieu.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.quanlychitieu.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class LoginFragment extends Fragment {

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword;
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
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etEmail = view.findViewById(R.id.et_email);
        etPassword = view.findViewById(R.id.et_password);
        btnLogin = view.findViewById(R.id.btn_login);
        tvRegister = view.findViewById(R.id.tv_register);
        tvForgotPassword = view.findViewById(R.id.tv_forgot_password);
        progressBar = view.findViewById(R.id.progress_bar);

        btnLogin.setOnClickListener(v -> loginUser());

        tvRegister.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_login_to_register)
        );

        tvForgotPassword.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_login_to_forgotPassword)
        );
    }

    private void loginUser() {
        // ĐÃ FIX: Sử dụng .trim() cho cả email và mật khẩu để đảm bảo đồng bộ với Register/Reset
        // Điều này giúp tránh lỗi do bàn phím tự động thêm dấu cách ở cuối
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email là bắt buộc");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Mật khẩu là bắt buộc");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(getView()).navigate(R.id.action_login_to_dashboard);
                    } else {
                        String errorMessage;
                        Exception e = task.getException();
                        Log.e("LoginError", "Auth failed", e);

                        if (e instanceof FirebaseAuthInvalidUserException) {
                            errorMessage = "Tài khoản không tồn tại.";
                        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            errorMessage = "Sai mật khẩu hoặc email không hợp lệ.";
                        } else {
                            errorMessage = "Lỗi: " + e.getLocalizedMessage();
                        }
                        
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }
}

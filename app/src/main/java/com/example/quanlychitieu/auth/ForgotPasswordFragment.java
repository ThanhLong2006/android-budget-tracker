package com.example.quanlychitieu.auth;

// Import các thư viện hỗ trợ giao diện và Firebase Auth
import android.os.Bundle; // Dùng để lưu trữ và truyền trạng thái của Fragment
import android.text.TextUtils; // Công cụ kiểm tra văn bản (như kiểm tra chuỗi rỗng)
import android.view.LayoutInflater; // Dùng để nạp file XML layout thành đối tượng View
import android.view.View; // Lớp cơ sở cho các thành phần giao diện
import android.view.ViewGroup; // Lớp chứa các View khác
import android.widget.Button; // Thành phần nút bấm
import android.widget.ProgressBar; // Hiển thị tiến trình đang xử lý trên màn hình
import android.widget.TextView; // Thành phần hiển thị văn bản
import android.widget.Toast; // Dùng để hiển thị thông báo nhanh cho người dùng

import androidx.annotation.NonNull; // Đánh dấu tham số không được phép null
import androidx.annotation.Nullable; // Đánh dấu tham số có thể là null
import androidx.fragment.app.Fragment; // Mảnh giao diện quản lý UI linh hoạt
import androidx.navigation.Navigation; // Thư viện điều hướng chuyển màn hình

import com.example.quanlychitieu.R; // Tham chiếu đến các tài nguyên của ứng dụng
import com.google.android.material.textfield.TextInputEditText; // Ô nhập liệu theo chuẩn Material Design
import com.google.firebase.auth.FirebaseAuth; // Thư viện quản lý xác thực của Firebase


 // ForgotPasswordFragment: Màn hình hỗ trợ người dùng khôi phục mật khẩu qua Email.
 // Sử dụng tính năng gửi email đặt lại mật khẩu của Firebase Auth.

public class ForgotPasswordFragment extends Fragment {

    // Khai báo các biến đại diện cho các thành phần giao diện
    private TextInputEditText etEmail; // Ô nhập địa chỉ Email cần khôi phục
    private Button btnSend; // Nút bấm gửi yêu cầu khôi phục
    private TextView tvBackToLogin; // Dòng chữ quay lại màn hình đăng nhập
    private ProgressBar progressBar; // Vòng quay tải dữ liệu
    private FirebaseAuth mAuth; // Đối tượng xử lý xác thực từ Firebase

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Khởi tạo FirebaseAuth ngay khi tạo Fragment để sẵn sàng sử dụng.
        // Chọn khởi tạo ở đây thay vì onCreateView để đảm bảo instance được tạo đúng vòng đời.
        mAuth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Nạp giao diện từ file XML fragment_forgot_password.
        // Chọn false vì hệ thống sẽ tự động gắn View này vào container sau đó.
        return inflater.inflate(R.layout.fragment_forgot_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ các View từ XML sang Java thông qua ID.
        // Việc này bắt buộc để code có thể lấy giá trị người dùng nhập hoặc gán sự kiện click.
        etEmail = view.findViewById(R.id.et_email);
        btnSend = view.findViewById(R.id.btn_send);
        tvBackToLogin = view.findViewById(R.id.tv_back_to_login);
        progressBar = view.findViewById(R.id.progress_bar);

        // Thiết lập sự kiện khi nhấn nút "Gửi yêu cầu".
        btnSend.setOnClickListener(v -> resetPassword());

        // Thiết lập sự kiện khi nhấn "Quay lại đăng nhập".
        // Dùng navigateUp() để quay lại màn hình trước đó trong ngăn xếp (backstack).
        tvBackToLogin.setOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp()
        );
    }


     // resetPassword: Logic kiểm tra email và gửi link đặt lại mật khẩu.

    private void resetPassword() {
        // Lấy email người dùng nhập và xóa khoảng trắng ở 2 đầu (trim).
        String email = etEmail.getText().toString().trim();

        // Kiểm tra xem email có bị bỏ trống hay không.
        // Dùng setError để hiển thị lỗi ngay tại ô nhập liệu giúp người dùng dễ thấy.
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email là bắt buộc");
            return;
        }

        // Hiện vòng quay tải và vô hiệu hóa nút gửi để tránh nhấn nhiều lần gây lỗi.
        progressBar.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);

        // Gọi phương thức của Firebase để gửi email khôi phục mật khẩu.
        // Chọn giải pháp này vì Firebase tự động xử lý việc gửi email, lập trình viên không cần server riêng.
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    // Sau khi xong (dù thành công hay bại), ẩn tiến trình và bật lại nút.
                    progressBar.setVisibility(View.GONE);
                    btnSend.setEnabled(true);
                    
                    if (task.isSuccessful()) {
                        // Nếu thành công, thông báo và quay lại màn hình đăng nhập.
                        Toast.makeText(getContext(), "Email đặt lại mật khẩu đã được gửi!", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(getView()).navigateUp();
                    } else {
                        // Nếu lỗi (vd: email không tồn tại), hiển thị thông báo lỗi chi tiết từ Firebase.
                        Toast.makeText(getContext(), "Lỗi: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
